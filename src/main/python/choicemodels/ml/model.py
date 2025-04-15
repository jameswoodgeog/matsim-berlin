#!/usr/bin/env python
# -*- coding: utf-8 -*-

import lightning as L
import torch
import torchmetrics


class ChoiceModel(L.LightningModule):
    """ A simple choice model using PyTorch Lightning """

    def __init__(self, choices: list, layers: list, joint_network: bool,
                num_global_features: int, num_features_per_mode: int,
                 dropout: float = 0.3, label_smoothing: float = 0.2):
        super().__init__()

        self.valid_acc = torchmetrics.classification.Accuracy(task="multiclass", num_classes=len(choices))
        self.joint_network = joint_network
        self.num_global_features = num_global_features
        self.num_features_per_mode = num_features_per_mode

        # One large network to model all decisions simultaneously
        if joint_network:
            num_features = num_global_features + num_features_per_mode * len(choices)
            l = []
            for i in range(len(layers)):

                layer = torch.nn.Linear(num_features, layers[i], bias=True)
                # torch.nn.init.xavier_uniform_(layer.weight)

                l.append(layer)
                l.append(torch.nn.ReLU())

                if i < len(layers) - 1 and dropout > 0:
                    l.append(torch.nn.Dropout(dropout))

                num_features = layers[i]

            l.append(torch.nn.Linear(num_features, len(choices), bias=True))

            self.model = torch.nn.Sequential(*l)
        else:

            # Separate networks for each choice
            self.model = torch.nn.ModuleList()
            for i in range(len(choices)):
                num_features = num_global_features + num_features_per_mode
                l = []
                for j in range(len(layers)):
                    layer = torch.nn.Linear(num_features, layers[j], bias=True)
                    # torch.nn.init.xavier_uniform_(layer.weight)

                    l.append(layer)
                    l.append(torch.nn.ReLU())

                    if j < len(layers) - 1 and dropout > 0:
                        l.append(torch.nn.Dropout(dropout))

                    num_features = layers[j]

                l.append(torch.nn.Linear(num_features, 1, bias=True))
                self.model.append(torch.nn.Sequential(*l))

        self.loss = torch.nn.CrossEntropyLoss(reduction="none", label_smoothing=label_smoothing)

        # Validation loss is not smoothed. Masking out unavailable choices leads to over/underflow
        self.val_loss = torch.nn.CrossEntropyLoss(reduction="none")

    def forward(self, x: torch.Tensor, avail: torch.BoolTensor = None):


        if self.joint_network:
            Y = self.model(x)
        else:

            logits = []
            for i, m in enumerate(self.model):

                idx = list(range(self.num_global_features))
                idx += list(range(self.num_global_features + i * self.num_features_per_mode, self.num_global_features + (i+1) * self.num_features_per_mode))

                # Pass global and mode features to each module
                l = m(x[:, idx])
                logits.append(l)

            # Stack the input from the separate networks
            Y = torch.stack(logits, dim=1).squeeze(-1)

        # Mask the logits for unavailable choices
        if avail is not None:
            Y[~avail] = torch.finfo(Y.dtype).min / 2

        return Y

    def training_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x)

        loss = self.loss(logits, y)
        loss = (loss * w).sum()

        self.log("train_loss", loss)
        return loss

    def validation_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x, avail)

        loss = self.val_loss(logits, y)

        self.log("val_loss", (loss * w).sum(), prog_bar=True)
        self.valid_acc.update(logits, y)

    def predict_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x, avail)

        return logits

    def on_validation_epoch_end(self):
        self.log('valid_acc_epoch', self.valid_acc.compute())
        self.valid_acc.reset()

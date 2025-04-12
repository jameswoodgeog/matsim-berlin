#!/usr/bin/env python
# -*- coding: utf-8 -*-

import lightning as L
import torch
import torchmetrics


class ChoiceModel(L.LightningModule):
    """ A simple choice model using PyTorch Lightning """

    def __init__(self, choices: list, layers: list, num_features: int):
        super().__init__()

        self.valid_acc = torchmetrics.classification.Accuracy(task="multiclass", num_classes=len(choices))

        l = []
        for i in range(len(layers)):

            layer = torch.nn.Linear(num_features, layers[i], bias=True)
            # torch.nn.init.xavier_uniform_(layer.weight)

            l.append(layer)
            l.append(torch.nn.ReLU())

            num_features = layers[i]

        l.append(torch.nn.Linear(num_features, len(choices), bias=True))

        self.model = torch.nn.Sequential(*l)
        self.loss = torch.nn.CrossEntropyLoss(reduction="none")

    def forward(self, x: torch.Tensor, avail: torch.BoolTensor):
        Y = self.model(x)
        # Mask the logits for unavailable choices
        Y[~avail] = torch.finfo(Y.dtype).min / 2

        return Y

    def training_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x, avail)

        loss = self.loss(logits, y)

        return (loss * w).sum()

    def validation_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x, avail)

        loss = self.loss(logits, y)

        self.log("val_loss", (loss * w).sum(), prog_bar=True)
        self.valid_acc.update(logits, y)

    def predict_step(self, batch, batch_idx):
        x, y, avail, w = batch
        logits = self(x, avail)

        return logits

    def on_validation_epoch_end(self):
        self.log('valid_acc_epoch', self.valid_acc.compute())
        self.valid_acc.reset()

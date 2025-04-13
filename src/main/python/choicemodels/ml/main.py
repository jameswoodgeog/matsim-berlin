#!/usr/bin/env python
# -*- coding: utf-8 -*-

import torch

from lightning.pytorch.cli import LightningCLI

from model import ChoiceModel
from dataset import ChoiceData

class MyLightningCLI(LightningCLI):
    """ Pytorch CLI for choice modelling using ML.
        This is purely experimental and not used in production."""

    def add_arguments_to_parser(self, parser):
        parser.link_arguments("data.choices", "model.choices")
        parser.link_arguments("data.num_features", "model.num_features", apply_on="instantiate")

        parser.add_optimizer_args(torch.optim.Adam)
        parser.add_lr_scheduler_args(torch.optim.lr_scheduler.ExponentialLR)
        # parser.add_lr_scheduler_args(torch.optim.lr_scheduler.CosineAnnealingWarmRestarts)

    def configure_optimizers(self, *args, **kwargs):
        result = super().configure_optimizers(*args, **kwargs)

        return {"optimizer": result[0][0], "lr_scheduler": result[1][0], "monitor": "val_loss"}

if __name__ == "__main__":
    cli = MyLightningCLI(ChoiceModel, ChoiceData)
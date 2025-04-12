#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import numpy as np
import pandas as pd

import torch
import torch.nn.functional as F

from lightning.pytorch.callbacks import BasePredictionWriter

class CustomWriter(BasePredictionWriter):
    """ Write the obtained predictions."""

    def __init__(self, output_dir):
        super().__init__('epoch')
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)

    def write_on_epoch_end(self, trainer, pl_module, predictions, batch_indices):

        ds = trainer.predict_dataloaders.dataset

        dfs = []

        for idx, logits in zip(batch_indices[0], predictions):
            probs = F.softmax(logits, dim=1).cpu().numpy()
            ref = ds.get_ref(idx)

            df = pd.concat((pd.DataFrame(ref).astype(object), pd.DataFrame(probs, columns=ds.modes)), axis=1, copy=False)
            dfs.append(df)

        df = pd.concat(dfs, axis=0)
        df.to_csv(os.path.join(self.output_dir, "predictions.csv"), index=False)

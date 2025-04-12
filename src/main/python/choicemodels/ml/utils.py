#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import numpy as np
import pandas as pd

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

            df = pd.concat((pd.DataFrame(ref).reset_index(drop=True), pd.DataFrame(probs, columns=ds.modes)),
                           axis=1, copy=False)

            df.rename(columns={"person": "ID"}, inplace=True)
            df["trip_n"] += 1
            df["chosen"] = np.diagonal(probs[..., ref.choice.values-1])

            dfs.append(df)

        df = pd.concat(dfs, axis=0)
        df.to_csv(os.path.join(self.output_dir, "predictions.csv"), index=False)

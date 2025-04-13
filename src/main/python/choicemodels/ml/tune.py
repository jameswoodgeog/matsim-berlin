#!/usr/bin/env python
# -*- coding: utf-8 -*-

import logging
import multiprocess as mp
import numpy as np
import optuna
from unittest import mock

from dataset import ChoiceData
from main import MyLightningCLI
from model import ChoiceModel


def objective(trial):
    args = ["main.py", "fit", "--config", "config.yaml", "--config", "tune.yaml", "--model.layers"]

    n_layers = trial.suggest_int('n_layers', 2, 5)

    layers = []
    for i in range(n_layers):
        n_units = trial.suggest_categorical(f'n_units_l{i}', [2, 4, 8, 16, 32, 64, 128])
        layers.append(str(n_units))

    args.append("[" + (", ".join(layers)) + "]")

    print("Layers: ", layers)

    def run(i):
        logging.getLogger("lightning.pytorch.utilities.rank_zero").setLevel(logging.FATAL)
        logging.getLogger("lightning.pytorch.callbacks.model_checkpoint").setLevel(logging.FATAL)
        logging.getLogger("lightning.pytorch.trainer.setup").setLevel(logging.FATAL)
        logging.getLogger("lightning.pytorch.trainer.trainer.connectors.data_connector").setLevel(logging.FATAL)

        with mock.patch("sys.argv", args):
            cli = MyLightningCLI(ChoiceModel, ChoiceData, save_config_callback=None)

        return float(cli.trainer.checkpoint_callback.best_model_score)

    with mp.Pool(8) as pool:
        scores = pool.map(run, range(8))

    print("Result: ", scores)

    return np.mean(scores)


if __name__ == "__main__":
    study = optuna.create_study(storage="sqlite:///calib.db", direction="minimize")
    study.optimize(objective, n_trials=100)
    print(study.best_params)

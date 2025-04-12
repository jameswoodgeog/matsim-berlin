#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import defaultdict

import torch
import lightning as L
import pandas as pd
import numpy as np

from torch.utils.data import random_split, Dataset, DataLoader
from scipy.stats import qmc

# Cost parameter from current berlin model
daily_costs = defaultdict(lambda: 0.0, car=-14.30, pt=-3)
km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

class ChoiceDataSet(torch.utils.data.Dataset):
    """ Wrapper around the dataframe containing the choice data """

    def __init__(self, df, modes, features, mode_features, mask=None):

        if mask is not None:
            df = df[mask]

        ft = list(features)
        for m in modes:
            ft += [f"{m}_{f}" for f in mode_features]

        self.ref = df[["person", "trip_n", "choice"]]
        self.x = df[ft].to_numpy().astype(np.float32)
        self.y = df["choice"].to_numpy().astype(np.int64) - 1
        self.w = df["weight"].to_numpy().astype(np.float32)
        self.avail = df[[f"{m}_valid" for m in modes]].to_numpy().astype(np.bool_)
        self.modes = modes

    def get_ref(self, idx):
        return self.ref.iloc[idx]

    def __getitem__(self, idx):
        # TODO: include matrix with variations, this can be applied only when requested to keep memory usage low
        return self.x[idx], self.y[idx], self.avail[idx], self.w[idx]

    def __len__(self):
        return self.x.shape[0]

def calc_trip_weight(df):
    """ Calculate weight of each trip in relation to whole day """
    dists = df.groupby("person").agg(dist=("beelineDist", "sum"))

    # Trips weighted by distance during the whole day
    dist_weight = df.beelineDist / dists.loc[df.person].dist.to_numpy()
    df["dist_weight"] = dist_weight

    df.dist_weight = df.dist_weight.fillna(1)

class ChoiceData(L.LightningDataModule):
    """ Read and augment choice data from csv file """

    def __init__(self, choice_path: str, trip_path: str, person_path: str,
                 choices: list, features: list, mode_features: list, batch_size: int = 1024):
        super().__init__()
        self.choice_path = choice_path
        self.trip_path = trip_path
        self.person_path = person_path
        self.choices = choices
        self.features = features
        self.mode_features = mode_features
        self.batch_size = batch_size

        self.train = None
        self.test = None
        self.variations = qmc.MultivariateNormalQMC(mean=[0] * len(choices), cov=np.eye(len(choices)), seed=42)

        # This attribute is exposed to the model and needs to be set during init
        self.num_features = len(features) + len(choices) * len(mode_features)

    def setup(self, stage: str):
        """ Setup the dataset """
        choices = pd.read_csv(self.choice_path, comment="#")
        persons = pd.read_csv(self.person_path)

        calc_trip_weight(choices)

        for mode in self.choices:
            choices[f"{mode}_daily_cost"] = daily_costs[mode] * choices["dist_weight"]
            choices[f"{mode}_distance_cost"] = km_costs[mode] * choices[f"{mode}_km"]

        df = choices.merge(persons, left_on="person", right_on="idx", how="left", suffixes=("", "_person"))

        # Normalize some variables
        df.income /= 2000
        df.age /= 30

        rng = np.random.default_rng()

        persons = df.person.unique()

        train = rng.choice(persons, size=int(len(persons) * 0.8), replace=False)
        test = np.setdiff1d(persons, train)

        self.train = ChoiceDataSet(df, self.choices, self.features, self.mode_features, df.person.isin(train))
        self.test = ChoiceDataSet(df, self.choices, self.features, self.mode_features, df.person.isin(test))


    def train_dataloader(self):
        """ Create the training dataloader """
        dl = DataLoader(self.train, batch_size=self.batch_size, shuffle=True)

        return dl

    def test_dataloader(self):
        """ Create the test dataloader """
        dl = DataLoader(self.test, batch_size=self.batch_size, shuffle=False)

        return dl

    def val_dataloader(self):
        return self.test_dataloader()

    def predict_dataloader(self):
        return self.test_dataloader()
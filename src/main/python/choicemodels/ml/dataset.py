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

    def __init__(self, df, modes, features, mode_features, persons, variations):

        # Mask the dataframe to only include the selected persons
        df = df[df.person.isin(persons)]

        ft = list(features)
        for m in modes:
            ft += [f"{m}_{f}" for f in mode_features]

        self.ref = df[["person", "trip_n", "choice"]]
        self.x = df[ft].to_numpy().astype(np.float32)
        self.y = df["choice"].to_numpy().astype(np.int64) - 1
        self.w = df["weight"].to_numpy().astype(np.float32)
        self.avail = df[[f"{m}_valid" for m in modes]].to_numpy().astype(np.bool_)
        self.modes = modes
        self.persons = persons
        self.variations = variations
        self.variations_idx = df["variation_idx"].to_numpy().astype(np.int64) if variations is not None else None

    def get_ref(self, idx):
        return self.ref.iloc[idx]

    def __getitem__(self, idx):
        # Return the data for given index
        if self.variations is None:
            return self.x[idx], self.y[idx], self.avail[idx], self.w[idx]

        # If there are variations return augmented data
        # Calculate the original index and variation index
        orig_idx = idx // self.variations.shape[1]
        var_idx = idx % self.variations.shape[1]

        v = self.variations_idx[orig_idx]
        variation = self.variations[v, var_idx]

        x = np.concatenate((self.x[orig_idx], variation), axis=0)

        return x, self.y[orig_idx], self.avail[orig_idx], self.w[orig_idx]

    def __len__(self):
        return self.x.shape[0] * self.variations.shape[1] if self.variations is not None else self.x.shape[0]

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
                 choices: list, features: list, mode_features: list,
                 variations: int = 0,
                 batch_size: int = 4096):
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
        self.variations = variations

        # This attribute is exposed to the model and needs to be set during init
        self.num_global_features = len(features)
        self.num_features_per_mode = len(mode_features)

        # Additional variation features
        if variations > 0:
            self.num_features += len(choices) - 1

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

        # Convert to boolean
        df.pt_abo_avail = df.pt_abo_avail.map(dict(yes=True, no=False))

        # Convert some of the categorical variables
        df = pd.concat((df, pd.get_dummies(df.employment, prefix="employment")), axis=1)
        df = pd.concat((df, pd.get_dummies(df.gender, prefix="gender")), axis=1)
        df = pd.concat((df, pd.get_dummies(df.region_type, prefix="region_type")), axis=1)

        rng = np.random.default_rng()

        dist = qmc.MultivariateNormalQMC(mean=[0] * (len(self.choices) - 1), cov=np.eye(len(self.choices) - 1), seed=42)

        persons = df.person.unique()

        variations = None
        person_to_variation = {}
        if self.variations > 0:
            variations = np.zeros((len(persons), self.variations, len(self.choices) - 1), dtype=np.float32)
            for i, person_id in enumerate(persons):
                sample = dist.random(self.variations)
                variations[i, :, :] = sample
                person_to_variation[person_id] = i

            # Add a column to the dataframe that maps each row to its variation index
            df['variation_idx'] = df['person'].map(person_to_variation)

        train = rng.choice(persons, size=int(len(persons) * 0.8), replace=False)
        test = np.setdiff1d(persons, train)


        self.train = ChoiceDataSet(df, self.choices, self.features, self.mode_features, train, variations)
        self.test = ChoiceDataSet(df, self.choices, self.features, self.mode_features, test, variations)


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
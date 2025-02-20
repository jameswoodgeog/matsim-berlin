#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import pandas as pd

if __name__ == "__main__":

    df = pd.read_csv(os.path.expanduser("~/Volumes/math-cluster/matsim-berlin/calibration-v6.5/mode-choice-10pct-estimated-v2/runs/000/analysis/traffic-car/count_comparison_by_hour.csv"))

    df = df.groupby("link_id").sum()

    lower = df.simulated_traffic_volume < df.observed_traffic_volume * 0.7

    df[lower].to_csv("counts_underestimated.csv", index=True, columns=[])

#!/usr/bin/env python
# -*- coding: utf-8 -*-

#%%

import pandas as pd
import seaborn as sns

from statsmodels.nonparametric.smoothers_lowess import lowess


#%%

df = pd.read_csv('walk_speeds.csv')

df = df[(df.speed > 1) & (df.speed < 40)]
df = df[ (df.age >= 0) & (df.age <= 90)]

aggr = df.groupby('age').mean().reset_index()

smoothed = lowess(aggr.speed, aggr.age, frac=0.2, is_sorted=True, return_sorted=False, it=0)

aggr['smoothed'] = smoothed

sns.regplot(data=aggr, x="age", y="smoothed")

#%%
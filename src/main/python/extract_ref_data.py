#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np
import os
from matsim.scenariogen.data import EconomicStatus, TripMode, preparation, run_create_ref_data


def person_filter(df):
    """ Default person filter for reference data. """
    df = df[df.reporting_day <= 4]
    df = df[df.location == "Berlin"]

    df["age"] = preparation.cut(df.age, [0, 12, 18, 25, 35, 66, np.inf])
    df["zone"] = df.zone.str.split("-", expand=True)[0]

    # df["zone"] = df.zone.replace({
    #     "1": "Mitte",
    #     "2": "Friedrichshain-Kreuzberg",
    #     "3": "Pankow",
    #     "4": "Charlottenburg-Wilmersdorf",
    #     "5": "Spandau",
    #     "6": "Steglitz-Zehlendorf",
    #     "7": "Tempelhof-Schöneberg",
    #     "8": "Neukölln",
    #     "9": "Treptow-Köpenick",
    #     "10": "Marzahn-Hellersdorf",
    #     "11": "Lichtenberg",
    #     "12": "Reinickendorf"
    # })

    preparation.fill(df, "income", -1)
    preparation.compute_economic_status(df)

    df["income"] = preparation.cut(df.income / df.equivalent_size, [0, 250, 500, 750, 1000, 1250, 1500, 1750, 2000, 2500, 3000, 3500, np.inf])

    return df


def trip_filter(df):
    # Motorcycles are counted as cars
    df.loc[df.main_mode == TripMode.MOTORCYCLE, "main_mode"] = TripMode.CAR

    # Other mode are ignored in the total share
    df = df[df.main_mode != TripMode.OTHER]

    return df


if __name__ == "__main__":
    d = os.path.expanduser("~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/")

    result = run_create_ref_data.create(
        d + "Berlin+Umland",
        person_filter, trip_filter,
        run_create_ref_data.InvalidHandling.REMOVE_TRIPS,
        ref_groups=["age", "income", "economic_status", "employment", "car_avail", "bike_avail", "pt_abo_avail", "zone"]
    )

    print(result.share)

    print(result.groups)

    t = result.trips

    # Weighted mean
    wm = lambda x: np.average(x, weights=t.loc[x.index, "t_weight"])

    t["speed"] = (t.gis_length * 3600) / (t.duration * 60)

    aggr = t.groupby("main_mode").agg(kmh=("speed", wm), dist=("gis_length", wm))

    print("Avg per mode")
    print(aggr)

    print("Mobile persons", result.persons[result.persons.mobile_on_day == True].p_weight.sum() / result.persons.p_weight.sum())

    aggr = t.groupby(["main_mode", "age"]).agg(kmh=("speed", wm), dist=("gis_length", wm))
    print("Avg per mode and age")
    print(aggr)
#!/usr/bin/env python3
"""Fills the history on the phone with up to a year of made-up past, so the month and year charts have something to show.

It writes the hours of the three sensors (gas, temperature, humidity) into the database of the app, from a year ago up to where the real history
starts, and never over an hour that already has a reading. The app is stopped while the database is changed and opened again after.

    python3 tools/seed_history.py                 # fill the past, a year back
    python3 tools/seed_history.py --days 180      # six months back
    python3 tools/seed_history.py --dry-run       # only say what would be written
    python3 tools/seed_history.py --remove-before 1761700000000   # take the made-up hours out (the number is printed when it fills)

The phone needs USB debugging and a debug build of the app (it opens the database with `run-as`). Everything made up is older than the first real hour,
which is what makes it possible to take out again: nothing the board says later can be older than that.
"""

import argparse
import math
import os
import random
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timedelta, timezone

APP = "io.github.gustavommcv.ciscsmarthome"
DATABASE = "history.db"
HOUR_MS = 3_600_000
STEP_MINUTES = 10
LOCAL = timezone(timedelta(hours=-3))  # the maquette lives in Brazil; only the shape of the day depends on this

GAS, TEMPERATURE, HUMIDITY = "gas-level", "temperature", "humidity"
DEHUMIDIFIER_ON_AT, DEHUMIDIFIER_OFF_AT = 65.0, 55.0
GAS_ALARM = 160.0


def season(moment):
    """1 in the middle of the southern summer, -1 in the middle of the winter."""
    day = moment.timetuple().tm_yday
    return math.cos(2 * math.pi * (day - 40) / 365.25)


def generate(start_ms, end_ms, seed=2026):
    """The hours from start_ms (inclusive) to end_ms (exclusive) as (entity, hourMs, lowest, highest), one reading every ten minutes inside each hour."""
    rnd = random.Random(seed)
    steps_per_hour = 60 // STEP_MINUTES
    start_hour = start_ms // HOUR_MS
    end_hour = end_ms // HOUR_MS

    # A rainy spell makes the air wetter for a few days; it is a slow walk that is pulled back to zero.
    wet = 0.0
    humidity, dehumidifier_on = 60.0, False
    gas_drift = 0.0

    leaks = leak_moments(start_hour, end_hour, rnd)
    outages = outage_hours(start_hour, end_hour, rnd)
    rows = []

    for hour in range(start_hour, end_hour):
        moment = datetime.fromtimestamp(hour * 3600, tz=LOCAL)
        if moment.hour == 0:
            wet = wet * 0.8 + rnd.gauss(0, 3.0)
            wet = max(-6.0, min(10.0, wet))
        temperatures, humidities, gases = [], [], []
        for step in range(steps_per_hour):
            clock = moment.hour + step * STEP_MINUTES / 60
            daily = math.cos(2 * math.pi * (clock - 15) / 24)  # warmest at 15:00
            temperature = 24.0 + 3.5 * season(moment) + 2.0 * daily + 0.3 * wet / 10 + rnd.gauss(0, 0.15)
            ambient = 65.0 + 5.0 * max(season(moment), -0.4) + wet - 3.0 * daily + rnd.gauss(0, 0.4)
            humidity += (ambient - humidity) * 0.10
            if dehumidifier_on:
                humidity -= 0.9
                if humidity <= DEHUMIDIFIER_OFF_AT:
                    dehumidifier_on = False
            elif humidity >= DEHUMIDIFIER_ON_AT:
                dehumidifier_on = True
            humidity = max(30.0, min(99.0, humidity))
            gas_drift += (0.0 - gas_drift) * 0.05 + rnd.gauss(0, 0.8)
            gas = 44.0 + gas_drift + 0.3 * (temperature - 24.0) + rnd.gauss(0, 1.2) + cooking(moment, step, rnd) + leak_level(hour, step, leaks)
            temperatures.append(temperature)
            humidities.append(humidity + rnd.gauss(0, 0.15))
            gases.append(max(5.0, gas))
        if hour in outages:
            continue
        rows.append((TEMPERATURE, hour * HOUR_MS, round(min(temperatures), 1), round(max(temperatures), 1)))
        rows.append((HUMIDITY, hour * HOUR_MS, round(min(humidities), 1), round(max(humidities), 1)))
        rows.append((GAS, hour * HOUR_MS, float(round(min(gases))), float(round(max(gases)))))
    return rows


def cooking(moment, step, rnd):
    """A bump of the gas reading while someone cooks: lunch and dinner, most days."""
    clock = moment.hour + step * STEP_MINUTES / 60
    for begin, end, chance in ((11.5, 13.0, 0.85), (18.5, 20.5, 0.9)):
        if begin <= clock < end:
            # The size of the bump is the same all through one meal: it is drawn from the day and the meal, not from each step.
            meal = random.Random(f"{moment.date()}-{begin}")
            if meal.random() > chance:
                return 0.0
            height = meal.uniform(12, 55)
            middle = (begin + end) / 2
            return height * max(0.0, 1 - abs(clock - middle) / ((end - begin) / 2))
    return 0.0


def leak_moments(start_hour, end_hour, rnd):
    """A handful of leaks over the period, each with the hour it begins at, how long it lasts and how high the reading goes."""
    hours = max(1, end_hour - start_hour)
    count = max(1, round(5 * hours / (365 * 24)))
    return [(start_hour + rnd.randrange(hours), rnd.uniform(0.4, 1.5), rnd.uniform(190, 420)) for _ in range(count)]


def leak_level(hour, step, leaks):
    at = hour + step * STEP_MINUTES / 60
    for begin, length, peak in leaks:
        if begin <= at < begin + length:
            return peak * math.sin(math.pi * (at - begin) / length)
    return 0.0


def outage_hours(start_hour, end_hour, rnd):
    """A few stretches when the phone did not hear the board, so the charts show the empty time as they do in real use."""
    hours = set()
    span = end_hour - start_hour
    for _ in range(max(0, round(3 * span / (365 * 24)))):
        begin = start_hour + rnd.randrange(max(1, span))
        hours.update(range(begin, begin + rnd.randrange(5, 30)))
    return hours


# ---- the phone -------------------------------------------------------------------------

def adb(*args, capture=True, check=True, input=None):
    return subprocess.run(["adb", *args], capture_output=capture, check=check, input=input)


def pull_database(directory, stop_app):
    if stop_app:
        adb("shell", "am", "force-stop", APP)
    for suffix in ("", "-wal", "-shm"):
        name = DATABASE + suffix
        result = adb("exec-out", "run-as", APP, "cat", f"databases/{name}", check=False)
        if result.returncode == 0 and (result.stdout or suffix == ""):
            with open(os.path.join(directory, name), "wb") as file:
                file.write(result.stdout)
    if not os.path.getsize(os.path.join(directory, DATABASE)):
        sys.exit("Could not read the database of the app. Is the debug build installed, with USB debugging on?")


def push_database(directory):
    adb("push", os.path.join(directory, DATABASE), "/data/local/tmp/seed-history.db")
    adb("shell", "run-as", APP, "sh", "-c", f"'cp /data/local/tmp/seed-history.db databases/{DATABASE} && rm -f databases/{DATABASE}-wal databases/{DATABASE}-shm'")
    adb("shell", "rm", "-f", "/data/local/tmp/seed-history.db")
    adb("shell", "monkey", "-p", APP, "-c", "android.intent.category.LAUNCHER", "1")


def first_real_hour(connection, default_ms):
    row = connection.execute("SELECT MIN(hourMs) FROM reading_hour").fetchone()
    return row[0] if row and row[0] is not None else default_ms


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--days", type=int, default=365, help="how far back to fill, at most (default 365)")
    parser.add_argument("--dry-run", action="store_true", help="say what would be written and change nothing")
    parser.add_argument("--remove-before", type=int, metavar="MS", help="take out the hours older than this moment (milliseconds), which is what filling printed")
    parser.add_argument("--seed", type=int, default=2026, help="the same number gives the same past")
    args = parser.parse_args()

    directory = tempfile.mkdtemp(prefix="seed-history-")
    try:
        pull_database(directory, stop_app=not args.dry_run)
        connection = sqlite3.connect(os.path.join(directory, DATABASE))
        if args.remove_before is not None:
            count = connection.execute("SELECT COUNT(*) FROM reading_hour WHERE hourMs < ?", (args.remove_before,)).fetchone()[0]
            print(f"{count} hours older than {args.remove_before} {'would be' if args.dry_run else 'are'} taken out.")
            if args.dry_run:
                return
            connection.execute("DELETE FROM reading_hour WHERE hourMs < ?", (args.remove_before,))
        else:
            now_ms = int(time.time() * 1000) // HOUR_MS * HOUR_MS
            cutoff = first_real_hour(connection, now_ms)
            start = max(now_ms - args.days * 24 * HOUR_MS, 0) // HOUR_MS * HOUR_MS
            if start >= cutoff:
                sys.exit("The history already goes back that far: nothing to fill.")
            rows = [row for row in generate(start, cutoff, args.seed)]
            print(f"The real history starts at {datetime.fromtimestamp(cutoff / 1000, tz=LOCAL):%Y-%m-%d %H:%M}.")
            print(f"Filling {len(rows) // 3} hours of each of 3 sensors, from {datetime.fromtimestamp(start / 1000, tz=LOCAL):%Y-%m-%d} up to there.")
            if args.dry_run:
                return
            connection.executemany("INSERT OR IGNORE INTO reading_hour (entityId, hourMs, lowest, highest) VALUES (?, ?, ?, ?)", rows)
            print(f"To take it out again: python3 tools/seed_history.py --remove-before {cutoff}")
        connection.commit()
        connection.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        connection.close()
        push_database(directory)
        print("Done. The app is open again.")
    finally:
        shutil.rmtree(directory, ignore_errors=True)


if __name__ == "__main__":
    main()

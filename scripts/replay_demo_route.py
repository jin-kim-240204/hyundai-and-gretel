#!/usr/bin/env python3
"""Inject the fixed synthetic eastbound demo route into one Pleos emulator."""
import argparse
import math
import shutil
import subprocess
import time


def coordinates(progress):
    return 37.4979, 127.0276 + progress / (111195 * math.cos(math.radians(37.4979)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', default='emulator-5554')
    parser.add_argument('--adb', default=shutil.which('adb'))
    parser.add_argument('--start-meters', type=float, default=0)
    parser.add_argument('--seconds', type=int, default=60)
    args = parser.parse_args()
    if not args.adb or not math.isfinite(args.start_meters) or not 1 <= args.seconds <= 600:
        parser.error('Provide adb, finite start-meters and seconds in 1..600')
    assert coordinates(0) == (37.4979, 127.0276)
    assert coordinates(100)[1] > coordinates(0)[1]
    started = time.monotonic()
    try:
        for tick in range(args.seconds + 1):
            lat, lon = coordinates(args.start_meters + tick * 10)
            subprocess.run([args.adb, '-s', args.serial, 'emu', 'geo', 'fix', str(lon), str(lat)],
                           check=True, timeout=10, capture_output=True)
            time.sleep(max(0, started + tick + 1 - time.monotonic()))
    except KeyboardInterrupt:
        print('Synthetic route stopped.')


if __name__ == '__main__':
    main()

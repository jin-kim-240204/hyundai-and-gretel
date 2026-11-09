#!/usr/bin/env python3
"""Run the opt-in SDK/Room demo integration test with synthetic A/B GPS replay."""
import argparse
import queue
import shutil
import subprocess
import threading
import time
import uuid
from replay_demo_route import coordinates


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=shutil.which('adb'))
    parser.add_argument('--serial', default='emulator-5554')
    parser.add_argument('--user', default='10')
    args = parser.parse_args()
    if not args.adb:
        parser.error('adb is required')
    adb = [args.adb, '-s', args.serial]
    token = str(uuid.uuid4())
    messages = queue.Queue()
    logs = subprocess.Popen(adb + ['logcat', '-T', '1', '-v', 'brief', '-s', 'RoadNoteE2E:I', '*:S'],
                            stdout=subprocess.PIPE, text=True)
    run = subprocess.Popen(adb + ['shell', 'am', 'instrument', '--user', args.user, '-w',
                                 '-e', 'runToken', token, '-e', 'class',
                                 'com.jinkim.hyundaiAndGretel.DemoFlowTest',
                                 'com.jinkim.hyundaiAndGretel.test/androidx.test.runner.AndroidJUnitRunner'],
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    stop = threading.Event()
    route = None
    passed = False

    def read_logs():
        for line in logs.stdout:
            if token in line:
                messages.put(line.strip())

    def replay(start, signal):
        try:
            for tick in range(48):
                if signal.is_set():
                    return
                lat, lon = coordinates(start + tick * 10)
                subprocess.run(adb + ['emu', 'geo', 'fix', str(lon), str(lat)],
                               check=True, timeout=10, capture_output=True)
                signal.wait(1)
        except (subprocess.SubprocessError, OSError) as error:
            messages.put(error)

    threading.Thread(target=read_logs, daemon=True).start()
    started = time.monotonic()
    try:
        while run.poll() is None:
            if time.monotonic() - started > 140:
                raise TimeoutError('SDK integration timed out')
            try:
                message = messages.get(timeout=0.5)
            except queue.Empty:
                continue
            if isinstance(message, Exception):
                raise message
            print(message, flush=True)
            if 'A_READY' in message or 'B_READY ' in message:
                stop.set()
                if route:
                    route.join(12)
                    if route.is_alive():
                        raise TimeoutError('Previous route did not stop')
                stop = threading.Event()
                start = float(message.split('B_READY ')[1]) - 300 if 'B_READY ' in message else 0
                route = threading.Thread(target=replay, args=(start, stop), daemon=True)
                route.start()
            if 'FLOW_PASS' in message:
                passed = True
        output = run.communicate(timeout=10)[0]
        print(output)
        if 'OK (1 test)' not in output or not passed:
            raise SystemExit('FAIL: integration assertions did not pass')
    finally:
        stop.set()
        logs.terminate()
        if run.poll() is None:
            run.terminate()
        if route:
            route.join(12)


if __name__ == '__main__':
    main()

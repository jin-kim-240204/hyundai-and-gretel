#!/usr/bin/env python3
"""Print Pleos debug setup commands or explicitly run read-only diagnostics."""

import argparse
from pathlib import Path
import re
import shlex
import subprocess
import sys

PACKAGE = "com.jinkim.hyundaiAndGretel"
APK = "app/build/outputs/apk/debug/app-debug.apk"
PORT = "tcp:8088"


def diagnostics(adb, serial, run, emit=print):
    user = run([*adb, "shell", "am", "get-current-user"]).strip()
    if not re.fullmatch(r"[0-9]+", user):
        raise ValueError("현재 Android 사용자 ID를 읽을 수 없습니다")
    emit(f"현재 Android 사용자 {user}")

    packages = run([*adb, "shell", "pm", "list", "packages", "--user", user, PACKAGE])
    installed = f"package:{PACKAGE}" in packages.splitlines()
    if installed:
        appop = run([*adb, "shell", "cmd", "appops", "get", "--user", user, PACKAGE, "android:mock_location"])
        mock_allowed = bool(re.search(r"(?im)^\s*MOCK_LOCATION:\s*allow(?:[;\s]|$)", appop))
        emit(f"mock GPS {'PASS' if mock_allowed else 'FAIL'} (현재 사용자의 AppOp)")
    else:
        mock_allowed = False
        emit("mock GPS NOT_TESTED (현재 사용자에게 앱이 없음)")

    forwards = run([*adb, "forward", "--list"])
    forwarded = any(line.split() == [serial, PORT, PORT] for line in forwards.splitlines())
    emit(f"앱 설치 {'PASS' if installed else 'FAIL'} (APK 버전 미확인)")
    emit(f"호스트 브라우저 전달 {'PASS' if forwarded else 'FAIL'} ({PORT})")
    emit("기기 내부 /vehicle/state HTTP 및 위치·TTS·AUDIO 권한 동작 NOT_TESTED")
    return 0 if installed and mock_allowed and forwarded else 1


def run_read_only(argv):
    return subprocess.run(argv, check=True, capture_output=True, text=True, timeout=10).stdout


def main(argv=None, run=run_read_only, emit=print):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Pleos Emulator의 emulator-NNNN 식별자")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true", help="현재 사용자·설치·AppOp·forward 읽기")
    mode.add_argument("--setup-commands", action="store_true", help="설치·포트 설정 명령도 출력 (실행 안 함)")
    args = parser.parse_args(argv)
    if not re.fullmatch(r"emulator-[0-9]+", args.serial):
        parser.error("--serial은 emulator-NNNN 형식이어야 합니다")
    adb = ["adb", "-s", args.serial]
    if args.check:
        try:
            return diagnostics(adb, args.serial, run, emit)
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            emit(f"진단 실패 ({type(error).__name__}). 대상 연결과 SDK Platform Tools를 확인하세요.")
            return 1

    prefix = shlex.join(adb)
    emit(f'pleos_user=$({prefix} shell am get-current-user) &&')
    emit('  case "$pleos_user" in ""|*[!0-9]*) false ;; *) true ;; esac &&')
    emit(f'  {prefix} shell pm list packages --user "$pleos_user" {PACKAGE}')
    emit(f'{prefix} forward --list')
    if args.setup_commands:
        emit(f"로컬 debug APK {'있음' if Path(APK).is_file() else '없음'} · 아래는 수동 실행용 명령")
        emit("./gradlew :app:assembleDebug")
        emit(f'pleos_user=$({prefix} shell am get-current-user) &&')
        emit('  case "$pleos_user" in ""|*[!0-9]*) false ;; *) true ;; esac &&')
        emit(f'  {prefix} install -r --user "$pleos_user" {APK}')
        emit(f"{prefix} forward --no-rebind {PORT} {PORT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

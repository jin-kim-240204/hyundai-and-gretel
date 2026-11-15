#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 3 || ! $3 =~ ^[[:xdigit:]]{64}$ ]]; then
    echo "Usage: $0 <Android SDK build-tools dir> <APK> <approved signer SHA-256>" >&2
    exit 2
fi

build_tools=$1
apk=$2
expected_signer=$(printf '%s' "$3" | tr '[:upper:]' '[:lower:]')
signature=$("$build_tools/apksigner" verify --print-certs "$apk")
actual_signer=$(printf '%s\n' "$signature" | sed -n 's/^Signer #[0-9][0-9]* certificate SHA-256 digest: //p')
if [[ $actual_signer != "$expected_signer" ]]; then
    echo "APK signer does not match the approved release signer." >&2
    exit 1
fi

badging=$("$build_tools/aapt" dump badging "$apk")
if ! grep -q "^package: name='com.jinkim.hyundaiAndGretel' " <<< "$badging" ||
    grep -q '^application-debuggable' <<< "$badging"; then
    echo "Expected a non-debuggable hyundai-and-gretel APK." >&2
    exit 1
fi
LC_ALL=C shasum -a 256 "$apk"
wc -c < "$apk"

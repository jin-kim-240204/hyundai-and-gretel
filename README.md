# hyundai-and-gretel

Gleo로 도로 상황을 제보하고, 합성 경로에서 접근하는 운전자에게 도로 쪽지와 음성으로 알리는 Pleos Connect 앱입니다.

debug 시연에서 제보자 A의 Gleo 입력을 처리해 합성 위치에 Room으로 저장하고, 수신자 B가 R1의 같은 방향에서 접근하면 수신함에 한 번 추가합니다. 확인은 명시적인 화면 조작으로 기록하며, 새 수신이 저장되면 정책이 허용하는 범위에서 Pleos TTS로 안내합니다. 이 흐름은 한 설치본에서 A/B를 전환하는 로컬 시연입니다.

## 실행 및 테스트

- [시연 런북](docs/PLEOS-DEMO.md)에 Android Studio 실행 구성, 현재 사용자 설치·권한 확인, Virtual Console 포트 및 A/B 시연 순서를 정리했습니다.
- 개발에 사용한 환경은 Pleos Connect Emulator **v2.1.0**, Android 14/API 34, arm64, **2560 × 1440**, 160dpi입니다. 이번 통합 빌드의 기기 검증은 미실행입니다. [이전 환경 기록](docs/work/gleo-architecture-refactor.md)
- 비기기 검증은 `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease`, `python3 -m unittest discover -s scripts`, `python3 scripts/verify_harness.py --build`를 사용합니다. SDK·구성은 [SETUP](docs/SETUP.md)을 참고하세요.

## APK

- 시연용 debug 빌드 출력은 `app/build/outputs/apk/debug/app-debug.apk`입니다.
- 로컬 release 빌드 출력은 `app/build/outputs/apk/release/app-release-unsigned.apk`입니다. 제출용 서명 APK와 GitHub Release 링크는 아직 없습니다. 새 설치 기능 및 서명 상태는 [제출 준비 점검표](docs/RELEASE-READINESS.md)에 기록했습니다. [공식 공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)

저장소 작업 규칙은 [AGENTS.md](AGENTS.md), 이번 변경과 검증 근거는 [통합 작업 기록](docs/work/integrated-hardening.md)을 참고하세요.

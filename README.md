# hyundai-and-gretel

Gleo로 도로 상황을 제보하고, 합성 시연 영역에서 제보 위치 가까이에 있는 운전자에게 도로 쪽지와 음성으로 알리는 Pleos Connect 앱입니다.

debug 시연에서 제보자 A의 Gleo 입력을 처리해 합성 위치에 Room으로 저장하고, 수신자 B가 제보 위치의 직선거리 500m 이내에 있으면 수신함에 한 번 추가합니다. 확인은 명시적인 화면 조작으로 기록하며, 새 수신이 저장되면 정책이 허용하는 범위에서 Pleos TTS로 안내합니다. 이 흐름은 한 설치본에서 A/B를 전환하는 로컬 시연입니다. 도로·진행 방향·고도는 수신 조건에 포함되지 않아 인접 도로, 반대편 차로, 고가도로 아래에서도 반경 안이면 수신할 수 있습니다.

## 실행 및 테스트

1. Android Studio에서 프로젝트를 열고 JDK 21, Android SDK Platform 35를 설정한 뒤 Gradle을 동기화합니다. 프로젝트의 `app` 모듈과 `debug` 빌드 변형을 선택합니다. [SDK 및 프로젝트 설정](docs/SETUP.md)
2. 기존 개발 환경은 Pleos Connect Emulator **v2.1.0**, Android 14/API 34, arm64, **2560 × 1440**, 160dpi입니다. AVD는 Cold boot로 설정합니다. 앱과 Gleo를 현재 운전자 사용자에서 준비하고, Virtual Console 연결 및 위치·음성 권한을 확인한 다음 시연 런북의 A/B 순서로 재생합니다. [시연 런북](docs/PLEOS-DEMO.md)
3. 기기 없이 JVM 테스트와 빌드를 확인하려면 저장소 루트에서 다음 명령을 실행합니다.

   ```sh
   ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease :app:lintRelease
   python3 -m unittest discover -s scripts
   python3 scripts/verify_harness.py
   ```

## APK

| 용도 | 위치 |
|---|---|
| 로컬 debug 시연 | `app/build/outputs/apk/debug/app-debug.apk` |
| 로컬 release 컴파일 | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| 공모전 제출용 | 서명 및 동작 확인 대기. 공식 경로는 `apk/팀명_앱이름_release.apk` |

[APK 검증·패키징 절차](apk/README.md) · [출시 제한 및 남은 검증](docs/RELEASE-READINESS.md)

## 제출 전 확인

- 1차 필수 제출물은 공식 접수양식(기획서)입니다. 팀원 전원이 개별 동의 여부와 서명·날짜를 작성하고, 제출 메일에 팀원 전원을 참조합니다. 접수 마감은 2026년 11월 15일 23:59 KST입니다. [공식 지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission) · [접수 추가 공지](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000007)
- 시연 영상·GitHub·APK는 선택입니다. GitHub로 제출할 때는 소스와 실행 가능한 release APK를 제공하고 접수 메일에 저장소 주소와 최종 커밋을 적습니다. 비공개 저장소의 공개 기한과 소스·APK 압축 파일 링크 대안은 공식 안내를 확인하세요. [공식 지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)
- 공모전 참가팀의 Gleo App Schema는 별도 검토·승인 없이 사용할 수 있습니다. 이 빌드의 스키마 스캔과 기기 런타임은 후속 확인이 필요합니다. 2차 PT 시연 환경은 Pleos Connect 에뮬레이터입니다. [공모전 개발 범위 및 기술 문의 안내](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000008)

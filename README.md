# hyundai-and-gretel

**현대와 그레텔**은 운전자가 구덩이·낙하물 등 도로 위 위험을 Gleo에게 말로 남기면, 해당 위치에 가까워지는 운전자에게 쪽지와 음성으로 전하는 Pleos Connect 앱입니다. 헨젤과 그레텔이 길에 빵 부스러기를 남기듯, 먼저 지나간 운전자가 다음 운전자를 위한 쪽지를 남깁니다.

- **음성 제보**: Gleo에게 도로 상황을 말하면 내용과 위치를 쪽지로 저장합니다.
- **근처 쪽지 줍기**: 유효한 제보 위치의 500m 이내에 들어오면 쪽지를 한 번 수신하고, 차량 상태가 허용하면 음성으로 안내합니다.
- **받은 편지함**: 음성으로 들은 쪽지도 정차 후 화면에서 다시 확인할 수 있습니다.

현재는 한 에뮬레이터에서 제보자 A와 수신자 B를 전환하는 로컬 시연입니다. 웹 콘솔의 속도·기어로 가상 경로를 재생하며, 차량 간 서버 공유는 향후 개발 범위입니다. 수신은 도로·진행 방향·고도 구분 없이 직선거리로 판단합니다.

## 실행 및 테스트

1. Pleos Connect Emulator **v2.1.0**, Android 14/API 34, arm64, **2560 × 1440**, 160dpi 환경을 준비하고 Cold boot로 실행합니다. **앱을 설치하기 전에 Virtual Console(웹 콘솔)을 먼저 연결**합니다. 호스트 브라우저에서 `http://127.0.0.1:8088/`가 열리는지 확인하세요. 포트 전달이 없다면 아래 명령을 실행합니다. 이미 전달돼 있으면 생략합니다.

   ```sh
   ~/Library/Android/sdk/platform-tools/adb forward --no-rebind tcp:8088 tcp:8088
   ```

2. 저장소 루트에서 APK를 현재 운전자 사용자에 설치합니다. **설치 후 개발자가 터미널에서 mock 위치 주입을 허용**해야 합니다. 아래는 macOS 기본 SDK 경로이며, 다른 환경은 `ADB` 경로를 바꿉니다. 여러 기기가 연결돼 있다면 사용할 기기만 연결한 뒤 실행하세요.

   ```sh
   ADB="$HOME/Library/Android/sdk/platform-tools/adb"
   driver_user=$("$ADB" shell am get-current-user | tr -d '\r')
   "$ADB" install -r --user "$driver_user" apk/jinkim_hyundai-and-gretel_release.apk
   "$ADB" shell cmd appops set --user "$driver_user" com.jinkim.hyundaiAndGretel android:mock_location allow
   "$ADB" shell cmd appops get --user "$driver_user" com.jinkim.hyundaiAndGretel android:mock_location
   ```

   설치 결과 `Success`와 mock 위치 상태 `allow`를 확인합니다. 이 명령은 시연용 모의 위치 허용이며, 앱 실행 시 요청하는 일반 위치 권한도 별도로 허용해야 합니다. debug 앱과 서명이 달라 업데이트가 거부되면 기존 앱을 삭제한 뒤 설치해야 하며, 삭제 시 앱 데이터가 지워집니다.

3. 앱의 **Debug 패널**에서 제보자 A·수신자 B를 전환하며 시연합니다. 웹 콘솔 연결과 mock 위치 허용은 release 시연에도 필요합니다. [A/B 시연 순서 및 연결 진단](docs/PLEOS-DEMO.md)
4. Gleo가 반복해서 “할 수 없는 기능”이라고 답하거나 제보를 잡담·웹검색으로 처리하면 **에뮬레이터를 완전히 종료한 뒤 다시 켜고 재시도**하세요. 개발 중 cloud 잡담·웹검색 경로로 오분류되던 증상이 재시작 후 해결됐습니다. 재시작 후에는 웹 콘솔 연결도 다시 확인합니다. 같은 응답이 계속되면 스키마 로딩·액션 전달·Callback 로그를 확인하세요. 해당 응답만으로 오분류라고 단정할 수는 없습니다.
5. 소스에서 실행하려면 Android Studio에서 JDK 21, Android SDK Platform 35를 설정하고 Gradle 동기화 후 `app` 모듈의 `debug` 변형을 실행합니다. [SDK 및 프로젝트 설정](docs/SETUP.md) 기기 없이 JVM 테스트와 빌드를 확인하려면 저장소 루트에서 다음 명령을 실행합니다.

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
| 공모전 제출용 서명 APK | [`apk/jinkim_hyundai-and-gretel_release.apk`](apk/jinkim_hyundai-and-gretel_release.apk) |

[APK 검증·패키징 절차](apk/README.md) · [출시 제한 및 남은 검증](docs/RELEASE-READINESS.md)

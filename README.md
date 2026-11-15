# 현대와 그레텔 · hyundai-and-gretel

**현대와 그레텔**은 운전자가 구덩이·낙하물 등 도로 위 위험을 Gleo에게 말로 남기면, 해당 위치에 가까워지는 운전자에게 쪽지와 음성으로 전하는 Pleos Connect 앱입니다. 헨젤과 그레텔이 길에 빵 부스러기를 남기듯, 먼저 지나간 운전자가 다음 운전자를 위한 쪽지를 남깁니다.

- **음성 제보**: Gleo에게 도로 상황을 말하면 내용과 위치를 쪽지로 저장합니다.
- **근처 쪽지 줍기**: 유효한 제보 위치의 500m 이내에 들어오면 쪽지를 한 번 수신하고, 차량 상태가 허용하면 음성으로 안내합니다.
- **받은 편지함**: 음성으로 들은 쪽지도 정차 후 화면에서 다시 확인할 수 있습니다.

현재는 한 에뮬레이터에서 제보자 A와 수신자 B를 전환하는 로컬 시연입니다. 웹 콘솔의 속도·기어로 가상 경로를 재생하며, 차량 간 서버 공유는 향후 개발 범위입니다. 수신은 도로·진행 방향·고도 구분 없이 직선거리로 판단합니다.

## APK 다운로드

[공모전 시연용 release APK 다운로드](https://github.com/jin-kim-240204/hyundai-and-gretel/raw/refs/heads/main/apk/jinkim_hyundai-and-gretel_release.apk)

저장소 내 위치: `apk/jinkim_hyundai-and-gretel_release.apk`. 아래 시연은 이 APK를 기준으로 진행합니다. release에도 A/B 전환용 **Debug 패널**이 포함돼 있습니다.

## 테스트 환경

| 항목 | 환경 |
|---|---|
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 |
| OS / ABI | Android 14 · API 34 · arm64 |
| 화면 | 2560 × 1440 · 160dpi |
| 필수 준비 | Virtual Console 연결, 현재 운전자 사용자의 앱·Gleo, 위치 및 mock 위치 허용 |

## 설치 및 필수 설정

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


## 시연 방법

1. 웹 콘솔에서 속도를 **0km/h**, 기어를 **Park**로 설정합니다. 앱을 열고 위치 권한을 허용한 뒤 우하단 **Debug** 버튼을 누릅니다.
2. 새 시연을 시작하려면 **새 시연 초기화 → 초기화 확인**을 사용합니다. 초기화하면 기존 시연 쪽지·수신 기록이 삭제됩니다. 이어서 **웹콘솔 GPS → 제보자 A → 제보자 시작 위치 설정**을 선택하고 **시연 준비 완료**를 확인합니다.
3. 웹 콘솔에서 **Drive**, **36km/h**로 설정해 약 **35초** 이동한 뒤 Gleo에게 “도로에 구덩이가 있어. 제보해줘”라고 말합니다. 제보 저장 결과를 확인합니다.
4. 다시 **0km/h · Park**로 설정하고, Debug 패널에서 **수신자 B → 수신자 시작 위치 설정**을 선택합니다. **시연 준비 완료** 후 **Drive · 36km/h**로 이동하면 제보 위치의 **500m 이내**에서 쪽지를 수신합니다. 음성 안내가 허용되는 차량·오디오 상태에서는 안내도 재생됩니다.
5. 정차 후 앱 화면에서 쪽지를 읽고 **확인**을 누릅니다. 음성으로 들은 쪽지도 화면에서 확인하기 전까지 읽지 않은 상태로 남습니다.

## 실행 중 문제가 생기면

- **Gleo가 “할 수 없는 기능”이라고 하거나 제보 대신 잡담·웹검색을 하는 경우**: 에뮬레이터를 완전히 종료한 뒤 다시 켜고 재시도하세요. 개발 환경에서 cloud 경로 오분류가 재시작 후 해결된 사례가 있습니다. 웹 콘솔 연결도 다시 확인합니다. 거절 응답의 원인이 항상 오분류인 것은 아니며, 계속되면 Gleo 스키마 로딩과 액션·Callback 전달을 확인해야 합니다.
- **mock 위치 미허용 / 웹 콘솔 연결 오류**: 위 설치·권한 설정을 다시 확인하세요. 일반 위치 권한과 mock 위치 허용은 서로 다른 설정입니다.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**: 기존 debug 앱과 release APK의 서명이 다르면 기존 앱을 삭제한 뒤 설치합니다. 삭제하면 앱에 저장된 시연 데이터도 지워집니다.

## 소스 빌드

Android Studio에서 **JDK 21**, **Android SDK Platform 35**로 Gradle을 동기화한 뒤 `app` 모듈의 `debug` 변형을 실행합니다. [상세 개발 환경 설정](docs/SETUP.md)

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease :app:lintRelease
python3 -m unittest discover -s scripts
python3 scripts/verify_harness.py
```

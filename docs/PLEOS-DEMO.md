# Pleos 도로 쪽지 debug 시연

## 실행 환경

기존 개발 환경은 Pleos Connect Emulator v2.1.0 / API 34 / arm64 / 2560 × 1440 / 160dpi이며, TTS 확인에 사용한 CAAS 버전은 4.2.10입니다. AVD의 Boot option은 Cold boot로 설정합니다. 이번 통합 작업의 확인 범위는 비기기 검사입니다. [기존 TTS 환경](work/gleo-tts-implementation.md) · [공식 Emulator 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)

Android Studio에서 프로젝트를 동기화하고 Build Variants의 `app`을 `debug`로 선택합니다. 공유 구성 **app Pleos**와 의도한 Pleos AVD를 선택해 기본 Activity를 실행할 수 있습니다. Studio 설치 후 현재 운전자 사용자에서 앱이 조회되는지 아래 진단으로 확인하세요. [Android Studio 실행 구성](https://developer.android.com/studio/run/rundebugconfig)

## 설치와 준비 진단

저장소 루트에서 `emulator-5554`를 실제 시연 AVD의 serial로 바꿉니다.

```sh
# 기본 dry-run. 명령 출력만 수행
python3 scripts/pleos_demo_setup.py --serial emulator-5554

# 빌드·현재 사용자 설치·포트 전달 명령을 출력하고, 실행은 운영자가 선택
python3 scripts/pleos_demo_setup.py --serial emulator-5554 --setup-commands

# 연결된 시연 AVD의 현재 사용자·설치 여부·mock AppOp·포트 전달을 읽기 전용으로 진단
python3 scripts/pleos_demo_setup.py --serial emulator-5554 --check
```

- `앱 설치 FAIL`이면 출력된 설치 명령으로 새 debug APK를 현재 사용자에 설치합니다. `앱 설치 PASS`는 패키지 존재 여부이며 현재 빌드와 버전이 같은지는 설치 경로로 확인합니다.
- `mock GPS FAIL`이면 시연 AVD의 현재 운전자 사용자에서 개발자 옵션의 모의 위치 앱으로 이 앱을 지정할 수 있는지 확인합니다. 해당 메뉴를 쓸 수 없는 환경에서는 운영자가 시연 권한 정책에 따라 현재 사용자의 `android:mock_location` AppOp를 설정하고 재진단합니다. 앱의 일반 위치 권한도 확인합니다. TTS에는 `pleos.car.permission.TTS_SERVICE`와 Speech Service, AUDIO 정책에는 `android.car.permission.READ_CAR_POWER_POLICY` 및 정책 연결이 필요합니다.
- 호스트 브라우저에서 `http://127.0.0.1:8088/`를 사용합니다. 호스트 전달이 없으면 출력된 `forward --no-rebind`를 수동 실행하세요. 포트 충돌이 있으면 어떤 세션이 사용 중인지 확인합니다. [공식 Virtual Console 셋업](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator) · [Android adb 포트 전달](https://developer.android.com/tools/adb)
- 앱은 AVD **내부** `http://127.0.0.1:8088/vehicle/state`에서 HTTP 200과 `controls.speed_kph`, `gear.actual.value`를 읽습니다. 브라우저 전달 목록은 이 응답의 증거가 아닙니다. 앱에 콘솔 연결 오류가 표시되면 Pleos 이미지의 Console 지원·부팅 상태와 호스트에서 전달된 같은 경로의 응답을 점검합니다. 속도는 0~240km/h, 기어는 Park·Neutral·Drive를 사용합니다.

## 시연 순서

1. 주차 상태에서 **시연 도구 → 새 시연 초기화**를 확인합니다. 새 설치에 필요한 단계입니다. 기존 시연에서 다시 실행하면 해당 시연의 쪽지와 수신 이력이 삭제되므로 필요한 경우에만 앱에서 직접 진행합니다.
2. **웹콘솔 GPS → 제보자 A → 제보자 시작 위치 설정**을 선택합니다. `READY`와 `실제 SDK 출발점 확인`을 확인한 뒤 D에서 합성 위치를 진행시킵니다. Gleo 스키마 등록이 반영된 환경에서 도로 상황을 한 문장으로 제보하고 저장 결과를 확인합니다. 이전 SDK 샘플이 제보 위치 기준으로 쓰이므로 JVM 재현은 36km/h로 35초 진행한 뒤 제보하고, B가 같은 속도로 진행해 수신하는 조건을 사용합니다. 실제 Gleo 입력 지연은 시연에서 별도 확인합니다. [제보와 수신 TTS 계약](work/gleo-tts-implementation.md)
3. 주차 상태로 돌아와 **수신자 B → 수신자 시작 위치 설정**을 선택하고 `READY`를 확인합니다. B는 A 쪽지보다 진행 방향 기준 600m 뒤에서 출발해 같은 방향으로 접근합니다. D로 진행해 500m 수신 조건에 들어오면 수신함 항목 한 건과 정책이 허용하는 음성 안내를 확인합니다.
4. 화면 정책이 허용할 때 쪽지 전체를 보고 확인 버튼을 누릅니다. 음성 재생과 확인은 별도이며, 재진입 시 같은 쪽지가 다시 수신되거나 재생되는지 확인합니다.

Gleo가 입력을 받지 못하면 debug APK 여부, 현재 사용자의 Gleo와 앱 설치, 스키마 등록, Callback 도착을 점검합니다. 음성·기기 UI·권한 동작의 실제 결과는 시연 시 별도로 기록합니다.

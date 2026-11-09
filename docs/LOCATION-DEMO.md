# 위치 쪽지 시연

환경: Pleos Connect2.1.0 arm64, Android14, 사용자10. SDK1.1.3, H3 res9. 한 에뮬레이터에서 가상사용자A/B가 같은 Room을 공유한다. 실제 위치를 쓰지 않는 합성시연이다.

## 빌드·설치

`./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug`

`adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`

`adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

`adb -s emulator-5554 shell am start --user 10 -n com.jinkim.hyundaiAndGretel/.MainActivity`

Android SDK의platform-tools를PATH에두거나아래스크립트의`--adb`로adb절대경로를지정한다. Gradle은이환경에서JDK21을사용한다. 기존앱ID com.jinnayeong.wakeup의데이터는새ID로이전하지않는다.

## 수동 발표

1. 디버그 패널에서 초기화. 합성 위치 사용 동의 후 시연 시작, 위치 권한 허용. 사용자 A를 선택한다.
2. `adb -s emulator-5554 forward tcp:8088 tcp:8088` 후 Pleos Virtual Console에서 주행 속도/기어를 조작한다. 웹콘솔은 GPS 경로 재생과 별도다.
3. `python3 scripts/replay_demo_route.py --seconds 90`으로 합성 동쪽 경로를 주입한다. 버퍼30초 준비를 확인한다. 기본 속도는10m/s다.
4. 약60초에 ‘구덩이를 발견했어. 제보해줘’로 Gleo를 호출한다. 또는 패널의 ‘구덩이 테스트 제보’를 눌러 저장 계층만 시연한다. 두 경로는 같은 report 로직을 쓴다. 저장 후 ‘쪽지를 남겼어요’가 표시되고, 도식에 현재 위치와 과거 위치 쪽지를 구분한다.
5. 디버그 패널의 심어진 쪽지 위치/도식 또는 Database Inspector로 progressMeters를 확인한다. 실제 JSON 수신 시각 기준이라 anchor가 정확히300m라고 가정하지 않는다.
6. ADB 재생을 Ctrl+C로 끝내고 B로 전환한다. 예를 들어 anchor가300m라면 `python3 scripts/replay_demo_route.py --start-meters 0 --seconds 45`를 실행한다. B는 anchor보다300m 뒤에서 출발한다.
7. 처음에는 수신 없음 → H3 주변 쪽지 조회·proximity 등록 → 접근 ENTER → 쪽지 표시. 반경 안을 계속 지나도 수신행은1개이고 원본쪽지는 남는다. callback/ENTER 지연 때문에 정확히150m경계에서 표시된다고 주장하지 않는다.
8. 화면 유지 스위치로 FLAG_KEEP_SCREEN_ON을 켜고 끌 수 있다. 주행 상태 변화로 앱 화면을 바꾸지 않는다. 앱이 완전히 가려지면 위치 수집/버퍼/등록을 정리하므로 복귀 후 다시 시작한다. 정차 자체는 제보/수신 금지 조건이 아니다.

## 자동 통합 검증

`python3 scripts/verify_location_demo.py --user 10`

실제 SDK에 A/B GPS를 순차 주입한다. 테스트는 이 앱의 합성시연 데이터를 초기화한다. A버퍼→과거anchor Room저장→B전환→SDK등록→수신행1개를assert한다. 음성인식/웹콘솔제어 검증을 대신하지 않는다.

Room·H3 계측만 실행: `adb shell am instrument --user 10 -w -e class com.jinkim.hyundaiAndGretel.RoomNotesTest com.jinkim.hyundaiAndGretel.test/androidx.test.runner.AndroidJUnitRunner`

`ProximityTest`는 별도 실행 후 RoadNoteTest의 READY 신호를 보고 경도127.0276, 위도37.4940→37.4979→37.4940을 각각 몇초간 주입하는 opt-in 시험이다. 일반connectedAndroidTest 전체실행은 이 외부주입대기를 포함하므로 사용하지 않는다.

## 로그

`adb logcat -s RoadNotes:I RoadNoteE2E:I RoadNoteTest:I '*:S'`

RoadNotes: PROXIMITY_REGISTERED / PROXIMITY_ENTER / PROXIMITY_EXIT / NOTE_SAVED / NOTE_RECEIVED / GLEO_CALLBACK_SENT / GLEO_CALLBACK_SEND_FAILED. 기본로그는위경도·발화원문을출력하지않는다. 화면의좌표는합성시연진단용이다. 실제음성발화/외부Gleo수신은별도확인해야한다.

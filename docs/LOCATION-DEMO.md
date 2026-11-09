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
4. 약60초에 패널의 ‘구덩이 테스트 제보’를 눌러 쪽지를 저장한다. 저장 후 ‘쪽지를 남겼어요’가 표시되고, 도식에 현재 위치와 과거 위치 쪽지를 구분한다. 이번 브랜치에는 음성 제보 수신 기능이 없다.
5. 디버그 패널의 심어진 쪽지 위치/도식 또는 Database Inspector로 progressMeters를 확인한다. 테스트 제보 처리 시작 시각 기준이라 anchor가 정확히300m라고 가정하지 않는다.
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

RoadNotes: PROXIMITY_REGISTERED / PROXIMITY_ENTER / PROXIMITY_EXIT / NOTE_SAVED / NOTE_RECEIVED. 기본로그는위경도·발화원문을출력하지않는다. 화면의좌표는합성시연진단용이다. 음성 연동은 기존 Gleo PR의 별도 작업이다. 이 브랜치에서는 디버그 제보→위치 저장→수신을 검증한다.

## 패널에서 시작 위치를 옮기는 시연 (권장)

debug 앱에 한 번 모의 위치 권한을 설정한다:

`adb -s emulator-5554 shell appops set --user 10 com.jinkim.hyundaiAndGretel android:mock_location allow`

1. 앱에서 합성 위치 동의·시연 시작 후 디버그 패널을 연다.
2. **A 시작 위치로 초기화**: 기준점0m로 실제 테스트 GPS를 주입한다. 버퍼·이전 SDK 등록을 비우고 새로운 SDK 위치를 기다린다.
3. 이 모드에서는 앱이 같은 에뮬레이터의 Pleos 웹콘솔 속도를 읽어 GPS를 이동시킨다. D에서 액셀을 밟으면 전진, P/N이면 정지, R이면 후진한다. 노트북 GPS 재생 스크립트를 동시에 실행하지 않는다.
4. A가 약60초 주행한 뒤 제보한다. **B 시작 위치로 초기화 · 쪽지600m뒤**를 누르면 최신미만료쪽지의600m뒤로이동한다. 쪽지가없으면기준점의600m뒤다. 시속36km에서는수신반경150m까지약45초걸린다.
5. **외부 GPS 사용 · ADB 경로 재생**을 누르면 테스트 provider를 제거하고 기존ADB입력으로복귀한다. 시연중지·앱비가시도provider를정리한다. 프로세스강제종료로정리가안됐으면앱다시실행시marker를통해정리한다.

시작 위치 초기화는 쪽지/수신이력을 삭제하지 않는다. B가이미받은쪽지는다시새수신되지않는다. 전체재시연은테스트데이터초기화후A제보부터진행한다. 파란점은현재차량,빨간점은전체로컬시연쪽지이며같은거리축에표시한다. 쪽지는차량이멀어져도H3후보조회와독립적으로표시한다. 점이동일좌표여도상하로나눠가리지않는다. 실제지도는아닌직선경로도식이다.

에뮬레이터전용권한설정과mockGPS코드/localhost통신은debug에만있다. 설정해제는 `adb shell appops set --user 10 com.jinkim.hyundaiAndGretel android:mock_location default`로한다. Android에뮬레이터도구의GPS와앱mockprovider를동시에쓰면동작이혼란스러우므로입력모드를전환해서사용한다.

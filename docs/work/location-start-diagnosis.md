# 위치 시작 무반응 및 Gleo 스키마 로그 진단

2026-11-14. 앱 삭제/재실행 후 시작 버튼 무반응. 기존 TTS 작업 변경은 보존하고 앱 코드는 수정하지 않았다.

- PASS: emulator-5554/user10, 앱 PID26146. RoadNoteService 바인딩 존재, 시작 호출 1회이나 현재 추적 중지 상태. 실제 UI 시작 버튼 클릭 재현.
- PASS: debug DemoRuntime.requestStart는 활성 시연 ID가 null이면 위치 수집을 시작하지 않고 “먼저 새 시연을 초기화해주세요” 메시지를 설정한다. 시연 도구를 열어 같은 메시지를 실제 화면 계층에서 확인했다. 일반 화면에서는 DemoState.message가 표시되지 않아 무반응으로 보인다.
- PASS: 설치 APK를 adb pull 후 assets/schema.json을 파싱했다. 파일은 비어 있지 않으며 JSON 파싱 성공, packageName 일치. 로그의 빈 JSON 입력이 설치 APK 파일 자체가 비어서 생긴 오류라고 판단할 근거는 없다.
- NOT_TESTED: Gleo 내부 setAppCapability에 빈 문자열이 전달된 정확한 경로/대상 앱. 공급자 스캔/캐시 오류나 다른 앱 데이터 가능성은 확정하지 않는다. 앱 시작 메시지의 원인과 구분한다.
- 안내: 시연 도구 → 새 시연 초기화 → 초기화 확인 → 제보자 시작 위치 설정. 외부 ADB 모드는 기존 실행 문서의 위치 입력이 추가로 필요하다. 초기화는 현재 시연 데이터를 삭제하므로 진단 과정에서는 실행하지 않았다.
- N/A: 소스 수정/자동 초기화/데이터 삭제/권한 강제 부여/빌드/커밋. 시연 도구만 열어 두었다. 실제 초기화 이후 SDK 위치 수집은 아직 검증하지 않았다.

## 후속: 초기화 이후에도 시작 실패

- 실제 재확인: PID23094, 시연 `제보자 A · WEB_CONSOLE · ERROR`, 메시지 “mock 위치 권한이 없습니다”. 앞선 초기화 안내만으로 다음 단계의 권한까지 해결되지 않았다.
- PASS: `adb -s emulator-5554 shell am get-current-user` → 10. `cmd appops get --user 10 com.jinkim.hyundaiAndGretel android:mock_location` → No operations / Default mode: deny.
- 조치: 해당 에뮬레이터/user10의 시연용 mock GPS AppOp만 allow로 복구했다. 일반 위치 권한·다른 앱·실차는 변경하지 않고, 시연 데이터 초기화/삭제도 실행하지 않았다.
- PASS: `cmd appops set --user 10 com.jinkim.hyundaiAndGretel android:mock_location allow` 후 get에서 MOCK_LOCATION: allow 확인. 기존 UI의 제보자 시작 위치 설정을 재시도했다.
- PASS: 실제 화면 `제보자 A · WEB_CONSOLE · READY`, “실제 SDK 출발점 확인”. RoadLocation 로그 session16 BUFFER_APPEND size10→21, 약1초 간격 증가 확인(좌표는 기록하지 않음). 즉 버튼 반응만이 아니라 실제 SDK 수신과 버퍼 저장을 확인했다. 현재 수집은 사용자가 이어서 테스트할 수 있도록 유지했다.
- README에 재설치 후 현재 사용자와 mock AppOp 재설정 절차를 추가했다. 소스 변경이 없어 빌드/단위 테스트는 N/A. TTS/Gleo 전체 E2E는 이 진단에서 NOT_TESTED다.

## 세 ADB 명령의 역할과 인과 구분

- 현재 운전자 user10, MOCK_LOCATION allow, Activity resolve 성공, adb forward 목록 emulator-5554 tcp:8088→tcp:8088, 기기 내부 0.0.0.0:8088 LISTEN 확인.
- install-existing는 기기에 존재하는 APK를 해당 사용자에게 설치 상태로 등록한다. 최신 소스 빌드/설치와 다르므로 새 코드 반영은 assembleDebug 후 adb install -r --user <현재 사용자>가 필요하다.
- am start --user는 Activity 실행 사용자를 맞춘다. 현재 기기에서는10이나 코드/스크립트는 am get-current-user로 조회하는 것을 권장한다.
- adb forward는 호스트→기기 방향이다(https://developer.android.com/tools/adb). 앱 AndroidDemoInputSource.readMotion의 주소는 기기 내부127.0.0.1:8088이므로 해당 호출은 호스트 forward를 통과하지 않는다. 앱→Mac 경로를 만드는 명령이라고 설명하지 않는다.
- WEB_CONSOLE 시작은 mock권한/웹콘솔 readMotion 성공 후에야 host.startTracking에 도달한다. 포트전달은 Mac 브라우저의 웹콘솔 조작용이며, 세 명령 모두가 위치 SDK의 필수 초기화라고 해석할 근거는 없다. forward를 제거하여 비교하는 실험은 사용자의 현재 웹콘솔을 방해하므로 미실행했다.
- 해결 안내 범위: 현재 사용자로 명시적 빌드APK 설치/실행, mock AppOp, 웹콘솔 사용시 forward를 개발 준비 단계로 묶는다. 근본 구조 개선은 일반 SDK 수집 모드와 시연 게이트 분리 및 시작 실패 UI 노출이며 이 설명 요청에서 소스는 수정하지 않는다.

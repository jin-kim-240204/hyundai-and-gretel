# 나영 리뷰 기반 화면·수신 흐름 정리

## 요청과 보존 범위
- `// 나영 리뷰:`와 이어지는 피드백을 읽고 반영된 요청은 주석을 제거한다. 원문은 작업 전 `/private/tmp/road-note-user-review-before.json`에 보관한다.
- 기존 사용자 polling·별도 작업 기록은 보존한다. 이번 작업은 커밋·푸시를 아직 요청받지 않았다.
- 기존 SDK 등록/해제, 세션 늦은 콜백 차단, 후보400m/수신150m/등록16개, 만료·방향·자기 제외, 명시ACK·쓰기 수명, CarUx 제한과 release 시연 제외를 유지한다.

## 반영 계획
1. Activity에는 화면 진입만 남기고 Route에서 상태 구독·화면 연결. 권한 요청 UI/수명과 수집 중 화면 유지 효과는 가까운 작은 Compose 단위로 분리.
2. 사용자 피드백에 따라 진입 시 권한이 없으면 권한 안내 화면, 있으면 제품 화면. 권한 허용 자체는 자동 수집 시작이나 DB쓰기의 근거가 아니다. 기존 명시 시작/CarUx 허가를 유지.
3. 제품 화면을 수집 제어/제보 상태/수신 카드로 분리. 시연 패널은 제품 화면 매개변수에서 제거하고 Route의 빌드별 슬롯으로 분리.
4. 디버그 확장함수 제거, 동일 ViewModel/Runtime을 명시적으로 참조. 패널 열림·상세·초기화 확인·타이머를 해당 UI 수명 가까이 이동. A/B DB키를 보존하고 화면 이름은 제보자A/수신자B로 표시.
5. `submitTestNote`를 공통 `submitRoadNote` 진입점으로 명명. 시연 버튼의 고정 원문은 debug에만 유지. Gleo/JSON 연동·release 제보 정책 확장은 하지 않음.
6. 수신 관찰 시작/취소를 start/stop에서 명시하고 입력 갱신·등록 동기화·ENTER·이미 반경 안 보완·실제 수신 수락을 이름으로 구분. 새 범용 이벤트/계층을 만들지 않음.

## 적용 규칙과 검증
- 클린 코드7/12/15/17~20/23~25/28~32: 진입점·소유자·콜백 처리 시점·자원정리·사용자 수락 쓰기를 유지.
- DOC-9844e9eaad3c 위치 콜백, DOC-1ba06876919b SDK 초기화/해제: 실제 어댑터와 SDK 계약 변경 없음.
- Compose ActivityResult/lifecycle 공식 계약, 현재 Pleos CarUx 정책과 기존56dp·문자/항목 제한 유지. 디자인 시스템 준수나 실차 적합 판정하지 않음.
- 기능 JVM 회귀 추가/전체JVM, debug/release·계측APK 컴파일/lint/harness/release 정적 검증. 실제 UI/권한 팝업/SDK 수명은 별도 관찰 근거가 필요하다.

## 피드백별 반영 결과

| 피드백 | 반영 |
|---|---|
| Activity의 pendingStart/권한 텍스트/콜백이 산재함 | MainActivity는 18줄의 화면 진입만 담당. LocationPermissionGate가 권한 안내·런처·복귀 시 조회를 담당. 자동 시작 후속 요청을 없애므로 pendingStart 자체가 불필요해졌다. |
| 화면 진입 시 권한별 화면 분기 | 권한 미허용은 안내 화면, 허용이면 제품 화면. 권한 허용과 수집 시작을 분리했다. 규제 미연결/NO_SETUP에서 권한 설정 차단, 수집 중지 버튼은 유지한다. |
| KEEP_SCREEN_ON의 조건·onStart/onStop 중복 | KeepScreenOnWhileTracking이 수집 여부와 STARTED 이상 수명을 함께 구독하고 정리한다. Activity에서 중복 처리하지 않음. |
| Activity에서 상태/시연/props를 조합함 | RoadNotesRoute에서 상태를 구독해 제품 화면과 빌드별 DemoPanel을 연결한다. 제품 화면 인자는 11개에서 6개로 줄임. |
| 제품 화면의 조건/문구/수신 카드가 뒤섞임 | TrackingControls, SubmissionStatus, ReceivedNoteCard로 분리. 수신 확인 직전 최신 규제와 원래 방문 정본 검사 유지. |
| DemoPanel의 ViewModel 확장함수 혼란 | 확장함수3개 제거, 같은 뷰모델을 받은 패널에서 tools.role/mode/reset 명시 호출. 별도 ViewModel 인스턴스 생성 없음. |
| 디버그 상태/표시 예산의 의미 불분명 | 패널 열림, 상세 보기, 초기화 확인, 진단 타이머를 해당 UI 수명 가까이 배치. visibleDemoContent는 기존 규제의 표시 가능한 키만 계산하는 순수 함수로 추출. 제품 카드의 예산 우선 확보·중복 구성·깊이 제한 회귀 추가. 상세 보기/확인은 패널을 닫으면 정리. |
| 역할을 제보자/수신자로 읽고 싶음 | 화면 라벨을 제보자A/수신자B로 변경. DB와 시연 입력의 A/B 키는 유지한다. |
| 공통 ViewModel에 테스트 제보라는 이름 | 계약/연결/서비스/뷰모델/호출부를 submitRoadNote로 통일. 고정 테스트 원문은 debug 자원에만 두고 실제 제보 처리자를 호출한다. Gleo/JSON/release 수락 조건을 선행 구현하지 않았다. |
| 수신 시점·이벤트 흐름 추적 어려움 | 생성자 자동 관찰 제거. 서비스 startTracking→startNotePickup→pickup.start에서 감시 시작, stop과 실패에서 관찰 취소. refreshNearbyNotes→synchronizeRegistrations→receiveNotesAlreadyInside 순서와 onEnteredRadius/onRegistrationCompleted→tryReceiveNote 진입점 명명. |
| 수신함 표시와 새 수신 저장이 혼동됨 | NoteReceiptProcessor.observeRecipientInbox는 조회만 담당하고 위치 중지 후에도 유지한다. receive/confirm만 쓰기이며 명시ACK 계약은 변경하지 않는다. |

- 반영한 `나영 리뷰:`20개 및 이어진 피드백 문장을 제거했다. 원문은 작업 전 스냅샷에 있다. 과거 `리뷰용:` 계약 설명 전체를 삭제하라는 요청은 아니므로 유효한 설명은 유지했다.
- 호출처가 없는 옛 DebugNoteSubmission(debug/release)·DebugSubmissionMessage, 그 전용 테스트1개/미사용 문자열10개 제거. 현재 시연 버튼과 제보/수신 오류 처리는 유지.
- 서비스에서 후보/수신/접근 감시 생성 인자를 명명하여, 위치·저장소·시계·콜백의 연결이 보이게 함.
- 코드 지도 `docs/ROAD-NOTE-CODE-MAP.md`를 새 진입점·실제 이벤트별 흐름으로 갱신했다. 부모 시스템 정본·DB 스키마·의존성·Manifest는 변경하지 않음.

## 재검토와 검사 결과

- RED: 기존 ProximityPickup 소스에서 새 관찰 수명 회귀를 실행하여 시작 전에도 관찰하는 반례 FAIL 확인. 소스는 finally에서 원상 복원. `/private/tmp/road-note-review-pickup-red.log`.
- GREEN/PASS: 전체 JVM100, 실패0/오류0/skipped0. 기존16상한/ID지연/ENTER·이미 안/위치·후보 갱신/만료·방향·자기 제외/실패 고정/수락 쓰기·ACK 소유권 회귀 포함. 신규4개에서 관찰 start-stop/늦은 ENTER·표시 예산3종 검증. 미사용 UI 전용 테스트1개 제거로 기존97+신규4-제거1=100.
- PASS: assembleDebug, assembleRelease, assembleDebugAndroidTest(계측 컴파일만), lintDebug. `/private/tmp/road-note-review-refactor-verified.log`.
- PASS: git diff --check, verify_harness, artifact 검사기4개 및 debug/release Manifest/DO/FGS·release 시연 제외·schema v1 정적 검사.
- PASS: 사용자 PleosLocationSource polling 파일을 작업 직전 스냅샷과 비교하여 바이트 동일. 별도 docs/work/pr4-implementation-feedback.md와 원래 untracked 파일들은 수정/삭제/스테이지하지 않음.
- NOT_TESTED: 이번 수정본의 권한 팝업/거절·설정 복귀, Activity 재생성·화면 꺼짐, 실제 Pleos SDK/Room·주행 UI·실차. 이번 리팩터링 중 앱/Studio/adb/계측을 실행하거나 APK를 설치하지 않았다. 직전 시연 결과를 새 UI 코드의 PASS로 재사용하지 않는다.
- 재검토에서 실패 시 관찰을 종료하도록 추가 정리. 동기 등록 실패 직후 관찰을 새로 시작하지 않으며 같은 세션 자동 재시도 없음. 관련 회귀 및 전체 검사 재통과.
- 사용자 후속 요청으로 이번 변경만 선택해 한국어 refactor 커밋과 원격 푸시를 진행한다. 기존 polling·별도 작업 기록은 제외하며 PR 코멘트는 게시하지 않는다.

## 공식 근거

- Android Compose Activity Result: https://developer.android.com/develop/ui/compose/libraries#activity_result — rememberLauncherForActivityResult로 UI 수명에 맞게 요청을 등록하고 사용자 버튼에서만 실행한다. DOC-COMPOSE-ACTIVITY#사용자요청런처.
- Compose Side Effects: https://developer.android.com/develop/ui/compose/side-effects — DisposableEffect의 키와 onDispose로 수명 관찰·윈도 플래그를 정리한다. DOC-COMPOSE-EFFECT#수명정리.

# 길쪽지 코드 읽기

현재 구조는 한 app 모듈의 MVVM/서비스 연결을 유지한다. 실제 처리자는 `Submission`·`Processor`·`Service`·`Screen/ViewModel/Connection`, 계약은 `Store`·`Source`·`Contract`로 구분한다. `Room`·`Pleos`·`H3` 접두어는 특정 기술의 구체 구현이다. 새 인터페이스/계층은 추가하지 않았다.

## 먼저 열 파일

| 목적 | 파일 / 종류 | 책임 |
|---|---|---|
| 화면 시작 | [MainActivity](../app/src/main/java/com/jinkim/hyundaiAndGretel/MainActivity.kt), 실제 Android 진입점 | 길쪽지 Route를 화면에 연결 |
| 화면 연결 | [RoadNotesRoute](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/RoadNotesRoute.kt), 실제 Compose 진입점 | 상태 구독·권한 화면·제품 화면과 debug 패널 연결 |
| 권한 진입 | [LocationPermissionGate](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/LocationPermissionGate.kt), 권한용 UI | 미허용이면 안내/허용·설정 버튼, 허용 상태면 제품 화면. 허용 자체로 수집 자동 시작 없음 |
| 화면 유지 | [KeepScreenOnWhileTracking](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/KeepScreenOnWhileTracking.kt), 화면 수명 효과 | 수집 중이며 STARTED 이상일 때만 화면 켜짐 유지, 화면 이탈/해제 시 정리 |
| 제품 화면 | [RoadNotesScreen](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/RoadNotesScreen.kt), 실제 화면 함수 | 상태와 버튼/현재 수신 카드. 패널은 DemoPanel로 별도 |
| 화면 이벤트 | [RoadNotesViewModel](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/RoadNotesViewModel.kt), 실제 처리자 | Android 서비스 연결/명령·방문 정본 snapshot |
| 연결 교체 | [RoadNoteServiceConnection](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/RoadNoteServiceConnection.kt), 실제 처리자 | 현재 서비스만 관찰·이벤트 전달. SQL/SDK를 모름 |
| 화면 상태 | [RoadNotesUiState](../app/src/main/java/com/jinkim/hyundaiAndGretel/ui/roadnotes/RoadNotesUiState.kt), data class | 화면 값과 버튼 가능 여부. 서비스 연결 수명과 별도로 읽음 |
| 화면용 계약 | [RoadNoteServiceContract](../app/src/main/java/com/jinkim/hyundaiAndGretel/service/RoadNoteServiceContract.kt), interface | 화면이 읽는 Flow와 제보/확인 명령 |
| 앱 조립/수명 | [RoadNoteService](../app/src/main/java/com/jinkim/hyundaiAndGretel/service/RoadNoteService.kt), 실제 Service | 첫 onStartCommand부터 시작/중지→쓰기 대기→종료. 아래에서 Room/SDK/기능을 생성·연결 |
| 알림 내용 | [RoadNoteServiceNotification](../app/src/main/java/com/jinkim/hyundaiAndGretel/service/RoadNoteServiceNotification.kt), 구체 helper | 채널·문구·PendingIntent만 구성. foreground 전환은 서비스에 남음 |
| 종료 조건 | [RoadNoteServiceLifetime](../app/src/main/java/com/jinkim/hyundaiAndGretel/service/RoadNoteServiceLifetime.kt), 순수 함수 | shouldStopRoadNoteService/foregroundServiceType. 수집과 수락 쓰기를 함께 판단 |
| 제보 | [RoadNoteSubmission](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/submit/RoadNoteSubmission.kt), 실제 처리자 | 과거 표본 고정·쪽지 생성·저장 요청·제출 상태 |
| 주변 후보 | [NearbyNoteCandidates](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/pickup/NearbyNoteCandidates.kt), 실제 처리자 | 현재 위치의 주변 Flow와 세션/거리/TTL·신선도 소비 검사 |
| 접근 감시 | [ProximityPickup](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/pickup/ProximityPickup.kt), 실제 처리자 | 같은 PickupSession/ProximityRegistration에서 등록/해제·늦은 ID/ENTER/onError 차단·조건 검사 |
| 수신/확인 | [NoteReceiptProcessor](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/pickup/NoteReceiptProcessor.kt), 실제 처리자 | 수신·현재 recipient 큐·확인 수락/카운트·관찰 소유권 |

## 저장 계약과 구체 구현

| interface | 구현 class | 경계 |
|---|---|---|
| [RoadNoteStore](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/RoadNoteStore.kt) | [RoomRoadNoteStore](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/storage/RoomRoadNoteStore.kt) | 원본 create/findById. 성공/중복/잘못된 입력/저장 실패 구분 |
| [NearbyNoteStore](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/pickup/NearbyNoteStore.kt) | 같은 RoomRoadNoteStore | 주변 셀 원본 Flow. 시나리오 scope/Room invalidation, 현재 소비 검사는 처리자 |
| [ReceiptStore](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/pickup/ReceiptStore.kt) | [RoomReceiptStore](../app/src/main/java/com/jinkim/hyundaiAndGretel/notes/storage/RoomReceiptStore.kt) | 원본 재조회/조건 재검사·복합키 수신/트랜잭션·전체 JOIN/멱등 확인 |

DAO/Entity/SQL/DB 초기화는 `notes/storage`에 있다. 서비스의 생성 지점이 구현을 선택하고 기능 처리자는 Store 계약을 받는다. `location`은 LocationSource 계약·PleosLocationSource 구현·추적/버퍼, `notes/spatial`은 SpatialIndex 계약·H3SpatialIndex 구현/거리/지원 영역이다. `demo`의 DemoRuntime은 기존 서비스 포트(DemoServiceContract)와 입력 포트(DemoInputSource)를 사용하며 AndroidDemoInputSource은 debug 실제 어댑터다. 이 작은 기존 포트를 새 계층으로 감싸지 않았다.

## 두 경로의 읽기 순서

**제보**

`debug DemoPanel 버튼 → RoadNotesViewModel.submitRoadNote → RoadNoteServiceConnection → RoadNoteService.LocalBinder → RoadNoteSubmission.submit → LocationTracking.selectAnchor → RoadNoteStore.create → RoomRoadNoteStore.create → RoadNoteDao.insert`

UI/서비스는 가능 여부·수명을, Submission은 고정한 anchor/작성자/시나리오와 저장 결과를 소유한다. DAO까지 내려가야 SQL이 나온다. 저장 전에 성공하지 않고 취소 전파/수락 후 쓰기 수명·중복 의미는 그대로다.

**수신과 확인**

`PleosLocationSource callback → LocationTracking(세션/gate/버퍼/현재 위치) → NearbyNoteCandidates → NearbyNoteStore / Room Flow → ProximityPickup(ENTER 또는 이미 안) → NoteReceiptProcessor.receive → ReceiptStore.receive → RoomReceiptStore.withTransaction → ReceiptDao.insert → recipient 전체 JOIN → RoadNotesUiState/Screen`

이후 현재 명시 확인은 `ViewModel 방문 snapshot → Connection → Binder 최신 규제 confirmVisibleReceipt → NoteReceiptProcessor.confirm → ReceiptStore.confirm → Room UPDATE`다. callback 등록/해제/늦은 결과는 같은 PickupSession/ProximityRegistration에 유지했다. 원본은 수신/확인으로 삭제하지 않으며 실제 표시/음성 자동 ACK는 추가하지 않았다.

## 현재 DB: 새 개발 기준 v1

미출시이며 보존할 개발 DB가 없다는 사용자 결정으로 현재 세 테이블(`road_notes`, `note_receipts`, `demo_state`)·모든 필드/인덱스/복합키/FK를 하나의 새v1에 담았다. KSP가 [1.json](../app/schemas/com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase/1.json)을 생성한다. 과거 export2~4·MIGRATION_1_2/2_3/3_4·이전 DB 호환 테스트는 제거했다. 새 DB의 저장·수신·JOIN·reset/rollback과 H3 실패 테스트는 유지한다.

**기존 설치본은 다음 실행 전에 앱 데이터 전체 초기화가 필요하다.** 예전v4뿐 아니라 예전v1도 구조/identity가 다르다. 행 삭제나 ‘새 시연 초기화’ 버튼으로 스키마가 새로 만들어지지 않는다. DB 이름은 road-notes.db를 유지하며 자동 destructive fallback/자동 삭제는 넣지 않았다. 이 변경은 기존 DB에 대한 이관/다운그레이드 경로가 아니다. [Room 공식 migration 안내](https://developer.android.com/training/data-storage/room/migrating-db-versions)는 스키마 이력/이관 지원의 근거이며, 이번 초기화 전제는 사용자 개발 운영 결정이다.

앱/adb 실행 금지가 유지되어 실제 앱 데이터는 이번에 지우지 않았다. 나중에 실행이 허용되면 기기의 앱 설정에서 저장공간/앱 데이터를 초기화한 뒤 새 DB로 실행한다. 이번v1 이후 보존할 데이터가 생기면 그 기준부터 버전 증가와 migration을 설계한다. 과거 단계 문서의1~4 migration 검증 기록은 이력이지 현재 지원 범위가 아니다.

## 서버 전환 전에 결정할 것

1. 응답이 끊긴 제보: 같은 noteId/idempotency key 재시도·서버 저장 여부 조회·중복 응답 의미. 현재는 자동 재시도가 없고 Duplicate를 생성 성공으로 바꾸지 않는다.
2. 주변 목록 갱신: 셀/위치 변경 시 fetch, 주기 조회/실시간 push, 캐시·오프라인/실패·신선도. Room Flow를 서버 알림과 같다고 가정하지 않는다.
3. 사용자별 수신·표시 이력: 기기/계정 간 동기화 범위, 오프라인 확인·충돌/시각/원본 삭제 정책. 현재는 로컬 복합키/멱등 확인이며 서버 동기화를 제공하지 않는다.


## 이벤트가 실제 발생하는 지점

- `RoadNoteService.startTracking → startNotePickup → ProximityPickup.start`에서 수신 감시를 시작한다. `stopNotePickup → ProximityPickup.stop`에서 입력 관찰을 취소하고 등록을 정리한다. 객체 생성만으로 수신 감시가 시작되지는 않는다.
- 수신 감시의 입력은 **위치 변경 / 주변 후보 변경 / 수신함 이력 변경**이다. `observePickupInputs → refreshNearbyNotes`에서 현재 입력을 읽고, `synchronizeRegistrations`와 `receiveNotesAlreadyInside`를 순서대로 실행한다.
- 실제 SDK ENTER는 `onEnteredRadius`, 등록 완료는 `onRegistrationCompleted`로 들어온다. 두 경우와 이미 반경 안 보완은 모두 `tryReceiveNote` 한 판정을 거쳐 수신 저장을 요청한다. ENTER만 왔다고 저장 성공인 것은 아니다.
- `NoteReceiptProcessor.observeRecipientInbox`는 위치 수집이 멈춰도 기존 수신함을 **조회**한다. `receive`가 새 이력을 저장하고, `confirm`만 기존 이력의 명시 확인 시각을 저장한다. 조회/렌더링으로 이력을 만들거나 확인하지 않는다.
- 시연 버튼의 원문은 debug 패널에만 있다. 공통 제보 명령은 `submitRoadNote(content, category)`이며 아직 Gleo JSON 수신이나 릴리스 제보 정책을 구현한 것은 아니다.

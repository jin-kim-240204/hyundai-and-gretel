# 길쪽지 파일명·책임 배치 리팩터링

기준 HEAD f9c1f0d. 사용자는 기존 기능을 보존하며 처리자/계약·진입점·의존 방향이 이름과 패키지에 드러나도록 요청했다. pleos-connect ENV-02/VEH-03/DES-06, android-compose-architecture·clean-code-review 이름/책임·취소/자원·기존 상태 정본, ponytail 최소 변경 적용. DOC-1ba06876919b#초기화해제·DOC-9faba42791f0#기능별규제의 기존 승인 계약을 유지하며 새 SDK/API/권한/정책을 도입하지 않는다. SDK 초기화·수락 쓰기 수명·DO/CarUx·명시 ACK는 바꾸지 않는다.

실제 기존 경로:
1. DemoPanel 제보 버튼 → LocationViewModel.submitTestNote → LocationConnection의 현재 세션 검사 → LocalBinder(debug/Ready/전환 검사) → RoadNoteRepository.submit → Tracking.selectAnchor로 불변 anchor/문맥 선택 → RoadNoteStore.create → RoomRoadNoteStore의 trim/검증/H3 → DAO INSERT.
2. Pleos SDK callback → LocationTracking의 Main/세션·demo gate·버퍼/현재 위치 → NearbyCandidates의 Room Flow/현재 소비 → ProximityPickup의 같은 Run/Request 등록·ENTER/이미 안 검사 → ReceiptRepository.receive의 수락/수명 → RoomReceiptStore.withTransaction(원본/TTL/소속/거리·복합키/IGNORE) → recipient 전체 JOIN → 화면 한 건. 명시 확인은 VM 방문 snapshot→Binder 최신 규제→ReceiptRepository.confirm→Room UPDATE로 이어진다.

방향: UI roadnotes → 서비스 계약/서비스 연결 → submit·pickup 처리자 → Store 계약 → Room 구현. service는 구현 선택·SDK/기능 연결과 수명을 소유한다. location에는 SDK 입력·추적·버퍼, spatial에는 H3/거리·지원 영역, pickup에는 주변 원본 관찰/접근/수신을 둔다. 이름 규칙은 Store/Source/Contract가 계약, Room/Pleos/H3가 구체 어댑터, Submission/Processor/Service/Screen/ViewModel/Connection이 실제 실행 책임이다. 새 인터페이스/범용 계층은 만들지 않는다.

먼저 이동/명명만 수행하고 알림 생성은 별도 구체 도우미로 추출한다. 채널/알림 ID·Intent action 값·PendingIntent request code·순서/예외/취소 정책을 유지하고 Manifest/Intent/알림·debug/release/테스트/정적 검사 참조를 같이 수정한다. Room Entity/DAO/SQL·DB 버전/schema는 변경하지 않는다. Nearby/Receipt/Submission 결과의 성공/중복/실패·CE 전파/Flow·늦은 결과 소유권을 보존한다.

기존 dirty 목록·내용 hash는 /private/tmp/road-note-refactor-original-dirty.json, 이동 전 Kotlin 원문은 /private/tmp/road-note-refactor-kotlin-before.json에 보존했다. PleosLocationSource의 사용자 polling과 원래 문서/미추적 파일은 수정하지 않는다. 현재 요청은 리팩터링/검증이며 새 commit/push/PR 전송은 실행하지 않는다. 기존 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지는 유지하고 host JVM/Fake/빌드/lint/harness/정적 검사·계측 compile만 수행한다. 실제 서비스/알림/Room/SDK/UI는 NOT_TESTED다.


사용자 추가 결정: 미출시·보존할 개발 DB 없음 전제로 최신 전체 구조를 새v1으로 합친다. 초기의 ‘DB 버전/schema 불변’ 범위는 이 명시 지시로 대체한다. Entity 필드·테이블·인덱스·복합키/FK·쿼리는 그대로이고 version만1, 기존 export2~4/이관 상수·등록/호환 전용 테스트를 제거한다. 현재 v1 export는 KSP로 재생성했다. h3 생성 실패 검사는 일반 RoomRoadNoteStoreTest로 옮기고 reset/JOIN/rollback은 fresh-v1 RoomScenarioStoreTest로 유지한다. 새 Room schema entities가 기존v4와 정확히 같음을 정적으로 비교한다. 자동 destructive fallback/DB 파일명 변경은 하지 않으며 기존 설치본은 다음 실행 전 앱 데이터 전체 초기화가 필요하다. 실제 초기화/앱 실행은 수행하지 않았다.

현재 공통 정본은 부모 worktree에서 새v1 정책 초안을 먼저 편집했고 기능 worktree의 동일 파일에 같은 내용으로 반영했다. 둘은 byte 동일이며 이번에는 어느 쪽도 commit/push하지 않는다. 향후 커밋 시 정본은 부모에서 별도 commit/push→기능 병합하고 기능 커밋에 중복 stage하지 않아야 한다. 과거 단계 검증 문서는 당시 커밋의 이력으로 남기며 현재 README/코드 맵에 새v1 운영 기준을 명시했다.

파일 추가 이유: RoadNoteServiceNotification은 서비스 foreground 수명과 달리 바뀌는 알림 채널/표시/버튼 구성만 분리한다. RoadNotesUiState는 화면 shape/버튼 조건을 서비스 연결의 관찰 교체 코드와 따로 찾게 한다. Store/Source는 기존 계약을 유지하고 실제 ReceiptRepository는 NoteReceiptProcessor로 명명해 저장 adapter와 구분한다. ProximityPickup의 Run/Request와 등록/해제·늦은 콜백 검사는 같은 파일에 두었다.

초기 검사에서 moved test의 InvalidNote import와 H3 검사의 클래스 삽입 위치 누락이 compile FAIL → import/테스트 소속을 수정해 전체 빌드 PASS. 제품 로직 변경으로 해결하지 않았다. 스키마 비교 임시 스크립트의 생략된 views 키 KeyError는 기본 빈 배열로 수정하며 제품/Room 실패와 구분한다. 무관한 import 정렬만 생긴7파일은 원문으로 되돌려 diff를 제한했다.


최종 결과: PASS JVM95/실패0/오류0/skip0, Python Manifest4, assembleDebug/assembleRelease/assembleDebugAndroidTest(컴파일만)/lintDebug, verify_harness --build 및 산출물 검사. 새 service의 양 variant Manifest/Intent 대상·DO/FGS·release 제외·단일v1/복합키/FK·ABI 확인.7개 핵심 처리자/화면/계약 본문과11개 root 서비스 명령·수명 함수는 이름/import 및 알림 helper 호출 외 정규화 비교 동일, 기존 필드 초기화 순서 동일. 기존dirty24개 내용 hash 동일. 로그 /private/tmp/road-note-refactor-final.log·road-note-refactor-harness.log·road-note-refactor-artifacts-final.json. 함수 비교 임시 스크립트가 inner override를 잡은 문제는 root 메서드 정규식으로 바로잡았다.

NOT_TESTED: 실제 서비스 시작·중지/알림·SDK/Room 새 DB 생성/계측/UI/실제 앱 데이터 초기화. 기존 실행 금지를 유지했다. 버전1 schema entities/인덱스/FK는 이전 최신v4와 동일하고 views 생략은 빈 배열로 비교했다. DemoHost/DemoInput도 DemoServiceContract/DemoInputSource로, 구체 구현은 AndroidDemoInputSource로 맞췄다. 기능/읽음/취소·수신 정책은 바꾸지 않았다. commit/push는 요청되지 않아 수행하지 않았다.

후속 Activity does not exist 진단: assembleDebug 새 빌드 PASS. SDK apkanalyzer 정적 분석에서 APK package=com.jinnayeong.wakeup, LAUNCHER=com.jinkim.hyundaiAndGretel.MainActivity(enabled 기본true), DEX의 해당 class 정의를 직접 확인했다. .idea RunManager의 선택은 Android App.app, module=hyundai-and-gretel.app, deploy=true/default_activity로 확인했다. namespace와applicationId가 다른 것 자체를 결함으로 판단하지 않는다. 기기의 설치본/설치실패/선택 serial은 기존 adb·기기 실행 금지로 미조회이며 원인 확정 불가. 앱 ID/Manifest/코드를 추측 수정하거나 설치/삭제하지 않았다. 기존 설치본 삭제 뒤 전체 Run 배포 및 설치 로그 확인이 필요한 경계다.

### Activity 실행 오류 조회 진단 (2026-11-12)
- 사용자에게 조회용 adb만 추가 승인받아 실행. 앱 실행/설치/삭제/사용자 전환은 하지 않음.
- `am get-current-user`: 10.
- `dumpsys package com.jinnayeong.wakeup`: User 0 installed=true, User 10 installed=false.
- 동일 MainActivity 명시 조회: `--user 0` 정상 해석, `--user 10` 및 `--user current`는 No activity found.
- 진단 PASS: Activity 이름/패키지 오류가 아니라 현재 사용자 10에 설치되지 않은 상태. 사용자 10 설치 조치는 미실행.
- 추가 조회: `dumpsys user`에서 User 0은 `android.os.usertype.system.HEADLESS`, User 10은 `android.os.usertype.full.SECONDARY`(운전자). `ro.fw.mu.headless_system_user=true`, Visible users=[10], Boot user=10. 두 운전자 프로필이 아니라 화면 없는 시스템 사용자와 실제 운전자 사용자 구분임을 확인. 생성 주체/Studio가 사용자0에만 배포한 원인 자체는 미확인.
- 사용자의 양쪽 설치 요청에 따라 `cmd package install-existing --user 10 com.jinnayeong.wakeup` 실행: 성공. 이후 사용자10의 MainActivity 명시 resolve 성공. 앱 실행/삭제/데이터 초기화는 하지 않음. Studio 향후 Run 배포 경로 및 실제 실행은 미검증.

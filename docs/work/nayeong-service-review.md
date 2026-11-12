# 제품 ViewModel·서비스 클라이언트·시연 상태 분리

- 새 `나영 리뷰` 및 이어진 질문을 검토한다. 원문을 `/private/tmp/road-note-service-review-before.json`에 보관하고 처리 후 사용자 주석만 삭제한다.
- 현재 Route의 ViewModel 조회 변경, 코루틴 업데이트 TODO, 사용자 polling 및 별도 문서는 보존한다. 커밋·푸시는 아직 요청받지 않았다.
- 적용: clean-code12/15/17/24/25, MVVM 책임 분리와 SDK/수락 쓰기 수명 보존. 새 범용 인터페이스/DI 라이브러리/별도 서비스 바인딩을 추가하지 않는다.
- Android 서비스 연결/해제·Intent 명령·실패 상태를 RoadNoteServiceClient에 응집. 제품 ViewModel은 화면 상태와 이벤트만 연결한다.
- DemoViewModel을 debug에 두고 기존 서비스 연결을 공유한다. main ViewModel에 DemoRuntime/시연 버퍼 문자열을 두지 않는다. main 서비스의 입력·수락 쓰기 경계에서 필요한 기존 시연 연결점은 유지한다.
- 버퍼 진단은 typed snapshot으로 전달하고 debug XML에서 표시 문자열을 만든다. Binder는 화면과 서비스 연결용이며 A/B 생성용이 아님을 설명한다.
- A/B 역할 변경은 기존 SDK/입력 정리→버퍼 초기화→새 출발점 확인을 유지하며 Room 원본·수신 이력은 유지한다.
- JVM 회귀/양variant·계측APK compile/lint/harness/static 확인. 실제 UI/서비스/기기와 계측 실행은 이번 작업에서 수행하지 않는다.

## 반영 결과

- 제품 RoadNotesViewModel은 화면 상태/방문 snapshot과 이벤트 연결만 갖는다. Android bind/unbind·Intent·시작 실패 처리는 RoadNoteServiceClient에 옮겼다. close는 멱등이며 늦은 연결 콜백을 무시하고, 서비스의 수락 쓰기는 취소하지 않는다.
- debug DemoViewModel은 같은 연결의 서비스 상태를 빌려 역할·모드·reset을 전달한다. 제품 ViewModel은 DemoRuntime을 import하지 않는다. 입력 gate/accepted writes/SDK 정리와 관련된 기존 서비스 시연 연결점은 수명 보존을 위해 유지했다.
- 시작 가능 여부는 현재 서비스 계약에서 즉시 읽는다. 구독 결과 갱신 전 연결 교체도 회귀로 검증했다.
- LocationBufferSnapshot은 수량·단조시각만 반환한다. 버퍼 문구 및 패널/진단 고정 표시 문구는 debug strings.xml에서 구성한다.
- 처리한 나영 리뷰와 연속 질문 주석을 삭제했다. 기존 polling, 코루틴 TODO, Route ViewModel 변경과 별도 사용자 문서는 보존했다.

## 질문에 대한 답변

- DemoViewModel 분리는 가능하며 이번에 debug 전용으로 분리했다. 별도 서비스를 만들거나 이중 바인딩하지 않는다.
- 문자열을 ViewModel에 둔 예외는 필요하지 않았다. 타입 값과 XML 표시를 나눴다.
- Binder는 화면에서 같은 프로세스 서비스의 계약에 접근하는 통로다. A/B 프로필을 생성하지 않는다. 역할 변경은 기존 SDK/버퍼를 정리하므로 B→A→B의 과거 메모리 표본은 복원하지 않는다. Room 원본/수신 이력은 별도로 유지된다.
- ViewModel은 상태뿐 아니라 화면 이벤트의 진입점이다. 다만 Android 연결 방식은 Client에, SDK/수락 쓰기 유지와 종료 정책은 Service에 응집했다. ViewModel 종료는 화면 연결만 해제한다.
- UiState는 화면을 그리는 값과 값에서 계산되는 가능 여부다. ViewModel은 서비스 상태와 방문 상태를 조합해 UiState를 만들고 이벤트를 전달하는 처리자다.

## 검사 결과

- PASS: JVM 102개(실패/오류/skip 0). 새 회귀는 현재 연결의 시작 자격 즉시 조회와 A→B→A→B 버퍼 초기화/저장 원본·수신 이력 보존이다.
- PASS: assembleDebug, assembleRelease, assembleDebugAndroidTest(compile만), lintDebug. 최초 진단 문자열 식의 Pair/Elvis 추론 오류는 중간 bufferText 값으로 수정 후 재검사 통과.
- PASS: verify_harness 및 양 variant Manifest/스키마/release APK 정적 검사, diff 공백 검사. 사용자 polling은 작업 시작 원문과 바이트 비교 동일.
- NOT_TESTED: 실제 서비스 bind/unbind·화면 재생성·패널 UI·SDK/Room 기기 동작·계측 실행. Studio/앱/기기/adb는 실행하지 않았다. 자동 검사 통과를 실제 시연 완료로 보고하지 않는다.
- N/A: 신규 SDK·권한·DB 스키마·자동 ACK/Gleo 추가. 커밋/푸시는 이번 요청에 없어 수행하지 않았다.

- 공식 요구 추적: DOC-1ba06876919b#초기화해제는 기존 기록의 SDK 초기화/해제 요구를 계승한다. SDK 어댑터는 수정하지 않았으며 화면 연결 해제로 서비스 소유 자원을 조기 종료하지 않는다. 런타임은 NOT_TESTED. 보고서 최초 검사에서 원문별 항목 누락을 발견해 보완했다.

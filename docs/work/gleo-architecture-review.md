# Gleo 리팩터링 변경 리뷰

2026-11-11. 대상: `38338f0` 대비 미커밋 리팩터링 변경과 신규 파일. 구현 수정 없이 리뷰한다. 단일 Android 프로세스, 사용자 음성 제보와 최대 100건 로컬 초안 규모를 전제로 했다.

## 검토 범위와 규칙

- ENV-02: Automotive Activity/Compose, Hilt Application·Activity·Receiver 연결을 확인했다.
- INT-01: 기존 입력 3개 검증·Callback data 배열 문자열·5초 제한·수신/전송 분리를 변경 전 코드와 비교했다. 근거 원문은 기존 gleo-architecture-refactor.md의 DOC-24357cf5e59a / DOC-c6fc94252d66 / DOC-f1d260bfb503 기록을 재사용한다. 스키마 변경·새 API 판단 없음.
- PRIV-01 / DOC-72a5d7ab4ebb#최소노출: 원문 logcat 제거와 진단 메타데이터의 유용성을 별개로 검토했다. 원문을 다시 기록하는 수정은 제안하지 않는다.
- DOC-e68476b79d1c#생명주기: 새 DraftRoute의 ON_RESUME 갱신과 현재 계측/단위 테스트의 실제 검증 범위를 비교했다.
- 모든 변경 파일, 신규 모델·Repository·ViewModel·DI, 관련 호출자·테스트·README·활성 Sprint 계약 문서를 읽었다. 취소 전파·기존 바이너리 읽기/쓰기 순서·암호키/원자 교체·디버그 저장 금지 보존을 비교했다.

## 발견 사항 — 모두 P2, 권장 보완

1. GleoProbe.kt:46. 완료 logcat에서 result/status/reason/전송 성공 여부가 사라졌다. 유효 요청, INVALID_INPUT, TIMEOUT, sendBroadcast 예외가 모두 COMPLETED+시간/길이로만 보인다. 개인정보 없는 status/reason 및 전송 시도 결과를 유지하고 원문 제외를 검증해야 한다. 현재 UI 메모리 기록은 프로세스 종료 시 사라지므로 사후 진단이 어려워진다.
2. DraftRoute.kt:12 및 DraftIntegrationInstrumentation.kt:130~139. onResume 갱신을 Route로 옮겼지만 새 계측은 debug 화면에서 ViewModel의 기본값 생성만 확인한다. ON_RESUME 연결을 제거해도 기존 34개 테스트와 해당 계측은 통과할 수 있다. Fake 저장소를 연결한 Route의 최초 진입/복귀 조회 및 화면 갱신 테스트가 필요하다. 실제 버그 재현 주장이 아닌 변경된 생명주기 연결의 테스트 공백이다.
3. docs/plans/sprint-1-gleo-drafts.md:40~48, 98~112. 현재 실행 명세가 삭제된 RoadNoteContract.kt, 이전 DraftScreen 개별 인자 시그니처, MainActivity의 상태 연결을 지시한다. 신규 RoadNote.kt/DraftStore.kt·DraftUiState·DraftRoute 계약으로 갱신하거나 당시 명세를 이력으로 표시하고 현재 계약을 연결해야 한다. 문서대로 협업하면 옛 파일 재생성과 컴파일되지 않는 호출이 생긴다.

## 검증과 한계

- PASS: 이번 리뷰의 verify_harness.py 및 git diff --check.
- 기존 결과 확인: 단위 테스트 XML 34개, failures=0/errors=0/skipped=0. 빌드/Studio/격리 저장/Hilt 실행 결과는 직전 변경 검증 기록을 재사용했으며 이번 리뷰에서 재실행하지 않았다.
- NOT_TESTED: 실제 Gleo 음성·timeout/전송 실패 E2E, Release 화면 최초 진입/복귀, 실차·모든 폼팩터. 기존 lint 일부 Lifecycle 검사 생략도 유지된다.
- 신규 크래시·저장 형식 회귀는 정적 비교에서 확인하지 못했다. 위 3건은 미수정이다. 소스/기존 테스트는 변경하지 않았다.

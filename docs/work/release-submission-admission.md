# release 제보 저장 차단

초기 HEAD `d938ca9c5e0ee71ce5344f5d16aaba91dd940548`. release에는 현재 제보 UI가 없지만 내부 Binder와 Gleo 제출 클라이언트의 공통 진입점은 위치 수집 중이면 `demo.enabled=false`에서 시연 검사를 우회했다. `RoadNoteSubmission`은 null 시연·경로로 쪽지를 만들 수 있고 Room은 유효한 좌표를 저장한다. 기기 재현은 수행하지 않는다.

- PRIV-01 / DOC-72a5d7ab4ebb#개인정보최소수집 · 위치 목적과 동의, 최소 저장. 프로젝트 정책은 Room 쓰기를 합성 시연으로 제한한다. 서비스에서 활성 시연 및 쓰기 허용 상태를 검사한다.
- INT-01 / DOC-f1d260bfb503#발신자와콜백 / DOC-2bec764daf11#스키마승인 · 예시의 broadcast 형식과 출시 신뢰 계약을 구분한다. 수신기 노출과 Callback 대상은 유지한다.
- REL-01 · 남은 기능 및 서명·깨끗한 설치 조건을 제출 점검표에 남긴다.

## 구현

- 공통 제보 진입점은 BUSY를 먼저 반환하고, 활성 시연 ID·시연 기능 활성·READY 제보자·쓰기 전환 상태를 검사한다. 시연 조건이 빠지면 위치 앵커를 조회하기 전에 `DEMO_REQUIRED`를 반환하고 관측 가능한 제출 상태를 갱신한다. 이미 진행 중인 쓰기의 상태는 유지한다.
- 기존 상태 영역에 시연 전용 저장 안내를 연결했다. release의 제보 UI가 없으므로 제출 시도가 없어도 안내를 볼 수 있게 했다. 내부 제보 요청의 응답에도 같은 이유를 전달한다.
- debug의 SDK 시연 입력과 위치 출처, Room 형식 및 레거시 읽기, ACK·오디오 정책, Manifest·Callback 대상은 기존 경로를 사용한다. 이 변경은 debug 위치 입력의 출처를 재인증하는 작업까지 포함하지 않는다.

## 검증

- PASS · `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease`. JVM XML 137개, 실패·오류·스킵 0. 활성 시연 및 READY/전환 조건, release 시연 ID 유무, BUSY 우선, 상태·문자열과 기존 debug 통합 회귀 확인.
- NOT_TESTED · `:app:testReleaseUnitTest`는 등록되지 않아 별도 release JVM 실행 불가. 공통 제보 정책의 release 분기는 debug JVM에서 검증했다.
- PASS · `python3 scripts/verify_harness.py --build`, `git diff --check`.
- FAIL · `python3 scripts/verify_harness.py --report docs/work/release-submission-admission.json`은 형식 검사를 통과했고 출시 미충족으로 종료코드 1.
- NOT_TESTED · IDE 실행·실제 기기/앱/adb·화면 렌더링·위치와 Gleo SDK·음성 검증 미실행. [제출 준비 점검표](../RELEASE-READINESS.md)

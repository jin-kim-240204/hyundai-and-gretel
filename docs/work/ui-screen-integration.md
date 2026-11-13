# 도로 쪽지 화면 통합

- 기준 main `8494fc5`, PR #5 `0965d6e`, 공통 조상 `6ce3463`. 별도 UI worktree에서 공용 화면 컴포넌트, 테마, 벡터, 폰트와 라이선스를 선택한다.
- PR #5의 `docs/design/compose-design-system.md`, 시작·수신 Preview와 권한 안내 Preview의 구조를 재사용한다. 실제 권한, 위치 수집, 제보 저장, 수신 및 확인은 main의 ViewModel·서비스 계약에 연결한다. 분할 창 표현은 PR #5의 512dp 분할 Preview와 1030dp 전체 Preview를 기준으로 창 폭을 사용한다.
- 수신 표시는 기존 차량 제한과 내용 예산을 따른다. 표시 기록과 확인 이벤트를 분리하고, 확인은 기존 RESUMED 가드와 서비스 검증을 거친다.
- DES-01/02/03/04/06/07 적용. 아래 원문을 기준으로 비기기 검사 범위와 화면 확인 범위를 구분한다. IME 및 신규 시스템 알림은 범위 밖이다.
- 계획 검사: 하네스, debug 빌드·lint, JVM 상태 변환 테스트, release 컴파일. Figma 원본, 창별 화면과 실기기 검증은 NOT_TESTED.

## 구현 및 디자인 범위

- 시작/권한 화면은 PR #5의 헤더 → 상태 카드 → 우측 액션 구조를 따른다. 수신 화면은 헤더 → 분류 배지·본문 → 상태 구조에 출처 주의 문구와 명시적 확인을 연결한다. 위치 수집 시작/중지, 권한 재요청 및 설정 이동, 대기·실패 상태의 기존 동작을 유지한다.
- 분할 레이아웃은 창 폭 512dp 이하에서 적용한다. 실제 문구와 동작을 연결하면서 수동 스크롤, 기존 3줄 생략 처리 및 차량 문자열/본문/항목 제한을 보존했다.
- 주야간 테마, 경고/정보 벡터, Asta Sans를 PR #5에서 선택했다. 비활성 액션은 기존 보조 텍스트 색상을 사용한다. PR #5의 Preview Activity, Gradle, Manifest 및 관련 기기 테스트 변경은 가져오지 않았다.
- 수신 표시의 `markPresented`는 차량 표시 예산만 기록한다. 사용자가 확인을 눌렀을 때만 `ReceiptKey`를 전달하며 기존 Route의 RESUMED 확인과 서비스의 중복·대상 검증을 사용한다. 서비스 계약 변경 없음. release의 DemoPanel은 기존 no-op 구현을 사용한다.
- PR #5에서 확인한 디자인·캡처 범위는 과거 Preview의 근거다. 요청의 Figma 노드, 현재 화면의 외형 및 동작, 실차는 NOT_TESTED다.

## 규칙별 확인

| 규칙과 원문 | 적용 및 증거 |
| --- | --- |
| DES-01 · DOC-9c1fe6a8475c#대비 · [기본 디자인 원칙](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro) | 비활성 액션 색상은 배경 합성 후 WCAG 상대휘도 기준 주간 6.02:1, 야간 7.00:1로 정적 PASS. 현재 화면 전체 대비는 NOT_TESTED. |
| DOC-9c1fe6a8475c#응답과즉시성 · 같은 문서 | 0.25초 초과 시 진행 피드백 필요. 화면에 위치 준비/대기, 저장 진행, 확인 기록 중 상태가 연결되어 있다. 실제 반응시간과 한 손 사용은 NOT_TESTED. |
| DOC-3790286d55ee#주행제한 · [주행 안전 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) | `visibleReceipt()`의 본문/문자/항목/깊이 제한과 확인 상태를 JVM으로 확인한다. 실제 차량 규제 전환 및 화면 확인은 NOT_TESTED. |
| DES-02 · DOC-9c1fe6a8475c#텍스트 · DOC-728dd6fe8092#글자위계 · [Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography) | PR #5 글자 위계와 본문 3줄 상한을 적용했다. 물리 글자 높이와 실제 줄바꿈은 NOT_TESTED. |
| DES-03 · DOC-94caa75424dd#의미색상 · [Colors](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors) | 기존 시맨틱 색상과 시스템 주야간 분기를 적용했다. 실제 화면의 주야간 색상 및 Figma 대조는 NOT_TESTED. |
| DES-04 · DOC-e12e49cc3efc#시스템영역 · DOC-189d894e8124#분할창 · DOC-92bfdd154e9d#화면전환 · [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) / [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) / [폼 팩터 테스트](https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide) | 기존 앱 창 내부에서 너비 기반 본문을 선택한다. 가로·세로·분할·시스템 영역은 NOT_TESTED. |
| DES-06 · DOC-d771e574367a#버튼상태 · [Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button) | PR #5의 64dp 화면 액션과 비활성 가드를 사용한다. 터치·pressed·실제 배치 확인은 NOT_TESTED. |
| DES-07 · DOC-6b1b8763468a#아이콘격자 · [Iconography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/contents-creation/iconography) | PR #5의 48dp 경고·정보 벡터를 사용한다. 현재 화면의 아이콘 표시와 원본 매칭은 NOT_TESTED. |
| DES-05 / DES-08 | 새 키보드 입력 및 시스템 알림·팝업·토스트 변경 범위 없음. N/A. |

## 검증 환경

- 첫 Gradle 실행은 기본 샌드박스에서 캐시 잠금 쓰기가 제한되었다. 권한 범위를 Gradle 실행으로 높여 검사했다. `local.properties`는 로컬에만 두었다.
- IDE 빌드 NOT_TESTED. Studio MCP는 전용 worktree가 열려 있지 않아 요청을 거절했다.
- 에뮬레이터, 시뮬레이터, 설치, 앱 실행, adb 및 화면 자동 제어는 수행하지 않았다.

## 최종 검사 결과

- PASS · `python3 scripts/verify_harness.py` 및 `python3 scripts/verify_harness.py --build`로 정적 하네스, `:app:assembleDebug`, `:app:lintDebug` 확인.
- PASS · `./gradlew :app:testDebugUnitTest :app:assembleRelease --console=plain`. JVM 114개 중 실패·오류·건너뜀 0개. 신규 상태 변환 검사 4개는 위치 상태, 제보 상태, 수신 분류·확인 상태, 차량 표시 제한을 확인했다. release APK의 DEX에 debug DemoViewModel/AndroidDemoInputSource 및 PR #5 Preview Activity가 없다.
- PASS · `git diff --check`. 보고서 검사는 미검증 항목을 보존하여 종료 코드 2를 반환한다.

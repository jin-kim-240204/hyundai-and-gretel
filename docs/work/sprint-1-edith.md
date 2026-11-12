# Sprint 1 — 이디스: 동의·초안 Compose UI

2026-11-08. 요청: `docs/plans/sprint-1-gleo-drafts.md`의 이디스 범위 구현. 사용자 후속 요청으로 내 파일만 의미 단위 커밋한다. push하지 않는다.

## 범위와 계약

- `ui/drafts/**`, UI 전용 테스트/프리뷰, 이 작업 기록만 편집한다. 현재 RoadNoteContract는 계획 v1과 일치한다.
- 순수 `DraftScreen`은 전달받은 상태만 표시하고 콜백만 호출한다. 저장소, Receiver, Activity, 권한, Gradle은 변경하지 않는다. MainActivity 연결과 busy/error 갱신은 자비스 담당이다.
- 미동의일 때 초안을 표시하지 않는다. 허용 버튼만 true를 전달한다. 거부/철회는 false를 전달하며 성공을 낙관적으로 표시하지 않는다. busy 때 전 조작 비활성화.
- 긴 내용·지명·방향은 최대 3줄 말줄임. 위치는 대기/미정만 표시하며 현재 좌표/차량 방향을 확보했다고 표현하지 않는다. 수신 시각은 현지 시간대와 UTC offset을 표시한다.
- 신규 의존성, 지도/게시 버튼, 저장소/DI 계층은 추가하지 않는다. 기존 호출자는 없으며 자비스가 공개 시그니처로 연결한다.

## 적용 원문 및 확인 계획

공식 의무, 권장, 프로젝트 선택을 구분한다. 원문 URL은 `.agents/skills/pleos-connect/references/source-map.md`와 아래 페이지 해시에 연결된다. 2026-11-08 공식 intro/basic-button을 다시 열어 본문이 스냅샷과 일치함을 확인했다. 나머지는 저장소 원문 스냅샷을 읽었으며 최신성 판정은 하지 않는다.

| 규칙/세부 요구사항 | 구현·확인 |
|---|---|
| DES-01 / DOC-9c1fe6a8475c#단순조작 | 한손 버튼, 자동 스크롤/장식 모션 없음. 실제 조작·인지시간은 NOT_TESTED |
| DOC-9c1fe6a8475c#지연피드백 | busy 안내와 버튼 비활성화. 0.25초 내 busy 갱신은 Activity 통합 후 측정 |
| DOC-9c1fe6a8475c#대비 | 4.5:1 기준, 동적 주야간 테마의 실제 렌더 대비 측정 필요 |
| DES-02 / DOC-9c1fe6a8475c#텍스트제한 | 단락별 3줄 말줄임, 영문 기준을 한국어 글자수로 치환하지 않음 |
| DOC-728dd6fe8092#글자크기 | 중립적인 기존 폰트 사용, 본문 20sp. 5mm/3mm와 실제 18px는 실화면 측정 필요 |
| DES-03 / DOC-94caa75424dd#주야간 | 기존 MaterialTheme의 색상 쌍 사용, 밝음/어두움 프리뷰 제공. 화면 확인 필요 |
| DES-04 / DOC-e12e49cc3efc#가로세로 / DOC-189d894e8124#윈도변경 | 세로 LazyColumn, 전체 폭 버튼, safeDrawing inset. 좁은/가로/긴 내용 프리뷰. 분할/회전/시스템 오버레이 실환경 필요 |
| DES-06 / DOC-d771e574367a#버튼상태와도해 | pressed는 기존 Material 버튼 사용, busy일 때 disabled. 공식 크기/위계 도해 확인 후 수치 기록 |
| PRIV-01 / DOC-72a5d7ab4ebb#목적최소수집 | 초안을 나중에 확인하기 위한 저장임을 설명, UI에서 저장/로깅/외부전송 없음 |
| DOC-4036191ed19c#명시적동의 | 항목(내용·장소/방향 표현·시각), 기기 내 최대 7일, 거부 가능, 미게시/미공유, 철회 삭제 고지. 동의 버튼 외에는 true 호출 없음 |
| DOC-4036191ed19c#철회삭제 | false 콜백만 전달. 실제 삭제·철회 원자성은 프라이데이 저장소와 자비스 연결에서 검증 |
| SAF-01 / DOC-3790286d55ee#주행규제 | DO 선언/주행 중 읽기 기능 추가 없음. CarUxRestrictions 및 실차 규제 검토는 통합 담당과 별도 검증 |

DES-05(입력/IME), DES-07(신규 아이콘/모션), DES-08(팝업/알림), 차량·음성 API는 사용하지 않는다. PRIV-01의 보안 원문 `27ac8605961f`도 읽었으며 UI에는 저장/네트워크/WebView 경계가 없다. 개인정보처리방침·법적 출시 적합성은 이 UI만으로 완료되지 않는다.

pleos-connect가 참조하는 `android-compose-architecture/SKILL.md`는 저장소와 설치 스킬 검색에서 찾지 못했다. 이번 범위는 상태를 소유하지 않는 순수 UI이며 구조 리팩터링/코루틴 연결은 수행하지 않는다.

## 검증 및 남은 작업

환경: macOS arm64, 기존 Gradle wrapper/compileSdk 35/minSdk 28, 작업 브랜치 `gm/feat-speech-to-text`. 실행 결과는 실행 시점 공유 작업 폴더 기준이며 타 담당 코드의 품질 완료 판정이 아니다.

- PASS: 첫 구현의 `./gradlew :app:testDebugUnitTest --console=plain`. 표시 검사 3개 모두 성공 (`app/build/test-results/testDebugUnitTest/TEST-com.jinkim.hyundaiAndGretel.ui.drafts.DraftPresentationTest.xml`), 당시 전체 31개 실패/오류/skip 0. 현재 도로/차량 방향을 미확정으로 표시, 두 위치 상태와 네 모드, 사용자 표현 및 null/blank 보조정보 검증.
- PASS: 첫 구현의 `python3 scripts/verify_harness.py --build` (assembleDebug/lintDebug 포함), Studio MCP `build_project` (isSuccess=true, problems=[]).
- 환경 제한 및 조치: 최초 CLI 빌드는 Gradle 캐시 lock 접근이 샌드박스에서 거절되어 승인된 명령으로 재실행 후 통과했다. 공식 버튼 SVG도 DNS 제한 후 승인된 curl로 다운로드했다.
- NOT_TESTED: 인앱 브라우저 host unavailable. SVG는 웹 도구가 image/svg+xml을 지원하지 않고 로컬 sips PNG 변환도 실패했다. 도해의 실제 수치/간격/위계는 판독하지 못했으므로 추측하지 않는다. 80dp 버튼 높이와 16dp 간격은 프로젝트 임시 선택이며 공식 도해 수치라고 주장하지 않는다. 원본: https://document.pleos.ai/assets/images/basic_size-033b09c8bbef4d1776a3f4c7c70a49d0.svg
- NOT_TESTED: Studio에서 프리뷰 파일을 여는 것은 성공했으나 CUA 프리뷰 상태 조회가 30초 timeout/kernel reset. 화면 렌더/스크린샷 검토 증거 없음. 7종 프리뷰는 재현 준비물이며 실행 증거가 아니다.
- 소스 검토: DraftScreen.kt의 consentGranted=false 분기는 목록을 구성하지 않는다. true는 허용 버튼에서만 전달한다. 모든 조작은 DraftAction의 enabled=!busy를 사용한다. UI 내부에 부수효과/저장/로그/성공 상태 변경은 없다. 실제 터치 이벤트의 중복 방지·삭제 대상 ID·철회 전후 화면은 통합 후 별도 확인 필요.
- 검토 수정: 좁은 창/큰 글꼴에서 동의 고지 누락 가능성을 줄이기 위해 거부와 철회 문장을 별도 단락으로 나눴다. 실제 잘림/가독성, 동적 테마 대비 4.5:1, mm 글자 높이, 폼팩터/회전/시스템 오버레이 및 주행 규제는 NOT_TESTED.
- 문구 수정 후 재검증 중 공유 폴더의 `roadnote/DraftFileStore.kt:110`에서 `ByteArrayInputStream` import 누락으로 CLI/Studio 빌드 실패를 관찰했다. 해당 파일은 프라이데이 소유이므로 수정하지 않았다. 이후 담당 변경으로 import가 생긴 것을 확인한 뒤 재실행하여 CLI harness `--build` 및 Studio 빌드 모두 PASS로 회복했다. 수정 없이 동일 실패를 반복 실행하지 않았다.
- 공유 폴더 검사 중 `LocalDraftStoreTest.updatesPreserveOriginalTimesAndResolveAllModes` 실패도 관찰했다. 이후 담당 테스트의 NAMED_PLACE 입력에 placeQuery가 추가되고 저장 검사 XML이 성공으로 바뀐 것을 확인했다. 이디스는 타 담당 파일을 수정하지 않았다. UI 전용 명령 `./gradlew :app:testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.ui.drafts.DraftPresentationTest --console=plain`은 최종 문구에서도 PASS(3개)다.
- JSON 보고 검사: `python3 scripts/verify_harness.py --report docs/work/sprint-1-edith.json`은 형식·규칙 ID·문서 연결을 통과하고 exit=2. 이는 남겨둔 필수 NOT_TESTED를 올바르게 반영하는 결과다. UI 검증 완료/출시 적합으로 보고하지 않는다.
- 최종 `./gradlew :app:testDebugUnitTest --console=plain` 재실행 PASS. 최종 코드의 harness `--build`, Studio build_project도 PASS. 최신 결과 XML의 개수는 동시 작업으로 늘어날 수 있으므로 이디스 고유 검사는 위 3개로 구분한다.

## 자비스 인계

- 공개 진입점은 계획의 `DraftScreen(consentGranted, drafts, busy, errorMessage, onConsentChange, onDelete, onRefresh)`와 동일하다.
- 최초 로딩과 저장/삭제/철회/새로고침 시작 전에 busy=true, 성공 또는 오류 확정 후 false가 필요하다. 오류는 예외 원문/제보 내용/파일 경로가 없는 짧은 비민감 문구만 넘긴다. 실패 때 목록이나 동의를 성공값으로 먼저 바꾸지 않는다.
- 삭제·철회 성공 후 저장소에서 목록과 동의를 다시 읽는다. Gleo 수신에서 Activity를 강제 실행하지 않는다. 순수 화면은 저장소·Receiver를 알지 못한다.
- 위치·방향은 모두 표현/대기/미정이며 측정값이 아니다. 초안은 3줄 요약이며 전문 열람/지도/게시 기능은 없다.
- 실제 동의→저장→삭제/철회, 재시작, 좁은/가로/분할/야간/큰 글꼴, 실제 터치/규제 증거가 필요하다. 개인정보처리방침, 같은 Android 사용자 내 운전자 격리, 출시 심사는 이 작업 범위 밖이다.

## 커밋

- `0c855f1` — 순수 Compose UI, 7종 프리뷰, 표시 로직 단위 검사, 초기 규칙/범위 기록. 경로 지정 `git commit --only`로 타인 staged 변경을 제외했다.
- 후속 커밋 — 짧은 동의 문구와 최종 검증/인계 기록(JSON 포함). 완료 후 git 로그에서 확인 가능. push하지 않는다.

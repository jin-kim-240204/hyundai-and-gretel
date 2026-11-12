# Compose 디자인 시스템 이식

## 요청 및 범위
- Foundation·Component 페이지 및 화면 프레임을 기준으로 디자인 토큰, 테마, 공용 컴포저블과 Preview를 구현한다.
- UI만 구현한다. 업무 로직, 저장, 차량 연동, 화면 전환은 범위 밖이다.
- 원본: https://www.figma.com/design/UMhLHbSurVcDVwaYsiwAKz/?node-id=39675-4521
- 기존 진입점은 MainActivity.kt, 기존 테마는 ui/theme/{Color,Type,Theme}.kt이다. 현재 기본 Material 색상과 타이포그래피만 있으며 공용 UI는 없다.
- 예상 영향: 테마 및 호출자, 새 공용 UI 및 Preview, 필요한 디자인 리소스. 원본 확인 전 수치나 컴포넌트를 추측해 추가하지 않는다.

## 규칙 및 확인 계획
- pleos-connect, pleos-design, android-compose-architecture, pleos-verify 적용.
- 원본 접근 후 DES 규칙과 공식 원문 세부 항목을 선정해 색상·타이포그래피·크기·상태별 구현에 연결한다. 현재는 선정/원문 검토 미완료다.
- UI는 상태·콜백과 Modifier를 받으며 ViewModel·차량 API에 의존하지 않는다.
- 구현 후 하네스 --build, Studio 빌드, 원본 대비 주야간·비활성·긴 텍스트·다양한 창 크기 검토 및 JSON 보고서를 수행한다.

## 현재 상태 (2026-11-12)
- PASS: 기존 소스와 테마, 작업 스킬 확인. 작업 전 git status는 깨끗함.
- NOT_TESTED: Figma 노드·페이지·Foundation·Component 열람. create_session이 편집 권한 부족 오류를 반환하여 읽기 시작도 불가. whoami로 연결 계정은 확인함.
- NOT_TESTED: 원본 토큰 추출, UI 구현과 빌드·화면 검증. 원본 확인이 선행되어야 한다.
- N/A: 업무 로직 및 차량 API 구현.

## 재개
연결된 Figma 계정에 이 파일 접근 권한이 반영되면 create_session을 다시 시도하고 페이지 목록부터 확인한다. 원본이 확보되기 전에는 기본 Material 값을 Pleos 토큰으로 간주하지 않는다.

## 접근 복구 및 구현 계획
- JINKIM 연결로 접근 복구. 세션 ec3fa752-a1fb-4ed9-b091-effe3cfa5e95에서 Design, Foundation, Components, Guide 페이지 확인.
- 고해상도 context 및 스크린샷 확보: Foundation 타이포그래피 67:12330/12341/12352/12363, 기본 버튼 67:22525, 화면 39675:4528/4537/4548/4608.
- 이후 Starter MCP 호출 한도로 색상 표 전체와 나머지 컴포넌트 상세 조회 차단. 확인한 값만 이식하며 전체 라이브러리 이식 완료로 보고하지 않는다.
- 공용 버튼은 Primary/Secondary, L/M/S와 enabled/pressed/disabled/loading, 아이콘 슬롯을 지원한다. 화면용 상태 카드·메시지 카드·헤더·상태 표시를 stateless UI로 제공한다.
- Foundation Typography는 60/80, 56/72, 48/64; 40/52, 36/44, 32/40; 30/38, 28/36, 26/34; Label 24/28, 20/24 (크기/행간 sp). Asta Sans를 앱에 포함한다.
- 화면 시안 고유 값(배경 #F7F8FA/#1D1E21, 카드 #FFFFFF/#313236, 본문 #131417/#FFFFFF, 보조 #555863/#BAC0CA)은 별도 앱 의미 토큰이다. Foundation 전체 의미 색상과 동일하다고 단정하지 않는다.
- 픽셀 도해의 레이아웃 수치를 Compose dp로 옮기는 것은 프로젝트 선택이며 물리 mm 검증을 대신하지 않는다.

### 적용 규칙과 원문 세부 요구사항
- DES-01 / DOC-9c1fe6a8475c#대비: 텍스트·아이콘 4.5:1. 주야간 주요 텍스트 수치 검사; 비활성 원본은 예외로 숨기지 않고 별도 측정한다.
- DES-01 / DOC-9c1fe6a8475c#피드백: 지연 시 응답 중 표시. 버튼 loading 시 표시 및 클릭 차단. 업무 지연 측정은 UI 범위 밖.
- DES-02 / DOC-9c1fe6a8475c#텍스트: 문단 3줄 이하, 영문 기준을 한국어 글자수에 적용하지 않음. 실제 mm는 NOT_TESTED.
- DES-04 / DOC-e12e49cc3efc#레이아웃 및 DOC-189d894e8124#앱창: 앱 윈도 내부 사용, 가로·세로·분할 크기에 대응. 시스템 UI를 복제하지 않음.
- DOC-728dd6fe8092#위계: 중립적 서체, 제목·보조 정보 위계와 일관된 크기 권장. Asta Sans와 Foundation 표를 사용.
- DOC-94caa75424dd#색상: 시맨틱 색상을 용도별 사용하고 주야간을 지원. 확인 못한 표 값은 미이식.
- DOC-d771e574367a#버튼상태: pressed, disabled, loading 표현; loading/disabled 클릭 불가. 크기 도해는 Figma 67:22525에 연결.
- 공식 원문 확인일 2026-11-12. URL은 각 DOC 해시의 source-map 항목 참조. 자동·실화면 검증은 구현 후 갱신.

## 구현 및 검증 진행

- `f316f72`: Asta Sans, Foundation Typography, 주야간 앱 의미 색상, 간격/크기 토큰, 기존 Theme 연결.
- `8cd05b7`: 기본 버튼 및 화면 공용 UI, 주야간/상태 Preview, 원본 SVG와 Android VectorDrawable.
- UI 로직 외의 ViewModel·Repository·저장·차량 API·화면 전환은 추가하지 않았다. MainActivity는 기존 시작 화면을 유지한다.
- PASS: `python3 scripts/verify_harness.py --build`; assembleDebug와 lintDebug. 최초 컴파일의 잘못된 semantics import와 FontVariation opt-in 누락을 수정한 뒤 통과.
- PASS(초기): Pleos_Connect_2_1_0 / Android 14 / 2560×1440 / 160dpi에서 instrumentation 3개 통과. enabled 콜백과 disabled/loading 클릭 차단, 주야간 렌더링 확인.
- 화면 검토 후 검사 강화: 원본과 다른 compact 줄바꿈, Text 자연 너비의 소수점 반올림에 따른 didOverflowWidth를 발견. compact 메시지 폭을 Android Asta Sans 실측에 맞추고 텍스트 레이아웃 폭을 명시한다. 줄 수·첫 줄·overflow 검사를 추가해 재검증한다. Figma 194px과 Android 제한 폭 220dp 차이는 플랫폼 글꼴 측정 차이를 수용한 프로젝트 선택이며 1:1 수치 일치라고 표시하지 않는다.
- 캡처 재현: 테스트 APK 설치 후 `adb shell am instrument -w -e class com.jinkim.hyundaiAndGretel.ui.PleosComponentsTest com.jinnayeong.wakeup.test/androidx.test.runner.AndroidJUnitRunner`. 스크린샷은 targetContext.filesDir에 저장하며 Automotive 현재 사용자 10에서 `run-as`로 가져온다.
- NOT_TESTED: Studio build_project. 이 작업 트리는 IDE에 열려 있지 않고 다른 checkout만 열려 있어 도구가 거절했다. 다른 checkout을 빌드해 현재 변경의 증거로 사용하지 않았다.
- 대비 측정: evidence/compose-design-system/contrast.json. 주요 텍스트 6.67~18.42:1, 버튼 9.41:1, 아이콘 내부 6.57~7.78:1. 원본의 비활성 버튼은 약 2.08:1로 4.5:1 미달(FAIL). 낮은 원본 대비를 숨기지 않고 재설계/공식 적용 조건 판단을 후속으로 남긴다.
- NOT_TESTED: 모든 실제 화면의 상태/pressed/긴 번역/큰 글꼴/IME/시스템 오버레이/물리 mm, 차량 및 심사 판정. 나머지 Foundation 의미 색상·Components는 호출 한도 때문에 미이식.
- PR은 기존 공통 설계 브랜치 `spec-road-note-system` 기준 Draft로 생성한다. 현재 UI 브랜치는 그 원격 head 6ce3463에서 시작했으며 main 대상 PR #2의 설계 변경을 중복 포함하지 않는다.


## 최종 결과

- PASS: 최종 app/테스트 APK 컴파일 및 lint. 하네스 --build 통과. git diff --check 통과.
- PASS: 초기 instrumentation 3개(첨부 initial-ui-tests.xml). 버튼 구현은 이후 변경되지 않아 클릭 차단 검증 근거 유지.
- PASS: 강화한 day/night 메시지 줄 수·compact 첫 줄·overflow 개별 검사, 1030×377 및 512×377 캡처 검토. night 메시지는 원본과 같은 첫 줄/둘째 줄을 표시한다. Android 글꼴 측정 및 자연 너비 소수점에 맞춰 텍스트 폭을 조정했다.
- NOT_TESTED: 최종 전체 suite. 반복 실행 중 Pleos vendor homescreen이 중복 task listener 예외로 종료되어 테스트 Activity가 resumed되지 않음. 자세한 환경과 예외는 evidence/compose-design-system/runtime-limit.txt. UI 로직과 무관한 launcher를 수정하거나 DO 선언을 추가하지 않았다.
- FAIL: 원본 disabled 대비 2.08:1. NOT_TESTED: 나머지 원본/실차/창 전환/큰 글꼴/IDE build. 완성된 디자인 시스템 또는 Pleos 적합성 검증 완료로 보고하지 않는다.
- 기본 시작 화면에 서비스 로직을 넣지 않았으며 공용 UI/Preview를 후속 화면 구현에서 사용한다.


## 에뮬레이터 표시 후속 요청

- 요청: 사용자가 UI를 볼 수 있도록 Pleos 에뮬레이터를 켜고 화면 표시.
- 범위: 기존 ReceivedNotePreview를 internal로 공개하고 debug source set 전용 Activity/Manifest에서 재사용. Release 진입점과 업무 로직은 추가하지 않는다.
- 적용: ENV-02의 ComponentActivity 구조 유지, 기존 DES-03/04 색상·앱 창 규칙 유지. 새 디자인이나 DO 선언 없음.
- 확인: 하네스 --build, debug APK 설치·명시 실행, resumed Activity 및 실제 화면 캡처 확인. 이전 전체 디자인 시스템 미검증 상태는 유지.
- PASS: 후속 debug APK assemble/lint 및 하네스 --build 통과. Pleos emulator에서 MAIN/LAUNCHER 명시 Intent로 DesignSystemPreviewActivity 실행 및 resumed 확인. 실제 앱 창에 수신 카드가 표시되는 스크린샷 확인.
- NOT_TESTED: Studio 빌드는 동일하게 작업 트리가 IDE에 열려 있지 않아 거절. 기존 전체 suite/디자인 시스템 제한은 유지.

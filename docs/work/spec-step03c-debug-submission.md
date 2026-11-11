# 03-c 스펙 작성·독립 리뷰 요청

요청: /private/tmp/road-notes-sdd-operations.md를 먼저 읽고 현재 코드/PR/기록 기반 다음 미완료03-c 스펙만 작성. 조정자01a1c569-6a15-72b0-ad11-37c542b2a173에게 반환. 독립 리뷰·리뷰이 판단·재검토 전 구현/커밋 금지. 앱/설치/Studio/adb/기기·emulator·simulator·계측 실행 금지. 이번에는 문서 검사만 수행한다. 신규 하위 에이전트/역할 세션 없음.

기준: HEAD/PR4 head 8b9504e6cf3cbf96d27a7e3a5b698ff8aec51798, base spec-road-note-system, ny/spec-driven-road-notes. PR은 gh GraphQL 읽기만 했다. unresolved4541836274(구독분리)·4541836293(SDK오류수명)와 해당 답변/작업기록을 대조했다. onConnected/onDisconnected가 추적 StateFlow만 교체하는 LocationViewModel, Binder가 제출상태·debug진입을 노출하는 서비스, Repository의 상태/고정입력/단일수락 구조를 읽었다.03-a/b는 구현되어 있어03-c가 다음이다. 현재03/00문서의 일부 진행 문구는 코드보다 오래되어03 문서에 이력 구분과 링크를 추가했다.

적용: pleos-connect → android-compose-architecture/clean-code-review/pleos-design/ponytail. ENV-02/VEH-03/PRIV-01/DES-01/DES-02. 공통정본의단발저장·noteId PK·합성한정·TTL·수명 정책은 바꾸지 않는다. debug/release소스셋과프로젝트선택은공식의무와구분한다.

## 원문 요구사항·확인 계획

- DOC-9c1fe6a8475c#즉시성: 공식 UI 가이드 Glanceability의 지연시상태표시·짧은인지·단순조작 → 03-c§4 Submitting/Loading을수락즉시표시, 일회클릭. 구현후순수상태/매핑검증, 실제0.25초지연측정은NOT_TESTED.
- DOC-9c1fe6a8475c#대비와문구: Color/Readability의4.5:1·문단3줄·물리글자높이 →짧은문구/기존Theme재사용계획, 실제대비·물리단위·주야간·잘림은NOT_TESTED. 영어문자기준을한글글자수로변환하지않음.
- DOC-d771e574367a#버튼상태: Basic button States의 Disabled/Loading 제어불가 → 저장중비활성/Loading, 서비스/Repositorygate별도유지. 도해수치준수는판정하지않음(실제측정없음).
- DOC-e12e49cc3efc#앱윈도우: Application window와시스템영역구분 및portrait/landscape 조절 → 기존앱윈도우에만표시, Driving view/GNB침범금지, 실제다중창은NOT_TESTED.
- DOC-9844e9eaad3c#콜백: 기존SDK콜백수명/단조시각/처리직전세션검사 변경없음, 03-c는순수Fake로기존로직검증계획만추가. 실제SDK오류주입은NOT_TESTED.

공식원문 2026-11-12 확인: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro 및 https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button . layout/window/colors/typography는저장소원문스냅샷을함께읽음. Android source sets 근거 https://developer.android.com/build/build-variants 확인. 기본source-set분리구현후debug/release빌드·패키징확인계획. 이번새라이브러리/SDK보장은없음.

수정파일: docs/specs/road-notes/03c-debug-submission-ui.md 신규,03-save-road-note.md 진행상태/링크보강, 이기록/JSON. 기존미커밋PleosLocationSource폴링·타문서·untracked흔적은보존한다. 부모정본/소스/Gradle/스키마/테스트/PR코멘트수정없음.

리뷰쟁점(정책/구현경계, 확정결함아님): 서비스소멸뒤Saved복원범위, 준비/첫표본대기버튼허용, pureJVM으로실제구독·종료규칙검증할최소추출. 기기검증금지는차단이지만운영합의대로NOT_TESTED기록후후속단계진행가능. 이단계에서는독립스펙검토만다음행동이며스스로구현하지않는다.

## 이번 검사 결과

PASS: 03/03-c 상대 Markdown 링크 전부 존재, git diff --check, verify_harness.py(exit0). report는7항목·exit2: DES-01/DES-02 실제UI/실물검증 NOT_TESTED를 의도적으로 보존. FAIL 없음. 문서 전용이므로 JVM/Gradle 빌드는 N/A(미실행), 앱/설치/Studio/adb/에뮬레이터/시뮬레이터/계측은 명시금지로 실행하지 않음. 시작status와비교해이번변경은스펙2개·작업기록2개뿐, 기존변경보존. 구현/커밋/푸시/PR댓글/resolve없음. 독립스펙리뷰를위해조정자에게반환후대기.

## 원격 스펙 리뷰 요청 승인

후속 사용자 지시로 이03-c스펙/관련상태/기록4개만 docs커밋·푸시하고 PR4에 AI스펙작성자·커밋SHA·리뷰쟁점·검사결과·실행제한을 게시한다. 역할별 피드백/리뷰이판단/향후반영도 PR4에 기록한다. 이번에는 스펙게시까지만 수행하고 조정자의 독립리뷰를 기다린다. 앱코드/기존변경 보존, 구현·기기실행 없음.

## 독립 리뷰 반영 (714387b 이후)

조정자 지시로 pr4-03c-spec-review.md 및 pr4-03c-spec-response.md를 읽고 채택2건만 스펙에 반영.4542288503: STOPPED/FAILED+Submitting에서 시작/제보 모두 비활성, 완료 후 기존조건으로수동시작, 자동시작/대기큐없음.4542288514: A=RECEIVING/Saved와B=STOPPED/Idle, 예약A갱신, 관찰쌍일관성/연결없음, B중지클릭0·B활성클릭A0/B1 검증계획. StateFlow 모든내부순간값관찰을가정하지않음. 원문/기존정책은앞선기록과동일하며 새공식주장없음.

독립review/response 기록은타역할소유이므로 읽기만하고 stage하지않음. 이번수정스펙과이작업기록2개만docs커밋·push. 링크/diff/harness검사, 보고서의기기UI NOT_TESTED유지. 앱코드·빌드·JVM·기기실행없음. PR두스레드에AI스펙작성자/SHA/반영범위답글후resolve없이조정자재검토대기.

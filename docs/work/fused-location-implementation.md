# 위치 쪽지 스프린트 구현

범위 정정: 아래 Gleo 관련 구현·실험은 요청 범위를 넘긴 과거 작업이며 제거했다. 현재 코드는 디버그 제보로 쪽지 심기·수신만 제공한다. 현재 검증은 [제거 작업 기록](remove-gleo-from-location-branch.md)을 따른다.

요청: S0~S6을 순서대로 구현하고 검증 단위로 한국어 커밋. push는 요청 범위 밖. MVVM + ponytail: UI는 상태/이벤트, ViewModel은 화면 조합, 위치/저장/판정은 별도 책임. 범용 프레임워크·서버·상시추적 추가 없음.

선택 규칙: ENV-01/02/03, VEH-01/03/04, PRIV-01, DES-01/04/06, SAF-01, INT-01. 원문: DOC-c5e08a80ebfd#의존성, DOC-402de45b9f3d#권한, DOC-1ba06876919b#수명, DOC-9844e9eaad3c#콜백, DOC-ba9b83fc86f6#오류, DOC-72a5d7ab4ebb#최소수집, DOC-e12e49cc3efc#레이아웃, DOC-189d894e8124#윈도. 앞선 상세 플랜의 검토 내용과 공식 SDK 샘플을 적용하고 설치 AAR 심볼 확인. 새 proximity 원문은 API 존재와 실험 결과를 구분한다.

수정 범위: Gradle/Manifest, location/domain/data/ui/debug, schema/Receiver, 단위·Room·SDK 계측, 시연 스크립트·문서. 보관45초/목표30초/허용5초, callback 단조시각 기반 근사배치. 위치버퍼 메모리, 공유쪽지와 수신이력 Room. H3와proximity를 사용하되 검증 실패 시 완료라 하지 않는다.

S0: 상세 설계·원본 Mermaid·앞선 실험 결론 기록을 커밋한다. 구현 숫자는 프로젝트 초기값이며 실차 정확성을 보장하지 않는다.
S1~S6: 진행 중. 자동검사·런타임·미검증을 아래에 별도 기록한다.

S1 PASS: 실제 Pleos2.1.0/user10에 앱·계측 설치. SDK1.1.3 register 후 ADB 밖→안→밖 주입에서 ENTER/EXIT, remove 확인. ProximityTest 1개 PASS(27.068초). 첫 계측은 JUnit 메서드 반환형 오류로 실패, Unit 명시 후 수정 재실행 PASS. 지원 개수/차단·정차·이미안등록은 별도 미검증. applicationId를 기존 기능 브랜치와 동일하게 정정했으며 예전 com.jinnayeong.wakeup 데이터는 자동 이전하지 않음.

S2 core PASS: LocationBufferTest 4개. callback 시각 기준45초 보관·30초 준비·최근접선택·5초 신선도·큰공백초기화·미터거리/경로검증. 패널은 데이터 계층과 ViewModel 연결 뒤 별도 UI 커밋에서 구현한다.

S3 PASS: Room2.8.5/KSP2.3.13/H3-Java4.5.1 빌드. 첫 기기계측은 H3.so가 Java리소스로 제외돼 실패; Gradle Copy로 Android ABI별JNI 추출·newSystemInstance 사용 후 재실행 2테스트 PASS(0.151초). request멱등/충돌,동시8개receipt1행,자기제보/만료제외,cascade삭제,H3 경계포함 cover 검증. generated schema1.json 버전관리. Room import/동기화 서버는 없음. Kotlin프로퍼티 기반 컬럼명 및 nullable displayedAt으로상태파생해 문서의중복state컬럼은 생략.

S4/S5 core: DemoController가 세션/입력snapshot/과거anchor/직렬쓰기와 SDK 등록수명을 소유하고 ViewModel은 UI이벤트와StateFlow만 노출한다. 위치·H3계산·Room은 UI에서 호출하지 않는다. SDK ENTER 중심 + 등록당시반경안/지연위치 보완검사. 최초A/B실제SDK 통합계측 PASS(56.673초), 후속방향/이벤트필터수정은 최종계측으로 재검증한다. H3는res9,고정서울경로400m bounding box cover+이웃ring으로경계포함,16개SDK등록초과는표시하며공식한도보장아님.

S2 UI/S5 화면: src/debug Compose패널(A/B·초기화확인·fixture제보·상태), release동일슬롯no-op. NotesViewModel→NotesScreen 상태/이벤트 단방향, 실제window안 UI·FLAG_KEEP_SCREEN_ON설정·비가시수집중단. 기본실행화면 스크린샷에서시스템영역침범/잘림없음 확인. 물리글자높이/다중폼팩터전체인증은 NOT_TESTED. debug/release 빌드,단위테스트,lint PASS. 주행값바꾼동작은 별도확인예정.

S6 PASS(앱 경계): 실제 GleoActionReceiver로 ACTION_PARAM broadcast → Room anchor 저장 → GLEO_CALLBACK_SENT success → B proximity 등록/ENTER → 수신1행의 통합계측 PASS(54.954초). 이전generation의capture재사용은SESSION_NOT_READY, 같은반경중복수신없음도assert. 실제 Gleo모델/마이크/최종음성수신확인은 NOT_TESTED. 콜백은저장커밋후1회요청하고 asking없음. exported debug입력은JSON타입/크기/enum검사, release는비공개.

최종 자동검사: debug/release assemble, lintDebug, 단위6개 PASS. Room/H3/GleoJson기기계측3개 PASS(0.128초). verify_harness --build PASS, Studio build_project PASS. 첫E2E호스트도구는텍스트버퍼때문에준비로그를놓쳐timeout; Queue기반리더+runToken으로수정후실제경로재생·계측PASS. 실패를허용치완화로숨기지않음.

UI 검증: Pleos2560x1440,density160,기본앱·패널캡처확인. 로컬Virtual Console의실제UI엔드포인트로Drive/36km/h 설정·조회확인,앱같은화면유지. 앱Window KEEP_SCREEN_ON 설정ON 플래그확인,스위치OFF시플래그제거확인. 실험후가상차량Park/0복구확인. 웹콘솔세션만료후복구첫요청503은heartbeat갱신후복구완료. Gleo마이크표시가있는장면에서도앱유지관측했으나마이크발화완료/인식성공을증명하지않음.

남은 NOT_TESTED: privacy차단·서비스강제끊김·권한철회전체수명·SDK등록최대개수·이미반경안SDK단독발행·전체가로세로/분할/주야간/실차·공급자승인. 이미안등록은앱보완경로구현,기기통합의주경로는ENTER검증. 최종보고서exit2는이필수미검증을표시한다. 코드완료와제품/심사전체검증완료를구분한다. 푸시하지않음.

화면 수신함에 여러행이있으면‘다음쪽지’로한번에하나씩확인하도록보완했다. 최종diff검사PASS, 변경후debug/release/단위/lint재실행PASS. 사용자위치버퍼는계속메모리전용이며별도상시서비스는추가하지않았다.

최종리뷰보완 PASS: 과거snapshot이남아도현재위치가사용불가면저장전LOCATION_UNAVAILABLE로거절. SessionGuardTest 1개실기기PASS(0.129초). 계측직전emulator접속이끊겨기존AVD재기동후실행했으며데이터wipe없음. 보완후debug/release assemble,lint,단위6개,Studio빌드PASS. 이보완은실제SDK오류주입전체검증을대체하지않는다.

커밋분리: 설계 / SDK접근 / 버퍼 / Room·H3 / 심기·수신 / MVVM UI·패널 / 다중쪽지표시 / Gleo / 저장전상태검사 / 통합계측·시연안내·최종정책. 실험원시JSON은추가하지않고필수Room스키마와검증보고서만관리한다.

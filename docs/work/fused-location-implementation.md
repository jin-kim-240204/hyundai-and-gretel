# 위치 쪽지 스프린트 구현

요청: S0~S6을 순서대로 구현하고 검증 단위로 한국어 커밋. push는 요청 범위 밖. MVVM + ponytail: UI는 상태/이벤트, ViewModel은 화면 조합, 위치/저장/판정은 별도 책임. 범용 프레임워크·서버·상시추적 추가 없음.

선택 규칙: ENV-01/02/03, VEH-01/03/04, PRIV-01, DES-01/04/06, SAF-01, INT-01. 원문: DOC-c5e08a80ebfd#의존성, DOC-402de45b9f3d#권한, DOC-1ba06876919b#수명, DOC-9844e9eaad3c#콜백, DOC-ba9b83fc86f6#오류, DOC-72a5d7ab4ebb#최소수집, DOC-e12e49cc3efc#레이아웃, DOC-189d894e8124#윈도. 앞선 상세 플랜의 검토 내용과 공식 SDK 샘플을 적용하고 설치 AAR 심볼 확인. 새 proximity 원문은 API 존재와 실험 결과를 구분한다.

수정 범위: Gradle/Manifest, location/domain/data/ui/debug, schema/Receiver, 단위·Room·SDK 계측, 시연 스크립트·문서. 보관45초/목표30초/허용5초, callback 단조시각 기반 근사배치. 위치버퍼 메모리, 공유쪽지와 수신이력 Room. H3와proximity를 사용하되 검증 실패 시 완료라 하지 않는다.

S0: 상세 설계·원본 Mermaid·앞선 실험 결론 기록을 커밋한다. 구현 숫자는 프로젝트 초기값이며 실차 정확성을 보장하지 않는다.
S1~S6: 진행 중. 자동검사·런타임·미검증을 아래에 별도 기록한다.

S1 PASS: 실제 Pleos2.1.0/user10에 앱·계측 설치. SDK1.1.3 register 후 ADB 밖→안→밖 주입에서 ENTER/EXIT, remove 확인. ProximityTest 1개 PASS(27.068초). 첫 계측은 JUnit 메서드 반환형 오류로 실패, Unit 명시 후 수정 재실행 PASS. 지원 개수/차단·정차·이미안등록은 별도 미검증. applicationId를 기존 기능 브랜치와 동일하게 정정했으며 예전 com.jinnayeong.wakeup 데이터는 자동 이전하지 않음.

S2 core PASS: LocationBufferTest 4개. callback 시각 기준45초 보관·30초 준비·최근접선택·5초 신선도·큰공백초기화·미터거리/경로검증. 패널은 데이터 계층과 ViewModel 연결 뒤 별도 UI 커밋에서 구현한다.

S3 PASS: Room2.8.5/KSP2.3.13/H3-Java4.5.1 빌드. 첫 기기계측은 H3.so가 Java리소스로 제외돼 실패; Gradle Copy로 Android ABI별JNI 추출·newSystemInstance 사용 후 재실행 2테스트 PASS(0.151초). request멱등/충돌,동시8개receipt1행,자기제보/만료제외,cascade삭제,H3 경계포함 cover 검증. generated schema1.json 버전관리. Room import/동기화 서버는 없음. Kotlin프로퍼티 기반 컬럼명 및 nullable displayedAt으로상태파생해 문서의중복state컬럼은 생략.

S4/S5 core: DemoController가 세션/입력snapshot/과거anchor/직렬쓰기와 SDK 등록수명을 소유하고 ViewModel은 UI이벤트와StateFlow만 노출한다. 위치·H3계산·Room은 UI에서 호출하지 않는다. SDK ENTER 중심 + 등록당시반경안/지연위치 보완검사. 최초A/B실제SDK 통합계측 PASS(56.673초), 후속방향/이벤트필터수정은 최종계측으로 재검증한다. H3는res9,고정서울경로400m bounding box cover+이웃ring으로경계포함,16개SDK등록초과는표시하며공식한도보장아님.

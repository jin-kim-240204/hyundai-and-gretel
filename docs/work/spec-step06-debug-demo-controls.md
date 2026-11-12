# 06 디버그 시연 도구 스펙 작성

기준 fdd9bbf/부모 fdc51fa. 사용자 전달:05 독립 재검토 PASS·기존 P2 두 스레드만 해결, 리뷰어 직접12 JVM PASS/원 ACK 반례 포함. 작성자가 이번에 해당 JVM/리뷰/resolve를 다시 실행한 결과가 아니다. 이번 범위는06 스펙·00/05 상태·이 기록/보고서와 PR 리뷰 요청뿐이다. 코드/의존성/DBschema/부모 정본 변경 없음, 기존 사용자 polling·문서·미추적 와이어프레임 보존.

## 읽은 자료와 적용

pleos-connect에서 시작, android-compose-architecture/pleos-design/commit/pr·ponytail의 기존 세션 규칙 적용. 00/정본·03/04/05 연결 계약, lessons-learned, 로컬 미추적 pleos-road-notes-wireframes의 제품/패널/규제/Figma미확인/충돌절을 읽었다. 원격 부모 fetch로 fdc51fa 확인, 현재 implementation-plan과 차이 없음. ae11722의 debug DebugGps·debug-start-positions·fix-console-gps-mode·RoutePlot을 필요한 범위만 읽었다. 과거 PASS를06 PASS로 쓰지 않는다.

- ENV-02: debug/release source set·의존성 불변을 유지하는 계획. release mock/HTTP/특례설정 제외, 실제 SDK를 조용한 fake로 대체하지 않음.
- VEH-03 / DOC-1ba06876919b#초기화해제: 전환 시 이전 입력Job·소유provider·접근·동일위치리스너·release 순서, 새SDK 및 실제처리시점 소유권 검사 계획. SDK1.1.3의 기존 실제소스 확인을 재사용하며 새 source/command/measurement ID가 있다고 발명하지 않음.
- DOC-402de45b9f3d#권한 / DOC-9844e9eaad3c#콜백: SDK dangerous 위치권한과mock app-op 구분. 이전캐시/명령목표를버퍼에직접넣지않고 실제SDK콜백gate를계획. 권한/정리/전달 실패별 노출, 자동외부/fake fallback 금지.
- PRIV-01: 명시reset의현재시나리오삭제만제안·legacy/다른소속보존. 원시위치메모리/로컬원본·이력경계 유지,실제데이터소속추정/무단삭제없음.
- DES-04/06: 제품/패널·앱window/시스템레이어 분리·주행같은구조·명시확인ACK보존. Figma내부/공식토큰·도해실측·대비·주야간·주행적합은미확인. 무조건주차전용/준수완료를주장하지않음.

공식 근거: 기존 Pleos 권한/초기화/위치 가이드·와이어프레임 원문 추적 재사용. 보충 Android API https://developer.android.com/reference/android/location/LocationManager 의 add/remove/setTestProviderLocation 및 mock app-op 예외/같은이름provider대체를 웹 원문 확인(2026-11-12). https://developer.android.com/develop/background-work/background-tasks/awake/screen-on 의Activity flag/가시성/해제 역할 확인. 문서에 링크했으며 Android API지원이Pleos실제전달보장인것처럼쓰지않음. 기존Pleos규칙인벤토리에새Pleos페이지추가없음.

## 설계 결과와 필수 리뷰 쟁점

명시 WEB_CONSOLE/EXTERNAL_ADB 양쪽 실제SDK통과, source정리·권한오류·소유권, B600m/전체원본축, 수락쓰기중role/reset/모드차단과일반중지허용, two-point 캐시gate 및 한계, nullable v4+활성시나리오/확인reset, release 제외와최소UI범위를스펙에기술했다.

정본 충돌은06 §12에 공개: B300m/REPLAY 대신600m/SDK경로, A0m 대신600m 출발 제안(R1내최초제보도B출발가능), 현재시나리오만reset·legacy보존, gate25m/5m/15초·D한정/R오류노출, 와이어프레임의주행규제일괄잠금과정본의동일화면시연 정리. 아직 확정/부모변경/코드선행하지 않는다. 정식console기어값/현재환경경로 일치와 같은위치주입SDK콜백정책은 추가구현/허용실행에서확인해야 한다.

## 검증과 다음 행동

문서만 변경하므로 JVM/Gradle/계측 컴파일은 N/A. 오늘 앱/Studio/adb/기기/에뮬레이터/계측을 실행하지 않는다.06 runtime·콘솔/mock/캐시gate/Roomv4/reset/배치/FGS/화면유지/E2E는 NOT_TESTED. 재현절차·미검증 수용기준은06 §11에 명시한다. 정적검사 후 docs커밋/push·PR4 AI스펙작성자/고정SHA/정책리뷰요청 게시, 독립스펙리뷰 대기.

최종 정적 결과: PASS 하네스(133원문/27게이트)·문서링크/코드펜스·한국어/기술식별자 확인·diff --check·부모정본불변·기존polling외코드변경없음. 보고서exit2는 VEH-03/DES-04/06 NOT_TESTED 유지다. 역할/모드선택은정지·이력조회상태로끝내고명시시작/위치설정에서만gate를수행하도록구분해대상없는B이력조회도가능하게했다. JVM/빌드/앱실행은N/A 또는 금지에 따른 NOT_TESTED로 유지한다.

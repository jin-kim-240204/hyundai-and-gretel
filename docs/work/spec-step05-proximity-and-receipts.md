# 05 접근·수신 스펙 작성 기록

범위: 사용자 지시에 따라05 스펙·관련 단계 상태·이 기록만 작성. 코드/의존성/스키마/부모 정본 수정 없음. 독립 스펙 리뷰 전 구현 금지. 기존 변경 보존. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지. 적용 스킬: pleos-connect, pleos-vehicle, android-compose-architecture, ponytail, commit/pr. 별도 에이전트 생성 없음.

## 근거와 적용 규칙

- ENV-02: 현재 ComponentActivity/Compose·서비스/Main 구조 유지. SDK버전1.1.3의 실제 소스를 확인, 의존성 변경하지 않음.
- VEH-03 및 DOC-1ba06876919b#초기화해제: 지원 확인/initialize·등록·동일 리스너 해제·release 대칭. 위치/접근은 기존 SDK 인스턴스 소유, release 후 재사용 금지. 초기화/해제 원문 2026-11-12 웹 재확인.
- DOC-402de45b9f3d#권한: dangerous FUSED_LOCATION 권한·null/privacy/오류 처리, Android Google 위치 정책과 별개. 권한 원문 웹 재확인. proximity 권한은 배포물 양쪽 @RequiresPermission/checkPermission으로 보완.
- DOC-9844e9eaad3c#콜백 / DOC-ba9b83fc86f6#오류: nullable 위치와 오류를 유효 좌표로 만들지 않음, 콜백 진입 시각과 실제 처리 세션 검사. 위치/오류 원문 웹 재확인.
- PRIV-01: 좌표/발화 원문 로그·업로드 없음. 원시 버퍼 메모리, 원본과 로컬 receipts 분리·legacy 보존. 새 실위치 제품 모드 없음.
- 원문 URL은05 §2에 연결. 새 공식 페이지 도입 없음(기존 source-map 재사용). 설치된 FusedLocation1.1.3 sources.jar SHA 경로65744a9894172fee820bae4499ab1f99008d8cbc와 fused-location-data0.0.3의 LocationData를 읽음. add/remove, request/listener/event, LocationServiceManager.release/late callback 구조 정적 대조. public 웹 가이드에 proximity 세부 설명이 없다는 한계를 명시.

## 읽은 현재 계약

00/03/04 및 정본의 최신03/04 보강절, 실제 currentCandidates/원본/DBv2/서비스 제출 수명, ae11722의 RoadNote/NoteDatabase·lessons-learned를 대조했다. 원격 spec-road-note-system을 fetch해2ef41ce 확인: 현재 정본 파일과 동일 여부 정적 검사 예정. 과거 구현 코드를 복사/병합하지 않음. 과거 기기 PASS를 이번 단계 PASS로 쓰지 않음.

04 수정21783e9 독립 재검토 PASS·원Probe 동일소스 PASS·P2 resolve는 이번 사용자 전달 사실로 상태 반영했다. 이번 작성자가 독립 검토/resolve를 다시 수행했다는 뜻이 아니다.

## 설계 결과와 리뷰 쟁점

- 가까운16개·pending 포함·이미 안 보완·ENTER 후150m/전방/TTL/자기/기수신 재검사, 취소된 자신의 context와 요청 식별, late ID/해제 실패 한계.
- 원본 경로3컬럼 nullable·v3 명시 migration/legacy 보존, receipts 복합키/FK/한 JOIN/ACK·동시 중복·수락된 쓰기 서비스 수명.
- SDK 미터accuracy 없음, heading 품질 추정 금지. 합성 설정 방향과 경로 투영 띠를 측정값과 구분함. 정본 accuracy20m 충돌은 필수 리뷰 쟁점으로 남김. 승인되기 전 코드 작성 금지, 승인 후 부모 정본 별도 커밋·동기화 필요.
- 2초 수신 신선도와04의5초 후보 신선도를 구분. 실제거리 가까운순과 역방향 전방 부호도 부모 명료화 제안.06 UI/GPS는 계약만 기술.

## 검사와 인계

PASS/NOT_TESTED 결과는 아래 최종 검사 후 기록한다. 문서만 변경하므로 JVM/Gradle 재실행은 N/A. 앱/Room/SDK/서비스/JNI/화면 실행은 NOT_TESTED. 다음 행동: 독립 스펙 리뷰→리뷰이 판단→정본 충돌 확정·부모 반영→재검토. 이번에는 docs 커밋/push·PR4 AI 스펙 작성자 리뷰 요청 게시 후 대기, 구현/05 완료 보고/06 진행 없음.

최종 정적 검사: PASS 하네스(133원문/27게이트),05 로컬 링크·코드펜스, diff --check, 원격 부모 정본과 현재 implementation-plan 파일 차이 없음. JSON 보고서는 최초 개별 DOC 항목 누락으로 FAIL→해당 초기화/해제 원문 추적 항목 추가 후 형식검사 통과. 최종 exit2는 VEH-03 NOT_TESTED를 유지한 결과이며 런타임 완료를 뜻하지 않는다. 문서의 정책 충돌은 미확정으로 명시했다. 앱 코드/의존성/부모 정본 변경 없음.

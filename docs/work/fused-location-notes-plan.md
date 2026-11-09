# 위치 쪽지 브랜치 상세 플랜 작성

상태: 설계 리뷰 초안. 제품 구현/검증 완료 아님.

범위: docs/plans/fused-location-notes-plan.md. 스프린트 S0~S6, SDK 실측 기반 정책 제안, Room 두 테이블·키·관계·인덱스·DAO·트랜잭션·복구·마이그레이션·서버 교체 경계, Compose debug 패널, 가상 주행 시연을 작성했다. 전체 정본은 아직 변경하지 않았으며 상충 정책은 S0에서 리뷰 후 수정한다.

선택 규칙: ENV-02(일반 Activity 유지), VEH-01/03(권한·수명), PRIV-01(임시 위치 최소 수집), DES-04(시스템 영역·윈도), INT-01(Gleo 입력/응답). pleos-connect/vehicle/design 및 ponytail 스킬 적용. 스킬이 참조한 android-compose-architecture 파일은 저장소에 없어 기존 책임 분리와 공식 Android 데이터 계층 문서를 참고했고 구현은 하지 않았다.

원문 검토: DOC-c5e08a80ebfd#SDK: 공식SDK1.1.3 의존성; DOC-402de45b9f3d#권한: dangerous 및 차단과 권한 거절 구분; DOC-1ba06876919b#수명: 동일 listener 해제, release 후 새 인스턴스; DOC-9844e9eaad3c#위치: null/error 및 마지막 위치를 새 측정으로 오인하지 않음; DOC-ba9b83fc86f6#오류: 기능제한; DOC-72a5d7ab4ebb#최소수집: 위치 목적/최소 로그; DOC-e12e49cc3efc#레이아웃 및 DOC-189d894e8124#윈도: 가로/세로/분할과 시스템 UI 유지. 대응은 플랜§2~8에 기록. 원문 URL은 각 로컬 sources 및 플랜 링크에 연결.

사용자 정정: 서버 이전은 공유 쪽지 저장/조회 대상. 위치 버퍼는 메모리, 수신함은 로컬 기본안. 메모리는 프로세스 생존 범위이고 백그라운드 수집과 별개. 현재 가시 앱 시연 정책 및 백그라운드 필요 여부 미확정을 플랜§10에 명시했다.

검증: 문서 정적 검사와 diff 검사만 실행. SDK 기존 spike PASS는 별도 기록을 참조, 이번 설계로 제품 동작·Room 제약·UI·백그라운드가 검증된 것은 아니다(NOT_TESTED). 커밋/푸시 없음.

사용자 요청으로 브랜치 흐름 Mermaid를 diagrams/fused-location-notes-flow.mmd로 추출하고 SVG로 렌더해 플랜에서 이미지로 표시했다. Mermaid 렌더/XML 파싱 PASS. SDK 임시 실험 파일은 삭제하고 결론 문서는 유지했다.

사용자 요청으로 잘못 출력된 fused-location-notes-flow.svg를 삭제하고 플랜의 SVG 참조도 제거했다. Mermaid 원본은 유지한다.

## H3·SDK proximity 방향 반영

사용자 합의: anchor 좌표 + H3 검색 키로 저장, 현재/주변 셀의 쪽지를 조회해 SDK proximity 등록, ENTER 후 경로·방향·만료·중복 검사. Room에서는 Flow, 서버에서는 구역 변경 즉시 fetch + 약10초 폴링 초안. 위치 버퍼는 메모리, 수신 이력은 로컬 유지.

변경: 상세 플랜§1/3/5/6/7/8 및 신규§11, Mermaid 원본. 공유 정본§8에 기존안과 최신 브랜치 설계의 차이를 명시했다. H3 해상도·조회범위는 미확정, 150m+폴링중 이동거리+지연여유 조건과 셀 경계 테스트를 정의. 공유 쪽지 Repository와 로컬 ReceiptStore 책임을 구분. 생성시H3는현재차량이아닌과거anchor에서산출한다.

VEH-03 / DOC-9844e9eaad3c#콜백 및 DOC-1ba06876919b#수명 요구에 대응해 SDK 등록/해제·generation·오류정책을 확장했다. proximity 함수·ENTER/EXIT는 앞선 공식샘플/API 검토 근거만 유지하고 이번 런타임은 NOT_TESTED. H3 공식 indexing/traversal/통계 원문을 확인해 gridDisk k는 미터가 아님과 평균 셀 크기만으로 범위 보장 불가를 기록했다. 새 Pleos 규정 판정은 하지 않았다.

PASS: verify_harness 정적 검사, git diff --check, 설계 필수 항목/도표 간 일치 점검. NOT_TESTED: H3 Android ABI, SDK ENTER/EXIT/등록한도/이미반경안, Room 제약, 화면/주행 E2E. 문서만 변경해 빌드·제품 테스트는 N/A. 사용자 요청대로 SVG 생성과 실험 JSON 추가는 하지 않았다. 커밋/푸시 없음.

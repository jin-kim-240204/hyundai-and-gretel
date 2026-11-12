# 주변 후보 6km · 수신 500m · H3 해상도7

## 범위와 결정
- 사용자 합의: 후보 조회 반경6000m, 수신 반경500m, H3 해상도7. 가까운 전방16개·경로/방향·TTL·자기 제외·기수신 제외·늦은 콜백/쓰기 수명과 명시 ACK는 유지한다.
- 기존 사용자 polling/문서는 보존한다. 미출시·보존 DB 없음 결정에 따라 Room 버전1 유지, 과거 인덱스 이관/호환 로직은 추가하지 않는다. 현재 테이블/인덱스/열 구조 자체는 변경 불필요다.
- 저장 H3와 조회 H3는 같은 해상도7을 쓴다. 후보의 실제 haversine 6km 필터를 유지하고 SDK 반경과 앱/저장 수락 반경500m를 일치시킨다.
- 위치 입력 어댑터가 쪽지 정책을 import하지 않도록 ProximitySource의 등록 인자로 반경을 전달한다. 수신 정책은 ReceiptStore의 공간 판정과 같은 곳에 한 값으로 둔다.
- 현재 후보 조회 중심의 서울 고정 구역 및 합성 시연 경로는 확대하지 않는다. 구역 경계 바깥6km의 원본도 조회해야 한다. 실제 경로 조건을6km로 완화하는 작업은 아니다.

## 근거/검증
- 적용 ENV-02/VEH-03, clean-code6/9/24/25/31. SDK API/권한·등록/해제 순서는 변경하지 않고 요청 반경만 변경한다.
- H3 공식 https://h3geo.org/docs/core-library/restable/ 의 해상도/면적 표: 해상도7 선택은 프로젝트 정책이다.
- H3 공식 https://h3geo.org/docs/api/regions/ polygonToCells 절: 셀 중심 기반 포함 판정이므로 기존 polygon 결과에 이웃1 ring을 포함하는 보수적 cover 유지, 실제 거리로 후처리.
- 실제 H3 native JVM으로 고정 중심 격자/셀 경계/6km 원둘레 커버, 6km±경계 거리 필터, 500m 전후·역방향/반경 안 보완/16상한·DB 수신 조건을 회귀 검사한다.
- 관련 JVM/양variant/계측APK compile/lint/harness/schema/release static. 실제 SDK 500m ENTER와 실제 주행은 별도 검증, host Fake 성공과 구분한다.
- 공통 정본은 spec-road-note-system의 소유다. 기능 브랜치에서는 작업 기록과 단계별 스펙/overview에 최신 정책을 명시하며, 정본의 별도 반영/merge는 커밋 요청 시 구분한다. 과거 실험/리뷰 기록을 새 결과처럼 일괄 치환하지 않는다.

## 결과와 새 구현 PR
- 수정 전 변경 정책 회귀 5개 중4개 FAIL 확인(resolution/6km 커버/후보 필터/500m). 수정 후 전체 JVM102 PASS. 기존400m 전제 fixture2건은 6km/500m 의미에 맞게 바꿔 재검사했다.
- SDK1.1.3 sources.jar의 ProximityAlertRequest는 Float 미터 반경, 양수 유한값을 검증한다. add/remove/콜백 수명은 그대로 두고500f를 명시 전달한다. Fake 요청의 실제 전달 반경500f를 단언했다.
- native H3 해상도7/6km 커버(지원 구역 중심과 그 바깥 원둘레)/6000±0.01m/500±0.01m/역방향/가까운16개/반경 안 보완/ACK/reset host 회귀 PASS.
- 사용자 후속 지시로 새 브랜치 ny/feat-fused-location-message를 origin/spec-road-note-system에서 분기했다. 새 폴더 /Users/jinnayeong/hyundai-challenge/project/hyundai-and-gretel-fused-location-message. 기존 PR4와 원본 폴더의 dirty polling/문서는 보존한다. 새 브랜치에는 임시 polling을 넣지 않았다.
- 새 브랜치는 앱 구현/테스트/Room JSON/빌드 설정만 선택해 옮기고 Kotlin/Gradle/XML 등 코드 주석을 제거했다. 문자열/URL/raw SQL은 보존하는 lexer로 처리하고 컴파일·회귀 재확인. 문서 변경0, 코드주석 제거 idempotence PASS. 정책 정본 변경은 이 사용자 지시에 따라 PR 본문의 최신 정책으로 명시하고 문서 변경을 PR에 포함하지 않았다.
- PASS: 주석 제거·임시 polling 제외 상태 JVM102(실패0/skip0), debug/release, 계측APK compile, lintDebug, harness, Manifest/schema/release static, diff 공백. DB 스키마 구조 변경 없음/버전1. 로그 /private/tmp/fused-location-message-ready.log.
- NOT_TESTED: 이 새 브랜치/500m 정책의 실제 SDK ENTER, 실차 및 주행 A/B. 앱 재설치·기기 DB 초기화는 하지 않았다.
- commit 0a9c39e0816fbf0af5b50f1a61cfefa0d90b63f1 push. PR6 https://github.com/jin-kim-240204/hyundai-and-gretel/pull/6 (base spec-road-note-system, draft). PR 본문에 Mermaid 흐름/ERD와 핵심 정책만 작성했다. 원격 파일92개/문서0/다이어그램2 확인. 새 worktree clean.

# 04. H3 인덱스와 주변 쪽지 후보 갱신

상태: **04 수정 `21783e9` 독립 재검토 PASS·P2 resolve(사용자 전달) / 실제 기기 검증 NOT_TESTED**. [05 접근·수신 스펙](05-proximity-and-receipts.md) 리뷰로 인계한다. 자신의 관찰 context 검사와 세 catch 방어·원 반례 통과를 포함하며 raw snapshot 소비 금지 계약은 유지한다. 현재 통합 결과는 [07 기록](../../work/step07-integration-verification.md)을 따르며 기존 runtime 미검증은 유지한다.
정본: [시스템 설계](../../plans/implementation-plan.md), [전체 단계](00-overview.md), [03 저장](03-save-road-note.md), [조사 교훈](lessons-learned.md).

## 1. 만들 결과와 단계 경계

유효한 현재 좌표에서 H3 res9 주변 셀을 계산해 Room 원본을 조회하고, 실제 거리400m 이하·미만료인 후보를 가까운 순으로 제공한다. 현재 cover의 Room Flow로 정지 중 새 원본도 반영하며10초 폴링은 도입하지 않는다. 기존45초 메모리 버퍼·30초 anchor·5초 신선도·고정TTL600초·noteId PK 단순 생성·동일ID거절·자동재시도없음·수락 저장 수명은 유지한다.

04 출력은 **공간 후보**이지 수신 대상 확정이 아니다.05가 경로/진행방향/전방/작성자/기수신과150m 조건을 검증하고 proximity를 등록한다.04에서는 SDK proximity·ENTER/EXIT·수신이력·표시ACK·16개 등록 제한·B 전환·웹콘솔·새 지도/UI·Gleo·서버를 구현하지 않는다. 실제 도로 식별이나 H3만으로 같은 도로/방향임을 보장하지 않는다.

현재03에는 좌표/offset/UTC만 있고 routeId·direction·progress·accuracy가 없다.04에서 계산 가능한 **H3 인덱스만** 저장 확장하고 경로/방향/품질 필드는05에서 입력 근거·migration을 별도 합의한다. 현재 좌표만 보고 쪽지 작성 당시 방향을 만들어 저장하지 않는다. 이 단계 구분은 독립 리뷰에서 먼저 확인한다.

## 2. 조회 계약과 상태 소유

```mermaid
flowchart TD
    Fix[추적 세션의 유효 현재 좌표] --> Cover[res9 셀 · 주변 셀 집합]
    Cover --> Refresh[최초 위치 · cover 교체 · DB 변경]
    Refresh --> Query[Store의 주변 원본 조회]
    Query --> Filter[중복 제거 · TTL · 실제400m 거리]
    Filter --> Sort[실제 거리 오름차순 · noteId tie break]
    Sort --> Snapshot[현재 세션·위치에 속한 후보 snapshot]
    Snapshot -. 05 계약 .-> Pickup[경로·방향·자기/기수신 검사 · SDK 등록]
    Stop[중지·SDK 오류·null·stale] --> Clear[조회 취소 · 후보 사용 금지]
```

- Store에 셀집합으로 공간 원본을 관찰하는 좁은 Flow 계약을 추가한다. SQL은 cover 셀 또는 null legacy 행을 관찰하며 조회 UTC를 고정 필터로 바인딩하지 않는다. Entity/DAO/H3Core는 서비스/ViewModel에 노출하지 않는다. 공통 BaseRepository/네트워크 인터페이스/캐시 프레임워크는 없다. 기존 create/findById 계약을 보존한다.
- 후보 갱신 작업은 추적 서비스 수명에 속한다. 같은 목록을 화면/ViewModel/서비스에서 각각 수집하지 않는다.04는 불필요한 화면을 추가하지 않고 이후05가 읽을 상태만 제공한다.
- 상태는 WaitingForLocation / Loading / Ready(snapshot) / Failed(reason)처럼 관찰 상태를 드러낸다. Ready는 마지막 평가 자료이며 시간이 지나도 유효한 후보 목록임을 보장하지 않는다. snapshot은 조회세션·셀집합·사용한좌표·원본목록·조회UTC/수신단조시각을 포함하는 불변 결과다. 구체 타입은 구현 리뷰에서 최소화한다.
- 실패는 성공한 빈 후보와 다르다. 실패/위치 없음에서 이전 셀의 자료를 현재 후보로 사용하지 않는다. 마지막 성공 결과를 보관하더라도 사용 가능 후보로 노출하지 않는다. 실패 원인은 H3_UNAVAILABLE / INDEX_ERROR / STORAGE_ERROR 수준으로 분리하고 debug에 작업명·예외타입만 기록한다.

### 소비 경계 — raw snapshot 직접 사용 금지

05는 raw Ready/목록을 등록·수신 대상으로 직접 읽지 않는다. 같은 Main 문맥에서 중간 suspend 없이 현재 세션 일치·현재 위치 유무·수신단조시각 신선도5초·현재 UTC의 TTL·실거리400m를 다시 검사해 불변 후보를 반환하는 좁은 진입점만 사용한다. 조건 불충족에서는 유효 후보를 반환하지 않는다. ready 목록에 시간이 지난 행이 남아 있어도 소비 결과에서 만료는 제외해야 한다. 이를 명확히 하는 대신 만료 전용 wake-up은 필수 구현하지 않는다.05의 실제 등록/수신 직전 재검사는 별도 책임이다.

## 3. 셀 범위와 경계 누락 방지

H3는 res9로 고정한다. 셀 ID는 H3 표준 주소 문자열이며 외부/UI가 입력하지 않고 검증된 좌표에서 앱이 계산한다. 평균 셀 면적/평균 변 길이를400m 반경 보장이나 일정한 셀 지름으로 사용하지 않는다. gridDisk의 k도 미터가 아니다.

후보 중심의 허용 영역은 위도 **[37.4900,37.5100]**, 경도 **[127.0200,127.0500]** 닫힌 사각형이다. 이번 프로젝트가 선택한 합성 진단 범위이며 서울 행정구역/실도로 지원 보장이 아니다. 기존03 원본 저장·보존은 이 영역으로 제한하지 않는다. 중심이 범위 밖이면 후보 사용 불가로 명시하지만 원본을 삭제하거나 저장을 거절하는 근거로 쓰지 않는다.

검증 여유 영역은 위도 **[37.4800,37.5200]**, 경도 **[127.0100,127.0600]**이다. 중심 경계에서도400m 원 전체를 cover로 조회하며, 원본 좌표를 중심 허용 영역으로 잘라 경계 밖 후보를 누락시키지 않는다. 같은 haversine oracle(R=6,371,000m)로 중심별400m 원이 여유 영역 안에 있는지 확인한다.

초기안은 ae11722의 평면 근사 사각형을 보수적으로 구성해 `polygonToCells` 중심 포함 셀에 `gridDisk(cell,1)`을 더하고 현재 셀도 포함하는 방식이다. 중복 제거·정렬한다. 이 근사를 전국/날짜변경선/극지/오각형 전역 보장으로 확대하지 않는다. 선택 영역의 바깥 후보까지 실제로 덮는지는 아래 회귀 근거로 확인한다.

이 방식의 경계 보장은 아직 검증되지 않았다. 구현 중 실제H3 JVM으로 격자·중심영역 네모서리/변·셀변/꼭짓점·대각선·원둘레·399.99/400/400.01m의 cell 포함 여부를 확인한다. fixture 수치 오차(예:1e-6m)는 별도 기록하고 제품의 <=400m 반경을 늘리는 허용오차로 쓰지 않는다. 유한 fixture 관측 PASS를 연속영역 모든 점의 무누락 증명이라고 하지 않는다. 구현자가 편한 몇 중심점만 검사하지 않고 경로 구간을 가로지르는 격자/경계 기반 fixture와 전체 좌표 거리 oracle을 대조한다. 누락이 나오면 cover를 보수적으로 확장해 수정하고 근거를 남긴다. 여분 후보는 허용하되400m 이내 유효 원본 누락은 허용하지 않는다. 단순 평균 변 길이로 k를 선택하는 대안은 채택하지 않는다. 누락이나 셀/SQL 입력 상한 문제가 생길 때만 v4.5.1 polygonToCellsExperimental의 containment_overlapping/containment_overlapping_bbox 대안과 보수적 확장을 같은 oracle로 비교해 단일 방식을 고른다. 반례 없이 복수 구현·비교를 필수화하거나 제품 fallback으로 함께 유지하지 않는다. Java 소스 선언과 선택 artifact/JNI 실행 가능성은 별도 확인한다.

이후 조회 결과는 위경도 기반 지표거리로 다시 필터한다. 거리는 검증된 좌표의 haversine 등 하나의 순수 함수로 계산하며 `distance <= 400m`을 포함한다. H3 결과 순서·셀 중심 거리·문자열 순서는 실제 근접 순서가 아니다.400m는 프로젝트 후보 정책이며 SDK 반경150m와 분리한다. 구면/평면 도로 고도 차이는 지원하지 않는다.

## 4. 관찰 갱신·동시성과 시간 경과

- 첫 유효 위치에서 cover의 Room observable query를 서비스가 한 번 관찰한다. cover/세션 교체 시 이전 구독을 취소·무효화한다. DB 변경은 Flow 재조회를 통해 정차 중 새 원본을 반영한다. 최초/cover 교체/DB 변경/유효 위치 변경이 계산을 깨운다. 별도10초 폴링이나 커밋 신호 루프를 중복 도입하지 않는다.
- 원시 위치 콜백은 모두 기존 버퍼로 전달한다. 후보 계산만 최신 위치와 결합하고 기록 이벤트를 conflate하지 않는다. 취소된 SQLite 조회가 즉시 끝난다고 가정하지 않고 결과 적용 직전 현재 세션·cover 키를 확인한다.
- SQL은 공간 셀+legacy null 행만 관찰한다. 영구 바인딩된 조회 시작 UTC로 이후 만료를 보장하지 않는다. Flow 결과 적용 직전에 새 UTC·최신 위치 신선도·실거리로 재평가하고, 소비 진입점에서도 반복한다. 느린 DB 응답이 TTL을 넘었다면 처음 조회 시 유효했어도 소비 후보에서 제외한다.
- 같은 cover 내부의 유효 위치 변경도 저장된 공간 원본과 결합해 거리·순서를 다시 계산한다. 셀이 같아도 cover가 달라지면 구독을 교체해400m 안 원본 누락을 막는다. 후보 중심이 지원 영역 밖이면 현재 소비는 허용하지 않는다.
- Room Flow는 시간 경과만으로 방출하지 않는다. 콜백/DB변경 없이 TTL이 지나도 raw snapshot은 당시 자료일 수 있지만 현재 UTC를 읽는 소비 진입점은 `nowUtc < expiresAt`만 허용한다. 만료 전용 wake-up은 추가하지 않는다.05도 등록/수신 직전에 별도 재검사한다.
- 신선도는 수신단조시각 기준5초 이내다. 콜백이 없어도 stale 기한에는 WaitingForLocation으로 전환한다. 소비 진입점이 매번 단조시계를 다시 확인하므로 전환 스케줄이 늦어도 stale 후보를 반환하지 않는다. 세션이 기한 작업을 소유하고 중지·재시작·null/잘못된 좌표·SDK 오류 뒤 이전 결과를 차단한다. 수락 저장은 기존03 수명대로 마무리하며 후보 구독 정리와 혼동하지 않는다.

## 5. 가까운 순과 방향의 책임

04는400m 내 원본을 **실제 거리 오름차순→noteId**로 정렬하고 임의16개로 자르지 않는다. 앞/뒤·동/역방향 이동에서도 가까운 원본이 먼저이며 위경도나 progress의 단순 오름차순을 거리로 사용하지 않는다. 반대 방향·이미 지난 원본도04공간 후보에 남을 수 있음을 계약에 명시한다.

05에서 도로 문맥이 확정되면 같은 경로·방향을 검사하고 `aheadMeters=(noteProgress-currentProgress)*directionSign`으로 전방만 남긴 뒤 전방거리 오름차순으로 등록한다. ae11722의 역방향 먼 쪽지 우선 버그와 수정 교훈을 재사용하며 정/역방향·동거리 tie를 회귀로 둔다. 방향을 모르면 임의 FORWARD로 채우지 않는다.04가 후보에 넣었다는 사실은05 수신 허가가 아니다.

## 6. Room v2와 기존 원본

이미 새 원본을 저장할 수 있는03이 있으므로 이전의 ‘사용 DB 없음’ 승인을 재사용하지 않는다. v1 export를 보존하고 **v2 명시적 migration**으로 nullable `h3CellId TEXT`와 이 컬럼 조회 인덱스를 추가한다. 전체 원본·ID·좌표·내용·UTC는 보존하며 destructive migration/앱 시작 시 삭제/같은버전 덮어쓰기를 금지한다. 필드는 Entity 내부 파생 인덱스이며 현재 RoadNote/화면 입력9개 필드의 의미는 유지한다.

- 신규 create는 저장 경계에서 좌표 검증 후 H3를 계산해 INSERT에 함께 넣는다. 외부가 임의 H3를 지정하지 않는다. H3 계산/로딩 실패는 INSERT 전 명시적 실패이며 성공을 반환하지 않는다. 기존 저장 실패 결과와 debug 진단으로 구분 가능한 최소 연결만 검토한다.
- 기존 v1 행은 h3CellId=null로 남긴다. 후보 SQL은 `h3CellId IN(cells) OR h3CellId IS NULL`조건으로 시간 필터 없이 관찰하고, null 행은 조회 매핑에서 좌표→H3를 계산해 cover와 실제 거리로 필터한다. 후속 무조건 전체 backfill/변경 트랜잭션은 추가하지 않는다. legacy 수가 작다는 프로토타입 한계이며 서버/대용량 전환 때 backfill을 별도 설계한다.
- null legacy를 조용히 제외하지 않는다. invalid 좌표/손상된 행은 오류로 식별하고 Ready(empty)로 숨기지 않는다. 기존 findById는 만료/인덱스 여부와 관계없이 원본 조회를 유지한다.
- SQL 조회에 셀수/입력크기 상한을 둬 SQLite bind 제한을 넘기지 않게 한다. 고정 서울 cover의 최대 크기를 JVM fixture로 확인하고, 범위 초과는 후보 사용 불가로 기록한다. 잘못 잘라 일부 셀만 조회하지 않는다.
- routeId/direction/progress/accuracy, scenarioId, receipts 테이블은 이번에 추가하지 않는다. 필요해지는05/06 스펙에서 기존 행 처리까지 결정한다. 신규 인덱스 추가가 서버 API/공간 인덱스/인증을 구현했다는 의미는 아니다.

## 7. H3 Java/JNI 도입과 실패

참고 버전 com.uber:h3:4.5.1을 우선 후보로 삼되 이 문서 단계에서는 설치하지 않는다. 구현 직전 공식 배포물/API와 현재 Gradle/Kotlin/Java/ABI를 대조해 버전을 고정한다. H3Core 생성·latLngToCell·polygonToCells·gridDisk가 실제 선택 artifact에 존재하는지 확인한다. Android는 APK lib/<abi>의 .so를 사용하는 H3Core.newSystemInstance, host JVM은 host 리소스 로더의 H3Core.newInstance를 선택 artifact/소스와 대조해 사용한다. 필요한 최소 생성 함수로만 테스트를 연결하고 공용 NativeLoader 계층은 추가하지 않는다. 생성/네이티브 로딩은 필요한 시점에 수행하고 지원되지 않으면 H3_UNAVAILABLE로 처리해 앱/기존 SDK 수집 전체를 시작 불가로 만들지 않는다. VM Error 전부를 삼키지 않고 네이티브 로딩의 예상 LinkageError/초기화 실패 경계만 다룬다. 좌표/내용은 오류 로그에 넣지 않는다.

과거4.5.1의 Android .so는 JAR 리소스에 있어 일반 dependency 선언만으로 APK의 lib/ABI에 들어가지 않았다. 선택 artifact를 정적으로 열어 Android ABI별 라이브러리·ELF 아키텍처를 확인하고, 필요한 .so만 generated jniLibs로 복사·APK merge에 연결한다. 예전의 android-arm64→arm64-v8a, android-arm→armeabi-v7a 매핑은 실제 artifact와 맞을 때만 재사용한다. Linux/macOS/Windows용 라이브러리를 Android JNI로 잘못 포장하거나 배포폴더 전체를 소스에 복사하지 않는다. 네이티브 생성 task는 debug/release 빌드와 입력/출력이 연결돼야 한다.

지원 ABI는 실제 .so가 있는 Android ABI만 선언한다. SDK/앱이 지원하던 ABI와 달라지는 점은 독립 구현 리뷰에서 확인한다. ABI 필터로 빌드만 통과시켜 설치 가능한 기기를 묵시적으로 줄이지 않는다. APK zip의 lib/<abi>/*.so 포함과 ELF/의존 라이브러리 정적 검사는 가능하지만 Android 동적 로딩·JNI 호출·16KB page size·에뮬레이터 기동 성공은 이번 실행 금지로 NOT_TESTED다. host JVM native 성공을 Android JNI PASS로 확대하지 않는다.

## 8. 검증과 단계별 종료 조건

| ID | 검증할 계약 | 허용 근거와 제한 |
|---|---|---|
| H01 | 실제H3 res9 인코딩/유효셀·왕복, 입력 거절 | host JVM native/공식 artifact; Android JNI NOT_TESTED |
| H02 | 경계·꼭짓점·대각선·400m 안/동일/밖 누락/오검출 | 서울fixture H3cover vs 전체좌표 거리oracle JVM. 영역/표본한계 기록 |
| H03 | Room Flow 변경·정지 신규원본·cover 교체·중복관찰 없음 | Fake observable Store/test scheduler; 실제Room 알림/시간은NOT_TESTED |
| H04 | A세션느린조회→B세션·중지·null·stale·오류 | 결과적용직전세션키/JVM, 이전결과재진입없음 |
| H05 | 콜백 없는 TTL 동일/직후·TTL 넘긴 느린응답·같은cover이동/stale | 현재시점 소비/적용 검사, raw직접소비없음·Failed와Ready(empty)구분 |
| H06 | 거리정렬·정/역이동·tie·중복셀 | 거리oracle/순수JVM; 방향수신조건은05범위 |
| H07 | v1→v2 보존·legacy null 조회·신규H3검증 | migration SQL/export 정적·계측코드작성/compile; 실제Room/migration NOT_TESTED |
| H08 | debug/release Android .so 패키징·ELF/ABI | assemble+zip/ELF정적; AndroidJNI·page size실로드 NOT_TESTED |
| H09 | 실패/취소·legacy오류·셀상한·재시작 | FakeH3/Store·스케줄러, 기존36개회귀; 실제SDK/Binder NOT_TESTED |

스펙 독립 리뷰→리뷰이 판단→반영·재검토 전 코드/의존성 변경 금지다. 이후04-a H3 경계/패키징,04-b v2 저장/조회,04-c 세션 후보 갱신 순으로 작은 구현·독립 리뷰를 진행한다. 같은 범위 내 회귀가 실패하면 수정 후 영향 검사만 재실행한다.04의 완성은 허용된 코드/검사와 런타임 미검증을 구분한 보고이며05수신/06시연 완료가 아니다.

## 9. 리뷰이 판단 반영과 재검토 요청

리뷰749ff7f의 P1/P2/P3/O1/O2 판단을 반영했다. P1은 raw 직접소비금지/현재시점 진입점으로 확정, P2는 명시 중심/여유영역과 경계밖 후보·03저장 보존, P3는 Room Flow 채택/10초 폴링 제거다. O1은 기존cover 실패/상한 문제 때만 overlap 대안을 비교, O2는 Android system/host resource 로딩을 구분했다.

공통 정본은 부모 설계브랜치의04 소비/지원영역 계약에 먼저 반영·병합한 뒤 이 문서를 동기화했다. 아직04 구현 승인이 아니며 독립 재검토를 기다린다. 셀 경계 실험·artifact/JNI·Room Flow/migration 실제 동작은 NOT_TESTED다. 앱/설치/Studio/adb/기기/에뮬레이터/시뮬레이터/GPS주입/계측 실행은 하지 않는다.

## 10. 구현·허용검사 인계

5a0dd01독립재검토통과후04-a/b/c를구현했다. [구현/검증기록](../../work/step04-nearby-candidates-implementation.md)을기준으로독립코드리뷰하며위의구현전문장은당시승인게이트이력이다. 실제AndroidJNI/Room/SDK/기기검증은NOT_TESTED이며유한host회귀를전영역증명으로확대하지않는다.05/06은미구현이다.

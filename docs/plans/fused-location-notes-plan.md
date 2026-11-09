# 위치 기반 쪽지 구현 플랜 — 리뷰 초안

작성: 2026-11-09 / 브랜치: `ny/feat-fused-location-notes` / 상태: **S0~S6 코드 구현, 단계별 검증 진행**.

전체 서비스 계약은 [전체 테크스펙](implementation-plan.md), 기술 실험 근거는 [SDK 검증 기록](../work/fused-location-spike.md)이다. 이 문서는 이번 기능 브랜치의 상세 설계와 스프린트 백로그다. 아래 변경 제안은 리뷰 후 정본의 해당 절에 반영하며, 상충한 두 정책을 동시에 구현하지 않는다. 스프린트는 기간이 아닌 검증 가능한 작업 묶음이다.

## 1. 목표와 현재 상태

**A가 주행하며 제보 → 앱 수신 약30초 전 위치에 Room 쪽지 저장 → B가 같은 경로로 접근 → B 수신함에 1회 등록·표시**를 노트북 한 대·Pleos 에뮬레이터 한 대에서 재현한다. 정차 시에도 위치 조건이 맞으면 동작한다. A/B는 같은 로컬 DB를 공유하는 가상 사용자이며 서로 다른 실차 간 공유를 입증하지 않는다.

설계 시작 시점의 앱은 ComponentActivity/Compose 기본 화면, Vehicle 의존성만 있다. FusedLocation·Room·스키마·Gleo Receiver는 없다. namespace는 `com.jinkim.hyundaiAndGretel`이지만 applicationId는 `com.jinnayeong.wakeup`으로 남아 있다. 기존 음성 PR의 앱 식별자·스키마·콜백 변경을 대조하고 통합할 때 일치시킨다. 기본 Manifest에는 distractionOptimized 선언도 없으므로 기존 기능 브랜치의 설정을 현재 브랜치에 이미 있다고 취급하지 않는다.

검증 완료: Pleos2.1.0 / SDK1.1.3, ADB40개 주입 →21개 실제 콜백,39.111초 수집, 최대 콜백 간격4.010초. SDK1Hz 전달 보장·accuracyMeters·측정 단조시각은 확인되지 않았다. SDK proximity ENTER/EXIT는 API 존재만 확인했다.

이번 범위 밖: 서버·인증·동기화·오프라인 업로드 큐, 실도로 지도매칭, NAMED_PLACE/DEFERRED, asking/draft, 자동 음성 경고, 상시 백그라운드 추적, 지도 SDK, 디버그 패널 라이브러리. 접근 트리거는 SDK proximity ENTER를 사용하고, 앱은 경로·방향·만료·중복을 추가 검사한다. 실제 proximity 동작 검증을 구현 전 게이트로 둔다.

## 2. 리뷰할 정책 제안

다음 숫자는 **프로토타입의 초기 제안**이며 공식 SDK 보장값이 아니다. S0에서 피드백을 반영해 확정한다.

| 항목 | 제안 | 근거·제한 |
|---|---|---|
| 목표 위치 | 제보 JSON 앱 수신 단조시각 -30초 | LLM 입력에 시각·좌표 추가 안 함 |
| 보관 | 최근45초, 추가·조회 때 정리 | 경계 앞 표본 보존.45초 미만 수집이어도30초 조건이면 사용 가능 |
| 선택 | 목표와 단조시각 차이가 가장 작은 표본, 동률이면 과거 표본 | 보간 생략. 실제 offset과 오차 기록 |
| 준비 | 현재 세션에서30초 이상 수집 + 목표 오차5초 이내 + 최신 콜백5초 이내 | 초기5초 표본만으로 준비 완료 처리하지 않음 |
| 수신 공백 | 5초 초과면 STALE, 이력 비우고 재준비 | 타이머로 판단, 새 콜백이 와야만 만료시키지 않음 |
| 샘플 시간 | 앱 callbackElapsedNanos 사용, `timeBasis=CALLBACK_RECEIVED` 명시 | 실제 측정시각이라고 부르지 않음. SDK timestampMillis는 진단값 |
| SDK 시각 역행 | 콜백 순서는 앱 단조시각으로 관리, SDK시각 역행 감지 시 오류 표시·세션 재준비 | 반복 좌표는 허용. 동일SDK시각의 반복 전달은 진단해 신규 표본 채택 여부 S1 확인 |
| 위치 품질 | 유한값·위경도 범위·신선도·시연 경로 투영으로 검증 | confidence를 accuracy미터로 변환하지 않음 |
| 경로 | 단일 평면 직선 fixture, 횡방향 이탈20m 이내, 방향은 fixture로 지정 | 이20m는 측정정확도가 아닌 경로 허용 폭. 일반 도로/방향 추론 아님 |
| 줍기 | 같은 경로·방향, 전방0~150m AND 지표거리150m 이내 | 정차/이미 반경 안도 평가, 속도·기어는 조건 아님 |
| 보존 | 쪽지 생성 후10분 TTL | 만료 시 논리 제외, 정리 작업에서 물리 삭제 |
| 화면 | 주행 상태와 무관하게 같은 앱 화면, 활성·가시 세션에서 화면 유지 | 시스템 UI·규제 우회 없음 |

5초 오차는 속도36km/h에서 최대약50m의 시간 선택 오차에 해당한다. GPS오차·콜백 전달지연은 별도이며 정확한 위험 위치를 보장하지 않는다. 45초 버퍼는 표본 부재를 해결하지 못하므로 오차 한도를 벗어나면 fail한다. callback 기준 정책 채택 시 기존 정본의 ‘측정시각30초 전’ 검증 조건을 명시적으로 변경해야 한다.

## 3. 시스템 흐름과 책임

[Mermaid 원본](diagrams/fused-location-notes-flow.mmd)

| 구성 요소 | 입력 → 출력 / 소유 상태 |
|---|---|
| LocationCollector | SDK nullable/error → LocationSample·LocationState. 동일 listener 등록/해제·release, 가시성 수명 관리 |
| LocationBuffer | session/generation 일치 표본 → 불변 snapshot·anchor 선택. Room에 궤적 저장 안 함 |
| DemoSessionController | A/B 전환·리셋 → scenarioId, actorId, generation, synthetic 동의. 단일 Mutex로 세션 변경/쓰기 직렬화 |
| CreateRoadNote | ReportInput + ReceiptContext + snapshot → SaveResult. 저장 전 입력·권한·anchor 검증 |
| RoadNoteRepository | 도메인 명령/조회·Flow 제공. Entity/DAO/SQLite타입을 외부에 노출 안 함 |
| RoomRoadNoteRepository | Entity 매핑·DAO·트랜잭션·제약 충돌을 도메인 결과로 변환 |
| NearbyNoteCoordinator | 현재 위치 → 주변 H3 셀 조회 → 쪽지 후보 목록. Room Flow 변경과 구역 이동에 반응 |
| ProximityRegistry | 후보 목록의 차집합 → SDK 위치 등록/해제. generation별 등록 상태·오류 관리 |
| PickupEvaluator | ENTER 또는 보완 검사 + 최신 유효 위치 → 경로·방향·만료·중복 검사. 저장은 저장소에 위임 |
| 화면 ViewModel | 세션·위치·저장 결과·수신함 → StateFlow UI상태, 화면에서 SDK/DAO 직접 호출 안 함 |

클래스명은 책임 제안이며 한 책임당 인터페이스/UseCase를 기계적으로 추가하지 않는다. 서버 교체 경계는 공유 쪽지용 RoadNoteRepository다. 수신 이력과 표시 ACK는 별도 로컬 ReceiptStore로 유지한다. 기존 Kotlin/Flow/Compose를 사용하고 DI 프레임워크·범용 Result 계층은 추가하지 않는다.

## 4. 시간·세션·동시성

LocationSample: `generation: Long`, `callbackElapsedNanos: Long`, `sdkTimestampMillis: Long?`, `latitude/longitude: Double`, `headingDegrees: Float?`, `confidence: Float?`, 시연용 `routeId/direction/progressMeters`. SDK 값의 null/범위는 어댑터에서 확인한다. 단조시각은 재부팅/다른 기기 간 비교 불가라 영속 절대시각으로 저장하지 않는다.

제보 진입 순간 `requestId(UUID)`, `receivedAtUtcMillis`, `receivedElapsedNanos`, actor/scenario/generation 및 snapshot을 함께 고정한다. 입력 검증 이후 뒤늦게 버퍼를 다시 읽어 위치를 바꾸지 않는다. DB 저장 시간 `createdAtUtcMillis`와 제보 수신 시간은 별도다. `anchorOffsetMillis`와 `anchorTimeErrorMillis=abs(offset-30000)`만 영속 기록한다.

A/B 전환은 generation 증가, 수집 listener 해제·새 인스턴스 초기화, 버퍼·현재 위치 폐기. Room 쪽지는 유지한다. 이전 generation의 지연 콜백/제보/수신은 폐기한다. B는 새 유효 위치만 있으면 줍기 가능하고30초 준비가 필요 없다.

세션 전환·리셋·쓰기 최종 구간은 같은 Mutex 사용. 잠금 획득 후generation 재확인 → 짧은DB트랜잭션 → 잠금 해제 순서다. SDK/콜백 전송/긴계산은 DB트랜잭션 안에서 하지 않는다. DB 스냅샷 경쟁은 트랜잭션 내부에서 만료·중복·scenario를 재검증한다.

비가시·권한철회·SDK오류·null/사용불가 위치·긴공백은 위치 유효성 해제 및 버퍼 재준비로 연결한다. 가시성은 Activity의onStop/복귀 실제 로그로 검증하며 Gleo오버레이가 수집을 끊으면 E2E차단 이슈로 다룬다. 프로세스 재시작 시 A·중지 상태로 시작, 쪽지/수신함은 복원, 위치는 재준비. UTC 점프 감지 시 수집/심기/줍기 중지 후 리셋을 요구한다. 단순시간 변경으로 오래된 쪽지를 되살리지 않는다.

## 5. Room v1 모델 — 서버 전환을 고려한 설계

### 5.1 경계와 ID

DB명 `road-notes-demo.db`, version1, exportSchema=true. 합성시연 전용 저장소다. UUID를 TEXT로 저장하고 auto-increment ID·A/B enum을 서비스 식별자로 사용하지 않는다. A/B는 화면 라벨이며 고정가상 actor UUID에 매핑한다. `authorId/recipientId`는 불투명 문자열이다. 지금은 로컬시연 신원이며 서버 권한증명이 아니다.

`scenarioId`는 시연 공간 UUID. 영속 쪽지를 통한 재시작 복원을 위해 debug 환경설정에 활성scenarioId를 저장한다. 환경설정은 DB와 원자적이지 않으므로 리셋은 **새scenarioId를 먼저 저장**하고 이전scenario행을 삭제한다. 중간종료 시 이전행이 남아도 새scenario조회에서 제외된다. 시작 시 비활성scenario 정리로 회수한다. 활성actor/generation은 메모리만 유지한다.

서버와 공통 의미를 갖는 데이터는 RoadNote 도메인 모델, 시연 필드(`scenarioId`, routeId, progressMeters, source, isSynthetic, timeBasis)는 로컬context/metadata로 구분한다. v1은 테이블 분할보다 컬럼으로 보관하되 Entity→도메인 매핑 시 별도 `DemoContext`로 묶는다. 시연경로 ID를 실도로 ID로 보내지 않는다. 사용자 테이블은 인증/프로필 정보가 없어 만들지 않는다.

### 5.2 `road_notes`

모든 컬럼은 아래 nullable 표시 외 NOT NULL. Kotlin과 SQLite 이름은 camelCase/snake_case 매핑을 명시한다.

| 컬럼 | SQLite / 도메인 타입 | 의미·검증 |
|---|---|---|
| note_id | TEXT / UUID문자열 | PK, 앱 발급, 재시도 때 재사용 |
| request_id | TEXT / UUID문자열 | UNIQUE, 동일 내부요청 중복저장 방지 |
| scenario_id | TEXT / UUID문자열 | 조회·초기화 범위 |
| author_id | TEXT / String | 신뢰된 세션에서 주입, JSON에서 받지 않음 |
| content | TEXT / String | trim후1~500자, 원본발화 별도저장 안 함 |
| category | TEXT / enum | WARNING / INFORMATION, ordinal 저장 금지 |
| received_at_utc_ms | INTEGER / Long | 앱 제보수신 UTC epoch millis |
| created_at_utc_ms | INTEGER / Long | 로컬 저장처리 시각, 서버전환 시 serverCreatedAt과 의미 대조 |
| expires_at_utc_ms | INTEGER / Long | createdAt+600000, createdAt보다 큼 |
| anchor_latitude | REAL / Double | 유한수, -90~90 |
| anchor_longitude | REAL / Double | 유한수, -180~180 |
| h3_cell_id | TEXT / String | anchor 좌표에서 산출한 고정 해상도의 H3 ID, 검색용 파생값 |
| h3_resolution | INTEGER / Int | 시연 환경에서 고정한 해상도. 셀 ID 자체와 일치 검증 |
| anchor_heading_degrees | REAL nullable / Float? | 유효값0이상360미만만 보관 |
| anchor_offset_ms | INTEGER / Long | receivedElapsed-anchorCallbackElapsed, 제안25000~35000 |
| anchor_time_error_ms | INTEGER / Long | abs(anchorOffset-30000), 제안0~5000 |
| time_basis | TEXT / enum | CALLBACK_RECEIVED, 측정시각으로 위장 안 함 |
| route_id | TEXT / String | fixture 버전포함 ID, 예 demo-r1-v1 |
| direction | TEXT / enum | FORWARD / REVERSE, fixture 진행축 기준 |
| progress_meters | REAL / Double | fixture 원점에서의 유한 진행거리 |
| source | TEXT / enum | SDK / REPLAY. 이번 기본경로SDK, replay 구현은 필요 시 별도 |
| is_synthetic | INTEGER / Boolean | v1은1만 허용. SDK입력도 합성좌표임을 표시 |

locationMode는 이번 전체가CURRENT_ROAD이므로 중복컬럼을 만들지 않고 입출력계약에서만 유지한다. observedAt/draftId/asking·anchorAccuracy 컬럼은 없다. confidence는 단위미확인이라 영속 품질지표로 쓰지 않는다. ACTIVE/EXPIRED는 now와expiresAt으로 파생, picked/publication/sync상태를 임의추가하지 않는다. 심어진anchor는 갱신하지 않는다.

인덱스: PK(note_id), UNIQUE(request_id), `index(scenario_id, h3_cell_id, expires_at_utc_ms)`. H3로 구역 후보를 줄이고 전방거리·지표거리는 앱에서 확인하므로 위경도 복합인덱스를 공간인덱스처럼 취급하지 않는다. 정리용scenario 조건은 기존 복합인덱스 선두를 활용한다. 성능증거 없이 인덱스를 늘리지 않는다.

### 5.3 `note_receipts`

| 컬럼 | SQLite / 도메인 타입 | 의미·검증 |
|---|---|---|
| note_id | TEXT | road_notes.note_id FK, ON DELETE CASCADE |
| recipient_id | TEXT | 신뢰된 세션 사용자ID |
| received_at_utc_ms | INTEGER / Long | 접근판정 수신함등록시각 |
| distance_meters | REAL / Double | 수신시 지표거리, 유한수0~150 |
| display_state | TEXT / enum | PENDING / DISPLAYED |
| displayed_at_utc_ms | INTEGER nullable / Long? | PENDING이면null, DISPLAYED면필수 |

복합PK `(note_id, recipient_id)`로 사용자별1회수신 보장. 원본쪽지를삭제/소비하지 않으므로 C도 수신가능하다. scenario는 부모쪽지와JOIN해 가져오며 복제하지 않는다. 인덱스 `(recipient_id, display_state, received_at_utc_ms)`, FK선두 note_id는 복합PK로 커버한다. 정렬은 receivedAt+noteId로 고정한다.

표시이력은 접근등록과 분리한다. UI에 렌더된 뒤 조건부UPDATE로 DISPLAYED 기록. ACK전 종료 시 다시 표시될 수 있으므로 ‘한 번의 수신행’과 ‘화면 exactly-once’를 구분한다. ACK는 이전generation에서 새세션으로 넘어오면 폐기한다.

### 5.4 제약을 강제하는 위치

PK/UNIQUE/FK/NOT NULL은 Room Entity 스키마로 강제한다. enum·범위·시각관계·표시상태 일관성은 저장소의 타입/검증 경계에서 강제하고 DAO를data패키지 내부로 제한한다. Room annotation이 임의CHECK를 자동생성한다고 가정하지 않는다. 필요 시 수동DDL/trigger를 추가하되 이번에는 추가하지 않는다. raw SQL/Inspector에서 값을 조작하면 앱 검증을 우회할 수 있으므로 불량행 읽기 시 명시적DataCorruption 결과로 제외·진단하고 잘못된enum을기본WARNING으로 바꾸지 않는다. 서버전환 시 동일검증을 서버와DB제약에 다시 구현해야 한다.

### 5.5 DAO / 저장소 계약

아래는 시그니처 수준 계약이다. Room Entity를 반환하는 DAO는 내부구현이고 외부에는 도메인 모델만 보낸다.

| 외부 저장소 메서드 | DAO 작업 / 의미 |
|---|---|
| `create(command): CreateResult` | 세션검증 후 note INSERT(ABORT), UNIQUE충돌은request로조회해 동일payload면기존결과, 다른payload면Conflict |
| `observeNearbyNotes(scope, cellIds): Flow<List<RoadNote>>` | scenario + 고정해상도 + h3_cell_id IN (:cellIds), author제외·수신이력NOT EXISTS. 경로/방향·만료는 등록 전과 수신 시 재검사 |
| `recordReceiptIfEligible(context, noteId): ReceiptResult` | 트랜잭션내부원본조회·만료/사용자/scenario/적격재확인 → 수신행INSERT. PK충돌은AlreadyReceived |
| `observeInbox(scope): Flow<List<InboxItem>>` | 부모JOIN+scenario+recipient, PENDING 및 유효쪽지, 안정정렬 |
| `markDisplayed(noteId, recipientId, at)` | PENDING행만UPDATE. 이미DISPLAYED면성공멱등 |

`scope`는 인증된 신원이 아닌 로컬session의scenario/actor/경로다. `command`에는 고정requestId/noteId, content/category,receivedAt,선택anchor가 담긴다. 호출자에게SQL트랜잭션·Flow<Entity>를노출하지 않는다. debug reset/count/cleanup은별도로컬관리클래스에두고미래서버공통인터페이스에넣지않는다.

INSERT OR REPLACE 사용 금지: 기존쪽지·자식수신이력을삭제할수있다. receipt 중복에만 제한적으로IGNORE를쓰되행미삽입이면PK중복인지부모소실/다른오류인지확인한다. FK/저장예외를AlreadyReceived로삼키지않는다.

### 5.6 원자성·만료·복구

- **심기:** 입력/anchor계산 → Mutex에서generation재확인 → Room.withTransaction으로request중복검사+insert → 커밋성공 → success1회전송. 저장실패/미준비면fail1회, 추가질문없음.
- **줍기:** 유효위치snapshot으로후보평가 → Mutex에서generation확인 → transaction에서note의현재만료/범위/자기제보/중복재확인+receipt삽입. 여러동시평가도PK로1행.
- **만료:** Room Flow는시간경과만으로재실행되지않는다. 위치이벤트·DB변경·세션시작과가시중1초ticker에서now를갱신해후보/수신함을거른다. 만료된pending은보여주지않는다. 활성scenario시작·심기·리셋때만료행정리(cascade). TTL정리를백그라운드영구작업으로만들지않는다.
- **리셋:** 잠금내generation증가/현재위치폐기 → 새scenario영속선택 → 이전scenario쪽지삭제transaction(cascade). 실패시새scenario유지·정리실패표시, 이전데이터다시노출안함.
- **저장후콜백실패:** DB와Gleo전송은원자적이지않음. 쪽지는남기고전송실패표시,자동재제보안함. 새Gleo호출은새request이므로의미상중복가능.
- **재시작:** 동일active scenario의미만료쪽지·PENDING복원,actorA/중지상태. 새위치확보전줍기안함.앱재설치보존·실데이터복구는이번범위밖.

### 5.7 서버 교체 시 유지할 것과 바뀔 것

교체 대상은 **공유 쪽지 생성·주변 셀 조회**다. `RoadNoteRepository`는 create와 observeNearbyNotes를 제공하고, 위 표의 recordReceiptIfEligible/observeInbox/markDisplayed는 로컬 `ReceiptStore` 책임으로 분리한다. 두 테이블은 지금 같은 Room DB에서 원자적으로 처리하되 인터페이스 책임을 구분한다. 서버 도입 후 FK를 유지하려면 받아온 쪽지의 로컬 스냅샷을 먼저 캐시해야 한다. 서버행과 로컬 FK를 직접 연결하지 않는다. 캐시 갱신·삭제 정책은 서버 전환 단계에서 확정한다.

유지: 도메인모델·UUID참조·입력검증·anchor선택·거리판정·화면상태, `RoadNoteRepository`의업무의미. 변경: Room구현을서버호출로교체,실계정author/recipient를서버인증에서결정,서버시각/TTL권위·H3 셀 검색·접근권한·멀티기기중복처리.

서버create는안정requestId를멱등키로받아야하고서버가중복을보장해야한다. 로컬UUID만으로서버정확히1회를보장하지않는다. 후보Flow는서버poll/push/로컬캐시등별도갱신전략이필요해단순DAO교체만으로완료되지않는다. 로컬Mutex는분산잠금이아니며서버트랜잭션/고유키가필요하다. 화면표시ACK는기기별인지사용자별인지서버단계에서결정한다.

쪽지의공유원본은서버로옮겨도수신함·표시ACK를로컬에남길수있다. ‘Room전체제거’와‘공유쪽지서버화’는별개선택이다. 오프라인지원·outbox·syncState·remoteId·ETag는요구사항이정해지기전추가하지않는다. 합성v1데이터는서버에업로드하지않으며새환경으로시작한다.

### 5.8 마이그레이션·테스트

Room/KSP버전은S3에서현재AGP9.5/Kotlin2.2.10/Gradle에맞춰공식호환성확인후고정한다. generated schema JSON을버전관리하고, v2부터MigrationTestHelper로기존쪽지/수신행보존을검증한다. fallbackToDestructiveMigration은사용하지않는다. 명시적debug리셋과업그레이드데이터삭제를섞지않는다.

v1기기계측필수: FK/cascade,동일request2회,동일request다른payload충돌,동시receipt삽입1행,다른recipient각1행,TTL경계,리셋과쓰기경쟁,재시작PENDING,DB예외후success금지. 순수로직테스트와실제SQLite제약테스트를분리한다.

## 6. 디버그 패널과 제품 화면

`src/debug`에Compose패널과테스트제보진입점을두고release에는버튼·fixture제보·리셋경로가포함되지않게한다. main은최소DebugTools슬롯,debug/release별같은시그니처구현으로분리한다. BuildConfig조건하나만으로테스트조작코드를release에남기지않는다.

패널: 접기/펼치기, A/B전환,테스트데이터리셋(확인1회),테스트제보1개버튼(S4까지),좌표·최근수신경과·수집span·anchor오차·쪽지/receipt수·최근결과·현재 H3 셀·주변 후보 수·proximity 등록 수/오류·최근 ENTER/EXIT. 전체발화/실차식별자를로그에출력하지않는다. 외부앱위에띄우는SYSTEM_ALERT_WINDOW권한없이앱window안에그린다. 닫아도추적계속,구성변경으로세션/쪽지초기화안함.

제품화면: 같은경로도식에현재위치/쪽지위치,준비·오류상태,한번에하나의수신쪽지. 지도타일없음. 패널이핵심쪽지표시를가리지않게배치하며가로/세로·분할·시스템영역·대비·터치영역을검증한다. driving UI를주행전용차단화면으로바꾸지않지만OS제한은우회하지않는다.

## 7. 스프린트 백로그

| 묶음 | 구현 범위 / 예상 파일 | 완료 조건과 리뷰 산출물 |
|---|---|---|
| S0 설계 확정 | 이문서,전체spec§5/6/8/9/10,관련 Mermaid 원본 | 숫자·시간의미·Room·H3 조회계약 리뷰완료,상충정책해소. Gleo브랜치통합방법/앱ID확정 |
| S1 SDK 연결·proximity 실험 | Gradle/Manifest,location/LocationCollector,Application/화면수명 | 실제앱에서ADB40초수신,권한거절·null/error·재시작처리,중복listener없음,SDKrelease대칭. ENTER/EXIT·이미 반경 안 등록·해제·세션 지연 이벤트·등록 한도 실험 |
| S2 버퍼·패널 | location/LocationBuffer, demo/DemoSessionController,ViewModel,src/debug UI | 불규칙콜백·30초준비·45초만료·동률·5초경계·A/B지연콜백단위테스트. 실제패널에서좌표/span/상태확인 |
| S3 Room 기반 | data/local Entity/DAO/Database,repository/domain모델,Gradle/KSP/schema | v1계측테스트통과, H3 산출/셀 경계·조회범위 테스트, DB관계/인덱스검토,DAO외부누출없음,실패·reset경쟁·복구검증 |
| S4 쪽지 심기 | CreateRoadNote,debug테스트진입,저장결과UI | 수신snapshot고정,약30초전anchor저장,현재위치와구분. 미준비/DB실패저장없음. 실제커밋후결과1회 |
| S5 주변 조회·SDK 접근·쪽지 줍기 | NearbyNoteCoordinator, ProximityRegistry, PickupEvaluator, ReceiptStore, 수신함 UI/ACK | 반경밖0회→접근1회→계속반경안추가0회,다른recipient수신가능,만료·역방향·자기제보제외. H3 경계·등록 차집합·정차/이미 반경 안/새 쪽지·등록 실패/한도 검증 |
| S6 Gleo·주행 E2E | 기존음성PR통합,영문schema/Receiver/callback,화면유지설정,시연스크립트 | 실제음성→저장후success→B수신,추가질문없음,앱수신부터응답목표5초. 주행중동일화면·Gleo오버레이수집확인 |

S1~S5는debug제보로독립검증한다. S6의Gleo코드통합은공유spec기준으로diff를검토하고사용자기존변경을덮어쓰지않는다. Gleo스키마는content/locationMode=CURRENT_ROAD/category 두enum의3필드,설명은영어,CREATE_ROAD_NOTE한번호출한번응답이다. 기존PR의초안저장과Room공유저장을동시에실행하지않는다. UI개편/앱이름정리는필수통합부분만취한다.

각스프린트종료시작동장면/검사결과를작업기록에남겨리뷰후다음단계로진행한다. FAIL수정없이반복실행하지않으며필수NOT_TESTED면완료라하지않는다. 자동검사기본은verify_harness,코드변경은assembleDebug/lintDebug및관련단위/Room계측테스트. release에서debug메뉴/제보진입부재도최종확인한다.

## 8. 최종 시연 절차와 기대 결과

1. Pleos2.1.0실행,Virtual Console연결,앱debug빌드실행. 리셋→합성위치목적고지/동의→권한허용→A선택.
2. 웹콘솔에서기어/속도주행상태설정. ADB스크립트는별도로동쪽직선R1,10m/s합성경로를주입한다. 웹콘솔애니메이션이GPS를움직인다고가정하지않는다. 둘의자동연동은이번범위밖.
3. 약60초주행후‘구덩이제보’발화. 실제JSON수신시각r과선택offset을기록. 기대anchor는수신30초전인x=10*(r-30)의근처. 정확히x=300을고정기대하지않는다.
4. 패널에서note1행·anchor오차한도·success후추가질문없음을확인. 패널닫으면현재위치와쪽지위치를도식에서구분한다.
5. A의 ADB 재생 중지 후 B 전환. 이전A좌표가B첫위치로채택되지않도록generation교체/SDK재등록완료후B경로주입. 필요시B첫고정좌표주입확인을기다리고재생한다. B는anchor보다300m뒤에서출발한다.
6. B의 현재 위치에서 주변 H3 셀을 조회하고 쪽지를 SDK proximity에 등록한다. 150m 밖에서는 receipt0행, 접근 시 ENTER → 추가 조건 검사 → receipt(B)1행·쪽지 표시. 셀 조회·등록 성공·ENTER·수신 저장을 각각 증거로 남긴다. 정확히150m 경계에서 이벤트가 온다고 가정하지 않는다.
7. 더이동/패널재열기해도receipt1행,원본note유지. B가반경안정차한경우도유효위치가있으면동작. 정차상태반복좌표에서콜백이안오는경우STALE정책을확인하고미검증을숨기지않는다.
8. 만료·재시작·리셋을각각확인. 화면녹화+선별로그+Room행수로증거를남기고실제좌표/원문발화는기본로그에서제외한다.

시연성공은단일기기의공유Room·독립A/B세션범위다. 두에뮬레이터는Room을공유하지않으므로그시연에는서버나별도공유전달이필요하다. 자발적음성경고는이번완료조건이아니다.

## 9. 리뷰 질문과 추적할 위험

- 초기45초보관/최근접표본/5초허용으로진행할지. 현재2초정책은실측에서실패했으므로정본수정필수.
- callback수신시각기준근사배치를프로토타입계약으로받아들일지. 실제측정시각보장과분리.
- Room공유원본과로컬수신함분리를서버단계에서도유지할지(현재는하나의DB,두테이블).
- SDK위치중단/정차콜백정책,Gleo오버레이가시성,기존브랜치앱ID/DO설정통합은실행검증게이트.
- 서버전환은저장소경계를재사용하지만인증·서버시각·갱신전략·분산중복·위치공유보안의추가설계가필수.

공식 참고: [Room Entity](https://developer.android.com/training/data-storage/room/defining-data), [관계](https://developer.android.com/training/data-storage/room/relationships), [마이그레이션](https://developer.android.com/training/data-storage/room/migrating-db-versions), [데이터계층](https://developer.android.com/topic/architecture/data-layer). 2026-11-09원문확인. 이문서는설계초안이며제품구현·Room제약·화면동작PASS증거가아니다.


## 10. 메모리 버퍼와 백그라운드 실행의 구분

위치 버퍼는 **메모리 전용**이다. Room에는 공유할 쪽지와 사용자별 수신·표시 이력을 저장한다. 서버 전환 대상은 공유 쪽지 저장·조회이며, 위치 버퍼를 서버에 업로드하지 않는다.

메모리의 위치 버퍼는 앱 프로세스 안의 객체다. Activity의 화면이 가려져도 소유 객체와 프로세스가 살아 있으면 데이터가 남을 수 있다. 그러나 프로세스 종료 시 사라지며, Application이나 Service에 두었다고 영구 보존되거나 백그라운드 실행이 보장되지는 않는다. 메모리 보관과 새로운 위치를 계속 수집하는 일은 별도다.

현재 초안은 **앱이 보이는 시연 세션에서 수집**하고 완전히 비가시 상태가 되면 수집 중단·버퍼 비움, 복귀 후 재준비하는 정책이다. 따라서 백그라운드 연속 제보/수신을 이미 지원하는 설계가 아니다. Gleo 오버레이·분할 화면은 실제 가시성 변화와 SDK 수신을 검증해야 한다.

다른 앱을 사용하는 동안에도 제보·접근 수신이 필요하다면 가시성 정책을 바꾸고 서비스 기반 수집의 수명·권한·사용자 알림·Pleos 지원 범위를 별도 설계한다. 이 경우에도 버퍼는 서비스가 사용하는 메모리에 둘 수 있으며 Room으로 바꿔야 하는 것은 아니다. Android foreground service를 선언하는 것만으로 Pleos 위치의 백그라운드 지원이나 프로세스 영구 생존을 보장하지 않는다.

참고: [Android 프로세스 수명](https://developer.android.com/guide/components/activities/process-lifecycle), [Foreground service](https://developer.android.com/develop/background-work/services/fgs). 백그라운드 연속 수집은 NOT_TESTED이며 현재 스프린트의 확정 완료 조건에 포함하지 않는다. 필요 여부를 리뷰에서 결정한다.


## 11. H3 주변 조회와 SDK proximity — 최신 합의

### 11.1 저장·검색·접근의 역할

쪽지 생성 시 **선택된 과거 anchor 위경도**로 H3 셀 ID를 계산한다. 현재 차량 좌표로 계산하지 않는다. 위경도는 원본 위치로 계속 저장하고 H3는 조회를 위한 파생 인덱스로 쓴다. H3 셀 중심으로 쪽지 좌표를 옮기지 않는다. SDK에는 원래 쪽지 좌표와150m 반경을 등록한다.

`h3_cell_id`는 정규화한 H3 문자열로 저장한다. 해상도는 환경별 하나로 고정하고 데이터마다 일치 검증한다. 초기 검토 후보는 resolution9이며 아직 확정값이 아니다. 셀의 평균 크기를 실제 최소 크기로 가정하거나 gridDisk(k=1)이 항상150m를 덮는다고 가정하지 않는다. 해상도·조회 셀 수는 S0~S3에서 시연 경로 fixture와 경계 테스트로 확정한다. H3 Android 바인딩의 ABI/패키징 호환성도 의존성 도입 전 확인한다. H3 인덱스는 도로·진행 방향을 표현하지 않는다.

### 11.2 주변 쪽지 조회와 갱신

1. 유효한 현재 위치로 고정 해상도의 현재 셀과 주변 셀 집합을 계산한다. 현재 셀 하나만 조회하지 않는다.
2. 조회 범위는 **경고 반경150m + 다음 갱신까지의 이동 거리 + 조회/등록 지연 여유**를 포함한다. 서버 단계10초 폴링 초안에서36km/h는 추가100m이며,250m는 지연 여유를 제외한 하한일 뿐이다. 일반 도로 속도 전체에 충분하다고 간주하지 않는다.
3. gridDisk의 k는 미터가 아닌 셀 이동 횟수다. 대상 범위를 덮는 셀 집합을 넉넉히 구하고 좌표/거리로 필터링한다. 원의 경계와 걸치는 셀도 조회해야 한다. 단순 셀 중심 포함 검사로 경계 쪽지를 누락하지 않도록 테스트한다.
4. **이번 Room 버전:** 활성 세션 시작·조회 셀 집합 변경 시 Room Flow 구독을 교체한다. 쪽지 추가/만료/수신 이력 변경 시 후보 갱신, 시간 만료는 ticker로 별도 처리한다. 로컬 DB를10초마다 폴링하지 않는다.
5. **추후 서버 버전:** 세션 시작·구역 변경 시 즉시 fetch, 같은 구역에서는 약10초 폴링을 초기안으로 검토한다. query의 셀 집합·generation·버전을 묶어 오래된 응답을 버린다. 실패 시 캐시의 유효 쪽지만 유지하고 오류/갱신 시각을 표시한다. 실패한 조회를 빈 성공 결과로 취급해 등록을 전부 해제하지 않는다. 캐시/페이지네이션·삭제/만료 전파는 서버 단계 계약이다.

두 버전의 결과는 같은 후보 목록을 ProximityRegistry에 전달한다. 개인정보 관점에서 서버에 보내는 현재 위치/셀 조회와 전체45초 위치 버퍼 업로드는 별개이며, 후자는 하지 않는다.

### 11.3 SDK 등록 상태와 ENTER 처리

ProximityRegistry는 `(generation, noteId)`와 SDK 등록 ID 매핑을 메모리에 관리한다. 후보 중 자기 제보·만료·이미 수신·다른 경로/방향을 제외하고 SDK에 등록한다. 원하는 후보와 실제 등록 성공 목록의 차집합만 추가/해제한다. 매 콜백마다 전부 재등록하지 않는다. 실제 함수 시그니처·이벤트 식별자·완료/오류 전달은 설치SDK 및 공식샘플과 대조한다.

등록 실패는 성공 목록에 넣지 않는다. 한도를 넘으면 가까운 전방 후보 우선으로 제한하고 미등록 개수/이유를 표시한다. SDK 한도는 아직 미확인이며 조용히 누락시키지 않는다. 실험 결과 수용할 수 없는 누락이 생기면 정책을 리뷰하고 S5를 완료 처리하지 않는다.

ENTER는 수신 완료가 아닌 **평가 트리거**다. noteId/generation을 검증한 뒤 최신 유효 위치·경로·방향·전방 거리·지표거리·만료·자기 제보·중복을 재검사하고 ReceiptStore 트랜잭션으로1행 등록한다. 수신 성공 시 해당 proximity를 해제한다. EXIT는 쪽지/수신 이력을 삭제하지 않는다.

보완 트리거는 등록 직후·B 세션 시작·새 쪽지 유입이다. 이미 반경 안일 때 ENTER가 오지 않을 수 있으므로 유효한 현재 위치로 한 번 동일 판정을 수행한다. ENTER가 위치 콜백보다 먼저 오면 신선한 위치가 확보될 때까지 해당 이벤트를 보류하고 다음 위치에서 재평가한다. 반대 방향으로 진입했다가 반경 안에서 방향이 바뀌는 경우에도 새 위치/경로 변경으로 반경 안 후보를 재평가한다. 보완 경로와 ENTER가 동시에 실행돼도 receipt PK로 중복을 막는다.

만료·조회 범위 이탈·쪽지 삭제·A/B 전환·리셋·권한 철회·비가시 수집 중단 시 해당 등록을 해제한다. listener/release와 별도로 proximity 등록도 정리한다. 지연 이벤트는 generation으로 무시한다. SDK 재생성 후 현재 후보에서 다시 등록하며 이전 등록이 자동 복구된다고 가정하지 않는다. 오래된 현재 위치만으로 수신을 만들지 않는다.

### 11.4 S1 실험 게이트와 S5 검증

S1: 고정 좌표1개·반경150m 등록 → ADB로 밖→안→밖 이동 → ENTER/EXIT 및 이벤트 시각/위치 관측. 이미 안에서 등록, 등록 해제 후 이동, 중복 등록, A/B 교체 후 지연 이벤트, 등록 실패/지원 개수도 확인한다. 주행 중 수신은 가상 속도/기어와 GPS 주입을 별도로 설정한다. 현재 이 항목들은 **NOT_TESTED**다.

S5: H3 경계 양쪽 쪽지 조회, 다른 셀 안150m 쪽지,150m 밖 후보 미수신, 새 쪽지 등록 당시 이미 반경 안, ENTER/보완 동시 호출, TTL 만료 등록 해제, reset 중 지연 이벤트, 후보 한도 초과를 검증한다. S1에서 SDK ENTER가 동작하지 않으면 조용히 전 위치 거리 계산으로 바꾸지 않고 실패와 대안을 문서화해 리뷰한다.

공식 근거: [H3 indexing](https://h3geo.org/docs/highlights/indexing/), [gridDisk](https://h3geo.org/docs/api/traversal/), [셀 크기 통계](https://h3geo.org/docs/core-library/restable/), [Pleos 공식 샘플](https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app). API 존재와 실제 기기 동작 검증을 구분한다. H3 도입은 이번 설계 방향이며 현재 의존성·Entity·SDK 등록 코드는 작성하지 않았다.


## 12. 구현 결과와 설계에서 단순화한 부분

현재 구현은 notes/domain 계산, data/Room·H3, location/SDK 어댑터, demo/세션 조합, ui/notes의 ViewModel·Compose로 나뉜다. 범용 DI/모듈 분할/추가 프레임워크는 없다. 실제 파일과 실행 방법은 [시연 안내](../LOCATION-DEMO.md), 검증 결과는 [작업 기록](../work/fused-location-implementation.md)에 있다.

- 위치 버퍼는 메모리45초, callback 단조시각 기준30초 근사 선택, 최대5초 오차다. source SDK / synthetic만 활성화했다.
- H3는res9, 고정 서울 직선 경로400m 사각 cover + 이웃ring 조회다. 실제 도로/전지구 범위 보장은 하지 않는다. 서버폴링10초는 구현하지 않았다.
- Room 컬럼명은 Kotlin프로퍼티명과 동일하다. offset으로부터 errorMillis를 계산하며 중복저장하지 않는다. receipt의 displayedAt=null이면PENDING, 값이있으면DISPLAYED로 파생해 상태컬럼불일치를없앴다. 생성스키마를app/schemas에보관한다.
- SDK 등록과 H3 후보 조합은 DemoController 안의 작은 함수로 구현했다. 책임이 작아 아직별도ProximityRegistry/PickupEvaluator클래스는 만들지 않았고, 순수 eligible 함수와 저장소경계는유지했다.
- SDK등록은시연최대16개로제한하고초과개수를표시한다. 공식SDK한도라는뜻은아니며실제등록실패도표시한다.
- 경로방향은고정fixture진행거리변화로FORWARD/REVERSE를판단한다. 처음정차상태는fixture의FORWARD방향을기본으로하고,실도로헤딩정확도를입증하지않는다.
- 제보는기존음성브랜치의schema계약만재사용해새Room저장흐름에연결했다. 기존DraftStore/미리보기성공경로를복제하지않았다. debug receiver만외부공개,release는내부전용이다.
- 디버그패널과하드코딩테스트제보는src/debug,release패널은no-op이다. 화면유지스위치는앱설정으로저장한다.
- 과거검증원시JSON은사용자요청으로삭제했다. 현재app/schemas/1.json은실험파일이아닌Room스키마산출물이다.

실제음성/Gleo승인·실차·전체폼팩터·개인정보차단·SDK등록한도는코드완료와구분해서NOT_TESTED로남긴다. 이문서앞부분의예상클래스/표는설계책임설명이고실제파일목록이아니다.

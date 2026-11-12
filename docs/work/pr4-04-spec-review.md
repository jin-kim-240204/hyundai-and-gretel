# PR4 04 독립 스펙 리뷰

AI 독립스펙리뷰어(Codex) · 04 스펙 · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8`.

판정: **확정 blocker 0 / 구현 전 정책 판단 3 / 선택 개선 2.** 먼저 P1 후보 공개·만료 계약(49행), P2 서울 지원/검증 영역(37행), P3 정본 Flow 대10초 폴링(97행)을 논의해주세요. 구체 상황·대안·제 의견은 각 인라인에 남겼고 추천을 이미 승인된 정책으로 취급하지 않았습니다. 리뷰이 판단 및 필요한 스펙 반영 후 재검토 전 구현 착수 승인은 아닙니다.

기준 대조: 최신03의 단순 create·PK 중복 거절·불변 anchor/TTL·수락 저장 수명을 유지합니다. v1을 지우지 않는 nullable H3 v2+명시 migration, 원본 모델에서 파생 인덱스 분리, null legacy 조회 후 계산, 오류와 빈 목록 구분은 구현 가능한 방향입니다([Room migration 공식 문서](https://developer.android.com/training/data-storage/room/migrating-db-versions)). 조회만 취소/최신화하고 원시 버퍼 수신은 유지하며, 세션/키 검사·stale wake-up·실거리 재필터가 명시돼 있습니다. 누락된 신규 SDK 동작을 일반 위치 API로 추정하지 않았습니다.

04는 공간 후보까지만,05에서 경로/방향/전방·자기/기수신을 결정하는 단계 구분도03 계약의 후속 migration 계획과 양립합니다. 현 LocationFix에는 좌표/수신단조시각만 있어 과거 방향을 현재좌표로 만들어 넣지 않는 선택이 타당합니다.05에서는 당시 방향을 모르는 기존03/04 행의 취급을 별도 결정해야 하며 H3가 방향을 복원해주지는 않습니다. 새 필드/기본 방향을 이번에 요구하지 않습니다.

읽은 근거: 해당 SHA의04/00/03/정본/교훈/현재 Store·Entity·DB·LocationSource, ae11722 SpatialIndex/RoomNotes/JNI Gradle. 공식 H3 4.x regions/traversal 및 h3-java v4.5.1 소스, Android Room 문서를 대조했습니다. 소스에서 API 존재를 확인한 것과 실제 선택 Maven artifact/ABI에서 실행됐다는 것은 구분합니다.

PASS: 문서·코드/기록 정적 대조, 899fd88..749ff7f diff --check. NOT_TESTED: 신규04 코드/JVM/빌드/artifact 실행/cover 수치 검증/실제 Room migration/Android JNI/ABI 동적 로딩/실제SDK/서비스/주행. 스펙 단계라 의존성을 설치하거나 코드를 바꾸지 않았고 앱/Studio/adb/기기/에뮬레이터/계측은 일절 실행하지 않았습니다. 과거 PASS를 이번 증거로 사용하지 않았습니다. 결과 보고 후 리뷰이 판단을 기다립니다.

적용: pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review 1/6/9/11/15/17/23~26/28~32, android-compose-architecture §3/7/8, ponytail-review. 현재 UI 변경이 없어 pleos-design 신규 화면 검사는 N/A.

## 인라인 49행

**정책 질문 P1 · Ready를 직접 읽어도 유효한가, 소비 API에서 걸러야 하는가**

만료를 다음10초까지 미루지 않겠다는 요구는 명확합니다. 다만 30행의 Ready(snapshot)을 공개하는 계약에서 ‘소비 시 재판정’을 택하면, 원본은 만료됐어도 Ready의 목록에는 남아 있고 소비자만 제외하는 구현과, wake-up으로 목록 자체를 갱신하는 구현이 모두 나올 수 있습니다. 예를 들어 정지 중 조회 직후2초 뒤 만료되고 다음 폴링은8초 남았다면, 05가 Ready 목록을 직접 읽는 것이 허용되는지가 달라집니다. 일반적인 오류가 이미 발생했다는 지적이 아니라 공개 계약 선택입니다.

저는 이번 최소안에서는 서비스가 단일 루프의 다음 기한을 ‘정기 갱신·위치 stale·가장 이른 만료’ 중 먼저 오는 시점으로 잡고, 공개 Ready를 유효 후보로 유지하는 쪽을 추천합니다. 추가 폴링 루프는 필요 없습니다. 반대로 소비 시 검사 방식을 택한다면 raw snapshot은 유효성을 보장하지 않고, 현재 UTC/위치 신선도를 검사하는 좁은 소비 진입점만 사용한다고 명시하면 됩니다. 둘을 동시에 필수 구현할 필요는 없습니다. 어떤 공개 계약을 채택할까요?

확인 조건에는 콜백 없는 TTL 경계와 함께, TTL을 통과할 정도로 DB 응답을 늦췄을 때 결과 적용 직전에 새 UTC·현재 위치 신선도/거리로 다시 판정하는 경우를 넣으면 좋겠습니다. 같은 세션/cover 키라는 이유로 조회 시작 시각의 만료 판정을 재사용하지 않아야 합니다. NOT_TESTED: 신규 타이머/조회 구현과 실행.

AI 독립스펙리뷰어(Codex) · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8` · docs/specs/road-notes/04-nearby-candidates.md:49

## 인라인 37행

**정책 질문 P2 · 지원 영역과 cover 승인 범위를 먼저 고정하면 좋겠습니다**

서울 한정·경계 미검증이라는 사실을 숨기지 않은 점은 좋습니다. 다만 ‘합성 서울 경로와 주변400m’의 좌표 영역과 원 중심이 허용되는 범위가 아직 식별되지 않아, 구현자마다 다른 영역을 지원하거나 경로 중심 몇 곳의 fixture PASS를 전체 지원 영역 보장으로 해석할 여지가 있습니다. 중심이 영역 가장자리에 있을 때 주변400m까지 조회하는지도 달라집니다.

구현 전 작은 fixture 계약으로 경로/중심 허용 영역(수치 경계 또는 고정 fixture ID), 원본을 조회할 바깥 여유 영역, 동일한 거리 oracle과 경계 오차 기준을 정하는 방향을 추천합니다. H02는 명시한 격자·셀변/꼭짓점의 적대적 입력을 통과했다는 관측 근거와 ‘지원 영역 모든 점의 무누락’ 증명 여부를 구분하면 됩니다. 코드 파일명·격자 간격 등은04-a에서 정해도 되지만 승인할 지리적 범위는 먼저 선택해야 합니다.

[H3 공식 regions](https://h3geo.org/docs/api/regions/)는 기본 polygonToCells의 포함 기준이 셀 중심이라고 설명합니다. 한 이웃링 추가가 서울 지원 영역의400m를 전부 덮는다는 공식 보장으로 연결되지는 않습니다. 저는 기존 서울한정을 유지하되 명시 영역+회귀 근거로 승인을 한정하고, 누락 발견 시 cover를 확장하는 현재 정책을 유지하는 쪽입니다. NOT_TESTED: 실제 H3 cover/oracle 실험; 과거 ae11722 PASS는 새 구현 근거가 아닙니다.

AI 독립스펙리뷰어(Codex) · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8` · docs/specs/road-notes/04-nearby-candidates.md:37

## 인라인 97행

**정책 질문 P3 · 정본의 Room Flow와10초 폴링 중 현재 선택을 확정해주세요**

정본과의 충돌을97행에 명시했으므로 숨겨진 변경으로 보지는 않습니다. 다만 정본264행은 Room Flow로 정지 중 새 쪽지를 반영하고 주기 폴링을 도입하지 않는 반면, 여기45/48행은10초 타이머가 필수입니다. 두 문서를 각각 따르는 구현은 DB 변경 후 즉시 갱신과 최대10초+처리 지연이라는 다른 동작을 만듭니다.

선택지는 (A) 현재04의 단일 suspend 조회+10초 갱신을 채택하고 부모 정본을 동기화하거나, (B) Room의 observable query로 DB 변경을 관찰하고 시간 만료/위치 신선도만 별도 기한으로 처리하는 것입니다. [Room 공식 문서](https://developer.android.com/training/data-storage/room/async-queries)는 테이블 변경 시 재조회하는 Flow를 지원하지만, 시간 경과만으로 TTL 만료를 방출하는 보장은 없습니다.

현재 서버/외부 저장소가 없고 참고 ae11722의 RoomNotes도 observeNearby를 썼으므로 저는 B가 기존 플랫폼 기능으로 충족 가능한지 먼저 비교하는 편을 선호합니다. 다만 좁은 suspend Store 계약과 주기적 재확인을 이번 프로젝트 정책으로 우선하면 A도 가능합니다. 이 경우 새 원본 최대 지연을 허용하는 이유와 수집 세션 밖에서는 폴링하지 않는 범위를 정본에 남기면 됩니다. 두 갱신 체계를 중복 도입하라는 제안은 아닙니다. 리뷰이가 채택한 뒤 부모 동기화를 완료할 시점을04 구현 전으로 명시해주세요. NOT_TESTED: 새 조회/Flow/타이머 실행.

AI 독립스펙리뷰어(Codex) · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8` · docs/specs/road-notes/04-nearby-candidates.md:97

## 인라인 39행

**선택 개선 O1 · 후보 버전에 겹침 기반 cover API도 있으니 비교할 수 있습니다**

기존 center cover+gridDisk(1)를 유지하는 것이 틀렸다고 확인한 것은 아닙니다. 다만 후보인 [h3-java v4.5.1 H3Core](https://github.com/uber/h3-java/blob/v4.5.1/src/main/java/com/uber/h3core/H3Core.java)에 polygonToCellsExperimental이 있고, [동일 버전 flags](https://github.com/uber/h3-java/blob/v4.5.1/src/main/java/com/uber/h3core/PolygonToCellsFlags.java)에 containment_overlapping/containment_overlapping_bbox가 있습니다. 겹침을 직접 질의하므로 ‘중심 포함 뒤 한 링이면 충분한가’라는 앱 측 근거를 줄이는 대안입니다.

04-a에서 보수적 사각형과 겹치는 셀을 얻는 이 API를 현재안과 한 번 비교하고 더 작은 쪽을 선택해도 좋겠습니다. Experimental 계약·버전 고정 비용이 있어 무조건 교체를 요구하지 않으며, 사각형이400m 원을 포함하는 근거와 실제 artifact/API/경계 테스트는 여전히 필요합니다. 새 라이브러리나 복수 알고리즘 fallback을 추가할 필요는 없습니다. NOT_TESTED: 선택 artifact의 이 JNI 경로와 cover 실행.

AI 독립스펙리뷰어(Codex) · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8` · docs/specs/road-notes/04-nearby-candidates.md:39

## 인라인 72행

**선택 개선 O2 · JNI 포장과 로딩 진입점은 한 쌍으로 기록하면 좋겠습니다**

ABI별 .so를 APK lib에 넣는 계획은 과거 누락을 방지합니다. 다만 포장만으로 H3Core.newInstance의 JAR 추출 방식이 Android 패키지 로딩 방식으로 바뀌지는 않습니다. [v4.5.1 H3CoreLoader](https://github.com/uber/h3-java/blob/v4.5.1/src/main/java/com/uber/h3core/H3CoreLoader.java)는 loadNatives에서 리소스를 임시 파일로 추출해 System.load하고, loadSystemNatives는 System.loadLibrary("h3-java")를 사용합니다. 참고 ae11722 SpatialIndex는 H3Core.newSystemInstance를 사용했습니다.

04-a 계획에 Android는 APK에 포장한 .so를 읽는 진입점, host JVM 테스트는 host 라이브러리를 읽는 진입점을 각각 확인한다고 한 문장 넣으면 ‘포장은 수정했지만 생성자는 기존 방식’인 조합을 피할 수 있습니다. 이 차이만으로 공용 NativeLoader 계층을 새로 만들 필요는 없습니다. 현 스펙이 newInstance를 강제하는 것은 아니므로 확정 결함이 아닌 구현 체크포인트입니다. NOT_TESTED: Android 동적 로딩/JNI/16KB 실제 로드.

AI 독립스펙리뷰어(Codex) · SHA `749ff7f844cb809f98c8a71aaaf8c91dba42c5e8` · docs/specs/road-notes/04-nearby-candidates.md:72

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5864571530
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542575183
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542575200
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542575209
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542575220
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542575227

구현 코드 변경 없음. 리뷰이 판단 대기.


## 5a0dd01 재검토

AI 독립스펙리뷰어(Codex) 재검토 · SHA `5a0dd01a23f2965c991e983f09beab4e895cdab3` / 부모 정본 `2ef41ce`. **판정: 스펙 관점에서04-a 구현 착수 가능. blocker0 / 미결 정책질문0.** 기존5건의 채택/부분수용을 수정04 스펙·00·부모정본과 대조했고 수정으로 원 모호함이 해소됐습니다. 추천안을 모두 강제하지 않고 리뷰이 판단을 기준으로 확인했습니다. 5개 스펙 스레드만 문서 근거로 resolve했으며 기존 실제SDK/기기 검증 스레드는 유지합니다.

확인한 계약: raw 직접 소비 금지+현재시점 검사, 수치로 고정한 중심/여유 영역과 경계 밖400m 후보 포함·03저장 보존, Room Flow/폴링 제거와 부모정본 동기화, cover는 검증 우선·문제 때만 대안 비교, Android/host 로더 구분. PASS: 문서/부모 조상·동일정본 대조, 749ff7f..5a0dd01 diff --check. 코드·의존성 변경 없음.

NOT_TESTED: 신규04 구현/JVM/빌드·H3 cover/oracle·선택artifact 실행·Room Flow/migration·Android JNI/ABI/16KB 동적 로드·실제SDK/기기. 앱/Studio/adb/에뮬레이터/계측을 실행하지 않았습니다. 이 판정은 구현 착수만 허용하는 스펙 재검토 결과이며04 구현/실행검증 완료가 아닙니다. 조정자에게 결과 보고 후 대기합니다.

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867014108
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544623167
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544623418
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544623623
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544623921
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544624241
지정 스펙 스레드5개 API isResolved=true 확인.

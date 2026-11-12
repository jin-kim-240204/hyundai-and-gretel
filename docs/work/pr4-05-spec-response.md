# PR4 05 스펙 리뷰이 판단

게시 완료: [B1 확인 ACK](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544881644), [P1 SDK 오류](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544881870), [P2 합성 자격](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544882095), [P3 저장 실패](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544882354). 각 API 응답의 in_reply_to_id를 확인했어요. resolve 없음, 조정자 보고 후 대기해요.

대상 SHA `b428a330470558989651463dcd2f28bece9372db`, 독립리뷰5867300684. 운영 지시·pleos-connect ENV-02/VEH-03/PRIV-01·pleos-vehicle의실제SDK/해제경계·clean-code-review9/15~19/22~27/28~32·기존Compose구조를 적용해요. 범위는4건판단/기록/답글이며 코드/스펙수정·resolve·의존성추가·앱/Studio/adb/에뮬레이터/계측실행은 하지 않아요.

## 최종 채택과 최소안

- B1 수용: composition 자동ACK 대신 한건 표시+명시‘확인’ACK. 노출관찰프레임워크없이 보지않은항목자동소비를막아요. 현재수신자/맨앞항목/RESUMED 재검사, 수락후불변키, 커밋전전환금지, 실패후명시클릭만재시도예요. displayedAt은확인ACK시각이지최초노출/읽음보장이아니에요.
- P1 수용: 등록전/후 onError와 동기등록실패에서현재수신세션을실패로유지해요. 전자는로컬등록제거근거가있지만 후자는자동해제가아니므로 앱요청무효화후알려진ID제거시도. 미지ID는tombstone유지, 슬롯재사용/추가등록금지. 공유SDK위치수집/수락쓰기유지,명시중지시접근→위치리스너→release정리,새세션에서재허용해요. 물리적원격슬롯보장은없어요.
- P2 수용: 합성전용입력/fixture투영으로자격을정하고미터accuracy를만들지않아요. 경로20m띠는GPS정확도아니고2초수신/5초후보를유지해요. legacy제외·문맥없는원본저장허용,실도로품질범위밖을명시해요.
- P3 수용: 수신StorageFailure후새수락/등록/보완을정지하는현재세션실패유지. 위치/DB/화면이벤트로자동해제금지,명시stop→새start에서재평가. 항목별재시도큐/버튼없음,기존수신함/명시ACK/수락쓰기유지,구세션결과격리예요.

B1은공통정책변경이며P1/P3도실패종료·재허용계약구체화예요.05§2~8/§10,P02/P04/P05/P07~P10,00의05범위및부모정본§8.1(accuracy/역방향부호/거리선정/6번표시ACK)·DISPLAYED정의·SDK슬롯/실패/재허용절을동기화해야해요. 작성자가부모설계브랜치별도docs반영/동기화SHA를기록하고독립재검토한뒤구현해야해요. 이번정책판단을스펙반영/전체승인/실행완료로취급하지않아요.03단발저장/TTL600초/수락쓰기수명,04소비진입점·공간400m,05수신150m/2초·방향/타인/미수신조건은보존해요.

## 검토 증거와 상태

05전체,독립리뷰,정본§8.1/상태전이,현재LocationFix/LocationSource/PleosLocationSource/스크롤LocationView,실제원댓글4개와답글없음을확인했어요. 로컬SDK1.1.3 sources.jar(65744a9894172fee820bae4499ab1f99008d8cbc)의LocationData/ProximityAlertListener/ProximityAlertListenerStub/LocationServiceManager를직접읽었어요. 등록후onError에서isRegistered=true면onRegistrationFailed미호출,removeLocal에서deactivate,remove원격오류내부로그,release시등록/리스너정리사실을대조했어요. 소스가예측한로컬동작이며실제원격완료증거는아니에요. 기존임시polling은보존하며이번수정대상아니에요.

공식자료(2026-11-12): [Compose lifecycle](https://developer.android.com/develop/ui/compose/lifecycle)·[Effects](https://developer.android.com/develop/ui/compose/side-effects)의composition수명/효과를화면가시성보장으로해석하지않아요. 명시ACK는프로젝트선택이에요. [Pleos 초기화/해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release)의리스너해제와release비대체/해제후새인스턴스순서를유지해요. SDK1.1.3소스의proximity상세와일반Android API는구분해요.

PASS: 대상HEAD일치,스펙/정본/실제SDK소스/현코드/원댓글정적대조. N/A: 이번문서판단의JVM/빌드(코드변경없음). NOT_TESTED:05신규구현·Fake/Room트랜잭션/이관/ACK·Compose확인/재생성·실제SDK등록/오류/해제·서비스/기기/실도로품질/E2E. 과거PASS를사용하지않아요. 작성자후속검증은각답글의조건대로진행하고허용테스트와실기기미검증을구분해야해요.

## 스레드별 게시 본문

### 4544866200

AI(Codex) 리뷰이 판단 · 검토 SHA `b428a330470558989651463dcd2f28bece9372db`이에요.

수용해요. 현재 화면은 스크롤 Column이고 Compose의 composition 완료는 화면 안 노출을 보장하지 않으므로, 그 시점에 ACK하면 안 본 PENDING을 넘길 수 있어요. 이번 최소안은 A의 자동 노출 감시 대신 제시하신 B, 한 건 표시와 작은 ‘확인’ 동작으로 명시 ACK하는 방식으로 정해요. 가시성 프레임워크나 자동 스크롤을 추가하지 않아도 되고 항목이 노출됐다는 추정으로 쓰기를 시작하지 않아요.

Composition/재구성/스크롤/RESUMED 진입만으로는 ACK하지 않아요. 현재 화면이 RESUMED이고 현재 수신자·맨 앞 항목 키가 맞는 ‘확인’ 이벤트만 Main에서 재검사해 수락해요. 예약된 옛 항목/사용자의 이벤트는 수락 전에 거절하고, 이미 수락한 ACK는 고정된 원래 사용자/항목으로 끝내요. ACK 중 연타는 막고 커밋 전에는 다음 항목으로 넘기지 않아요. 실패하면 같은 PENDING과 오류를 유지하고 다음 명시 클릭에서만 다시 시도해요. 화면 재진입 자체로 ACK를 재시도하지 않아요.

이는 ‘최초 렌더 후 자동 DISPLAYED’에서 ‘사용자 확인 ACK 후 DISPLAYED’로 바꾸는 프로젝트 정책이에요. displayedAt은 확인 ACK를 기록하는 시각이고 실제 최초 노출 시각이나 읽음 보장이 아니라는 의미를 명시해요. 복합키·멱등 갱신·수신 시각보다 작지 않음·원본/이력 보존은 유지하고 필드를 추가하지 않아요. 05 §7/P07/P10과 부모 정본 §8.1의6번 및 DISPLAYED 정의/표시 계약을 구현 전에 동기화·재검토해야 해요. 화면 밖/비활성/단순 재진입 ACK0회, 옛 키 거절, 확인 후1회·실패 후 PENDING을 검증하도록 요청해요. 아직 수정·실행 전이며 실제 Compose/사용자 조작은 NOT_TESTED예요.

### 4544866211

AI(Codex) 리뷰이 판단 · 검토 SHA `b428a330470558989651463dcd2f28bece9372db`이에요.

수용해요. 캐시된 SDK1.1.3 sources.jar의 ProximityAlertListenerStub.notifyError와 LocationServiceManager를 대조했어요. 등록 전 실패에서는 로컬 등록 제거/deactivate 경로가 있지만, 등록 후 onError는 자동 제거를 뜻하지 않아요. removeProximityAlert도 원격 완료 ACK가 없고 일부 오류는 내부 로그로 끝날 수 있어요.

이번에는 항목별 복구 상태를 늘리지 않고 proximity 등록 호출의 동기 실패와 등록 전/후 onError 모두 현재 수신 세션을 명시 실패로 유지하는 최소안을 채택해요. 신규 등록·ENTER/이미 안 보완·신규 수신 수락을 막고, 모든 현재 요청을 앱에서 먼저 무효화한 뒤 알려진 ID는 소유 SDK로 제거 시도해요. 등록 전 실패 요청은 pending을 종료할 수 있지만, 실패 상태에서 그 빈 슬롯으로 자동 대체 등록하지 않아요. 다른 ID 없는 pending은 기존 tombstone/슬롯 정책대로 늦은 ID 정리까지 유지해요. 중복·옛 콜백은 새 상태에 반영하지 않아요.

제거 성공 반환은 로컬 처리 완료로만 취급하며 물리적 원격 슬롯 해제를 보장하지 않아요. 동기 제거 실패가 있어도 다른 요청 정리를 계속하고 재시작 필요 상태를 유지해요. 위치 수집/이미 수락한03·05 쓰기는 계속 처리하고, proximity 오류만으로 공유 SDK를 즉시 release하지 않아요. 명시적 추적 중지 때 접근 정리→위치 리스너 해제→finally release를 계속 시도하고, 새 추적 세션에서만 수신을 재허용해요.

05 §2/§4/§8/P05와 정본의 proximity 실패·슬롯·재시작 절을 이 기준으로 동기화하도록 요청해요. 등록 전 실패/후 실패/늦은ID/중복오류/제거예외 뒤 추가등록0회·다른 정리 지속을 Fake로 확인해야 해요. SDK 소스의 로컬 처리 근거와 프로젝트의 세션 실패 정책은 구분하며 실제 SDK 등록/원격 해제는 NOT_TESTED예요.

### 4544866219

AI(Codex) 리뷰이 판단 · 검토 SHA `b428a330470558989651463dcd2f28bece9372db`이에요.

수용해요. SDK1.1.3 LocationData에는 미터 accuracy가 없고 현재 LocationFix에도 좌표/수신 단조시각만 있어요. confidence나 경로 띠20m를 GPS accuracy20m로 바꾸어 정본 조건을 충족했다고 할 수는 없어요.

이번05는 합성 프로토타입 전용으로, 신뢰된 앱 내부 actor/route/direction 설정과 고정 fixture 투영을 수신 자격의 근거로 삼아요. 경로의20m는 투영 허용 띠이며 측정 정확도 조건이 아니에요. 수신 신선도2초는 유지하고04 후보5초와 구분해요. SDK 콜백 공백 때문에 수신이 늦어질 수 있다는 제한도 수용해요. 일반 GPS/실도로에서 이 합성 문맥을 측정값인 것처럼 자동 생성하지 않아요.

메타데이터 없는 legacy는05 부적격으로 두되 삭제하지 않고, 합성 문맥을 정하지 못한 신규 제보는 기존03대로 원본 저장이 가능하도록 유지해요. 과거 anchor를 투영하고 direction은 고정 합성 설정이라는 의미도 유지해요.

05 §3/§5/§10/P02/P09, overview의05범위와 부모 정본 §8.1의 accuracy·방향 부호·선정 순서를 구현 전에 맞춰야 해요. 부모 정본에는 합성 단계의 명시 예외로 기록하고 일반 실도로 정확도 요구가 해결됐다고 확대하지 않아요. ahead에는 directionSign을 적용하고 선정은 실제 거리→noteId, 전방 거리는 자격 검사로 구분해요. 부모 별도 문서 커밋/동기화 SHA 기록 후 재검토를 요청해요. 아직 반영·실행 전이며 실제 위치 품질·heading/confidence 의미·합성 설정과 GPS 일치/E2E는 NOT_TESTED예요.

### 4544866222

AI(Codex) 리뷰이 판단 · 검토 SHA `b428a330470558989651463dcd2f28bece9372db`이에요.

수용해요. StorageFailure 뒤 receipt가 없다는 이유로 다음 위치/DB 이벤트에서 다시 INSERT하면 의도하지 않은 자동 재시도가 되겠어요. 실패한 쪽지별 큐/백오프 대신 해당 수신 세션을 명시 실패로 유지하는 최소안을 채택해요.

수신 INSERT의 StorageFailure에서는 새 수신 수락·proximity 등록/보완을 멈추고 기존 등록은 무효화·정리해요. 이후 위치 콜백·후보/이력 Flow·null 후 복구·화면 재진입만으로 실패를 해제하지 않아요. 기존 시작/중지 동작으로 명시적으로 추적을 중지하고 새 세션을 시작했을 때만 다시 평가해요.06의 명시적 역할/경로 전환이 새 추적 세션을 만드는 경우도 같은 경계를 따르고, 이번에는 재시도 버튼/영속 큐를 추가하지 않아요.

위치 수집과 이미 수락된 쓰기는 원래 사용자로 끝내고 기존 수신함은 계속 조회·확인할 수 있어요. ACK 실패는 별개로 PENDING 유지 후 명시 ‘확인’ 클릭에서만 재시도하는 정책을 적용해요. AlreadyReceived/Ineligible은 StorageFailure와 구별하고, 중복 PK는 새 세션에서도 같은 사용자/쪽지 중복 생성을 계속 막아요. 이전 세션 쓰기의 늦은 실패가 새 수신 세션을 중단시키지 않도록 결과도 격리해요.

05 §6/§8/P04/P08 및 부모 정본의 수신 실패·재허용/ACK 절에 반영하도록 요청해요. 실패 뒤 여러 위치/DB 신호에도 추가 쓰기0회, 명시 새 세션에서만 새 시도, 이전 쓰기 완료·새 UI 격리를 확인해야 해요. 판단만 완료했고 아직 수정·테스트 전이며 실제 Room/서비스 수명은 NOT_TESTED예요.

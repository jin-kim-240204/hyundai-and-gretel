# PR4 05 독립 스펙 리뷰

AI 독립스펙리뷰어(Codex) ·05 스펙· SHA `b428a330470558989651463dcd2f28bece9372db`.

**판정: 구현 전 계약 보완1건(B1), 정책 판단3건(P1~P3), 선택 스타일 개선0.** 먼저 B1 실제 표시/ACK(117행), P1 등록 전후 onError와 슬롯 정리(70행), P2 합성 전용 accuracy 충돌/부모 동기화(155행)를 논의해주세요. P3 수신 StorageFailure 뒤 재허용 조건(109행)은 그 다음 우선순위입니다. 각각 해당 문서 줄에 상황·근거·최소 대안을 남겼으며 추천을 승인된 정책으로 취급하지 않았습니다. 리뷰이 판단·반영·재검토 전 구현 착수는 보류합니다. 실제 구현 결함을 이미 발견했다는 뜻이 아니라 스펙의 상충/미확정 계약입니다.

대조한 타당한 부분: 같은 SDK 인스턴스와 요청 객체 소유권, pending 포함16·늦은ID tombstone·release 후 앱 반영 차단, 물리적 원격16 보장은 하지 않는 한계, ENTER와 등록 성공 후 이미안 보완의 공통 판정,04 currentCandidates 소비·400m/수신150m·2초/5초 분리, 과거 anchor 투영과 합성 direction 구분이 드러납니다. receipts 복합PK/FK·원본과 분리·단일 JOIN·멱등 ACK·만료 원본/legacy 보존·v1→2→3 이관, 수락 쓰기/ACK까지 서비스 종료를 지연하는 방향은03/04와 양립합니다. 메타데이터 부족 legacy를 임의 방향으로 채우지 않고 제외하는 제한도 명시돼 있습니다. 일반 Android 위치 API를 Pleos에 적용하지 않았습니다.

확인 근거: FusedLocation1.1.3 sources.jar(캐시 해시65744a9894172fee820bae4499ab1f99008d8cbc)의 FusedLocation,LocationData,ProximityAlertRequest/Listener/Event,LocationServiceManager,ProximityAlertListenerStub,BaseServiceConnector. onRegistered만 완료이며 add Unit은 완료가 아니고, remove는 권한 후 로컬 deactivate/원격 요청이며 원격 제거 오류는 내부 로그에 그칠 수 있습니다. release는 로컬 callback/목록/대기 호출을 정리하고 원격 해제를 시도하지만 원격 완료를 보장하는 근거로 사용하지 않았습니다. 공식 [초기화/해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release)·[위치](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access), [Compose Effect](https://developer.android.com/develop/ui/compose/side-effects), [SQLite conflict](https://www.sqlite.org/lang_conflict.html)을 확인했습니다. 정본/00/03/04/작성기록 및 현 서비스·원본·currentCandidates·과거 교훈을 대조했습니다.

PASS: 문서/배포물 소스 정적 검토, 21783e9..b428a33 diff --check. N/A: 스펙 단계의 코드/JVM/빌드 검사. NOT_TESTED: 새05 구현·proximity 실제SDK 등록/해제/늦은콜백·물리적한도, Room migration/트랜잭션/JOIN/ACK·Compose 노출/재생성·서비스수명·JNI/실차/E2E. 앱/Studio/adb/기기/에뮬레이터/계측은 실행하지 않았고 코드/의존성 수정도 없습니다. 과거 ae11722 PASS를 신규 증거로 쓰지 않았습니다. 결과 보고 후 리뷰이 판단을 기다립니다.

적용: pleos-connect ENV-02/VEH-03/PRIV-01, pleos-vehicle의 실제SDK/수명 경계, clean-code-review 9/15~19/22~27/28~32, android-compose-architecture §3/5~8, pleos-design 표시/오류/한번행동, ponytail-review. 신규제어/차량장치API없음.

## 인라인 117행

**B1 · 구현 전 계약 보완 필요: Composition 적용을 실제 표시 ACK로 쓰면 안 본 항목이 사라질 수 있습니다**

최초 실제 표시 뒤 ACK하려는 의도는 이해했습니다. 다만 ‘composition 적용 후’만 조건이면 현재처럼 전체 Column을 세로 스크롤하는 화면에서 viewport 밖 수신 항목도 composition에 들어가 효과가 실행될 수 있습니다. 수신 항목이 화면 아래에 있는데 ACK가 커밋되면 PENDING에서 다음 항목으로 넘어가 사용자는 첫 항목을 한 번도 보지 못합니다. 성공한 composition과 사용자가 볼 수 있는 상태는 다른 조건입니다. [Compose 공식 lifecycle](https://developer.android.com/develop/ui/compose/lifecycle)은 composition 진입/재구성을 설명하며 화면 노출 보장으로 정의하지 않고, [Effect 문서](https://developer.android.com/develop/ui/compose/side-effects)의 composition 완료 효과 역시 가시성 확인은 아닙니다.

최소 선택은 (A) 현재 사용자/항목이 맞고 화면이 RESUMED이며 항목이 실제 보이는 범위에 배치됐을 때만 한 번 ACK하거나, (B) 작은 ‘확인’ 동작으로 그 항목을 명시적으로 ACK하는 것입니다. 자동 ACK를 유지하려면 A의 관찰 가능한 노출 조건을 문서에 명시하는 쪽을 추천합니다. 반대로 composition 완료 자체를 제품상 ‘표시’로 정하려면 화면 밖에서도 이력이 처리될 수 있다는 계약 변경을 정본과 함께 합의해야 합니다. 인간이 읽었다는 보장이나 큰 가시성 프레임워크를 요구하지 않습니다.

P10에는 화면 아래/화면 비활성에서는 ACK0회·PENDING 유지, 유효한 항목 노출 뒤1회, actor/항목 교체 직전 예약된 ACK의 격리를 넣으면 됩니다. 현재 스펙의 실제 표시 전 PENDING 보존을 구현자마다 다르게 만들 수 있어 구현 전 보완이 필요합니다. NOT_TESTED: 실제 Compose 배치/렌더/스크롤/기기 실행.

AI 독립스펙리뷰어(Codex) · SHA `b428a330470558989651463dcd2f28bece9372db` · docs/specs/road-notes/05-proximity-and-receipts.md:117

## 인라인 70행

**P1 · SDK onError는 등록 전과 등록 후를 구분해야 슬롯 정리 기준이 명확합니다**

pending까지16개에 세고 늦은 ID는 소유 SDK로 정리하는 정책은 좋습니다. 다만 ‘onError까지 슬롯 유지’와68행의 일반 onError 처리만으로는 등록 뒤 오류가 왔을 때 슬롯/ID를 어떻게 끝낼지가 남습니다. 등록 완료→onError→슬롯 해제·새 후보 등록으로 구현하면, SDK 쪽 이전 등록은 남아 있는데 앱은 빈 슬롯으로 셀 수 있습니다.

SDK1.1.3 sources.jar의 ProximityAlertListener는 onError를 ‘등록 또는 처리 오류’로 정의합니다. ProximityAlertListenerStub.notifyError는 isRegistered=false일 때만 onRegistrationFailed를 호출하고, LocationServiceManager는 그 콜백에서만 로컬 등록을 제거합니다. 등록 후 onError는 자동 해제/비활성화 계약이 아닙니다. 웹의 일반 위치 오류를 proximity 해제로 해석하지 않았습니다.

추천은 등록 전 실패(소스에서 로컬 제거됨)는 해당 pending을 종료하고, 등록 후 오류는 해당 요청을 즉시 앱에서 무효화한 뒤 알려진 alertId 제거를 시도하는 정책입니다. 제거 실패는72행의 재시작 필요/추가 등록 차단을 그대로 적용하면 됩니다. SDK 중복 콜백도 상태별로 무시하고, 오류를 성공 보완 평가로 우회하지 않습니다. 이 정책을68~72/P05에 한 문장으로 구분하면 요청 상태를 과하게 늘릴 필요가 없습니다. 등록 후 오류를 세션 전체 수신 실패로 끝낼지 해당 요청만 끝낼지는 리뷰이 맥락을 확인해주세요. NOT_TESTED: SDK 실제 등록/오류/원격 해제.

AI 독립스펙리뷰어(Codex) · SHA `b428a330470558989651463dcd2f28bece9372db` · docs/specs/road-notes/05-proximity-and-receipts.md:70

## 인라인 155행

**P2 · accuracy 충돌은 합성 전용 계약으로 한정해 해소하는 방향에 동의합니다**

SDK1.1.3 LocationData 원문을 대조했으며 latitude/longitude/altitude/heading/confidence/timestampMillis만 있고 미터 accuracy는 없습니다. 현재 버퍼도 좌표/수신단조시각뿐입니다. confidence를20m로 환산하거나, abs(y)<=20m의 경로 띠를 GPS accuracy20m 충족으로 부르는 구현은 근거가 없습니다.

제 의견은 이 제안을 **합성 프로토타입 전용**으로 채택하는 것입니다. 실제 측정 정확도 조건을 충족했다고 하지 않고, 신뢰된 앱 내부 경로/방향 설정+알려진 fixture 투영을 수신 자격으로 삼으면 현재 단계는 구현 가능합니다. 실도로 위치 정확도는 미검증·범위 밖으로 유지하고, 메타데이터 없는 legacy 제외·문맥 없는 신규 원본 저장 허용도 유지하면 됩니다. 2초 수신 신선도는 후보5초와 구분해 유지하되 느린 SDK 콜백 때 늦게 수신될 수 있다는 현재 제한을 수용하는 선택입니다.

구현 전 부모 정본§8.1의 accuracy/방향 부호/선정 순서를05와 동기화하고 그 SHA를 기록해주세요. 단순히 테스트에서 accuracy 조건을 지워 PASS시키는 것은 완료 근거가 아닙니다. 이는 승인 요청이 아니라 리뷰이에게 제시하는 정책 의견입니다. NOT_TESTED: 실제 위치 품질·heading/confidence 의미·합성 설정과 실제 GPS 일치/E2E.

AI 독립스펙리뷰어(Codex) · SHA `b428a330470558989651463dcd2f28bece9372db` · docs/specs/road-notes/05-proximity-and-receipts.md:155

## 인라인 109행

**P3 · 수신 저장 실패 뒤 다음 위치 이벤트가 자동 재시도가 되는지 정해주세요**

수락한 쓰기를 끝내고 기존 사용자로 결과를 격리하는 수명 정책은 명확합니다. 남은 부분은 StorageFailure 뒤 재평가입니다. 실패하면 receipt가 없고 진행 중 요청도 끝나므로, 다음 위치/후보 이벤트에서 같은 쪽지가 다시 선택·등록·이미 안 보완을 거쳐 INSERT를 재시도할 수 있습니다. 정차 중 반복 위치 콜백만으로도 무한 반복될 수 있어 ‘무한 재시도 없음’과 어떻게 맞출지 구현자가 선택하게 됩니다.

최소안은 저장 실패를 해당 수신 세션의 명시 실패로 유지하고 자동 수락을 멈춘 뒤 명시적 추적 재시작에서 다시 평가하도록 하는 것입니다. 위치 수집과 이미 수락된 다른 쓰기는 계속 정리하며, receipt PK는 같은 사용자/쪽지 중복을 계속 막습니다. 개별 항목 재시도가 꼭 필요하면 재시도 버튼으로 범위를 정할 수도 있지만 지금 별도 큐/백오프 계층을 선제 도입하지 않는 쪽을 추천합니다.

ACK는117행에 명시적 재시도 정책이 있으므로 수신 INSERT 실패도 ‘다음 신호만으로 재시도하는가/어떤 동작이 다시 허용하는가’를 구분해 적으면 좋겠습니다. P04/P08에는 실패 뒤 여러 위치/DB 신호에도 추가 쓰기0회, 허용된 명시 동작 뒤에만 새 시도라는 조건을 넣을 수 있습니다. 자동 재평가를 의도한 맥락이 있다면 횟수/종료 조건을 선택해주세요. NOT_TESTED: 신규 수신/재시도 구현과 DB 실행.

AI 독립스펙리뷰어(Codex) · SHA `b428a330470558989651463dcd2f28bece9372db` · docs/specs/road-notes/05-proximity-and-receipts.md:109

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867300684
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544866200
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544866211
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544866219
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544866222
구현 착수 보류·리뷰이 판단 대기. 기존 미커밋/다른역할파일 보존.


## 5957bf2 수정 스펙 재검토

AI 독립스펙리뷰어(Codex) 재검토 · SHA `5957bf24a06f8d8e5e7966fff03c39c37a95ba7a` / 부모 `fdc51fa` / 병합 `de92dca`. **판정: 스펙 관점에서05 구현 착수 가능. blocker0 / 미결정책0.** 기존 B1/P1/P2/P3 네 항목이 리뷰이 판단대로05·00·정본에 반영됐고 부모/병합 조상·정본동일도 정적으로 확인했습니다. 명시확인ACK/시각의미, 등록전후오류의 수신Failed·리소스정리·위치/수락쓰기유지, 합성전용자격/accuracy날조금지·수신2초유지, StorageFailure 자동재시도금지·stop/newstart 재허용을 확인했습니다. 추천했던 자동 가시성 ACK를 강제하지 않고 리뷰이의 작은 명시확인 선택을 기준으로 재검토했습니다.

수정 입증된 지정4개 스펙스레드만 resolve했고 기존 기기 검증 스레드는 유지합니다. 새 선택개선 범위를 추가하지 않았습니다. PASS: 문서/정본/판단 대조, b428a33..5957bf2 diff --check. 코드/의존성변경없음. N/A: 문서 재검토의 JVM/빌드. NOT_TESTED: 신규05 구현·proximity 실제SDK 등록/해제·늦은콜백·슬롯·Room migration/수신트랜잭션/JOIN/ACK·Compose 확인·서비스수명·실기기/E2E. 앱/Studio/adb/기기/에뮬레이터/계측은 실행하지 않았습니다. 이는 구현 착수 전 스펙 판정이며05 구현/실행완료가 아닙니다. 결과 보고 후 대기합니다.

게시 확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867373540
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544924957
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544925183
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544925492
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544925735
지정4스펙스레드 isResolved=true API 확인. 기존무관변경보존.

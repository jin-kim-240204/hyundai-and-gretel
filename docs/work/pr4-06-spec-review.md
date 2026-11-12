# PR4 06 독립 명세 리뷰

AI 독립스펙리뷰어(Codex) ·06 명세· SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f` / 작성요청 issuecomment-6811920542.

**판정: 계약 보완1건(B1 기존legacy PENDING 조회/ACK), 구현 전 정책 선택2건(P1 A출발 여유/P2 기능별UX제한), 선택 스타일 개선0.** 세 지적을 해당 줄에 상황·근거·최소안으로 남겼습니다. 먼저 B1 legacy/릴리스 호환, P1 600m+gate오차, P2 규제/정본 현황을 논의해 주세요. 이는 명세의 호환/검증 조건이며 현재06 실행 코드 결함을 찾았다는 뜻은 아닙니다. 리뷰이 판단·부모 정본 동기화·반영 재검토 전 구현 착수는 보류합니다.

§12 다섯 정책에 대한 의견: ①B600m/약45초와 경로밖 명시 실패는 지지하되 A600/첫표본은 P1 보완 필요. ②WEB_CONSOLE/EXTERNAL_ADB 모두 실제SDK를 경유하고 직접 REPLAY/DemoClock 대체를 제외하는 안에 동의합니다. 허용 JVM Fake를 제품fallback과 혼동하지 않아야 합니다. ③nullable v4/활성상태행·현재시나리오만 확인reset/다른소속·legacy 물리보존은 지지하되 B1 조회/ACK 경계는 확정해야 합니다. ④두점 gate25m/5m/단계15초는 통제된 시연용 제안으로 채택 가능하며, 보장되지 않는 SDK측정원천 인증으로 확대하지 않는86행 한계를 유지하면 됩니다. 더 많은 점/고정sleep/복수gate알고리즘을 추가할 이유는 없습니다. D 정방향/R 명시오류도 현재05 역방향 순수 자격회귀를 없애지 않는 범위에서 채택 가능. ⑤기본UI유지+기능별제한/DO현황정정은 P2 기준을 추천합니다.

gate·입력 대조: 새 리스너/콜백진입단조시각만으로 새 측정을 보장하지 않는 SDK1.1.3 한계를 인정하고, raw를 Ready 전 버퍼에 넣지 않는 점, probe/목표를 직접 LocationFix로 만들지 않는 점은 타당합니다. 두 단계 제한시간/소유권/취소·오류·null 기다림·원천인증 한계를 이미 정의했으므로 재지적하지 않았습니다. mock provider 성공은 Pleos수신 성공이 아니고, 외부ADB를 앱이 중지했다고 하지 않으며 provider정리/권한/콘솔오류에 조용한 fallback이 없습니다. 실제 전달은 오늘 미검증인 채 구현 가능한 최소상태로 두면 됩니다.

역할/reset 대조: writes0 검사→전환소유권→신규수락차단이 Main에서 suspend없이 수행되고 일반중지는수락쓰기종료를허용합니다. 기존03/05수명·dataSync ACK·관찰토큰·현재시나리오만원자reset/롤백·nullable기존행보존 방향과 양립합니다. release제외와공통이관/실제SDK/ACK유지 중 B1 외에는 새충돌을확정하지않았습니다. 제품/인앱패널분리·패널가림터치차단/명시ACK·여닫기의업무무영향·KEEP_SCREEN_ON의가시Activity범위도 읽었습니다. 기존미추적와이어프레임은 참고만 했으며 수정/커밋하지 않았습니다.

근거: 해당SHA06/00/05·정본fdc51fa·작업기록·현재SDK/서비스/수신함/DB·ae11722 DebugGps 및 debug-start-positions, 기존wireframe. 공식 [LocationManager](https://developer.android.com/reference/android/location/LocationManager)·[Pleos 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)를 확인했습니다. Android testprovider 지원이 Pleos 전달 보장인 것으로 추정하지 않았습니다.

PASS: 문서/현코드/이력 정적대조, fdd9bbf..c0c4f62 diff --check, A596→B-4 경계의순수산술. N/A: 명세단계 JVM/Gradle빌드. NOT_TESTED: 신규06구현·consoleHTTP/gear실형식·mock/provider수명/SDKcachegate/외부입력·Roomv4/reset/서비스·FGS·실제UI/CarUx/DO·기기·E2E. 앱/Studio/adb/기기/에뮬레이터/계측 실행 및 코드/의존성 수정은 하지 않았습니다. 과거SDK PASS를06 PASS로 복사하지 않았습니다. 결과보고 후 리뷰이 판단 대기.

적용: pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review15~19/22~27/28~32, android-compose-architecture §3/5~8, pleos-design·pleos-vehicle 주행규제/표시·소유권, ponytail-review.

## 인라인 114행

**B1 · 계약 보완 필요: v3의 기존 PENDING이 보존돼도 확인할 경로가 없어질 수 있습니다**

nullable scenarioId로 과거 소속을 추측하지 않고 reset도 현재 시연만 삭제하는 방향은 타당합니다. 다만 기존 v3 원본은 전부 scenarioId=null이고,118행은 이력 관찰까지 활성 시나리오로 한정하며114행은 null legacy 이력을 활성 시연에 섞지 않고 별도 이관/소속 UI도 제외합니다. 그러면05에서 이미 수신한 PENDING을 갖고06으로 업그레이드한 경우, 새 시연을 만들기 전후 모두 그 항목이 현재 inbox 조회에서 빠집니다. release에는 시연 도구가 없으므로134행의 ‘기존 로컬 수신함 ACK 유지’도 이 조회만으로는 충족하지 못합니다. 원본을 물리적으로 남기는 것과 기존 수신 기록을 조회·확인할 수 있는 것은 다른 계약입니다.

작은 추천은 **제품 수신함의 기존 사용자별 legacy 조회/명시 ACK는 유지하고**, 활성 시나리오 필터는 신규 후보·등록·데모 대상/진행축·시연 카운트에 적용하는 것입니다. 현재 시연 외 기록을 같은 한건 큐에 둘지 별도 조회 범위를 허용할지는 판단이 필요하지만, 데이터에 임의 scenarioId를 채우거나 삭제할 필요는 없습니다. 다른 선택은 기존 legacy 확인 기능을 보류하는 호환성 변경을 명시적으로 채택하는 것이며, 이 경우134행/05/정본의 보존·조회 약속도 함께 바꿔야 합니다. 추천을 승인된 정책으로 간주하지 않습니다.

확인 조건: v3의 미확인 receipt→v4 migration→활성 시나리오 없음/새 시연 생성/reset 뒤에도 해당 사용자의 과거 PENDING을 조회·ACK할 수 있고, 후보에는 legacy가 자동 혼합되지 않으며 reset은 그 원본/이력을 지우지 않아야 합니다. debug/release 양쪽 조회 경로를 구분해 주세요. NOT_TESTED: 실제 migration/Room/화면/ACK 실행.

AI 독립스펙리뷰어(Codex) · SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f` ·06:112~118·134행,05§7 조회/확인 계약 대조.

## 인라인 69행

**P1 · A600m를 고정하면 허용된 gate 오차 안에서도 첫 제보의 B 출발점이 경로 밖일 수 있습니다**

A 기본값을600m로 옮겨 첫 표본 제보도 B 출발을 만들려는 의도는 이해했습니다. 다만81행은 실제SDK progress가 목표±5m이면 gate를 엽니다. 예를 들어 실제 확인 표본596m는 A600 목표와4m 차이라 통과하지만, 이를 첫 anchor로 저장하면 B=596-600=-4m로67행의 범위 오류가 됩니다. R1 역투영식만으로 직접 산술 확인했습니다. SDK가 그 오차를 실제 냈다는 실행 주장은 아닙니다.

선택지는 (A) A 기본 출발에 작은 여유를 두기(예:610m), (B) A600을 유지하되 실제 확인 anchor>=600m인 경우만 첫 제보의 B 출발을 보장하고 그보다 작으면 명시 범위 오류를 수용하기입니다. 저는 ±5m gate를 엄격한 정확도 보장으로 바꾸지 않으면서 첫 표본 제보를 유지하는 A를 추천합니다. R1 확장·B clamp·30초 대기·추가 준비 상태는 필요 없습니다. A610이면 설명의 anchor/B 예시도10m만 이동하면 됩니다.

A600이라는 숫자 자체가 필수 시연 조건이라면 B도 가능하지만, 최초표본 제보 뒤 B 출발이 항상 가능한 것으로 안내하지 않도록 명시해 주세요. D02/D03에 목표-허용오차 표본→제보anchor→B600m 역산을 연결해 검사하면 됩니다. NOT_TESTED: 실제SDK 좌표오차/출발점 주입.

AI 독립스펙리뷰어(Codex) · SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f` ·06:67/69/81행. 정책 선택이며 현재 구현 결함은 아닙니다.

## 인라인 177행

**P2 · 동일 화면 유지와 기능별 UX 제한 반영을 분리해 정본에 확정하면 좋겠습니다**

정본의 ‘주행 중 같은 UI’와 와이어프레임의 ‘미확인 규제 일괄 잠금’ 중 하나만 택할 필요는 없어 보입니다. [Pleos 공식 규제 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)는 CarUxRestrictions/ActiveRestrictions에 따른 기능별 적용을 요구하고, DO 선언도 이를 면제하지 않는다고 설명합니다. ‘OS가 앱을 가리면 우회하지 않는다’만으로 앱 내부 버튼의 기능별 제한까지 반영되는 것은 아닙니다.

제안은 제품/패널의 기본 구조는 유지하고, 적용 가능한 UX 제한에 따라 해당 행동만 막는 계약입니다. 속도0/P를 허가로 삼거나 주행 시 전면 축소 화면을 만들 필요는 없습니다. DO는 현재 Manifest에 없으므로 부모의 ‘기존 DO 선언 유지’는 현황 오류로 먼저 정정하고, 이번에 새로 선언할지와 별개로 판단하면 됩니다. 아직 카테고리별 조작 매핑을 확정하지 못했다면 그 규제 근거/미검증을 명시하고, 같은UI유지 또는 DO만으로 주행 허가를 확보했다고 하지 않는 현재 제한을 유지해주세요.

독립 판단에서는 ‘조건 없이 모든 debug 조작 활성’ 또는 ‘무조건 주차 전용’으로 바꾸기보다 이 기능별 규칙을 정본과06에 맞추는 쪽을 추천합니다. 구현 시 실제 제한 신호·변경 중 동작은 별도 검증하며, 신규 일반 규제 엔진/오버레이 권한은 요구하지 않습니다. NOT_TESTED: 현재환경 ActiveRestrictions/DO 효과·실주행 UI·기기.

AI 독립스펙리뷰어(Codex) · SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f` ·06§12 항목5(177행) 및44행,정본§9/현재Manifest 대조.

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867776712
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545264510
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545264523
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545264528
코드/와이어프레임변경없음. 리뷰이판단대기.


## 6f33659 수정 명세 재검토

AI 독립스펙리뷰어(Codex) 재검토 · 고정 SHA `6f336599d71eaa23227907ded09c2ad5e6cadc23` / 부모 `2ca00c0ee411faa731390601619c38747be71279`. **판정: blocker0 / 미결정정책0 / 명세관점06 구현착수가능.** 기존4545264510/4523/4528 및 다섯정본정책의 리뷰이판단·PR반영답글·06/00/05·부모정본·UX01~09매핑표를 대조했습니다. legacy전체제품수신함/ACK와 scenario신규평가필터 경계,A610±5/B5..15,두mode실제SDK/noREPLAY,현재scenario원자reset/보존,두점gate한계·CarUx기능별제약이 일치합니다. 채택된정책만 재검토했으며 새선택개선범위를 추가하지 않았습니다.

PASS: 부모2ca00c0가현HEAD조상이며정본파일동일 확인,명세/판단/원댓글대조,diff --check. 지정된 세 명세스레드만 반영확인후resolve했고 기존 기기검증스레드는 유지합니다. 코드/의존성/와이어프레임 수정없음.

N/A: 문서재검토의JVM/빌드. NOT_TESTED:06구현/콘솔·mock/provider수명/실제SDKcachegate/외부입력/Roomv4·reset/CarUx실제연동·기능별표시/DO효과/FGS/UI/기기·E2E. 매핑표와finite gate가 실제규제준수/측정인증/런타임PASS를 보장한다고 하지 않습니다. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지 준수. 이결과는 명세구현착수판정이며06구현완료가 아닙니다. 결과보고후대기합니다.

게시확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867895005
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545362536
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545362767
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545363027
지정3스펙스레드 isResolved=true API확인.

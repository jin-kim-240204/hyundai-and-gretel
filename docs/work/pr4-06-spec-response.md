# PR4 06 명세 리뷰이 판단

게시 완료: [legacy 제품 수신함](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545281093), [출발점·SDK 입력·gate](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545281247), [CarUx·DO](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545281461). 각API응답의in_reply_to_id를확인했어요. 조정자보고후대기해요.

대상 SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f`, 독립리뷰5867776712. 적용 pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review15~19/22~27/28~32, 기존 Compose 구조/pleos-design/pleos-vehicle의규제·보존·소유권 기준. 범위는 판단기록/3스레드답글이며 코드/명세수정·resolve·런타임실행은 하지 않아요.

## 3건 판단과 다섯 정본 정책

B1/P1/P2 모두수용해요. 현재코드의06실행결함판정이아니라구현전호환/정책보완이에요.

1. 거리/출발점: B요청목표는실제선택anchor에서600m뒤, A기본610m. gate±5m라면첫anchor605~615→B5~15. 실제anchor가경로밖목표를만들면실패,clamp/경로확장/30초대기없음. A60초예시는현재1210/anchor약910/B약310, 접근150m까지약45초. 요청목표간격과실제수락SDK표본의오차는구분해요.
2. 입력: WEB_CONSOLE/EXTERNAL_ADB양쪽실제SDK경유. 직접REPLAY/DemoClock제품대체/조용한fallback제외, 허용JVMFake는유지. 외부ADB중지를앱이보장하지않고provider/입력배타정리실패는명시실패예요.
3. v4/reset: nullable scenario/단일활성상태행/확인후현재시연만원자reset·새ID채택. 다른시연/nulllegacy원본·receipt는보존. **제품수신함은기존recipient별전체JOIN/같은한건큐/명시ACK를debug·release모두유지**하고시나리오필터는후보·등록·신규수신·대상·진행축·시연카운트에적용. 별도legacy화면/소속채움없음. 관찰소유권격리는유지해요.
4. gate/기어: probe25m이격/목표5m/단계15초·전환소유권·gate전raw차단채택. D정방향/R명시오류,05역방향순수회귀유지. 이는제한된시연용캐시배제휴리스틱이며원천인증/새측정/실도로freshness보장아니에요. 점/고정sleep/복수gate추가없음.
5. UI/DO: 기본구조유지+실제CarUx기능별제한. 현재DO없음정정/이번새선언없음. NO_SETUP에는시연설정명령(역할·모드·출발점/reset/입력시작)을프로젝트분류로차단,중지/취소/닫기는유지. 규제미연결/오류는시연설정불허·이유표시,전체제품잠금아님. 문자열/탐색상한은플랫폼값적용/DB불변/자동ACK금지. 추가적용항목은공식근거매핑표로반영후재검토. 제한활성화시미수락전환취소/정리·수락DB완료·해제시자동재개없음. 일반규제엔진없음.

## 부모 동기화와 다음 단계

작성자는06§2~8/§9/§11~12와D02/D03/D05/D06/D08,05§7제품수신함조회범위,00진행/입력정책을맞춰요. 부모정본§9의DO현황/기능제약,§10A/B거리·출발/시간·reset,§11입력REPLAY대체/시험시계/증거범위,관련모델·조회/ACK·단계표를동기화해야해요. 특히모든옛수치300m/15초·A0m/600m,직접REPLAYfallback,전체삭제/자동청소,‘기존DO’문구를일관되게정리해요. 기존설계브랜치별도문서커밋·동기화SHA기록후독립재리뷰까지**구현금지**예요. 이번채택을부모반영/전체승인/구현완료로취급하지않아요. 미추적와이어프레임은참고만하고수정하지않아요.

## 근거와 상태

06전체/독립리뷰/원격3코멘트+요약/현재정본,Manifest(location|dataSync,DO없음),RoomReceiptStore내ReceiptDao의recipient전용JOIN·keyedACK를대조했어요. 이전SDK1.1.3의캐시/수신시각한계와05정책을유지하며새SDK지원보장을하지않아요. 공식 [Pleos 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [CarUxRestrictions](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions) 확인(2026-11-12): ActiveRestrictions기능별적용/DO비면제,NO_SETUP·문자/내용제한값의의미를확인했어요. 구체시연설정조작분류·미연결시조작차단은프로젝트정책이에요.

PASS: HEAD일치·문서/현코드/공식근거정적대조, 순수산술 `610-5-600=5`, `596-600=-4`, `(600-150)/10=45` 확인. NOT_TESTED:06신규코드/JVM·실제SDKcachegate/입력HTTP/mock/외부ADB/Roommigration·reset·UI/CarUx/DO/FGS/실기기. N/A:명세판단단계빌드. 앱/Studio/adb/기기/에뮬레이터/계측실행없음,코드/명세/와이어프레임수정·resolve없음. 이전PASS재사용없음. 원격답글중복없음확인후게시해요.

## 각 스레드 게시 본문

### 4545264510

AI(Codex) 리뷰이 판단 · 검토 SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f`이에요.

수용해요. 현재 ReceiptDao.inbox는 recipient로만 한정한 JOIN이고 명시 ACK도 noteId/recipient 키로 처리해요.06에서 조회까지 활성 scenario로 제한하면 v3의 scenarioId=null PENDING을 물리 보존해도 확인할 수 없게 되므로, 기존05와 release 호환 계약을 유지해야 해요.

최소안은 debug/release 모두 제품 수신함을 ‘현재 사용자에 속한 보존된 모든 receipt’의 같은 한 건 큐로 유지하는 거예요. 활성 시나리오 없음·새 시연 생성·reset 뒤에도 null legacy와 다른 시나리오의 PENDING/기존 이력을 조회하고 원래 키로 확인할 수 있어야 해요. 기존 receivedAt→noteId 정렬·명시 확인·멱등 ACK·추적 STOPPED/dataSync 수명을 유지해요. 옛 항목이 앞에 있으면 먼저 확인할 수 있으며, 별도 legacy 화면/소속 변경 UI는 추가하지 않아요.

반면 신규 후보·등록·수신 자격·B 대상·진행축·시연 카운트는 활성 시나리오로만 제한해요. 제품 수신함의 전체 PENDING 수와 현재 시연 카운트는 의미를 구분해요. null legacy를 활성 시연 후보에 넣거나 임의 scenarioId로 채우지 않아요. 제품 큐의 DB 필터와 별개로 reset/역할 전환 뒤 옛 직접 UI 결과를 막는 관찰 소유권 격리는 유지해요.

nullable v4와 단일 활성 상태행, 확인 후 ‘현재 시나리오 원본+FK 이력만 삭제/새 ID 갱신’하는 원자 reset도 채택해요. 다른 시나리오/null legacy는 보존하고 실패 시 롤백해요.05 §7,06 §8/§9/D05/D06, 부모 정본의 시나리오 조회·reset·release 호환 절을 동기화하도록 요청해요. v3 PENDING→v4→활성 없음/생성/reset 뒤 조회·ACK 및 reset 범위·롤백을 검증해야 해요. 아직 수정·실행 전이며 migration/Room/화면은 NOT_TESTED예요.

### 4545264523

AI(Codex) 리뷰이 판단 · 검토 SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f`이에요.

수용하고 A 기본 출발610m를 채택해요. 허용된 목표±5m라면 첫 확인 anchor가605~615m이고 B=anchor-600은5~15m라서, A600 목표에서596 표본→B-4m가 되는 경계를 작은 여유로 피할 수 있어요. 순수 산술로 확인했으며 SDK가 실제로 이 오차를 냈다는 뜻은 아니에요.

B는 계속 실제 저장 anchor에서 정확히600m 뒤를 요청해요. R1 확장·B clamp·30초 대기·추가 gate는 넣지 않아요. A610은 기본 출발점의 여유일 뿐 모든 이후 anchor를 보장하는 조건은 아니므로, 실제 선택 anchor로 역산한 B 목표가 경로 밖이면 기존처럼 명시 실패해요. SDK에서 확인된 두 출발 좌표 간 실제 간격은 gate 오차가 있을 수 있어 debug에서 요청 목표와 수락 SDK 좌표를 구분해요.

예시는 A610→10m/s 60초 후현재1210/anchor약910/B약310으로 바꾸고,150m 접근까지 약45초는 유지해요. 실제 SDK 주기·선택 offset·기어/속도 변화에 따라 달라진다는 제한도 유지해요.06 §4/재현절차/D02/D03와 부모 정본 §10의 A0/B300/15초 및 관련 표·수식을 함께 동기화해야 해요.

요약의 두 점 gate도25m 이격/목표5m/단계15초로 채택하되 통제된 시연의 이전 캐시 배제 절차로 한정해요. WEB_CONSOLE/EXTERNAL_ADB 모두 실제 SDK 콜백을 통과하고 gate 전 raw는 버퍼에 넣지 않아요. 직접 REPLAY/DemoClock 제품 대체·조용한 fallback은 제외하고 JVM Fake 회귀는 유지해요. 콘솔 D 정방향/R 명시오류 정책도 채택하며05 역방향 순수 회귀는 유지해요. 부모 정본 §10/§11의 REPLAY 대체·시험시계·SDK 증거 설명을 구현 전에 맞춰야 해요. 실제 SDK gate/입력 전달은 NOT_TESTED이며 반영·재검토 전 구현 금지를 유지해요.

### 4545264528

AI(Codex) 리뷰이 판단 · 검토 SHA `c0c4f62a1d82d4a3ddbe19c0d86ae050f7f8653f`이에요.

수용해요. 현재 Manifest에는 DO 선언이 없으므로 정본의 ‘기존 distractionOptimized=true 유지’는 현황 오류로 정정해요. 이번06에서는 DO를 새로 선언하지 않고, 같은 UI나 선언만으로 주행 표시/조작 허가를 확보했다고 하지 않아요.

기본 제품/패널 구조는 유지하고 CarUxRestrictions의 실제 신호와 적용되는 기능별 제한을 반영하는 계약으로 정해요. 속도0/P를 허가로 쓰거나 주행 시 별도 축소 화면을 만들지는 않아요. 작은 매핑으로 역할·모드·출발점/reset/입력 시작 같은 시연 설정 명령은 NO_SETUP 적용 대상으로 취급하고, 해당 제한에서는 진입/실제 Main 명령 양쪽에서 막아요. 이는 이번 프로젝트의 보수적 조작 분류이며 SDK가 이 버튼들을 자동 분류해 준다는 뜻은 아니에요. 중지·취소·패널 닫기는 계속 가능하게 해요.

문자 길이/탐색 내용 제한은 해당 플랫폼 상한을 적용하되 DB 원문을 바꾸거나 항목을 숨겼다는 이유로 자동 ACK하지 않아요. 그 외 이 화면에 적용되는 규제 매핑은 공식 근거와 함께 작성자가 표로 명시하고, 확인되지 않은 항목을 허가로 간주하지 않아야 해요. 규제 신호가 미연결/조회 실패면 시연 설정을 허용 상태로 가정하지 않고 이유와 함께 막되 기존 데이터와 수락 쓰기를 보존해요. 이는 미확인 때 전체 앱/수신함을 잠그는 정책과는 구분해요.

실제 제한이 설정 전환 도중 활성화되면 명령 처리 직전에도 다시 확인하고, 금지된 미수락 전환은 취소·소유권 무효화·입력/SDK 정리를 수행해요. 이미 수락한 DB 쓰기는 기존 계약대로 끝내고, 제한 해제만으로 자동 주행/전환을 재개하지 않아요. 새 일반 규제 엔진/OS 오버레이 권한은 추가하지 않아요.

06 §2/§3/§12/D08, 부모 정본 §9·단계표의 DO 현황/기능별 제한을 동기화하고 미추적 와이어프레임은 수정하지 않아요. 공식 규제 가이드의 기능별 반영 의무와 프로젝트 매핑을 구분하며 실제 ActiveRestrictions/DO 효과/주행 UI는 NOT_TESTED예요. 부모 반영·매핑 보완·독립 재검토 전 구현은 보류해요.

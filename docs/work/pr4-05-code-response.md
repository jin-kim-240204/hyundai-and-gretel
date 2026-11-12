# PR4 05 코드 리뷰이 판단

게시 완료: [STOPPED ACK FGS 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545098096), [B→C→B ACK 소유권 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545098464). 각 API 응답의 in_reply_to_id를 확인했어요. resolve 없음. 보고 후 대기해요.

대상 SHA `a58efa249960bbaa6ecbca73777150bc2fb06812`, 독립리뷰5867564793. 적용: pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review15/17/23~25/27/28/32, 기존 Main한정/서비스수락쓰기수명 기준. 요청범위는 근거대조·최소수정계약·원격답글·기록이며 제품코드/스펙수정·resolve·앱/adb/기기/에뮬레이터/Studio/계측실행은 하지 않아요.

## 판단과 변경 범위

두 건 모두 필수수정으로 수용해요. 새 제품정책을 확장하기보다 합의된 위치와무관한수신함확인·수락쓰기유지·이전소유UI결과격리를 충족하는 수정이에요.

4545085650: LocationTrackingService의 storage hold FGS 타입을 수집중location/STOPPED저장전용dataSync로 구분하고 Manifest타입/권한/API분기까지 함께 맞춰요. 타입상수만 바꾸거나 위치권한재요청으로회피하지않아요. 수락전foreground/started유지성공,실패시store미호출·PENDING/오류,수락후writes/마지막unbind수명/종료정리를보존해요. 현재target34/min28,Manifestlocation만선언,confirm의onWriteAccepted예외시DB미호출을대조했어요. 데이터동기화의local file processing 사용례로선택하며별도서비스/WorkManager/SDK재시작은추가하지않아요. 실제FGS유지중타입전환과플랫폼예외경로는작성자가검증한범위만보고해야해요.

4545085659: recipient 관찰의소유기간토큰1개+ACK수락시캡처로직접UI결과를격리해요. 같은사용자ID여도새소유기간이면catch/finally의inbox반영금지. 수락쓰기Job은취소하지않고원래DB키완료·내부단일ACK잠금/writes/onFinished정리는항상수행해요. 실제옛DB커밋이JOINFlow를통해새B에보이는것은정상이며임의로숨기지않아요. 관찰교체시진행중잠금을풀어동시ACK를새로허용하는정책변경은요구하지않아요.

작성자후속수정범위: LocationTrackingService/AndroidManifest·필요한좁은타입선택Fake검증,ReceiptRepository/ReceiptRepositoryTest,서비스쓰기수명회귀/작업기록. 공통정본의명시ACK/위치수집과수신함분리/수락쓰기보존/이전결과격리정책은변경없고구현세부기록만동기화하면돼요. 기존미커밋polling/타역할문서보존. 수정및허용회귀통과·독립재리뷰전05완료판정보류예요.

## 확인한 근거·검증 한계

로컬HEAD대상일치,실제원댓글2개/답글없음,독립리뷰기록,실제Service/Manifest/ReceiptRepository·LocationSource/기존테스트,반례 `/private/tmp/roadnotes05-review-probe/AckOwnerProbeTest.kt` 및 `/private/tmp/roadnotes05-ack-owner-probe-result.xml`를대조했어요. XML은old B failure→new B inbox.error=true 1FAIL이에요. 이건production Repository+Fake반례로실제Room/역할UI실행은아니에요. 정상25PASS/전체69PASS는타역할기록으로이번직접재실행결과아니에요.

공식 [Android14 FGS runtime checks/dataSync/location](https://developer.android.google.cn/about/versions/14/changes/fgs-types-required?hl=en) 확인(2026-11-12): location은위치권한런타임전제,dataSync는타입권한선언+local file processing사용례/위치권한런타임전제없음을확인했어요. .com service-types 페이지는fetch timeout이라공식미러로확인했어요. 앱포그라운드시작제약전반의면제나OS강제종료후완료보장을뜻하지않아요. 이번지적은Android서비스계약이며Pleos위치/proximity동작을추정하지않아요.

PASS: 코드/반례/공식문서정적대조. FAIL(독립리뷰실행기록): ACK소유기간반례1건. NOT_TESTED: 이번직접테스트/빌드,수정안실행,AndroidFGS/권한철회/마지막unbind·Room·SDK·UI. N/A: 제품코드변경검증(이번변경없음).

확인조건: STOPPED/위치권한없음ACK→저장도달,승격실패→store0/카운트누수없음,수집중location유지,저장완료뒤종료; B→C→B옛성공/실패UI격리·원래키쓰기완료/정리·현재B새실패정상표시. 기존제보/수신/ACK수락수명회귀도유지해요.

## 원격 답글 본문

### 4545085650

AI(Codex) 리뷰이 판단 · 검토 SHA `a58efa249960bbaa6ecbca73777150bc2fb06812`이에요.

필수 수정으로 수용해요. holdStorageForeground→location 타입 startForeground가 ReceiptRepository.confirm의 onWriteAccepted보다 먼저 실행되고, 승격 예외 시 store.confirm이 호출되지 않는 경로를 확인했어요. target34/Manifest location 선언과 Android14의 location FGS 런타임 위치 권한 조건을 대조하면, GPS가 필요 없는 과거 수신함 확인이 위치 권한에 종속되는 문제가 맞아요. 기기에서 재현한 근거로 해석하지는 않아요.

최소안은 같은 서비스에서 수집 중 location 타입 유지와 STOPPED의 저장 전용 dataSync 승격을 구분하는 것으로 정해요. Manifest에 location|dataSync 및 FOREGROUND_SERVICE_DATA_SYNC 권한을 선언하고, 실제 전달 타입/API 분기를 함께 맞춰야 해요. 저장만 하는 승격에 location 비트를 함께 넘기거나 SDK를 다시 시작하거나 위치 권한을 재요청하지 않아요. dataSync의 local file processing 사용례를 근거로 선택하며 별도 서비스/WorkManager는 추가하지 않아요.

쓰기 수락 전 필요한 started/foreground 유지에 성공해야 하고, 승격/started 전환 실패면 쓰기를 수락하지 않은 채 PENDING·명시 오류를 유지해요. 성공하면 기존 쓰기 카운트로 마지막 unbind 뒤 완료·마지막 쓰기 뒤 종료 요청을 보존해요. 정상 위치 수집과 수집 중지 후 남은 쓰기 경로도 함께 확인해야 해요.

위치 권한 없음+STOPPED에서 타입에 location이 없고 ACK가 저장 경계에 도달하는지, 승격 실패면 저장 호출0회/카운트 누수 없음인지, 수집 중에는 location 유지인지 Fake/정적으로 확인하도록 요청해요. 공식 Android FGS 조건과 Pleos SDK 계약은 별개예요. 아직 수정·재검증 전이며 실제 foreground 승격/마지막 unbind/Room 실행은 NOT_TESTED예요.

### 4545085659

AI(Codex) 리뷰이 판단 · 검토 SHA `a58efa249960bbaa6ecbca73777150bc2fb06812`이에요.

필수 수정으로 수용해요. 실제 ReceiptRepository와 B→C까지만 다루는 기존 테스트, 임시 AckOwnerProbeTest 및1 FAIL XML을 대조했어요. recipient ID가 다시 B가 되면 옛 ACK의 catch/finally가 새 B inbox를 바꿀 수 있으므로, 사용자 ID만으로 소유 기간을 구별할 수 없다는 근거가 맞아요. 현재 A 고정 UI에 역할 전환이 이미 있다는 주장은 아니고, 제공된 Repository 계약의 결함으로 판단해요.

최소안은 현재 recipient 관찰 소유 기간에 지역 세대/토큰 하나를 두고 ACK 수락 시 캡처하는 방식이에요. 오류와 confirming 등 화면 상태 반영은 현재 사용자 ID와 소유 토큰이 모두 맞을 때만 허용해요. 수락한 쓰기는 계속 살아 있어야 하므로 그 쓰기 Job의 active 여부만 검사하거나 옛 ACK를 취소하는 방법은 맞지 않아요. 범용 SessionManager도 추가하지 않아요.

store.confirm은 원래 noteId/recipientId로 끝내고, 내부 단일 ACK 잠금·writes 감소·onFinished 정리는 UI 소유권과 무관하게 반드시 수행해요. 기존 단일 진행 정책을 유지하고 관찰 교체 때 잠금만 먼저 풀어 옛 finally가 새 작업을 정리하는 경로를 만들지 않아요. 옛 작업이 정상 커밋한 실제 DB 변경을 새 B의 JOIN Flow가 관찰하는 것은 유효한 데이터 갱신이며, 차단할 대상은 옛 작업의 직접 UI 오류/confirming 반영이에요.

B→C→B에서 옛 성공/실패가 새 inbox 오류/confirming을 덮지 않고 원래 DB 키 쓰기·카운트 정리가 끝나는지, 현재 B의 새 ACK 실패는 정상 오류인지 회귀로 요청해요. 이번에는 기존 반례 소스·결과를 검토했으며 직접 재실행하거나 수정하지 않았어요. 실제 Room/역할전환UI/기기 동작은 NOT_TESTED이며 수정·검증·재리뷰 전 완료 판단은 보류해요.

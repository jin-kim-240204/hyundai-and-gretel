# 05 접근·수신 구현

기준5957bf2/부모fdc51fa. 사용자 전달: 독립 스펙 재검토5867373540 blocker0/미결정책0,4스레드 해결. 이번 단계만 구현하며06/Gleo/서버/가상GPS/오버레이 제외. 기존 사용자 PleosLocationSource polling 및 다른 역할 기록 보존, 명시 stage.

적용: pleos-connect ENV-02/VEH-03/PRIV-01, android-compose-architecture MVVM/Repository/서비스 수명, clean-code-review 취소/실패/단일상태, ponytail 최소구현. DOC-1ba06876919b#초기화해제: 동일 SDK 등록/해제/release 대칭; DOC-402de45b9f3d#권한: 기존 dangerous FUSED_LOCATION 사용; DOC-9844e9eaad3c#콜백: 실제 Main 처리 전 세션/요청 검사. 공식 원문·SDK1.1.3 배포물/ae11722 조사 근거는 승인05 §2와 기존 기록 재사용, 새 계약 발명 없음.

묶음1 합성 경로/nullable 메타데이터·DBv3 migration, 묶음2 receipts 트랜잭션/JOIN/명시ACK, 묶음3 proximity/서비스/최소UI 연결·수명. 각 묶음 허용 검사 뒤 기능별 커밋/push. JVM·Gradle빌드/lint/하네스·패키징·schema 정적 확인, 계측 작성/컴파일만. 앱/Studio/adb/기기/에뮬레이터/계측 실행 절대 금지. 실제 Room/SDK/UI/JNI/서비스 실행 NOT_TESTED, 독립 코드리뷰 전 전체 완료 아님.

## 묶음1

합성 R1 투영/방향 부호·메타데이터 경계 검증,03 anchor에만 투영·문맥없음 저장 허용. Room v3 nullable route3컬럼+receipts 복합PK/FK/인덱스, 명시2→3 migration. 기존1/2 schema 수정 없음. PASS: JVM51/51(합성2추가), 계측 APK 컴파일. migration1/2→3 보존 검사는 작성·컴파일만 하며 실제 Room 실행 NOT_TESTED.

## 묶음2

수신 자격 공통 함수/불변 수락 입력,Room트랜잭션 재조회·TTL/타인/경로·거리 재검사/중복 확인·INSERT OR IGNORE, 실패와중복구분. 단일JOIN·멱등명시ACK·확인시각, UI와분리된수락쓰기수명. PASS: JVM55/55(수신/ACK4추가), 계측APK컴파일. Room동시복합키/다른수신자/JOIN/만료/ACK/CASCADE 검사는 작성·컴파일만, 실제실행 NOT_TESTED.

## 묶음3 접근 감지

동일 LocationSource에 좁은 proximity 계약·SDK1.1.3 실제 ENTER/ID 어댑터를 추가한다. 위치 SDK를 새로 복제하지 않는다. pending/tombstone 포함16개·거리/noteId 정렬·이미안 보완·현재시점 소비 재검사·원래 소유SDK lateID제거·release후 SDK호출차단. 등록 동기 예외는 미지요청tombstone 보존, SDK onError 등록전 로컬제거 근거와 구분. 등록 전후 오류/해제예외/StorageFailure는수신세션Failed유지·위치/수락쓰기유지·명시새세션만재허용. 고유 요청 객체와 Main 처리 직전 소유권 검사,04 자신의context 검사는 유지.

PASS: 전체JVM68(기존49+합성2+수신/ACK6+접근10+수명1), Debug/Release/계측APK컴파일/lintDebug/하네스--build, APK H3 arm64/armv7 ELF경로·아키텍처 및 v2필드보존/v3nullable경로·receipt복합PK/FK/인덱스 정적확인. 정적 스키마 스크립트 최초 nullable필드의 생략된notNull키를 직접접근해 KeyError가 났고, export형식에맞춰기본false로확인후PASS. 제품/빌드실패와구분한다. 마지막동기예외tombstone수정은후속허용회귀재확인예정.

기존 사용자 polling은 작업트리에 그대로 두고, HEAD 원문에 이번SDK메서드/임포트만 적용한 별도staged내용과 비교해 이번변경만커밋한다. Gradle검사는 기존polling이있는작업트리를컴파일했으며 polling/앱은실행하지않았다. 실제SDK/Room/서비스/화면실행은NOT_TESTED.

동기 예외 tombstone 수정 후 JVM68/빌드/계측컴파일/lint 재PASS. staged SDK 26줄만 포함하며 남은 unstaged 추가/삭제줄이 작업 시작 polling diff와 정확히 같음(assert PASS).

## 묶음4 서비스와 최소 확인 UI

Binder→LocationConnection→ViewModel→Compose로 수신상태·맨앞항목과 확인이벤트만 연결. MainActivity가 클릭 시 RESUMED를 읽고 같은 Main 호출체인에서 현재수신자/맨앞키/연타를 재검사한다. composition/재구성/화면복귀 자동ACK 없음. 확인커밋 전 다음항목 전환금지·실패후명시클릭만. 수신한기록이라는 문구로 만료된과거이력을 새위험처럼 표시하지 않음. 화면규정은 pleos-design 및 기존 Basic button/typography/layout/color 원문을 검토했고 기존Material/56dp버튼/간격을 재사용했다. 물리치수/대비/실제조작/스크린샷은 NOT_TESTED(DES-06), 테마만으로준수판정하지않음.

서비스는 위치수집·수락된제보/수신/ACK쓰기 수명을구분한다. 마지막수신/ACK쓰기전종료금지의순수판단회귀추가. 추적중지상태의bound-only서비스에서 명시ACK를수락할때 started/foreground로유지하고,저장만남은알림·마지막쓰기종료처리를연결한다. 실제Android foreground/bind수명은NOT_TESTED. 실앱setup은05계약대로A/R1/+1고정, B수신은Fake검증이며06역할UI/GPS를추가하지않았다.

추가PASS: 실제03 Repository·04 NearbyCandidates.currentCandidates·05수신/ACK를연결한메모리/Fake 흐름1개. anchor저장→400m밖미등록→400m내등록→150m안보완수신→중복차단→명시확인→원본유지 검증. 총JVM69/69 실패/오류/skip0. Debug/Release/계측APK컴파일/lintDebug 재PASS. 실제SDK/Room테스트PASS로확대하지않음. 증거 /private/tmp/step05-final-checks.log, step05-harness-build.log 및 Gradle XML. 계측테스트는컴파일만했다.

P01~P05/P08/P09는Fake/순수회귀범위PASS, P06/P07 Room이관·JOIN/동시성·cascade는계측작성/컴파일·schema정적만PASS이고실제실행NOT_TESTED. P10확인계약은Repository/Fake만PASS이며실제Compose노출/조작NOT_TESTED. P11빌드/패키징정적PASS,SDK/JNI/기기E2E NOT_TESTED. 스펙은사용자전달의독립재검토PASS로상태만갱신하며부모정책을새로바꾸지않음. 독립코드리뷰대기·06선행없음.

## 05 코드 리뷰 P2 두 건 후속

기준a58efa2. pr4-05-code-review.md/response.md와 원격4545085650/5659·리뷰이답글4545098096/8464 대조. 필수수용: 동일서비스 수집location/저장전용dataSync·Manifest/API일치·수락전승격실패DB0·수락쓰기유지, recipient관찰소유토큰으로B→C→B옛ACK 직접UI결과격리·원래키DB쓰기/단일잠금/writes/onFinished유지. 적용ENV-02/VEH-03/PRIV-01·기존초기화해제/명시ACK계약. 코드/테스트/해당작업기록만 수정하며공통정책/06/SDK/polling/타역할파일은변경하지않음.

수정 전 독립 AckOwnerProbeTest 동일소스를 직접 실행해1FAIL 확인: old B failure→new B inbox.error=true. 증거 /private/tmp/step05-p2-owner-before.xml/log. FGS 문제는 공식Android14 runtime/dataSync/location 원문과현Manifest/호출에근거하며기기재현이아님. 현재 두호출의 동일location 선택을좁은순수함수로행동보존추출한뒤 STOPPED 기대값 회귀를 red→green으로확인한다.

P2 수정결과: 순수 타입선택(API28=0/API29+=수집location·저장dataSync)을 두 실제승격호출에연결하고 Manifest에location|dataSync/FOREGROUND_SERVICE_DATA_SYNC를함께선언. 추적중지후수락쓰기만남아도저장타입으로전환시도하며실패가이미수락한쓰기를취소하지않음. 새ACK 수락전foreground→started중예외는DB0/오류/PENDING,기존수락쓰기가있으면started실패정리로기존foreground를임의제거하지않음. 별도서비스/WorkManager/위치권한재요청없음. 공식Android14 dataSync/local file processing·location/runtime 원문 https://developer.android.google.cn/about/versions/14/changes/fgs-types-required?hl=en 을2026-11-12 재확인했다. 플랫폼실행PASS가아니라타입선택/호출/Manifest 정적근거다.

ACK는 recipient 관찰 시작마다 소유토큰1개를교체하고수락시캡처한다. catch/finally의직접UI오류/confirming반영만사용자ID+토큰으로제한한다. 원래noteId/recipientId 쓰기·단일ACK잠금·writes감소·onFinished는관찰교체와무관하게끝낸다. 옛성공커밋은새B의정상JOINFlow에보이도록유지. 범용세션매니저/쓰기취소없음.

검사: 수정전 ACK독립반례1FAIL 및 기존타입선택을행동보존추출한타입회귀1FAIL(expecteddataSync1/actuallocation8)→수정후원반례동일소스+타입회귀모두PASS. /private/tmp/step05-p2-owner-before.xml·type-before.xml·probes-after.log. 최종기존69+추가4=JVM73/73,실패/오류/skip0. B→C→B옛성공/실패UI격리·원래키/잠금/카운트/후처리보존·새B자신실패,STOPPED/위치권한없음Fake에서ACK저장도달·승격/started예외DB0,수락쓰기후종료판단회귀PASS.

PASS: assembleDebug/assembleRelease/lintDebug·하네스--build·diff정적,Debug/Release mergedManifest와원본의타입/권한일치,기존pollingdiff일치. /private/tmp/step05-p2-final.log·harness.log. 현재영향의순수JVM/빌드/정적만실행했고이번계측작성/실행없음. 실제AndroidFGS유형전환/권한회수/마지막unbind·Room/SDK/UI/기기는NOT_TESTED. 기존사용자polling이있는작업트리를컴파일했으며해당코드실행/커밋없음. 다음은고정SHA로PR두스레드반영답글·독립재검토대기,resolve/06진행없음.

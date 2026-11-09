# 시작 위치 초기화와 쪽지 도식 수정

요청: debug패널에서A/B시작위치초기화, 파란현재위치·빨간쪽지표시수정. pleos-connect/vehicle/design 및 ponytail 적용. MVVM이벤트→controller→debug전용GPS주입, UI가SDK/Room을직접조작하지않는다.

원인: 현재Canvas는현재위치중심의상대축이라파란점이고정돼보이며,빨간점은H3주변조회에종속돼조회범위밖에서사라진다. 전체시연쪽지도식과proximity후보조회책임을분리한다.

범위: debug모의GPS와웹콘솔속도연결,권한설정안내,위치초기화이벤트,Room시연쪽지조회,축/레이블/겹침해결,단위·기기검증. 위치초기화로쪽지나수신이력을삭제하지않는다. release모의GPS제외. 서버/새SDK/지도SDK추가없음.

선택규칙 VEH-01/03/04,PRIV-01,DES-04/06. DOC-402de45b9f3d#권한:실제Pleos위치권한유지; DOC-1ba06876919b#수명:이전listener/등록/모의provider정리; DOC-9844e9eaad3c#콜백:실제SDK결과를표시; DOC-e12e49cc3efc#레이아웃:앱window내도식/패널; DOC-72a5d7ab4ebb#최소수집:합성좌표만처리.

공식샘플commit c955c3a811371892e8803f3b1d9ad3f0426e832c의location/FusedLocationClient.kt에서LocationManager.addTestProvider/setTestProviderLocation/removeTestProvider,marker파일정리방법을gh로확인. mock app-op은에뮬레이터의우리앱에만설정한다. 웹콘솔은이전검증에서확인한동일에뮬레이터localhost8088 /vehicle/state의controls.speed_kph를읽는다. 실제차량API보장으로해석하지않는다.

정책: A기준0m, B최신쪽지600m뒤(쪽지없으면기준점600m뒤). 활성시연에서만버튼사용,버퍼·이전등록정리후새SDK세션에서확인. mock모드해제버튼으로provider제거후ADB사용. 결과는아래PASS/NOT_TESTED로기록예정.

## 결과

PASS: StartPositionTest 실제Pleos2.1.0/user10 계측1개(37.679초). A0m 실제SDK콜백,30초buffer후Room저장,B최신note600m뒤SDK콜백,같은에뮬레이터웹콘솔D36km/h에따른SDK좌표전진,외부GPS전환을assert. 모의위치app-op은이debug앱에만allow설정했다. 계측후가상P/0복원. 위치초기화는Room쪽지/수신이력삭제안함.

PASS: RoutePlotTest2개(고정축현재위치이동/쪽지좌표유지,600m뒤및먼쪽지범위포함),전체단위8개. debug/release빌드및lint검증. 최초lint WrongConstant·domain.includeSubdomains 오류는공식상수/명시설정으로수정후PASS. Studio빌드는최종재확인한다.

PASS: 실제UI버튼B초기화→파랑-600m/빨강0m표시. SDK전달float오차를floor로표시해-599m로보이던것은roundToInt로수정. 도식위쪽빨강/아래쪽파랑으로동일좌표겹침회피. 웹콘솔주행후파란점은약-549m로이동하고빨간점은0m유지하는장면관측. 전체시연쪽지Room Flow와H3후보Flow를분리해B600m뒤(주변후보0개)에서도빨간쪽지표시. B에는30초제보준비대신접근수신대기문구표시.

NOT_TESTED: 전체폼팩터/주야간/실차인증,다른Pleos버전,프로세스강제종료복구오류주입. 지원범위는debug Pleos에뮬레이터이며release에는mockprovider·localhostHTTP구현이없다. 에뮬레이터GPS/앱mock동시입력은지원하지않고외부GPS전환필요. 모의권한이없으면시작위치실패를표시하며fakeUI위치로대체하지않는다.

최종PASS: debug/release assemble,lint,전체단위8개,Studio build_project. HOME으로가시성중단/provider정리후최종debug APK설치·실행완료. 앱mock app-op allow는시연버튼사용을위해유지했다. 실제마이크/Gleo승인/실차검증은이번범위밖. 작업보고서exit2는전체폼팩터NOT_TESTED를의미한다.

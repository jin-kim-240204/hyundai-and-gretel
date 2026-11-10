# 웹콘솔 주행 시 위치 정지와 파란 점 문제 수정

요청: 웹콘솔 주행에도 현재위치/쪽지거리갱신안됨·파란점사라짐 조사. 30초대기제한제거도진행중인동일작업으로유지.

실행상태증거: 현재UI는웹콘솔연동GPS행없고외부SDK좌표-100m고정. adb appops get은 MOCK_LOCATION deny·최근reject. 모의provider정리시SecurityException로그. 앱시작이raw SDK만시작하고role전환이mockjob을중단한뒤복원하지않는문제도코드확인.

수정: 사용자시연시작은선택GPS모드에따라시작,debug기본웹콘솔모드. 외부GPS선택은명시적으로기억·안내. 연동모드A/B전환은시작위치초기화로연결해mockjob재가동·B쪽지600m뒤출발. mock app-op사전확인·실패이유별도상태로유지해일반위치콜백이오류메시지를덮지않음. release는외부SDK모드.

규칙 VEH-01/03,PRIV-01,DES-04. DOC-402de45b9f3d#권한: SDK권한과mock app-op구분; DOC-1ba06876919b#수명: 모드/역할전환listener/provider정리·재시작; DOC-9844e9eaad3c#콜백: 화면은실제SDK위치만표시. 새라이브러리/서버/Gleo추가없음. 실제emulator의우리앱app-op만재설정하고설치후확인.

검증예정: StartPositionTest에서UI시연시작과동일startDemo→첫SDK위치직후즉시저장→switchActor(B)자동600m뒤→속도SDK이동→외부모드복귀. 단위·debug/release·lint·Studio빌드·실제UI거리/마커확인. 사용자데이터는설치로삭제하지않음.

검증 PASS: ConsoleGpsDeniedTest(0.062초): deny에서running/위치/심기가능false,명시적권한오류확인. 권한allow복구후StartPositionTest+SessionGuardTest(8.481초) PASS: startDemo자동연동,첫위치즉시저장,사용자B전환만으로600m뒤초기화,실제SDK좌표전진,외부GPS복귀. 최초속도assert36고정은웹콘솔자연감속으로31이관측돼실패했으며,초기setpoint대신실제콘솔속도와1초수집주기에맞춰비교하도록테스트수정후PASS. 허용치만바꿔고정초기값을통과시킨것이아님.

실제UI검증 PASS: 동의→시연시작→디버그제보→‘사용자 B’만클릭해서현재-600m/쪽지0m/600m차이와파란마커확인. 웹콘솔D주행에서파란마커/거리감소를확인했고사용자도정상동작확인. 가상차량P/0복구요청실행. 사용자요청으로추가실행검증은종료했다. 에뮬레이터의우리앱mock app-opallow설정유지. 권한재차단시안내하며외부고정GPS로조용히대체하지않는다.

자동검증 PASS: 단위9개,debug/release assemble,lintDebug,verify_harness --build,Studio build_project. NOT_TESTED: 전체폼팩터/실차·다른Pleos버전. Gleo코드·스키마추가없음. PR에는기존SDK/Room/접근검증과이번추가검증을구분해기록한다.

PR 준비 확인: 최신작업트리에서debug/release assemble·lintDebug·단위9개재실행PASS(8초). 코드수정없이직전기기계측결과를재사용했다. before-driving.png는실제B600m출발UI증거로포함한다. 종료요청후캡처된after-driving.png는주행완료증거로부적절해제외했다. 새기기실행은하지않았다.

# 03-c debug 제보 연결 구현

승인: 조정자 전달로 스펙4a0f332 독립재검토 통과(blocker0/미결정책0, PR리뷰5864295551). 이번 범위는03-c 구현·허용검사·커밋/푸시·PR기록 후 대기.04진행없음. 운영문서 준수, 기기/Studio/adb/앱/계측 실행 금지. 기존미커밋 polling·타역할기록 보존.

적용 ENV-02/VEH-03/PRIV-01/DES-01/DES-02, 기존 스펙 원문요구 DOC-9c1fe6a8475c#즉시성/대비와문구, DOC-d771e574367a#버튼상태, DOC-e12e49cc3efc#앱윈도우, DOC-9844e9eaad3c#콜백. 안드로이드아키텍처·clean-code-review·ponytail: 좁은 서비스연결 계약과 production Flow전환만 JVM검증, 원본저장/버퍼·SDK수명 유지. debug source set 슬롯/문자열/fixture와 빈release슬롯. 새 라이브러리/스키마/전역scope 없음.

검증계획: 다른A/B쌍·예약A갱신·연결없음·현재클릭대상·저장중시작차단·완료자동시작없음 JVM, debug문구매핑/JVM·기존Repository/버퍼회귀, FakeLocationSource오류→production종료판단·수락저장보존. Debug/Release/계측APK컴파일/lint/harness. 실제UI/Binder/Room/SDK/알림/재생성/주행대비/물리측정은NOT_TESTED. 구현중이며 독립코드리뷰전 전체AC완료로 보고하지 않음.

## 구현 결과와 코드 리뷰 지점

- LocationSession은서비스의제한된상태/제출계약이며 Android타입없는Fake로교체한다. LocationConnection은같은세션의status/submission을combine하고flatMapLatest/buffer(0)로교체한다. 연결해제는기본연결없음상태. 클릭은표시캐시가아닌현재세션의최신상태로재검사한다. 연결관찰은제출을호출하지않는다.
- LocationViewModel은기존Androidbind/lifetime을유지하고Connection의uiState와현재제출경계를사용한다. 기존status구독은기존계측호환을위한파생Flow다. 저장중start는canStart로차단하고저장완료후자동start코드없음. Main/순수화면은상태/콜백만전달한다.
- src/debug 슬롯은고정content/WARNING·문구매핑·저장중버튼Loading. src/release는빈동명슬롯. 기존SDKsource와Room스키마/수락저장정책불변. 진단추가없음.
- shouldStopTrackingService는실제서비스의저장완료/정리경로와SDK오류Fake테스트가함께사용하는작은종료판단. 테스트자체의별도종료규칙없음. 긴TrackingLifecycleTest의수동해제/재연결/200ms관찰부분은JVM연결검증으로옮기고실제SDK등록·화면재생성·알림/중지조건은유지한다. 계측은compile만했다.

## 검증

PASS: JVM35개/실패0(기존30+연결/문구4+SDK오류종료1). 다른A/B상태, 예약A갱신, 해제상태유지, 이전A/B혼합거절, B중지클릭0·B활성클릭A0/B1·연타차단, 결과재관찰/새서비스Idle·관찰중자동제출없음, 7추적×제출상태행렬·시작차단, 모든실패문구매핑. FakeSDK오류후수집해제1회·새저장0·수락저장성공/실패뒤종료판단을같은production함수로확인. 실제Android수명증거가아님.

PASS: assembleDebug/assembleRelease/assembleDebugAndroidTest/lintDebug(최종빌드로그 /private/tmp/step03c-build-final.log), verify_harness.py --build, git diff --check. APK zip정적대조: debug에는fixture/resource/debug문구매핑클래스존재, release에는모두부재(resources.arsc의UTF8/UTF16및dex). XML/슬롯문자열도source-set분리. 새기기의실행/설치명령은전혀사용하지않았다.

NOT_TESTED: 앱·UI·클릭·Binder·재생성·Room실제동작·SDK실오류·서비스알림/마지막unbind·기기JNI/주행·다중창·대비·물리글자높이/터치·0.25초실측. 사용자금지로Studio(build_project포함)/adb/emulator/simulator/계측/컴퓨터UI를실행하지않음. 과거계측PASS재사용없음. report의DES-01/02 NOT_TESTED로exit2를보존한다.

이번종료지점은03-c구현·허용검사후독립코드리뷰대기다. 전체AC완료/실기기완료주장없음. 기존보류스레드4541836274/4541836293의production JVM경계는추가했지만독립리뷰전resolve하지않는다. 코드리뷰후조정자가처리한다.04스펙미착수. 이번범위파일만명시stage·한국어feat커밋/푸시후AI구현자/SHA/검증제한을PR4에기록한다.

후속정적수정: 최초명시stage후새파일LocationSession.kt 끝빈줄이 diff --check에검출됐다. 로직변경없이EOF공백만제거하고추가커밋으로정리했다(amend/force push없음). 전체변경diff검사를다시실행했다. 이공백변경은빌드/테스트재실행대상이아니다.

## 독립 코드 리뷰 채택 테스트 보강

대상3e4667b·리뷰5864409926/4542436999 및리뷰이답변기록을읽고선택개선1건수용. production변경없이기존Session Fake/LocationConnection으로직접A→B와A→null→B를중간runCurrent없이확인하는테스트추가. 표시캐시가A인상태에서B중지클릭A0/B0, 예약A갱신처리후B상태/이후A무시, B활성클릭A0/B1을검증한다. 빠른null의중간방출은필수로단언하지않는다. 스레드반영SHA답글후resolve없이재검토대기, 실제SDK수명4541836293은NOT_TESTED유지. 기존변경과다른역할기록보존. 앱/Studio/adb/기기/계측실행없음.

후속검증 PASS: testDebugUnitTest 전체36개/실패0, 추가1테스트안의직접/빠른null교체두경로통과, diff검사통과. production코드변경없어APK/lint추가재실행N/A. report의UI NOT_TESTED는계속보존한다. 이번테스트/기록/보고서3개만커밋한다.

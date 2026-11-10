# 02 버퍼 리뷰 반영 및 구현

요청: 리뷰 답글3건 반영 후 구현, ponytail·기존 ba7e02a 상태 리팩터링 유지. 기존 로컬 PleosLocationSource 임시 POLL_1S와 pr4-implementation-feedback.md는 사용자 변경으로 보존한다. SDK 콜백 계약을 변경할 때 임시 polling과 정리 코드를 지우지 않는다. 코드 커밋·푸시는 별도 리뷰 후.

공통 정책은 spec-road-note-system e4c2222로 먼저 커밋·푸시 후 기능 브랜치에 merge. 기능 스펙02는 답글 링크/시각/공백/무효화/테스트 갱신. ENV-02/VEH-03/PRIV-01 및 DOC-9844e9eaad3c#콜백/DOC-ba9b83fc86f6#오류: 기존 nullable/error·수신단조시각 요구 적용. SDK 필수순서/해제와 좁은 Binder/파생 ViewModel 상태 유지. 신규 UI·DB·서버·DI 없음.

설계: 순수 LocationBuffer 1개, 기존 추적에서 소유. 마지막 수락 이벤트시각1개가 표본 정렬과 무효화 경계 역할을 함께 수행(동시각 null 우선, 유효 동시각 거절). 목록 길이·준비 여부·offset은 저장하지 않고 계산. 콜백은 null 수신시각을 별도 인자로 전달하며 어댑터는 단조시계를 한번만 읽음. 유효fix의 기존 수신시각과 인자는 같은 값이어야 함. Main 처리에서 조회 시각을 읽고 즉시 불변 선택값 반환.

검증: 버퍼 경계/이벤트 역전/공백복구 단위 및 기존 회귀, debug/release·lint·harness·IDE, 실제45초 이상 수집·화면밖·재생성. 구현/단위/실제SDK 증거를 구분하며 미검증은 명시.

## 결과와 근거 (2026-11-11)

PASS: 버퍼 단위10개+추적 단위7개 총17개.45초 경계, 첫표본/짧은이력, 동률, 신선도5초/공백6초후과거선택, 목표오차5초와초과, null동시각우선/역전/복구, 미래·음수·잘못된좌표, 만료후순서기준보존, 정차이력/불변결과, Main조회진입시각, 중지/실패초기화/이전세대콜백. Android 없는 시계 인자로 검증.

PASS: debug/release assemble, assembleDebugAndroidTest, lintDebug, harness --build, 실제 원래 프로젝트 경로의 Studio build_project. UI/권한/Gradle/Manifest 변경 없음. 기존 Kotlin/SDK외 새의존성 없음.

PASS: Pleos2.1.0/API34/user10에 현재 로컬 상태(임시 캐시폴링 포함)를 빌드·설치. 기존1단계 계측3개1.929초 통과(리소스 정리·재연결·재생성). 기존필수SDK 순서와 narrow Binder 상태만 유지.

PASS: 기존 Activity 수명 테스트에 `-e bufferWindow true`로 실행하는50초 보관창 검증 추가, 실제 SDK 좌표를1초 간격으로 host에서 주입하며53.616초 통과. 첫 BUFFER_APPEND oldest222127/latest222127 → 마지막 oldest229150/latest273294,45개/44,144ms span. 화면 CREATED20초 및 재생성 뒤에도 같은 세션/SDK등록1회, oldest가 앞으로 이동하고 이력이30초 이상 남음. BUFFER_APPEND는 실제 연속콜백 경로에만 있으며 임시 POLL_1S 캐시로그는 버퍼 입력 아님. 근거 evidence/step02-location/window-summary.txt. 이 로그로 모든 선택조건/실제GPS측정정확도까지 입증했다고 하지 않음.

장시간 검증 명령: `adb shell am instrument -w -e class com.jinkim.hyundaiAndGretel.location.TrackingLifecycleTest#activityStopAndRecreationKeepTheSameTrackingSession -e bufferWindow true com.jinnayeong.wakeup.test/androidx.test.runner.AndroidJUnitRunner`. 동시에 호스트에서 합성 GPS경로 주입 필요. 기본 기기회귀는 기존처럼 빠르게 실행. 테스트를 위해 Binder를 넓히지 않음.

문서: 리뷰 답글3개 링크·수정 계약을02스펙에 반영. 공통정책은 e4c2222/05423be로 부모에 푸시 후 기능 브랜치에 merge(충돌없음). 기능 구현·스펙 변경은 아직 미커밋, 기존 다른 작업도 포함하지 않음.03단계 제보/DB 구현 없음.

NOT_TESTED: 실제 SDK privacy차단/null/error/서비스강제끊김 주입, 실차·폼팩터전체. null/error/순서역전은 가짜 소스 경계로 확인. 기존1단계의 권한팝업대기중재생성 미검증은 이번으로 대체하지 않음. 기능 규칙 구현·선택단위 PASS와 실제 장애 전체 검증을 구분.

마무리: 기능 diff의 docs/plans는 부모와 동일함 확인. 최종 단위17개·diff 재확인 PASS, 보고서 exit2는 실제 장애 미검증을 보존한다. 수명 계측 종료 후 수집 중지, 이번 실행의 에뮬레이터 종료. 기존 임시polling과 사용자 작업 기록은 유지했다.

커밋 요청: 사용자 승인으로02스펙 반영·구현·검증을 feat:- 커밋1개로 묶어 푸시. 임시 폴링을 /tmp/step02-source-with-local-polling.kt에 백업하고 HEAD 어댑터에 이번 콜백 계약2줄만 적용한 정확한 커밋 상태에서 단위17/debug/release/계측컴파일/lint 재실행PASS(2초). 해당2줄만 stage 후 임시 폴링 원본을 로컬 작업 파일에 복원했다. pr4-implementation-feedback.md의 기존 수정도 미포함. 실제 SDK 장시간 증거는 앞서 명시한 로컬 폴링 포함 상태이며 캐시 경로는 버퍼 입력에 사용하지 않았다.

2026-11-11 중간리뷰 정정: 앞선53.616초 테스트의 최종 보관창과 등록유지 assertion만으로 CREATED 구간내 수집까지 입증할 수는 없었다. 과거 PASS는 그 범위로 제한한다. location-midpoint-refactor.md에서 구간 진입의 단조시각보다 새로운 실제 콜백을 복귀 전에 확인하는 검사를 추가했고, 보강된54.191초 실행 결과를 별도로 기록했다.

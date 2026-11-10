# 01단계 상태 소유와 파생값 리팩터링

요청: 상태를 여러 곳에서 변경하거나 중복 저장하는 값, 함께 바뀌는 플래그를 실제 호출/조합으로 검토하고 동작 보존하는 작은 refactor 커밋1개와 푸시. 현재 폴더/브랜치 유지. 선행 로컬 임시 polling 및 pr4-implementation-feedback.md 수정은 커밋 제외. 기준0aede85(02단계 문서는 수정 안 함).

적용 스킬: pleos-connect, ponytail, commit, pleos-verify. android-compose-architecture 파일 없음. ENV-02 기존 Activity/Compose 구조 유지, VEH-01 및 DOC-402de45b9f3d#권한: 화면 수명과 권한 승인 후 시작 정책 보존. VEH-03, DOC-1ba06876919b#수명: 지원확인→initialize→등록, 동일리스너 해제→finally release, 새 인스턴스 재시작 유지. DOC-9844e9eaad3c#콜백: 이전 세션 콜백 차단과 nullable/error 유지. PRIV-01 데이터/로그 범위 불변. UI 레이아웃/차량 제어 변경 없음.

원문: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release (수명), 기존 declaring-permissions/location-data-access 스냅샷 및 스펙§4/5. Kotlin 공식 flatMapLatest/stateIn 문서 재확인(최신 문서는1.11이므로 설치 coroutines1.9 컴파일/회귀로 대조). flatMapLatest는 새 upstream으로 바뀔 때 이전 구독 취소, stateIn은 ViewModel scope에서 상태 공유. 새 의존성 없음.

## 실제 상태 조합과 판단

- MainActivity pendingStart/resultReady: (false,false) 대기없음, (true,false) 권한응답 대기, (true,true) 응답받았으나 RESUMED 대기. (false,true)는 경로상 의도하지 않은 조합. requestStart→권한결과→complete/ON_RESUME, onStop·중지·재생성에서 대기 의도 폐기. 두 Boolean을 단일 enum으로 치환, 시작·취소 정책 불변.
- ViewModel mutableStatus: 서비스 flow를 collect해 복사하고 disconnect에서 STOPPED를 직접 기록. observation Job도 연결/해제/onCleared에서 수동 관리. 연결된 서비스의 StateFlow만 저장하고 status는 flatMapLatest/stateIn으로 파생. disconnected=null→STOPPED, 연결 변경 시 이전 구독 취소, Eagerly로 기존 상시 관찰 유지. bind 결과 bound는 비동기 연결 성공 여부와 다른 의미이므로 유지(등록했으면 unbind 필요).
- startCommandFailed: 서비스에 도달하지 못한 명령 실패이므로 서비스 status에서 계산 불가. 별도 값 유지, 재시도에서만 초기화하는 기존 정책 보존.
- LocationTracking source/status/generation: source는 실제 해제 대상, status는 미지원/실패/null 등을 구분하므로 서로 계산 불가. generation은 중지/재시작의 늦은 콜백 식별에 필요. source 변경은 항상 generation이 바뀌는 start/stop에만 있으므로 generation 검사와 source 동일성 중복 검사를 제거할 수 있음. 같은 소스 인스턴스 재사용 시에도 세대 검사가 필요함을 회귀로 확인.
- PleosLocationSource initialized/listener: initialize가 예외를 던지면 initialized=true/listener=null 가능. 이를 listener!=null로 대체하면 release 누락. 해제 먼저 플래그 무효화→finally release 구조 유지. 임시 polling도 이번 범위 밖.

검증 계획: 세션 단위 및 같은 소스 재사용 회귀, 실제 화면 재연결·disconnect/reconnect·명령 실패·어댑터 해제 계측, debug/release·lint·harness·IDE. 실제 권한 팝업 대기 중 재생성은 기존 NOT_TESTED 유지. 최종 결과는 아래 추가.

## 구현 후 검토

명명 인자(scope/createSource/log/onFailure, onLocation/onError)와 콜백 들여쓰기를 정리. 화면 레이아웃은 그대로 유지. 권한 요청 Boolean2개는 Activity 내부 enum1개로 치환했으며 이벤트 순서/권한 검사/RESUMED 조건/취소 시점은 동일하다. 서비스 상태는 연결된 StateFlow에서 파생하고 수동 복사 상태 및 observation Job을 제거했다. startCommandFailed/permissionMessage/bound/initialized는 각기 다른 과거 결과·해제 의무를 나타내므로 유지했다.

Flow 선택의 비용: flatMapLatest/stateIn과 ExperimentalCoroutinesApi opt-in을 읽어야 함. 효과: 상태 복사와 연결/해제/onCleared의 Job 취소를 없애고 ‘연결됐으면 해당 서비스 상태, 아니면 STOPPED’를 한곳에 표현. Eagerly로 기존 화면 비가시 중 관찰을 보존한다. 연결 콜백 직후 같은 호출 스택에서 status.value를 읽는 동기 계약은 두지 않는다. 운영 사용처는 Compose 구독이며 동기 결과 의존 없음(rg 확인). 계측은 상태 전파를 기다려 연결·해제·재연결을 확인한다.

유지한 필수 순서: 서비스 startForeground→tracking.start, 세션 무효화→source.stop, SDK unregister의 finally release, 서비스 foreground 해제, ViewModel 최종 unbind. SDK 어댑터는 이번 커밋에서 수정하지 않는다. 상태 통합용 reducer/이벤트 버스/새 의존성은 추가하지 않았다.

## 이번 실행 증거 (2026-11-11)

PASS — LocationTrackingTest6개, failures0/errors0. 기존5개 및 generationRejectsOldCallbacksEvenWhenSourceInstanceIsReused 추가: 같은 소스 인스턴스를 재시작해도 보관한 이전 위치/오류 콜백은 무시하고 현재 콜백만 반영.

PASS — Pleos Connect2.1.0/API34/user10 기기 계측3개,9.047초. 재생성 상태 재연결/초기 등록1회, 화면 연결 해제 콜백을 직접 전달한 뒤 실제 서비스를 중지·재시작해도 화면은 STOPPED 유지, 재연결 후 현재 WAITING/RECEIVING 관찰. 기존 시작 명령 실패 보존 및 SDK unregister 실패 후 release/unbind 회귀도 통과. 실제 OS 서비스 사망을 주입한 테스트는 아님.

PASS — debug/release assemble, 계측 APK 컴파일, lintDebug, harness --build, git diff --check, 현재 원래 프로젝트 경로에서 Studio build_project. SDK 어댑터를 HEAD 원본으로 잠시 복원해 임시 폴링을 제외한 실제 커밋 내용을 빌드·실행했다. 폴링 원본은 /tmp/step01-poll-before-refactor.kt에 백업 후 커밋 뒤 로컬로만 복원한다.

NOT_TESTED — 권한 팝업을 기다리던 중 실제 Activity 재생성/늦은 허용 조합, 실제 SDK privacy·원격 연결 장애, 전체 폼팩터/실차. 기존 미검증은 유지하며 이번 refactor 전체가 모든 AC를 입증했다고 하지 않는다. 권한 세 상태 전이는 코드의 동등 조합/분기로 대조했고 실제 팝업 계측을 새로 PASS 처리하지 않았다. 화면 레이아웃/리소스 변경은 N/A.

커밋은 이번 source6개 및 이 기록/보고서만 포함한다. 선행 로컬 pr4-implementation-feedback.md·임시 폴링은 포함하지 않는다. 기존02단계 스펙 커밋0aede85를 보존하고 PR 브랜치에 일반 푸시한다.

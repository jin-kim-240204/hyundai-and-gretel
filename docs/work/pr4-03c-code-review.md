# PR4 03-c 독립 코드 리뷰

AI 독립코드리뷰(Codex) · 03-c · SHA `3e4667b6b27363848801eb866fffd2e096bf3e6f` (base `4a0f332`, 기능 `847b6ac`). pleos-connect/clean-code-review/android-compose-architecture/pleos-design/ponytail-review를 적용했습니다.

**판정: 확인 범위에서 blocker/확정 실행 결함 없음. 선택 테스트 개선1건은 인라인에 남겼습니다.** 스펙 채택2건의 production 연결을 확인했습니다. canStart는 Submitting을 제외하고, 제출은 화면 캐시 대신 현재 session 상태를 다시 확인합니다. 단일 연결 내부 combine을 flatMapLatest로 전환하며 같은 서비스 결과를 재관찰합니다. 기존 Repository의 연타 gate·불변 anchor/UTC·취소 전파와 서비스 소유 저장 수명을 재사용합니다. LocationSession은 실제 Binder와 JVM Fake 교체에 쓰이고, shouldStopTrackingService도 실제 종료 경로와 Fake 테스트가 공유하므로 의미 없는 추상화로 보지 않았습니다.

직접 확인: JVM LocationConnectionTest4개+SubmissionSdkFailureTest1개 **5 PASS/0 실패/0 오류**, diff --check PASS. 동일연결 결과/새서비스 Idle, 상태 행렬/시작 차단, 이전 구독 차단·현재 클릭 대상, Fake SDK 오류 후 수집 해제 및 수락 저장 성공/실패 후 종료 판단을 검사합니다. source set의 debug fixture/문구·UI와 빈 release 슬롯을 정적 대조했습니다. 기존 로컬 APK에서 fixture/resource/message class가 debug에는 있고 release에는 없는 것도 관찰했으나, 이번에 release를 새로 빌드한 근거로 사용하지 않습니다. 전체 JVM35개 및 debug/release/androidTest/lint/harness PASS는 작성자 기록이며 이번 직접 재실행과 구분합니다.

AC 연결: C01 debug/release source set, C02 현재session 제출, C03 debugMessage+기존 Repository commit 후 Saved, C04 재연결 관찰·기존 gate, C05 production Connection tests, C06 production 종료판단+Fake SDK, C07 기존 버퍼/Repository 계약 재사용, C08 DB/스키마 변경 없음, C09 속도·기어 gate 없음/기존 Theme, C10 canStart와 행렬 테스트. 이 매핑은 **전체 AC 실행 완료 판정이 아닙니다.**

NOT_TESTED: 실제 UI 클릭/Compose 재구성·Activity 재생성·Binder·SDK·Room·프로세스 재실행·foreground 알림/마지막 unbind·주행/대비/물리 크기/실제 Loading 응답. 앱/Studio/adb/에뮬레이터/계측 실행은 하지 않았습니다. 미커밋 PleosLocationSource polling은 보존·리뷰대상 제외. 앱 코드 수정 없음. 리뷰이의 후속 판단 및 다음 지시를 기다립니다.

## 인라인 선택 개선

위치: app/src/test/java/com/jinkim/hyundaiAndGretel/ui/location/LocationConnectionTest.kt:38

**선택 개선 · 해제 완료를 기다리지 않는 A→B 교체도 확인하면 좋겠습니다**

현재 테스트는 A 갱신 예약→connect(null)→runCurrent()로 A 구독 정리를 마친 뒤 B를 연결합니다(33~38행). 따라서 해제 후 이전 값 차단은 입증하지만, ‘A→null→B가 scheduler 실행 전에 연속 도착’하거나 null 없이 A→B로 바뀌는 경우의 전환은 별도로 관찰하지 않습니다. 연결 입력은 StateFlow라 빠른 null 중간값이 생략될 수 있습니다([공식 설명](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)). 이는 현재 production 코드가 잘못 동작한다고 확인한 지적은 아닙니다.

기존 테스트에 작은 별도 케이스 하나로 A의 갱신을 예약한 뒤 runCurrent 없이 B(또는 null→B)를 연결하고, B만 관찰·클릭 대상이 되는지 확인하는 것을 추천합니다. 표시 캐시가 A여도 현재 B가 STOPPED면 클릭 0회, B 활성 후에는 A0/B1회라는 현재 단언을 재사용하면 됩니다. 새 연결 계층이나 구현 변경은 요구하지 않습니다.

AI 독립코드리뷰(Codex) · SHA `3e4667b6b27363848801eb866fffd2e096bf3e6f` · clean-code-review 25/28/32. 직접 기존 연결 테스트4개와 SDK오류 테스트1개 PASS. 제안한 추가 순서는 NOT_TESTED이며 실제 Android 콜백/Binder 재연결도 미검증입니다. 현재 리뷰의 blocker가 아닌 테스트 보완 제안입니다.

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5864409926
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542436999

리뷰이 판단 대기. 기존 스레드 resolve는 이번 요청 범위에서 수행하지 않음. 공식 원문은 이번 세션의 Android started/bound·build variants·Kotlin StateFlow·Pleos Basic button 확인 근거를 재사용. 규칙 clean-code-review 11/15/17/23~25/28~32. 빌드/기기 검증은 위 구분 유지.


## 899fd88 마지막 재검토

AI 독립코드리뷰어(Codex) 재검토 · SHA `899fd88ce3db0ef93fe9b67868841bd80958c28b`. **판정: 코드리뷰 관점에서 03-c 구현 완료 / 실기기 검증 대기.** 마지막 선택 테스트 보강이 실제 production 경계를 검증함을 확인했습니다. 신규 blocker/확정 실행 결함은 없고 범위를 추가 확장하지 않습니다. 직접 연결 JVM5개(추가1개 안의 직접/빠른null 교체 두 경로 포함) PASS, diff검사 PASS. 전체36개 PASS는 작성자 기록과 구분합니다. 테스트/기록만 변경돼 APK/lint 추가 재실행은 하지 않았습니다.

4542436999와 원 구독분리4541836274는 요구 충족 확인 후 resolve했습니다. 실제 SDK 오류 후 Android 서비스/알림/마지막 unbind 수명4541836293은 NOT_TESTED·미해결로 유지합니다. 실제 UI/Binder/SDK/Room/재생성/주행·대비·물리크기 등 전체 AC 실행 완료가 아닙니다. 앱/Studio/adb/에뮬레이터/계측은 실행하지 않았고 앱 코드 수정도 없습니다. 후속 단계 진행은 조정자 판단에 맡기고 대기합니다.

게시 확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5864462421
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542481040
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542481327
지정 두 스레드 isResolved=true 확인, 실제 SDK 스레드 유지.

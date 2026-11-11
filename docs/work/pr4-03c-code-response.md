# PR4 03-c 코드 리뷰이 판단

게시 완료: [4542436999에 대한 리뷰이 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542451678). API 응답의 in_reply_to_id로 대상 스레드를 확인했어요.

대상 SHA `3e4667b6b27363848801eb866fffd2e096bf3e6f`, 독립 리뷰 5864409926. 운영 지시와 pleos-connect·clean-code-review(15/17/25/28/32)·기존 Compose 구조 기준을 적용했어요. 이번 범위는 코드/리뷰 대조·판단 기록·지정 스레드 답글이며 코드/스펙 수정·resolve는 하지 않아요. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지를 유지해요.

## 4542436999 — 수용: 작은 테스트 보강

현재 LocationConnection은 하나의 current StateFlow에서 flatMapLatest로 세션을 바꾸고 각 세션 안에서 상태를 combine해요. submitTestNote는 화면 캐시 대신 current.value와 세션의 최신 값을 다시 확인해요. 기존 테스트는 A 예약 갱신→null→runCurrent→B 순서여서 빠른 직접 A→B 또는 A→null→B는 별도 입력 경계예요. 지적은 타당하지만 현재 구현의 확정 실행 결함 근거는 아니에요.

권고: A가 관찰된 뒤 A 갱신을 예약하고 중간 runCurrent 없이 직접 B 또는 null→B로 교체하는 두 입력을 기존 Fake/production 경계로 확인해요. B STOPPED에서는 표시 캐시가 A여도 양쪽 제출 0회, B 활성 뒤 A0/B1회, 스케줄러 처리 후 B 반영 및 이후 A 갱신 무시를 확인해요. 빠른 null이 생략될 수 있으므로 모든 연결 대기 방출을 요구하지 않아요. 새 연결 계층·세션 플래그·코드 리팩터링은 요구하지 않아요. 작성자가 작은 테스트로 반례를 확인한 뒤 실패가 있을 때만 원인 수정하면 돼요.

## 기존 스레드의 입증 범위

- **4541836274 구독 분리:** production LocationConnection을 제어 가능한 Flow/스케줄러로 검증하는 별도 JVM 테스트가 생겼고 긴 계측의 수동 해제/재연결 관찰은 분리됐어요. 남은 계측의 delay(200)는 화면 CREATED 뒤 서비스가 계속 수집하는 확인 경로로, 구독 해제 단언과는 달라요. 원 지적의 구조/검증 경계는 코드상 반영됐다고 판단해요. 직접 빠른 교체 보강은 위 새 선택 개선으로 별도 관리해요. 실제 Android 연결 경로 PASS를 뜻하지 않으며 이번 권한상 resolve는 하지 않아요.
- **4541836293 SDK 오류 수명:** Fake가 실제 LocationTracking 오류 콜백을 호출하고 Repository 및 서비스와 공유하는 shouldStopTrackingService 판단을 검증해요. 수집 정리·수락 저장 중 종료 요청 0회·성공/실패 후 종료 요청 1회·새 저장 없음은 코드상 검증 대상이에요. 실제 Android 서비스/알림/마지막 unbind는 이 테스트에 없어요. 특히 오류 직후 신규 요청은 기존 Submitting gate도 막으므로 오류만의 신규 차단 근거로 확대하지 않고, 저장 완료 후 LOCATION_UNAVAILABLE 확인과 함께 해석해요. 실제 SDK 오류·마지막 unbind 뒤 생존/알림 증거는 NOT_TESTED로 남겨 스레드 유지가 적절해요.

## 근거와 상태

읽은 파일: 독립 리뷰 기록, LocationConnection/LocationConnectionTest, LocationSession/LocationViewModel, SubmissionSdkFailureTest, 서비스 종료 호출부, TrackingLifecycleTest 관련 구간, 작성자 작업 기록, 원격 원댓글 3개. HEAD가 대상 SHA와 일치하고 새 스레드 답글이 없는 것을 확인했어요.

- PASS: 실제 코드와 지적의 입력 순서/검증 범위 정적 대조.
- 참고: 독립 리뷰어의 연결 4개+SDK 오류 1개 PASS, 작성자의 JVM35개·빌드 PASS는 타 역할 실행 기록이며 이번 직접 실행 결과가 아니에요.
- NOT_TESTED: 이번 직접 JVM/빌드 실행, 추가 교체 순서 테스트, 실제 UI/Binder/SDK/Room/서비스 수명. 같은 검사 반복 대신 작은 추가 테스트를 작성자에게 요청해요.
- 변경: 이 판단 기록만 추가. 기존 미커밋 코드/문서 보존, 앱 코드 변경/커밋/푸시/resolve 없음.

게시 본문은 `/private/tmp/pr4-03c-code-response.json`에 정확히 기록했어요. 정책/선택 개선 판단 완료이며 전체 AC 실행 완료나 무조건적인 기능 승인으로 확대하지 않아요. 조정자는 작성자에게 작은 테스트 보강을 전달하고 결과를 재검토하면 돼요. 보고 후 대기해요.

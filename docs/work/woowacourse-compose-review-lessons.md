# 우아한테크코스 Android Compose PR에서 배운 코드 리뷰 기준

2026-11-12. 공개 PR의 인라인 리뷰, 리뷰가 가리킨 변경 코드, 확인 가능한 작성자 답글을 읽었다. 회원가입 Compose 미션은 학습 사례로, 런세권·토독토독·책책은 팀 프로젝트 사례로 다룬다. 아래 기준은 개별 리뷰에서 길쪽지에 적용할 판단을 뽑은 것이다.

## 읽을 PR

| 구분 | 저장소와 PR | 코드에서 볼 곳 |
| --- | --- | --- |
| Compose 미션 | [회원가입 #2](https://github.com/woowacourse/android-signup/pull/2/files), [#30](https://github.com/woowacourse/android-signup/pull/30/files), [#37](https://github.com/woowacourse/android-signup/pull/37/files), [#38](https://github.com/woowacourse/android-signup/pull/38/files) | 입력 컴포저블의 레이아웃 제어, 상태 소유자, 검증 메시지 반환 계약 |
| 팀 프로젝트 | [런세권 #864](https://github.com/woowacourse-teams/2025-course-pick/pull/864/files), [#886](https://github.com/woowacourse-teams/2025-course-pick/pull/886/files) | 코스 목록 Compose UI, 선택 상태, 사용자 좌표로 거리 계산 |
| 팀 프로젝트 | [토독토독 #681](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/681/files), [#793](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/793/files) | 페이지 선로딩 기준, NavHost 범위, 화면 상태와 필수 콜백 |
| 팀 프로젝트 | [책책 #526](https://github.com/woowacourse-teams/2026-chaekchaek/pull/526/files) | 실패 후 재시도 UI 테스트의 단언 |

## 길쪽지에 적용할 판단

### 1. 같은 타입이라도 출처와 기준이 다르면 호출부에서 구분한다

런세권 리뷰에서는 지도 카메라 좌표를 넘겨서 사용자와 코스 사이의 거리가 지도 중심 기준으로 표시되는 사례를 짚었다. 같은 좌표 타입이어도 어떤 대상을 기준으로 계산하는지에 따라 결과가 달라진다. 작성자 답글은 좌표가 목록 조회 범위를 정하는 값이 아니라 거리 계산 기준이라고 설명한다. [리뷰](https://github.com/woowacourse-teams/2025-course-pick/pull/886#discussion_r3252817197) · [답글](https://github.com/woowacourse-teams/2025-course-pick/pull/886#discussion_r3254704865)

길쪽지의 `LocationFix`와 `LocationAnchor`를 읽을 때 SDK 콜백 수신 시각, 기준 위치 선택 시각, 쪽지의 UTC 기록 시각을 각각 확인한다. `LocationBuffer`의 30초 기준은 실제 목격 시각을 복원하는 값과 의미가 다르다. 저장이나 수신 코드를 리뷰할 때에는 “이 좌표는 어느 차량의 어느 시점인가?”와 “이 두 시간은 같은 시계 기준인가?”를 먼저 묻는다.

### 2. 상태 하나에서 계산할 수 있는 값을 또 저장할 때는 이유가 필요하다

런세권에서는 항목의 `selected`와 화면의 `selectedCustomCourse`가 같은 선택을 표현하는지, 재선택을 이벤트로 따로 다루는 이유가 무엇인지 물었다. 재선택으로 지도를 다시 맞추는 동작과 현재 선택을 표시하는 상태는 각자 의미가 있다. 토독토독에서는 `Loading`과 `Success.isLoading`이 같은 로딩을 뜻하는지 질문했다. 초기 로딩과 기존 데이터를 유지하는 새로고침이라면 두 경우의 이름과 화면 결과를 드러낼 수 있다. [런세권 리뷰](https://github.com/woowacourse-teams/2025-course-pick/pull/886#discussion_r3254447318) · [토독토독 리뷰](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/793#discussion_r2450130817)

길쪽지의 `LocationView`가 수집 중 여부를 `TrackingStatus`에서 계산하는 방식이 좋은 예다. 권한 요청 대기, 서비스 시작 실패, 실행 중인 추적은 각각 다른 주체가 관리하는 사실이다. 함께 바뀌어야 하는 중복 상태가 확인되면 그때 정리하고, `generation`처럼 늦은 콜백을 거르는 세션 식별은 보존한다.

### 3. 화면과 호출자가 제어할 값을 구분한다

회원가입 미션에서 고정 너비와 여백을 갖던 버튼은 호출자가 `Modifier`를 전달하도록 바뀌었다. 다른 미션에서는 입력 상태를 화면 바깥에서 받도록 논의했다. 토독토독에서는 동작이 필수인 카드 클릭 콜백의 기본값이 호출자 누락을 숨길 수 있다고 짚었다. [버튼 수정 답글](https://github.com/woowacourse/android-signup/pull/2#discussion_r1798832943) · [상태 호이스팅 리뷰](https://github.com/woowacourse/android-signup/pull/37#discussion_r1808799259) · [필수 클릭 콜백 리뷰](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/793#discussion_r2450090477)

길쪽지의 `LocationView(status, onStart, onStop, ...)`처럼 화면은 표시할 상태와 행동을 인자로 받으면 Preview에서도 실제 SDK 없이 그릴 수 있다. 시작·중지처럼 필수 동작은 호출자가 연결하도록 두고, 버튼의 터치 영역처럼 컴포넌트가 지켜야 하는 제약은 내부에 둘 이유가 있다.

### 4. 기능의 범위가 드러나는 패키지와 공개 API를 유지한다

런세권에서는 코스 화면에서 쓰는 컴포넌트를 전역 `component`에 둔 이유를 물었다. 토독토독에서는 화면 하나를 위해 `NavHost`를 두고 외부에서 쓸 일이 보이지 않는 `NavHostController`까지 받는 구성을 검토했다. 실제 사용처를 확인해야 공용화가 줄이는 코드와 공용화가 늘리는 결합을 비교할 수 있다. [런세권 패키지 리뷰](https://github.com/woowacourse-teams/2025-course-pick/pull/864#discussion_r3199034876) · [토독토독 NavHost 리뷰](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/793#discussion_r2450053969)

길쪽지에서는 `ui/location`, `location`, `notes/storage`만으로 화면, 위치 처리, Room 구현을 찾을 수 있다. `LocationSource`는 가짜 소스로 세션을 테스트하는 실제 사용처가 있고, `RoadNoteStore`는 Room 구현과 쪽지 저장 계약을 분리한다. 비슷한 이름의 인터페이스나 폴더를 더 만들기 전에 어떤 호출자나 테스트가 그 경계를 쓸지 확인한다.

### 5. 함수 이름과 숫자에서 제품 정책을 읽을 수 있게 한다

회원가입 미션의 `validateUsername`은 검증 메시지를 돌려줬고, 리뷰 후 `getValidationMessage`로 이름을 바꿨다. 토독토독의 선로딩 기준 3개는 이름이 이미 있었지만, 왜 3개인지 묻자 작성자는 너무 일찍 다음 페이지를 요청하는 비용을 설명했다. [이름 변경 답글](https://github.com/woowacourse/android-signup/pull/38#discussion_r1810551823) · [선로딩 기준 논의](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/681#discussion_r2373860024)

길쪽지에서는 `selectAnchor()`의 결과에서 선택 실패 이유를 확인할 수 있다. 45초 보관, 30초 목표, 5초 신선도와 5초 목표 오차는 서로 다른 정책이다. 값이 우연히 같아도 이름을 별도로 유지하고, 경계에서 어떤 쪽지가 선택되는지 테스트로 설명한다.

### 6. 예외 문법은 실패 처리 계약에 맞춘다

회원가입 미션에서 리뷰어는 `try/catch`를 사용한 이유를 물었다. 후속 답글에서는 변경을 요구한 것이 아니라고 밝히고, 성공과 실패에서 값을 반환하는 경우 `runCatching().fold()`를 예로 들었다. 단순 이메일 검증에서 나온 제안이다. [질문](https://github.com/woowacourse/android-signup/pull/30#discussion_r1805878269) · [리뷰어 답글](https://github.com/woowacourse/android-signup/pull/30#discussion_r1808068037)

길쪽지의 위치 수집과 Room 저장에는 취소 전파, 오류 변환, 리스너 정리처럼 관찰 가능한 동작이 있다. `try/catch/finally`를 바꾸기 전에 어떤 예외를 처리하는지와 `CancellationException`이 호출자에게 전달되는지를 비교한다. [Android 코루틴 권장사항](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)

### 7. 실행 시점과 관찰 지점을 리뷰한다

토독토독에서는 `LaunchedEffect`의 키가 `Unit`일 때와 `viewModel`일 때 어떤 실행 차이가 생기는지 물었다. 책책에서는 재시도 테스트의 `first() == last()` 비교가 요청 한 건만 있어도 통과한다고 지적했다. 요청이 실제 두 번 나갔는지와 두 번째 제출 상태가 관찰됐는지를 확인해야 재시도를 검증할 수 있다는 피드백이다. [Effect 키 리뷰](https://github.com/woowacourse-teams/2025-Todok-Todok/pull/793#discussion_r2450074839) · [재시도 테스트 리뷰](https://github.com/woowacourse-teams/2026-chaekchaek/pull/526#discussion_r4290226548)

길쪽지의 SDK 수집 검증에서도 화면 복귀 후 좌표가 보이는 것과 화면을 벗어나 있는 동안 새 좌표가 들어온 것은 다른 결과다. 기존 `TrackingLifecycleTest`처럼 구간 시작 전후의 실제 콜백 시각과 등록 횟수를 확인한다. Room 저장 시연에서는 성공 문구와 함께 실제 저장 결과를 확인한다.

## 다음 PR에서 먼저 볼 질문

1. 같은 타입의 인자끼리 바꿔 넣어도 컴파일되는 곳이 있는가? 그때 사용자에게 어떤 잘못된 결과가 보이는가?
2. 상태가 어디에서 시작해 어디에서 바뀌는가? 함께 변경하는 값과 독립적으로 관리하는 값을 설명할 수 있는가?
3. 콜백·타이머·효과가 시작되는 조건과 종료되는 조건이 코드에서 가까이 보이는가?
4. 공용 함수, 패키지, 인터페이스가 해결하는 실제 호출처나 테스트가 있는가?
5. 검증하려는 동작을 빼고도 테스트가 통과하는가? 횟수·시점·실제 저장 중 필요한 관찰을 단언하는가?

검증 범위는 공개 인라인 리뷰와 해당 변경 코드, 일부 작성자 답글 및 반영 커밋이다. 전체 PR 이력이나 팀의 모든 규칙을 조사하지 않았고, 외부 앱과 길쪽지 앱은 이 조사에서 빌드·실행하지 않았다.

# 우아한테크코스 Android 미션 리뷰에서 배운 설계 기준

2026-11-12. [6기 Android 크루 아카이브](https://github.com/kimhm0728/woowacourse-archive)에 나열된 본과정 레벨 1·2·4 코딩 미션 11종의 공개 PR 22개와, 다른 참가자의 Compose PR 2개를 표본으로 삼았다. 인라인 리뷰의 변경 코드, 피드백, 확인할 수 있는 답글을 함께 읽었다. 팀 프로젝트 사례는 [기존 Compose 레슨런](woowacourse-compose-review-lessons.md)에 있다.

## 검토한 미션 PR

| 레벨 | 미션 | PR |
| --- | --- | --- |
| 1 | 자동차 경주 | [#100](https://github.com/woowacourse/kotlin-racingcar/pull/100), [#116](https://github.com/woowacourse/kotlin-racingcar/pull/116) |
| 1 | 로또 | [#74](https://github.com/woowacourse/kotlin-lotto/pull/74), [#94](https://github.com/woowacourse/kotlin-lotto/pull/94) |
| 1 | 블랙잭 | [#72](https://github.com/woowacourse/kotlin-blackjack/pull/72), [#97](https://github.com/woowacourse/kotlin-blackjack/pull/97) |
| 1 | 오목 | [#62](https://github.com/woowacourse/kotlin-omok/pull/62), [#93](https://github.com/woowacourse/kotlin-omok/pull/93) |
| 2 | 영화 티켓 예매 | [#72](https://github.com/woowacourse/android-movie-ticket/pull/72), [#83](https://github.com/woowacourse/android-movie-ticket/pull/83) |
| 2 | 영화 극장 선택 | [#57](https://github.com/woowacourse/android-movie-theater/pull/57), [#75](https://github.com/woowacourse/android-movie-theater/pull/75) |
| 2 | 쇼핑 장바구니 | [#54](https://github.com/woowacourse/android-shopping-cart/pull/54), [#79](https://github.com/woowacourse/android-shopping-cart/pull/79) |
| 2 | 쇼핑 주문 | [#65](https://github.com/woowacourse/android-shopping-order/pull/65), [#79](https://github.com/woowacourse/android-shopping-order/pull/79) |
| 4 | DI 구현 | [#86](https://github.com/woowacourse/android-di/pull/86), [#109](https://github.com/woowacourse/android-di/pull/109) |
| 4 | 복잡한 뷰 | [#85](https://github.com/woowacourse/android-paint/pull/85), [#97](https://github.com/woowacourse/android-paint/pull/97) |
| 4 | Compose 회원가입 | [#2](https://github.com/woowacourse/android-signup/pull/2), [#30](https://github.com/woowacourse/android-signup/pull/30), [#37](https://github.com/woowacourse/android-signup/pull/37), [#38](https://github.com/woowacourse/android-signup/pull/38) |

## 길쪽지에 적용할 레슨런

### 1. 값이 없다는 결과와 조회 실패는 다음 행동이 다르다

장바구니 코드는 조회를 `runCatching`으로 감싸고 실패하면 상품을 새로 삽입했다. 리뷰어는 상품이 없어서 삽입해도 되는 경우와 조회 자체가 실패한 경우를 구분하라고 지적했다. [장바구니 리뷰](https://github.com/woowacourse/android-shopping-cart/pull/79#discussion_r1615217336)

길쪽지에서도 쪽지를 찾지 못한 경우, DB가 실패한 경우, 사용자가 작업을 취소한 경우를 호출부에서 서로 다르게 처리할 수 있어야 한다. 오류를 `null`로 바꾸는 리팩터링은 후속 쓰기 동작까지 확인한다. 저장 성공을 확인하기 전에 성공 안내를 내보내는 경로도 함께 살핀다.

### 2. 상태를 저장할 때는 원본과 같이 갱신해야 하는지 살핀다

블랙잭에서는 카드를 받을 때마다 멤버 변수 `cardSum`을 초기화해 다시 쓰는 코드를 보고, 카드 목록에서 점수를 계산할 수 있는데 멤버로 저장할 이유가 있는지 물었다. 오목에서는 게임 종료 상태를 Activity 플래그로 관리하는 대신 게임이 판단하도록 제안했다. [블랙잭 리뷰](https://github.com/woowacourse/kotlin-blackjack/pull/72#discussion_r1518410948) · [오목 리뷰](https://github.com/woowacourse/kotlin-omok/pull/93#discussion_r1545670658)

길쪽지에서 `collecting`을 `TrackingStatus`로 계산하는 방식은 유지할 만하다. 세션 번호, 마지막 이벤트 시각처럼 늦은 콜백을 걸러내기 위해 기억해야 하는 값에는 별도 역할이 있다. 다시 계산하는 비용이 중요한 값이라면, 그때 캐시의 갱신 조건을 정한다.

### 3. 화면 재생성 전후에 다시 그릴 상태와 한 번 처리할 행동을 구별한다

영화 예매에서는 복원 코드 삭제 후 인원과 스피너 선택이 초기화됐고, 작성자는 둘 다 복원하도록 수정했다고 답했다. 쇼핑 주문에서는 상품 화면 상태를 소비된 이벤트처럼 읽어 화면 회전 뒤 로딩에서 벗어나지 않는 동작이 관찰됐다. [예매 리뷰](https://github.com/woowacourse/android-movie-ticket/pull/83#discussion_r1581849909) · [수정 답글](https://github.com/woowacourse/android-movie-ticket/pull/83#discussion_r1581868548) · [주문 리뷰](https://github.com/woowacourse/android-shopping-order/pull/65#discussion_r1621948512)

길쪽지에서는 재생성된 화면이 서비스의 현재 수집 상태를 다시 읽어야 한다. 권한 팝업을 기다리던 시작 요청은 그 요청의 유효 기간을 따른다. 구현 리뷰에서는 “재생성 직후 무엇을 보여주는가?”와 “SDK가 새로 등록되는가?”를 별도로 확인한다.

### 4. 테스트는 비동기 작업이 끝나는 조건을 관찰한다

쇼핑 주문의 목록 테스트는 로딩 완료를 기다리지 않고 표시 여부를 검사해 불안정했다. 리뷰어는 로딩이 끝났을 때 목록이 보인다는 시나리오로 구체화했고, 후속 대화에서는 ViewModel에서 검증할 정책과 UI 연결에서 확인할 부분을 논의했다. 자동차 경주에서는 난수에 기대는 단위 테스트의 재현성도 질문했다. [로딩 테스트 논의](https://github.com/woowacourse/android-shopping-order/pull/79#discussion_r1632043101) · [후속 논의](https://github.com/woowacourse/android-shopping-order/pull/79#discussion_r1632078656) · [난수 테스트 리뷰](https://github.com/woowacourse/kotlin-racingcar/pull/100#discussion_r1490899810)

길쪽지에서는 가짜 시계와 위치 소스로 버퍼의 시간 경계를 고정하고, 실제 Pleos 연결에서만 확인할 수 있는 수집은 에뮬레이터에서 검증한다. 화면을 벗어난 동안의 콜백이나 Room 쓰기 완료처럼 요구사항을 직접 보여주는 증거를 기다린다.

### 5. 규칙을 결정하는 곳과 외부 작업을 실행하는 곳을 찾는다

자동차 경주에서는 `processStep()`이라는 넓은 이름을 계기로, 자동차가 어떻게 움직이는지까지 Controller가 결정할 필요가 있는지 논의했다. 영화 극장 선택에서는 UI 모델이 Repository를 직접 조회하는 코드를 지적했고, 오목에서는 게임 규칙 객체에 DAO를 받는 것이 적절한지 물었다. [경주 리뷰](https://github.com/woowacourse/kotlin-racingcar/pull/116#discussion_r1493338360) · [극장 선택 리뷰](https://github.com/woowacourse/android-movie-theater/pull/75#discussion_r1596555692) · [오목 DAO 리뷰](https://github.com/woowacourse/kotlin-omok/pull/93#discussion_r1545657021)

길쪽지의 30초 전 표본 선택은 `LocationBuffer`에서 판단하고, 서비스는 수집을 시작·중지한다. 쪽지 화면에 필요한 값을 조합할 때 UI 모델 생성자나 Composable 안에서 몰래 DB를 조회하는 경로가 생기는지 본다. 오목 리뷰의 Activity 이동 제안은 당시 구조에 대한 선택지다. 길쪽지에 적용할 교훈은 저장과 규칙의 경계를 드러내는 것이다.

### 6. 클래스와 인터페이스는 지금 설명할 수 있는 책임을 담는다

자동차 경주에서 `TryCount`가 표준 `repeat`를 감싸자 리뷰어는 직접 호출을 제안했다. 답글에서는 횟수와 반복 행위를 묶는 장점도 인정하고 비용을 비교했다. DI 미션에서는 같은 타입에 여러 구현체가 있을 때 등록 키가 부족하다는 점을 짚었고, 작성자는 실제로 그 요구가 들어오는 다음 단계까지 변경을 미뤘다. [반복 래퍼 논의](https://github.com/woowacourse/kotlin-racingcar/pull/116#discussion_r1493336925) · [트레이드오프 답글](https://github.com/woowacourse/kotlin-racingcar/pull/116#discussion_r1493643885) · [DI 단계 합의](https://github.com/woowacourse/android-di/pull/86#discussion_r1744798353)

길쪽지의 `LocationSource`에는 실제 SDK와 가짜 입력을 바꾸는 사용처가 있다. 반면 표준 기능이나 한 호출을 그대로 전달하는 래퍼라면 삭제했을 때 어떤 정책이나 테스트 경계가 사라지는지 확인한다. 새 구현이 필요한 시점에 계약을 넓힌다.

### 7. 호출부에서 반환값과 정책의 소유자를 읽을 수 있게 한다

로또에서는 `WinningStatistics.get(LottoPrize)`가 당첨 수량을 돌려주는지 금액을 돌려주는지 모호하다고 짚었다. 상수도 사용 횟수보다 그 값을 가장 잘 설명하는 객체에 두는 방향을 제안했다. [반환값 리뷰](https://github.com/woowacourse/kotlin-lotto/pull/94#discussion_r1505939907) · [상수 리뷰](https://github.com/woowacourse/kotlin-lotto/pull/74#discussion_r1499380832)

길쪽지에서 `receivedElapsedMillis`처럼 기준과 단위를 드러내는 이름을 사용한다. 45초 보관, 30초 목표, 5초 신선도, 5초 목표 오차는 위치 버퍼 안에서 각 정책의 이름으로 읽을 수 있다. 두 숫자가 같더라도 각 정책을 바꾸는 이유는 다를 수 있다.

### 8. 렌더링이 상태를 바꾸는 코드라면 이벤트 쪽을 함께 읽는다

그리기 미션에서 리뷰어는 `onDraw`에서 이전 도형을 지우는 대신 터치 이동 시 경로를 갱신하자고 제안했다. 작성자는 기존 경로를 재사용하고 불필요한 `TouchType`을 없애는 방향으로 수정했다. 다른 스레드에서는 객체 속성을 설정하는 블록의 의도가 `run`보다 `apply`에서 더 드러나는지 논의했다. [렌더링 리뷰](https://github.com/woowacourse/android-paint/pull/97#discussion_r1787208335) · [작성자 답글](https://github.com/woowacourse/android-paint/pull/97#discussion_r1788592035) · [스코프 함수 리뷰](https://github.com/woowacourse/android-paint/pull/85#discussion_r1782162506)

길쪽지 Compose에서 화면 상태를 읽는 흐름은 화면을 그리는 데 쓰고, 수집 시작과 중지는 사용자 동작이나 명시한 수명 이벤트에서 실행한다. SDK 등록과 해제처럼 부수 효과를 수행하는 부분은 호출 순서와 정리 조건을 한눈에 드러내는 쪽을 택한다.

### 9. Compose 재사용은 호출자가 실제로 조정해야 하는 값부터 연다

회원가입에서는 고정 크기 버튼을 호출자의 `Modifier`로 조정하도록 수정했다. 상태 호이스팅에서는 화면 바깥에서 입력 상태를 넘기는 방향을 논의했다. 같은 미션의 `runCatching` 논의는 이메일 검증의 표현 선택이었고, 리뷰어는 변경 요구가 아니었다고 답했다. [Modifier 답글](https://github.com/woowacourse/android-signup/pull/2#discussion_r1798832943) · [상태 호이스팅 리뷰](https://github.com/woowacourse/android-signup/pull/37#discussion_r1808799259) · [예외 표현 답글](https://github.com/woowacourse/android-signup/pull/30#discussion_r1808068037)

길쪽지 화면은 상태와 사용자 행동을 받아 렌더링하는 현재 형태를 활용한다. 배치 변경이 실제로 필요한 컴포넌트에 `Modifier`를 열고, 시작·중지처럼 반드시 연결해야 할 행동은 호출부에 드러낸다. 코루틴이나 SDK 코드에서 예외 문법을 바꿀 때는 취소 전파와 자원 정리를 함께 비교한다.

## 다음 PR에서 사용할 질문

- 결과가 비어 있을 때, 실패했을 때, 취소됐을 때 각각 다음에 어떤 코드가 실행되나요?
- 이 값을 원본에서 계산할 수 있나요? 별도로 저장하면 어느 코드가 두 값을 같이 갱신하나요?
- 화면을 다시 열거나 SDK 콜백이 뒤늦게 도착하면 어떤 상태와 자원이 남나요?
- 이 인터페이스나 래퍼의 실제 호출자가 얻는 것은 무엇인가요?
- 지금 검증하려는 동작을 제거해도 테스트가 통과할 수 있나요?

조사 범위는 위 PR의 공개 인라인 리뷰 표본이다. 모든 기수와 모든 제출 PR을 전수 조사하지 않았다. 리뷰어의 직무나 재직 여부는 개별 검증하지 않았고, 교육 단계에 맞춘 제안을 실무 전체의 규칙으로 일반화하지 않았다. 앱 빌드와 실행은 이번 자료 조사에 포함하지 않았다.

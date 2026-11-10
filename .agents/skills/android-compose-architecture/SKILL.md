---
name: android-compose-architecture
description: hyundai-and-gretel의 Kotlin·Compose 구현, 리팩터링 및 설계 리뷰에 공모전용 MVVM·Repository·StateFlow 규칙을 적용한다. 현재 구현과 도입 예정 스택을 구분하고 가독성, 취소 전파, 리소스 해제와 제출 준비를 확인한다.
---

# 공모전 프로젝트 설계 및 개발 규칙

## 1. 목적과 적용 방법

개발 속도, 코드 가독성, 안정성, 협업 효율성을 우선한다. Kotlin·Compose 기능 구현, 리팩터링, 설계 리뷰 시 이 문서를 적용한다. 문서만 요청받았다면 소스나 의존성을 변경하지 않는다.

> 코드는 위에서 아래로 자연스럽게 읽혀야 하며, 구현 방법보다 핵심적인 실행 흐름과 의도가 명확하게 드러나야 한다.
>
> 선언적인 Kotlin 코드를 지향하되, 과도한 람다 중첩, 조건문 중첩, 함수 체이닝, 축약 문법으로 가독성을 희생하지 않는다.
>
> 공모전 프로젝트의 특성에 맞게 불필요한 추상화와 보일러플레이트를 최소화하면서도 MVVM, 상태 관리, 예외 처리, 리소스 해제 등 핵심 설계 원칙은 준수한다.

규칙의 강도는 아래와 같다. MUST도 적용 조건이 있을 때의 필수이며, 미사용 기능의 선제 구현을 뜻하지 않는다.

| 표기 | 의미 |
|---|---|
| MUST(필수) | 적용 대상 작업에서 지킨다. 요구사항 변경이 필요하면 근거를 기록한다. |
| SHOULD(권장) | 기본 선택이다. 다른 선택이 더 단순하거나 안전하면 작업 기록에 이유를 남긴다. |
| MAY(선택) | 실제 필요가 확인된 경우에만 도입한다. |

- **MUST**: [AGENTS.md](../../../AGENTS.md)와 [Pleos Connect 스킬](../pleos-connect/SKILL.md)에서 시작하고 해당 작업 기록을 읽는다. 프로젝트 선택을 공식 의무로 표현하지 않는다.
- **MUST**: 현재 브랜치의 소스·호출자·테스트·의존성을 먼저 확인한다. 아래 현황은 확인 시점의 스냅샷이며 다른 브랜치에 그대로 적용하지 않는다.
- **MUST**: 수정 범위·적용 규칙·확인 방법을 `docs/work/<작업명>.md`에 남기고 구현 → 검사 → 검토 → 실패 수정 순서로 진행한다. 결과는 PASS / FAIL / NOT_TESTED / N/A와 증거로 기록한다.
- **SHOULD**: 변경 기능부터 점진적으로 적용한다. 규칙 도입만을 이유로 전체 패키지 이동이나 일괄 리팩터링을 하지 않는다.
- **MUST**: 이 스킬을 커밋·푸시·APK 업로드·외부 계정 초대의 승인으로 해석하지 않는다.

## 2. 현재 프로젝트와 목표 스택

2026-11-11 `main` 확인 기준. [Gradle 설정](../../../app/build.gradle.kts), [버전 목록](../../../gradle/libs.versions.toml), [현재 화면](../../../app/src/main/java/com/jinkim/hyundaiAndGretel/MainActivity.kt), [설정 경위](../../../docs/SETUP.md)가 근거다.

| 항목 | 현재 상태 | 신규 기능 설계 기준 |
|---|---|---|
| 모듈·화면 | 단일 `:app`, ComponentActivity + Compose 시작 화면 | 현재 구조 유지, 기능별 패키지 우선 |
| 언어·UI | Kotlin, Compose Material 3 적용 | 계속 사용 |
| MVVM·Repository | 기능 ViewModel·Repository 미구현 | 기능 추가 시 ViewModel → Repository |
| 상태·비동기 | Lifecycle ViewModel/Compose 및 Coroutines 의존성 있음. StateFlow 화면 상태 미구현 | StateFlow + Coroutines/Flow |
| DI | Hilt 미적용 | 의존성 연결이 필요한 기능 도입 시 Hilt |
| 네트워크 | Retrofit·OkHttp 미적용 | 네트워크 기능 도입 시 Retrofit + OkHttp |
| 화면 이동 | Navigation Compose 미적용 | 여러 화면 연결 시 Navigation Compose |
| 로컬 저장 | DataStore·Room 미적용 | 설정은 DataStore, 구조화된 데이터·쿼리는 Room을 필요 시 선택 |
| 차량 | Vehicle SDK `2.3.2` 의존성 있음, 차량 API 호출 없음 | 기능별 공식 지원·권한·생명주기 확인 후 사용 |
| 테스트 | JUnit·coroutines-test 의존성 있음, 현재 테스트 소스 없음 | 핵심 동작부터 추가 |

현재 소스 구조:

```text
app/src/main/java/com/jinkim/hyundaiAndGretel/
├── MainActivity.kt        # 시작 화면, setContent, Preview
└── ui/theme/
    ├── Color.kt          # 색상 정의
    ├── Theme.kt          # 앱 Theme
    └── Type.kt           # 타이포그래피
```

**MUST**: 위 현황을 기능 구현 완료나 런타임 검증 완료로 해석하지 않는다. Gleo JSON 수신·도로 제보·위치 수집은 현재 `main`에 없다. 다른 브랜치의 구현을 존재한다고 가정하지 않는다.

## 3. 구조와 책임

- **MUST**: MVVM + Repository Pattern을 사용한다. ViewModel이 Repository를 직접 주입받는다. Clean Architecture를 적용하지 않고 별도의 Domain/UseCase 계층을 필수 구성으로 만들지 않는다.
- **MUST**: ViewModel은 사용자 이벤트, Repository 호출, 화면 데이터 조합, 로딩·성공·실패 상태를 담당한다. Retrofit API·DAO·DataStore를 직접 호출하지 않는다.
- **MUST**: Repository는 데이터 소스 접근과 일관된 데이터 처리를 담당한다. UI나 NavController에 의존하지 않는다.
- **SHOULD**: 단일 app 모듈과 기능별 패키지를 유지한다. 공통 패키지는 실제 공유가 생길 때 만든다.
- **MAY**: 여러 호출자에서 반복되는 복잡한 업무 규칙은 책임이 드러나는 클래스로 추출한다. 이름만 다른 위임 계층을 만들지 않는다.
- **MUST**: 필요가 입증되지 않은 Interface, Mapper 클래스, 추상 클래스, BaseViewModel/BaseRepository를 만들지 않는다. DTO·내부 모델·UiState를 의미 없이 복제하지 않는다.

다음은 **향후 기능의 배치 예시**이며 현재 파일 목록이나 확정 서비스 기획이 아니다. 빈 폴더를 미리 만들지 않는다.

```text
com.jinkim.hyundaiAndGretel/
├── MainActivity.kt
├── report/
│   ├── ReportRoute.kt     # ViewModel 연결·상태 수집
│   ├── ReportScreen.kt    # 상태 렌더링·콜백
│   ├── ReportViewModel.kt # 이벤트·화면 상태
│   ├── ReportRepository.kt# 데이터 접근·처리
│   └── Report.kt          # 기능 내부 모델
├── navigation/            # 여러 화면이 필요해지면 추가
├── di/                    # Hilt 외부 객체 제공
└── ui/theme/              # 기존 Theme 재사용
```

의존 방향은 `Screen ← UiState ← ViewModel → Repository → API/저장소/SDK`다. UI 이벤트는 Screen → Route/ViewModel로 올라간다. 데이터 소스가 단순하면 별도 DataSource 클래스 없이 Repository에서 사용한다.

## 4. Kotlin 가독성

- **SHOULD**: Kotlin 공식 컨벤션을 따른다. 함수 이름은 책임과 의도를 나타내고 핵심 함수는 검증 → 실행 → 결과 처리 순서로 읽히게 한다. 복잡함을 주석으로 덮지 않고 이름과 구조로 해소한다.
- **SHOULD**: `val`과 불변 모델을 우선한다. 접근 범위는 가능한 좁히고 의미 있는 상수·enum·sealed interface를 사용한다. 불변 `data class` 안의 변경 가능한 컬렉션도 외부에 노출하지 않는다.
- **MUST**: 외부 데이터의 null을 `!!`로 단정하지 않는다. Safe Call·단순 Elvis·명시적 분기를 사용한다. 의미를 알 수 없는 약어와 매직 값은 피한다.
- **SHOULD**: Guard Clause/Early Return으로 중첩을 줄이고 상태별 분기는 `when`을 쓴다. 복잡한 Boolean은 의미 있는 변수·함수로 분리한다. 조건 제거를 위한 전략 클래스는 만들지 않는다.

```kotlin
// 지양: 성공 경로가 중첩 안에 숨는다.
fun submit(report: Report) {
    if (report.description.isNotBlank()) {
        if (report.location != null) {
            save(report)
        }
    }
}

// 권장: 검증과 실행이 위에서 아래로 읽힌다.
fun submit(report: Report) {
    if (report.description.isBlank()) return showDescriptionError()
    if (report.location == null) return showLocationError()

    save(report)
}
```

위 예시는 흐름 설명용이다. 사용자 입력 실패를 조용히 무시하지 않고 호출자나 UiState에 전달한다.

- **SHOULD**: 컬렉션의 의도가 명확할 때 `filter`, `map`, `any` 등 표준 연산을 사용한다. 부수 효과·조기 중단·복잡한 누적은 반복문이 더 읽기 쉬우면 반복문을 사용한다.
- **SHOULD**: Scope Function(`let/run/apply/also/with`)은 수신 객체와 목적이 분명할 때만 쓴다. 중첩 람다는 명시적인 매개변수 이름을 사용하고 복잡한 체이닝은 중간 변수나 함수로 나눈다.
- **SHOULD**: 다중 Elvis, 중첩된 `if` 표현식, 긴 한 줄, 억지 Expression Body를 피한다. 짧은 코드보다 명확한 코드를 선택한다.

```kotlin
// 지양: 수신 객체와 부수 효과를 한 번에 추적해야 한다.
reports.let { it.filter { it.isVisible }.map { it.toUiModel() } }
    .also { updateCount(it.size) }.let { render(it) }

// 권장: 선언적 변환과 부수 효과를 구분한다.
val visibleReports = reports.filter { report -> report.isVisible }
val items = visibleReports.map { report -> report.toUiModel() }
updateCount(items.size)
render(items)
```

## 5. Compose와 화면 상태

- **MUST**: UDF를 지킨다. UI는 상태를 받고 이벤트를 콜백으로 전달한다. Composable 본문에 업무 로직·네트워크 요청·직접적인 Side Effect를 두지 않는다.
- **SHOULD**: Route는 ViewModel 연결과 `collectAsStateWithLifecycle()` 수집, Screen은 UiState와 콜백을 맡는다. 간단한 화면은 한 파일에 함께 둬도 된다.
- **MUST**: 재사용 Composable이 ViewModel이나 NavController를 직접 참조하지 않게 한다.
- **SHOULD**: 재사용 컴포넌트에 `modifier: Modifier = Modifier`를 제공하고 루트 UI에 적용한다. 색상·타이포그래피는 기존 Theme, 사용자 문자열은 리소스로 관리한다. Lazy 목록에는 안정적인 고유 key를 사용한다.
- **MUST**: 서버 데이터와 화면 업무 상태는 ViewModel에서 관리하고 중복 저장하지 않는다. `remember`는 UI 내부 상태, `rememberSaveable`은 복원할 수 있는 UI 상태에 사용한다. 영속 데이터는 저장소에 둔다. ViewModel은 프로세스 종료까지 상태를 보장하지 않는다.

아래 예시는 목표 구조이며 Hilt/Navigation 의존성을 이미 설치했다는 뜻이 아니다. `Report`, `ReportApi`, `ReportScreen` 등의 선언과 import는 설명상 생략했다.

```kotlin
@Composable
fun ReportRoute(viewModel: ReportViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportScreen(state = state, onRefresh = viewModel::refresh)
}
```

**MUST**: `ReportScreen(viewModel)`로 ViewModel 전체를 전달하는 대신 필요한 상태와 콜백만 넘긴다. 이유는 Preview·단위 UI 검증과 책임 분리를 유지하기 위해서다.

| Effect | 적용 규칙 |
|---|---|
| `LaunchedEffect` | **SHOULD**: 화면 진입·key 변경에 따른 작업. key 변경은 취소·재실행을 유발하므로 의도에 맞게 선택 |
| `DisposableEffect` | **MUST**: 여기서 등록한 리스너는 `onDispose`에서 해제 |
| `rememberUpdatedState` | **MAY**: Effect를 재시작하지 않고 최신 콜백·값을 참조해야 할 때 |
| `rememberCoroutineScope` | **SHOULD**: 스크롤·스낵바 등 UI 이벤트의 화면 수명 작업에 사용. 업무 요청은 ViewModel에 위임 |
| `derivedStateOf` | **MAY**: 원본 값 변화보다 UI 파생 값이 적게 바뀌어 실제 갱신을 줄일 때. 일반 계산마다 사용하지 않음 |

## 6. ViewModel, StateFlow, 이벤트

- **MUST**: 내부는 `MutableStateFlow`, 외부는 읽기 전용 `StateFlow`로 노출한다. 불변 `data class` UiState로 응집도 있는 화면 상태를 표현한다.
- **SHOULD**: 기존 상태를 바꾸면 `update`를 사용한다. `update` 람다는 재평가될 수 있으므로 네트워크·저장 등 부수 효과를 넣지 않는다.
- **MUST**: 로딩·성공·실패와 중복 요청 정책을 정의한다. 늦게 끝난 이전 요청이 최신 화면을 덮는 문제를 고려한다. 검색은 이전 작업 취소, 제출은 중복 방지 등 기능에 맞는 최소 정책을 선택한다.

```kotlin
data class ReportUiState(
    val items: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoadError: Boolean = false,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ReportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { state -> state.copy(isLoading = true, hasLoadError = false) }

        viewModelScope.launch {
            try {
                val result = repository.loadReports()
                result.fold(
                    onSuccess = { reports ->
                        _uiState.update { state -> state.copy(items = reports) }
                    },
                    onFailure = {
                        _uiState.update { state -> state.copy(hasLoadError = true) }
                    },
                )
            } finally {
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }
}
```

이 예시는 Repository가 예상 실패를 `Result`로 반환하고 취소는 다시 던지는 계약을 전제로 한다.

- **MUST**: 사용자 이벤트와 UI 상태를 구분한다. 이동은 UI 계층에서 수행하고 ViewModel은 NavController를 보유하지 않는다.
- **SHOULD**: 완료 확인처럼 놓치면 안 되는 결과는 상태와 소비 확인 콜백으로 표현한다. 스낵바 등은 상태 기반 또는 Channel/SharedFlow 중 실제 수명에 맞게 고른다.
- **MUST**: 일회성 이벤트는 수집 중단 시 유실·재전달·중복 처리 가능성을 검토한다. Channel/SharedFlow가 화면 이동의 정확히 한 번 실행을 보장한다고 가정하지 않는다. UI 수집도 Lifecycle에 연결한다.
- **MUST**: 불필요한 범용 EventBus나 모든 화면을 위한 공통 이벤트 계층을 만들지 않는다.

## 7. Coroutines와 Flow

| 작업 | 기본 선택 |
|---|---|
| 단발성 API 요청·저장 | **SHOULD**: `suspend` 함수 |
| 지속 변경 데이터·GPS·차량 상태 | **SHOULD**: `Flow` |
| 화면의 현재 상태 | **MUST**: `StateFlow` |

- **MUST**: ViewModel에서는 `viewModelScope`를 사용한다. `GlobalScope` 및 메인 스레드의 `runBlocking`을 사용하지 않는다. 비 UI 수신점은 수명 소유자·종료·취소 정책을 명시한다.
- **MUST**: 취소 예외는 다시 전파한다. 정리 작업은 `finally`·`use`·`awaitClose` 등으로 보장한다.
- **SHOULD**: 실제 블로킹 I/O는 데이터 계층에서 IO, 큰 CPU 계산은 Default로 전환한다. Retrofit suspend 요청을 관성적으로 `withContext(IO)`로 감싸지 않는다. 테스트에 필요하면 Dispatcher를 주입한다.
- **SHOULD**: `map/combine`은 변환·조합, `flatMapLatest`는 최신 요청 전환, `debounce`는 입력 빈도 제어, `distinctUntilChanged`는 중복 제거에 사용한다. 무의미한 변환·중첩 collect를 피한다.
- **MAY**: 공유할 필요가 있을 때 `stateIn/shareIn`을 사용한다. UI 전용 수집은 `WhileSubscribed` 등의 시작·중단 정책을 선택한다. UI 수집 중단만으로 다른 scope에서 실행 중인 producer까지 멈춘다고 가정하지 않는다.
- **MUST**: Flow `catch`가 처리하는 upstream 범위를 이해하고 downstream 오류를 숨기는 용도로 쓰지 않는다. 직접 예외를 잡을 때도 취소를 삼키지 않는다.
- **MUST**: GPS/차량 콜백을 `callbackFlow`로 감싸면 `awaitClose`에서 해제한다. 다중 수집 시 중복 등록, 등록 실패, 권한 거절, 값 누락, 버퍼 초과 시 처리도 실제 SDK 계약에 맞게 정한다.

```kotlin
// 패턴 예시. 실제 SDK 심볼이 아니며 최신 값만 필요한 UI 관측에 한정한다.
fun observeSpeed(): Flow<Speed> = callbackFlow {
    val listener = SpeedListener { speed -> trySend(speed) }
    source.register(listener)
    awaitClose { source.unregister(listener) }
}.conflate()
```

**MUST**: 빠진 이벤트가 허용되지 않는 기록·제어 명령에는 위 `conflate()` 패턴을 그대로 쓰지 않는다. 화면 이탈·백그라운드·권한 회수 시 무엇을 중단할지 정한다. Pleos에서는 앱이 안 보여도 프로세스가 살아 있을 수 있다.

## 8. Repository, 예외 처리, DI

- **SHOULD**: 단발 요청의 예상 실패에는 표준 `Result<T>`를 우선 검토한다. 정상적인 입력 검증 결과는 별도로 전달한다. 커스텀 Result Wrapper를 관성적으로 만들지 않는다.
- **MUST**: 실패를 빈 목록이나 성공으로 바꾸지 않는다. UI에서 Retrofit/OkHttp 예외를 직접 분기하지 않는다. 원시 예외 메시지를 사용자에게 그대로 보여주지 않는다.
- **MAY**: 화면 행동이 달라질 때 네트워크 단절·인증·서버 오류를 구분하는 `sealed interface AppError`를 도입한다. `Result`의 예외를 Repository 경계에서 앱 오류로 분류하거나 최소 오류 모델을 사용하되 표현을 이중으로 만들지 않는다.

```kotlin
// 지양: 취소도 실패로 바꾸고 예외 원문을 UI에 전달하기 쉽다.
suspend fun loadReports() = runCatching { api.getReports() }

// 권장: 취소는 전파하고 예상되는 통신 실패만 Result에 담는다.
class ReportRepository @Inject constructor(private val api: ReportApi) {
    suspend fun loadReports(): Result<List<Report>> {
        return try {
            val reports = api.getReports().map { dto -> dto.toReport() }
            Result.success(reports)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Result.failure(error)
        } catch (error: HttpException) {
            Result.failure(error)
        }
    }
}
```

이 예시는 Retrofit의 body 반환 suspend API를 가정한다. `Response<T>`를 반환하면 HTTP 실패 응답을 명시적으로 처리한다. 파싱·저장 오류는 사용하는 라이브러리 계약에 맞춰 처리하고 프로그래밍 오류까지 무조건 삼키지 않는다.

- **MUST**: `runCatching`은 `CancellationException`도 포착함을 고려한다. suspend 작업에서 사용하면 취소를 다시 던진다. **MAY**: 같은 처리가 반복될 때만 `suspendRunCatching`을 추출한다.
- **MUST**: `require`는 프로그래머가 지켜야 할 인자 사전 조건, `check`는 내부 상태·불변식에 사용한다. 사용자 입력 오류는 검증 결과/UiState로 제공하고 예외를 정상 분기 수단으로 남용하지 않는다.
- **SHOULD**: DTO는 Repository 경계에서 필요한 내부 모델로 바꾼다. 단순 매핑은 확장 함수면 충분하다. DTO를 UI에 직접 전달하거나 Mapper Interface를 자동 생성하지 않는다.
- **MAY**: Repository Interface는 Fake를 통한 테스트나 실제 구현 교체 등 필요가 확인됐을 때 도입한다. 구체 Repository를 사용하면 Fake API·저장소로 테스트할 수도 있다. 테스트를 위해 모든 클래스를 `open`으로 바꾸지 않는다.
- **MUST**: Hilt 도입 후 ViewModel은 `@HiltViewModel`과 생성자 주입, Repository는 `@Inject constructor`를 사용한다. ViewModel에서 Repository를 직접 생성하지 않는다.
- **MUST**: Hilt 최초 도입 작업에는 Gradle 플러그인·코드 생성 설정, `@HiltAndroidApp` Application, Manifest 등록, 필요한 `@AndroidEntryPoint`를 함께 구성하고 빌드한다. 현재 적용됐다고 가정하지 않는다.
- **SHOULD**: Retrofit·OkHttp·Room 등 외부 객체는 Hilt Module의 `@Provides`, 실제 Interface와 구현체 연결은 `@Binds`를 사용한다. 생성자 주입 가능한 클래스에 불필요한 Module을 만들지 않는다.
- **MUST**: `@Singleton`은 실제 공유할 객체에만 사용한다. Application 수명 객체에 Activity를 보관하지 않는다.
- **MUST**: 민감정보·인증 토큰·개인 위치·제보 원문을 로그에 남기지 않는다. 사용자용 오류와 개발자 진단을 분리하며 release에서 인증 헤더/본문 로그를 노출하지 않는다.

## 9. 테스트와 협업

- **MUST**: 핵심 업무 로직과 위험한 변경에 동작 테스트를 작성한다. ViewModel의 주요 로딩·성공·실패, 입력 검증, 취소·중복 요청, 저장 데이터 유실 위험을 우선한다.
- **SHOULD**: Coroutines 테스트는 `runTest`와 테스트 Dispatcher를 사용하고 ViewModel 테스트의 Main Dispatcher를 설정·복원한다. 실제 서버·GPS·차량 없이 Fake 데이터 소스로 검증한다.
- **MUST**: 구현의 호출 순서를 복제하는 테스트보다 사용자가 관찰하는 결과를 검증한다. 필요한 규칙을 테스트 없이 완료라고 보고하지 않는다.
- **SHOULD**: 일정상 핵심 경로부터 검증한다. 단순 문서·문구 변경에 불필요한 테스트나 의존성을 추가하지 않는다.
- **MUST**: [검증 스킬](../pleos-verify/SKILL.md)에 따라 하네스, 영향받은 단위 테스트, Android 변경 시 assemble/lint 및 IDE 빌드를 수행한다. 필요한 차량·화면 증거가 없으면 NOT_TESTED다.
- **SHOULD**: 기능 단위로 변경을 작게 묶고, 공용 모델/API 계약 변경은 호출자·fixture·관련 문서를 같은 변경에 반영한다. PR에는 문제·변경 동작·검증·남은 제한을 적는다.

## 10. 공모전 제출 규칙

아래는 **사용자가 제공한 공모전 제출 조건 및 팀 운영 규칙**이다. 공모전 공지 URL·팀명·심사 계정은 아직 제공되지 않았다. 실제 제출 시 최신 공지와 대조하고 값을 확정한다. Pleos App Market 심사와 공모전 제출을 혼동하지 않는다.

### 저장소·APK·README

- **MUST**: 새 저장소 또는 기존 저장소에 프로젝트 전체 소스와 실행 가능한 **Release APK**를 제공한다. 팀명을 확인하고 `apk/팀명_앱이름_release.apk`로 배치한다. debug/unsigned APK를 제출용으로 바꾸어 부르지 않는다.
- **MUST**: Release 서명, build variant, 필요한 환경 설정·권한, 외부 연동과 주행 정책을 확인하고 대상 Pleos 환경에서 설치·실행한다. debug에서만 가능한 연동은 Release에서 따로 검증한다.
- **MUST**: APK가 GitHub 업로드 한도를 넘으면 GitHub Releases에 첨부하고 해당 Release 페이지를 제출한다. 제출 페이지에는 저장소 URL과 APK 경로 또는 Release 링크를 입력한다. GitHub 한도는 업로드 방식별 최신 공식 문서를 확인한다.
- **MUST**: README에 앱 이름과 한 줄 소개, 실행 방법, 테스트 환경·필요 설정, APK 위치 또는 Release 링크를 포함한다. **MAY**: 주요 기능·실제 적용 스택·실행 화면을 추가한다.
- **SHOULD**: 루트 `ARCHITECTURE.md`가 필요하면 이 스킬을 설계 원본으로 링크한다. 같은 규칙을 두 문서에 복사하지 않는다.

제출 시 구성 예시이며 현재 APK가 존재한다는 뜻이 아니다.

```text
project/
├── app/
├── apk/팀명_앱이름_release.apk
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── .agents/skills/android-compose-architecture/SKILL.md
└── README.md
```

### 현재 확인된 제출 준비 항목

| 현재 상태 | 제출 전에 해야 할 일 |
|---|---|
| `.gitignore`가 `*.apk` 제외 | 저장소 직접 배치 시 검증된 제출 APK만 추적하도록 좁은 예외 추가. 모든 빌드 산출물을 해제하지 않음 |
| namespace `com.jinkim.hyundaiAndGretel`, applicationId `com.jinnayeong.wakeup` | 앱 식별자를 팀에서 확정하고 외부 등록·스키마·설치 영향 확인. 이 문서 작성에서는 수정하지 않음 |
| Release signingConfig 미설정 | 비밀키를 저장소 밖에서 관리해 Release 서명 구성·검증 |
| README는 시작 프로젝트 안내 | 실제 기능·재현 가능한 실행 환경·APK 링크로 갱신 |
| 제출 APK·최종 링크 미확인 | APK 설치·기동·핵심 기능·권한 거절 시 동작 및 링크 접근 확인 |

### Private 저장소와 보안

- **MAY**: Private 저장소로 제출한다. **MUST**: 그 경우 공지된 심사용 GitHub 계정을 Collaborator로 초대하고 마감 전 실제 접근 가능 여부를 확인한다. 개인 Private 저장소의 Collaborator는 쓰기 권한도 가질 수 있으므로 초대 전 권한과 대상을 확인한다.
- **MUST**: API Key·토큰·비밀번호·서명키를 소스·APK 내 평문 설정·로그·커밋 이력에 노출하지 않는다. 클라이언트에 포함할 수밖에 없는 키는 비밀로 간주하지 않고 공급자 제한 정책을 적용한다.
- **MUST**: `local.properties`, 로컬 SDK 경로, `.env`, signing 설정 등 제외 여부를 `.gitignore`로 확인한다. 이미 유출된 비밀은 파일 삭제만으로 해결하지 말고 폐기·교체한다.
- **MUST**: 최종 저장소·APK 또는 Release URL을 심사자가 접근할 수 있는 조건에서 열어 확인한다. 브랜치/커밋, APK 버전·서명·테스트 환경과 검증 결과를 기록한다.
- **MUST**: 실제 초대·게시·업로드는 사용자에게 요청받은 범위에서만 실행한다. 문서에 체크리스트가 있다는 이유로 실행하지 않는다.

## 11. 공식 참고 자료

확인일: 2026-11-11. 아래는 플랫폼 동작의 근거이며 이 문서의 MUST/SHOULD/MAY와 기술 선택은 프로젝트 규칙이다. 라이브러리 도입 시 현재 Gradle/Kotlin 호환성을 별도 확인하고 문서의 최신 버전을 그대로 강제하지 않는다.

- [Android Architecture 권장사항](https://developer.android.com/topic/architecture/recommendations): UI·데이터 책임, UDF 및 생명주기 기반 상태 수집의 참고. 공식 권장과 팀의 필수 규칙을 구분한다.
- [Compose Side Effects](https://developer.android.com/develop/ui/compose/side-effects): Effect별 수명과 key, 정리 동작.
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html): 이름·배치·표현식 가독성.
- [Hilt 공식 가이드](https://developer.android.com/training/dependency-injection/hilt-android): 생성자 주입·모듈·Android 진입점.
- [runCatching API](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run-catching.html): Throwable 포착 범위. 취소 예외 보존 필요.
- [callbackFlow API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html): cold 수집·콜백 해제 및 awaitClose.
- [Pleos 앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model): 멀티 윈도우·백그라운드 전환과 수집 정책. 화면·차량의 세부 규정은 해당 Pleos 스킬에서 추가 확인.
- [GitHub 대용량 파일](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github): 업로드 한도 및 Release 배포 대안.

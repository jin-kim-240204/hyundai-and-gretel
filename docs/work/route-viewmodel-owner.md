# Route에서 ViewModel 조회

- 요청: MainActivity가 ViewModel을 얻어 전달하지 않고 RoadNotesRoute가 기본 인자로 조회한다.
- 적용 규칙: 클린 코드12/17/19, Compose 아키텍처의 화면 진입점/상태 구독 분리. 현재 단일 화면·Activity 범위 유지.
- 수정: Activity의 by viewModels와 관련 import 제거. Route에서 androidx.lifecycle.viewmodel.compose.viewModel() 사용. window를 필수 첫 인자로 전달하고 ViewModel 직접 주입도 가능하게 유지.
- 공식 근거: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis 의 closest ViewModelStoreOwner. Nav/별도 소유자가 없으므로 같은 Activity ViewModelStore 및 기본 AndroidViewModel Factory를 사용한다. Application 생성자와 서비스 bind/unbind 코드는 변경하지 않는다.
- 호출부: MainActivity 한 곳. TrackingLifecycleTest의 ViewModelProvider(Activity) 조회는 같은 소유자이므로 변경하지 않는다.
- 기존 사용자 polling·별도 문서 변경 보존. 새 테스트/Nav/별도 소유자 추가 없음. Compose viewModel() API에 필요한 lifecycle-viewmodel-compose를 기존 Lifecycle과 같은 2.9.1로 추가한다.
- 초기 컴파일에서 Compose용 ViewModel 모듈이 없어 참조 실패. 표준 AndroidX 연결 모듈 한 개를 추가하여 해결한다.
- PASS: JVM100/실패0/오류0/skipped0, debug/release·계측APK 컴파일/lint. `/private/tmp/road-note-route-viewmodel-build2.log`. 실제 Activity 재생성·SDK 동작은 NOT_TESTED.
- 사용자 IDE import 오류 후속 확인: Studio 검사에서 compose/viewModel 미해결2건 재현. IDE의 마지막 Gradle 모델 동기화가 의존성 추가 전임을 확인하여 실행 중인 Studio의 Sync Project with Gradle Files 실행. 코드 재수정 없이 IDE 모델을 갱신했다. 앱·에뮬레이터 실행은 하지 않았다.

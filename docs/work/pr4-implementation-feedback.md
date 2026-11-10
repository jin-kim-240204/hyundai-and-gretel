# PR4 구현 리뷰 반영

요청: 최신 전체 피드백 검토, 필요한 코드/검증 반영 및 한국어 요체 답글 게시. 커밋·푸시는 요청 범위에 포함하지 않음. 기준 PR head b9d1469, 별도 worktree /private/tmp/hyundai-road-notes-sdd. 기존 스펙 스레드8개는 이미 해결되어 중복 답글하지 않음.

적용 스킬: pleos-connect, ponytail, pleos-design(기존 임시 화면 추출), pleos-verify. android-compose-architecture 파일은 없음.

규칙 ENV-02: ComponentActivity/Compose 유지. VEH-03 및 DOC-1ba06876919b#수명: initialize 후 동일 listener 해제와 release는 별개, 재시작은 새 인스턴스. DOC-9844e9eaad3c#콜백: nullable/error와 동일 listener 해제, 세션 유효성 검사 유지. PRIV-01: 기존 debug 합성좌표 로그 한정 유지, 추가 시작 실패 로그는 예외 종류만 기록. DES-04 및 DOC-e12e49cc3efc#레이아웃, DOC-189d894e8124#윈도: 기존 화면 레이아웃·버튼을 별도 LocationView로 이동, 권한/서비스 수명은 Activity/ViewModel/Service에 유지. 신규 디자인/정량 적합성 주장은 하지 않음.

공식 원문 재확인(2026-11-10): https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release (SDK 수명), https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access (업데이트 수신/중지), https://developer.android.com/kotlin/coroutines/coroutines-best-practices (취소 예외 재전달). Main 전달 보장을 새로 주장하지 않으며 세션 직렬화/취소 처리는 유지.

수정 범위: MainActivity 표시를 LocationView로 추출. LocationViewModel의 서비스 상태와 시작 명령 전달 실패를 분리(별도 Boolean, 새 시작 때 초기화), debug 로그 추가. LocalBinder는 읽기 전용 StateFlow만 공개하고 tracking/stopTracking 비공개. LocationTracking은 좌표 유효성 predicate 및 early return으로 정리, 세션 식별/오류 처리 유지. 호출처는 ViewModel 및 TrackingLifecycleTest 전부 검색·수정.

검증: 기존 세션 단위5개 회귀, 재생성 후 실제 Activity ViewModel 상태 및 추가 REGISTERED 없음 확인, 시작명령 실패→바인딩 후 오류 유지 회귀. debug/release·lint·계측 컴파일·harness·IDE빌드 및 가능한 기기 검증. SDK 정리 실패와 실제 권한 대기 중 재생성은 별도 증거로 구분하며 단위 FakeSource PASS로 확대하지 않음.

## 이번 실행 결과

PASS — 단위 LocationTrackingTest 5개(failures0/errors0), debug/release 빌드, 계측 APK 컴파일, lintDebug(오류0, 기존경고40), harness --build, git diff --check, 수정 worktree의 Studio build_project.

PASS — Pleos Connect2.1.0/API34/user10 계측3개(6.309초): TrackingLifecycleTest2개(실제 Activity ViewModel 상태 재연결 및 REGISTERED 추가 없음, 시작 명령 SecurityException 뒤 실제 서비스 상태 수집에도 별도 오류 유지), PleosLocationSourceTest1개(실제 SDK1.1.3 사용, ContextWrapper에서 unregister 권한 검사를 거절시켜 예외를 유도한 뒤 release에 따른 unbind 호출 및 반복 stop의 추가 해제 없음 확인). OS 권한 철회·실제 원격 서비스 장애 전체를 재현한 것은 아님. 새 SDK 래퍼/의존성 없음.

PASS(기본 화면 한정) — 분리한 View의 중지 화면을 실제 캡처·검토. 제목/상태/시작·중지 버튼 잘림과 시스템 영역 침범 없음. evidence/pr4-implementation-feedback/controls.png. 전체 폼팩터·정량 대비는 NOT_TESTED.

NOT_TESTED — 권한 요청 팝업 대기 중 재생성→늦은 허용→등록 없음→새 시작 후 등록. 접근성 기반 회귀를 시도했으나 Pleos 다중 창에서 대상 UI 조회가 타임아웃. 모든 접근성 창 조회로 수정해도 타임아웃되어 불안정한 새 테스트는 삭제. 제품 결함 여부를 판정할 증거는 없으며 기존 권한 사전허용 수명 테스트를 이 경로의 PASS로 확대하지 않음. 실제 권한 팝업이 열린 동안 재생성과 허용 결과를 관측하는 별도 기기 검증이 필요.

실패 수정 — 최초 화면 추출 들여쓰기 lint1건은 수정 후 PASS. 시작명령 실패 회귀의 가짜 Application Context 미연결은 attachBaseContext 추가 후 PASS. 스크린샷 ADB 최초 sandbox 거절은 권한 승격 재실행 후 성공. IDE 최초 호출은 worktree 미열림으로 거절되어 해당 프로젝트를 연 뒤 PASS.

기존 구현 기록 보정 — 기존 TrackingLifecycleTest의 tracking 인스턴스 동일성만으로 새 화면 관찰을 입증하지 못함. 기존 FakeSource.stop 예외는 상위 상태 유지 테스트이며 SDK unregister/release를 실행한 증거가 아님. 이번 계측3개의 범위와 구분. 취소 단위 테스트도 동기 start 예외 재전달만 확인하며 준비 중 사용자 취소 전체는 미검증.

리뷰 답글은 각 미해결 구현 스레드에 이번 로컬 반영과 검증 범위를 설명. 이전 스펙8개에는 중복 게시하지 않음. 커밋/푸시/스레드 해결 없음. 원격 head는 b9d1469 유지. 전체 AC 및 기능 검증 완료 판정 아님.

## 답글 게시 결과

새 미해결 구현 스레드12개에 요체 답글 게시 완료(2026-11-10). 첫 답글 https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4525690003, 마지막 답글 https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4525693830. 모두 로컬 반영/커밋·푸시 전이라고 명시했고, AC-01은 미검증으로 답변. 스레드 해결 처리 없음. 기존 해결된 스펙8개는 중복 게시하지 않음. GitHub 커넥터는 저장소404로 접근 불가하여 인증된 gh api를 사용함.

보고서 최초 임의 AC ID는 harness가 거절하여 등록된 VEH-01/DOC-402de45b9f3d#권한으로 수정. report 검사10개는 형식/정적 PASS, 남은 NOT_TESTED 때문에 exit2(전체검증 완료 아님).

최종 확인: 답글12개를 GET 재조회해 각 원본 댓글에 연결된 것을 확인했다. 마지막 IDE 재호출 시 worktree 프로젝트가 더 이상 열려 있지 않아 거절됐다. 앞서 운영 코드 수정 후 IDE 빌드 PASS는 유효하나, 이후 추가 테스트 파일까지 포함한 최종 IDE 재빌드는 NOT_TESTED이며 CLI 계측 컴파일/기기3개/harness 빌드로 확인했다. 이번에 시작한 에뮬레이터는 테스트 후 종료했다. 설치 APK와 허용 권한은 기존 AVD에 남아 있다.

후속 요청: 사용자가 리뷰 반영 변경의 커밋·푸시를 승인했다. 이전 검사 이후 운영 코드 변경 없음. 기존 단위5개·계측3개 및 빌드 결과를 재사용하고 diff/보고서를 재확인한다. 권한 대기 중 재생성 등 NOT_TESTED는 유지한다.

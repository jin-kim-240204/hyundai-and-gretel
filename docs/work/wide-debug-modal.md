# PR 10 Debug 중앙 모달

기준은 `c319474`다. 현재 worktree의 debug 패널과 리소스, 기존 정적 레이아웃 검사만 수정한다. 실제 렌더 및 blur 동작은 기기 없이 검증할 수 없다.

- DES-01·02 · DOC-9c1fe6a8475c#대비·응답·문자 · DOC-728dd6fe8092#타이포그래피. 대비율 4.5:1, 응답 0.25초 초과 시 진행 상태, 언어별 문자 수와 문단 3줄, 실물 글자 높이를 검토한다. 기존 `PolicyText`와 규제 상태를 유지한다. [Pleos 디자인 원칙](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)
- DES-03·04 · DOC-94caa75424dd#색상 · DOC-e12e49cc3efc#앱영역 · DOC-189d894e8124#창크기. 테마 표면을 사용하고 시스템 영역을 제외한 Dialog 제약에서 폭·높이 상한을 계산한다. 작은 창에서 헤더와 별도로 본문을 스크롤한다. [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)
- DES-06·08 · DOC-d771e574367a#버튼상태 · DOC-3a266a228ea1#아이콘버튼 · DOC-904663536d42#닫기가있는팝업 · DOC-8233dc2b1ed3#그림자. 기존 56dp 버튼 높이와 활성 조건을 유지하고, 제목과 접근성 이름이 있는 닫기 X를 둔다. 사용자가 연 모달에만 그림자와 배경 처리를 적용한다. 개발 패널은 Compose 그림자 8dp를 사용한다. 아이콘 버튼과 팝업 원문은 온라인 열람 실패로 저장소 스냅샷을 참고했다. [Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button)
- DOC-3790286d55ee#규제전환. 기존 설정·제보 제한, 콘텐츠 깊이·수량, 문자 제한 및 reset 확인을 JVM과 코드로 확인한다. [주행 안전 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)
- Android 12 이상에서 창 뒤 blur를 사용하고, 기기 미지원·시스템 비활성화·이전 API에는 더 진한 dim을 적용한다. 열린 창의 blur 활성 상태 변화를 구독하고 닫을 때 구독을 해제한다. [Android 창 블러](https://source.android.com/docs/core/display/window-blurs)

## 구현 및 비기기 검증

- Dialog 내부 창 제약의 90%와 최대 840dp를 폭 상한으로 쓰고 높이는 90%로 제한한다. 56dp 닫기 X는 스크롤 밖에 고정하고, 본문은 가운데 정렬하며 역할 버튼은 좁은 폭에서 줄을 바꾼다. X·뒤로가기·외부 탭은 같은 닫기 콜백을 사용한다.
- 기존 56dp FAB 높이와 하단 8dp 여백을 유지해 본문 스크롤의 64dp 확보를 유지했다. Debug 글자가 들어갈 가로 여백만 추가했다. release 패딩은 0dp이고 reset은 기존 명시적 확인 버튼을 거친다.
- 창 블러 반경은 24dp를 픽셀로 환산한다. 초기 dim과 비활성 dim은 0.60, 블러 활성 시 dim은 0.32다. Android WindowManager의 활성 상태 콜백은 등록 시 현재 상태도 전달한다. Compose Dialog 1.10.4 로컬 소스에서 중앙 배치, back/outside dismiss 및 창 제약 전달을 확인했다. [WindowManager API](https://developer.android.com/reference/android/view/WindowManager)
- PASS · `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease`. JVM 142개, 실패·오류·스킵 0. lint 오류 0, 경고 debug 44개·release 42개. 변경한 소스와 리소스에 lint 경고가 없다. 기본 샌드박스에서 Gradle 래퍼 캐시 잠금 접근이 거부되어 승인된 로컬 접근으로 실행했다.
- PASS · `python3 scripts/test_demo_panel_layout.py` 4개, `python3 scripts/verify_harness.py`, `python3 scripts/verify_harness.py --build`, `git diff --check`. 정적 검사는 FAB 여유, 모달 상한과 닫기 배치, blur API 가드와 dim 전환, debug 리소스 계약을 확인한다. FAB 색 대비 계산은 11.01:1이다.
- PASS · APK의 DEX 및 `aapt2 dump resources`에서 `DemoDialog`, `observeDemoBlur`, `debug_tools_badge`, `debug_close`는 debug에만 있다.
- NOT_TESTED · 화면·그림자·스크림 실렌더, 배경 blur 및 시스템 비활성화 시 dim 전환, 작은 창 스크롤·터치·포커스·TalkBack, 주행 중 규제 변경, 실제 텍스트 높이와 응답 시간. 기존 호스트 렌더 테스트 작업이나 의존성이 없었고 신규 환경은 설치하지 않았다. Studio는 원본 checkout만 열려 있어 현재 worktree에 대한 IDE 빌드 요청은 실행되지 않았다. CLI 빌드를 근거로 사용했다. 에뮬레이터·ADB·앱 실행은 수행하지 않았다.
- JSON 보고서 스키마 검사는 성공했고 종료코드 2는 실제 화면과 기기에서 남은 미검증 항목을 반영한다.
- N/A · 모달에 텍스트 입력이 없어 IME 변경 범위가 없다. 출시 심사는 작업 범위 밖이다.

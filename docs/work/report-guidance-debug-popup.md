# 제보형 수신 안내와 debug 제어 팝업

- 범위는 Gleo 설명과 계약 검사, debug DemoPanel 및 리소스다. release no-op, 안내문 저장 및 일반 TTS 대체 문구, 주행 규제 정책의 기존 경로를 사용한다.
- INT-01 · DOC-24357cf5e59a#수신안내설명 (§2.1, §4). 영어 1~2문장으로 필드를 설명하고 구체 예시는 생략한다. 제보 출처, 미확정, 부정·불확실성, 근거 없는 공간·시간 정보 금지를 계약 검사로 확인한다. [공식 원문](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)
- DES-01~04 · DOC-9c1fe6a8475c#가독성 · DOC-94caa75424dd#색상 · DOC-728dd6fe8092#서체 · DOC-e12e49cc3efc#레이아웃 · DOC-189d894e8124#윈도. 앱 영역 우하단에 작은 debug 버튼을 두고 모달의 폭과 높이를 창에 맞춘다. 고정 버튼 색의 대비는 계산하며 실제 크기와 가림은 렌더 검증 항목이다. [디자인 가이드](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)
- DES-06 · DOC-d771e574367a#레이블과상태. 접근 가능한 레이블과 최소 48dp 진입 버튼을 두고 기존 제어 버튼의 56dp 최소 높이와 활성 조건을 유지한다. [Basic Button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button)
- DES-08 · DOC-904663536d42#팝업과닫기. 사용자가 열 때만 제목과 제어를 중앙 모달로 표시한다. 닫기 버튼, back, 바깥 탭을 동일한 상태 변경으로 연결한다. 온라인 팝업 페이지 열람 실패로 저장소 공식 원문 스냅샷을 참고했다. [System Popup](https://document.pleos.ai/docs/connect/guide/docs-design/components/popups/system-popup)
- DOC-9faba42791f0#규제갱신. 기존 실시간 규제 상태, 설정·제보 활성화 판단, 콘텐츠 예산 및 중지/닫기 경로를 유지한다. JVM의 기존 debug 패널 콘텐츠 제한 테스트를 실행한다. [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)

## 구현 및 검증

- DBG 버튼은 우하단 8dp 여백에 48dp 최소 크기로 배치했다. 호박색 배경과 고정폭 레이블을 적용했다. 대비 계산은 11.01:1이다. 팝업은 가용 폭의 90%와 480dp 중 작은 값을 사용하고 가용 높이의 90%를 상한으로 둔다. 버튼 쌍은 폭이 부족하면 다음 줄로 이동하고 팝업 내용은 세로로 스크롤한다.
- 클린 코드 리뷰에서 진입점과 release no-op, 팝업 열림 상태와 닫기 콜백, 규제 전파 및 기존 콘텐츠 예산을 확인했다.
- PASS. JDK 21과 SDK 35에서 `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease` 통과. JVM 142개, 실패·오류·스킵 0. lint 오류 0이며 debug 44, release 42개 경고가 있다. 외부 Gradle 캐시의 쓰기 제한은 로컬 캐시 접근 승인으로 해소했다. 저장소에 release 전용 JVM 테스트 작업은 없다.
- PASS. APK DEX와 리소스 테이블에서 `DemoPanelContents` 및 `debug_tools_badge`가 debug에만 포함됨을 확인했다.
- PASS. `python3 scripts/test_receiver_guidance_schema.py`, `python3 scripts/verify_harness.py`, `python3 scripts/verify_harness.py --build`, `git diff --check` 통과. JSON 보고서 종료코드 2는 명시한 미검증 항목을 반영한다.
- NOT_TESTED. 실제 Gleo 생성·등록, 앱 렌더, 창별 가림·스크롤·접근성, 실제 back/outside 입력과 주행 중 전환. Studio는 원본 worktree만 열려 있어 현재 worktree의 CLI 빌드로 확인했다. 에뮬레이터·ADB·기기·앱 실행은 수행하지 않았다.

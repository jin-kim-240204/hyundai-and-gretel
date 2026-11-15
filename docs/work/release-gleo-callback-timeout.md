# release Gleo 응답 대기 진단

2026-11-15 에뮬레이터 emulator-5554 읽기 전용 확인. 사용자가 발화 후 듣기 지속 및 '앱에서 처리가 조금 늦어지고 있어' 안내를 보고함.

- PASS(관측): logcat 16:46:28.847 STT 결과, 16:46:31.143 AppActionExecution CREATE_ROAD_NOTE 및 onUpdateAppActionData, 16:46:31.145 AppAction timeout 시작, 16:46:51.218 actionCallback timeout. 약20초 앱 응답 대기 후 안내. 발화 종료 자체가 계속 실패한 증거와 구분한다.
- 코드: app/build.gradle.kts 기본 gleoReceiverExported=false(debug만 true). GleoActionReceiver.kt sendCallback은 DEBUG일 때만 ai.umos.ambientai, release는 자기 패키지. 패널 공통화에서 이 경로는 변경하지 않았다.
- 판정: 이번 관측은 Gleo가 발화를 제보로 해석했으나 앱 응답을 받지 못한 timeout. release 수신/응답 설정은 왕복을 막는 확정 구성 문제. 실제 요청이 receiver에 들어갔는지는 release probe가 꺼져 있어 이번 로그만으로 확정하지 않는다.
- INT-01 FAIL: release Gleo 왕복 미완료. PRIV-01 외부 수신 노출·발신자 검증·Callback 목적지를 공식 계약으로 확인 후 수정 필요. 원시 로그에는 발화·기기 식별자가 있어 저장소에 복사하지 않음.
- NOT_TESTED: 수정·재빌드·재서명·왕복 재현. 이번에는 코드 변경이나 앱 삭제/설치 없이 진단만 수행.

## 수정 계획 및 공식 계약

사용자가 수정 후 main 직접 커밋·푸시 요청. 수정 범위는 app/build.gradle.kts의 receiver 공개 기본값과 GleoActionReceiver.sendCallback의 패키지 목적지다. 기존 파서 8192 bytes/문자열·enum 검사, 저장 전 시연 준비/권한 상태 가드와 5초 처리 제한, release 로그 비활성은 유지한다.

- INT-01 / DOC-f1d260bfb503#service요청: 공식 예시 §1.1의 component=service는 ACTION_PARAM JSON을 담은 패키지 지정 broadcast로 전달한다. 따라서 외부 Gleo가 receiver를 호출할 수 있어야 한다.
- INT-01 / DOC-f1d260bfb503#응답브로드캐스트: §2의 APP_ACTION_RESPONSE action 및 packageName/response extras 유지. 응답은 로컬에서 실제 확인된 Gleo 패키지 ai.umos.ambientai만 지정한다. 공식 예시는 목적지 제한이 없지만 프로젝트는 최소노출을 위해 지정 유지.
- DOC-c6fc94252d66#응답필드: functionName/status/영어 완결문 message 및 data JSON 배열 문자열 계약은 기존 serializer 유지. 문서 §2.2는 7초 timeout, 이번 에뮬레이터는 약20초 관측. 앱은 기존 5초 제한을 유지한다.
- PRIV-01 / DOC-27ac8605961f#외부컴포넌트: 외부 호출이 필요한 receiver만 공개. 입력을 신뢰하지 않고 파서/시연 가드 유지. 문서 예시에서 확인되지 않은 signature 권한을 임의 추가하지 않는다. 이번 변경은 Gleo 발신자 인증을 추가하지 않으므로 다른 로컬 앱도 유효한 입력을 보낼 수 있으며 응답은 고정 Gleo 패키지만 받는다.
- 공식 원문 금일 재확인: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples , https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback . 이 두 페이지는 기존 스냅샷과 관련 요구사항 차이 없음.
- 검사 계획: debug/release build·lint, 기존 JVM/Python 테스트, harness, IDE build, 실제 release 병합 Manifest exported=true 및 컴파일 결과 callback 목적지 확인. 기존 서명 APK는 수정 전이므로 재서명·실제 음성 왕복은 별도로 기록한다.

## 수정 검증

- PASS: `python3 scripts/verify_harness.py --build` (debug assemble/lint), `./gradlew assembleRelease lintRelease testDebugUnitTest` (순차 재검사), Python 테스트11개, Studio build_project 성공(기존 deprecated 경고 존재).
- PASS: debug/release 병합 Manifest의 GleoActionReceiver.exported=true. javap로 두 variant의 컴파일된 sendCallback에 ai.umos.ambientai/APP_ACTION_RESPONSE/Intent.setPackage 호출 확인.
- 최초 실패: Gradle 검사와 IDE/다른 Gradle 빌드의 동시 실행 중 Hilt 컴파일 실패. 동시 빌드 종료 후 순차 실행으로 변경하여 통과. 동일 환경 동시 실행을 반복하지 않음.
- NOT_TESTED: 수정된 release의 새 서명·설치·음성 Gleo 왕복. 기존 설치 release와 제출 APK는 수정 전 파일이므로 이번 검사로 시간초과 해결 완료를 주장하지 않는다.

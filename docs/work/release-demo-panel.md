# 공모전 release 시연 패널

요청: A/B 사용자 전환에 필요한 기존 Debug 패널을 공모전 release에도 포함한다. 기존 release는 빈 DemoPanel 및 비활성 DemoRuntime 때문에 패널과 제보가 동작하지 않았다.

## 범위 및 검증 계획

- 기존 demo 코드·리소스를 main으로 이동하고 빈 release 구현 제거. 런타임 테스트도 공통 test로 이동하여 release 테스트에 포함한다.
- 시나리오 초기화는 BuildConfig.DEBUG 대신 demo.enabled로 판단한다. release의 시연 불가 안내도 공통 시연 기능에 맞춘다.
- 에뮬레이터 mock 입력에 필요한 권한과 127.0.0.1만 HTTP 허용하는 네트워크 정책을 공통으로 이동. 외부 HTTP 전체 허용은 하지 않는다. 에뮬레이터 검사·mock 앱 권한·주행 제한·데이터 가드는 유지한다.
- Gleo 외부 수신/Callback 계약 및 로그의 DEBUG 가드는 이번 요청에서 변경하지 않는다. release 외부 Gleo 경로는 별도 미해결 사항이다.
- ENV-02: 기존 Activity/Compose 구조 유지. PRIV-01 / DOC-27ac8605961f#최소권한: 기존 시연 입력에 필요한 권한만 공통 이동, 루프백 HTTP 예외만 유지. DES-* 새 시각 디자인은 N/A이며 기존 UI를 그대로 공유한다.
- 검사: debug/release assemble·lint·JVM 테스트, Python 검사, harness, release 병합 Manifest 및 DEX에서 demo 구현 포함 확인.
- 기존 업로드 APK는 이번 소스 변경 이전 파일이다. 사용자 키를 임의 사용하거나 제출 APK를 자동 덮어쓰지 않는다. 변경된 release 재서명·설치 검증은 별도 필요.

## 검사 결과

- PASS: `./gradlew assembleDebug assembleRelease lintDebug lintRelease testDebugUnitTest`, JVM 146개 실패/오류 0. Studio build_project isSuccess=true, problems=[]; Python 테스트 11개; harness; diff --check.
- PASS: release unsigned APK의 DEX에서 DemoPanelKt/DemoRuntime/DemoViewModel/AndroidDemoInputSource 존재 확인. 병합 Manifest에서 debuggable=false 및 루프백 네트워크 정책 확인.
- 실패 수정: 기존 Python 레이아웃 검사는 이동 전 debug 경로·예전 12dp/흰 backdrop 기대값을 참조하여 현 공통 경로와 기존 48dp/검정 backdrop 값으로 수정했다. 공통 리소스의 다른 텍스트는 debug 전용 문구 검사에서 제외.
- NOT_TESTED: release 전용 JVM 테스트는 현재 AGP 설정에서 testReleaseUnitTest 태스크가 없어 실행하지 못했다. 공통 테스트를 testDebugUnitTest로 실행했다. 새 release 서명·설치·모달/A-B 전환·실기기 UI 검토 미실행.
- 기존 docs/RELEASE-READINESS.md와 branch-security-apk-check.md는 이전 산출물의 판정이다. 이번 변경으로 패널과 시연 저장 가드의 비활성 원인은 수정했지만 외부 Gleo 경로 및 실제 release 동작 검증은 남아 있어 전체 제출 준비 완료를 주장하지 않는다.

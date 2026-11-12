# 주행 중 간결한 상태 UI

> 현재성: 사용자 후속 요청으로 상태 감시·간결 UI·관련 테스트를 원복하고 DO 선언만 유지했다. 현재 상태는 [원복 기록](revert-driving-ui.md)을 따른다. 아래 결과는 당시 구현의 과거 증거다.

2026-11-08. 사용자 요청: 주행 중 앱 사용 불가 화면 대신 UI 표시. pleos-connect/pleos-design/ponytail/pleos-verify 적용. 현재 MainActivity·GleoProbe·Manifest와 기존 주행 규제 기록을 확인했다.

범위: MainActivity의 CarUxRestrictions 수신·수명주기, 주행/미확인 상태의 간결한 상태 UI, Activity DO 선언과 android.car 플랫폼 라이브러리, 관련 테스트/문서. 저장·음성 호출·Callback/로그 기능은 변경하지 않는다. 사용자 기존 Manifest 권한4줄은 커밋에서 제외한다.

## 원문·규칙

SAF-01 / ENV-02 / DES-01 적용. 2026-11-08 공식 웹 원문 재확인:
- DOC-6386c5462132#DO미선언: [지역 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy), DO 미선언은 주차 허용/주행 차단. 지역/차종/플랫폼별 차이가 있음.
- DOC-9faba42791f0#상태연동: [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), CarUxRestrictionsManager 초기 상태 및 listener로 isRequiresDistractionOptimization/activeRestrictions 반영. 속도/기어를 임의 기준으로 쓰지 않음. listener 해제/Car disconnect 대칭 관리.
- DOC-3790286d55ee#간결UI: [주행 안전](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline), 짧은 인지/최소 조작. 주행/미확인에서는 긴 JSON·스크롤 기록·동의/삭제/새로고침 버튼을 표시하지 않고 세 줄 상태만 표시한다.
- DOC-12839eb0a523#카테고리: [카테고리](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category), 목적과 지역에 맞는 제한적 기능만 허용. 도로 제보는 Mobility Services 후보이나 실제 마켓 카테고리 승인/양산 허용 증거가 아님.
- [AOSP DO metadata](https://source.android.com/docs/automotive/driver_distraction/guidelines) 및 android.car API: Activity의 distractionOptimized=true는 최적화 준수 선언이며 규정 준수 자체를 시스템이 보장하지 않음. 현재 설치 SDK android.car.jar 심볼과 공식 API 페이지를 대조한다.

Google Play의 일반 AAOS 앱 문서는 DO 선언을 Car App Library Activity로 제한하는 반면 Pleos 문서는 별도 일반 Activity/DO·직접 UX 제한 경로를 설명한다. 이 프로젝트는 사용자가 선택한 Pleos Connect 일반 ComponentActivity 구조를 유지하며 Google Play 출시 적합성을 주장하지 않는다.

프로젝트 선택: 제한 여부/차량 서비스 연결을 확인할 수 없으면 제한 UI를 기본값으로 표시한다. 주행 UI는 제보 처리 결과를 짧게 보여주는 수동 정보 화면이며 raw JSON을 노출하지 않는다. PC logcat 수집은 계속 가능하다.

검증: 순수 상태/문구 분기 단위 검사, assembleDebug/lintDebug·harness·Studio 빌드. 사용자가 직접 기능 테스트 중이므로 에뮬레이터 속도/기어/제한 상태를 임의 조작하지 않는다. 현재 주행 상태 읽기·업데이트 설치 후 기본 화면 증거를 확인하고 지역/전체 전이/실차는 NOT_TESTED로 남긴다. DO 선언을 심사 승인으로 보고하지 않는다.

실환경 차이: 최초 required android.car 선언 빌드가 INSTALL_FAILED_MISSING_SHARED_LIBRARY로 설치 실패했다. pm list libraries에는 android.car가 없지만 BOOTCLASSPATH의 /apex/com.android.car.framework/javalib/android.car-module.jar에 플랫폼 API가 제공된다. 필수 uses-library를 제거하고 Gradle useLibrary("android.car", false)로 컴파일용/선택 라이브러리 참조를 사용한다. 숨겨진 Pleos internal 클래스나 시스템 설정 변경은 사용하지 않는다.

## 결과

단위40개/실패0, assembleDebug/lintDebug, Studio MCP build_project, harness, diff 공백 검사 PASS. Pleos emulator-5554 user10 현재 CarUxRestrictions=requiresDO true/active80에서 설치/실행 성공. GleoProbe UX 로그 compact=true/activeRestrictions=80을 확인했고 UIAutomator에서 앱의 3개 텍스트(음성 도로 제보/발화 안내/상세 기록은 정차 후)가 표시되며 스크롤·버튼·rawJSON이 없는 것을 확인했다. 시스템 주행불가 화면은 표시되지 않았다. 차량 속도/기어/제한을 변경하지 않았다.

라이프사이클 stop에서 listener 해제/disconnect, 불명/미지원에서 간결UI 유지, rawJSON이 상태문구에 포함되지 않는 분기 단위검사 PASS. 정차→주행→정차 전이/연결단절 주입·야간/다른 폼팩터·실차/지역 카테고리 심사는 NOT_TESTED. DO는 준수 선언이며 승인 완료로 보고하지 않는다.

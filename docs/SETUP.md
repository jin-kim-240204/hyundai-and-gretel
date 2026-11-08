# 프로젝트 구성

과거 구성 빌드 검증: 2026-11-02 MCP build_project 성공, Gradle assembleDebug 및 lintDebug 성공. 병합 Manifest에서 Automotive 필수 선언, 일반 MainActivity 및 service 부재를 확인했다. 이 결과는 당시 구성의 기록이며 현재 변경 후 빌드·실행을 재검증한 결과가 아니다. 현재 소스/설정은 2026-11-08 대조했다. 에뮬레이터 관련 과거 점검은 docs/work에 있으며 현재 소스 빌드의 실행 완료 증거로 재사용하지 않는다.

사용자 선택: Car → No Activity → 일반 Empty Activity 추가와 같은 최종 구성.

현재 프로젝트는 MCP로 파일을 구성했다. Android Studio 마법사에서 해당 순서를 완주해 생성했다고 주장하지 않는다. 이전 Empty Activity 프로젝트의 Gradle wrapper, 기본 아이콘 및 Compose theme를 재사용했다.

- Android Automotive OS에 설치하는 application 모듈.
- android.hardware.type.automotive required=true, touchscreen required=false.
- 일반 ComponentActivity가 Compose 화면을 표시한다.
- Car App Library 의존성, CarAppService, Session/Screen 구조는 사용하지 않는다.
- Pleos Vehicle SDK 2.3.2와 공식 Maven 저장소를 등록했다.
- minSdk 28, compileSdk 35, targetSdk 34. targetSdk는 현재 설치된 Pleos API 34 에뮬레이터에 맞춘 개발 설정이며 출시 시 최신 심사 기준을 다시 확인해야 한다.
- Manifest에는 pleos.car.permission.CAR_DOORS, CONTROL_CAR_DOORS, CAR_WINDOWS, CONTROL_CAR_WINDOWS가 선언되어 있다. MainActivity에는 Vehicle 초기화·조회/제어 호출이 없으며 권한 선언만으로 차량 연결·승인이 완료된 것은 아니다. 실제 기능 구현 시 필요한 권한·지원 여부를 재검토한다. MainActivity는 distractionOptimized를 선언하고 CarUxRestrictions 제한/미확인 상태에서는 간결한 음성 제보 상태만 표시한다. 전체 JSON/목록/버튼은 제한이 없는 상태에서만 표시한다.
- allowBackup=false 및 MainActivity의 windowSoftInputMode=adjustResize가 설정되어 있다. 초안 보안/키보드 동작의 실제 검증은 별도다.
- 소스 package/namespace/applicationId는 com.jinkim.hyundaiAndGretel로 정합했다. 이전 com.jinnayeong.wakeup 설치본과는 별도 앱이며 데이터 자동 이전은 없다. versionName=1.0, versionCode=1.
- Gleo Schema·입력 검증·Callback 생성·내부 전용 Receiver와 Android Keystore/AES-GCM 초안 저장·동의/목록/삭제 UI를 구현했다. debug는 무저장 Gleo 테스트 관측 화면을 표시하고 외부 입력을 받으며 Callback 대상은 에뮬레이터에 설치된 ai.umos.ambientai다. release는 외부 수신 비활성이다. 실제 음성/발신자 신뢰 계약은 미검증이다. 위치·서버·게시/공유는 미구현이다. 도로 제보의 현재 설계는 [Gleo 인터페이스](plans/gleo-interface-design.md)와 [구현 계획](plans/implementation-plan.md)을 따른다.
- 수면 타이머·알람·좌석 제어는 아직 구현하지 않았다.

공식 시작 가이드: https://document.pleos.ai/docs/pleos-only/getting-started
공식 문구의 Car/Automotive → Empty Activity와 현재 Android Studio의 템플릿 선택지가 달라, 위 구성을 사용자가 선택했다.

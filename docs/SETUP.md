# 프로젝트 구성

검증: 2026-11-02 MCP build_project 성공, Gradle assembleDebug 및 lintDebug 성공. 병합 Manifest에서 Automotive 필수 선언, 일반 MainActivity 및 service 부재를 확인했다. 에뮬레이터 실행은 아직 확인하지 않았다.

사용자 선택: Car → No Activity → 일반 Empty Activity 추가와 같은 최종 구성.

현재 프로젝트는 MCP로 파일을 구성했다. Android Studio 마법사에서 해당 순서를 완주해 생성했다고 주장하지 않는다. 이전 Empty Activity 프로젝트의 Gradle wrapper, 기본 아이콘 및 Compose theme를 재사용했다.

- Android Automotive OS에 설치하는 application 모듈.
- android.hardware.type.automotive required=true, touchscreen required=false.
- 일반 ComponentActivity가 Compose 화면을 표시한다.
- Car App Library 의존성, CarAppService, Session/Screen 구조는 사용하지 않는다.
- Pleos Vehicle SDK 2.3.2와 공식 Maven 저장소를 등록했다.
- minSdk 28, compileSdk 35, targetSdk 34. targetSdk는 현재 설치된 Pleos API 34 에뮬레이터에 맞춘 개발 설정이며 출시 시 최신 심사 기준을 다시 확인해야 한다.
- 차량 권한과 distractionOptimized 선언은 추가하지 않았다. 실제 차량 기능 구현 시 지원 여부 및 별도 권한을 확인한다.
- 수면 타이머·알람·좌석 제어는 아직 구현하지 않았다.

공식 시작 가이드: https://document.pleos.ai/docs/pleos-only/getting-started
공식 문구의 Car/Automotive → Empty Activity와 현재 Android Studio의 템플릿 선택지가 달라, 위 구성을 사용자가 선택했다.

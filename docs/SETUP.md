# 프로젝트 구성

사용자가 선택한 Car → No Activity → 일반 Empty Activity 추가에 해당하는 구성입니다. 단일 Automotive application 모듈에서 일반 `ComponentActivity`와 Compose를 사용합니다.

- Gradle Wrapper 9.6.0, JDK 21로 비기기 검사를 수행했습니다. Android SDK Platform 35가 필요하며 `ANDROID_HOME` 또는 로컬 `local.properties`의 `sdk.dir`로 지정합니다.
- minSdk 28, compileSdk 35, targetSdk 34. applicationId는 `com.jinkim.hyundaiAndGretel`, versionName은 1.0, versionCode는 1입니다.
- Pleos 공식 Maven 저장소와 Vehicle 2.3.2, FusedLocation 1.1.3, TextToSpeech 3.3.1을 사용합니다. 의존성 다운로드, SDK 사용 승인, 기기에서 권한이 부여되는지는 각각 확인합니다.
- `MainActivity`가 Compose 화면을 열고, `RoadNoteService`가 위치 수집·제보 저장·접근 수신과 TTS 수명을 관리합니다. Room은 시연별 쪽지와 수신 이력을, H3는 주변 후보의 셀 범위를 관리합니다. R1·A/B 합성 경로와 활성 시연이 저장·수신의 전제입니다.
- debug는 외부 Gleo 입력과 시연 도구를 제공합니다. release의 새 설치 제한은 [제출 준비 점검표](RELEASE-READINESS.md)를 참고하세요.
- 기기 준비와 재현 절차는 [시연 런북](PLEOS-DEMO.md), 비기기 검사 명령은 [README](../README.md)에 있습니다.

[공식 Pleos Connect 시작 가이드](https://document.pleos.ai/docs/pleos-only/getting-started)

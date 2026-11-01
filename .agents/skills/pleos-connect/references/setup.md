# 환경 및 출처

먼저 sources/eb1428718f0c.md, 25c9fa8b4f9f.md, 81d445e8e7a1.md를 읽는다. 에뮬레이터 변경 시 447cb4d9b001.md, 532942c7b49f.md, 3240d46da07b.md도 읽는다.

- 공식 quickstart는 Car/Automotive → Empty Activity, Kotlin, minSdk 28 이상, API 34 Pleos image, Cold boot 및 RAM 4GB를 안내한다.
- 현재 구조는 사용자가 선택한 Car No Activity + 일반 Activity와 같은 최종 구성이다. 마법사를 완주했다고 주장하지 않는다.
- 시작 문서 Vehicle 2.3.1 예시와 SDK import 페이지 2.3.2가 다르다. 현재는 2.3.2가 실제 빌드된 설정. 업데이트 시 릴리즈 노트와 실제 artifact를 확인한다.
- Maven 저장소와 SDK 사용 신청/승인은 별개다. Manifest 권한만으로 SDK 사용 승인이 완료되었다고 말하지 않는다.
- AVD 존재/설정과 앱 런타임 성공은 별도 판정한다. APK 생성만으로 설치·Vehicle 연결 성공을 주장하지 않는다.
- targetSdk 34는 현재 개발 설정이며 출시 적합성 확인을 대신하지 않는다.

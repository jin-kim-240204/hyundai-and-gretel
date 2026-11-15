# 공모전 APK 업로드

- 요청: 기존 복제 APK를 `jinkim_hyundai-and-gretel_release.apk`로 변경하고 커밋·푸시.
- 범위: APK, 해당 파일만의 ignore 예외, apk/README 및 이 기록. 새 빌드나 앱 코드 수정 없음.
- 규칙: REL-01 중 파일명·경로·서명 산출물 확인만 적용. 전체 제출/실행 적합 판정은 제외.
- PASS: 원본과 SHA-256 일치, `verify_submission_apk.sh`로 서명/기존에 확인한 인증서 지문/패키지명/비디버그 상태 검증. `git diff --check`, harness 검사.
- NOT_TESTED: 새 APK 설치·기동·핵심 기능 및 바이너리와 소스 커밋의 대응 관계. 사용자가 이미 생성한 산출물을 그대로 업로드한다.

## 시연 패널 포함 APK 교체 (2026-11-15)

- 사용자가 2ca9f04 반영 후 새로 생성한 app/release/app-release.apk로 제출 파일을 교체. build/outputs의 파일과도 해시 동일.
- PASS: 기존과 같은 인증서 SHA-256, APK 서명, 패키지명, 비디버그 상태 검사. DEX에서 DemoPanelKt/DemoViewModel/AndroidDemoInputSource 존재 확인. 복사본과 원본 바이트 일치.
- 파일: 43663257 bytes, SHA-256 `0e9404fd3d56e35fbdda3f9330bb07ee190bbcdd34213301805c19783be53602`.
- NOT_TESTED: 새 서명 APK 설치·기동·A/B 전환·mock 위치의 실제 동작. 클래스 포함은 실행 성공의 증거가 아니다.

## release Gleo 수정 APK 교체 (2026-11-15)

사용자가 46488e5 수정 후 생성한 APK로 루트 apk/ 제출 파일 교체. 원본 app/release 및 build/outputs 파일 해시 동일. 기존 인증서/패키지/비디버그 서명 검사 PASS. 실제 APK Manifest의 GleoActionReceiver exported=true 확인. 원본과 복사본 바이트 일치.

43663261 bytes, SHA-256 `0626a289e36794af41a09eacae5dbadd1684145e72d03199efcc44d01d97cd2a`. 새 APK 실제 설치 및 음성 Gleo 왕복은 NOT_TESTED. 이번 요청은 로컬 제출 파일 교체이며 커밋·푸시는 실행하지 않음.

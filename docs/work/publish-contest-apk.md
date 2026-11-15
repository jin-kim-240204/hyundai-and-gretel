# 공모전 APK 업로드

- 요청: 기존 복제 APK를 `jinkim_hyundai-and-gretel_release.apk`로 변경하고 커밋·푸시.
- 범위: APK, 해당 파일만의 ignore 예외, apk/README 및 이 기록. 새 빌드나 앱 코드 수정 없음.
- 규칙: REL-01 중 파일명·경로·서명 산출물 확인만 적용. 전체 제출/실행 적합 판정은 제외.
- PASS: 원본과 SHA-256 일치, `verify_submission_apk.sh`로 서명/기존에 확인한 인증서 지문/패키지명/비디버그 상태 검증. `git diff --check`, harness 검사.
- NOT_TESTED: 새 APK 설치·기동·핵심 기능 및 바이너리와 소스 커밋의 대응 관계. 사용자가 이미 생성한 산출물을 그대로 업로드한다.

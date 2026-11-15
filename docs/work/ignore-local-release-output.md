# 로컬 release 출력 제외

- 요청: `app/release/` 전체를 Git에서 제외하고 설정을 커밋·푸시한다. 현재 APK는 저장소 루트 `apk/`에 복제하되 제출 파일명은 팀명·앱명 확인 후 정한다.
- 범위: `.gitignore`에 `/app/release/` 추가. 기존 `*.apk` 제외 유지. 키·서명·앱 소스 변경 없음.
- 규칙: 저장소 커밋 절차 적용. Android 동작·공식 UI 규정 변경은 N/A.
- PASS: `git check-ignore -v app/release/app-release.apk app/release/output-metadata.json`, `git diff --check`, harness 정적 검사.
- N/A: 빌드·기기 테스트. ignore 규칙만 수정하므로 앱 바이너리에 영향 없음.

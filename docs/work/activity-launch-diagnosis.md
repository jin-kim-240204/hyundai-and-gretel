# MainActivity 실행 오류 확인

2026-11-14. 앱 삭제 후 `Activity class ... does not exist` 오류 문의. 앱 소스/IDE 설정은 변경하지 않았다.

- 범위: Manifest·applicationId·MainActivity package, 연결 기기/현재 Android 사용자, 설치 패키지 및 Activity 해석·직접 실행 확인. SDK 변경/에뮬레이터 설정 변경은 N/A.
- PASS: applicationId/namespace/Kotlin package 모두 `com.jinkim.hyundaiAndGretel`, Manifest `.MainActivity` 선언 일치.
- PASS: 연결 `emulator-5554`의 현재 사용자 10. 사용자 0/10 모두 앱 설치 상태. `cmd package resolve-activity --brief --user current com.jinkim.hyundaiAndGretel` → `.MainActivity`.
- PASS: `adb -s emulator-5554 shell am start -W --user current -n com.jinkim.hyundaiAndGretel/.MainActivity` → Status: ok, WARM, Complete.
- NOT_TESTED: 오류 발생 당시 Studio 설치/배포 로그. 현재 재현되지 않아 당시 원인을 확정하지 않는다. 삭제 후 미설치 상태에서 실행했거나 배포 대상 사용자/기기 불일치 가능성만 안내한다.
- N/A: 소스 수정/빌드/재설치/데이터 삭제/커밋. 해당 진단에서 설치를 덮어쓰거나 앱 데이터를 지우지 않았다.

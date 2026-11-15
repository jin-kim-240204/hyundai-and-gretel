# 공모전 제출용 APK

현재 산출물은 [jinkim_hyundai-and-gretel_release.apk](jinkim_hyundai-and-gretel_release.apk)입니다. 공식 파일명 형식은 `팀명_앱이름_release.apk`입니다. [공식 지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)

사용자가 생성한 서명 APK를 복사했습니다. SHA-256: `0e9404fd3d56e35fbdda3f9330bb07ee190bbcdd34213301805c19783be53602`. 기본 APK ignore를 유지하고 이 파일만 예외로 추적합니다. 서명 검증과 실제 동작 검증은 별개입니다. [기존 출시 제한](../docs/RELEASE-READINESS.md)은 새 APK에서 재검증이 필요합니다.

## 서명 완료 후 패키징

1. 승인된 release 서명 절차로 APK를 준비합니다. 담당자에게 승인된 서명 인증서 SHA-256 지문을 확인합니다.
2. 같은 APK로 기존 debug 데이터가 없는 Pleos 환경에서 설치·기동, Gleo 스키마 스캔과 Action/Callback, 제보·수신·TTS, 화면과 주행 제한을 확인합니다.
3. 저장소 루트에서 다음 명령으로 서명, 인증서, 앱 ID 및 디버그 가능 여부를 확인한 뒤 복사합니다. 이미 같은 이름의 파일이 있으면 복사를 중단합니다. `signed_apk`에는 서명한 APK 경로, `team_name`과 `app_name`에는 확정된 이름, `signer_sha256`에는 승인된 지문을 넣습니다.

   ```bash
   destination="apk/${team_name:?}_${app_name:?}_release.apk"
   test ! -e "$destination" && test ! -L "$destination" &&
     bash scripts/verify_submission_apk.sh "$ANDROID_HOME/build-tools/35.0.0" "${signed_apk:?}" "${signer_sha256:?}" &&
     cp -n "$signed_apk" "$destination"
   ```

4. 복사한 APK의 SHA-256이 검사 결과와 일치하는지 확인하고, 최종 제출 경로 또는 Release 링크와 소스 커밋을 기록합니다. 기기 동작 확인은 별도로 필요합니다.

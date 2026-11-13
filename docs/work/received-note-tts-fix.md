# PR #8 수신 쪽지 TTS 무음 대응

2026-11-14. 기준 c30abb94db50a1aacd9739b514b1c6e3f03b1d01, jarvis/fix-received-note-tts. 기존 조사와 설계는 tts-no-audio-code-review.md, ../plans/gleo-tts.md 참조. 보고 전 push 보류.

범위는 speech, RoadNoteService, JVM 회귀 테스트 및 작업 기록. AUDIO 미확인·비활성 시 출력 금지, 새 수신만 안내, ACK 독립, Pleos ON_DEVICE 및 160ms PCM 유지. ENV-02 / VEH-03 / AUDIO-01 / AUDIO-02 / PRIV-01 / INT-01 적용. 검증은 JVM·assemble·lint·하네스 및 SDK 배포물 정적 확인. 기기/앱/계측 실행과 가청 출력은 NOT_TESTED.

| 공식 항목 | 적용 및 확인 방법 |
|---|---|
| DOC-5ec59d586502#초기화해제, DOC-426eee956b29#재생이벤트, DOC-72916a4c511a#오류코드 | 초기화 성공 후 설정, 정상 종료는 onDone, 동일 리스너 해제 후 SDK release. 초기화/발화 오류 코드 및 요청·시작·완료를 구분. |
| DOC-fac75a3790c3#권한, DOC-cdaaba80cb83#온디바이스 | 기존 TTS_SERVICE 선언, ON_DEVICE. 설치 시 권한/한국어 엔진 동작은 별도 검증. |
| DOC-f04338dec3b6#포커스, DOC-a243773e232d#Suspend | 거절 시 재생 금지, 상실/전원 비활성에 중지·반납. [Android CarPowerManager](https://developer.android.com/reference/android/car/hardware/power/CarPowerManager)의 조회·등록 권한과 기존 Manifest 대조. |
| DOC-2cbe51f12c43#PCM | 150~200ms 권장. 160ms의 실제 PCM과 완료 marker 유지. [Android AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)의 marker·release 계약에 따라 초기 설정부터 정리 보장. |

## 구현과 확인된 결함

- Android 14 AOSP의 getCurrentPowerPolicy()는 @Nullable이고 원격 오류 시 null을 반환할 수 있다. 이전 코드는 즉시 역참조하고 등록된 콜백도 지웠다. 이제 null이면 AUDIO를 차단한 채 콜백을 유지하고, 유효한 후속 정책에서만 출력 가능 상태로 바뀐다. 연결·등록 예외와 실제 AUDIO 비활성을 구분한다. [AOSP Android 14 CarPowerManager](https://android.googlesource.com/platform/packages/services/Car/+/refs/heads/android14-release/car-lib/src/android/car/hardware/power/CarPowerManager.java)
- PCM 준비 단계부터 리스너 제거·stop·release를 독립적으로 시도한다. 정리 예외가 PCM 오류나 취소를 덮지 않으며, write/marker 반환값과 timeout을 구분한다.
- debug RoadSpeech에서 수신→큐 판단→DB 조회→SDK 초기화→포커스→PCM→발화 요청→ready/start/done을 구분한다. 취소, 전원·추적·본문 정책 리셋, SDK 오류도 별도 코드로 남긴다. 앱 로그에는 쪽지·식별자·위치·정책 ID·예외 원문을 넣지 않는다. 기존 조사에서 확인된 공급자 시스템 로그의 합성 문장 노출 가능성은 별개이며 이번에는 확인하지 않았다.
- Pleos Maven의 실제 TextToSpeech 3.3.1 AAR를 Gradle로 resolve하고 classes.jar를 javap로 확인했다. 생성자 Context/Mode, initialize(listener), setAudioAttributes(AudioAttributes), setLocale(Locale), speak(String, boolean), stop/release 및 콜백을 확인했다. 공식 start-and-stop 문서상 boolean은 isFinal이며 전체 문장 요청에서 true가 맞다. setAudioZoneId(int) 공개 선언은 있으나 대상 환경의 zone 및 원격 Speech Service 라우팅·포커스 동작은 확인하지 않아 호출하지 않았다.
- 위 결함은 정적으로 확인됐다. 사용자 발생 당시 어느 단계가 무음이었는지는 확정할 수 없다. 새 수신 이벤트에서 처음 진행이 끊긴 단계의 RoadSpeech 로그와 실제 가청 출력을 대조해야 한다.

## 검증 결과

- PASS: JDK 21 / Gradle 9.6.0, `./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. JVM 123개 중 실패/오류/건너뜀 0, ReceiptAnnouncementTest 12개. 신규 테스트는 정책 회복 시 새 수신만 안내하고 ACK가 없는지, 예외 본문이 진단에 빠지는지 확인한다. 기존 실패 테스트는 SDK 오류 코드와 조회 시간 초과 구분을 단언한다. lint 0 errors / 53 warnings.
- PASS: `python3 scripts/verify_harness.py --build` (원문 133개, 규칙 27개, Manifest 및 assembleDebug/lintDebug), `git diff --check`.
- NOT_TESTED: 에뮬레이터·시뮬레이터·adb·기기·앱 실행·계측을 수행하지 않았다. 실제 청취, 초기 PCM 출력/첫 음절, 차량 전원 콜백과 Suspend/Resume, 권한·한국어 엔진·오디오 포커스·라우팅, 공급자 로그 및 관측 무음 재현은 미확인이다. Studio 빌드는 원본 checkout만 열려 있어 이 worktree에서 실행되지 않았다.
- N/A: UI, DB, ACK, 과거 수신 재생, 별도 TTS 엔진, merge·deploy. Push는 사전 보고 요청에 따라 보류했다.

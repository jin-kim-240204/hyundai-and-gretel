# 문장형 제보와 수신 TTS 구현

2026-11-14. 요청: 계획 수정 후 구현. 기준 `gm/feat-gleo-tts`/8494fc5. 기존 계획 문서·문서 지도 변경을 보존한다.

## 범위/계약

- schema의 content 의미를 사실·부정·불확실성을 보존하는 짧은 완결 문장으로 명확히 한다. 기존 필드·Room 형식 유지. 앱이 임의 사실을 보충하거나 AI를 재호출하지 않는다.
- 새 수신 커밋 이벤트에만 연결하는 단일 큐와 SDK 출력 경계를 추가한다. UI ACK와 음성 완료는 구분하고 음성 실패가 저장 성공을 취소하지 않는다.
- 서비스·운전자·추적/시연 세대·만료·중복·취소를 검사한다. 직접 Callback으로 Gleo 대화를 만들지 않는다. ON_DEVICE를 우선하며 동일 Gleo 음색을 보장하지 않는다.
- 적용: INT-01, AUDIO-01, AUDIO-02, PRIV-01. 상세 DOC별 원문/필수·권장·조건은 gleo-tts-plan.md 표를 적용한다. 공식 TTS import/permission/init/events/locale/registration/speak/errors, 오디오 포커스/Suspend/선행 묵음 원문은 계획 단계 2026-11-14 확인 결과를 사용한다. 실제 AAR 선언·출력 소유권을 확인하고 차이는 여기 기록한다.
- 확인 범위: 기존 호출자/ReceiptStore Fake/Room·시연/서비스 종료, SDK callback/취소/권한/AudioFocus, debug/release 빌드 및 테스트. 음색·첫 음절·포커스/전원 실청취는 별도 증거가 없으면 NOT_TESTED.

## 진행

- SDK resolve 및 실제 API·내부 오디오 경로 확인 중. 함수명만 보고 시그니처나 오디오 정책을 추정하지 않는다.

## 구현 결정과 실행 흐름

1. `assets/schema.json`의 content 설명만 변경했다. 최초 Gleo 해석에서 완결 문장·부정·불확실성 보존을 요청한다. 앱 파서는 기존 길이/형식 검증을 유지하고 문법이나 사실을 임의 교정하지 않는다. 새 필드/Room migration/추가 LLM은 없다. 실제 스키마 스캔 및 Gleo 문장 품질은 NOT_TESTED다.
2. `ProximityPickup.tryReceiveNote`가 기존 저장 완료 콜백의 현재 세션 검사 후 RECEIVED만 알린다. Processor에 별도 이벤트를 만드는 계획보다 기존 소유권 검사를 재사용하는 것이 단순하다. ALREADY_RECEIVED/INELIGIBLE/STORAGE_FAILURE와 과거 수신함 관측은 발화하지 않는다.
3. `speech/ReceiptAnnouncement.kt`는 DB 커밋 후 수신함을 다시 읽고 키·운전자·시연·추적 세대·만료를 확인한다. 큐는 최대 8건, 조회 5초 제한이다. 직렬 발화, 중복 방지, 초기화 후 재검증, 실패 후 나머지 큐 폐기를 담당한다. 본문은 120 code point 이하면 원문을 유지해 카테고리 접두사만 붙이며 긴 글/본문 정책 미확인은 일반 수신 안내로 대체한다. 문장을 중간에서 잘라 부정/불확실성을 잃지 않는다.
4. `NoteSpeaker`는 테스트 Fake가 필요한 출력 경계다. `PleosNoteSpeaker`가 SDK 초기화→한국어/속성 설정→출력 가능 여부→포커스→실제 PCM 무음 160ms→출력 재검증→speak→onDone→정리를 처리한다. onStop/onError/35초 초과는 오류다. 외부 코루틴 취소는 다시 전파한다. SDK 이벤트에는 발화 ID가 없어 발화별 새 인스턴스를 사용하고 이전 인스턴스 리스너/연결/포커스를 정리한다. 첫 발화 지연은 남는다.
5. `CarAudioPower`는 공개 `CarPowerManager.addPowerPolicyListener/currentPowerPolicy/removePowerPolicyListener`와 AUDIO component를 사용한다. 조회 권한은 `android.car.permission.READ_CAR_POWER_POLICY`다. 참고: https://developer.android.com/reference/android/car/hardware/power/CarPowerManager . 전원/권한 불명은 비활성으로 취급한다. 해제/연결 끊김/정책 비활성은 출력과 큐를 취소하며 활성화만으로 재생하지 않는다.
6. `RoadNoteService`가 Room 저장소를 공유하고 음성 수명을 소유한다. 추적 종료·실패·actor/scenario 변경·시연 차단·서비스 종료에 큐/SDK를 중단한다. 본문 표시 정책 변경도 발화와 큐를 중단한다. UX 표시 제한을 음성에 대한 공식 허용 판정으로 간주하지 않고 본문 노출을 줄이는 프로젝트 정책으로만 사용한다. UI ACK는 음성 완료와 무관하다. 별도 UI·서비스·미디어 FGS·마이크는 추가하지 않았다.

숫자 제한은 공식값이 아닌 초기 프로젝트 선택이다. 큐/지연/문장 길이는 실제 청취 후 조정한다. 선행 PCM 무음과 SDK 원격 스트림은 별개이므로 실제 첫 음절 보장은 별도 검증한다. AudioAttributes는 NOTIFICATION_EVENT/SPEECH, 포커스는 TRANSIENT_MAY_DUCK이며 상실 시 재개하지 않는다. 일반 쪽지를 긴급/안전/내비 스트림으로 위장하지 않는다.

## SDK·환경에서 확인한 사실

- TextToSpeech 3.3.1 resolve 성공. 실제 AAR 공개 API에서 initialize(listener), setAudioAttributes(AudioAttributes), setLocale(Locale), speak(String, Boolean), stop, removeEventListener, release를 확인했다. setSpeakerType은 String이며 Gleo 음색 대응을 추정하거나 적용하지 않았다. 숨김 AIDL/내부 AmbientAI API는 사용하지 않았다.
- SDK Manifest가 INTERNET 및 Speech Service 조회 intent를 병합한다. 앱에는 서버 자격 증명·등록·네트워크 호출을 추가하지 않고 ON_DEVICE만 사용했다.
- 별도 read-only Pleos_Connect_v2.1.0 AVD, API34/arm64, CAAS4.2.10, emulator-5554에서 검사했다. SDK의 CAAS 최소4.1.29보다 새 버전이다. TTS_SERVICE와 READ_CAR_POWER_POLICY 설치 권한 granted=true를 확인했다. 에뮬레이터 시계와 호스트 시간이 달라 XML timestamp는 실제 검사 시각과 다르며 호스트 기준 작업일은 2026-11-14다.
- 계측에서 취소 후 새 발화 onStart/onDone 및 앱 요청 포커스 반납을 확인했다. 속성은 NOTIFICATION_EVENT/SPEECH였다. AUDIO 전원 정책 연결→enabled, close→disabled 확인. 실제 Suspend 전이/복구·다른 음원 충돌·가청 출력/첫 음절 청취와 같은 음색 여부는 확인하지 않았다.
- 앱 로그는 실패 작업명/예외 클래스만 남긴다. 다만 CAAS의 시스템 디버그 로그에서 합성 시험 문장 본문이 출력되는 것을 관찰했다. 앱에서 공급자 로그를 제어할 공개 API는 확인하지 못했다. 실사용 민감 제보·제출 환경에서는 공급자 로그 정책/비활성화 방법을 확인해야 하므로 개인정보 검증 전체를 PASS로 처리하지 않는다. 원본 logcat에는 합성 문장만 사용했고 저장소에 올리지 않는다.

## 실패 수정과 검증

- 초기 Kotlin 컴파일에서 lazy 속성 상호 참조의 타입 추론 순환이 발생해 타입을 명시했다. 수정 후 debug/release 컴파일 통과.
- 긴 문장 테스트에서 일반 안내의 “도착했습니다”에 포함된 한 글자를 검사해 오탐했다. 예상 일반 문장 전체를 검사하도록 수정했다. 구현의 중간 잘림은 없다.
- 마지막 검토에서 SDK 준비 중 만료/사용자/본문 정책 변경과 상위 timeout 취소를 확인했다. 발화/포커스 직전 guard와 취소 재전파를 추가했다.
- PASS: 단위 테스트 121개(음성 조정 10개 및 새 수신 트리거/옛 세션 커밋 검사 포함), 실패 0. 명령: `./gradlew :app:testDebugUnitTest :app:compileReleaseKotlin`.
- PASS: Pleos SDK 계측 2개(취소 후 다음 발화 정상 완료, AUDIO 정책 연결/해제) 및 RoomReceiptStoreTest 1개. 명령: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jinkim.hyundaiAndGretel.speech.PleosNoteSpeakerTest,com.jinkim.hyundaiAndGretel.notes.RoomReceiptStoreTest`. 결과 `app/build/outputs/androidTest-results/connected/debug/TEST-Pleos_Connect_v2.1.0(AVD) - 14.xml`.
- PASS: assembleDebug/assembleDebugAndroidTest 및 하네스 --build(assembleDebug/lintDebug), 원문133개·규칙27개 검사. Studio MCP build_project: isSuccess=true, problems=[].
- NOT_TESTED: 실제 Gleo schema 갱신→문장 생성→A 저장→B 접근→가청 출력 E2E, 첫 음절·실차 음색, 통화/음악/Gleo/긴급음 포커스 거절·상실·복구, Suspend/Resume, 권한 거절/서비스 끊김 실제 계측. 자동 검사 통과와 구현 반영을 전체 기능 검증 완료로 해석하지 않는다.
- N/A: 서버 공유·새 UI/Room 필드·음성 ACK 영속화·커밋/푸시·제출 APK 업로드. 현재 합성 A/B와 로컬 Room 시연의 범위다.

- PASS: 최종 수정 후 단위121개/계측3개 재통과, 하네스 --build/Studio 빌드 재통과. 규칙 JSON 12개 검사 결과 exit=2는 위 필수 실환경 NOT_TESTED를 보존한 결과이며 자동 빌드 실패가 아니다. `git diff --check` 통과. 검사 로그는 `/private/tmp/gleo-tts-final-tests.log`, `/private/tmp/gleo-tts-final-harness.log`에 있다.

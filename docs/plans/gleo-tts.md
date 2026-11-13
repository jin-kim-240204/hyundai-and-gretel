# 수신한 도로 쪽지 음성 안내 계획

작성일: 2026-11-14. 대상: `gm/feat-gleo-tts`, 기준 커밋 `8494fc5`. 상태: **구현 반영·자동 검사·SDK 계측 통과. 실청취·전원/포커스 시나리오는 별도 검증 필요**.

사용자 후속 결정: 최초 제보 시 Gleo가 사실·부정·불확실성을 보존한 짧고 완결된 문장으로 `content`를 생성한다. 별도의 ttsContent 필드나 수신 시 LLM 재호출 없이 기존 content를 저장한다. 수신 시 앱은 분류별 고정 접두사만 붙여 TTS로 읽는다. 스키마 설명은 생성 지침이며 문장 품질 보장은 실제 음성 검증으로 확인한다.

## 1. 목표와 구현 경로

운전자 B에게 새로운 쪽지 수신이 확정되면, 화면을 누르지 않아도 차량 스피커로 짧은 한국어 안내를 재생한다. 주행 중에도 동작하고 정차를 이유로 막지 않는다. 이 계획은 기존 implementation-plan.md에서 제외했던 선제 음성 안내를 이번 브랜치의 추가 범위로 제안한다. 위치/수신 거리/경로 판정과 합성 시연 DB 정책은 바꾸지 않는다.

권장 경로는 **Pleos TextToSpeech SDK → Speech Service → 차량 오디오**다. Gleo Interface는 사용자의 Action 요청과 앱 Callback 응답을 위한 인터페이스다. 수신 이벤트마다 임의의 APP_ACTION_RESPONSE를 보내 Gleo 대화를 시작하는 공개 계약은 확인하지 못했으므로 그렇게 구현하지 않는다. 기존 CREATE_ROAD_NOTE 필드·제보 응답 형식은 유지하고 content 설명만 문장형 생성 지침으로 구체화한다.

여기서 'Gleo 음성 안내'의 1차 구현 의미는 Pleos 음성 서비스로 쪽지를 읽는 것이다. Gleo 대화창·캐릭터 표시, 동일 음색, Gleo LLM의 새 문장 생성은 별도 요구사항/지원 확인 대상이다. API 탐색에 setSpeakerType이 보이지만 인자·모드별 지원 및 Gleo 음색 대응은 아직 확인하지 않았다.

## 2. 현재 코드의 연결점

```text
ProximityPickup.tryReceiveNote()          접근/세션/위치 조건 확인
  → NoteReceiptProcessor.receive()
    → RoomReceiptStore.receive()          트랜잭션에서 재검증·중복 방지 INSERT
      → ReceiveResult.RECEIVED            새 수신 확정
    → ProximityPickup 현재 세션 확인
    → ReceiptAnnouncement          수신 키를 받아 발화 대기열에 등록
      → 커밋 후 Room 스냅샷에서 내용 확인
      → 수신자/시연/추적 세대·유효 기간 재확인
      → [추가] PleosNoteSpeaker           초기화·오디오·음성 재생
      → Speech Service → 차량 스피커

기존 observeInbox → RoadNotesScreen      화면 표시/확인 경로는 계속 독립적으로 동작
```

- 기존 수신 유일성은 `(noteId, recipientId)` PK와 트랜잭션이 담당한다. 후보 접근 콜백이나 Compose 렌더링을 음성 트리거로 쓰지 않는다.
- 현재 TTS 의존성·TTS_SERVICE 권한·음성 재생 코드는 없다. SDK v3.3.1은 공식 가이드 기준이며 현재 Gradle/에뮬레이터와의 호환은 첫 단계에서 확인한다.
- `InboxState.pending`/`displayedAtUtcMillis`는 화면 표시/확인용이다. 이를 음성 완료로 간주하거나 음성 완료 때 `confirm()`을 호출하지 않는다. 현재 UI의 버튼 확인 정책도 유지한다.
- 현재 수신은 한 앱의 가상 A/B 및 합성 경로 기반이다. TTS를 붙였다고 실차 간 서버 공유나 실위치 영속화까지 구현됐다고 하지 않는다.

## 3. 사용자 동작과 기본 정책 — 프로젝트 선택

| 상황 | 계획된 동작 |
|---|---|
| 새 쪽지 수신 커밋 | 분류 접두사와 본문을 한국어로 한 번 안내. 예: '도로 경고입니다. 전방 도로에 낙하물이 있습니다.' |
| WARNING / INFORMATION | '도로 경고입니다.' / '도로 안내입니다.' 정도로 구분. 시스템 SAFETY/EMERGENCY 권한을 주장하지 않음 |
| 동일 쪽지 재접근/중복 INSERT | ALREADY_RECEIVED이므로 추가 안내하지 않음 |
| UI 재구성/재바인딩 | 새 수신 이벤트가 아니므로 다시 읽지 않음 |
| 여러 쪽지 도착 | 커밋된 순서로 한 번에 하나만 재생. SDK의 불명확한 내부 큐에 모두 넘기지 않음 |
| 긴 본문 | 발화 길이를 제한해 짧은 안내로 구성. 정확한 글자/시간 상한은 한국어 실청취 후 확정·기록하며 중간 잘림을 전체 안내 완료라고 표시하지 않음 |
| 초기화/권한/음성 엔진 오류 | 수신 데이터와 UI는 보존. 실패 코드만 기록하고 반복 음성 재시도하지 않음 |
| 포커스 거절·통화/다른 음성 개입 | 현재 발화를 시작하지 않거나 중단. 오래된 대기열을 나중에 몰아서 자동 재생하지 않음 |
| 추적 중단·A/B 변경·시연 초기화·서비스 종료 | 현재 음성 중단, 대기열 폐기, 이전 세대의 늦은 콜백 무시 |
| 차량 Suspend/서비스 연결 끊김 | 정지·리스너/포커스 정리. Resume만으로 이전 안내를 자동 재생하지 않음 |
| 초기 수신함 로드/프로세스 재시작 | 과거 이력을 새 수신으로 보지 않음. 과거 쪽지 자동 낭독은 1차 범위 밖 |

첫 버전은 **새 커밋 이벤트에 대한 best-effort 자동 안내**다. 수신 커밋 직후 프로세스가 종료되면 음성 안내가 누락될 수 있다. DB 커밋·오디오 출력은 원자적 작업이 아니며, 앱이 '정확히 한 번 들려줬다'고 보장할 수 없다. 재시작 후 미완료 안내 복구가 필수라면 화면 ACK와 별개인 영속 음성 상태를 후속으로 설계한다. 이번에는 그 목적만으로 Room 스키마를 바꾸지 않는다.

## 4. 구현 순서

### 1단계 — SDK와 출력 경로 확인

1. `ai.pleos.playground:TextToSpeech:3.3.1` resolve 및 최소 컴파일로 실제 심볼 확인. 기존 Pleos Maven 사용.
2. Manifest의 `pleos.car.permission.TTS_SERVICE`와 환경의 실제 권한 부여를 확인. 불필요한 마이크/녹음 권한을 추가하지 않는다.
3. 최초 제안은 `Mode.ON_DEVICE`, `Locale.KOREA`. 네트워크와 Client Secret 없이 동작하는 모드로 기본 발화를 검증한다. 다른 엔진으로 조용히 대체하지 않는다.
4. 동일 Gleo 음색이 제품 필수 조건이면 setSpeakerType의 실제 선언·모드별 지원/청취를 먼저 확인한다. HYBRID/SERVER가 필요하면 registerApp 및 onConnected 이후 요청, 자격 증명 보관·외부 전송 정책을 별도 적용한다.
5. `initialize` 성공 → 리스너 등록/언어 설정 → speak → onDone/onStop/onError → stop/removeEventListener/release 경로를 짧은 합성 문장으로 검증한다. `onReady`나 `onStart`를 완료로 표시하지 않는다.
6. **통합 전에 확인할 조건:** setAudioAttributes/setAudioZoneId의 실제 타입·지원, SDK와 앱 중 실제 포커스 소유자, 차량 Suspend 통지 경로, 선행 묵음 처리 주체. 생성형 API 페이지의 심볼 이름만으로 호출 시그니처나 보장 동작을 추정하지 않는다.

### 2단계 — 음성 출력 담당 분리

- `speech/NoteSpeaker.kt`: 테스트 Fake가 필요한 실제 경계만 둔다. `speak`의 성공은 단순 호출 성공이 아닌 정상 종료 이벤트로 정의하고, 중지·오류를 구분한다. 구체 시그니처는 1단계 API 확인 후 확정한다.
- `speech/PleosNoteSpeaker.kt`: 발화별 SDK 인스턴스·준비 상태·직렬 발화·이벤트 정리 담당. 콜백에 발화 ID가 없어 이전 인스턴스 해제 후 새 인스턴스로 격리한다. Android Context는 applicationContext를 사용한다. SDK 호출/콜백의 스레드 조건을 확인하고 서비스의 Main 처리와 합친다.
- 콜백에 요청 ID가 없는 경우를 고려한다. 중지/timeout 후 늦게 도착한 이전 onDone이 다음 발화를 완료시키지 않도록 종료 확인 후 다음 발화를 시작한다. 종료를 확인할 수 없으면 이전 리스너/인스턴스를 해제하고 새 세대로 초기화한다. 취소 시 SDK stop 및 자원 정리도 수행한다.
- SDK가 포커스를 관리한다면 앱의 중복 요청으로 서로 선점하지 않게 한다. 앱이 관리해야 한다면 AudioAttributes/AudioFocusRequest와 획득·상실·반납을 출력 담당에 구현한다. 실제 스트림 속성과 요청 속성이 일치해야 한다.
- 일반 도로 쪽지를 자체 내비게이션이나 EMERGENCY로 위장하지 않는다. 짧은 알림/음성 목적의 속성을 공식 매트릭스 및 실제 SDK 라우팅에 맞춰 확정한다. 포커스 거절이면 재생하지 않고, Gleo 대화·통화·긴급 안내에 양보한다.
- 선행 묵음은 150~200ms 권장 구간을 확인한다. SDK/Speech Service 적용 여부를 먼저 확인하고, 단순 `delay(150)`를 오디오 앞단 묵음으로 간주하지 않는다. 공개 API로 처리할 수 없다면 공급자 확인·출력 경로 검증이 남은 상태로 기록한다.

### 3단계 — 새 수신 이벤트만 연결

1. `ProximityPickup`의 기존 `NoteReceiptProcessor.receive` 완료 콜백을 재사용한다. 현재 추적 세션 소유권을 확인하고 **RECEIVED일 때만** `onReceived(ReceiptRequest)`를 호출한다. 별도 Processor 이벤트는 추가하지 않는다. 음성 실패가 이미 성공한 수신을 STORAGE_FAILURE로 바꾸지 않게 한다.
2. `speech/ReceiptAnnouncement.kt`가 키를 받아 같은 키의 중복 등록을 막고 한 번에 하나씩 처리한다. DB/IO 작업을 추가로 기다리지 않는 빠른 등록 함수로 만들어 기존 `writes/onFinished` 수명에 발화를 끼워 넣지 않는다.
3. `RoadNoteService`에서 기존 RoomReceiptStore 인스턴스를 공유한다. 발화 직전 기존 `observeInbox(recipient).first()`로 커밋 후 스냅샷을 읽어 해당 키를 찾는다. 기존 UI observer의 반영 타이밍에 기대거나 입력 콜백의 미확정 내용을 읽지 않는다. 필요한 경우에만 전용 단건 조회로 좁힌다.
4. 조회 전후와 실제 발화 직전에 현재 recipient·scenario·추적 세대·만료를 재검증한다. 관측된 본문은 읽을 텍스트로만 취급하고 명령/도구 호출로 실행하지 않는다. 제한된 안내 문장으로 변환한다.
5. queue/중복 키는 활성 세션의 유효 수신 범위로 제한한다. actor/scenario 변경·만료·중단 시 정리해 무한히 누적하지 않는다. 여러 위치 콜백/Room 재발행은 추가 발화를 만들지 않는다.

### 4단계 — 서비스 생명주기와 시연 연결

- 소유자는 `RoadNoteService`다. Composable/ViewModel이 SDK를 생성하거나 화면 재구성마다 speak하지 않는다.
- 새 수신 시 초기화한다. 소량의 시연 수신에 맞춰 상시 SDK 연결을 줄이고 콜백 세대를 격리한다. 첫 안내에는 초기화 지연이 포함된다. 초기화 실패가 위치 수집 자체를 실패시키지 않는다.
- `stopTracking()`, `DemoServiceContract.changeActor()`, 시연 리셋, 추적 실패 및 `onDestroy()`에서 stop/queue clear/세대 무효화를 연결한다. 위치/저장 정리 중 음성 정리 오류가 생겨도 나머지 정리는 계속 실행한다.
- 현재 서비스 종료 조건과 FGS 유형을 유지하는 것을 기본으로 한다. 추적 중단 후 안내를 끝까지 재생시키려고 서비스를 불필요하게 유지하지 않는다. 별도 장기 배경 재생이 실제 필요해질 때만 mediaPlayback 등 Android 실행 요건을 추가 검토한다.
- 수신 표시 제한과 음성 허용 여부는 같은 Boolean으로 단정하지 않는다. `canShowBody=false`를 근거로 그대로 읽거나 무조건 음소거하지 않고 카테고리/개인정보/정책을 확인한다. 필요하면 본문 대신 짧은 수신 사실만 안내한다. 속도·기어만으로 허용을 결정하지 않는다.
- 새 팝업/읽기 확인 강제/전체 화면 전환은 추가하지 않는다. 음성 상태를 표시할 필요가 생기면 기존 서비스 상태→ViewModel 경로에 최소 상태만 노출한다.

## 5. 변경 예상 파일

| 파일 | 변경 내용 |
|---|---|
| `app/build.gradle.kts`, 필요 시 버전 목록 | TTS 의존성 추가·resolve |
| `AndroidManifest.xml` | TTS_SERVICE 권한과 실제 SDK 요구 설정만 추가 |
| `speech/NoteSpeaker.kt`, `PleosNoteSpeaker.kt` | Fake 가능한 음성 출력 경계와 SDK 연동 |
| `speech/ReceiptAnnouncement.kt` | 문장 구성·유효성 확인·중복 방지·순차 처리 |
| `notes/pickup/ProximityPickup.kt` | 현재 세션의 RECEIVED 이후 새 수신 이벤트 전달 |
| `service/RoadNoteService.kt` | 초기화·수신 연결·actor/추적/실패/종료 정리 |
| `notes/pickup/ProximityPickup.kt`, debug `DemoRuntime.kt` | 기본은 기존 판정 유지. 세대/리셋 정리에 실제 필요한 부분만 변경 |
| `app/src/test/.../speech`, 기존 ReceiptProcessor 테스트 | Fake 음성·수신·중단·경합 테스트 |
| `app/src/androidTest/.../speech`, 기존 Room 테스트 | 실제 SDK/권한과 Room 연동 검증 |
| 관련 README/실행 명세 | 선제 음성 제외 범위를 이번 기능 범위로 갱신, 실행 설정·한계 기록 |

UseCase·Domain·새 서버·새 네트워크 라이브러리·범용 EventBus·별도 TTS Service는 추가하지 않는다.

## 6. 검증과 완료 조건

| 검증 | 통과 기준 |
|---|---|
| 새 수신·중복·실패 | RECEIVED만 1회 요청. ALREADY_RECEIVED/INELIGIBLE/STORAGE_FAILURE와 DB 최초 로드·UI 재구성은 0회 |
| 큐/완료 | A 종료 전 B 재생 없음. onReady/onStart는 완료 아님. onDone만 정상 종료, 오류/중단은 별도 |
| 세대 경합 | 조회/초기화/재생 중 actor 전환·리셋·추적 중단 후 옛 본문 재생 없음. 늦은 콜백이 새 작업에 적용되지 않음 |
| 본문/만료 | 빈값·긴값·구두점/개행·한글/이모지 처리 확인. 발화 직전 만료/삭제/수신자 변경이면 안내하지 않음 |
| 기능 독립성 | 음성 실패에도 Room 수신 행과 기존 화면/버튼 확인이 보존됨. 음성 완료로 displayed ACK하지 않음 |
| 자원 | SDK 초기화/stop/remove/release·포커스 반환·scope 취소가 수명에 맞게 수행됨 |
| 자동 검사 | testDebugUnitTest, 하네스 --build/규칙 report, debug/release 및 AndroidTest 컴파일, Studio 빌드 |
| Pleos 실청취 | 한국어 실제 오디오 확인, 첫 음절 손실 없음, 두 쪽지 겹침 없음, 동일 쪽지 재접근 반복 없음 |
| 다른 오디오·전원 | 음악/내비/Gleo 대화/통화/긴급 안내와 포커스 충돌, 거절/상실, Suspend/Resume·연결 끊김 확인 |
| 시연 | A가 심은 쪽지를 B가 수신할 때 화면과 음성을 확인. 주행·정차, 화면 재진입, B→A 전환/초기화 포함 |

SDK resolve·컴파일과 에뮬레이터의 취소 후 재발화/onDone은 확인했다. 음색·첫 음절·포커스 충돌 및 Suspend/Resume 실제 출력은 NOT_TESTED다. 최종 명령 결과와 제한은 [구현 기록](../work/gleo-tts-implementation.md)을 따른다. 이 항목이 남으면 음성 안내 기능 완료나 공식 적합성 통과로 보고하지 않는다.

## 구현 시 확정한 최소 정책

- `CarAudioPower`는 공개 Android CarPowerManager의 AUDIO 전원 정책을 관측한다. `READ_CAR_POWER_POLICY`를 선언하고 연결/권한/정책이 불확실하면 새 발화를 막는다. AUDIO 비활성·연결 끊김 때 현재 음성과 대기열을 버리고, 활성화만으로 이전 안내를 복구하지 않는다. 실제 Suspend 전이와의 대응은 실차/에뮬레이터 시나리오로 추가 검증한다.
- 출력 속성은 `USAGE_NOTIFICATION_EVENT` + `CONTENT_TYPE_SPEECH`, transient may-duck 포커스다. 일반 쪽지를 내비/안전 긴급 오디오로 분류하지 않는다. AUDIOFOCUS_GAIN 이외의 변경이면 중단하고 재개하지 않는다.
- PCM 160ms 무음을 먼저 출력한다. 다른 오디오 객체인 SDK 스트림까지 첫 음절 손실 방지가 보장된다고 주장하지 않는다.
- 본문은 120 code point 이하만 읽는다. 초과 시 자르지 않고 수신 사실만 읽는다. 화면 본문 허용 정책이 미확인/불허여도 짧은 수신 사실만 알린다. 음성 전용 정책이 확정되기 전의 보수적인 프로젝트 선택이다.
- 대기열 최대 8건, DB 조회 5초, 초기화~발화 완료 35초, 묵음 완료 2초 제한이다. 모두 공식 제한이 아닌 초기 프로젝트 값이며 실제 청취 후 조정한다. 실패 시 대기열을 버리며 DB와 UI ACK는 유지한다.

## 7. 공식 근거

2026-11-14 재확인. 아래 TTS 가이드 내용은 기존 저장소 스냅샷과 주요 요구사항 차이가 없었다. 규정의 필수 조건, 권장값, 위 프로젝트 선택은 구분한다.

- [Gleo Interface](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts), [Callback](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback): 요청/응답 인터페이스와 자발 수신 안내 구분.
- [TTS 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro), [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions).
- [초기화/해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release), [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners), [언어](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale), [등록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration), [시작/중지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop), [오류](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code).
- [오디오 포커스](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development), [Suspend](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus), [선행 묵음](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide).
- [생성형 TextToSpeech API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/entry): 목록에서 setAudioAttributes/setAudioZoneId/setSpeakerType/speak를 확인. 웹 본문에서 타입/설명은 확인되지 않아 실제 AAR/API 선언 확인이 필요하다.

# Gleo 음성 연동 로그 확인

현재 debug 빌드는 저장 동의 없이 파라미터 검증과 Callback만 수행한다. 화면의 최근 10건은 메모리 기록이고, `GleoProbe` logcat에는 수신/처리 완료 시점·문자 수·처리 시간만 남긴다. 입력/출력 JSON 원문은 앱의 메모리 테스트 화면에서 확인한다. Gleo 프로세스 자체의 로그에는 발화가 포함될 수 있으므로 공유 전에 민감정보를 제거한다. release에는 외부 테스트 수신과 원문 로그를 켜지 않는다.

사용자 요청으로 주행 상태 감시/간결 UI 전환을 원복했다. 현재 테스트 빌드는 distractionOptimized 선언만 유지하고 기존 전체 JSON 관측 UI를 표시한다. PC logcat도 계속 사용할 수 있다.

## 바로 보기

터미널 1 — 우리 앱 수신과 결과:

```bash
adb logcat -v time -s GleoProbe:D '*:S'
```

터미널 2 — 음성 인식부터 앱 전달까지 핵심 단계:

```bash
adb logcat -v time | rg --line-buffered 'DICTATION_SERVER|AGENTCLASSIFIER|refineToolOutput|sendActionCall|GleoProbe|actionCallback|timeout'
```

연결 기기가 여러 개면 각 adb 뒤에 `-s emulator-5554`처럼 `adb devices`에 나온 실제 시리얼을 붙인다. `Ctrl+C`로 로그 보기를 끝낸다. 전체 logcat 삭제(`adb logcat -c`)는 기본 절차에 넣지 않는다. 기존 장애 증거가 사라지기 때문이다.

## 어디에서 멈췄는지 읽기

| 로그 | 확인할 것 | 다음 로그가 없을 때 |
|---|---|---|
| DICTATION_SERVER | 실제로 인식한 발화 | 음성 인식/네트워크/사용자 문장 확인 |
| AGENTCLASSIFIER | 앱 호출 agent로 분기했는지 | Gleo 의도 분류/재질문 여부 확인 |
| refineToolOutput | 선택한 함수와 parameters | Schema 로딩/함수 선택/필수값 확인 |
| sendActionCall | 실제 패키지·Android Action·component·파라미터 | Gleo 실행/전달 단계 확인 |
| GleoProbe RECEIVED | 우리 Receiver에 실제 도착했는지·입력 문자 수 | 패키지 접두사·Manifest filter/exported·설치 빌드 확인 |
| GleoProbe COMPLETED | 처리 완료 시점·처리 시간 | 파싱/필드 검증/실행 예외 확인 |
| 앱의 Callback JSON 화면 | 앱이 보낸 success/fail 응답 JSON | 전송 예외·기한 초과 확인 |
| Gleo actionCallback | Gleo가 받은 응답 처리 | Callback 목적지·action·extra·functionName 대조 |

`Callback 전송 요청`은 Android sendBroadcast 호출이 끝났다는 뜻이며 Gleo가 응답을 받았거나 음성 안내를 마쳤다는 증거가 아니다. Gleo 단계는 Gleo 프로세스 로그에서 확인한다.

## Gleo 전체 로그만 별도로 보기

```bash
adb shell pidof ai.umos.ambientai
```

출력된 숫자를 아래 PID에 넣는다. Gleo 프로세스를 재시작하면 PID가 바뀐다.

```bash
adb logcat --pid=2131 -v time
```

Android Studio에서는 Logcat의 package 필터로 `com.jinkim.hyundaiAndGretel`과 `ai.umos.ambientai`를 각각 선택해도 된다. 우리 앱 로그만 보면 Gleo의 함수 선택 이전 단계는 보이지 않는다.

## 이번에 확인한 장애

에뮬레이터 시각 02:19:05에 Gleo는 함수를 선택하고 다음 Android Action을 보냈다.

```text
com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE_DRAFT
```

앱은 당시 `CREATE_ROAD_NOTE_DRAFT`만 기다렸기 때문에 수신하지 못했고 약20초 후 Gleo가 timeout을 안내했다. 실제 관측 형식에 맞춰 Manifest/Receiver를 패키지 접두사가 있는 Action으로 수정했다. Schema 함수명과 Callback functionName은 기본 함수명을 유지한다. 공식 문서의 전달 예제/7초 제한과 실제 설치 빌드의 동작 차이는 자비스 기록에 남겼다.

같은 요청에서 LLM이 2023년 utteranceRecordedAt을 생성했다. 이 값은 실제 발화 기록 시각으로 사용하지 않는다. 시각 메타데이터 제공 계약은 여전히 미확인이다.

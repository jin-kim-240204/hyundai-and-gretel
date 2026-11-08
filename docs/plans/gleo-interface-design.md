# 이번 브랜치: Gleo 음성 명령 연동 설계

수정·관련 공식 원문 재확인: 2026-11-08. 상태: 검토용 설계, 구현 전. 저장소 현황 대조: 2026-11-08.
기존 STT → Gleo 설계를 대체한다. 앱이 STT/TTS SDK를 사용하지 않고 시스템 Gleo의 음성 입력·해석·응답을 활용하도록 사용자 요청에 따라 변경했다.

## 1. 결정과 범위

흐름은 **사용자 → 시스템 Gleo → 앱 Action 수신 → 초안 처리 → Callback → Gleo 음성 응답**이다. 여기서 ‘Gleo 바로 호출’은 사용자가 시스템 Gleo로 우리 앱의 기능을 실행하는 의미다. 앱 내부 버튼이 Gleo 마이크를 여는 API는 공식 문서에서 확인되지 않았으므로 구현 전제로 삼지 않는다.

메인 시나리오는 목격 즉시 Gleo를 호출해 자연스럽게 제보하는 것이다. 초 단위 입력은 요구하지 않는다. 이번 브랜치는 내용과 위치 처리 의도를 받아 나중에 UI에서 이어 쓸 수 있는 로컬 초안을 저장하고 결과를 Gleo가 안내하는 것까지다. 이전 메모리 전용 초안 계획을 대체한다. 실제 위험 좌표 계산·쪽지 게시·다른 차량 수신은 후속 작업이다. 초안을 만들었다는 응답에 ‘도로에 등록했다’는 의미를 섞지 않는다.

앱의 마이크 녹음, 상시 청취, STT/TTS SDK 및 관련 권한, 별도 LLM 호출, 음성을 위한 상주 FGS는 추가하지 않는다. 시스템 Gleo의 음성 서비스 내부 구현은 앱이 소유하지 않는다.

## 2. 공식 규격과 프로젝트 선택

공식 근거:

- [S1 Gleo Interface](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts), Core Concepts: 앱 함수 정의와 Gleo Action/Callback 흐름.
- [S2 App Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability), Fields 및 작성 가이드라인: 함수·파라미터 형식.
- [S3 Callback Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback), Fields 및 Status: 응답 형식, 음성 모드와 7초 취소.
- [S4 Intent/Callback 예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples), Android Intent 변환 및 Callback: Android 전달 계약.
- [S5 Schema 전달](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema): 파일 경로, 필터, 검토·활성화·스캔 조건.

현재 저장소는 ComponentActivity/Compose 시작 화면과 Vehicle 2.3.2 의존성 등록 상태다. 차량 SDK 초기화·차량 조회/제어, Gleo Schema/Receiver/Callback, 위치·초안 저장·서버는 구현되지 않았다. Manifest에는 도어/창문 조회·제어 권한 4개가 선언되어 있지만 기능 구현·런타임 승인 증거는 아니다. allowBackup=false와 adjustResize는 이미 설정되어 있으며 초안 저장 보호·UI 적합성 전체가 검증된 것은 아니다.

패키지 예제는 사용자 지정 식별자 `com.jinkim.hyundaiAndGretel`로 통일한다. 소스 package/Gradle namespace도 이 값이다. **현재 app/build.gradle.kts의 applicationId는 아직 `com.jinnayeong.wakeup`이므로 아래 예제와 설치 앱 식별자가 불일치한다.** 실제 Gleo 등록·설치 전 applicationId를 목표 식별자에 맞추는 별도 설정 변경이 필요하다. 이번 문서 수정에서는 설치 앱 식별자·기존 앱 데이터에 영향을 주는 Gradle 설정을 변경하지 않았다.

공식 의무와 프로젝트 설계안을 구분한다. 아래 `CREATE_ROAD_NOTE_DRAFT`, 필드명, 입력 제한, 앱 내부 함수 시그니처는 프로젝트가 정한 제안이다. Gleo가 제공하는 SDK 클래스나 등록된 공식 도로 제보 API가 아니다.

## 3. App Schema 초안

구현할 때 `app/src/main/assets/schema.json`에 둔다. 지금은 문서의 예시만 작성한다.

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "appDescription": "An app for preparing and sharing road condition reports with approaching drivers.",
  "version": "1.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "component": "service",
        "name": "CREATE_ROAD_NOTE_DRAFT",
        "description": "Prepare a road condition report draft without publishing it to other drivers.",
        "parameters": {
          "type": "object",
          "properties": {
            "content": {
              "type": "string",
              "description": "The road condition observed by the user."
            },
            "locationMode": {
              "type": "string",
              "enum": ["CURRENT_ROAD", "NAMED_PLACE", "NEEDS_CLARIFICATION", "DEFERRED"],
              "description": "The location handling intent: nearby current road for an immediate observation, an explicitly named place, an unclear location requiring clarification, or explicit deferral for later placement."
            },
            "placeQuery": {
              "type": "string",
              "description": "The explicitly named place and its spatial qualifiers for location lookup."
            },
            "directionMode": {
              "type": "string",
              "enum": ["CURRENT_VEHICLE", "USER_DESCRIPTION", "UNKNOWN"],
              "description": "The direction reference: the reporting vehicle's current travel direction, a direction explicitly described by the user, or an unknown direction."
            },
            "directionDescription": {
              "type": "string",
              "description": "The user's explicit description of the affected road's travel direction."
            },
            "utteranceRecordedAt": {
              "type": "string",
              "description": "The original report utterance start time captured by the speech system, in ISO 8601 UTC format, when available as measured metadata."
            },
            "draftId": {
              "type": "string",
              "description": "The app-issued identifier returned for an existing draft being continued."
            }
          },
          "required": ["content", "locationMode"]
        }
      }
    }
  ]
}
```

| 항목 | 구성 이유 |
|---|---|
| packageName / version | 사용자 지정 목표 packageName과 현재 versionName=1.0 사용. namespace는 일치하지만 applicationId는 미반영이므로 등록 전 정합 필요. 마켓 등록 버전은 미검증 |
| 함수 한 개 | 이번 브랜치에 필요한 초안 생성만 노출. 공식 최대 3개를 채우기 위한 기능은 만들지 않음 |
| CREATE_ROAD_NOTE_DRAFT | 대문자·밑줄 규칙을 지키고 게시와 초안을 명확히 구분 |
| component=service | UI를 열지 않는 명령 수신. 공식적으로 이 값은 startService가 아니라 sendBroadcast를 뜻함 |
| content | 원문 전체 녹취 대신 사용자 관찰 내용만 전달받음. 실제 원문과 동일하다고 가정하지 않음 |
| locationMode | Gleo가 위치 처리 의도를 분류. 사용자는 enum이나 초를 말할 필요 없음 |
| placeQuery | NAMED_PLACE에서 필요한 지명·출구·전후 관계. 실제 좌표는 앱의 장소 검색으로 결정 |
| directionMode | 실시간은 CURRENT_VEHICLE로 앱이 현재 차량 진행 방향을 사용. 명시 방향은 USER_DESCRIPTION, 불명은 UNKNOWN |
| directionDescription | 사용자 명시 방향을 보존. USER_DESCRIPTION에서 필요하며 실제 도로 방향과 대조 |
| utteranceRecordedAt | 음성 시스템이 실제 관측한 원제보 발화 시작 시각. UTC ISO 8601, 제공 불가 시 생략 |
| draftId | 기존 초안 후속 수정에만 사용. 앱 발급값을 확인하며 임의로 생성하지 않음 |
| content + locationMode 필수 | 내용과 처리 분기만 공통 필수. 지명/ID의 조건부 검증은 앱에서 수행 |

S2에 따라 description은 영어이며 구체적인 발화 예시를 넣지 않았다. 파라미터는 모두 string, enum도 문자열만 쓴다. nullable union·중첩 object·조건부 JSON Schema 기능은 사용하지 않는다. 선택값이 없으면 키를 생략하고 빈 문자열/null로 대체하지 않는다. 필수값 충족도 보장되지 않으므로 앱이 재검증한다.

`invokeWord`는 공식 예시에 있지만 필드 명세에서 의미·필수 여부·호출 트리거 보장이 설명되지 않는다. 앱명 음성 인식과 호출 문구는 실제 환경/공급자 확인 후 결정하고 지금 스키마에는 넣지 않는다. 앱 이름을 포함해 말한다고 반드시 이 함수가 선택되는 것은 아니다.

위험/경고/안내/정보 분류는 후속 작업이다. secondsBeforeUtterance 등 초 수치화 필드는 사용하지 않는다. 위치 분기와 근사 배치 기준은 11절을 따른다.

## 4. Gleo → 앱 Action JSON 및 Android 수신

예상 사용자 발화(테스트 시나리오): “방금 도로에 구덩이 봤어. 제보해 줘.”

Gleo가 구성할 논리적 Action 형태는 S4를 따른다. 실제 LLM의 표현·함수 선택은 실기기에서 확인한다.

locationMode는 사용자 표현을 앱 처리 분기로 바꾸는 프로젝트 계약이다. LLM이 좌표·반경·정확한 관측 시각을 생성하지 않는다.

### 목격 직후: 현재 도로 주변

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE_DRAFT",
  "actionParameters": {
    "content": "도로에 구덩이가 있음",
    "locationMode": "CURRENT_ROAD",
    "directionMode": "CURRENT_VEHICLE",
    "utteranceRecordedAt": "2026-11-08T03:00:00Z"
  }
}
```

“10초 전”도 목격 직후 문맥에서는 같은 CURRENT_ROAD로 분류한다. 시간 표현을 별도 필드로 전달하거나 초를 역산하지 않는다. directionMode=CURRENT_VEHICLE은 수치 방향을 Gleo가 계산하는 의미가 아니라 앱이 차량 센서에서 채우라는 뜻이다. 위 utteranceRecordedAt은 **시스템 관측 시각을 전달할 수 있는 경우의 제안 예시**이며 현재 Gleo가 이 메타데이터를 제공한다는 증거가 아니다. 사용자에게 시각을 말하게 하거나 LLM이 현재 시각을 지어내게 하지 않는다.

### 명시 지명: 해당 장소 주변

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE_DRAFT",
  "actionParameters": {
    "content": "도로에 낙하물이 있음",
    "locationMode": "NAMED_PLACE",
    "placeQuery": "역삼역 4번출구 쪽",
    "directionMode": "UNKNOWN",
    "utteranceRecordedAt": "2026-11-08T03:00:00Z"
  }
}
```

“뱅뱅사거리”도 NAMED_PLACE다. 역 중심점으로 출구/방향 수식어를 잃지 않으며 앱이 장소 후보를 검증한다.

### 위치 불명: 저장 후 질문

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE_DRAFT",
  "actionParameters": {
    "content": "도로에 낙하물이 있었음",
    "locationMode": "NEEDS_CLARIFICATION",
    "directionMode": "UNKNOWN",
    "utteranceRecordedAt": "2026-11-08T03:00:00Z"
  }
}
```

앱은 먼저 위치 미정 초안을 저장하고 asking으로 장소를 묻는다. “아까 뱅뱅사거리”처럼 지명이 있으면 NAMED_PLACE이며, 단어 하나보다 전체 문맥을 우선한다.

### 모름/나중에 지정: 저장 후 대화 종료

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE_DRAFT",
  "actionParameters": {
    "content": "도로에 낙하물이 있었음",
    "locationMode": "DEFERRED",
    "directionMode": "UNKNOWN",
    "draftId": "draft-local-001"
  }
}
```

직전 초안 ID를 정확히 전달할 수 있을 때만 갱신한다. 독립적인 최초 “위치는 모르겠고 내용만 저장해 줘” 요청에는 draftId를 생략한다. 현재 제안의 모든 호출은 content 전체를 포함한다. 기존 초안과의 대화 연결이 확인되지 않으면 6절의 UI 복구 규칙을 따른다.

앱이 이 Action JSON을 Gleo로 보내는 것이 아니다. Gleo가 Android Intent로 변환해 앱에 보낸다. 수신은 `BroadcastReceiver`에서 처리한다. 향후 Manifest의 수신점 구상은 다음과 같다.

```xml
<receiver
    android:name=".gleo.GleoActionReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="CREATE_ROAD_NOTE_DRAFT" />
    </intent-filter>
</receiver>
```

이 XML은 설계 예시이며 발신자 인증 설정이 확정된 최종 Manifest가 아니다. 외부 Gleo가 호출할 수 있어야 하므로 exported 수신점이 필요하지만 action filter나 대상 패키지 지정만으로 발신자가 인증되지는 않는다. 공식 제공 권한·서명/발신자 검증 수단을 확인하고 적용한다. 미확인 상태를 안전한 수신점으로 판정하지 않는다. 후속 게시·차량 제어까지 이 수신점의 신뢰를 확대하지 않는다.

### 문서 불일치와 파싱 원칙

S4 본문은 파라미터를 개별 extra로 넣는다고 설명하지만 코드 예시는 문자열 extra `ACTION_PARAM` 안의 JSON을 읽는다. 송신 예시는 바깥 따옴표도 혼재한다. 이번 설계는 수신 코드 예시의 `ACTION_PARAM` JSON 객체 문자열을 기준안으로 잡되, 구현 첫 단계에서 실제 Gleo Intent의 타입·키·인코딩을 확인한다.

검증 전 임의의 여러 포맷을 동시에 허용하거나 이중 디코딩을 반복하지 않는다. 실제 전달 형식이 다르면 공급자 확인 및 계약/테스트를 먼저 수정한다. 필드 구조만 기록하고 발화·위치 내용은 로그에 남기지 않는다.

수신 처리 규칙(프로젝트 선택):

1. 허용 발신자와 정확한 Action을 확인한다. 인증되지 않은 요청에는 저장하거나 Callback을 보내지 않는다.
2. ACTION_PARAM이 JSON 객체 문자열인지 확인한다. 프로젝트 제한은 UTF-8 8 KiB, content 1~500자, placeQuery 1~200자, draftId 1~128자, directionDescription 1~200자, utteranceRecordedAt 최대 40자다. 문자열은 trim 후 Kotlin String.length 기준이며 선택값은 미제공 시 생략한다.
3. content/locationMode 누락은 asking, 잘못된 타입·enum·형식·한도 초과는 fail이다. 누락값을 CURRENT_ROAD로 기본 설정하지 않는다. 알 수 없는 입력 키는 실행·위치 판단에 쓰지 않는다.
4. NAMED_PLACE는 placeQuery가 있어야 한다. 누락되면 내용과 지명 의도만 미정 초안으로 보관하고 asking한다. 다른 모드에 placeQuery가 함께 있으면 모순 입력으로 fail 처리하며 무시하고 현위치로 게시하지 않는다. NEEDS_CLARIFICATION은 지명이 없는 불명 위치, DEFERRED는 사용자의 명시적인 보류/모름 의도다.
5. draftId가 있으면 인증된 호출 경로·현재 사용자 소유·미게시·미만료 여부를 검증하고 해당 초안만 갱신한다. 유효하지 않으면 fail이며 새 초안으로 조용히 대체하지 않는다. ID는 권한 증명이 아니다.
6. 유효한 내용을 로컬에 내구성 있게 저장한 뒤 초안 상태에 맞춰 success/asking 중 한 번 응답한다. 저장 실패 시 fail이며 “저장했다”고 알리지 않는다.
7. directionMode 생략 시 CURRENT_ROAD는 CURRENT_VEHICLE, 나머지는 UNKNOWN으로 처리한다. CURRENT_VEHICLE은 CURRENT_ROAD에서만 허용한다. USER_DESCRIPTION은 directionDescription이 있어야 하며 없으면 내용을 보관하고 방향을 확인한다. 다른 모드에 directionDescription이 있으면 모순 입력으로 fail이다. 방향 미정은 11절의 기존 미확정 경로를 따른다.
8. utteranceRecordedAt은 실제 시스템 관측값이라는 전달 계약 확인이 선행되어야 한다. 값이 있으면 시간대 포함 ISO 8601을 파싱해 UTC로 정규화한다. 잘못된 형식은 fail이며, 신뢰 출처가 확인되지 않은 시각은 확정 발화 시각으로 채택하지 않는다. 미래 시각·시계 불일치도 검증하고 신뢰할 수 없으면 원발화 시각 미상으로 남긴다. 누락되면 제보를 거부하지 않고 앱 receivedAt만 기록한다.
9. CURRENT_ROAD는 최초 수신 때 사용 가능한 신선한 위치를 한 번 고정한다. 후속 질문·재시도 시 이동한 현위치로 초안을 옮기지 않는다. 정확한 발화 위치라고 설명하지 않는다. 위치가 없거나 오래됐으면 위치 미정으로 보관한다. 모드별 정책과 수신 지역은 11절을 따른다.

## 5. 앱 처리 함수와 Callback function interface

Gleo Interface는 Kotlin interface 구현을 등록하는 SDK 방식이 아니라, **App Schema + Intent 수신 + 응답 Broadcast** 방식이다. 아래는 이 wire contract를 앱에서 처리하기 위한 최소 함수 시그니처 구상이다. 지금 실제 Kotlin 파일은 만들지 않는다.

```kotlin
// 앱 내부 설계 시그니처. Gleo 공식 SDK 타입이 아니다.
fun handleGleoAction(context: Context, intent: Intent)

// 검증한 입력을 로컬 저장. draftId가 있으면 해당 초안만 갱신. 게시 없음.
// 선택값은 wire에서 생략하고 앱 내부에서만 nullable로 표현한다.
fun saveRoadNoteDraft(
    content: String,
    locationMode: String,
    placeQuery: String? = null,
    draftId: String? = null,
    directionMode: String? = null,
    directionDescription: String? = null,
    utteranceRecordedAt: String? = null
): String

// receivedAt 및 차량 방향/측정 시각은 수신·위치 처리에서 앱이 확보한다.
// LLM 입력값으로 받아 차량 센서 관측값을 덮어쓰지 않는다.

// status는 success/asking/fail만 허용. 이 브랜치는 mode=voice로 고정.
// dataJsonArray는 JSON 객체 배열의 문자열 또는 null.
fun sendGleoCallback(
    context: Context,
    functionName: String,
    status: String,
    message: String,
    dataJsonArray: String? = null
)
```

별도 범용 Repository/SDK wrapper/interface 계층은 필요하지 않다. `handleGleoAction`은 수신 검증 → 초안 저장 → 응답 한 번으로 이어진다. `sendGleoCallback`은 S4 응답 Broadcast 형식에 맞춰 JSONObject로 직렬화한다. 문자열 이어 붙이기로 JSON을 만들지 않는다.

전송 계약:

```text
Intent.action = ai.pleos.playground.intent.action.APP_ACTION_RESPONSE
extra[packageName] = com.jinkim.hyundaiAndGretel          (우리 앱의 식별자)
extra[response] = Callback JSON 전체를 직렬화한 문자열
```

S4의 packageName extra는 우리 앱 식별자이며 Gleo 목적지 패키지명이 아니다. Gleo 수신 대상 패키지·수신 권한은 별도 확인한다. 공개 Broadcast 예시를 개인정보 전송 보안의 완성본으로 취급하지 않는다. 대상 제한 방법이 확인되기 전에는 발화·위치 원문을 응답에 포함하지 않는다. 아래 성공 data도 최소 식별자와 처리 상태만 담는다.

## 6. Callback JSON과 위치 확인 대화

S3에 따라 functionName은 수신 Action/Schema 함수명과 일치하고 status/message는 필수다. message는 영어 완전 문장, mode는 voice로 명시한다. Gleo가 최종 음성을 생성하며 앱은 TTS를 재생하지 않는다. 한국어 안내와 후속 질문의 실제 동작은 실기기에서 확인한다. `data`는 JSON 객체 배열을 인코딩한 **문자열**이다.

### 저장 성공: 위치 처리는 아직 대기 중

```json
{
  "functionName": "CREATE_ROAD_NOTE_DRAFT",
  "status": "success",
  "message": "The report draft was saved; location processing is pending and it has not been published.",
  "mode": "voice",
  "data": "[{\"draftId\":\"draft-local-001\",\"draftState\":\"PENDING_LOCATION\",\"published\":false}]"
}
```

CURRENT_ROAD/NAMED_PLACE의 위치 준비가 아직 안 끝난 경우다. success는 초안 저장 성공만 뜻한다. 이 브랜치에는 실제 게시가 없으며 “쪽지를 심었다/주변 차량에 전달했다”고 답하지 않는다.

### 불명확한 위치: 내용을 보관하고 질문

```json
{
  "functionName": "CREATE_ROAD_NOTE_DRAFT",
  "status": "asking",
  "message": "The report draft was saved; which place or landmark was it near?",
  "mode": "voice",
  "data": "[{\"draftId\":\"draft-local-001\",\"draftState\":\"AWAITING_LOCATION\",\"published\":false}]"
}
```

“아까”만 말하거나 장소 후보가 여러 개면 정확한 지명/주변 기준물을 묻는다. 사용자에게 초를 말하도록 요구하지 않는다. CURRENT_ROAD에서 유효 위치를 얻지 못했을 때도 이 경로를 쓴다. content 자체가 없으면 저장 없이 필요한 내용을 묻고 draftId를 반환하지 않는다.

### 모름/보류: 나중에 UI에서 지정

```json
{
  "functionName": "CREATE_ROAD_NOTE_DRAFT",
  "status": "success",
  "message": "The report draft was saved without a location for later placement in the app; it has not been published.",
  "mode": "voice",
  "data": "[{\"draftId\":\"draft-local-001\",\"draftState\":\"AWAITING_LOCATION\",\"published\":false}]"
}
```

DEFERRED에는 다시 위치를 묻지 않는다. 사용자가 답하지 않거나 대화를 닫아도 저장된 초안은 남고 UI에서 이어 간다. 위치를 지정하기 전에는 서버 게시·다른 차량 배포를 하지 않는다.

후속 Action의 draftId는 앱 발급 초안 식별자이며 공식 Gleo requestId가 아니다. Gleo가 Callback의 ID와 내용 전체를 후속 Action에 전달할 수 있는지 미확인이다. 정확한 ID로 연결할 수 있을 때만 같은 초안을 갱신한다. draftId 없이 온 완전한 새 요청은 독립 초안으로 처리하며 중복 가능성을 UI에서 보여 준다. 내용 없는 “모르겠어” 응답이거나 연결이 불명확하면 임의로 최근 초안을 고르지 않고 이미 저장된 항목을 UI에서 이어가도록 한다. 이 대화 연결은 구현 전 실기기 검증 게이트다.

### 실패

```json
{
  "functionName": "CREATE_ROAD_NOTE_DRAFT",
  "status": "fail",
  "message": "The report draft could not be saved.",
  "mode": "voice",
  "data": "[{\"reason\":\"SAVE_FAILED\"}]"
}
```

잘못된 형식은 INVALID_INPUT, 저장 실패는 SAVE_FAILED 등 최소 사유만 쓴다. 예외·내부 경로·제보 원문을 노출하지 않는다. 저장된 후 응답 전송이 실패했다면 초안을 지우지 않는다. 전송 성공도 Gleo 수신·음성 재생 완료 증거는 아니다.

## 7. 응답 시간·저장 수명·중복

S3의 Gleo 취소 기한은 7초다. 프로젝트 내부 목표는 수신 후 최대 5초 내 응답이며 콜드 스타트를 포함한 Gleo 측 실제 수신 시간으로 확인한다. 먼저 로컬 저장을 확정하고 응답한다. 위치 탐색·장소 검색·게시 네트워크를 기다리느라 응답 기한을 넘기지 않는다. goAsync를 쓰더라도 7초가 늘어나지 않는다. 한 수신 실행당 응답은 한 번이며 기한 뒤 추가 성공 Callback을 보내지 않는다.

저장 상태는 PENDING_LOCATION(위치 처리 대기), AWAITING_LOCATION(사용자 위치 지정 필요), READY(앱 위치 정책 검증 완료)로 구분한다. 모두 미게시 상태다. 별도 게시 단계가 서버 저장을 확정해야 published=true가 된다. 위치 처리가 실패하면 AWAITING_LOCATION으로 남기고 UI에서 복구한다. 백그라운드 처리가 끝났다는 이유만으로 Gleo를 다시 발화시키지 않는다.

“나중에 UI” 요구 때문에 메모리 전용 저장은 폐기한다. 앱 전용 로컬 저장소에 사용자별로 초안 ID·내용·모드·장소 표현·상태·생성/수정 시각을 저장한다. CURRENT_ROAD에는 확보한 최초 위치 기준·진행 방향과 그 측정 시각·품질 정보를 필요한 만큼만 더한다. 발화 기록 시각(제공될 때), 최초 수신 시각, 생성/수정/게시 시각은 서로 구분한다. 후속 질문이나 UI 편집 때 원제보 발화 시각·최초 수신 시각을 덮어쓰지 않으며, 연결된 후속 발화 시각도 원제보 시각으로 대체하지 않는다. 앱 재시작 후에도 목록에서 확인·위치 지정·삭제할 수 있어야 한다. 로그/분석에 내용·좌표를 남기지 않고 원시 음성·전체 이동 이력은 수집하지 않는다. 민감 초안의 저장 보호와 백업 제외, 운전자별 접근 분리 및 동의 철회/계정 삭제 시 삭제를 설계한다. 보유기간과 자동 만료 기간은 구현 전 결정·고지해야 하며 무기한 보존하지 않는다. 사용자가 보관 중인 초안을 운전자 전환만으로 지우는 대신 다른 운전자에게 보이지 않게 격리한다.

공식 예시에는 요청 상관 ID·취소 이벤트 계약이 없다. 유효한 draftId 갱신은 같은 초안을 대상으로 하지만 최초 요청 재전달과 새로운 제보를 완벽히 구분하지 못한다. 동일 텍스트만으로 제보를 삭제하지 않는다. 실제 게시에는 별도 멱등 키·서버 중복 방지가 필요하다.

## 8. 등록·구현 순서와 영향 범위

1. 실제 Gleo 가용성·Schema 검토/활성화·Intent 형식·발신자 인증·Callback 대상 제한과 후속 draftId 전달을 확인한다.
2. assets/schema.json·Manifest Action 필터·Receiver·로컬 초안 저장/응답을 최소 구현한다. 일반 ComponentActivity/Compose는 유지한다. applicationId는 사용자 지정 목표 식별자 com.jinkim.hyundaiAndGretel과 일치시키는 별도 변경이 선행되어야 한다. service는 sendBroadcast 경로이며 상주 Service를 뜻하지 않는다.
3. 초안 목록·삭제·위치 지정 진입을 제공하고 앱 재시작 후 복구를 확인한다. 실제 지도 선택 UI는 위치 단계에서 연결한다. 주행 중 지도 조작 가능 여부를 별도 Pleos 디자인/규제 검토 없이 허용하지 않는다.
4. Schema 승인·실제 스캔/갱신과 음성 → Action → 저장 → Callback → 음성 안내를 확인한다. 파일 생성/ADB 설치만으로 활성화 완료를 주장하지 않는다.
5. 다음 단계에서 현도로 주변 구역/지명 검색·UI 배치·서버 게시·수신 필터를 연결한다.

예정 파일은 assets/schema.json, AndroidManifest.xml, gleo/GleoActionReceiver.kt와 최소 초안 저장/응답 코드, MainActivity.kt의 초안 UI, 핵심 검증 테스트다. 저장은 기존 의존성과 플랫폼 기능을 확인한 뒤 최소 수단을 고른다. STT/TTS·녹음·별도 LLM·불필요한 SDK는 추가하지 않는다. 현재 이 문서의 계약만 변경했으며 코드/설정은 구현 전이다.

## 9. 설계 피드백과 주의점

목격 즉시 제보가 주 시나리오라면 자연어의 정밀도를 강제하지 않고 주변 구역 제보로 처리하는 편이 목적에 맞다. 초 환산과 과거 경로 복원은 핵심 흐름에서 제거한다. Gleo는 의도/장소 표현을 전달하고, 좌표·도로·범위·초안 상태·게시 여부는 앱이 검증한다.

다만 “넓은 구역”은 “정확한 위험 좌표”와 다르다. 사용자가 현위치 주변에 대략 배치된 제보임을 알 수 있어야 하며, 수신자에게도 “주변 도로에 제보”처럼 안내하고 정밀한 장애물 위치를 단정하지 않는다. 반경만 키우면 옆 도로·고가/지하·반대 방향 차량까지 오경고를 받으므로 도로 맥락 필터와 범위 상한이 필요하다. 지명도 검색 후보를 얻는 단서이며 출구·교차로의 어느 접근로인지까지 자동 확정되는 것은 아니다.

나중에 배치할 때는 낙하물처럼 이미 사라졌을 수 있는 내용이 새 제보처럼 노출되지 않게 재확인한다. 생성 시각과 게시 시각을 분리하고, 생성 시각을 실제 목격 시각으로 위장하지 않는다. 유형별 유효기간·오래된 초안 재확인 기준은 게시 단계에서 정한다.

선제 음성 경고는 명령 Callback과 별도다. 이 브랜치에서 앱 STT/TTS를 추가하지 않으며 차량 접근 시 공식 Gleo 선제 안내 경로가 있는지는 후속 게이트로 유지한다.

## 10. 검증 계획 및 상태

| 검증 | 기대 결과 | 현재 |
|---|---|---|
| 앱 식별자 | namespace/소스 package/설치 applicationId/Schema 일치 | FAIL: Gradle applicationId가 이전 값으로 남아 있음 |
| Schema/Action/Callback 예시 | 필드·모드 조건·함수명 일치, data 내부 JSON 배열 | 문서 자동 검사 |
| 자연어 분기 | 방금/10초 전 → CURRENT_ROAD, 지명 → NAMED_PLACE, 아까만 → 질문, 모름 → DEFERRED | NOT_TESTED: 실기기 |
| 문맥·충돌 | 아까+지명은 장소 우선, 불명/장시간 과거를 임의로 현위치화하지 않음 | NOT_TESTED |
| 수신 신뢰/입력 검증 | 인증·모드·타입·한도·초안 소유권 검증 | NOT_TESTED |
| 저장 내구성 | asking 전 저장, 재시작 복구, 삭제·격리·철회 처리, 저장 실패 안내 | NOT_TESTED |
| 후속 대화 | 정확한 ID 갱신, 모름이면 질문 종료, 중단해도 UI 복구 | NOT_TESTED |
| 기한/중복 | 콜드 스타트 포함 7초 내 응답, 중복 게시 없음 | NOT_TESTED |
| 위치/배포 | 신선도·지명 후보·도로 맥락·반경 상한·만료·오프라인 처리 | NOT_TESTED: 후속 단계 |
| 방향/시각 | 실시간 차량 방향 고정, 정차/오래된 방향 미정, 발화 시각과 수신 시각 구분·누락·시간대·미래값 처리 | NOT_TESTED |
| 규제/UI | 주행 중 위치 지정 제한·접근성·차량 폼팩터 확인 | NOT_TESTED: UI 구현 전 |

harness·문서 JSON·보고 형식 검사와 실제 Gleo/차량/서버 기능 검증을 구분한다. 수동 Intent 주입은 앱 수신 테스트일 뿐 음성 연동 완료 증거가 아니다.

## 11. 위치 처리 정책: 현도로 주변·지명·질문·보류

2026-11-08 사용자 결정. 이전의 발화 시각 기반 초 역산·최근 2분 이동 이력 필수 제안을 대체한다. 위치를 모른다고 내용을 버리거나 반복 질문하지 않는다.

| 사용자 표현/문맥 | locationMode | 앱 처리 |
|---|---|---|
| 방금, 막, 여기, 목격 직후 문맥의 10초 전 | CURRENT_ROAD | 최초 수신 시 신선한 현위치와 주행 도로를 기준으로 넉넉한 주변 구역 준비 |
| 뱅뱅사거리, 역삼역 4번출구 쪽 | NAMED_PLACE | 지명과 수식어로 실제 장소 후보 검색, 해당 지역에 배치 |
| 아까, 어디였더라, 위치 없는 과거 제보 | NEEDS_CLARIFICATION | 내용 저장 → 지명/주변 기준물 질문 |
| 위치 모름, 나중에 지정, 일단 저장 | DEFERRED | 위치 없이 보관 → 대화 종료 → 나중에 UI 배치 |

시간 숫자를 요구하거나 “방금”을 특정 초로 변환하지 않는다. 목격 직후이고 위치 언급이 없는 문맥은 CURRENT_ROAD로 분류할 수 있지만, 과거/불명 문맥을 현위치로 억지 분류하지 않는다. “아까 역삼역 4번출구”는 NAMED_PLACE, “방금 뱅뱅사거리”도 명시 장소 우선이다. 서로 다른 장소가 충돌하면 질문한다. 긴 과거 시간이나 이미 도로를 이탈했다는 단서가 있으면 현재 도로 기준으로 넓게 배치하지 않는다. 경계 사례는 실발화 평가로 조정한다.

### CURRENT_ROAD: 현위치 기반 근사 구역

앱이 최초 Action을 받을 때 확보한 신선한 위치·측정 시각·사용 가능한 방향/품질을 기준으로 도로 주변 영역을 만든다. 이후 처리 중 차량이 이동해도 쪽지 영역이 따라 움직이지 않게 고정한다. 실제 관측 좌표가 아니라 **수신 시점 현도로 주변의 근사 제보**라는 제품 선택이다. 이전 도로를 특정하는 별도 설명이 있으면 그 단서를 확인하고 무시하지 않는다.

단일 원형 반경만으로 전 차량에 보내기보다는 현재 도로의 주변 구간을 후보로 두고 수신 차량의 도로·접근 방향을 비교한다. 단순 원형 조회는 서버 후보 조회에 쓸 수 있으나 최종 수신 판정과 구분한다. 길/방향이 확인되지 않으면 반대 차로를 추측하거나 모든 인접 도로로 확대하지 않고 위치 확인/미정 초안으로 남긴다. 반대편이라는 명시 표현은 별도 확인 없이 차량 진행 방향으로 바꾸지 않는다. directionMode=USER_DESCRIPTION과 directionDescription에 보존하고 앱이 해당 도로 방향을 검증한다.

반경/구간 길이는 Gleo 인자가 아니라 앱 정책이다. 도로 특성·위치 품질·지연 평가로 상한을 정하고, 오차가 크다고 무제한 넓히지 않는다. 시내/고속도로·교차로 직후·평행도로·고가/지하에서 검증한다. 예를 들어 100km/h로 30초 이동하면 약 833m이므로 “넉넉한 반경”만으로 원목격 지점 포함을 보장하지 않는다. 지연이 알려져 상한을 넘거나 도로 문맥이 깨지면 질문/보류한다. 원발화 시각이 없으면 전달 지연 자체를 앱이 완전히 검출할 수 없다는 위험을 유지한다. utteranceRecordedAt 전달 필드를 추가하지만 실제 시스템 시각 제공이 확인되지 않으면 생략하며, 메인 흐름의 선행 필수조건으로 강제하지 않는다.

### 진행 방향과 시각 데이터 계약

CURRENT_ROAD의 기본 진행 방향은 **최초 수신 시점 현재 차량의 신선한 이동 방향**이다. 위치와 함께 한 번 고정하고 차량이 회전/유턴하거나 후속 답변을 해도 쪽지 방향이 따라 바뀌지 않게 한다. 차량 이동 방향은 수신 대상 도로 흐름에 대한 근사 기준이며 위험물이 놓인 차로를 확정하는 값이 아니다. 정차/저속·터널·센서 결측·오래된 샘플에서 유효 방향을 얻지 못하면 0도(북쪽)로 채우지 않고 미정으로 보관한다. 명시적으로 반대편 등을 말하면 USER_DESCRIPTION으로 보존하고 도로를 검증한다. NAMED_PLACE/과거/보류 제보는 현차량 방향을 자동으로 적용하지 않고 명시 방향 또는 나중에 UI에서 확인한 도로 방향을 사용한다.

Gleo Action은 방향의 기준/설명과 전달 가능한 실제 발화 기록 시각을 보내고, 앱은 아래처럼 관측 데이터를 보강해 저장한다. 다음은 **앱 내부 초안 JSON 예시**이며 Gleo가 생성하거나 Callback으로 돌려보낼 JSON이 아니다.

```json
{
  "draftId": "draft-local-001",
  "content": "도로에 구덩이가 있음",
  "locationMode": "CURRENT_ROAD",
  "directionMode": "CURRENT_VEHICLE",
  "travelHeadingDegrees": 135.0,
  "directionCapturedAt": "2026-11-08T03:00:19Z",
  "utteranceRecordedAt": "2026-11-08T03:00:00Z",
  "receivedAt": "2026-11-08T03:00:20Z",
  "draftState": "PENDING_LOCATION",
  "published": false
}
```

| 필드 | 생성 주체·의미 |
|---|---|
| travelHeadingDegrees | 앱의 수치 방향 계약: 진북 0°, 동 90°, 남 180°, 서 270°, 유한수 0 이상 360 미만. SDK 단위/북 기준을 확인한 뒤 정규화하며 LLM이 생성하지 않음 |
| directionCapturedAt | 방향 샘플의 실제 측정 시각, UTC ISO 8601. 방향값과 함께 저장하며 측정 시각을 모르면 수신 시각으로 위장하지 않음 |
| utteranceRecordedAt | 음성 시스템이 원제보 발화 시작을 기록한 시각, UTC ISO 8601. 목격 시각·쪽지 게시 시각과 다름. 검증된 전달 경로가 없으면 생략 |
| receivedAt | 앱이 최초 제보 Action을 받은 시각, 앱이 항상 기록. 발화 시각 결측을 이 값으로 대체하지 않음 |

사용자는 날짜/시각/각도를 말할 필요가 없다. 원발화 시각은 App Schema에 이름만 추가한다고 확보되지 않으므로, 실제 시스템 관측 메타데이터를 Action에 넣는 방법을 Gleo 공급자/실기기에서 확인해야 한다. 전달 가능 여부가 확인되기 전에는 utteranceRecordedAt을 선택값으로 유지한다. 기기 시계 차이 허용치는 구현 전 정하며, 시각만으로 지연을 확정하려면 두 시계가 비교 가능한지도 확인한다. 앱 내부 대기 시간 측정은 단조 시계를 사용한다.

### NAMED_PLACE: 명시 장소 중심 구역

앱의 장소 검색/지오코딩으로 placeQuery를 해석한다. LLM이 위도/경도·지도 장소 ID·신뢰도를 생성하지 않는다. 출구 번호와 “쪽/앞/지난 뒤” 수식어를 보존하고 실제 후보와 관련 도로를 확인한다. 현재 위치는 동명이소 후보 정렬에만 참고할 수 있고 사용자 명시 장소를 현위치로 대체하지 않는다. 후보가 다수이거나 지도에 해당 출구/도로가 없으면 장소를 재확인하며 실패를 현위치 배치로 돌리지 않는다. 장소 검색 제공자·API 지원·라이선스는 미선정이다.

### 임시저장과 나중에 배치

불명확한 제보는 질문 전에 AWAITING_LOCATION으로 저장한다. 사용자가 모르면 DEFERRED로 끝내고 초안 목록에서 위치를 선택하게 한다. 앱이 종료돼도 복구되어야 하며, 나중에 위치를 정한 뒤 내용의 현재 유효성을 확인하고 별도 게시 절차를 거친다. 주행 중 지도 핀 조작은 규제 검토 전 허용한다고 가정하지 않는다.

### 공식 API·운영 경계

공개 Gleo 계약은 발화 시각·주행 이력 제공을 보장하지 않는다. 이것으로 Gleo 내부가 이력을 저장하지 않는다고 단정하지 않는다. 이 설계에서는 과거 궤적 수집을 필수로 하지 않는다.

[FusedLocation 위치 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)의 getLastKnownLocation은 새 측정이 아니며 null일 수 있다. 앱은 신선한 위치인지 확인하고, 필요한 경우 업데이트 콜백으로 위치를 받으며 null/onError를 처리한다. LocationData의 시각 단위·시계/품질 의미·콜백 빈도는 설치 SDK에서 확인한다. 수신 시각을 측정 시각으로 바꾸거나 신뢰도를 임의의 m 단위 정확도로 해석하지 않는다.

[권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)과 지원 환경을 확인하고 initialize 후 사용하며 동일 listener 해제와 release를 지킨다. 권한 거절·차단·미지원·오래된 위치는 미정 초안 경로다. Google Fused Location 동작이나 백그라운드 상시 수집을 Pleos 보장으로 해석하지 않는다. [개인정보 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy)에 따라 목적·저장 항목·보유기간·삭제·공유를 고지하고 동의받는다. 전체 경로 수집·외부 전송은 추가하지 않는다.

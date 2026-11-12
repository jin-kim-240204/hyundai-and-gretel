> 현재 E2E 구현 정본은 [즉시 도로 제보 프로토타입 테크스펙](implementation-plan.md)이다. 수신 30초 전 위치 버퍼·Room A/B 심기/줍기·단발 3필드 스키마는 해당 문서를 따른다. 이 문서의 이후 상세 예시 중 정본과 다른 지명·후속 대화·목격 시각 정책은 과거 설계 기록이다.

> 현행 단발 계약: 입력은 content/locationMode/category. draftId·asking·후속 대화 문맥 없음. 아래 추가 정보/지명/보류 예시는 과거 설계다.

> 최신 프로토타입: observedAt 입력/시간 질문/10분 검증 제거. 앱 수신 receivedAt을 기준으로 한다. 아래 과거 시각 시나리오는 현행 요구사항에서 제외한다.

> 2026-11-09 현재 계약: CREATE_ROAD_NOTE, Gleo 노출 locationMode는 CURRENT_ROAD만 지원한다. 아래 지명·보류 시나리오는 향후 기능 설계이며 이번 주 구현 범위에서 제외한다. 방향 조회는 아직 미구현이다.

# 이번 브랜치: Gleo 음성 명령 연동 설계

수정·관련 공식 원문 재확인: 2026-11-08. 상태: Sprint 1 로컬 구현 반영, 실제 Gleo 외부 연동 검증 전. 저장소 현황 대조: 2026-11-08.
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

현재 저장소는 ComponentActivity/Compose 동의·초안 목록/삭제 UI, Vehicle 2.3.2 의존성, Gleo Schema/파서/처리 엔진/Callback 및 내부 전용 Receiver, Keystore/AES-GCM 로컬 저장을 포함한다. 차량 SDK 초기화/조회/제어, 위치·서버 게시/공유는 미구현이다. 기존 Manifest 도어/창문 권한 4개는 선언 상태이며 차량 기능/런타임 승인 증거가 아니다. allowBackup=false와 adjustResize는 유지한다.

소스 package/Gradle namespace/applicationId와 패키지 예제는 모두 `com.jinkim.hyundaiAndGretel`로 정합했다. versionName=1.0이다. release Receiver는 exported=false다. 현재 debug는 사용자 요청에 따라 외부 Gleo 테스트 Action을 받아 저장 없이 검증/Callback만 수행하고, 설치 확인한 ai.umos.ambientai로 응답을 제한한다. Action 발신자 인증과 실제 Gleo 수신/음성 E2E는 미검증이다. debug 화면에서 최근 입력·출력 JSON을 확인하며, GleoProbe logcat에는 수신/처리 완료·문자 수·처리 시간만 기록한다. 실제 Manifest는 빌드 타입 placeholder로 debug=true/release=false를 분리한다. 아래 exported=true XML은 외부 테스트 수신 형식의 예시다. 실제 구현/담당·확인 범위는 [Sprint 1 명세](sprint-1-gleo-drafts.md)와 [자비스 기록](../work/sprint-1-jarvis.md)을 따른다.

공식 의무와 프로젝트 설계안을 구분한다. 아래 `CREATE_ROAD_NOTE`, 필드명, 입력 제한, 앱 내부 함수 시그니처는 프로젝트가 정한 제안이다. Gleo가 제공하는 SDK 클래스나 등록된 공식 도로 제보 API가 아니다.

## 3. App Schema 초안

Sprint 1에서 `app/src/main/assets/schema.json`에 구현했다. 아래 예시와 필드 계약을 일치시킨다.

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "invokeWord": "제보",
  "appDescription": "An app for reporting road conditions and sharing them with other drivers.",
  "version": "1.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "component": "service",
        "name": "CREATE_ROAD_NOTE",
        "description": "Report a road condition the user has just observed on the current road.",
        "parameters": {
          "type": "object",
          "properties": {
            "content": {
              "type": "string",
              "description": "The road condition observed by the user."
            },
            "locationMode": {
              "type": "string",
              "enum": [
                "CURRENT_ROAD"
              ],
              "description": "The current road where the user has just observed the reported condition."
            },
            "category": {
              "type": "string",
              "enum": [
                "WARNING",
                "INFORMATION"
              ],
              "description": "The report category classified from the content: WARNING for road hazards or conditions requiring safety precautions, or INFORMATION for other road condition information."
            }
          },
          "required": [
            "content",
            "locationMode",
            "category"
          ]
        }
      }
    }
  ]
}
```

| 항목 | 구성 이유 |
|---|---|
| packageName / version | namespace/applicationId와 일치하는 com.jinkim.hyundaiAndGretel 및 versionName=1.0 사용. 마켓 등록 버전은 미검증 |
| 함수 한 개 | 이번 브랜치에 필요한 초안 생성만 노출. 공식 최대 3개를 채우기 위한 기능은 만들지 않음 |
| CREATE_ROAD_NOTE | 대문자·밑줄 규칙을 지키고 게시와 초안을 명확히 구분 |
| component=service | UI를 열지 않는 명령 수신. 공식적으로 이 값은 startService가 아니라 sendBroadcast를 뜻함 |
| content | 원문 전체 녹취 대신 사용자 관찰 내용만 전달받음. 실제 원문과 동일하다고 가정하지 않음 |
| locationMode | Gleo가 위치 처리 의도를 분류. 사용자는 enum이나 초를 말할 필요 없음 |
| placeQuery | NAMED_PLACE에서 필요한 지명·출구·전후 관계. 실제 좌표는 앱의 장소 검색으로 결정 |
| directionMode | CURRENT_VEHICLE로 앱이 발견 시각의 차량 진행방향 이력을 사용. 명시 방향은 USER_DESCRIPTION, 불명은 UNKNOWN |
| directionDescription | 사용자 명시 방향을 보존. USER_DESCRIPTION에서 필요하며 실제 도로 방향과 대조 |
| observedAt | Gleo가 실제 현재 시각에서 사용자 상대 시간을 빼서 계산한 발견 시각(UTC ISO 8601). 방금은 현재 시각. CURRENT_ROAD에서는 필수이며 센서 실측 시각으로 보장하지 않음 |
| draftId | 기존 초안 후속 수정에만 사용. 앱 발급값을 확인하며 임의로 생성하지 않음 |
| content + locationMode 필수 | 내용과 처리 분기만 공통 필수. 지명/ID의 조건부 검증은 앱에서 수행 |

S2에 따라 description은 영어이며 구체적인 발화 예시를 넣지 않았다. 파라미터는 모두 string, enum도 문자열만 쓴다. nullable union·중첩 object·조건부 JSON Schema 기능은 사용하지 않는다. 선택값이 없으면 키를 생략하고 빈 문자열/null로 대체하지 않는다. 필수값 충족도 보장되지 않으므로 앱이 재검증한다.

사용자 요청으로 공식 예시의 `invokeWord` 필드에 `제보`를 시험 적용한다. 의도한 발화 시작은 “제보할게”이며, 공개 필드 명세에는 이 필드의 의미·일치 방식·강제 라우팅 보장이 없다. 함수 선택 효과는 실제 classifier/Action 로그로 확인하기 전 NOT_TESTED다.

위험/경고/안내/정보 분류는 후속 작업이다. secondsBeforeUtterance 등 초 수치화 필드는 사용하지 않는다. 위치 분기와 근사 배치 기준은 11절을 따른다.

## 4. Gleo → 앱 Action JSON 및 Android 수신

예상 사용자 발화(테스트 시나리오): “방금 도로에 구덩이 봤어. 제보해 줘.”

Gleo가 구성할 논리적 Action 형태는 S4를 따른다. 실제 LLM의 표현·함수 선택은 실기기에서 확인한다.

locationMode는 사용자 표현을 앱 처리 분기로 바꾸는 프로젝트 계약이다. LLM은 좌표·반경을 생성하지 않는다. 발견 시각은 실제 현재 시각과 상대 시간으로 계산하며 센서 실측값으로 간주하지 않는다.

### 목격 직후: 현재 도로 주변

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE",
  "actionParameters": {
    "content": "도로에 구덩이가 있음",
    "locationMode": "CURRENT_ROAD",
    "directionMode": "CURRENT_VEHICLE",
    "observedAt": "2026-11-08T03:00:00Z"
  }
}
```

“10초 전”은 Gleo가 실제 현재 시각에서 10초를 뺀 발견 시각을 observedAt으로 전달한다. 10분 미만만 CURRENT_ROAD이며 정확히 10분부터는 지명이 있으면 NAMED_PLACE, 없어도 NAMED_PLACE로 보내 지명을 질문한다. CURRENT_VEHICLE은 앱이 해당 발견 시각의 차량 위치·방향 이력을 찾는 의도이며 Gleo가 각도를 생성하지 않는다.

### 명시 지명: 해당 장소 주변

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE",
  "actionParameters": {
    "content": "도로에 낙하물이 있음",
    "locationMode": "NAMED_PLACE",
    "placeQuery": "역삼역 4번출구 쪽",
    "directionMode": "UNKNOWN",
    "observedAt": "2026-11-08T03:00:00Z"
  }
}
```

“뱅뱅사거리”도 NAMED_PLACE다. 역 중심점으로 출구/방향 수식어를 잃지 않으며 앱이 장소 후보를 검증한다.

### 위치 불명: 저장 후 질문

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE",
  "actionParameters": {
    "content": "도로에 낙하물이 있었음",
    "locationMode": "NAMED_PLACE",
    "directionMode": "UNKNOWN",
    "observedAt": "2026-11-08T03:00:00Z"
  }
}
```

앱은 기존 내용을 pendingReport에 보존한 asking으로 장소를 묻고 완성 전에는 최종 저장하지 않는다. “아까 뱅뱅사거리”처럼 지명이 있으면 NAMED_PLACE이며, 단어 하나보다 전체 문맥을 우선한다.

### 모름/나중에 지정: 저장 후 대화 종료

```json
{
  "packageName": "com.jinkim.hyundaiAndGretel",
  "action": "CREATE_ROAD_NOTE",
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
        <action android:name="com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE" />
    </intent-filter>
</receiver>
```

이 XML은 설계 예시이며 발신자 인증 설정이 확정된 최종 Manifest가 아니다. 외부 Gleo가 호출할 수 있어야 하므로 exported 수신점이 필요하지만 action filter나 대상 패키지 지정만으로 발신자가 인증되지는 않는다. 공식 제공 권한·서명/발신자 검증 수단을 확인하고 적용한다. 미확인 상태를 안전한 수신점으로 판정하지 않는다. 후속 게시·차량 제어까지 이 수신점의 신뢰를 확대하지 않는다.

### 문서 불일치와 파싱 원칙

실기기 로그에서 Gleo는 Android Action에 패키지명을 붙여 `com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE`로 전송했다. Manifest/Receiver는 이 관측 형식을 사용하고 Schema/Callback 함수명은 기본 이름을 유지한다. [단계별 로그 확인](gleo-debugging.md)을 참고한다.


S4 본문은 파라미터를 개별 extra로 넣는다고 설명하지만 코드 예시는 문자열 extra `ACTION_PARAM` 안의 JSON을 읽는다. 송신 예시는 바깥 따옴표도 혼재한다. 이번 설계는 수신 코드 예시의 `ACTION_PARAM` JSON 객체 문자열을 기준안으로 잡되, 구현 첫 단계에서 실제 Gleo Intent의 타입·키·인코딩을 확인한다.

검증 전 임의의 여러 포맷을 동시에 허용하거나 이중 디코딩을 반복하지 않는다. 실제 전달 형식이 다르면 공급자 확인 및 계약/테스트를 먼저 수정한다. 필드 구조만 기록하고 발화·위치 내용은 로그에 남기지 않는다.

수신 처리 규칙(프로젝트 선택):

1. 허용 발신자와 정확한 Action을 확인한다. 인증되지 않은 요청에는 저장하거나 Callback을 보내지 않는다.
2. ACTION_PARAM이 JSON 객체 문자열인지 확인한다. 프로젝트 제한은 UTF-8 8 KiB, content 1~500자, placeQuery 1~200자, draftId 1~128자, directionDescription 1~200자, observedAt 최대 40자다. 문자열은 trim 후 Kotlin String.length 기준이며 선택값은 미제공 시 생략하며 빈 문자열/공백도 미제공으로 정규화한다.
3. content/locationMode 누락은 asking, 잘못된 타입·enum·형식·한도 초과는 fail이다. 누락값을 CURRENT_ROAD로 기본 설정하지 않는다. 알 수 없는 입력 키는 실행·위치 판단에 쓰지 않는다.
4. NAMED_PLACE는 placeQuery가 있어야 한다. 누락되면 내용과 지명 의도를 pendingReport에 담아 asking하며 최종 저장/성공 데이터 반환은 보류한다. 다른 모드에 비어 있지 않은 placeQuery가 함께 있으면 모순 입력으로 fail 처리하며 무시하고 현위치로 게시하지 않는다. 지명이 없는 과거 제보도 NAMED_PLACE로 분류하고, DEFERRED는 사용자의 명시적인 보류/모름 의도다.
5. draftId가 있으면 인증된 호출 경로·현재 사용자 소유·미게시·미만료 여부를 검증하고 해당 초안만 갱신한다. 유효하지 않으면 fail이며 새 초안으로 조용히 대체하지 않는다. ID는 권한 증명이 아니다.
6. 필수값이 미완성이면 질문·missingField·pendingReport를 담은 asking을 즉시 반환한다. 필수값 완성 후에만 로컬 저장 및 success와 최종 데이터를 반환한다. 저장 실패 시 fail이며 “저장했다”고 알리지 않는다.
7. directionMode 생략 시 CURRENT_ROAD는 CURRENT_VEHICLE, 나머지는 UNKNOWN으로 처리한다. CURRENT_VEHICLE은 CURRENT_ROAD에서만 허용한다. USER_DESCRIPTION은 directionDescription이 있어야 하며 없으면 내용을 보관하고 방향을 확인한다. 다른 모드에 directionDescription이 있으면 모순 입력으로 fail이다. 방향 미정은 11절의 기존 미확정 경로를 따른다.
8. observedAt은 이름을 호환 유지한 발견 시각이다. 시간대 포함 ISO 8601을 UTC로 정규화하고 형식 오류/미래 시각은 fail이다. CURRENT_ROAD에서 누락 또는 빈 값이면 asking(observedAt), 수신 대비 10분 이상인 CURRENT_ROAD는 모드를 바꾸거나 지명을 묻지 않고 fail(STALE_CURRENT_ROAD)로 반환한다. 오래된 제보는 Gleo가 NAMED_PLACE로 분류해야 한다. 유효 시각은 receivedAt과 별도로 저장한다.
9. CURRENT_ROAD는 발견 시각의 차량 위치·진행방향 이력을 기준으로 근사 영역을 고정한다. 이후 이동한 현재 위치로 대체하지 않는다. 이력/품질이 부족하면 위치를 확인하거나 미정 초안으로 남긴다. 실제 이력 조회·배포는 후속 구현이다.

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
    observedAt: String? = null
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
  "functionName": "CREATE_ROAD_NOTE",
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
  "functionName": "CREATE_ROAD_NOTE",
  "status": "asking",
  "message": "The report draft was saved; which place or landmark was it near?",
  "mode": "voice",
  "data": "[{\"draftId\":\"draft-local-001\",\"draftState\":\"AWAITING_LOCATION\",\"published\":false}]"
}
```

“아까”만 말하거나 장소 후보가 여러 개면 정확한 지명/주변 기준물을 묻는다. 사용자에게 초를 말하도록 요구하지 않는다. CURRENT_ROAD에서는 지명을 묻지 않으며 시각/명시 방향의 누락 항목만 요청한다. content 자체가 없으면 저장 없이 필요한 내용을 묻고 draftId를 반환하지 않는다.

### 모름/보류: 나중에 UI에서 지정

```json
{
  "functionName": "CREATE_ROAD_NOTE",
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
  "functionName": "CREATE_ROAD_NOTE",
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

“나중에 UI” 요구 때문에 메모리 전용 저장은 폐기한다. 앱 전용 로컬 저장소에 사용자별로 초안 ID·내용·모드·장소 표현·상태·생성/수정 시각을 저장한다. CURRENT_ROAD에는 확보한 최초 위치 기준·진행 방향과 그 측정 시각·품질 정보를 필요한 만큼만 더한다. 계산된 발견 시각, 최초 수신 시각, 생성/수정/게시 시각은 서로 구분한다. 후속 질문이나 UI 편집 때 원제보 발견 시각·최초 수신 시각을 덮어쓰지 않으며, 연결된 후속 발화 시각도 원제보 시각으로 대체하지 않는다. 앱 재시작 후에도 목록에서 확인·위치 지정·삭제할 수 있어야 한다. 로그/분석에 내용·좌표를 남기지 않고 원시 음성·전체 이동 이력은 수집하지 않는다. 민감 초안의 저장 보호와 백업 제외, 운전자별 접근 분리 및 동의 철회/계정 삭제 시 삭제를 설계한다. 보유기간과 자동 만료 기간은 구현 전 결정·고지해야 하며 무기한 보존하지 않는다. 사용자가 보관 중인 초안을 운전자 전환만으로 지우는 대신 다른 운전자에게 보이지 않게 격리한다.

공식 예시에는 요청 상관 ID·취소 이벤트 계약이 없다. 유효한 draftId 갱신은 같은 초안을 대상으로 하지만 최초 요청 재전달과 새로운 제보를 완벽히 구분하지 못한다. 동일 텍스트만으로 제보를 삭제하지 않는다. 실제 게시에는 별도 멱등 키·서버 중복 방지가 필요하다.

## 8. 등록·구현 순서와 영향 범위

1. 실제 Gleo 가용성·Schema 검토/활성화·Intent 형식·발신자 인증·Callback 대상 제한과 후속 draftId 전달을 확인한다.
2. assets/schema.json·Manifest Action 필터·Receiver·로컬 초안 저장/응답을 최소 구현한다. 일반 ComponentActivity/Compose를 유지하고 applicationId를 com.jinkim.hyundaiAndGretel과 일치시켰다. service는 sendBroadcast 경로이며 상주 Service를 뜻하지 않는다.
3. 초안 목록·삭제·위치 지정 진입을 제공하고 앱 재시작 후 복구를 확인한다. 실제 지도 선택 UI는 위치 단계에서 연결한다. 주행 중 지도 조작 가능 여부를 별도 Pleos 디자인/규제 검토 없이 허용하지 않는다.
4. Schema 승인·실제 스캔/갱신과 음성 → Action → 저장 → Callback → 음성 안내를 확인한다. 파일 생성/ADB 설치만으로 활성화 완료를 주장하지 않는다.
5. 다음 단계에서 현도로 주변 구역/지명 검색·UI 배치·서버 게시·수신 필터를 연결한다.

예정 파일은 assets/schema.json, AndroidManifest.xml, gleo/GleoActionReceiver.kt와 최소 초안 저장/응답 코드, MainActivity.kt의 초안 UI, 핵심 검증 테스트다. 저장은 기존 의존성과 플랫폼 기능을 확인한 뒤 최소 수단을 고른다. STT/TTS·녹음·별도 LLM·불필요한 SDK는 추가하지 않는다. Sprint 1의 로컬 구현을 연결했으며 실제 외부 Gleo 활성화·위치/게시·실환경 검증은 미완료다.

## 9. 설계 피드백과 주의점

목격 즉시 제보가 주 시나리오라면 자연어의 정밀도를 강제하지 않고 주변 구역 제보로 처리하는 편이 목적에 맞다. 초 환산과 과거 경로 복원은 핵심 흐름에서 제거한다. Gleo는 의도/장소 표현을 전달하고, 좌표·도로·범위·초안 상태·게시 여부는 앱이 검증한다.

다만 “넓은 구역”은 “정확한 위험 좌표”와 다르다. 사용자가 현위치 주변에 대략 배치된 제보임을 알 수 있어야 하며, 수신자에게도 “주변 도로에 제보”처럼 안내하고 정밀한 장애물 위치를 단정하지 않는다. 반경만 키우면 옆 도로·고가/지하·반대 방향 차량까지 오경고를 받으므로 도로 맥락 필터와 범위 상한이 필요하다. 지명도 검색 후보를 얻는 단서이며 출구·교차로의 어느 접근로인지까지 자동 확정되는 것은 아니다.

나중에 배치할 때는 낙하물처럼 이미 사라졌을 수 있는 내용이 새 제보처럼 노출되지 않게 재확인한다. 생성 시각과 게시 시각을 분리하고, 생성 시각을 실제 목격 시각으로 위장하지 않는다. 유형별 유효기간·오래된 초안 재확인 기준은 게시 단계에서 정한다.

선제 음성 경고는 명령 Callback과 별도다. 이 브랜치에서 앱 STT/TTS를 추가하지 않으며 차량 접근 시 공식 Gleo 선제 안내 경로가 있는지는 후속 게이트로 유지한다.

## 10. 검증 계획 및 상태

| 검증 | 기대 결과 | 현재 |
|---|---|---|
| 앱 식별자 | namespace/소스 package/applicationId/Schema 일치 | PASS: 소스/설정 정합, 설치 실행은 NOT_TESTED |
| Schema/Action/Callback 예시 | 필드·모드 조건·함수명 일치, data 내부 JSON 배열 | 문서 자동 검사 |
| 자연어 분기 | 방금/10초 전 → CURRENT_ROAD, 지명 → NAMED_PLACE, 아까만 → 질문, 모름 → DEFERRED | NOT_TESTED: 실기기 |
| 문맥·충돌 | 아까+지명은 장소 우선, 불명/장시간 과거를 임의로 현위치화하지 않음 | NOT_TESTED |
| 수신 신뢰/입력 검증 | 인증·모드·타입·한도·초안 소유권 검증 | NOT_TESTED |
| 저장 내구성 | asking 전 저장, 재시작 복구, 삭제·격리·철회 처리, 저장 실패 안내 | NOT_TESTED |
| 후속 대화 | 정확한 ID 갱신, 모름이면 질문 종료, 중단해도 UI 복구 | NOT_TESTED |
| 기한/중복 | 콜드 스타트 포함 7초 내 응답, 중복 게시 없음 | NOT_TESTED |
| 위치/배포 | 신선도·지명 후보·도로 맥락·반경 상한·만료·오프라인 처리 | NOT_TESTED: 후속 단계 |
| 방향/시각 | 발견 시각의 차량 방향 이력, 정차/오래된 방향 미정, 발견 시각과 수신 시각 구분·누락·시간대·미래값 처리 | NOT_TESTED |
| 규제/UI | 주행 중 위치 지정 제한·접근성·차량 폼팩터 확인 | NOT_TESTED: UI 구현 전 |

harness·문서 JSON·보고 형식 검사와 실제 Gleo/차량/서버 기능 검증을 구분한다. 수동 Intent 주입은 앱 수신 테스트일 뿐 음성 연동 완료 증거가 아니다.

## 11. 위치 처리 정책: 현도로 주변·지명·질문·보류

2026-11-08 사용자 후속 결정. Gleo의 상대 시간 역산과 발견 시각 기준 위치·방향 이력 정책으로 갱신한다. 위치를 모른다고 내용을 버리거나 반복 질문하지 않는다.

| 사용자 표현/문맥 | locationMode | 앱 처리 |
|---|---|---|
| 방금, 막, 여기, 목격 직후 문맥의 10초 전 | CURRENT_ROAD | 수신 대비 10분 미만의 발견 시각과 차량 위치·방향 이력으로 도로 주변 구역 준비 |
| 뱅뱅사거리, 역삼역 4번출구 쪽 | NAMED_PLACE | 지명과 수식어로 실제 장소 후보 검색, 해당 지역에 배치 |
| 아까, 어디였더라, 위치 없는 과거 제보 | NAMED_PLACE | 내용 저장 → 지명/주변 기준물 질문 |
| 위치 모름, 나중에 지정, 일단 저장 | DEFERRED | 위치 없이 보관 → 대화 종료 → 나중에 UI 배치 |

사용자에게 시간 숫자를 요구하지 않는다. “방금”은 실제 현재 시각, 명시적 상대 시간은 현재 시각에서 차감한다. 정확히 10분 이상은 지명 확인 대상이다. 목격 직후이고 위치 언급이 없는 문맥은 CURRENT_ROAD로 분류할 수 있지만, 과거/불명 문맥을 현위치로 억지 분류하지 않는다. “아까 역삼역 4번출구”는 NAMED_PLACE, “방금 뱅뱅사거리”도 명시 장소 우선이다. 서로 다른 장소가 충돌하면 질문한다. 긴 과거 시간이나 이미 도로를 이탈했다는 단서가 있으면 현재 도로 기준으로 넓게 배치하지 않는다. 경계 사례는 실발화 평가로 조정한다.

### CURRENT_ROAD: 발견 시각의 도로 이력 기반 근사 구역

발견 시각의 차량 위치·방향 이력으로 방금 지나온 도로의 근사 영역을 정하고 고정한다. CURRENT_ROAD는 수신 대비 10분 미만만 허용한다. 지명은 필요 없으며 이력이 없으면 현위치에 임의 배치하지 않고 확인한다. 현재 Sprint에서는 시각/모드 검증과 초안 저장까지만 구현한다.

단일 원형 반경만으로 전 차량에 보내기보다는 현재 도로의 주변 구간을 후보로 두고 수신 차량의 도로·접근 방향을 비교한다. 단순 원형 조회는 서버 후보 조회에 쓸 수 있으나 최종 수신 판정과 구분한다. 길/방향이 확인되지 않으면 반대 차로를 추측하거나 모든 인접 도로로 확대하지 않고 위치 확인/미정 초안으로 남긴다. 반대편이라는 명시 표현은 별도 확인 없이 차량 진행 방향으로 바꾸지 않는다. directionMode=USER_DESCRIPTION과 directionDescription에 보존하고 앱이 해당 도로 방향을 검증한다.

반경/구간 길이는 앱 정책이다. 도로·이력 품질로 상한을 정하며 오차가 크다고 무제한 넓히지 않는다. 발견 시각 없이 원목격 위치를 추측하지 않는다. 위치 이력 조회와 도로 정합성 검증은 후속 구현이다.

### 진행 방향과 시각 데이터 계약

CURRENT_ROAD의 기본 방향은 발견 시각의 차량 진행방향 이력이다. 사용자 명시 방향은 USER_DESCRIPTION으로 보존한다. 정차·저속·센서 결측·과거 샘플 부재 시 0도나 현재 방향으로 대체하지 않고 미정 처리한다.

Gleo Action은 방향의 기준/설명과 전달 가능한 실제 발화 기록 시각을 보내고, 앱은 아래처럼 관측 데이터를 보강해 저장한다. 다음은 **앱 내부 초안 JSON 예시**이며 Gleo가 생성하거나 Callback으로 돌려보낼 JSON이 아니다.

```json
{
  "draftId": "draft-local-001",
  "content": "도로에 구덩이가 있음",
  "locationMode": "CURRENT_ROAD",
  "directionMode": "CURRENT_VEHICLE",
  "travelHeadingDegrees": 135.0,
  "directionCapturedAt": "2026-11-08T03:00:19Z",
  "observedAt": "2026-11-08T03:00:00Z",
  "receivedAt": "2026-11-08T03:00:20Z",
  "draftState": "PENDING_LOCATION",
  "published": false
}
```

| 필드 | 생성 주체·의미 |
|---|---|
| travelHeadingDegrees | 앱의 수치 방향 계약: 진북 0°, 동 90°, 남 180°, 서 270°, 유한수 0 이상 360 미만. SDK 단위/북 기준을 확인한 뒤 정규화하며 LLM이 생성하지 않음 |
| directionCapturedAt | 방향 샘플의 실제 측정 시각, UTC ISO 8601. 방향값과 함께 저장하며 측정 시각을 모르면 수신 시각으로 위장하지 않음 |
| observedAt | Gleo가 실제 현재 시각에서 사용자 상대 시간을 빼서 계산한 발견 시각(UTC ISO 8601). 방금은 현재 시각. CURRENT_ROAD에서는 필수이며 센서 실측 시각으로 보장하지 않음 |
| receivedAt | 앱이 최초 Action을 수신한 시각. 발견 시각과 별도로 기록 |

사용자는 절대 날짜/시각/각도를 말할 필요가 없다. Gleo는 실제 현재 시각과 사용자의 상대 시간으로 발견 시각을 계산한다. 현재 시각을 제공받지 못하면 날짜를 생성하지 않는다. 스키마만으로 Gleo의 시계 접근/계산 정확성이 보장되지 않으므로 실제 JSON을 검증해야 한다. 앱은 receivedAt을 별도 보관하고 미래값을 거부한다.

### NAMED_PLACE: 명시 장소 중심 구역

앱의 장소 검색/지오코딩으로 placeQuery를 해석한다. LLM이 위도/경도·지도 장소 ID·신뢰도를 생성하지 않는다. 출구 번호와 “쪽/앞/지난 뒤” 수식어를 보존하고 실제 후보와 관련 도로를 확인한다. 현재 위치는 동명이소 후보 정렬에만 참고할 수 있고 사용자 명시 장소를 현위치로 대체하지 않는다. 후보가 다수이거나 지도에 해당 출구/도로가 없으면 장소를 재확인하며 실패를 현위치 배치로 돌리지 않는다. 장소 검색 제공자·API 지원·라이선스는 미선정이다.

### 임시저장과 나중에 배치

미완성 제보는 pendingReport로 질문 문맥을 반환하며 최종 저장은 보류한다. 사용자가 모르면 DEFERRED로 끝내고 초안 목록에서 위치를 선택하게 한다. 앱이 종료돼도 복구되어야 하며, 나중에 위치를 정한 뒤 내용의 현재 유효성을 확인하고 별도 게시 절차를 거친다. 주행 중 지도 핀 조작은 규제 검토 전 허용한다고 가정하지 않는다.

### 공식 API·운영 경계

공개 Gleo 계약은 발화 시각·주행 이력 제공을 보장하지 않는다. 이것으로 Gleo 내부가 이력을 저장하지 않는다고 단정하지 않는다. CURRENT_ROAD의 과거 위치 배치에는 발견 시각의 차량 위치·방향 이력이 필요하다. 확보 방법·최소 수집 범위·동의·보유기간은 후속 위치 구현에서 검증한다.

[FusedLocation 위치 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)의 getLastKnownLocation은 새 측정이 아니며 null일 수 있다. 앱은 신선한 위치인지 확인하고, 필요한 경우 업데이트 콜백으로 위치를 받으며 null/onError를 처리한다. LocationData의 시각 단위·시계/품질 의미·콜백 빈도는 설치 SDK에서 확인한다. 수신 시각을 측정 시각으로 바꾸거나 신뢰도를 임의의 m 단위 정확도로 해석하지 않는다.

[권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)과 지원 환경을 확인하고 initialize 후 사용하며 동일 listener 해제와 release를 지킨다. 권한 거절·차단·미지원·오래된 위치는 미정 초안 경로다. Google Fused Location 동작이나 백그라운드 상시 수집을 Pleos 보장으로 해석하지 않는다. [개인정보 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy)에 따라 목적·저장 항목·보유기간·삭제·공유를 고지하고 동의받는다. 전체 경로 수집·외부 전송은 추가하지 않는다.

## 최신 응답 정책 — 2026-11-08 후속 수정

CURRENT_ROAD에서는 지명을 묻거나 다른 모드로 자동 전환하지 않는다. 필수 정보가 유효하면 즉시 success와 검증된 제보 데이터(content/locationMode/directionMode/observedAt)를 반환한다. 지명 asking은 NAMED_PLACE에서 placeQuery가 없을 때만 발생한다. 기타 실제 필수 필드 누락(content/locationMode/발견 시각/명시 방향 설명)은 해당 필드만 요청한다. 오래된 CURRENT_ROAD는 STALE_CURRENT_ROAD fail이다. Gleo 노출 enum은 CURRENT_ROAD/NAMED_PLACE/DEFERRED이며 기존 NEEDS_CLARIFICATION은 저장 호환용으로 남기되 새 입력은 fail이다.
Callback data는 기존 JSON 배열 문자열 형식을 유지하고 success에 검증된 제보 필드를 추가한다. 실제 서버 게시/배포 경로는 없으므로 published=false이며 debug는 무저장이다. success는 제보 입력 접수 성공을 의미한다.

## 후속 답변과 최종 데이터 반환 — 2026-11-08

asking은 즉시 반환하는 중간 Callback이며 최종 제보 JSON이 아니다. data 배열 항목에는 missingField와 pendingReport(내용/모드/방향/이미 확보한 시각·지명)를 담는다. Gleo가 이 문맥과 답변을 결합한 완전한 Action을 다시 보내야 한다. 시각 질문은 내부 필드 이름을 읽는 대신 제보를 발견한 경과 시간을 묻는다. 필수값이 완성되기 전에는 report/최종 저장/게시를 하지 않는다. 완성 후에는 한 번의 호출에 success와 최종 report 데이터를 반환한다. 사용자에게 재확인하는 승인 질문은 추가하지 않는다.

공식 status는 success/asking/fail이며 answer는 없다. Callback 자체를 사용자 답변까지 보류하면 timeout이므로 각 Action에 응답한다. 짧은 후속 발화를 datetime agent 대신 앱으로 재분류하는 것은 Gleo 담당이며 이 문맥 전달로 해결되는지는 실제 음성 E2E 검증 전 NOT_TESTED다. 앱이 받지 못한 발화를 임의로 처리하지 않는다. draftId 없는 후속 Action의 연결은 Gleo가 전달한 전체 문맥에 의존하며 앱이 최근 초안을 추측하지 않는다. 현재 무저장 debug에서는 프로세스 종료 후 대화 복구를 보장하지 않는다.

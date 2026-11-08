# Sprint 1: Gleo 도로 제보 초안

2026-11-08. 현재 브랜치 `gm/feat-speech-to-text`에서 작업한다. 브랜치명은 과거 이름이며 STT SDK를 구현한다는 뜻이 아니다. 사용자가 지정한 3명은 **자비스(이 세션, 메인테이너/조율자), 프라이데이, 이디스**다. 다른 AI는 사용자가 별도 세션에서 호출한다. 각 담당자는 이 문서와 AGENTS.md, pleos-connect 스킬에서 시작한다.

## 현재 사용자 우선순위: 음성 호출·JSON 관측

사용자 후속 결정으로 현재 수동 검증 목표는 **음성 → Gleo 함수 선택 → 전달 파라미터 → Callback**이다. debug 빌드는 저장 동의/로컬 저장을 거치지 않고 검증 결과만 응답하며 관측 화면을 바로 표시한다. 외부 Action 수신은 debug에서만 열고 실제 설치 확인된 ai.umos.ambientai에 Callback을 제한한다. 발신자가 인증되었다고 주장하지 않으며 draft/차량/게시 기능을 실행하지 않는다. release 수신점은 내부 전용이다.

[실시간 로그와 장애 단계 확인 방법](gleo-debugging.md)을 먼저 참고한다. 최근 10건의 관측은 메모리에만 남는다. 사용자 요청으로 debug `GleoProbe` logcat에 Action/키·타입/입력 JSON/결과/Callback JSON·전송 요청 시간을 기록한다. `adb logcat -s GleoProbe:D '*:S'`로 확인한다. 전송 요청 성공과 Gleo 실제 수신/음성 응답 성공을 구분한다. 사용자가 직접 음성 테스트하며 에이전트가 합성 Action/음성 호출을 자동 실행하지 않는다.

아래 저장/UI 스프린트 범위는 이미 구현된 기반 기능이며 현재 테스트의 선행조건으로 강제하지 않는다.

## 스프린트 구성

| 스프린트 | 산출물 | 완료 기준 |
|---|---|---|
| 1 — 이번 브랜치 | Gleo 입력 검증·Schema/Callback, 내구성 있는 미게시 초안, 동의·목록·삭제 UI | 로컬 저장/재시작/철회/잘못된 요청 자동 검사, 빌드/lint, 보안 경계를 지킨 수신 연결. 실제 Gleo는 별도 E2E 증거 필요 |
| 2 | FusedLocation 현위치·현재 차량 방향, 지명 검색, UI 위치 지정 | 신선도/정차/결측/지명 다중 후보/진행 방향·반경 정책 검증 |
| 3 | 서버 게시·공간 조회·인증·만료·멱등성 | 두 클라이언트 게시/수신, 미확정 위치 게시 금지, 오래된 제보 재확인 |
| 4 | 접근 감지·경고·장애 복구와 출시 준비 | 도로/방향 필터·권한 철회·오프라인·차량 전원·폼팩터/심사 증거 |

Gleo 선제 음성 경고 가능성은 Sprint 2 이전에 조사한다. 지원을 확인하지 못하면 명령 Callback으로 선제 경고가 가능하다고 가정하지 않는다. 스프린트는 시간 약속이 아니라 기능 경계다.

## 이번 브랜치 범위

목격 직후 자연어 → `CREATE_ROAD_NOTE_DRAFT` → 앱 입력 검증 → 동의 확인 → 로컬 저장 → success/asking/fail Callback이다. 위치 질문 전에 내용을 저장하고, 모름/DEFERRED이면 반복 질문하지 않으며 다음 앱 실행에서 초안을 볼 수 있게 한다.

이번에는 GPS/진행 각도 취득·지도·지오코딩·위치 핀·서버 게시·다른 차량 수신·자체 STT/TTS·차량 제어를 구현하지 않는다. CURRENT_ROAD/NAMED_PLACE는 PENDING_LOCATION으로 저장하고 정확한 좌표가 확보됐다고 답하지 않는다. NEEDS_CLARIFICATION/DEFERRED는 AWAITING_LOCATION이다. 나중에 UI에서 실제 위치를 심는 기능은 Sprint 2다.

사용자 요청에 따라 applicationId를 `com.jinkim.hyundaiAndGretel`로 정합시킨다. 이전 `com.jinnayeong.wakeup`는 별도 설치 앱으로 남을 수 있고 데이터 자동 이전은 하지 않는다. 기존 Manifest 도어/창문 권한은 이 브랜치에서 임의로 변경하지 않는다.

## 역할·파일 소유권

공통 경로 접두사는 `app/src/main/java/com/jinkim/hyundaiAndGretel/`이며 테스트도 같은 패키지 경로를 따른다.

| 담당 | 구현·검증 책임 | 독점 편집 파일 |
|---|---|---|
| 자비스 | 공통 타입·계약, 입력 파서/Callback, 처리 엔진, Schema, 패키지/수신 신뢰 경계, 마지막 앱 연결·통합검증 | `roadnote/RoadNoteContract.kt`, `gleo/**`, `assets/schema.json`, `MainActivity.kt`, Manifest, Gradle, scripts, 현재 설계/스프린트 문서 |
| 프라이데이 | 로컬 초안 저장·동의·7일 만료·삭제·재시작·오류/동시성 테스트 | `roadnote/LocalDraftStore.kt` 및 저장 관련 추가 파일, `test/**/roadnote/*Store*Test.kt`, `androidTest/**/roadnote/**`, `docs/work/sprint-1-friday.*` |
| 이디스 | 순수 Compose 동의·초안 목록/삭제·로딩/오류/빈 상태·미게시 안내, 디자인 검토 | `ui/drafts/**`, UI 전용 테스트/프리뷰, `docs/work/sprint-1-edith.*` |

자비스 외에는 MainActivity/Gradle/Manifest/공통 계약을 편집하지 않는다. 필요한 의존성/시그니처 변경은 담당 기록에 요청하고 자비스와 맞춘다. 새 범용 Repository/DI/네트워크 계층을 만들지 않는다. 소유권이 다른 파일을 임의로 포맷하지 않는다.

## 공통 코드 계약 v1

자비스가 다음 코드를 `roadnote/RoadNoteContract.kt`에 먼저 제공한다. 모든 타입의 package는 `com.jinkim.hyundaiAndGretel.roadnote`다. 실제 코드가 문서와 다르면 임의 추측 대신 자비스에게 조율 요청한다.

```kotlin
enum class LocationMode { CURRENT_ROAD, NAMED_PLACE, NEEDS_CLARIFICATION, DEFERRED }
enum class DirectionMode { CURRENT_VEHICLE, USER_DESCRIPTION, UNKNOWN }
enum class DraftState { PENDING_LOCATION, AWAITING_LOCATION }

data class RoadNoteInput(
    val content: String,
    val locationMode: LocationMode,
    val placeQuery: String? = null,
    val directionMode: DirectionMode = DirectionMode.UNKNOWN,
    val directionDescription: String? = null,
    val utteranceRecordedAt: Instant? = null,
    val draftId: String? = null
)

data class RoadNoteDraft(
    val id: String,
    val input: RoadNoteInput,
    val receivedAt: Instant,
    val updatedAt: Instant,
    val state: DraftState
)

interface DraftStore {
    suspend fun hasConsent(): Boolean
    suspend fun setConsent(granted: Boolean)
    suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft
    suspend fun list(): List<RoadNoteDraft>
    suspend fun delete(id: String): Boolean
    suspend fun clear()
}
```

`Instant`은 java.time.Instant다. coroutine은 기존 kotlinx-coroutines를 사용한다. 이 작은 인터페이스는 세 담당자가 같은 계약으로 독립 구현/검증하기 위한 경계다.

### 프라이데이 상세 명세

- 진입점은 `class LocalDraftStore(context: Context, now: () -> Instant = Instant::now) : DraftStore`. 내부 Context는 applicationContext로 유지한다. 메서드는 IO dispatcher에서 실행하고 같은 프로세스의 여러 인스턴스에서도 동시 갱신 유실을 막는다. 별도 Android process는 사용하지 않는다.
- 앱 전용 `noBackupFilesDir`에 최소 파일로 저장하고 원자적으로 교체한다. 표준 플랫폼 저장 기능을 우선하며 Room 등 새 의존성은 추가하지 않는다. 내용/위치/시각을 로그에 남기지 않는다.
- 동의 기본값 false. save 자체에서도 동의를 확인한다. 사용자가 UI에서 명시적으로 허용한 뒤만 저장한다. false로 철회할 때 초안 전부 삭제 후 철회 완료를 확정한다. 실패를 성공으로 위장하지 않는다.
- 이번 개발 브랜치의 보유기간은 **최초 receivedAt부터 7일**(프로젝트 선택). list/save 시 만료를 제외하고 정리한다. 앱 미실행 중 정확한 시점의 물리 삭제 작업은 없으므로 UI에는 “7일 유효, 만료 시 앱 이용 때 삭제”로 고지한다. 최대 100개, 초과 시 자동으로 기존 초안을 버리지 말고 저장 실패를 반환한다.
- 신규 ID는 UUID. input.draftId가 있으면 해당 로컬 Android 사용자 컨텍스트의 미만료 초안만 갱신한다. 없는/만료 ID는 `IllegalArgumentException`. 갱신은 원본 receivedAt/utteranceRecordedAt을 보존하고 updatedAt만 현재 시각으로 바꾼다. ID 변경/자동 신규 생성 금지.
- save 결과의 input.draftId는 실제 id로 통일한다. state는 locationMode에서 결정하되 NAMED_PLACE의 placeQuery 누락 또는 USER_DESCRIPTION의 directionDescription 누락은 AWAITING_LOCATION으로 둔다. list는 receivedAt 내림차순. clear는 내용만 비우고 동의는 유지한다. setConsent(false)는 내용 삭제와 동의 철회다.
- 저장 실패/손상은 IOException 등으로 호출자에게 전달하고, 손상 파일을 빈 목록으로 조용히 덮어쓰지 않는다. 거절/미동의는 IllegalStateException. 별도 암호화 라이브러리/사용자 정의 암호/계정 시스템은 만들지 않는다. 공식 보안 의무에 따라 Android Keystore의 AES-GCM으로 저장 본문과 임시 파일을 암호화하고 키는 파일에 저장하지 않는다. JVM 테스트는 cipher 입출력 함수를 주입할 수 있으나 실제 LocalDraftStore의 암호화 해제 기본값을 두지 않는다. Android 사용자별 앱 샌드박스 범위이며 동일 Android 사용자를 공유하는 운전자 간 분리는 제공하지 않으므로 출시 전 별도 해결한다.
- 핵심 테스트: 재생성 후 복구, 원자 저장 실패에서 기존 데이터 유지, 갱신 원시각 보존, 없는 ID, 7일 경계, 동의 기본/철회 삭제, 동시 저장/삭제, 손상/용량 한도. 테스트 실행 환경/방법을 기록한다.

### 이디스 상세 명세

`ui/drafts/DraftScreen.kt`, package `.ui.drafts`에 다음 순수 UI 진입점을 만든다. 저장소/Receiver/Activity를 직접 참조하지 않는다. 상태 로딩과 코루틴 연결은 자비스가 MainActivity에서 담당한다.

```kotlin
@Composable
fun DraftScreen(
    consentGranted: Boolean,
    drafts: List<RoadNoteDraft>,
    busy: Boolean,
    errorMessage: String?,
    onConsentChange: (Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit
)
```

- 미동의 화면: 제보 내용·장소/방향 표현·시각을 기기 내 최대 7일 보관, 아직 게시/공유 안 함, 거부 가능, 철회 시 삭제됨을 짧게 고지. 동의 버튼을 누르는 행동만 동의로 처리한다.
- 동의 후: 빈 상태/새로고침, 초안 내용·필요한 지명/방향·앱 수신 시각·위치 대기/미정 상태·삭제 버튼·동의 철회. 로딩 때 중복 조작 차단, 오류 문구는 호출자가 제공하는 짧은 비민감 문구만 표시.
- Gleo 수신 즉시 UI를 강제 실행하지 않는다. “미게시”, “위치는 다음 단계에서 지정”을 분명하게 표시한다. 구현 안 된 지도 버튼/게시 버튼/가짜 성공 상태를 만들지 않는다.
- pleos-design 스킬과 실제 사용 컴포넌트 원문을 읽고 긴 내용/지명·주야간·분할/회전·터치 영역/대비를 검토한다. 긴 원문 전체를 운전 중 읽게 하는 기능이 아니며 초기에는 distractionOptimized를 선언하지 않았으나 후속 사용자 요청으로 CarUxRestrictions에 따른 간결 UI와 함께 MainActivity에 선언했다. 상세 근거는 docs/work/driving-status-ui.md를 따른다. UI 실기기·주행 규제 검토 미확인은 NOT_TESTED.

### 자비스 상세 명세

- 위 공통 타입을 우선 제공하고 Schema를 assets/schema.json으로 옮긴다. 제거된 위치 설명 필드는 재도입하지 않는다.
- 파서는 JSONObject 경계 뒤 순수 Kotlin 검증 함수를 두어 단위 테스트한다. 잘못된 타입/한도/enum·공백·모드 충돌·시각 형식/미래값을 검증한다. 방향 기본값은 CURRENT_ROAD→CURRENT_VEHICLE, 나머지 UNKNOWN. 필수 content/locationMode 누락은 asking, 선택값 누락은 허용한다.
- 지명/방향 누락으로 질문해야 하는 경우 유효 내용은 저장한다. Callback status와 저장 성공을 분리하고 DEFERRED는 추가 질문을 하지 않는다. Callback은 최소 ID/상태/미게시만 포함한다.
- 실제 Gleo 발신자 인증/수신 대상 패키지/권한은 공급자 계약 확인 전 확정하지 않는다. 외부에 무방비 exported Receiver를 열지 않는다. release Receiver는 **exported=false**로 유지한다. 사용자 요청의 debug 관측 빌드에서만 exported=true로 테스트 입력을 받고 저장·차량·게시 작업을 하지 않는다. 실제 Gleo 호출은 사용자가 직접 검증한다. schema 파일 존재와 외부 연동 활성화를 구분한다. 실제 보안 계약 확보 후 자비스만 외부 수신/대상 제한을 변경한다.
- 실제 시각 제공 계약이 없으므로 utteranceRecordedAt을 LLM 문자열만 보고 신뢰하지 않는다. 형식 검증 후라도 앱 경계에서 검증되지 않은 시각은 채택하지 않고 receivedAt만 기록한다. SDK 방향값은 Sprint 2이며 임의 각도/현재 위치를 생성하지 않는다.
- 프라이데이 저장소와 이디스 UI가 준비되면 MainActivity·Receiver·처리 엔진을 연결한다. `goAsync`/IO·응답 1회/5초 내부 목표·저장 실패/미동의 처리를 검증한다. 시스템 Gleo는 7초 취소이며 앱 내부 시간만으로 준수를 판정하지 않는다.

## 병렬 작업·커밋·통합

사용자는 다른 세션에 “너는 프라이데이/이디스야. docs/plans/sprint-1-gleo-drafts.md를 읽고 담당 범위 구현·검증·커밋해”라고 지정하면 된다. 자비스는 다른 AI를 대신 자동 호출하지 않는다.

1. 같은 작업 폴더에서 진행하면 역할별 독점 파일만 편집한다. git checkout/reset/stash/clean, 전체 add, 타인 변경 되돌리기 금지. 커밋은 **자기 소유 파일 경로를 명시**한다. 커밋 직전에 staged diff를 확인한다.
2. 분리 worktree를 사용하면 현재 미커밋 설계/계약이 빠지지 않게 자비스의 계획/계약 커밋을 기반으로 시작한다. 기존 미커밋 사용자 변경은 포함하거나 삭제하지 않는다.
3. 각 담당은 docs/work/sprint-1-<역할>.md와 .json에 규칙별 PASS/FAIL/NOT_TESTED, 실제 명령·환경·커밋 SHA·남은 조치를 남긴다. 커밋은 사용자가 이번 요청에서 승인했고 push는 승인되지 않았다.
4. 자비스는 담당 변경을 검토하고 통합 커밋을 만든다. 같은 브랜치의 커밋은 다시 cherry-pick하지 않는다. 별도 브랜치는 SHA를 확인한 뒤 통합한다. 타인 작업을 재구현해서 덮어쓰지 않는다.
5. 다른 담당 결과가 아직 없으면 자비스 자신의 파서/엔진/Schema 검증을 먼저 끝낸다. 준비 안 된 모듈을 완료했다고 보고하거나 가짜 구현으로 메우지 않는다. 통합은 해당 결과가 도착하면 이어서 진행한다.

## 검증 게이트

필수 자동 검사: `:app:testDebugUnitTest`, harness `--build`(assembleDebug/lintDebug), Studio MCP build_project. 저장/수신 연결 후 정상·미동의·네 모드·후속 ID·손상/삭제·프로세스 재시작 확인. 자비스는 최종 변경 전체와 규칙별 보고를 확인한다.

필수 실환경 증거: Gleo 발신자/응답 대상 제한, 승인/스캔/실제 음성→Callback, 7초 내 응답, UI 폼팩터·삭제/동의, Android 사용자 격리. 없으면 기능의 검증 완료를 주장하지 않는다. 내부 수신 테스트는 실제 Gleo E2E를 대체하지 않는다.

# 수신자 안내문 분리

PR 10의 `7dd9721` 기반에서 Gleo 제보 텍스트와 Gleo가 생성하는 수신 안내문을 별도로 저장한다. 변경 범위는 action schema, 입력 검증, 제출 및 Room, 수신 표시·확인, TTS다. 기기 작업 없이 오프라인 Gradle과 호스트 SQLite로 검증한다. 후속 안전 조건은 TTS에서 원문 대체 읽기를 제거하는 범위다.

- INT-01 및 DOC-24357cf5e59a#설명과문자열 (§2.1, §4). 영어 최대 두 문장, 구체 예시 없이 두 문자열의 역할을 선언한다. Gleo의 필수 값 생성은 보장되지 않으므로 기존 스키마 입력도 처리한다. [App Schema Writing Guide](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)
- DOC-c6fc94252d66#성공응답 (§2.1, §4). 저장 성공 후 기존 함수명과 결과를 반환한다. 안내문은 수신자에게 쓰이므로 기존 작성자 콜백에는 추가하지 않는다. [Callback Schema Writing Guide](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback)
- DOC-2bec764daf11#등록과갱신. 앱 asset의 스키마 검토 및 승인, 기기에서 갱신된 스키마의 실제 생성은 별도 검증이 필요하다. [Delivering the Schema to Gleo](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)

## 필드와 호환 정책

- `content`는 Gleo가 음성 제보에서 추출해 전달한 텍스트다. 음성 파일은 저장하지 않으며 정확한 STT 전사는 보장되지 않는다. 기존 Gleo 입력의 500 UTF-16 코드 유닛 검사, Room의 500 코드 포인트 검사 및 양끝 공백 제거를 유지한다. 내부 문구는 안내문 생성으로 대체하지 않는다.
- `receiverGuidance`는 Gleo가 제보자의 언어로 작성하는 완결형 수신 안내문이다. 스키마는 120자 이내를 요청하고 제보 사실·부정·불확실성을 보존하며 새 사실·행동 지시·추정 방향·시간을 추가하지 않도록 명시한다. 신규 스키마는 `receiverGuidance`를 `required`로 선언해 Gleo에 생성을 요구한다. 실제 값의 생성은 보장되지 않으므로 파서는 구버전 스키마 입력도 허용한다. 누락, JSON null, 빈 문자열, 문자열이 아닌 값, 500 코드 포인트 초과는 NULL로 처리하고 유효한 원문 제보는 저장한다. 검사는 트림 전 길이에 적용하며 앱에서 생성 문장의 의미 일치까지 검증하지는 않는다. 기존 전체 입력 8 KiB 한도를 유지한다.
- 파서 → `GleoActionProcessor` → 제출 클라이언트 → 서비스 Binder → `RoadNoteSubmission` → `RoadNote` → `RoadNoteEntity`로 전달한다. Room의 원문 정규화 `copy`는 새 필드를 보존한다. 조회·후보·시나리오·수신 JOIN의 전체 행을 `toNote()`로 복원한다. 작성자 성공 콜백의 `content`는 기존 제보 텍스트이며 안내문은 로컬에 남는다.
- Room 1→2는 `ALTER TABLE road_notes ADD COLUMN receiverGuidance TEXT` 한 문장으로 기존 행을 NULL 상태로 유지한다. 수신 기록과 활성 시나리오에도 쓰기 작업이 없다. 화면·본문 길이 제한·레이아웃 재확인·ACK·시연 수신 판단은 모두 안내문을 우선 사용한다. NULL일 때 화면에 표시하는 내용은 Gleo가 추출한 원문이며, 이 문구를 기준으로 표시 제한과 확인 가능 여부를 판단한다. 콘텐츠 예산 키는 기존 수신자/쪽지 ID를 유지한다.
- TTS는 본문 읽기가 허용되고 비어 있지 않은 안내문이 120 코드 포인트 이내일 때만 공백을 정규화해 접두사 없이 읽는다. 안내문 누락·NULL·빈값·타입 오류·길이 초과·본문 제한은 모두 일반 도착 문구로 처리한다. 원문은 TTS에 전달하지 않는다. 길이는 TTS 공백 정규화 전에 검사하고, TTS는 ACK를 수행하지 않는다.
- 별도 모델 호출·키·전송을 추가하지 않았다. debug/release merged Manifest에서 Gleo receiver의 exported 값은 각각 true/false로 확인했다. 기존 시연·저장 제한과 오디오 생명주기를 사용한다.

## 검증 결과

| 상태 | 검증 | 근거 |
| --- | --- | --- |
| PASS | 오프라인 Gradle | JDK 21, SDK 35. `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease :app:compileDebugAndroidTestKotlin`. JVM 141개, 실패·오류·스킵 0. 파서에서 거부한 안내문과 legacy NULL에 대해 일반 도착 문구만 읽고 ACK를 남기지 않는 회귀를 포함한다. 최초 샌드박스의 외부 캐시 잠금 오류는 승인된 로컬 Gradle 접근으로 해결했다. |
| PASS | Gleo 스키마 | `python3 scripts/test_receiver_guidance_schema.py`. 필수 필드와 제보 원문·수신 안내문의 영어 설명 계약을 확인했다. |
| PASS | 호스트 SQLite | `python3 scripts/test_receiver_guidance_migration.py`. v1 export와 Room 코드의 실제 SQL을 사용했다. 제보 원문·인덱스·외래키·대기 및 확인 완료 수신·활성 시나리오를 비교하고 v2 Room export 및 새 안내문 inbox 조회를 확인했다. |
| PASS | 하네스 | `python3 scripts/verify_harness.py`, 저장소 정적 설정. `--report docs/work/receiver-guidance.json` 종료코드 2는 명시한 미검증 항목을 반영한다. |
| NOT_TESTED | Gleo 등록 및 실제 생성 | 스키마 등록·승인·갱신 후 실제 호출 및 부정·불확실성 보존을 확인해야 한다. |
| NOT_TESTED | 기기 Room·화면·오디오 | instrumentation은 컴파일했다. 실제 Room 업그레이드, 본문 배치, TTS 출력 및 전원 복귀는 미실행이다. ADB·에뮬레이터·앱을 실행하지 않았다. |
| NOT_TESTED | Studio 빌드 | 열린 IDE 프로젝트는 원본 worktree였다. 현재 worktree를 대상으로 CLI Gradle 검증을 수행했다. |

- DOC-9faba42791f0#주행제한. 본문 표시와 명시 확인에 현재 주행 제한을 적용한다. [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
- AUDIO-01 및 DOC-f04338dec3b6#포커스, AUDIO-02 및 DOC-a243773e232d#전원과 DOC-2cbe51f12c43#선행묵음. 기존 포커스 획득·반납, 전원 중단 및 PCM 선행 묵음 경로에서 전달 문구만 바뀐다. [오디오 포커스](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development) [Suspend](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus) [TTS 선행 묵음](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide)

# Sprint 1 자비스

2026-11-08. 사용자 승인: 스프린트/3역할 분담 작성 후 자비스 담당 구현 시작, 담당별 커밋과 마지막 통합은 자비스. 다른 AI는 사용자가 호출한다. 이번 브랜치 gm/feat-speech-to-text.

pleos-connect/ponytail/pleos-verify 적용. 현재 MainActivity·Gradle·Manifest·설계/작업 기록과 S1~S5·보안/개인정보 원문을 읽었다. 새 SDK/차량 제어·위치 API는 사용하지 않는다. 실제 외부 Gleo 인증 계약 부재 때문에 내부 Receiver만 준비하고 외부 연동은 미활성으로 남긴다.

## 범위·선택 규칙

ENV-02 / INT-01 / PRIV-01. 상세 담당/공통 타입/검증/스프린트는 docs/plans/sprint-1-gleo-drafts.md. 자비스는 계약·검증 파서·Callback·엔진·Schema·Gradle 패키지·최종 연결을 소유한다. 프라이데이는 LocalDraftStore, 이디스는 순수 DraftScreen을 소유한다. 기존 사용자 Manifest 4권한 변경과 다른 이전 기록을 자기 코드 커밋에 섞지 않는다.

| 세부 ID | 원문 요구사항 / 적용 | 확인 방법 |
|---|---|---|
| DOC-24357cf5e59a#함수파라미터 | 최대3/고유대문자·영어 description·지원 타입/문자열enum·required | schema/파서/단위 검사 |
| DOC-c6fc94252d66#응답 | 함수명·success/asking/fail·영어문장·voice·data 객체배열 문자열 | Callback 검사 |
| DOC-c6fc94252d66#기한 | 7초 취소, 내부5초 목표 | 수신 통합 후 실제 측정 필요 |
| DOC-f1d260bfb503#전달 | ACTION_PARAM 코드 예제와 개별extra 본문 차이 | 코드 예제 형식만 처리, 외부 실제계약 NOT_TESTED |
| DOC-2bec764daf11#활성화 | assets/schema.json·필터·검토/승인·스캔 | 파일/Manifest, 실제 등록 NOT_TESTED |
| DOC-27ac8605961f#입력신뢰 | 외부 입력 검증·공개API·민감정보 노출 방지 | 내부 수신점·모드/타입/길이·원문로그 없음 |
| DOC-72a5d7ab4ebb#최소수집 | 목적/동의/최소보유 | 미동의 저장금지·최소 Callback, 저장/UI 담당 연결 후 확인 |
| DOC-4036191ed19c#동의철회 | 명시동의·보유기간·철회시 삭제 | 7일 개발정책·저장/UI 검증 필요 |

검증: 의미 있는 파서/처리/응답 단위 검사, harness/build/lint/Studio 빌드. 문서/코드 구현 완료와 실기기·공식 활성화·외부 신뢰 경계 검증을 구분한다. 문서 계약 확정 뒤 자비스 구현을 즉시 이어간다.

## 조율 메모 — 프라이데이 전달용 (2026-11-08)

프라이데이 기록에서 확인한 DOC-27ac8605961f의 민감정보 암호화 의무는 생략하면 안 된다. Sprint 1의 “새 암호화 시스템 제외”는 사용자 정의 암호/추가 라이브러리 제외로 정정한다. Android Keystore AES/GCM/NoPadding으로 데이터와 임시 파일 모두 암호화하고 키는 파일과 분리해야 한다. 저장 파일 코어에 encode/decode 경계(기본 identity는 JVM 테스트 전용)를 주고 LocalDraftStore가 Keystore cipher를 제공하는 최소 변경으로 해결할 수 있다. 프라이데이 소유 파일은 자비스가 덮어쓰지 않는다. 자비스는 이 요구를 스프린트 명세에 반영한다. 다른 세션은 이 기록과 최신 명세를 확인한다.

저장 state도 모드만이 아니라 NAMED_PLACE의 placeQuery 누락 또는 USER_DESCRIPTION의 directionDescription 누락일 때 AWAITING_LOCATION으로 보관해야 한다. 공통 데이터 타입은 변경하지 않는다.

사용자는 각자 커밋하도록 명시했고 이번 후속에서도 자비스 자기 작업만 중간 커밋하라고 재확인했다. 각자 소유 파일만 커밋하고 push는 하지 않는다. 공통 계약/계획 기준 커밋: 451efe1.

프라이데이 저장 구현 9fc027c가 커밋되어 통합 검토를 시작했다. 공식 암호화 의무와 누락 지명 상태를 해결하기 위해 자비스가 별도 통합 변경을 작성한다. 최초 프라이데이 구현/기록은 해당 커밋에 보존하고, 이후 자비스 수정만 별도 커밋한다. 프라이데이 소유 파일의 미커밋 변경이 없는 상태를 확인했다. 이디스의 진행 중 UI 파일/테스트는 편집·커밋하지 않는다.

이디스 UI 후속 검토: 저장 코어는 만료 데이터를 list/save 때 삭제하므로 “최대 7일 보관”을 물리 삭제 보장으로 오해하지 않게 해야 한다. 이디스 후속 변경이 진행 중이므로 자비스는 UI 파일을 건드리지 않았다. 고지는 “최초 수신부터 7일간 유효하며, 만료된 초안은 앱 이용 시 삭제합니다”로 맞추는 것이 정확하다. 이디스 작업 종료 후 자비스 통합 수정에서 반영한다.

## 사용자 범위 정정: 음성 함수 호출/JSON 관측 우선

사용자가 직접 테스트한다고 하여 에뮬레이터 기능 조작을 중단했다. 이어 “동의/철회보다 음성 명령대로 Gleo 함수 호출·데이터 확인까지만” 및 실시간 로그/JSON 관측을 명시 요청했다. 현재 최우선은 debug 전용 **무저장 Gleo 연동 관측**이다. 저장/동의는 release 경로에 남기고 debug의 MainActivity는 관측 화면을 바로 표시한다.

Debug Receiver만 외부 테스트 Action을 받고, 실제 설치 확인된 ai.umos.ambientai에 Callback을 대상으로 제한한다. 발신자를 인증한 것으로 표시하지 않는다. 파라미터 검증/asking/fail 및 무저장 success만 수행하고 draft/위치/차량/게시 권한으로 연결하지 않는다. release는 exported=false와 기존 동의/저장 경로를 유지한다. 사용자 요청에 따른 GleoProbe 로그는 debug에서만 Action/Intent 키·타입/입력 JSON/결과/Callback JSON·시간을 분할 기록하고, 화면은 최근 10건을 메모리에 보유한다. 요청에 따라 테스트 발화 원문이 debug logcat에 포함될 수 있으며 디스크 저장 기능은 추가하지 않는다.

## 실제 사용자 음성 요청 장애 분석

에뮬레이터 02:19:03 발화의 STT/함수 선택은 성공했다. 02:19:05.509 `builtin.refineToolOutput`와 `launch.externalApp.intent`가 com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE_DRAFT 및 content/CURRENT_ROAD/CURRENT_VEHICLE을 생성했다. 02:19:05.511 Gleo `sendActionCall`은 실제 Android action에 패키지명을 붙였지만 우리 Manifest/Receiver는 기본 함수명만 기다렸다. GleoProbe 수신 로그가 없었고 02:19:25.513에 no response timeout 발생. 앱 처리 자체의 지연이 아니라 수신 action 불일치였다.

실환경 근거에 맞춰 Android action은 GLEO_INTENT_ACTION(패키지 접두사 포함)으로 고정하고 Schema/Callback 함수명은 기존 CREATE_ROAD_NOTE_DRAFT를 유지한다. 임의 다중 포맷 지원 대신 실제 관측 계약으로 수정했다. 재검증 회귀 테스트 추가. Gleo가 만든 utteranceRecordedAt=2023-10-11T17:25:00.000Z는 실제 2026 발화 시각이 아니므로 기존 미신뢰 정책을 유지한다. 해당 빌드에서 실제 timeout은 로그상 약20초로 관찰되었으며 공식 7초 문서와 다르다. 내부5초 목표는 유지하며 공식 의무를 이 관측으로 완화하지 않는다.

## 자비스 통합 결과 및 커밋

- 451efe1: 계획/3역할/공통 타입. 207a457: 파서·처리·Callback/Schema와 초기18테스트. 62e542e: applicationId 정합.
- 프라이데이 9fc027c/e5c261c 저장 코어와 이디스 0c855f1/bf5b518 UI를 검토하고 연결했다. 담당 원본 커밋은 그대로 보존했다.
- 979011a: 자비스의 Keystore/AES-GCM 보강·누락 지명 상태·변조/키유실/복구 테스트. 991ca91: UI/내부Receiver 연결. e41ac3e: 유효기간·이용시 삭제 고지 보완.
- 2801265: 사용자 우선순위에 따른 debug 무저장 관측·실시간 JSON/logcat·외부 테스트 수신 및 release 분리. f00e963: 실제 Gleo가 보낸 패키지 접두사 Action 수정.
- 최종 자동 검사: 단위38개/실패0, assembleDebug/lintDebug, Studio MCP build_project PASS. debug/release 빌드와 merged Manifest의 debug exported=true/release=false도 관측 기능 추가 시 확인했다. 이후 Action 접두사 수정 뒤 debug 테스트/빌드/lint/Studio 빌드를 재실행 PASS. AndroidTest Kotlin 컴파일 PASS(접두사 수정은 테스트 Intent 상수 교체).
- 런타임: Pleos emulator-5554, Android user10. platform Instrumentation의 격리 저장소 검사에서 Keystore/GCM·JSON·Callback직렬화·재조회/삭제/철회 PASS. 별도 seed → am force-stop → recover로 실제 새 프로세스 복구/철회 PASS. UI 빈목록에서 철회 후 재실행시 미동의 유지 확인. 사용자의 기존 초안은 사용하지 않았고 테스트 디렉터리는 정리했다. 해당 검사는 debug 관측으로 범위를 좁히기 전 저장 경로에 대한 증거다.
- UI 최초 확인: 2560x1440,160dpi, app bounds[853,90][2535,1300], P/0kmh. 분할/야간/글자크기/규제 전체 평가는 NOT_TESTED. 최신 관측 화면은 설치/실행했으며 사용자가 직접 음성 테스트한다.
- 실제 사용자 첫 음성: Schema 인식·함수 선택/파라미터 생성·Gleo sendActionCall은 PASS, 앱 도착/Callback은 Action 불일치로 FAIL이었다. f00e963으로 수정·설치했다. 수정 후 실제 음성 수신/Callback/음성응답은 사용자 재검증 전 NOT_TESTED다. 설치/합성 Intent를 실제 음성 E2E로 보고하지 않는다.
- 로깅 절차와 단계별 판별은 docs/plans/gleo-debugging.md에 작성했다. 임의 Gleo 재시작/로그 전체삭제/자동 음성·합성 Action 주입을 하지 않는다.
- 사용자 기존 Manifest 도어/창문 권한4줄은 작업 파일에 유지하되 커밋에서 hunk 단위로 제외했다. 타인의 미커밋 파일/과거 기록을 스테이징하지 않았고 push하지 않았다.

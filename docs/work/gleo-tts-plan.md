# 수신 쪽지 음성 안내 계획 검토

2026-11-14. 사용자 요청: gm/feat-gleo-tts에서 문서·현행 코드를 파악해 먼저 계획 작성. 상세 계획: [gleo-tts.md](../plans/gleo-tts.md).

## 범위

- ProximityPickup → NoteReceiptProcessor → RoomReceiptStore의 새 수신 커밋, RoadNoteService의 소유/종료/actor 변경, Inbox/화면 확인 계약을 읽었다.
- TTS 의존성/권한/재생 미구현 확인. 기존 Gleo Callback과 자발 수신 안내를 구분하고 Pleos TTS SDK 사용을 제안한다. 앱 소스·Gradle·Manifest·테스트는 변경하지 않는다.
- 기존 선제 음성 제외 결정은 이번 사용자 요청에 따라 추가 계획으로 분리했다. 합성 위치·Room 시연 제한과 Gleo 외부 수신 정책은 유지한다.

## 공식 원문별 추적

기존 `.agents/skills/pleos-connect/references/source-map.md`의 동일 ID 원문과 저장소 스냅샷을 읽고 웹에서 재확인했다. 날짜는 2026-11-14. 아래 반영 위치는 계획 절이며 구현 완료 근거가 아니다.

| 규칙 / 원문 세부 ID | 요구 및 조건 | 계획 반영 / 확인 방법 |
|---|---|---|
| INT-01 / DOC-fa1de2cb7c79#요청응답 | Gleo Action에 앱 Callback으로 응답하는 계약 | 1절. 임의 수신 알림 Callback 전송을 지원한다고 주장하지 않음 |
| AUDIO-01 / DOC-f04338dec3b6#포커스 | 재생 전 속성/포커스, 상실 처리·반납, 매트릭스 적용 | 4절 1·2단계. 실제 SDK 포커스 소유자/오디오 속성 확인 후 구현·충돌 테스트 |
| AUDIO-02 / DOC-a243773e232d#Suspend | Suspend 전 출력 중단·포커스 반환, Resume 시 자동 복구 금지 | 3·4절. 전원 이벤트 지원을 먼저 확인하고 정리/재평가 검증 |
| DOC-2cbe51f12c43#선행묵음 | 안내 시작 전 묵음, 150~200ms 권장, 오디오 데이터 레벨 우선 | 4절 2단계. SDK 적용 여부 확인·첫 음절 실청취. delay를 묵음으로 대체하지 않음 |
| DOC-caeada44e9ec#의존성 | Pleos Maven·TTS 3.3.1 가이드 기준 | 4절 1단계. 현재 SDK/Gradle resolve 및 컴파일 |
| DOC-fac75a3790c3#권한 | TTS_SERVICE 선언 및 실제 권한, 거절 오류 처리 | 4절 1단계. Manifest·권한 허용/거절 계측 |
| DOC-5ec59d586502#수명 | initialize 후 API 사용, 동일 리스너 해제 후 release, 해제 뒤 재생성 | 4절 1·2·4단계. SDK Fake와 종료/재초기화 테스트 |
| DOC-426eee956b29#완료 | onReady/onStart/onDone/onStop/onError 의미 구분, 동일 인스턴스 리스너 제거 | 4절 2단계. onDone만 정상 완료·늦은 콜백/오류 처리 |
| DOC-ed4df99eb4d2#언어 | speak 전에 locale 설정, ko-KR 지원 | 4절 1단계. Locale.KOREA 및 한국어 재생 확인 |
| DOC-d44a8e787a9e#등록 | SERVER 및 HYBRID 서버 사용은 자격 증명 등록/onConnected 필요, ON_DEVICE는 불필요 | 4절 1단계. 초기 ON_DEVICE 제안, 서버 선택 시 외부 전송/키 정책 확인 |
| DOC-abf8dfd20e18#발화 | speak 완성 문장 기본 isFinal=true, stop 뒤 onStop | 4절 2단계. 한 발화 직렬 처리·종료 이벤트 검증 |
| DOC-72916a4c511a#실패 | 초기화/권한/네트워크/엔진/언어/요청 오류와 알 수 없는 타입 처리 | 3·6절. 실패해도 수신 데이터/화면 유지·코드만 로그 |

생성형 API의 class/package/메서드 페이지는 탐색했으나 웹에서 상세 시그니처/본문을 확인하지 못했다. source-map의 생성형 API 미러 제외 방침에 따라 본문 없는 사본을 index에 만들지 않고, source-map과 상세 계획에 확인 URL·미확인 범위를 연결한다. 기존 규칙 ID를 사용하며 새로운 SDK 동작 의무를 추측해 추가하지 않는다.

## 검증 결과

- PASS: 현재 브랜치·수신 트랜잭션·UI/음성 ACK 분리·서비스 수명·현재 의존성 확인. 공식 가이드의 확인된 요구를 계획에 연결했다.
- NOT_TESTED: 실제 TTS SDK resolve/설치/권한/음색/포커스·선행 묵음·한국어 재생·전원/중단 동작. 이번 단계는 계획이므로 앱 기능 완료를 보고하지 않는다.
- PASS: `python3 scripts/verify_harness.py --report docs/work/gleo-tts-plan.json` 하네스 133개 원문/27개 규칙 검사 통과. 보고서 7개 항목은 위 실제 SDK·오디오 검증 전이므로 exit=2(NOT_TESTED 포함), 기능 완료 판정이 아니다.
- PASS: 문서 로컬 링크/코드 블록, git diff --check, 변경 파일 목록을 검사해 계획·작업 기록·공식 문서 지도만 바뀌었음을 확인했다. 새 원문 요구는 기존 규칙에 연결하고 생성형 API 미러 제외는 명시했다.
- N/A: 앱 코드 변경·빌드·새 기능 테스트·자동 음성 호출·커밋/푸시.

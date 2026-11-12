# 현재 기능 브랜치 숫자 정책 조사

2026-11-11. 요청: 숫자 제한과 선정 근거 설명. gm/feat-speech-to-text의 미커밋/미추적 파일을 포함한 작업 트리 기준. 앱 코드는 수정하지 않는다.

## 범위와 규칙

- INT-01 / DOC-24357cf5e59a#함수개수: App Schema 2.1의 actionCalls 최대 3개. 파라미터 최대 3개라는 뜻이 아니다. 현재 함수 1개, 필수 입력 3개.
- INT-01 / DOC-c6fc94252d66#기한: Callback 2.2의 Gleo 7초 취소. 앱 내부 5초 정책과 구분한다.
- PRIV-01 / DOC-4036191ed19c#보유기간: 보유기간 고지·최소 수집·철회 삭제. 공식 문서에서 7일이라는 수치를 지정한 것으로 해석하지 않는다.
- 수정 범위: 이 조사 기록만. 코드/테스트/설정/호출자는 변경하지 않는다. UI 디자인 적합성 및 암호 알고리즘 적합성 평가는 범위 밖.
- 확인 방법: main 소스/스키마의 숫자 전수 검색, 현재 파서/저장/Receiver/Probe/UI 읽기, 설계 정본 및 기존 작업 기록 대조, 도입 커밋 탐색. App Schema/Callback 공식 웹 원문 2026-11-11 재확인.

## 실제 적용 값

| 항목 | 값과 동작 | 근거 수준 |
|---|---|---|
| content | 비공백, 원문 String.length <= 500 검사 후 trim | 프로젝트 선택. 500 자체의 실측/산식 근거는 발견하지 못함 |
| locationMode/category | 각각 원문 길이 <= 32, 비공백; trim 후 CURRENT_ROAD / WARNING·INFORMATION만 허용 | 프로젝트 선택. 32 자체의 산정 기록 없음 |
| ACTION_PARAM | UTF-8 <= 8192바이트(8KiB), 인코딩 전 String.length도 <=8192 | 큰 외부 입력 방어. 정확히 8KiB를 택한 정량 근거 없음 |
| 로컬 초안 | 최대 100건, 신규 초과 실패, 기존 자동 축출 없음 | 기존 초안 보존을 명시한 스프린트 정책. 100 자체의 산정 기록 없음 |
| 만료 | 최초 수신 + 7일 경계부터 제외, 갱신으로 연장 없음; list/save 때 물리 정리 | 개발 브랜치 정책으로 명시. 7일 자체의 산정 기록 없음. 미실행 중 삭제 예약 없음 |
| 저장 파일 | 암호화된 확정 파일 읽기 전에 >2,000,000바이트이면 IOException | 손상/과대 파일 방어로 해석(코드 추론). 쓰기 시 동일 크기 검사는 없음. 2MB 선정 산식 없음 |
| 수신 처리 | withTimeout 5000ms, TIMEOUT 실패 | 공식 7초 취소보다 먼저 응답하기 위한 내부 목표. 블로킹 IO 즉시 중단 보장 없음 |
| debug 진단 | 최근 10건, parameters/extras 각각 8192 String 단위로 절단 | 메모리 관측용 프로젝트 선택. release 기록 없음 |
| debug extras | 최대 32키, 키 100 String 단위, 문자열 값 8192 String 단위 | 진단 데이터 크기 방어(코드 추론). 숫자별 정량 근거 없음 |
| 초안 UI | 본문/버튼 최대 3줄 + 말줄임, 글자 20sp, 본문 행높이 28sp, 버튼 최소높이 80dp | 표시/조작을 위한 구현 값. 저장 본문을 3줄로 자르는 정책은 아님. 실차 판독성 근거는 이번에 검증하지 않음 |

String.length/take는 UTF-16 코드 단위다. 이모지 등은 한 글자당 2개 이상으로 셀 수 있다. 계획 정본은 content를 trim 후 1~500자로 설명하지만 코드는 trim 전에 상한을 검사한다. 파일 저장 계층 자체에는 500자 검증이 없고 Gleo 파서 경계에서 적용한다. DataOutputStream.writeUTF의 필드별 modified UTF-8 65,535바이트 한계도 존재한다(플랫폼 형식, 제품 정책 아님).

기술 상수: 저장 포맷 쓰기 v2/읽기 v1~v2, 암호 헤더 4바이트/IV 12바이트/인증 태그 128비트(16바이트), 최소 암호 파일 길이 32바이트. AES 키 크기는 앱에서 setKeySize로 고정하지 않는다. SDK 버전·색상·간격·Preview 치수·테스트 fixture 숫자는 제품 제한과 구분한다.

근거 파일: GleoActionParser.kt:21,38,67 / DraftFileStore.kt:50,52,106,134,233 / GleoActionReceiver.kt:56,98 / GleoProbe.kt:34,56,63 / DraftScreen.kt:138,150 / DraftCipher.kt:21,26,39. 문서: docs/plans/sprint-1-gleo-drafts.md:90, docs/work/sprint-1-friday.md:20,38, docs/work/sprint-1-jarvis.md:15.

## 계획에만 있는 값

docs/plans/implementation-plan.md 5·6·7·8절과 docs/work/prototype-e2e-spec.md:19 기준:

- 수신 30초 전 위치 배치, 메모리 버퍼 30초, 연속 수집 준비 30초.
- 샘플 공백 2초 초과면 재준비, 최신 샘플 2초 이내, 목표 이후 2초 이내 샘플 선택(실제 오프셋 28~30초).
- 샘플 정확도 20m 이하, 시연 수신 목표 1Hz(공식 보장 아님).
- 접근: 전방 진행차 0~150m 및 직선거리 150m 이하, 같은 경로/방향. 한 화면 쪽지 1개, 사용자별 같은 쪽지 수신 이력 1개.
- Room 쪽지 TTL 10분(초안 7일과 별개), 만료 즉시 조회 제외/세션 시작·심기·리셋 시 삭제.
- 노트북 1대/에뮬레이터 1대/앱 1개에서 가상 사용자 A/B 시연.

위 값은 합성 시연 초기 프로젝트 선택이라고 명시돼 있다. 각 숫자의 실측 최적화 근거는 없다. 30초는 목격 시각 실측치가 아니다. Room/위치 버퍼/접근 판정은 현 코드에 구현되지 않았다. 과거 gleo-interface-design.md의 placeQuery 200자, draftId 128자, directionDescription 200자, observedAt 40자는 현행 입력 정책이 아니다. 파서는 해당 과거 필드를 실행 입력에 넣지 않는다.

## 결과

- PASS: 현재 코드와 정책 문서의 수치 조사, 공식 App Schema 최대 함수 3개 및 Callback 7초 원문 확인.
- 차이 발견: trim 전/후 길이 정책 불일치, 2MB 읽기 검사와 쓰기 상한의 차이. 이번 요청은 설명이므로 수정하지 않음.
- NOT_TESTED: 런타임 경계 동작 재실행, 실차/에뮬레이터 응답 시간·UI 판독성, 메모리/디스크 최적 수치 산정. 기존 테스트의 존재를 실행 PASS로 보고하지 않음.
- N/A: 코드 변경 빌드/테스트, 새 기능 구현, 커밋/푸시.
- PASS: `python3 scripts/verify_harness.py` — harness wiring, 133 source pages, 27 rule gates, static Manifest. UI/차량 런타임/문서 최신성/심사 적합성을 검증한 결과는 아니다.

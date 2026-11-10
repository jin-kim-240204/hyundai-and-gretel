# 즉시 제보 프로토타입 E2E 설계 개편

요청: implementation-plan을 현도로 단발 제보·수신 30초 전 위치·FusedLocation 30초 버퍼·Room A/B 한 노트북 시연의 전체 테크스펙으로 재작성. 심기부터 접근 수신까지 데이터/상태/인터페이스/재현/구현 순서 설계. 앱 스키마 계약과 문서 정합 확인. 기능 구현 요청으로 확대하지 않음.
수정 범위: docs/plans/implementation-plan.md, gleo-interface-design.md의 현행 계약 진입 안내, 작업 기록. assets/schema.json은 이미 content/locationMode/category 단발 계약이므로 그대로 재사용한다. SDK/Room/화면 유지 구현은 계획이며 현재 완료로 표시하지 않는다.

| 규칙 / 세부 원문 ID | 문서 절·요구사항 | 설계/검증 |
|---|---|---|
| INT-01 / DOC-24357cf5e59a#단발입력 | App Schema 2.1/4: 영어 설명, string enum, 실제 필수 입력 | 스키마 3개 필드·필수값·함수명/예시 동기화 정적 확인 |
| INT-01 / DOC-c6fc94252d66#저장결과 | Callback 2.1/2.2: 함수명 일치, success/fail, data 배열 문자열, 7초 timeout | 커밋 후 성공·한 번 응답 설계. 기존 preview와 목표 실제 저장 명확히 구분 |
| VEH-01 / DOC-402de45b9f3d#위치권한 | FUSED_LOCATION dangerous 권한, 권한 거절/프라이버시 차단 | 동의/권한 게이트 및 null/error 상태 명시 |
| VEH-03 / DOC-1ba06876919b#수명 | isSupported→initialize→등록, 동일 listener 해제→release | 포그라운드 세션 시작/종료와 연결 |
| VEH-03 / DOC-9844e9eaad3c#업데이트 | 마지막 위치는 신규 측정 아님, onChanged nullable/onError, 동일 listener 해제 | 연속 측정 버퍼, 측정 시각/신선도 확인을 구현 게이트로 지정 |
| VEH-03 / DOC-ba9b83fc86f6#오류 | null을 정상 위치로 사용하지 않음 | 위치 실패 시 임의 대체 금지 |
| SAF-01 / DOC-9faba42791f0#화면유지 | DO 선언 후에도 CarUxRestrictions/ActiveRestrictions 반영 | 주행 표시와 keep-screen-on 구분, 주차에서만 A/B/재설정 |
| PRIV-01 / DOC-72a5d7ab4ebb#최소수집 | 목적 고지/동의, 로그 최소화 | 메모리 30초, 합성 Room DB 분리·초기화·보존 한도 |
| ENV-02 / DOC-e68476b79d1c#전환 | 창 크기·전경/배경/재생성 처리 | Activity 구조 유지, 버퍼 소실/재가열 명시 |

2026-11-09 공식 웹 재확인: 위 FusedLocation 위치/권한/수명, Gleo App/Callback, 주행 규제 가이드. 기존 로컬 스냅샷과 관련 요구사항 차이 발견 없음. Android Room/keep-screen-on 공식 문서도 확인. 상세 URL은 구현 계획의 공식 근거 절에 연결한다. SDK LocationData 심볼(측정 단조시각·accuracy·bearing 등) 및 에뮬레이터 위치 주입 전달은 미검증이므로 가정과 실제 지원을 구분한다.
프로젝트 선택: 30초 고정 오프셋, 2초 샘플 오차/신선도, 20m 정확도, 150m 접근 거리, 10분 쪽지 TTL, 1개 에뮬레이터 A/B. 공식 수치가 아니며 합성 시연 초기값으로 문서화한다.
검증 예정: Markdown/JSON/스키마 스냅샷·Mermaid 구조·현재 코드 대비 상태 검토, harness, report. 문서만 변경하므로 빌드/UI/차량 동작 테스트 N/A. 계획 기능의 실행 검증은 NOT_TESTED.

## 검토 결과

- PASS: implementation-plan 전면 재작성. 제보→30초 anchor→Room commit→B 접근 판정→NoteReceipt→화면 ACK의 책임/상태/저장·갱신 및 오류 정책 연결.
- PASS: 현재 assets/schema.json과 문서의 APP_SCHEMA 스냅샷 JSON 동등성. 3개 필드 및 required 계약 유지. 기존 스키마 내용은 변경할 필요 없어 코드 파일 수정 없음.
- PASS: JSON 코드 블록/콜백 data 배열 문자열 파싱, 상대 링크 존재, Mermaid flowchart/sequence/state 3블록 구조 및 fence 확인. 시각 렌더링은 NOT_TESTED.
- PASS: verify_harness 및 git diff --check. 문서만 변경하여 Gradle/앱 동작 검증 N/A.
- 설계 검토에서 보강: 실음성 지연을 무시하는 고정 x=300 시연 대신 수신 순간 anchor에서 B 출발 위치를 계산. REPLAY만 시험 시계/재생을 함께 정지하며 시스템 시계는 수정하지 않음. SDK/REPLAY와 합성/실측 provenance를 분리. note requestId UNIQUE 및 수신 복합키로 중복 정책 명시.
- NOT_TESTED: FusedLocation 측정 필드·위치 주입·권한/프라이버시/실제 lifecycle, CarUxRestrictions/화면 유지, Room 트랜잭션·접근 판정 및 Gleo 저장 후 음성 E2E. 문서 완료와 기능 완료를 구분한다.
- 커밋/푸시는 이번 문서 요청에 포함하지 않아 수행하지 않음.

## 도표 이미지 후속 반영

사용자 미리보기에서 Mermaid 코드만 표시되어 Mermaid CLI로 3개 도표를 SVG 렌더링하고 implementation-plan의 코드 블록을 이미지 참조로 교체했다. 원본은 docs/plans/diagrams/*.mmd에 보관하며 SVG 크게 보기/원본 링크를 제공한다. 임시 /tmp에 렌더러 설치, 프로젝트 의존성 추가 없음.
PASS: 3개 Mermaid 문법 실제 렌더, SVG XML 파싱, 이미지/원본 링크 존재, Chrome 렌더 PNG로 한글·화살표·배치 시각 확인. 앞의 Mermaid 시각 렌더링 NOT_TESTED는 이 결과로 대체한다. 특정 사용자의 Markdown 뷰어에서의 표시 자체는 별도 미확인. 앱 기능은 변경 없음.
정적 검사 최초의 error-icon 문자열 탐색은 정상 SVG 공통 CSS까지 감지해 실패했으므로 실제 XML 요소 class 검사로 수정했고 통과했다.

## 주행 시연 정정

사용자 정정: 시연은 주차가 아니라 주행 중이며 기존 distractionOptimized=true 및 주행 감지 화면 전환 원복을 유지한다. MainActivity/Manifest/revert-driving-ui.md를 확인했다. implementation-plan의 주차 A/B 전환·축소 UI 계획을 제거하고 주행 상태를 유지하는 동일 화면 시연으로 수정한다. 로직 재현은 t=60초에 테스트 JSON 예약 주입, 실제 Gleo는 계속 주행하며 수신 순간 기준으로 검증. 가상 경로/역할 전환과 차량 상태/화면 전환을 구분한다. 문서/도표만 변경, 앱 코드 변경 없음. SAF-01 공식 요구와 에뮬레이터 시험 정책을 구분하며 정식 배포 규제 검증은 미완료로 유지한다.

정정 검증 PASS: 주차 전환/축소 UI 구현 계획 잔여 없음, 3개 Mermaid SVG 재렌더, 문서/도표 정합성 확인. 실제 주행 동작은 문서 변경 범위 밖으로 재검증하지 않음.

## 주행 지원과 위치 수신 조건 명확화

사용자 정정: 주행 중에도 동작해야 한다는 의미이며 정차를 금지하는 것이 아니다. Mermaid 원본 3개에서 주행 유지 필수 표현을 제거하고 주행/정차와 무관한 위치 기반 판정을 명시. 구현 계획에도 속도/기어로 제보·수신을 차단하지 않으며 이미 반경 안에서 시작·정차 중 쪽지 생성도 처리하도록 위치+Room 변경+B 시작 트리거를 설계했다. 반경 안/밖 정차 테스트를 추가했다. 기존 위치 품질/방향 신뢰도·중복 방지 정책 유지. 앱 코드는 변경하지 않았다.

검증 결과: 상태도 최초 단일행 note가 Mermaid 렌더 오류를 일으켜 이미 선언된 Receiving에 다중행 note로 수정. 3개 SVG 최종 렌더 성공, Chrome 화면 확인 및 XML 텍스트 확인 PASS. 주행·정차 정책 문서/원본 정합성 및 하네스 PASS. 기능 동작 검증은 미실행.

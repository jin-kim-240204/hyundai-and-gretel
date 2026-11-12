# 도로 제보 발견 시각 계약 수정

2026-11-08. 사용자 요청: Gleo가 현재 시각에서 상대 시간을 빼서 발견 시각을 전달. CURRENT_ROAD는 지명 없이 최근 시각/차량 방향을 사용. 오래된 제보는 지명 질문. 선택 필드 빈 문자열은 미제공.

범위: schema, 파서, debug/release 공통 응답, 저장 전달, Gleo 단위 테스트, 계획 3종. 기존 저장 포맷/콜백 구조/차량 API/게시 기능은 변경하지 않는다.
프로젝트 선택: 최근은 수신 시각 대비 10분 미만. 정확히 10분부터 지명 필요. CURRENT_ROAD 시각 누락은 asking, 형식/미래 시각 오류는 fail. 오래된 CURRENT_ROAD는 NEEDS_CLARIFICATION/UNKNOWN으로 정규화해 현재 차량 방향 오용 방지. utteranceRecordedAt 이름은 호환 유지하되 발견 시각으로 의미 변경. LLM 계산값이며 센서 측정값으로 보장하지 않음.

INT-01 적용. 공식 https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability (2026-11-08 재확인):
- DOC-24357cf5e59a#함수파라미터: service는 Broadcast, 이름/enum/타입/required 계약 유지.
- DOC-24357cf5e59a#설명: 영어 설명, 구체적 발화 예시나 타 기능 억제 없이 의미를 1~2문장으로 설명.
- DOC-24357cf5e59a#선택필드: 전 모드 공통 필수만 required에 유지, 모드별 필수는 앱 검증.
검증: testDebugUnitTest, verify_harness --build, Studio build, report. 실제 Gleo 상대시간 계산과 차량 이력 기반 위치 배포는 NOT_TESTED. 차량 API/UI 변경 N/A.

## 검증 결과
- PASS: testDebugUnitTest 42개, failures=0/errors=0. compileDebugAndroidTestKotlin PASS. 첫 테스트의 이전 fixture 개수 가정을 제거한 후 관련 검사 재실행.
- PASS: Studio build_project, git diff --check.
- 실제 Gleo E2E는 NOT_TESTED. 설치/재시작/음성 주입은 수행하지 않음. Gleo가 임의 과거 날짜를 생성하면 이제 현재 도로 성공이 아니라 지명 질문이 됨.
- 기존 초안은 저장 포맷과 원시 시각값을 보존하며 새 의미로 재계산/재게시하지 않음.
- PASS: 최종 verify_harness.py --build (assembleDebug/lintDebug 포함). 보고서 ID 형식 수정 후 재검사.

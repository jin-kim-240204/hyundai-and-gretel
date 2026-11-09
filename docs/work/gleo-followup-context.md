# Gleo asking 후속 문맥

2026-11-08 사용자 요청: 제보 질문/답변을 이어가고 완성 전 최종 데이터 반환 보류.
실제 증거: 03:40:49 앱 입력은 content/locationMode만 포함. 시각 누락 asking에는 missingField만 있고 기존 제보 문맥이 없음. 이후 제공된 classifier 로그는 datetime agent 선택이며 앱 미도달. 이를 앱 세션으로 가로챌 수는 없음.

INT-01 적용. 공식 원문 https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback (이번 세션 확인):
- DOC-c6fc94252d66#상태: 후속 정보 필요 시 asking, 정상 완료 success, 실패 fail. answer status 없음.
- DOC-c6fc94252d66#문맥: data는 JSON 객체 배열 문자열. 후속 대화에 필요한 정보 제공 권장. asking에 missingField/pendingReport로 현재 내용·모드·방향/시각·지명 보존. 최종 report는 success에만.
- DOC-c6fc94252d66#기한: 공식 7초 timeout. 앱의 내부 5초 응답 유지. 사용자 답변까지 pending broadcast를 붙잡지 않음.
- DOC-24357cf5e59a#설명: 영어 단문으로 후속 완성 의미와 content 보존 의미를 schema/계획에 동기화.

범위: parser/processor/probe/Callback/schema/계획/회귀 테스트. 시각 누락도 Valid+missingField로 만들어 기존 내용 보존. 미완성 입력은 저장하지 않음. debug/release 질문 함수 공유. 완성된 제보는 확인 질문 없이 기존 success 경로. 앱 발신자/저장 동의/실게시 기능은 변경하지 않음.

한계: 서버가 후속 발화를 datetime으로 분류하면 앱에 도달하지 않음. Gleo의 재분류/문맥 재전달은 실제 음성 E2E 전 NOT_TESTED. 스키마만으로 현재 시각 접근 보장 불가. 세션을 로컬 최신 요청과 임의 연결하지 않음. 중간 Callback JSON은 timeout 방지를 위해 필요하며 최종 제보 JSON과 구분한다.

검증 PASS: testDebugUnitTest 44개 failures=0/errors=0, Studio build_project, git diff --check. 실제 음성/Android JSON 직렬화는 NOT_TESTED.

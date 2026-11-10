# 제보 모드 보존 및 즉시 응답

2026-11-08. 사용자 요청: CURRENT_ROAD에서 지명을 묻지 말고, 모드별 필수 정보가 갖춰지면 제보 JSON을 즉시 반환. 이전 road-report-time.md의 오래된 CURRENT_ROAD 자동 전환 결정을 대체한다.

INT-01, DOC-24357cf5e59a#함수파라미터(공식 app-capability 2.1/4절, 이번 세션 확인), DOC-c6fc94252d66#응답(공식 https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback의 success/asking/fail 및 data 배열 문자열) 적용. schema enum/파서/processor/probe/Callback/tests/계획 수정. 저장 포맷·기존 내부 enum 유지, 외부 신규 NEEDS_CLARIFICATION 제거.
지명 질문은 NAMED_PLACE의 placeQuery 누락에 한정. CURRENT_ROAD 시각 누락은 해당 필드 질문, 미래/형식 오류 및 10분 이상은 fail. 완성된 제보는 debug/release 모두 success와 report 필드를 반환. 서버/위치/차량 이력/실배포 미구현이므로 published=false 유지. 사용자의 게시 요청은 실제 배포까지 미완료.
검증: 단위 테스트/하네스 빌드/Studio/보고서. 실제 Gleo 음성은 NOT_TESTED.

PASS: 단위 테스트 {'tests': 42, 'failures': 0, 'errors': 0}, verify_harness --build(assembleDebug/lintDebug), Studio build_project, git diff --check. 실제 Gleo/게시 NOT_TESTED.

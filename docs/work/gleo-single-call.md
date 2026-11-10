# 단발 즉시 제보 계약

요청: draftId 및 asking/추가 정보 이어받기 제거. 요청별 success/fail 한 번 응답.
범위: Gleo schema/파서/처리기/콜백/디버그/Receiver timeout/테스트/계약 문서. 내부 저장 ID 및 기존 파일/화면은 유지한다.
INT-01 / DOC-24357cf5e59a#입력: App Schema 2.1/4, content/locationMode/category만 노출 및 필수 검증. CURRENT_ROAD만 허용, 방향은 앱 기본 CURRENT_VEHICLE. draftId 등 미지정 키는 실행 파라미터로 사용하지 않음.
DOC-c6fc94252d66#단발응답: Callback 2.1/2.2, success/fail만 사용하고 asking/pendingReport/missingField 제거. 누락 입력은 선언형 오류 응답, 추가 질문 없음. timeout도 fail 1회 시도. 취소/전송 실패/프로세스 종료까지 수신 보장을 의미하지 않음.
원문 스냅샷: references/sources/24357cf5e59a.md 및 c6fc94252d66.md, Source 공식 URL. Gleo가 앱 호출 전후 자체 대화를 진행하는지 여부는 앱 콜백만으로 보장 불가.
저장 실패/동의 검증은 유지. success는 수신/검증(디버그) 또는 로컬 저장이며 published=false 유지.
검증 예정: 누락/잘못된 enum fail, 독립 호출마다 저장, draftId 무시, 저장 실패·취소·동의 검증, 전체 테스트/빌드/린트/IDE 빌드.
NOT_TESTED: 실제 Gleo 대화 종료/음성 안내 및 timeout E2E. N/A: UI/차량 API/저장 형식 변경.

결과: 입력 3개 스키마 단정 PASS. 전체 단위 테스트 PASS(누락/타입/enum fail, 구 draftId 무시, 독립 호출별 저장, 동의·저장 실패·취소 유지). 하네스 --build/compileDebugAndroidTestKotlin/Studio build_project/git diff --check PASS. 계측 테스트에는 callback에 draftId/pendingReport/missingField가 없는지 검증을 추가했으나 실행은 NOT_TESTED(컴파일만 PASS). 실제 Gleo 대화 종료와 콜백 전송 실패/timeout E2E는 NOT_TESTED.

# 제보 카테고리

요청: LLM이 content로 분류하는 string category, WARNING(경고)/INFORMATION(안내). 스키마 영어 규칙 유지.
범위: schema, 입력 모델, 파서, 콜백, 로컬 저장, 단위/계측 fixture, 인터페이스 스키마 예시. UI 확장 없음.
INT-01 / DOC-24357cf5e59a#카테고리: App Schema 2.1/4의 string enum 및 입력 의미 설명, category 필수. 위험·안전상 주의는 WARNING, 그 외 도로 정보는 INFORMATION. 잘못된 타입/enum 거절, 누락 질문.
DOC-c6fc94252d66#응답데이터: Callback 2.1/4, report/pendingReport에 분류값 보존.
원문은 .agents/skills/pleos-connect/references/sources/24357cf5e59a.md 및 c6fc94252d66.md, 공식 URL은 각 Source 항목. 기존 규약 확장으로 신규 API 판단 없음.
저장 v2에 category 추가, v1 읽기는 유지하고 이전 기록은 미분류(null)로 보존한다. 임의 분류/데이터 삭제 없음.
검증 예정: enum/누락/타입/응답 보존 단위 테스트, v1→v2 저장 호환 테스트, 하네스 --build, IDE 빌드.
NOT_TESTED: 실제 Gleo 분류 정확도 및 음성 호출. N/A: 차량 API/UI 변경.

결과: 스키마 단정 PASS. 전체 testDebugUnitTest PASS(두 분류값 수용·미지원 enum/타입 거절·누락 asking·pending/final report 보존·v1 기존 초안 무손실 및 v2 재시작 분류 보존). 하네스 --build 및 Studio build_project PASS. 실제 Gleo 분류 정확도 NOT_TESTED.

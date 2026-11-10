# Gleo 함수 설명 변경

2026-11-08 사용자 요청: 함수 설명을 도로 상태 제보로 변경.
INT-01 / DOC-24357cf5e59a#설명 적용. 공식 App Schema Writing Guide 4절(이번 세션 확인): 영어로 목적을 간결하게 설명하고 발화 예시/다른 기능 억제 지시를 넣지 않는다.
원문: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability
schema.json과 gleo-interface-design.md의 함수 description을 Report road conditions observed by the user. 로 동기화. 함수명/처리/Callback 계약 변경 없음.
PASS: JSON 파싱. 빌드/하네스는 아래 결과 기록. 실제 분류 변화는 NOT_TESTED(설치/음성 실험 미실행).

PASS: verify_harness, git diff --check, Studio build_project. 보고서 exit=2는 실제 Gleo 분류 변화 NOT_TESTED에 따른 결과.

# 도로제보 호출어 후보

2026-11-08 사용자 요청: “도로제보할게”로 시작하면 우리 앱 함수를 쓰도록 호출어 지정.
INT-01 / DOC-24357cf5e59a#호출어예시 적용. 공식 https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability 3절 예시에 invokeWord 문자열이 있음(2026-11-08 재확인). 2.1 필드 명세에는 의미/필수/매칭·강제 라우팅 보장 설명 없음. 공식 의무가 아닌 사용자 요청에 따른 시험 적용이다.
DOC-24357cf5e59a#설명: 4절 영어 description/예시·타 기능 억제 금지 유지. description에 라우터 강제 지시를 추가하지 않음.
schema.json 최상위 invokeWord=도로제보 추가, 계획 스키마 동기화. 함수명/파라미터/Callback/앱 라벨 변경 없음.
검증: JSON 파싱/하네스/IDE 빌드. 실제 “도로제보할게” 발화→on-device_call_app_action_agent→CREATE_ROAD_NOTE_DRAFT는 NOT_TESTED. 앱만으로 Gleo 분류 강제 불가.

PASS: JSON 파싱, verify_harness, git diff --check, Studio build_project. 보고서 exit=2는 실제 함수 선택 효과 NOT_TESTED 때문이다. 설치/음성 실험 미실행.

## 호출어 변경
사용자 후속 요청으로 invokeWord를 도로제보에서 제보로 변경하고 계획 문서를 동기화했다. 기존 호출어의 로딩은 사용자 로그에서 확인됐으나 앱 함수 선택은 실패했다. 새 호출어의 실제 선택 효과는 NOT_TESTED.

# 목격 시각 필드명 변경

요청: utteranceRecordedAt을 observedAt으로 변경하고 directionMode를 Gleo 입력 스키마에서 제거한다.
범위: 스키마, 파서, 입력 모델, 콜백 JSON, 저장 코드, 단위/계측 테스트, 인터페이스 문서. 내부 directionMode는 기존 저장 호환과 CURRENT_VEHICLE 기본값 처리를 위해 유지한다. 저장은 필드명 없는 순서 기반 바이너리이므로 필드 순서/버전은 바꾸지 않는다.

규칙 INT-01:
- DOC-24357cf5e59a#입력: App Schema 2.1/4의 명확한 parameter 이름 및 실제 필수 입력 구분. observedAt은 목격 시각이며 기존 ISO 8601 검증·10분 경계·누락 질문 정책 유지. directionMode 입력 노출 제거.
- DOC-c6fc94252d66#콜백: Callback 2.1/4에 따라 누락 필드와 반환 report에도 observedAt 사용.
원문: .agents/skills/pleos-connect/references/sources/24357cf5e59a.md, c6fc94252d66.md. 공식 URL은 각 Source 항목. 이번 작업은 기존 계약의 이름 변경이며 신규 API/규정 판단 없음.
검증: JSON 단정, 전체 단위 테스트, 하네스 빌드/린트, IDE 빌드 예정.
NOT_TESTED: 실제 Gleo 스키마 재등록/음성 호출. 수신 시점 차량 방향 조회는 여전히 미구현.
N/A: UI·차량 API·저장 포맷 변경.

결과: 스키마 단정·하네스 --build(assembleDebug/lintDebug)·전체 testDebugUnitTest·Studio build_project PASS. app/src의 옛 필드명 잔여 참조 없음. 실제 Gleo 호출 NOT_TESTED.

# 즉시 목격 제보 스키마 정리

요청: 앱 목적·content·방향 설명 수정, CREATE_ROAD_NOTE로 식별자 통일, Gleo 노출 위치 모드는 CURRENT_ROAD만 유지. 추가 정리는 함수/모드 설명과 사용하지 않는 placeQuery 제거로 한정한다.

범위: assets/schema.json, Manifest action, 파서 상수, 콜백, 디버그 표시, 식별자 테스트, 인터페이스 문서의 스키마. 기존 저장 데이터와 내부 NAMED_PLACE/DEFERRED 타입·분기는 유지해 저장 호환성을 보존한다. 과거 작업 로그는 변경하지 않는다.

적용 규칙 INT-01 및 원문:
- DOC-24357cf5e59a#이름: App Schema 2.1/4, 고유한 대문자/밑줄 함수명. CREATE_ROAD_NOTE 적용.
- DOC-24357cf5e59a#설명: 주요 목적과 입력 의미를 간결히 기술. 모든 description은 영어로 작성한다. 사용자의 후속 정정에 따라 directionDescription을 제거하고 directionMode 노출 enum은 CURRENT_VEHICLE만 유지한다.
- DOC-c6fc94252d66#함수명일치: Callback 4, functionName은 스키마 함수명과 일치해야 한다.
- DOC-2bec764daf11#등록: assets/schema.json과 Manifest 필터를 함께 갱신. 실제 기기의 패키지 접두사 형식을 유지한다.
공식 URL은 .agents/skills/pleos-connect/references/sources/각 해시.md의 Source에 연결되어 있다. 스냅샷 기반 계약 검토이며 최신 공식 문서 재확인은 미수행.

검증 예정: JSON 단정 검사, 하네스 --build, Gleo 단위 테스트, IDE 빌드.
NOT_TESTED: 실제 Gleo 재등록·음성 호출. 방향 조회·서버 공유는 이번 스키마 수정으로 구현되지 않는다. 수신 시점 차량 방향 조회는 후속 구현 필요.
N/A: UI 레이아웃, 차량 API 변경, 저장 형식 변경.

검증 결과: JSON/action 단정 PASS, 하네스 --build(assembleDebug/lintDebug) PASS, GleoActionTest PASS, Studio build_project PASS. 병행 빌드 중 Kotlin daemon 오류는 fallback 컴파일로 복구되어 최종 빌드는 성공했다. 실제 Gleo 호출 및 방향 자동 채움은 NOT_TESTED.

후속 정정: 한국어는 요구사항 전달 언어였음. directionDescription 제거, directionMode는 CURRENT_VEHICLE만 노출. 내부 저장 필드·파서는 저장 호환성을 위해 유지. 설명 전체 ASCII 영어 여부와 제거 필드/enum JSON 단정 검사 PASS.

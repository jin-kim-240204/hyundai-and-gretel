# 즉시 제보 수신 시각 기준

요청: observedAt을 Gleo 입력에서 제거. 즉시 목격 프로토타입은 앱이 JSON을 받은 receivedAt을 동선 확인의 시간 기준으로 사용.
범위: schema, 파서/콜백/추가 질문, 호출부 및 테스트, 인터페이스 문서. 기존 receivedAt=Receiver 진입 Instant.now() 경로 재사용. 차량 동선 조회/게시 기능은 이번 범위에서 새로 구현하지 않음.
INT-01, DOC-24357cf5e59a#즉시제보: App Schema 2.1/4, 실제 필요한 파라미터만 노출. observedAt 제거, 오래된 시각/누락 시각 질문 분기 삭제. 과거 스키마가 보낸 observedAt은 다른 미지정 키와 동일하게 무시.
DOC-c6fc94252d66#응답데이터: Callback 2.1/4, 검증된 입력 반환에서 observedAt 제거. status success는 현재 구현에서 수신/검증 또는 로컬 저장 의미이며 서버 게시 완료를 의미하지 않는다.
원문 스냅샷: .agents/skills/pleos-connect/references/sources/24357cf5e59a.md 및 c6fc94252d66.md, Source의 공식 URL 참조. 신규 API/규정 판단 없음.
내부 observedAt 저장 필드는 이전 바이너리 데이터 보존 용도로만 유지하며 신규 Gleo 입력에는 채우지 않는다. 저장 형식/버전 변경 없음.
검증 예정: 시간 없는 즉시 제보 성공·과거 날짜 무시·receivedAt 전달 테스트, 전체 단위 테스트, 빌드/린트/IDE 빌드.
NOT_TESTED: 실제 Gleo 재등록/음성 응답 및 차량 동선 조회.

결과: 시간 입력/콜백 제거 단정 PASS, 전체 단위 테스트·compileDebugAndroidTestKotlin·하네스 --build·Studio build_project·git diff --check PASS. 디버그 성공 message는 The road report was received successfully.로 변경. PROBE_ONLY/published=false 유지. 설치/실제 Gleo 재등록·실발화는 미실행.

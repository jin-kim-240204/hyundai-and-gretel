# Gleo 거절 응답 진단

2026-11-08. 범위: 현재 에뮬레이터의 함수 선택 이전 단계 진단. 앱 코드 변경 없음.

적용 규칙 INT-01. 공식 원문 https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema 를 재확인했다.
- DOC-2bec764daf11#asset: assets/schema.json으로 스키마 제공. PASS: 현재 파일 존재 및 02:46:41.308 Gleo의 해당 패키지 Loaded schema.json 로그.
- DOC-2bec764daf11#filter: 함수 Action 수신 필터 필요. 정적 PASS: Manifest에 패키지 접두사 Action 존재. 이번 발화의 실제 수신은 NOT_TESTED: 함수 호출 단계에 도달하지 않음.
- DOC-2bec764daf11#scan: 부팅 시 스키마 스캔/DB 저장 설명. 로딩 로그만으로 서버 함수 선택 가능성까지 입증하지 않는다.

증거: 에뮬레이터 emulator-5554 logcat, 기기 시각 기준.
- 02:46:41.308 우리 스키마와 CREATE_ROAD_NOTE_DRAFT 로딩.
- 02:49:12.377 STT 최상위 결과는 ‘방금 지나온 길에 구덩이가 있었거든 제거해줘.’, 제보해줘는 하위 후보.
- 02:49:12.992 AGENTCLASSIFIER는 cloud_call_chitchat_agent 선택.
- 02:49:15.570 ‘그건 내가 할 수 있는 일이 아니야.’ 응답.
- 해당 로그에서 우리 함수 sendActionCall/GleoProbe 수신 증거 없음. 이번 실패는 앱 처리 이전 STT/라우팅 단계로 판정.

실제 음성 E2E: FAIL(함수 선택 이전 거절). 다른 컴퓨터의 동일 발화/설치 APK/Gleo 버전/서버 설정 비교: NOT_TESTED. 현재 관찰로 모든 환경 차이를 단정하지 않음.
다음 확인: 새 요청으로 ‘도로 상태 제보 초안을 만들어줘. 도로에 구덩이가 있어.’라고 발화한 뒤 STT → classifier → 함수 선택 → 수신 → Callback 비교. 사용자 음성 조작이나 Gleo 재시작은 수행하지 않았다.

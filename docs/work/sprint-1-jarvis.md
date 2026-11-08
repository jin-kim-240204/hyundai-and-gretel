# Sprint 1 자비스

2026-11-08. 사용자 승인: 스프린트/3역할 분담 작성 후 자비스 담당 구현 시작, 담당별 커밋과 마지막 통합은 자비스. 다른 AI는 사용자가 호출한다. 이번 브랜치 gm/feat-speech-to-text.

pleos-connect/ponytail/pleos-verify 적용. 현재 MainActivity·Gradle·Manifest·설계/작업 기록과 S1~S5·보안/개인정보 원문을 읽었다. 새 SDK/차량 제어·위치 API는 사용하지 않는다. 실제 외부 Gleo 인증 계약 부재 때문에 내부 Receiver만 준비하고 외부 연동은 미활성으로 남긴다.

## 범위·선택 규칙

ENV-02 / INT-01 / PRIV-01. 상세 담당/공통 타입/검증/스프린트는 docs/plans/sprint-1-gleo-drafts.md. 자비스는 계약·검증 파서·Callback·엔진·Schema·Gradle 패키지·최종 연결을 소유한다. 프라이데이는 LocalDraftStore, 이디스는 순수 DraftScreen을 소유한다. 기존 사용자 Manifest 4권한 변경과 다른 이전 기록을 자기 코드 커밋에 섞지 않는다.

| 세부 ID | 원문 요구사항 / 적용 | 확인 방법 |
|---|---|---|
| DOC-24357cf5e59a#함수파라미터 | 최대3/고유대문자·영어 description·지원 타입/문자열enum·required | schema/파서/단위 검사 |
| DOC-c6fc94252d66#응답 | 함수명·success/asking/fail·영어문장·voice·data 객체배열 문자열 | Callback 검사 |
| DOC-c6fc94252d66#기한 | 7초 취소, 내부5초 목표 | 수신 통합 후 실제 측정 필요 |
| DOC-f1d260bfb503#전달 | ACTION_PARAM 코드 예제와 개별extra 본문 차이 | 코드 예제 형식만 처리, 외부 실제계약 NOT_TESTED |
| DOC-2bec764daf11#활성화 | assets/schema.json·필터·검토/승인·스캔 | 파일/Manifest, 실제 등록 NOT_TESTED |
| DOC-27ac8605961f#입력신뢰 | 외부 입력 검증·공개API·민감정보 노출 방지 | 내부 수신점·모드/타입/길이·원문로그 없음 |
| DOC-72a5d7ab4ebb#최소수집 | 목적/동의/최소보유 | 미동의 저장금지·최소 Callback, 저장/UI 담당 연결 후 확인 |
| DOC-4036191ed19c#동의철회 | 명시동의·보유기간·철회시 삭제 | 7일 개발정책·저장/UI 검증 필요 |

검증: 의미 있는 파서/처리/응답 단위 검사, harness/build/lint/Studio 빌드. 문서/코드 구현 완료와 실기기·공식 활성화·외부 신뢰 경계 검증을 구분한다. 문서 계약 확정 뒤 자비스 구현을 즉시 이어간다.

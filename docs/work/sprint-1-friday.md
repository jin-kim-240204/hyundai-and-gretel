# Sprint 1 — 프라이데이

요청: 로컬 저장·동의·최초 수신부터 7일 만료·삭제·재생성·오류/동시성 구현. 2026-11-08.

## 범위와 확인 방법
- 독점 파일: roadnote/LocalDraftStore.kt, roadnote/DraftFileStore.kt, test/**/roadnote/*Store*Test.kt, 이 기록과 JSON. 공통 계약 v1 확인 완료. MainActivity/Manifest/Gradle/UI/수신 연결은 자비스/이디스 소유로 편집하지 않는다.
- 영향: DraftStore 호출자는 저장/읽기/철회 실패를 처리해야 한다. 삭제/철회/만료 정리는 의도적으로 데이터를 제거하며, 실패는 예외로 전달한다. 새 의존성 없이 JVM 파일 API + 코루틴 사용.
- noBackupFilesDir 단일 파일, IO dispatcher, 프로세스 전체 Mutex로 읽기-변경-원자 교체 직렬화. 임시 파일 fsync 후 같은 디렉터리 ATOMIC_MOVE. 파일 포맷 version/consent/count 및 각 필드. 손상은 IOException; 조용히 초기화 금지.
- 테스트: 재생성, 실패한 원자 교체, 원시각 보존, 없는/만료 ID, 7일 경계, 미동의/철회, 삭제/clear, 여러 인스턴스 동시 저장·삭제, 손상/100개 한도. :app:testDebugUnitTest, harness --build, Studio build_project, 보고서 검사.

## 규칙 및 공식 원문 세부 요구사항
2026-11-08 공식 URL 재확인. 저장 기능 관련 적용 규칙 PRIV-01. 차량/오디오/Gleo SDK/디자인 규칙은 이 저장소 구현 범위 N/A.
- DOC-72a5d7ab4ebb#최소수집: [개인정보 최소 수집](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy). 목적 설명/동의 필요, 불필요 PII 및 민감 로그 금지. 저장은 명시 동의 후, 내용 로그 없음. 고지는 이디스 담당.
- DOC-72a5d7ab4ebb#운전자전환: 계정/세션 관리 절. 여러 운전자 전환 지원 요구. 프로젝트는 Android 사용자 샌드박스만 사용하며 동일 사용자 운전자 분리는 출시 전 미해결.
- DOC-4036191ed19c#명시동의: [사용자 개인정보 활용](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security). 항목/목적/보유기간/거부권 고지와 동의 버튼의 명시 행동 요구. store 기본 false 및 save 재확인, UI 행동 검증은 통합 담당.
- DOC-4036191ed19c#철회삭제: 보안 표 사용자 데이터 무단 사용. 취소 시 사용 경로 차단 및 저장 데이터 삭제. 동의 false/빈 목록을 하나의 원자 교체로 확정, 실패 시 예외.
- DOC-27ac8605961f#저장보호: [민감 데이터 관리](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide). 앱 전용 저장 권장, 자동 백업 제외 권장, 민감 로그 금지. noBackupFilesDir 사용, 전송/권한 추가 없음.
- DOC-27ac8605961f#민감정보암호화: 같은 절은 내부 샌드박스와 함께 앱 수준 암호화를 의무화한다. 계획은 새 암호화 시스템 제외를 명시한다. 이번 개발 구현은 평문 저장으로 이 요구를 충족하지 않음(FAIL). 민감 내용을 포함할 수 있으므로 출시/민감 실데이터 사용 전 자비스가 암호화 범위를 확정해야 한다. 적합성 완료로 보고하지 않는다.
- DOC-4036191ed19c#출시고지: 개인정보처리방침 공개/약관 제출/국가별 위치정보 규제 증거는 이번 저장소 범위 밖 N/A(출시 전 필요).
- 프로젝트 선택: 7일은 법적/플랫폼 규정이 아닌 개발 브랜치 보유 정책. list/save에서 정리하며 앱 미실행 기간의 물리 삭제 스케줄은 제공하지 않는다.

## 결과
사용자 후속 지시로 프라이데이 소유 파일만 의미 단위 커밋 승인됨. push는 하지 않는다.

1차 구현: LocalDraftStore는 applicationContext.noBackupFilesDir를 사용하며 Context를 보관하지 않는다. DraftFileStore는 JVM에서도 동일 구현을 검사할 수 있도록 분리한 파일 저장 구현이며 별도 Repository/DI 계층은 없다.

자동 검사: :app:testDebugUnitTest PASS (저장 핵심 9개, 기존 다른 담당 테스트도 함께 통과), Studio MCP build_project PASS(isSuccess=true, problems=[]), harness 정적 PASS. 첫 실행에서 JUnit 테스트 하나의 반환형이 예외 객체로 추론되어 initializationError 발생; Unit 반환형으로 수정하고 재실행 PASS.

동작 증거는 app/src/test/java/com/jinkim/hyundaiAndGretel/roadnote/LocalDraftStoreTest.kt 및 app/build/test-results/testDebugUnitTest/TEST-com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStoreTest.xml. Android 프로세스 재시작/실기기 파일시스템/사용자 격리는 별도 검증 필요(NOT_TESTED).

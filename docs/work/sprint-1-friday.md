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


## 최종 검증 및 인계
- 구현 커밋: `9fc027c` (저장소 + 핵심 테스트). 후속 변경: 종료 직전 남은 `.tmp`를 다음 read에서 삭제하여 미확정 동의/내용을 채택하지 않음. 자동 검사 통과 후 이 보완 및 최종 기록은 별도 커밋.
- 최종 `:app:testDebugUnitTest`: PASS. 저장소 10개, Gleo 18개, UI presentation 3개 = 31개, failures=0. JVM/macOS에서 실제 임시 디렉터리와 ATOMIC_MOVE를 사용. 테스트에서 원자 교체 직전 IOException을 주입하여 기존 파일/동의 유지 및 임시 파일 정리를 확인했다.
- `python3 scripts/verify_harness.py --build`: PASS, assembleDebug/lintDebug 성공. `Studio MCP build_project`: PASS, isSuccess=true, problems=[]. `git diff --check`: PASS.
- `adb devices`: 연결 기기 0개. Android 실파일시스템 원자 교체/강제 종료 후 프로세스 재시작/Android 사용자별 격리는 NOT_TESTED. noBackupFilesDir 경로는 LocalDraftStore.kt:8 코드 검토만 완료. 기기에서 동의→저장→force-stop→재실행→조회→철회→재실행, 별도 Android 사용자 조회 없음 확인 필요.
- 자동 테스트에서 store 재생성 복구는 확인했지만 실제 앱 프로세스 재시작을 대체하지 않는다. ATOMIC_MOVE 미지원/파일 쓰기 실패는 IOException으로 호출자에게 전달하며 비원자 교체로 fallback하지 않는다.
- list/save 시 최초 수신 + 7일 이상인 항목을 제거한다. 앱이 실행되지 않는 동안 만료 시각에 맞춘 물리 삭제는 제공하지 않는다. 100개 한도에서 기존 초안을 자동 축출하지 않는다. DataOutputStream UTF 필드 한도 초과는 IOException이며 기존 파일을 보존한다. 현재 입력 파서 한도 내 데이터는 해당 한도보다 작다.
- 손상 파일은 list/save/setConsent(true)/clear가 예외를 전달하며 기존 데이터를 유지한다. 명시적 setConsent(false)는 내용 읽기 없이 빈 목록/false로 교체해 손상 데이터도 철회 가능하다.
- 자비스 인계: MainActivity/처리 엔진에서 `LocalDraftStore(context)`를 DraftStore로 연결하고 실패/미동의를 처리. 추가 의존성/Gradle/Manifest 변경 요청 없음. 이디스 UI의 명시 동의/고지 및 통합 동작은 이 담당 결과에 포함하지 않는다.
- 저장 구현은 완료. 전체 검증/출시 적합성은 미완료: 암호화 요구 FAIL, 동일 Android 사용자를 공유하는 운전자 분리 및 Android runtime 증거 NOT_TESTED. 보고서 검사는 이 제한을 의도대로 실패 상태(exit 1)로 유지한다.

## 동시 통합 변경 발견 및 커밋 경계
최종 커밋 준비 중 자비스가 조율 메모(자비스 기록의 후속 통합 절)에 따라 LocalDraftStore/DraftFileStore에 Keystore 암호화·누락 지명 상태 변경을 작성 중인 것을 확인했다. 이 변경은 프라이데이 커밋에 포함하지 않는다. DraftFileStore는 인덱스에 프라이데이의 임시 파일 정리 hunk만 남겼고 작업 파일의 자비스 변경은 보존했다. 프라이데이 결과/JSON은 `9fc027c`와 임시 파일 보완까지 검증한 시점 기준이며, 자비스 후속 암호화의 적합성/테스트 결과를 판정하지 않는다. 계획의 암호화 제외 문구가 조율 메모에서 Keystore 필수로 정정되었으며, 이 의무 해결과 최종 보고서 갱신은 이미 자비스가 진행 중인 통합 변경에서 확인해야 한다.

자비스 확인 사항: DraftFileStore의 ByteArrayInputStream import는 이번 프라이데이 최초 구현에서 사용하지 않아 제거했다. 후속 decode 경로에서 사용하면 import 추가 필요. LocalDraftStoreTest의 NAMED_PLACE fixture는 최초 계약에 따라 placeQuery가 없는 값으로 PENDING_LOCATION을 기대하므로 수정된 누락 지명 정책에 맞춰 fixture/검증 변경 필요. 두 사항 모두 프라이데이 변경에 섞거나 타인 작업을 되돌리지 않았다.

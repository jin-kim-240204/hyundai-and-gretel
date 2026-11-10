# 위치 쪽지 spec-driven 재구현 준비

요청: 참고 브랜치 보존, 최종 리팩터링 커밋과 조사·검증 지식의 첫 커밋 기록, 전체 작업계획만 작성 후 Draft PR. 구현은 사용자 리뷰 후 단계별 진행.

범위: docs/specs/road-notes 두 문서와 이 작업 기록/결과. 새 worktree는 origin/spec-road-note-system(c00b3c2) 기반. 기존 브랜치는 ae11722까지 리팩터링 커밋·푸시했고 미커밋 변경 없이 보존. 새 브랜치에는 참고 앱 코드를 가져오지 않는다.

규칙: ENV-02 — 기존 ComponentActivity 구조 유지(문서만 작성). VEH-03 / DOC-9844e9eaad3c#콜백 — 기존 공식 위치 데이터 접근 원문에 따라 nullable/error 및 동일 listener 해제 요구를 계획에 반영. 원문은 .agents/skills/pleos-connect/references/sources/9844e9eaad3c.md의 위치 업데이트 수신/중지 절 재사용. 신규 SDK/API 판단 없음. PRIV-01 — 위치 표본은 메모리·업로드 없음. 과거 PASS와 신규 미검증 구분. 사용자 합의로 구현 전 스펙 리뷰와 코드 리뷰를 별도 진행한다.

확인 방법: Markdown 로컬 링크, git diff --check, verify_harness와 report. 앱 코드/Gradle/Manifest/UI 변경 없으므로 빌드·기기·화면 검증 N/A. 전체 계획은 리뷰 대기이며 기능 완료 보고 아님.

결과 PASS: 로컬 Markdown 링크 확인, git diff --check, verify_harness --report(4 checks, exit0). N/A: 앱 빌드/기기(문서 전용). 사용자 전체 계획 리뷰는 대기 중.

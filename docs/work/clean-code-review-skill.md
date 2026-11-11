# 클린 코드 리뷰 스킬 추가

## 요청과 범위
- 사용자 제공 `docs/work/clean-code-review-rules.md`의 32개 규칙을 `.agents/skills/clean-code-review/SKILL.md`에 보존한다.
- AGENTS.md와 pleos-connect의 코드·PR 리뷰 경로에서 새 스킬을 함께 읽도록 연결한다.
- main 기반 별도 worktree에서 작업하며 기존 브랜치의 미커밋 파일은 포함하지 않는다.
- 리뷰 요청만으로 코드 수정 권한을 추정하지 않으며 프로젝트 기준과 공식 의무를 구분한다.

## 적용 규칙과 확인 방법
- 프로젝트 요구: 스킬 이름·설명 frontmatter, 원문 32개 규칙 보존, 두 진입점의 유효한 경로를 확인한다.
- ENV-02 및 DOC-eb1428718f0c#Activity구조: N/A. 문서·스킬만 변경하며 Android 구조 및 공식 API 계약은 변경하지 않는다. 공식 문서 재조회 불필요.
- 검증: quick_validate.py, 원문 규칙 비교, 진입점 경로 확인, verify_harness.py --report, git diff --check.

## 결과
- PASS: 스킬 형식 검사, 원문 32개 규칙 비교 및 진입점 링크 확인.
- PASS: 하네스 및 작업 JSON 보고서 검사, git diff --check.
- N/A: Android 빌드·UI·실차 검사. 실행 코드 변경 없음.
- NOT_TESTED: 새 세션의 실제 리뷰에서 스킬이 적용되는지 여부. 진입점 명시와 형식·경로 검사까지 확인했으며 모델의 준수를 기술적으로 보장하지 않는다.

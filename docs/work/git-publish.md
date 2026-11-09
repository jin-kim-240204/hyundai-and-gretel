# 의미별 커밋 및 원격 동기화

사용자 승인: 지금까지 변경을 한국어로 의미별 커밋한 뒤 pull/rebase/push. 충돌 시 해결안을 먼저 사용자에게 질문하고 답변 전 해결하지 않는다.
범위: 현재 앱 구현/계약·작업 기록/진단 및 검토 기록 3묶음. 코드 의존성 때문에 스키마·파서·콜백은 함께 커밋한다. 원격은 origin, 브랜치는 gm/feat-speech-to-text. 강제 push 없음.
검증 근거: gleo-single-call.md/json의 전체 단위 테스트/assembleDebug/lintDebug/계측 컴파일/IDE 빌드 PASS. 이번 분류에서 앱 코드 변경 없음. INT-01 원문 및 제한은 해당 기록 참조.
계획 문서 상단에 최신 단발 계약과 과거 설계의 적용 범위를 명시한다. 필수 E2E NOT_TESTED는 그대로 보존한다.

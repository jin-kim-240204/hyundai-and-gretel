# 서비스 이름 변경
요청: hyundai-and-gretel로 앱/프로젝트 명칭 변경 및 git push. 원격 저장소 이름은 사용자가 별도 변경.
범위: 앱 표시명, Gradle rootProject, Compose/XML theme 이름, 초기 화면의 기존 이름/기획 문구, README/AGENTS/스킬의 서비스명. Android namespace/applicationId는 설치 식별자로 유지. 공식 원문 스냅샷·과거 조사기록은 변경하지 않음. 기존 좌석 기능 미커밋 변경은 이번 커밋에서 제외한다.
규칙 ENV-02, DOC-eb1428718f0c#프로젝트이름 및 기존 Automotive/일반 Activity 구조 유지. 기능·화면설계 변경 없음. 변경명칭 참조 대조 및 harness/build/lint로 검증. 새 동작이 없어 별도 단위테스트 추가 N/A.

검증 PASS: 현재 작업트리 및 git index에서 추출한 이름변경 단독 스냅샷 모두 harness/assembleDebug/lintDebug 통과. git diff --cached --check 통과.
NOT_TESTED: Studio는 이동 전 /Users/jinnayeong/wake-up을 열고 있어 실제 경로를 대상으로 한 MCP IDE 빌드는 실행 불가. 앱 재설치·에뮬레이터 표시명 검증 미실행. 명칭 변경 구현과 CLI 검증 완료이며 IDE/실화면 검증은 별도.
로컬 프로젝트 폴더는 push 후 /Users/jinnayeong/hyundai-challenge/project/hyundai-and-gretel로 변경한다. 원격URL은 기존 origin을 유지한다.

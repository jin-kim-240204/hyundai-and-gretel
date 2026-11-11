# 공모전 설계 스킬 작성

2026-11-11. 요청: Kotlin/Compose 공모전 개발 규칙을 팀 공용 한국어 스킬로 작성. 소스 변경 금지.

## 범위와 판단

- 현재 `main`은 단일 app + ComponentActivity/Compose 시작 화면. 실제 파일·Gradle·Manifest·README·gitignore를 확인했다.
- 기존 pleos-connect가 참조하지만 없었던 `.agents/skills/android-compose-architecture/SKILL.md`를 설계 원본으로 추가한다. 공용 `.claude/skills`는 `.agents/skills` 심볼릭 링크이므로 별도 복사하지 않는다.
- MVVM/Repository/StateFlow/Hilt/Retrofit/OkHttp/Navigation/DataStore/Room의 현재 상태와 도입 기준을 구분한다. 코드 예시는 향후 패턴이며 구현 완료 주장이 아니다.
- 수정 대상은 스킬과 이 기록뿐이다. 라이브러리 설치·소스 수정·기존 설정 변경·커밋·업로드·계정 초대는 하지 않는다.

## 적용 규칙 및 원문 추적

- ENV-02: 사용자 선택인 Automotive 일반 Activity/Compose 구조 유지. Car App Library 전환 및 Domain/UseCase 추가 없음. 근거: AGENTS.md, docs/SETUP.md, Manifest.
- DOC-e68476b79d1c#화면과복원: 공식 앱 구조 문서의 화면 방향/크기 대응과 프로세스 재생성 시 상태 복원 전략을 요구한다. 스킬 5·7절에 화면 수명 상태와 영속 상태 구분, 관련 Pleos 디자인 검토 연결을 기록했다. 실제 화면 검증은 이번 범위 아님.
- DOC-e68476b79d1c#백그라운드: 앱이 보이지 않아도 프로세스가 남을 수 있다. 불필요한 화면 갱신·센서 수집은 중단하고 지속 작업은 시작/중단 정책을 정의한다. 스킬 5·7절의 Lifecycle 수집·producer 수명·awaitClose에 연결했다.
- 공식 URL: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model 의 화면 방향 및 크기 대응 / 포그라운드·백그라운드 전환 및 동작 설계. 2026-11-11 원문 재확인, 관련 스냅샷과 위 요구사항의 의미 차이 없음.
- 일반 Kotlin/Android/Hilt/Flow/GitHub 근거는 스킬 11절에 직접 링크. 팀 필수 규칙과 플랫폼 권장을 구분했다. 공모전 제출 조건은 사용자 제공 요구사항이며 독립적으로 공지를 확인했다고 주장하지 않는다.

## 검증

- PASS: `python3 scripts/verify_harness.py --report docs/work/architecture-skill.json` — 133개 원문·27개 규칙 하네스 및 문서 범위 3개 추적 항목 통과(exit 0).
- PASS: 별도 Python 표준 라이브러리 검사로 frontmatter 필드·이름, Markdown 코드 블록 닫힘, 로컬 링크 존재, 한국어 본문, 줄 끝 공백·최종 개행을 확인했다. Git tracked/untracked 목록을 대조해 이 스킬과 작업 기록 md/json 외 변경이 없음을 확인했다. `git diff --check`도 통과했다.
- NOT_TESTED: skill-creator의 `quick_validate.py`는 `ModuleNotFoundError: No module named 'yaml'`로 실행 불가. 환경 의존성을 추가하지 않았다. 위 별도 구조 검사는 수행했지만 정식 YAML 파서 기반 검증을 대체했다고 주장하지 않는다.
- 문서 검토 PASS: 요구한 10개 범주, MUST/SHOULD/MAY, 지양/권장 Kotlin 예시, 현재/예정 스택, 취소·리스너 해제, 공모전 제출 조건과 현재 설정 차이를 확인했다. 스킬 작성 완료와 미실행 검사를 구분한다.
- 빌드·런타임 N/A: 소스·Gradle·Manifest·리소스를 변경하지 않음. 향후 예시 코드는 import/보조 선언을 생략한 설명용이며 컴파일 검증 대상이 아님.
- 제출 검증 NOT_TESTED: 이번 작업은 규칙 문서 작성. Release 서명·설치/실행·실제 음성/차량 기능·심사 계정 접근·최종 APK 링크는 검증하지 않았다. 제출 준비 완료를 뜻하지 않는다.

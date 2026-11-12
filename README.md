# hyundai-and-gretel

AI 작업은 [AGENTS.md](AGENTS.md)에서 시작한다. Codex/Claude 공유 스킬은 [.agents/skills](.agents/skills)에 있으며, [문서 지도](.agents/skills/pleos-connect/references/source-map.md)에서 관련 공식 규정을 선택한다. 구현 후 `python3 scripts/verify_harness.py`와 작업별 검증 기록을 갱신한다.

Pleos Connect 차량용 도로 제보 앱. 목격 직후 Gleo로 제보하고 주변 도로 차량에 공유하는 [서비스 설계](docs/plans/implementation-plan.md)를 검토 중이다.

현재 Sprint 1의 입력 검증·미게시 초안 암호화 저장·동의/목록/삭제 UI를 연결했다. debug에서는 Gleo 외부 테스트 입력을 받아 저장 없이 검증/Callback과 실시간 JSON 화면·GleoProbe 로그를 제공한다. release 외부 수신은 비활성이고, 위치 지정·게시·차량 간 공유는 후속 스프린트다. [담당별 실행 명세](docs/plans/sprint-1-gleo-drafts.md)와 [자비스 검증 기록](docs/work/sprint-1-jarvis.md)을 참고한다.

현재 코드의 책임과 실행 흐름은 [Gleo 구조 정리 기록](docs/work/gleo-architecture-refactor.md)을 참고한다. `MainActivity`는 화면 선택, `DraftRoute`는 상태 수집, `DraftsViewModel`은 화면 상태, `DraftRepository`는 초안 데이터 접근을 담당한다. Hilt가 저장소·처리기·ViewModel 의존성을 연결한다. Gleo의 입력 파싱은 `GleoActionParser`, 응답 직렬화는 `GleoCallback`, Android 수신·전송은 `GleoActionReceiver`, 메모리 기록과 테스트 화면은 `GleoProbe` / `GleoProbeScreen`으로 나뉜다.

자세한 구성과 템플릿 적용 방식은 [docs/SETUP.md](docs/SETUP.md)를 참고한다.

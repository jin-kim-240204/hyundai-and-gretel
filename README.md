# hyundai-and-gretel

AI 작업은 [AGENTS.md](AGENTS.md)에서 시작한다. Codex/Claude 공유 스킬은 [.agents/skills](.agents/skills)에 있으며, [문서 지도](.agents/skills/pleos-connect/references/source-map.md)에서 관련 공식 규정을 선택한다. 구현 후 `python3 scripts/verify_harness.py`와 작업별 검증 기록을 갱신한다.

Pleos Connect 차량용 도로 제보 앱. 목격 직후 Gleo로 제보하고 주변 도로 차량에 공유하는 [서비스 설계](docs/plans/implementation-plan.md)를 검토 중이다.

현재 Sprint 1의 입력 검증·미게시 초안 암호화 저장·동의/목록/삭제 UI를 연결했다. debug에서는 Gleo 외부 테스트 입력을 받아 저장 없이 검증/Callback과 실시간 JSON 화면·GleoProbe 로그를 제공한다. release 외부 수신은 비활성이고, 위치 지정·게시·차량 간 공유는 후속 스프린트다. [담당별 실행 명세](docs/plans/sprint-1-gleo-drafts.md)와 [자비스 검증 기록](docs/work/sprint-1-jarvis.md)을 참고한다.

자세한 구성과 템플릿 적용 방식은 [docs/SETUP.md](docs/SETUP.md)를 참고한다.

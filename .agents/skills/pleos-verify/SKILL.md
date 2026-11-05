---
name: pleos-verify
description: hyundai-and-gretel 변경 완료 전 Pleos 규정 추적, 자동 검사, 실기기 증거와 실패 수정 루프를 수행한다. 구현 후 검증과 규정 리뷰에 사용한다.
---
# 완료 게이트

루트 AGENTS.md, 현재 작업 기록, `../pleos-connect/references/rules.json`과 관련 원문을 읽는다.

1. 해당 작업의 선택 규칙과 원문 세부 요구사항을 작성한다. 제외는 사유와 함께 N/A로 기록한다. SKIP/PASS로 모호하게 덮지 않는다.
2. `python3 scripts/verify_harness.py`를 실행한다. Android/Gradle/Manifest/리소스 변경이면 `--build`도 실행한다. 코드 편집 후 Studio MCP build_project로 IDE 빌드를 확인한다.
3. 타이머 상태 전이/취소/중복/오류처리 등 새로운 동작은 의미 있는 단위 테스트를 추가하고 실행한다. 텍스트 수정에 불필요한 테스트를 만들지 않는다.
4. UI 변경은 pleos-design의 실제 화면 검토를, 차량 제어는 pleos-vehicle의 지원/권한/오류/규제 검토를 수행한다.
5. 오디오를 사용하면 f04338dec3b6, a243773e232d, 2cbe51f12c43을 읽고 포커스 획득·상실·복구와 suspend, TTS 선행 묵음을 확인한다. 휴식음과 기상음에 동일한 정책이라고 가정하지 않는다.
6. 개인정보/외부 API는 72a5d7ab4ebb, 4036191ed19c, 27ac8605961f을 읽고 수집·저장·전송·동의·최소권한·비밀정보를 확인한다.
7. 성능 변경은 0ee9d3c6e22c 기준을 앱 유형·상태에 맞게 측정한다. CPU/RAM 수치와 측정환경을 남긴다.
8. 제출 작업이면 036572f580ac, c455775b964b와 하위 심사 5종, 태그/버전/보안/업데이트 페이지를 모두 확인한다. 현재 개발 빌드로 출시 적합 판정을 내리지 않는다.
9. FAIL을 수정하고 관련 검사만 다시 실행한다. 환경/권한 부족은 NOT_TESTED와 필요한 조치를 남긴다. 이전 결과가 수정 후에도 유효한지 판정한다.
10. 작업 JSON report를 만들고 `python3 scripts/verify_harness.py --report <파일>`을 실행한다. 자동 검사 자체가 수동 증거의 진실성을 보장하지 않는다. 완료 범위와 미검증 범위를 사용자에게 알린다.

보고 형식은 `docs/work/REPORT-FORMAT.md`. 다른 세션에 넘길 때 요청·파일·규칙·검증·남은 항목을 이 기록에 남긴다. 별도 에이전트나 커밋/푸시는 자동으로 실행하지 않는다.

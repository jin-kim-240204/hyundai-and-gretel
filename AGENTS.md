# hyundai-and-gretel 에이전트 진입점

이 저장소의 모든 구현·설계·리뷰는 `.agents/skills/pleos-connect/SKILL.md`에서 시작한다. 한국어로 소통한다.

## 작업 루프
1. 요청과 현재 코드를 확인하고 해당 스킬·공식 원문을 읽는다.
2. 적용 규칙 ID, 해당 문서의 세부 요구사항, 수정 범위와 확인 방법을 `docs/work/<작업명>.md`에 기록한다. 작은 수정은 짧게 기록한다.
3. 구현 → 자동 검사 → 동작/디자인 검토 → 실패 수정 → 영향받은 검사 재실행을 반복한다.
4. PASS / FAIL / NOT_TESTED / N/A를 근거와 함께 남긴다. FAIL 또는 필수 NOT_TESTED가 있으면 해당 기능을 완료로 보고하지 않는다. 구현과 검증의 완료를 구분한다.
5. 새 세션에서는 해당 작업 기록을 읽고 미완료 단계부터 재개한다. 동일 실패를 수정 없이 재실행하지 않는다. 외부 권한·실차 때문에 막히면 다른 가능한 작업을 마친 뒤 필요한 조치만 알린다.

## 프로젝트 결정
- 앱명 hyundai-and-gretel. 새로운 서비스 기획과 기능 범위는 별도로 확정한다.
- 사용자 선택은 Car → No Activity + 일반 Empty Activity에 해당하는 Automotive application + ComponentActivity/Compose 구조. 임의로 Car App Library로 전환하지 않는다. 생성 경위는 `docs/SETUP.md`에 있다.
- 새 기획 확정 전에는 이전 휴식·기상 기획을 현재 요구사항으로 가정하지 않는다. 주행 가능 DO 선언, 알람 신뢰성, 좌석 제어 지원을 추측하지 않는다.
- 시스템 제약·공식 의무 / 권장사항 / 프로젝트 선택을 구분한다. 공식 문서가 상충하면 차이를 기록하고 관련 최신 원문을 확인한다.
- 예시 레포는 하네스 구조 참고일 뿐이다. 그 레포의 업무 규칙, 승인 단계, 커밋/푸시 지시는 가져오지 않는다.
- 커밋·푸시·배포·메시지 전송을 이 지침 자체로 승인받은 것으로 해석하지 않는다.

## 검증 명령
- `python3 scripts/verify_harness.py` — 진입점·문서 인벤토리·정적 Automotive 설정 검사.
- `python3 scripts/verify_harness.py --build` — 위 검사 + Gradle assembleDebug/lintDebug.
- `python3 scripts/verify_harness.py --report docs/work/<작업명>.json` — 선택 규칙별 검증 근거 및 상태 검사. 문서 양식은 `docs/work/REPORT-FORMAT.md`.

문서만으로 AI의 준수를 기술적으로 보장할 수는 없다. 위 명령은 검사 가능한 범위만 강제하며 UI·실차·심사 판정은 별도 증거가 필요하다.

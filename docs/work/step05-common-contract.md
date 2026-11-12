# 05 접근·수신 공통 정본 동기화

사용자 전달의05 독립 리뷰 B1/P1/P2/P3 리뷰이 수용을 반영. pleos-connect ENV-02/VEH-03/PRIV-01 및 DOC-1ba06876919b#초기화해제 적용. 정본§8.1/표시 상태·흐름/05 최신절만 문서 수정. 명시 확인 ACK·확인시각 의미, proximity 등록 전후 오류의 세션 Failed/전체 요청 정리, 합성 전용 자격·accuracy 날조 금지·2초 신선도, 수신 StorageFailure 자동 재시도 금지·stop/newstart 재허용을 동기화한다. 기존03 저장 수명·TTL/04소비계약·원본 보존 유지.

근거: 기능 브랜치 b428a33의05와 docs/work/pr4-05-spec-review.md/response.md. SDK1.1.3 소스 근거는 기존05 기록 재사용하며 새 SDK 계약 없음. 코드/의존성 변경 없음. 문서 정적검사 후 별도 docs 커밋·부모 push·기능 브랜치 병합. 앱/Studio/adb/기기/계측 실행 금지, 런타임 NOT_TESTED. 독립 재검토 전 구현/resolve 없음.

PASS: 부모 worktree 하네스·diff --check, 본문/분리 Mermaid의 명시 확인 ACK 문구 동기화. JVM/빌드는 문서 작업으로 N/A. SDK/Room/Compose/서비스 런타임은 NOT_TESTED.

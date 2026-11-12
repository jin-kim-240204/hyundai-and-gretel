# 06 리뷰 수용 공통 정본 동기화

대상06 c0c4f62, 리뷰이 판단 pr4-06-spec-response.md 및 원격답글4545281093/1247/1461. 사용자 지시에 따라 기존 spec-road-note-system worktree에서 공통 정본/분리 Mermaid만 수정한다. 코드/의존성/앱/기기/adb/계측 실행 없음. 기존 미추적 와이어프레임/다른 역할 기록 보존.

적용 ENV-02/VEH-03/PRIV-01, DOC-9faba42791f0#기능별규제: 실제 CarUx 신호·기능별 제한/DO 비면제. NO_SETUP의 시연설정 분류와 미연결 차단은 프로젝트 선택, stop/닫기 유지. 현재 DO 없음·이번 새 선언 없음. 공식 Pleos 규제/CarUx 원문을2026-11-12 재확인, 실제 CarUx/주행 실행 NOT_TESTED.

부모 정본의 옛 A0/B300/15초 예시와 SDK 미통과 REPLAY 대체·DemoClock 제품 대체·기존 DO 유지·광범위 삭제/자동 청소 설명을 갱신했다. A610±5→anchor605..615→B5..15, 실제 anchor-600 요청/범위밖 보정금지. 두모드 실제SDK·25m/5m/15초 gate/D한정. v4nullable소속/활성상태행·현재시연만atomic reset. 제품 recipient 전체 JOIN 큐/명시ACK debug/release 보존, 시나리오필터는 신규후보/수신/대상/도식/시연카운트만. 실제요청목표간격과SDK확인표본오차는구분한다.

PASS: 하네스·diff --check·본문/분리 Mermaid 동기화. JVM/빌드는 문서로 N/A, 실제 SDK/Room/CarUx/UI/FGS/입력·시연은 NOT_TESTED. 부모 별도 docs커밋/push 후 기능 브랜치 병합·06/00/요구사항매핑/기록 반영·독립 재검토 전 구현 금지.

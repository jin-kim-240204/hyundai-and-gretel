# 전체 설계 브랜치 분리

사용자 요청: 전체 구현 계획을 문서 전용 PR로 분리하고 기존 기능 브랜치가 설계를 공통 조상으로 포함하도록 정리. 다음 FusedLocation 버퍼/심기/접근 수신 구현용 브랜치 생성. 충돌은 해결 전에 사용자 확인.

범위: main에서 gm/spec-road-note-system 생성. implementation-plan 및 diagrams 6개와 prototype-e2e-spec 검토 기록만 복사. 기능 상세 Gleo/Sprint 문서는 기존 브랜치에 남긴다. 설계 PR 생성, 기존 PR base 변경, gm/feat-fused-location-notes 분기. 기능 구현·PR merge는 요청 범위 아님.

ENV-02 / INT-01: 앱 코드·Manifest·SDK·스키마를 바꾸지 않고 설계 계약만 분리한다. 공식 요구사항과 미검증은 prototype-e2e-spec.md/json 그대로 보존. 문서 PR이 설계 검토 완료와 기능 검증 완료를 혼동하지 않도록 draft로 연다.
검증: origin/main 대비 문서만 차이, 상대 링크 존재 및 embedded JSON 파싱, harness·diff --check. 앱 코드 변경이 없어 빌드/차량 검증 N/A. 기능 runtime NOT_TESTED는 설계 기록 그대로 유지.

브랜치 정책: 설계 PR을 먼저 main으로 merge한 뒤 기능 PR base를 main으로 변경한다. 기능 코드가 설계 브랜치에 merge돼 문서 PR이 오염되지 않도록 문서에 명시. 새 위치 브랜치는 설계만 상속하며 기존 Gleo 구현의 연결 의존성은 후속 구현에서 통합한다.

정적 검증 PASS: main 대비 문서만 추가, JSON·callback data·상대 링크·SVG XML, harness, diff --check. PR 웹 조작은 계정 권한/브라우저 사용 경합으로 진행 대기 중. Git 원격 fetch는 성공.

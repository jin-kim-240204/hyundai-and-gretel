# 브랜치 전환 전 남은 작업 보존

2026-11-13 사용자 요청: 현재 ny/spec-driven-road-notes의 모든 미커밋 작업을 해당 원격 브랜치에 보존해 브랜치 전환이 가능하게 한다. 기존에 제외한 임시 polling/별도 작업 문서까지 이번 요청으로 커밋 범위에 포함한다. 구현 전용 ny/feat-fused-location-message/PR6은 변경하지 않는다.

- 후보6km/수신500m/H3 res7과 SDK 요청 반경 전달을 feat 커밋으로 보존한다. 관련 근거는 spatial-policy-6km-500m.md에 있다.
- debug getLastKnownLocation 1초 캐시 조회 로그는 별도 chore 커밋으로 보존한다. SDK 콜백/버퍼 입력과 섞지 않고 stop에서 취소한다. 임시 조사 코드라는 성격을 유지한다.
- 남은 리뷰·응답·와이어프레임·조사·검증 기록은 docs 커밋으로 보존한다. 문서의 과거 NOT_TESTED/미결 상태를 현재 PASS로 바꾸지 않는다.
- PASS: 현재 작업 트리 JVM102, assembleDebug, lintDebug, diff 공백, harness 정적. 로그 /private/tmp/pr4-save-before-switch.log. staging할 모든 새 문서의 내용/헤더와 민감정보 후보를 확인했다.
- NOT_TESTED: 이번 보존 작업에서 앱/Studio/기기/실제500m ENTER/주행은 실행하지 않았다. 별도 PR6도 이 범위는 초안 유지다.

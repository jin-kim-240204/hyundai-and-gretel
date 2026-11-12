# 주행 UI 변경 원복

2026-11-08. 사용자 명시 요청: b61ffd7의 간결 UI/CarUxRestrictions 화면 전환/연결 및 listener/관련 테스트를 모두 원복하고 MainActivity distractionOptimized=true만 유지한다. pleos-connect/ponytail/pleos-verify 적용.

범위: MainActivity/GleoProbe/GleoActionReceiver를 b61ffd7 직전 상태로 복원, driving 신규 파일3개/테스트1개 제거, Gradle android.car 참조 제거, 현황 문서 수정. Manifest의 DO 선언과 기존 사용자 권한4줄은 유지한다. 미커밋 빈 선택 필드 파서/테스트/설계 변경과 과거 작업 기록은 수정하거나 커밋하지 않는다.

ENV-02 / SAF-01 적용. DOC-6386c5462132#DO: 기존에 읽은 공식 지역 정책의 DO 선언에 따른 플랫폼 차단 처리와 사용자 요청을 구분한다. DOC-9faba42791f0#UX제한: 공식 가이드는 DO 앱도 CarUxRestrictions로 기능 제한을 적용하도록 요구하지만 사용자가 이번 테스트 UI의 상태 감시/축소를 명시적으로 원복하도록 지시했다. 따라서 이 변경을 주행 규정/출시 적합성 충족으로 기록하지 않는다. 현재 우선 목적은 에뮬레이터 Gleo/JSON 관측이다.

검증: 삭제 기능 참조 잔여 없음, Manifest DO 유지, 복원 파일과 기준 커밋 일치, 단위/빌드/lint·harness·Studio. 차량 상태/속도/기어는 변경하지 않는다. 실차/출시 적합성은 FAIL 또는 NOT_TESTED로 구분한다.

결과: MainActivity/GleoProbe/Receiver가 b61ffd7 직전 코드와 바이트 단위로 동일함을 확인했다. 차량 API 참조/간결 화면3파일/관련 테스트 제거, Manifest DO=true 유지 확인 PASS. 단위39개(기존 미커밋 파서 회귀검사 포함)/실패0, assembleDebug/lintDebug·harness·Studio build_project·diff 공백 검사 PASS. 사용자 기존 미커밋 파서/테스트는 이 원복 커밋에 포함하지 않는다. 업데이트 APK를 에뮬레이터에 설치하고 기존 화면을 실행했다. 실제 음성/주행 상태 전이/출시 적합성은 이번 원복에서 검증하지 않았다.

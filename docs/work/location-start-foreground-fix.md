# 위치 수집 시작 요청의 포그라운드 전환 누락 수정

## 요청과 범위
- 사용자가 수집 불가 문제 해결 및 앱 디버깅을 요청했다. 현재 PR4 작업 트리의 기존 리팩터링/polling/별도 문서는 보존한다. 커밋·푸시는 요청되지 않았다.
- 관측: ActivityManager에 `startForegroundService() did not then call Service.startForeground()` 및 서비스 종료 기록. 운전자 user10, 에뮬레이터는 emulator-5554 한 대다.
- 설치 앱 DB는 읽기 복사로 확인했다. demo_state 행 없음, road_notes 0개. 데이터 초기화/삭제는 하지 않는다.
- 시작 호출부, Service.requestStart, DemoRuntime.requestStart/allowed 및 finishWhenStorageIsIdle, 기존 수명/계측 테스트를 확인한다.

## 적용 기준
- ENV-02 / VEH-03 / clean-code24: 시작 계약과 정리 순서, SDK 세션·수락 쓰기 수명 유지.
- Android 공식 launch: https://developer.android.com/develop/background-work/services/fgs/launch 의 Launch/Promote 절. startForegroundService 요청 후 서비스가 즉시 startForeground로 승격해야 한다. Android14 location 유형 권한 요구는 그대로 유지한다.
- Android 공식 troubleshooting: https://developer.android.com/develop/background-work/services/fgs/troubleshooting 의 ForegroundServiceDidNotStartInTimeException 절. 조건에 따른 시연 거절이 승격 의무를 생략하게 해서는 안 된다.
- 수정은 debug 시작 진입점의 조건부 승격을 무조건 선행 승격으로 바꾸는 최소 범위. 시연/규제/SDK·쓰기 수락 판단 자체는 변경하지 않는다. 거절 후 기존 종료 판단으로 알림/started 수명을 정리한다.
- 확인: 회귀 반례(초기 시연 없음·연결 유지), JVM/양 variant/계측 compile/lint/정적 검사, 허용된 현재 앱 재현. 자동 ACK/Gleo/DB schema/권한/공용 UI 브랜치는 변경하지 않는다.

## 수정 및 결과
- 수정 전 재현: 2026-11-12 21:01:39 시작 클릭 → 21:02:20 ANR와 PID4527 종료. ActivityManager 원인 문구가 동일했다. `/private/tmp`에서 `road-note-start-before.db` 읽기 복사로 초기 시연 없음/쪽지0개 확인. DB는 삭제하지 않았다.
- RoadNoteService.requestStart에서 조건부 승격을 없애고, 시연 조건 판정보다 startForeground를 먼저 호출한다. DemoRuntime의 규제/저장중/전환중/시연 미초기화 판단과 기존 finishWhenStorageIsIdle 종료는 그대로다. release 경로는 변경하지 않았다.
- RejectedTrackingStartTest 추가: 기존 시연이 없는 설치에서만 실행(데이터가 있으면 assume으로 건너뜀). Activity에 바인딩된 서비스에 FGS 시작을 보내고 45초 뒤 연결 유지/STOPPED/시연 미초기화 안내/DB 미변경을 확인한다. 직접 46.536초 실행, `OK (1 test)`. 기기 ANR 재현과 이 통과를 red→green 근거로 남긴다. 로그 `/private/tmp/road-note-start-green-runtime.log`.
- PASS: JVM102(실패0), debug/release 빌드, 계측APK compile, lintDebug, harness, Manifest/schema/release APK static, Python4, git diff --check. 빌드 로그 `/private/tmp/road-note-start-green-build.log`.
- 환경: user10의 mock_location이 deny여서 개발 기기 AppOps만 allow로 설정했다. 앱 코드가 권한을 자체 상승하지 않는다. reverse8088 시도는 이미 포트 사용 중으로 실패했고 변경되지 않았다. 기기 로컬8088의 GET /vehicle/state가 HTTP200임을 확인했다. 앞선 '웹콘솔 연결 설정도 빠짐' 추정은 취소한다. 기기 웹콘솔은 이미 정상 연결돼 있었다.
- 앱 준비: 활성 시연이 없고 원본0임을 확인한 뒤 UI의 새 시연 초기화/확인을 눌러 최초 시연을 만들었다. 기존 원본이나 수신 데이터를 삭제하지 않았다. 제보자 A 시작 위치 설정을 실행했다.
- PASS(현재 에뮬레이터 범위): 패널 `A / WEB_CONSOLE / READY`, `실제 SDK 출발점 확인`, `SDK RECEIVING`, 목표610m/SDK610m. 실제 SDK→tracking 로그에서 session3 RECEIVING과 BUFFER_APPEND가 연속 증가했다. SDK 권한 허용 및 쓰기 busy false 확인. 패널을 닫고 수집을 유지한다.
- 기존 사용자 polling은 작업 전 원문과 바이트 동일. 기존 리팩터링·다른 문서는 보존. 커밋/푸시는 미수행.
- NOT_TESTED: 속도/기어를 바꾼 실제 주행, A/B 제보·수신 전체 재시연, 실차/다른 Android 버전. 위치 수신 복구 및 거절 경로만 확인한 결과다.

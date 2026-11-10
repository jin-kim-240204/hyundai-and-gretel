# 현재 변경 Ponytail 리뷰

요청: ponytail 스킬로 현재 코드 리뷰. 스킬의 기본 범위에 따라 미커밋 변경(ProximityTest 삭제, LOCATION-DEMO 안내 정리)과 연결된 PleosLocationSource, DemoFlowTest, GPS 검증 스크립트 및 관련 테스트를 읽었다. 코드는 수정하지 않았다. 부하는 단일 에뮬레이터의 합성 시연을 기준으로 한다.

규칙: ENV-02 N/A — Activity/Manifest/의존성 변경 없음. SDK·디자인·권한 공식 요구사항 N/A — 운영 구현 변경 및 공식 적합성 판단 없음. 삭제된 테스트의 SDK 요구사항 자체를 재심사하지 않는다.

검토 결과: 문서에서 일반 connectedAndroidTest 실행 주의사항까지 삭제했으나 DemoFlowTest는 여전히 외부 GPS 입력을 기다린다. docs/LOCATION-DEMO.md:35 부근에 전용 verify_location_demo.py 사용 안내와 전체 계측 실행의 외부 입력 의존성을 유지해야 한다. DemoFlowTest:25는 GPS 없이 최대 55초 후 실패하며, scripts/verify_location_demo.py:65 이후가 READY 로그에 맞춰 재생을 제공한다.

PASS — git diff --check, python3 scripts/verify_harness.py. PASS — 삭제된 테스트 참조와 운영 코드 영향 정적 검토. NOT_TESTED — 빌드 및 에뮬레이터 계측 재실행. 기존 작업 기록의 빌드 PASS는 이번 실행 결과로 재사용하지 않았다. EXIT 전용 SDK 이벤트 검증은 삭제되며 남은 DemoFlowTest는 수신 흐름을 검증한다.

조치: 사용자 요청에 따라 LOCATION-DEMO.md에 DemoFlowTest의 외부 GPS 의존성과 전용 스크립트 사용 안내를 복원했다. 저장소 전체 리뷰/실차 검증/출시 적합성 판정은 이 기록의 범위 밖이다.

조치 후 PASS — :app:assembleDebugAndroidTest, git diff --check, python3 scripts/verify_harness.py --report docs/work/ponytail-current-review.json. 보고서 첫 검사는 개별 DOC 항목 누락으로 FAIL이었으며 적용 제외 사유를 명시한 항목 추가 후 PASS. 에뮬레이터 런타임은 미실행.

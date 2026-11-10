# 수동 proximity 실험 테스트 제거

요청: 코드리뷰에서 확인한 ProximityTest.kt 삭제. 수동 GPS 주입이 있어야 완료되는 SDK 실험용 계측이며 일반 자동 테스트로 적합하지 않아 제거한다.

범위: 해당 테스트 파일, docs/LOCATION-DEMO.md의 삭제된 테스트 실행 안내 및 전용 로그 태그. 실제 PleosLocationSource의 proximity 등록/이벤트 처리와 나머지 계측은 유지. 과거 실험 기록의 PASS는 당시 이력으로 보존한다.

규칙 ENV-02 및 DOC-9844e9eaad3c#콜백: 앱 구조·SDK 콜백 구현은 변경하지 않는다. pleos-connect/ponytail 기준으로 삭제와 참조 정리만 수행하며 새 테스트/의존성은 추가하지 않는다.

검증: 삭제 파일 및 운영 안내의 참조 부재 확인, diff 검사와 harness 정적 검사. 계측 소스 빌드로 나머지 테스트 컴파일 확인. 실제 SDK 재실행은 앱 동작 변경이 없어 N/A.

결과: PASS — harness 정적 검사, git diff --check, :app:assembleDebugAndroidTest. N/A — 앱 실행/SDK 이벤트 재검증(운영 코드 변경 없음). 커밋·푸시는 수행하지 않았다.

# 위치 브랜치 전체 Ponytail 리뷰와 리팩터링

요청: 기존 미커밋 변경 수정·커밋·푸시 후 브랜치 전체 리뷰 및 자가 리팩터링. 선행 변경은 39c6647로 origin/ny/feat-fused-location-notes에 푸시했다.

범위: 원격 기본 브랜치 main(c5fd034, ls-remote 확인)과 HEAD의 최종 diff에 있는 Kotlin/Python/Gradle/Manifest/스키마/시연 문서 및 호출·테스트. 운영·debug/release·계측·단위 테스트·두 스크립트를 모두 읽었다. 단일 Pleos 에뮬레이터의 합성 서울 직선 경로를 예상 부하로 사용한다. 문서의 기존 30초 준비 제한은 후속 remove-buffer-warmup 결정으로 폐기된 이력이다.

적용: ENV-02(일반 ComponentActivity 유지), VEH-03(콜백/수명 유지). SDK를 새로 도입하거나 차량 제어·UI 구조를 변경하지 않는다. 기존 개인정보 미검증/출시 적합성은 이 리팩터링으로 완료 처리하지 않는다.

공식 원문 2026-11-10 재확인(스냅샷과 관련 조항 차이 없음):
- DOC-9844e9eaad3c#콜백: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access 의 위치 업데이트 수신/중지. nullable 위치와 오류를 처리하고 동일 listener를 해제한다. PleosLocationSource의 기존 경계를 유지, 세션 계측으로 확인.
- DOC-1ba06876919b#수명: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release 의 SDK 생명주기. 지원 확인→initialize→listener 해제→release 순서와 재시작 시 새 인스턴스 사용. 기존 소스 구현 유지, debug/release 빌드 및 계측 확인.
- DOC-ba9b83fc86f6#오류: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/error-handling 의 오류 처리. null/error에서 위치 기능 제한을 유지. SessionGuardTest로 현재위치 없음 저장 차단 확인. 실제 privacy 차단 주입은 별도 미검증.

확인 사항과 최소 조치:
1. 후진 후보를 progress 차이 오름차순으로 정렬하여 먼 쪽지가 먼저 16개를 차지한다. 전방거리 계산을 도메인 한 곳에서 공유하고 양방향 가까운 순 정렬을 테스트한다.
2. 수신함 JOIN 결과에서 note를 버리고 각 행마다 재조회한다. JOIN에서 note+recipient+수신시각을 함께 반환하여 단일 스냅샷으로 읽는다. 기존 스키마·안정정렬·검증을 보존하고 다중 수신/삭제 회귀 계측을 추가한다.
3. RoadNoteRepository는 사용처가 없지만 기존 설계 §5.7에서 명시적으로 정한 서버 교체 계약이다. 이번에는 제거하지 않는다. 호출부가 쓰는 로컬 관리 기능과 분리한 현재 계약을 유지한다.

검증 계획: 정렬 회귀 단위 테스트, Room 계측, 기존 세션/SDK 통합 계측, debug/release assemble·lint·단위·계측 컴파일, harness/report, Studio 빌드. 결과와 미확인은 완료 시 갱신한다. 운영 동작을 바꾸는 추상화/새 의존성/대규모 파일 분리는 추가하지 않는다.

결과: [전체 리뷰](ponytail-branch-review.md)에 문제/최소 수정/영향/검증을 기록했다. 정렬 회귀 FAIL을 수정해 단위11개 PASS. debug/release·계측 컴파일·lint·하네스·Studio PASS. Pleos Room/세션 3개와 실제 SDK E2E 1개 PASS. 스키마 변경 없음. SDK privacy 차단/null/error 주입·실차·폼팩터는 NOT_TESTED. 부하 문의 후 emulator 종료, 추가 빌드/기기 실행 중단. 후속 리팩터링은 미커밋으로 남긴다.

후속 요청: 사용자가 리팩터링 커밋·푸시를 승인했다. 위 검증 이후 운영 코드 변경 없이 정적 검사와 diff를 재확인하고 현재 변경을 보존한다. 이 리팩터링이 새 spec-driven 작업의 최종 참고 구현이며 신규 기능의 검증을 대신하지 않는다.

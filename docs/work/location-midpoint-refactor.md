# 위치 구현 중간 정리와 PR #4 리뷰 반영

요청: 최신 리뷰 확인·반영 및 폴더/가독성 중간 리팩터링. 기준ff7eee3. 기존 로컬 PleosLocationSource 임시 polling과 pr4-implementation-feedback.md는 그대로 보존. 커밋/푸시는 이번 요청에 포함하지 않음.

스킬: pleos-connect, android-compose-architecture, ponytail(full), ponytail-review(검토 기록용, 코드 수정은 사용자 명시 요청). 범위는 위치기능 코드/호출/단위·계측·현재 UI·권한·SDK수명·문서까지 읽고 단일 에뮬레이터/45초 위치이력 부하로 판단. UI·서비스·SDK·버퍼 책임 분리는 적절하며 새Repository/UseCase/Hilt/전략/이벤트계층·패키지 일괄이동은 하지 않음. Repository는 실제 저장소 기능 도입 단계에서 다루며 현 단계의 합의된 좁은 서비스 연결을 위임계층으로 감싸지 않음.

적용 ENV-02/VEH-03/PRIV-01, DOC-1ba06876919b#수명(지원확인/initialize/register/동일listener해제/release), DOC-9844e9eaad3c#콜백(nullable/수신순서), DOC-ba9b83fc86f6#오류(취소와SDK실패·정리 유지). 기존 공식 스냅샷 및01/02합의 재사용. 새SDK/권한/알림유형 추가 없음.

## 검토와 최소 변경

1. 필수: B13 기기 검증이 최종 창만 확인하여 CREATED20초 수집0건도 통과 가능. stopped/recreated 각 구간 직후 로그 기준점을 찍고 복귀 전에 최신 표본이 증가했는지 별도 확인. 실제 수집 중단 결함을 재현한 것이 아니라 검증 누락 수정이다. 리뷰4527587067.
2. 권장: 버퍼 선택에서 충분한 이력 조건을 두 번 계산하고 목표시각 산식이 중첩. historyCoversTarget/targetElapsedMillis/targetErrorMillis 지역값과 정책분기when으로 정리. 경계/동률/짧은이력 결과 보존. 리뷰4527428211/4527587337.
3. 가독성: LocationFix/LocationSource 공유 계약이 세션 구현 파일에 있어 버퍼/SDK에서 구현 파일을 찾아야 함. 같은location패키지의 LocationSource.kt로 계약만 분리, 패키지/API 변경 없음. 빈폴더/모델복제 없음.
4. 현행 스킬 권장: 사용자 표시문구는 리소스로 이동, 화면/알림/권한 문구 동일 유지. 화면 배치/동작/Theme 변경 없음.

상태 검토: 권한 enum과 ViewModel 파생Flow는 직전ba7e02a 유지. source/initialized/listener/generation/lastEventAt은 각각 해제 대상·초기화실패시release의무·해제listener·세션차단·무효화/순서 기준으로 서로 대체 불가. 버퍼크기/offset/준비Boolean을 새로 저장하지 않는다. 자원 정리 순서는 바꾸지 않는다.

검증: 기존 버퍼10+추적7 단위, debug/release/계측/lint/harness/IDE, 기본기기3개와 보강된50초profile. 문구리소스 변경은 기본화면 확인. 실제 SDK 오류주입/모든폼팩터/실차 검증은 이 정리로 대체하지 않음.

## 현재 폴더와 읽는 순서

- MainActivity: Android 화면·권한 결과를 받아 ViewModel로 전달.
- ui/location/LocationView: 상태/콜백으로 렌더링, LocationViewModel: 서비스 상태를 파생하고 명령 전달.
- location/LocationSource.kt: SDK 어댑터와 추적이 공유하는 최소 입력 계약/LocationFix.
- location/PleosLocationSource: SDK 필수 호출·정리.
- location/LocationTrackingService: FGS·알림·추적 소유.
- location/LocationTracking: 세션/상태·Main 콜백·버퍼 연결.
- location/LocationBuffer: 시간창과 선택 정책. 시간 단위를 lastEventElapsedMillis로 드러내고 좌표검증은 파일 private.

폴더는 현재처럼 기능위치와 UI위치만 유지한다. 테마는 기존 공유영역에 남긴다. 디렉터리 이름 통일을 위한 전체 파일 이동이나 빈 data/domain 폴더 생성은 하지 않는다. 후속 Room 기능에서 실제 저장 책임이 생기면 그때 기능 Repository를 설계한다.

## 결과 (2026-11-11)

PASS: 단위17개(버퍼10/추적7), debug/release·계측APK 빌드·lintDebug, harness --build, 실제 현재경로 Studio build_project. lint 오류0/기존 경고40이며 Lifecycle 도구 API 불일치로 일부검사가 생략되는 기존한계는 남음.

PASS: 기존 기기회귀3개2.473초, 보강된장시간profile1개54.191초. CREATED 전이 완료 뒤 단조시각164192보다 그 구간 종료 전 최신표본184291이 증가. 재생성 뒤 기준184550보다 최신199586이 증가. 단순히 처리시각이 늦은 예전 콜백으로 통과하지 않도록 SDK 콜백 진입시각과 비교했다. 같은등록1회와 최종45초창 assertion도 유지. evidence/location-midpoint/window-checks.txt.

검증 의미: CREATED 동안 새 수신0건인 기존리뷰 반례는 latest가 구간 시작시각을 넘을 수 없어 이제 실패한다. SDK가 모든 ms/매초 빠짐없이 콜백을 보장한다는 주장은 하지 않는다. 신선도/공백 허용 정책 자체는 변경하지 않았다.

PASS: 최소화면의 제목/상태/시작·중지 문구를 UI 계층에서 실제 확인하고 캡처 검토. 기존 앱영역·레이아웃·버튼 상태 유지, 위치진단UI 추가 없음. controls.png. 문구리소스 외 전체 폼팩터/물리대비/주야간 인증은 이번 범위에서 새로 입증하지 않음.

기기 검증은 현재 로컬 상태(사용자 임시캐시polling 포함)로 진행했다. 해당POLL_1S는 버퍼 입력이 아니며 이번 수정으로 변경하지 않음. SDK순서·null·취소·정리의 계약과 기존회귀 유지. 실제 privacy/null/error/강제서비스사망 주입과 실차는 NOT_TESTED.

기존 step02-buffer-implementation.md의 장시간 테스트 해석은 이번 정정으로 제한한다. 과거53.616초 테스트는 마지막 보관창·상태·등록 유지까지 확인했으며 CREATED 구간내 새 표본을 assertion하지 않았다. 이번 새 구간별 검증이 해당 누락을 보완한다.

판정: 최신 리뷰의 검증누락 수정 및 가독성/책임 배치 정리 완료. 새 상태·의존성·추상계층 추가 없음. 전체SDK/출시적합성 완료 판정은 하지 않는다. 동작 규칙/공통 설계 변경이 없어 부모 설계 브랜치 수정은 N/A. 커밋·푸시·리뷰답글 전송은 하지 않았다.

후속 커밋 요청: 사용자 승인으로 이번 중간정리를 refactor 커밋1개로 푸시. 임시폴링을 메모리에 백업하고 HEAD 어댑터로 잠시 대체한 정확한 커밋 대상에서 단위17/debug/release/계측컴파일/lint 재검증 PASS(3초), finally로 원본폴링 복원. 어댑터와 pr4-implementation-feedback.md는 stage하지 않음. 과거 실제기기 증거와 미검증은 그대로 구분.

# 위치 쪽지 참고 구현: 조사·검증·리팩터링 기록

기록일: 2026-11-10. 새 구현에서 반복 조사를 줄이기 위한 지식 인덱스다. 아래 PASS는 과거 환경에서의 결과이며 새 브랜치 구현의 PASS가 아니다.

## 고정된 기준

- 참고 브랜치: `ny/feat-fused-location-notes`, 최종 리팩터링 커밋 **ae11722dc61a33960703506ac7ebcc2978ba06a6**. 이동하는 브랜치 HEAD 대신 이 커밋을 기준으로 읽는다.
- 새 브랜치: `ny/spec-driven-road-notes`. 부모 `spec-road-note-system`의 c00b3c2. 참고 구현의 앱 코드는 병합하지 않는다.
- 기존 Gleo 기능은 `gm/feat-speech-to-text`/PR #1 소유다. 참고 브랜치의 과거 Gleo 구현은 범위 초과로 제거됐다. 새 위치 기능에서도 재작성하지 않는다.
- 원격 참고: https://github.com/jin-kim-240204/hyundai-and-gretel/tree/ae11722dc61a33960703506ac7ebcc2978ba06a6
- 로컬 조회: `git show ae11722:<파일경로>`. 전체 브랜치를 매번 읽지 말고 단계별 관련 파일만 읽는다.

## 조사 결과와 적용할 판단

| 주제 | 확인된 사실 / 프로젝트 결정 | 새 구현에서의 의미 |
|---|---|---|
| 가상 GPS | 공식 샘플이 `adb emu geo fix <경도> <위도>` 안내. 실제 SDK 콜백까지 확인 | ADB 성공과 SDK 수신 성공을 구분한다 |
| 콜백 주기 | 40개/39.018초 주입에 콜백21개, 최대 간격4.010초 | 1Hz 전체 전달이나 정확한30초 표본을 가정하지 않는다 |
| 버퍼 | 참고 구현은 메모리45초, 목표30초, 최근접 표본 선택. 보간 없음 | 실시간 대기30초를 요구하지 않는다. 이력 부족이면 가장 오래된 실제 표본, 실제 offset 기록 |
| 시간 | SDK timestamp는 차량 시계 영향 가능. 버퍼 경과 계산은 콜백 단조시계 | LLM의 observedAt 제거. receivedAt과 단조시각은 역할을 구분 |
| SDK 필드 | latitude/longitude/altitude/heading/confidence/timestampMillis 확인 | confidence를 미터 accuracy로 간주하지 않는다 |
| 저장소 | 위치 버퍼 메모리, 쪽지 원본·사용자별 수신이력 Room 별도 테이블 | 원본 저장·주변 조회만 추후 서버로 교체. 원시 위치 버퍼는 업로드하지 않는다 |
| H3 | res9 셀을 후보 검색에 사용, 고정 경로400m 범위+이웃 셀 | H3는 후보 축소용이며 실제 반경·방향 판정을 대체하지 않는다 |
| H3 Android | Java4.5.1의 네이티브 라이브러리 누락으로 기기 실패, ABI별 JNI 패키징 후 통과 | JVM 테스트만으로 Android 동작을 보장하지 않는다. 도입 단계에서 최소 구성 검토 |
| proximity | 실제 SDK 등록→밖/안/밖에서 ENTER/EXIT 관측 | ENTER 뒤 앱 조건 검사. 이미 반경 안/새 쪽지/지연 콜백 보완 필요 |
| SDK 등록 | 참고 구현의16개는 앱의 시연 상한 | 공식 최대치가 아니다. 가까운 순 등록과 초과 상태 필요 |
| 웹콘솔 | 속도/기어 조작 자체가 GPS 이동을 보장하지 않음 | debug 어댑터가 로컬8088 상태를 읽어 가상 GPS 주입. 실제 SDK 콜백으로 화면 갱신 |
| mock GPS | SDK 위치 권한과 mock app-op는 별개. deny면 실패 안내 필요 | 고정 외부 GPS로 조용히 대체하지 않는다 |
| A/B | 한 앱·한 Room 공유, B는 쪽지600m 뒤에서 시작 | A/B 전환 시 버퍼와 SDK 등록은 격리하고 쪽지/수신이력은 유지 |
| 화면 | 주행 중 같은 UI 유지, 화면 켜짐 설정 별도 | 정차도 허용. 화면 유지와 위치 수집 수명은 별개 |
| 메모리 수명 | 프로세스가 살아 있는 동안만 유지 가능 | 현재 범위는 가시 세션. 백그라운드/종료 시 수집 중단, 복귀 시 새 버퍼. 서비스 상시수집 미구현 |

## 최종 리팩터링에서 배운 점

1. 후진에서 좌표차 오름차순은 먼 쪽지를 먼저 등록했다. 방향 부호를 반영한 전방거리로 정렬하고 판정에도 같은 계산을 사용한다. `notes/RoadNote.kt`, `ProximityCandidatesTest.kt` 참고.
2. 수신함은 JOIN 후 원본을 건별 재조회하지 않는다. 한 JOIN 결과에 원본+수신자+수신시각을 담는다. 스키마 변경 없이 N+1 조회 제거. `data/NoteDatabase.kt`, `RoomNotes.kt`, `RoomNotesTest.kt` 참고.
3. 큰 Controller를 기계적으로 분해하거나 서버용 계층을 미리 늘리지 않는다. 화면/상태/SDK/저장 책임은 구분하되 필요한 계약만 만든다. 기존 클래스와 파일 수를 복제하지 않는다.

## 과거 검증의 근거와 한계

환경: Pleos Connect2.1.0, Android14/API34, SDK1.1.3, 에뮬레이터 user10, 합성 서울 직선 경로. Room2.8.5/KSP2.3.13/H3-Java4.5.1은 당시 사용 버전이지 최신 권장 버전 선언이 아니다.

- PASS: ADB→SDK 연속 좌표 수신39.111초. 초기 표본개수/2초 공백 가정은 FAIL이었고 정책 재설계 근거가 됐다.
- PASS: proximity ENTER/EXIT 계측27.068초. 해당 수동 `ProximityTest.kt`는 삭제됐으므로 현재 자동 실행 가능하다고 하지 않는다.
- PASS: 최종 리팩터링의 단위11개, Room/세션 계측3개0.516초, SDK A저장→B접근→중복방지 E2E59.055초.
- PASS: 당시 debug/release 빌드·계측 컴파일·lint·harness·Studio 빌드. lint 경고48개 및 도구 API 불일치로 미실행된 Lifecycle 규칙2종은 별도 한계다.
- PASS: 첫 신선한 표본으로 즉시 심기, B600m 초기화, 웹콘솔 주행에서 좌표/거리 갱신. 권한 deny 안내도 검증했다.
- NOT_TESTED: 실제 privacy 차단/null/error 주입 전체, 서비스 강제 끊김, SDK 최대 등록 수, 이미 반경 안 등록의 SDK 단독 동작, 실차/전체 폼팩터/주야간/심사 적합성.
- 초기 실험 원시 JSON·임시 패치·로그는 사용자 요청으로 삭제했다. 수치는 기록된 관측치이며 원시 증거 재분석은 불가하다.

고정 커밋의 원문 기록(저장소 루트 기준): `docs/work/fused-location-spike.md`, `fused-location-implementation.md`, `remove-gleo-from-location-branch.md`, `remove-buffer-warmup.md`, `fix-console-gps-mode.md`, `ponytail-branch-review.md`, `ponytail-branch-refactor.json`, `docs/LOCATION-DEMO.md`. 오래된 구현기록의 Gleo/30초 대기 설명은 후속 제거·수정 기록으로 대체한다.

공식 출처: https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app (조사 commit c955c3a811371892e8803f3b1d9ad3f0426e832c), https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access 및 initialization-and-release, declaring-permissions, error-handling. 기존 `.agents/skills/pleos-connect/references/sources/` 스냅샷도 재사용한다.

## 재사용과 재검증 경계

같은 환경에서 SDK 존재 여부·실험 경로를 매번 재조사하지 않는다. 먼저 이 기록과 고정 커밋의 해당 파일만 읽는다. 새로 작성한 코드는 각 단계 성공 조건으로 반드시 검증한다. SDK/에뮬레이터/권한/ABI/수명 정책이 바뀌거나 실제 실패가 생기면 영향받은 공식 원문과 경로만 다시 확인한다. 과거 PASS를 새 구현 PASS로 복사하지 않는다.

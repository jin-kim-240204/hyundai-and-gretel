# PR4 · 06 독립 코드 리뷰

- AI 독립코드리뷰어, 2026-11-12.
- 고정 HEAD `79516eb2b60874eb94830523f53c0f8b39560af8`, base `6f33659`. 게시 전후 원격 head 일치.
- 최초 판정(79516eb): 필수 수정 P2 2건. 정책 결정 0 / 선택 개선 추가 0. 구현 완료 승인 아님. 리뷰이 후속 판단 대기.
- 적용: pleos-connect, clean-code-review(23 취소/24 해제/25 소유권/28~32 테스트), android-compose-architecture, pleos-vehicle, pleos-design, pleos-verify, ponytail-review. 사용자 실행 금지가 스킬 런타임 루프보다 우선한다.
- 범위: 06 승인 명세/00/공통정본/작업 기록/제출 issuecomment-6812879764, 변경 production callers·테스트·Manifest·debug/release source set, SDK1.1.3 계약. 기존 스펙 합의 재논의와 무관 스타일 변경 제외.

## 계약 추적 · 정적 확인

| 계약 | 코드/근거 | 판정 한계 |
|---|---|---|
| scenario v4·nullable legacy·현재 scenario 원자 reset | NoteDatabase migration3→4, RoomScenarioStore.reset transaction, RoomReceiptStore recipient 전체 JOIN, RoomRoadNoteStore scoped 후보 | 정적 PASS, 실제 SQLite migration/reset NOT_TESTED |
| accepted writes와 role/mode/reset·서비스 수명 | DemoRuntime.allowed/configure/reset, DemoHost, LocationTrackingService.holdStorage/finishWhenStorageIsIdle, ReceiptRepository 관찰 소유권 | 정적 및 기존 JVM 경계 PASS, 실제 FGS/DB NOT_TESTED |
| SDK/mockGPS/external·noReplay·late IO/cache gate | AndroidDemoInput, DemoRuntime.requestStart/checkOwner/closeInput, DemoGeometry.StartGate | gate/취소 host JVM PASS, 실제 SDK/AppOp/provider/HTTP NOT_TESTED. 입력 공백 FAIL 아래 |
| A610±5 / B anchor-600 | DemoGeometry.start/fix/probe, DemoRuntimeTest | host JVM PASS, 실제 SDK 좌표 수신 NOT_TESTED |
| CarUx NO_SETUP/미연결·문자·탐색·ACK·stop닫기 | CarRestrictionsSource, UiRestrictions, LocationViewModel/View/DemoPanel, 서비스 binder | 일부 JVM PASS, 최신 탐색 제한 ACK 재검사 정적 FAIL 아래 |
| 제품 UI와 debug/release 분리 | main/release/debug DemoRuntime/DemoPanel·debug manifest/network config, MainActivity | 정적 확인. 실제 UI/배포 APK 실행 NOT_TESTED |

## 직접 검사

- PASS: `./gradlew testDebugUnitTest --tests 'com.jinkim.hyundaiAndGretel.demo.DemoRuntimeTest' --tests 'com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictionsTest' --tests 'com.jinkim.hyundaiAndGretel.ui.location.LocationConnectionTest'` — 11+2+5=18, XML failures/errors 0. 로그 `/private/tmp/roadnotes06-targeted-tests.log`.
- FAIL(결함 재현): production DemoRuntime + suspend하는 DemoInput, gate 완료 후 가상시간 3001ms. 기대 ERROR지만 READY 및 stop 호출 수 불변. 임시 테스트 `/private/tmp/roadnotes06-review-probe/DemoGapProbeTest.kt`, init `/private/tmp/roadnotes06-review-probe.init.gradle`, 결과 `/private/tmp/roadnotes06-gap-probe-result.xml`. `./gradlew --no-configuration-cache -I /private/tmp/roadnotes06-review-probe.init.gradle testDebugUnitTest --tests 'com.jinkim.hyundaiAndGretel.demo.DemoGapProbeTest'`. 실제 HTTP 검사가 아니라 production orchestration의 공백 처리 반례다.
- 작성자 보고: JVM86/build/lint/static PASS는 제출 근거로 읽었으며 이번 직접 검사 18개와 구분. 빌드/전체86 재실행 안 함.
- 공식 근거 DOC-URLConnection#read-timeout: https://developer.android.com/reference/java/net/URLConnection#setReadTimeout(int) — readTimeout은 데이터 읽기 대기 제한이며 전체 응답 시간 상한이 아님. 06 §6 114행의 마지막 성공 입력 2초 공백과 별개.
- ACK 정적 반례: 최신 raw policy가 LIMIT_CONTENT/maxDepth=0 → combine/stateIn 전파 전 queued click → VM 이전 allowsContent 통과 → binder 최신 canShowBody/fits만 검사 → confirm 수락. 명세 UX04의 보류 시 ACK 불변 위반. 화면/CarUx 경합 실제 실행은 NOT_TESTED.

## 게시

리뷰 COMMENT: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5868267298

- P2 입력 공백: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545675100 (`DemoRuntime.kt:169`).
- P2 최신 탐색 제한 ACK: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545675109 (`LocationTrackingService.kt:144`).
- API로 실제 등록/고정 commit/inline 위치 확인. 기존 스레드 resolve 없음.

## 미검증/보존

runtime NOT_TESTED: 실제 SDK·mock/provider·웹콘솔 HTTP·외부 ADB·CarUx/UI·FGS·Room migration/reset/legacy JOIN·JNI·화면 밖 수신/E2E. 앱/Studio/adb/기기/에뮬레이터/계측 실행 0회.
제품 코드/추적 테스트 파일 수정 없음. 기존 PleosLocationSource.kt polling dirty와 기존 작업문서 보존. 새 기록은 이 파일만 작성, 임시 반례는 /private/tmp에만 둠. 수정 전 완료 처리하지 않으며 리뷰이 판단을 기다린다.

## 게시한 정확한 본문

AI 독립코드리뷰어(Codex) · 06 구현 · 고정 SHA `79516eb2b60874eb94830523f53c0f8b39560af8` / base `6f33659`. 게시 직전 원격 head 일치 확인.

판정: **필수 수정 P2 2건**을 각 production 코드 줄에 남겼습니다. 입력 응답 대기 중 2초 공백 정책과 ACK 직전 최신 LIMIT_CONTENT 재검사입니다. 새 정책 결정/선택 개선은 추가하지 않았습니다.

06 승인 명세·00/공통정본·구현 기록·production 호출부·SDK 1.1.3 계약을 대조했습니다. v4 nullable legacy/현재 scenario만 transaction reset, recipient 전체 JOIN/명시 ACK 보존, accepted writes 잠금·저장전용 수명, 전환 소유권/늦은 입력 차단, A610/B anchor-600 및 두 점 gate, NO_SETUP·미연결 설정 차단/stop·닫기 보존, debug/release 분리는 정적으로 추적했습니다. 이들 정적 확인을 런타임 PASS로 간주하지 않습니다.

독립 검사: 기존 DemoRuntime 11 + UiRestrictions 2 + LocationConnection 5 = **18/18 host JVM PASS**. 별도 임시 host JVM 반례 1건은 기대 ERROR 대신 READY를 확인해 **FAIL(결함 재현)**. 작성자 보고 JVM86/build/lint/static PASS는 별도 제출 근거이며 이번에 전부 재실행하지 않았습니다. 제품/테스트 추적 파일 수정 없이 임시 소스·init script는 /private/tmp에만 두었습니다.

**NOT_TESTED:** 실제 SDK/mock AppOp/provider·HTTP/web console·external ADB·CarUx/UI·FGS·Room migration/reset/legacy JOIN·JNI·화면 밖 수신/E2E. 앱/Studio/adb/기기/에뮬레이터/계측 실행은 하지 않았습니다. 기록: `docs/work/pr4-06-code-review.md`. 수정 검토 전이며 06 완료 승인이 아닙니다.

[P2] 입력 읽기 대기에도 2초 공백 기한을 적용해주세요

AI 독립코드리뷰어 · 고정 SHA `79516eb2b60874eb94830523f53c0f8b39560af8`.

06 §6(114행)은 마지막 성공 입력부터 2초가 넘으면 sticky 입력 오류로 전환하고 수집을 정리하도록 합의했는데, 현재 공백 검사는 `readMotion()`이 반환한 다음 `consoleDelta`에서만 실행됩니다. HTTP 응답 본문이 1초 미만 간격으로 조금씩 도착하면 AndroidDemoInput의 개별 readTimeout/64KiB 제한을 지키면서도 전체 읽기는 2초 이상 지속될 수 있어요. 그동안 READY/SDK 수집이 유지됩니다. [공식 URLConnection 문서](https://developer.android.com/reference/java/net/URLConnection#setReadTimeout(int))도 readTimeout을 전체 응답 마감 시간이 아니라 읽을 데이터가 없을 때의 대기 제한으로 정의합니다.

추적 파일을 수정하지 않고 production DemoRuntime에 지연 DemoInput을 주입한 host JVM 반례로 확인했습니다. gate 완료 → 첫 read 대기 → 가상시간 3001ms 경과 후에도 `READY`, stop 호출 수 불변(기대 ERROR)입니다. 실제 HTTP 재현은 NOT_TESTED입니다.

별도 복잡한 상태를 추가하기보다 마지막 성공 시점 기준 남은 기한으로 입력 읽기 전체를 제한하고, 그 기한 초과를 사용자 취소와 구분해 기존 fail/정리 경로로 보내는 쪽을 추천해요. 블로킹 HTTP 정리와 늦은 응답 소유권 확인은 유지해야 합니다. 수정 후 응답 미완료/조금씩 오는 응답에서도 2초 초과 오류·수집 중지, 이후 늦은 응답 주입 0회, 명시 재시작만 가능함을 확인해주세요.

[P2] ACK 수락 직전에 최신 탐색 깊이 제한도 확인해주세요

AI 독립코드리뷰어 · 고정 SHA `79516eb2b60874eb94830523f53c0f8b39560af8`.

현재 서비스에서 최신 본문/문자열 제한을 다시 읽는 의도는 좋습니다. 다만 `LIMIT_CONTENT`의 `maxDepth`/항목 예산 판정은 ViewModel의 `uiState.value`에만 남아 있어요. 이 값은 combine/stateIn으로 전달되므로 원본 CarUx 상태가 바뀐 직후에는 이전 허용 값일 수 있습니다.

구체적으로 본문과 확인 버튼을 보던 중 제한이 `LIMIT_CONTENT, maxDepth=0`으로 바뀌고, UI 상태 전파 전에 이미 대기 중인 확인 클릭이 실행되면 ViewModel은 이전 깊이로 통과합니다. 이 서비스 조건은 canShowBody/fits만 보므로 최신 정책에서 `allowsContent(..., depth=1)`이 false여도 `receipts.confirm`을 호출해 DB ACK를 수락합니다. 06 §2.1 UX04의 ‘다음 항목 표시 보류 시 DB 큐/ACK 불변’ 계약과 어긋납니다. 정적 호출 경로로 확인했으며 실제 CarUx/클릭 경합은 NOT_TESTED입니다.

본문/문자열 검사처럼 최신 제한과 현재 방문 예산을 같은 Main 수락 경로에서 함께 판정하면 충분해 보입니다. 큰 권한 계층을 만들 필요는 없고, 기존 allowsContent 판정을 재사용하는 작은 변경을 추천합니다. 제한 변경 직후 UI 상태 전파 전 ACK는 쓰기 0회/큐 유지, 이미 수락된 ACK는 끝까지 완료되는 회귀를 구분해 확인해주세요.

# P2 수정 독립 재검토 · e8ef142

AI 독립코드리뷰어 재검토 · 고정 SHA `e8ef1421f6561fa39fde83414c76ad14bd3ee66f` / base `79516eb`. 기존 P2 두 건만 재검토했습니다.

**판정: 기존 4545675100/4545675109 수정 입증, 이 범위의 차단 0건.** 두 스레드에서 근거를 답하고 resolve했습니다. 새 정책/선택 개선은 추가하지 않았습니다. 사용자 결정대로 현재 읽음 정책과 명시 확인 ACK를 유지하며 음성·화면 자동 ACK는 적용하지 않습니다.

독립 검사: 원래 production DemoRuntime 반례 동일 소스/조건 **1/1 PASS** + 관련 production 회귀 **25/25 JVM PASS**(DemoRuntime15/ReceiptAdmission2/UiRestrictions2/LocationConnection6). 2초 전체 기한·SDK 우선 중지·늦은 주입0·정리 잠금·옛 기한 격리 및 최신 raw LIMIT_CONTENT/visited snapshot 수락 검사·거절 DB0·수락 쓰기 완료를 확인했습니다. 작성자 JVM93/build/lint/static PASS는 읽었으며 전체93/빌드/lint를 이번에 재실행한 결과는 아닙니다.

기록 `docs/work/pr4-06-code-review.md` 갱신. 제품코드/기존 dirty 보존. **runtime NOT_TESTED:** 실제 SDK/HTTP/mock provider/CarUx/Binder/Compose/Room/FGS/기기 E2E. 금지된 앱/Studio/adb/기기/에뮬레이터/계측 실행 없음. 허용한 코드 재검토 완료와 기기 검증 대기를 구분하며, 07은 시작하지 않고 조정자 후속 판단을 기다립니다.

AI 독립코드리뷰어 재검토 · 고정 SHA `e8ef1421f6561fa39fde83414c76ad14bd3ee66f` / base `79516eb`.

해결을 확인했습니다. 원래 production DemoRuntime 입력 read 대기 반례 소스를 수정 없이 직접 재실행해 1/1 PASS입니다. 기존 3001ms READY 실패가 ERROR로 바뀌었습니다. 새 production 회귀도 직접 실행해 2000ms 허용/2001ms 오류, tick 포함 전체 기한·prepare 검증시각 유지, 미완료 읽기 종료 전 SDK 중지, 정리 전 재시작 차단, 늦은 주입 0회, 부분 응답으로 기한 연장 없음, 명시 중지/재시작·옛 기한 격리를 확인했습니다.

DemoRuntime 166~201/251~265행에서 현재 motion 소유권에 묶인 기한과 fail의 SDK 우선 중지→읽기 취소/join→입력 정리를 대조했습니다. 기존 CE 전파/HTTP finally는 유지됩니다. 이번 독립 실행은 원 반례 1 + 관련 JVM25 PASS이며 작성자 JVM93/build/lint 보고와 구분합니다. 실제 HTTP/SDK/provider/FGS는 NOT_TESTED입니다. 수정과 허용 검사로 원래 지적이 해소돼 이 스레드만 resolve합니다.

AI 독립코드리뷰어 재검토 · 고정 SHA `e8ef1421f6561fa39fde83414c76ad14bd3ee66f` / base `79516eb`.

해결을 확인했습니다. VM은 파생 uiState 대신 visited.value.toSet()을 현재 Connection→Session→Binder로 전달하고, Binder는 최신 restrictions.state.value로 production confirmVisibleReceipt의 본문/문자/allowsContent 판정을 수행합니다. 현재 사용자·항목·RESUMED 및 쓰기 잠금 검사를 기존 Repository.confirm까지 suspend 없이 이어갑니다. 거절 클릭의 markPresented도 제거됐습니다.

해당 production 수락 함수+실제 ReceiptRepository를 사용하는 host JVM 2건을 직접 통과했습니다. UI 파생 상태를 진행시키지 않은 raw LIMIT_CONTENT/depth0 변경은 DB 쓰기0/큐 유지, 방문 예산 초과 거절·이미 노출된 항목 허용 경계, 수락 후 제한 변경에도 원래 ACK 완료·쓰기 카운트/onFinished 유지를 확인했습니다. Connection snapshot 전달 회귀도 직접 PASS입니다. 명시 확인 ACK는 유지되며 음성/노출 자동 ACK는 추가되지 않았습니다.

이번 직접 검사 총 관련 JVM25 PASS, 작성자 JVM93/build/lint 보고와 별도입니다. Android Binder/VM/Compose/CarUx/Room/FGS 실제 경합은 NOT_TESTED입니다. 원래 지적의 수정 근거가 충족돼 이 스레드만 resolve합니다.

## 실행 및 게시 증거

- 기준 SHA `e8ef1421f6561fa39fde83414c76ad14bd3ee66f`, base `79516eb`. 2026-11-12 원격 head 일치 확인.
- 기존 원 반례 명령을 그대로 재실행: `./gradlew --no-configuration-cache -I /private/tmp/roadnotes06-review-probe.init.gradle testDebugUnitTest --tests 'com.jinkim.hyundaiAndGretel.demo.DemoGapProbeTest'`. 1 test, failures/errors 0. XML `/private/tmp/roadnotes06-rereview-original.xml`, 로그 `/private/tmp/roadnotes06-rereview-original.log`.
- 독립 대상 검사: `./gradlew testDebugUnitTest --tests 'com.jinkim.hyundaiAndGretel.demo.DemoRuntimeTest' --tests 'com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictionsTest' --tests 'com.jinkim.hyundaiAndGretel.ui.ux.ReceiptAdmissionTest' --tests 'com.jinkim.hyundaiAndGretel.ui.location.LocationConnectionTest'`. XML 15+2+2+6=25 tests, failures/errors 0. 로그 `/private/tmp/roadnotes06-rereview-targeted.log`.
- 작업기록 step06-review-fixes.md·pr4-06-code-response.md 및 PR 수용/반영 답글 4545698690/8910/4545777491/7666 대조. 최신 사용자 결정인 명시확인 ACK 유지/음성·화면 자동 ACK 금지 적용.
- 정적 경로: VM55→Connection49→Session15→Binder142→confirmVisibleReceipt27~39→ReceiptRepository.confirm. 최신 raw 정책 + 방문 snapshot 검사/Repository 수락 사이 suspend 없음. 기존 수락 쓰기 scope·카운트·onFinished와 서비스 저장 수명 보존.
- 입력 정적 경로: prepare 전체 검증시각→startMotion166~201의 소유 기한→fail251~265 SDK 우선 중지 및 finally 정리, closeInput79~85의 motion cancel/join. 원 HTTP 구현·기한 뒤 응답의 ensureActive/owner 검사는 유지.
- 입력 반영 확인 답글: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545793636
- ACK 반영 확인 답글: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545793786
- GraphQL resolve 결과 두 thread 각각 isResolved=true 확인. 그 외 스레드는 변경하지 않음.
- COMMENT 재리뷰: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5868418468 (API state COMMENTED 및 commit 일치 확인).
- 새 판정: 기존 P2 두 건 해소/해당 재검토 차단 0, 코드 및 허용 JVM 검사 완료. 실제 HTTP/SDK/CarUx/Binder/Compose/Room/FGS는 NOT_TESTED, 기기 검증 대기. 전체 제품 완료 승인 또는07 착수 지시가 아님.
- 제품 코드 변경 없음, 원래 polling·문서 dirty 보존. 이 리뷰 기록만 갱신.

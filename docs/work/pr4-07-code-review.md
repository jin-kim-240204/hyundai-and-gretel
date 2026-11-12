# PR4 · 07 최종 구현 독립 코드 리뷰

- AI 독립코드리뷰어 · 2026-11-12.
- 고정 HEAD `8f9556f7f58698185d488be2e3d185d22f27fcca`, base `3b844d1`, 부모 정본 `0f0a7da`. issuecomment-6814324203 확인.
- 판정: 새 확정 결함0 / 정책 미결정0 / 추가 선택 개선0. 07 코드 및 허용 검사 범위 차단 없음. 실제 기능 전체 완료·ready/merge 승인 아님.
- 적용: pleos-connect ENV-02/VEH-03/DES-06, clean-code-review23~26/28~32, android-compose-architecture, pleos-vehicle/design/verify, ponytail-review. 사용자 런타임/IDE 실행 금지가 스킬보다 우선.

## 변경 범위 및 계약

MainActivity activity 자식 DO 한 줄만 production 변경이다. 기존 CarRestrictionsSource→UiRestrictions→UI/명령·confirmVisibleReceipt 최신 ACK 수락/서비스 저장 수명은 유지된다. 승인07/00/부모정본의 새 DO 정책과 일치하며 이전06 새 DO 없음은 과거 범위로 처리한다. 다른 Activity/권한/SDK/스키마·Gleo·자동 ACK 변경 없음.

| 근거/AC | 현재 구현 및 관찰 | 판단/한계 |
|---|---|---|
| DOC-AAOS#activity-metadata / I08 | Manifest28, check_activity_do의 직접 자식·단일true 검사, source/양 merged/APK | 직접 PASS, 실제 Drive NOT_TESTED |
| DOC-9faba42791f0#DO이후기능제한 / I06 | 기존 CarUx 구현 불변, DemoRuntimeTest SDK NO_SETUP 상수 보정·새 통합 B 설정 차단/READY 수신/ACK | JVM/정적 PASS, 실제 CarUx/UI NOT_TESTED |
| I01/I05/I07 | RoadNotesDemoFlowTest 첫 테스트: 실제 Tracking/Buffer의30초 anchor→Repository→Candidates→Pickup→Receipt 명시ACK→scoped reset/legacy·타소속 보존 | production orchestration 직접 JVM PASS. Store/Source/Spatial ports는 fake, Room/H3기기/SDK 증거 아님 |
| I03 | 두 번째 통합: 수락 create 대기→SDK Fake 오류→옛 callback 무시→원래 쓰기 성공/실패→공유 종료 판단→신규 제출 거절 | 직접 JVM PASS. Service/Binder/notification/last unbind는 실행 안 함 |
| I02/I04/I06 | 기존 버퍼·후보·Pickup·DemoRuntime·ReceiptAdmission 회귀 재사용 | 전체 clean JVM95 직접 PASS, Android 경계 미검증 유지 |
| F02/F03 | 정확한 최종40자리 새 worktree, full build139 tasks/95 XML/Python4/아티팩트 | 직접 PASS, 작성자 보고 복사가 아님 |
| F05/F06 | LOCATION-DEMO/README/00~07/PR 본문·검증기록, 과거와 현SHA/미검증 구분 | 정적 일치, 전체AC/시연 완료 주장 없음 |

공식 원문 재확인(2026-11-12):
- https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en — activity 자식 DO metadata, 선언과 실제 준수 별개.
- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide — DO 이후에도 실제 ActiveRestrictions를 직접 적용해야 함.
- 기존 DOC-6386c5462132#DO-Drive 지역별 정책은 승인07의 근거로 유지. 이번 SDK API 동작 추가 추정 없음.

## 독립 직접 검사

작성자 `/private/tmp/step07-final-head-evidence.json`의95/Python4/build/lint/해시 보고와 분리해 아래를 직접 수행했다.

전용 clean worktree `/private/tmp/road-notes-07-independent-8f9556f`를 지정SHA로 새로 만들었다. 시작/종료 `git status --porcelain=v1 --untracked-files=all` 빈 결과. PleosLocationSource byte는 해당 git blob과 동일. app/scripts tree는 코드 검사 commit ff92aa2와 동일. 원래 작업 디렉터리 dirty27개는 작성자 최초 해시 목록과 비교해 변경0개.

환경: JAVA_HOME `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (설치 JBR21), ANDROID_HOME `/Users/jinnayeong/Library/Android/sdk`. Studio 실행이 아니라 JDK 명령만 사용. local.properties/소스 복사·외부 init script/임시 테스트 source 주입 없음.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lintDebug --console=plain
PYTHONDONTWRITEBYTECODE=1 python3 scripts/test_road_notes_artifacts.py
python3 scripts/verify_road_notes_artifacts.py
python3 scripts/verify_harness.py
```

- PASS: Gradle BUILD SUCCESSFUL,139 actionable tasks executed. 로그 `/private/tmp/pr4-07-independent-build.log`. 계측 APK는 컴파일/패키징만 수행.
- PASS: clean worktree XML 합계 tests95/failures0/errors0/skipped0.
- PASS: Python4 반례 tests. 로그 `/private/tmp/pr4-07-independent-python-clean.log`.
- PASS: artifact 검사 source/양 merged/APK Manifest·수집location/저장dataSync/권한, release debug 구현/localhost/mock/네트워크리소스 제외, v4nullable/복합키/FK cascade, H3 두ABI ELF. `/private/tmp/pr4-07-independent-artifacts.json`.
- PASS: harness133원문/27규칙·정적Manifest. `/private/tmp/pr4-07-independent-harness-clean.log`. --build를 별도로 다시 돌리지 않고 동일 전체 Gradle 검사와 정적 harness를 각각 직접 실행.
- PASS: `git diff --check 3b844d1..8f9556f`, schema1..4 base 대비 변경 없음.
- 직접 생성 APK SHA256: debug `7c31b601980091efebc314c54ca6dd8598a63afccd5492e54044994f73a91991`, release unsigned `0519343915ce7c653fc78586411a9f0d1c63d85eb0eb01a8afa7527166b5851a`. 작성자 최종metadata/PR 본문과 일치.

## 원격 상태 및 게시

reviewThreads first100 pageInfo.hasNextPage=false 직접 확인. unresolved4541836293 및 이전07 확인 의견4546012581/2592 유지, resolve0. PR OPEN/DRAFT, head고정 일치. 본문은 과거 문서전용 설명을 현 구현/허용검사/runtime대기로 갱신했고 독립검증 대상SHA/APK해시와 일치.

COMMENT 리뷰 https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5869065963

수정 요청 없는 줄별 확인 의견:
- MainActivity DO28: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546261248
- production SDK오류/수락쓰기 host 경계225: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546261257

API로 COMMENTED/commit 일치와 두 코멘트 실제 등록 확인. 제품코드 변경/커밋/푸시/PR ready/merge 없음. 원래 dirty 보존. 결과만 조정자에게 반환 후 대기.

## NOT_TESTED

실제 Drive 화면/DO 효과·SDK/mock/provider/HTTP·CarUx/UI·Room migration/JOIN/reset/동시성·Android JNI·Service/FGS/마지막unbind/시연 R01~07은 NOT_TESTED. 실제SDK 스레드4541836293은 미해결이다. 앱/Studio/adb/기기/에뮬레이터/계측 실행0. 허용검사 통과와 전체 기능 검증 완료를 구분한다.

## 게시 본문

AI 독립코드리뷰어(Codex) · 07 최종 구현 · 고정 SHA `8f9556f7f58698185d488be2e3d185d22f27fcca` / base `3b844d1` / 부모 정본 `0f0a7da`.

**새 확정 결함 0 · 구현 전 미결정 정책 0 · 필수/선택 개선 추가 0. 이번07 변경과 허용 검사 범위에서 차단을 발견하지 못했습니다.** DO와 새 통합 경계에 수정 요청 없는 확인 의견을 각각 인라인으로 남겼습니다. 전체 AC/runtime 완료 또는 ready/merge 승인은 아닙니다.

독립 직접 검증은 작성자 보고와 별도로 새 `/private/tmp/road-notes-07-independent-8f9556f` detached clean worktree에서 수행했습니다. 지정40자리 SHA·시작/종료 clean 및 polling source의 git blob 일치를 확인했습니다.
- JVM **95/95 PASS**, failures/errors/skipped0.
- Python Manifest 반례 **4/4 PASS**.
- assembleDebug/assembleRelease/assembleDebugAndroidTest(컴파일·패키징만)/lintDebug **PASS** —139 tasks executed.
- harness 정적 및 source/양 merged/APK DO·FGS/권한·release DEX/리소스 격리·v4 schema/ABI ELF 검사 **PASS**. schema1~4는 base 대비 변경 없습니다.
- APK SHA256 debug `7c31b601980091efebc314c54ca6dd8598a63afccd5492e54044994f73a91991`, release unsigned `0519343915ce7c653fc78586411a9f0d1c63d85eb0eb01a8afa7527166b5851a`: 작성자 최종 증거/PR 본문과 일치.
- 원래 dirty27개 내용 해시 불변. production 변경은 MainActivity DO 한 줄이며 기존 CarUx·수락 쓰기·명시 ACK/Gleo 코드는 변경하지 않았습니다.

승인07/00/부모정본·production callers·새 host 통합·시연 안내/README·검증 기록/PR 본문을 대조했습니다. I01/I03/I05/I07의 연결은 새 통합2건, 세부 시간/취소/최신 ACK는 기존 회귀, I08은 실제 산출물 검사로 범위를 구분합니다. Fake DB/SDK와 공유 종료 판단을 Room 원자성/Android 수명으로 주장하지 않았습니다. 명시 확인 ACK는 유지하며 화면/음성 자동 ACK·Gleo는 추가하지 않았습니다.

**NOT_TESTED:** 실제 DO/Drive 화면·SDK/provider/HTTP·CarUx/UI·Room migration/JOIN/reset/동시성·Android JNI·FGS/마지막 unbind/E2E. `4541836293`은 unresolved로 유지했고 기존07 스펙 확인 의견도 일괄 resolve하지 않았습니다. 앱/Studio/adb/기기/에뮬레이터/계측 실행0, 제품코드 수정0. OPEN/DRAFT 유지, 후속 판단 대기. 기록 `docs/work/pr4-07-code-review.md`.

AI 독립코드리뷰어 · 고정 SHA `8f9556f7f58698185d488be2e3d185d22f27fcca` · 확인 의견(수정 요청 없음).

MainActivity activity 자식에만 DO를 한 번 선언한 최소 변경을 확인했습니다. 별도의 clean detached HEAD에서 직접 빌드한 debug/release의 merged·패키징 Manifest를 검사해 같은 Activity의 단일 true가 유지됩니다. 누락/false/중복/잘못된 부모/한 variant만 있는 반례 Python 검사도 직접 4/4 PASS입니다.

[AAOS 선언 규칙](https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en)과 위치가 일치하며, [Pleos 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)가 요구하는 DO 이후 ActiveRestrictions 처리는 기존 CarUx 코드를 유지합니다. 실제 NO_SETUP 상수로 설정은 막고 Ready 수집·수신·명시 ACK를 유지하는 새 host 통합도 직접 PASS입니다. 선언/정적 성공을 Drive 화면 유지·안전/심사 PASS로 확대하지 않는 문서 경계가 적절합니다. 실제 CarUx/UI/Drive는 NOT_TESTED입니다.

AI 독립코드리뷰어 · 고정 SHA `8f9556f7f58698185d488be2e3d185d22f27fcca` · 확인 의견(수정 요청 없음).

이 테스트는 가짜 SDK 오류를 production Tracking에 전달하고, 이미 수락한 Repository 쓰기를 대기시켜 성공/실패 양쪽에서 정리 전 종료 판단이 늘지 않음·완료 후 종료 판단·옛 callback 무시·신규 제출 거절을 실제 단언합니다. 새 정상 통합도 Tracking/버퍼의 실제30초 anchor→후보→Pickup→수신→명시 ACK→DemoRuntime reset을 연결해 단순 Fake 상태 맞추기보다 의미 있는 경계 검사로 확인했습니다. 최종 clean HEAD의 전체 JVM95를 직접 PASS했습니다.

다만 DemoHost/Store/Source는 in-memory ports이고 stopDecisions는 공유 판단 함수의 카운트입니다. Android Service/Binder·마지막 unbind·알림/실제 SDK 오류나 Room rollback을 실행한 증거가 아닙니다. 구현 기록/시연 문서/PR이 이 한계를 분리하므로 기존4541836293은 해결하지 않고 유지합니다. 해당 runtime은 NOT_TESTED입니다.
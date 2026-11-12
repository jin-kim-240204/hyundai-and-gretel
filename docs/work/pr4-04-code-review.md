# PR4 04 독립 코드리뷰

AI 독립코드리뷰어(Codex) ·04 구현· SHA `ef4f86006e09c67212a128cb11efaf9ebbf772b4` / base `5a0dd01` (93006bd/c097947/ef4f860).

**판정: 코드 결함1건(P2) 수정·재검토 필요. 정책 질문0 / 선택 스타일 개선0.** 취소된 관찰의 늦은 실패가 같은 세션/cover로 재개한 새 관찰을 지우는 문제를 실제 production NearbyCandidates와 임시 JVM Fake에서 재현해 인라인에 게시했습니다. 구현 완료 판정을 보류하며 리뷰이가 판단하고 작성자가 최소 수정·검증한 뒤 재리뷰해야 합니다.

직접 검사: 기존 H3SpatialIndexTest3 + NearbyCandidatesTest7 + LocationTrackingTest10 = **20 PASS/0 실패/0 오류**. 별도 반례1개는 기대값 Ready에 대해 실제 Failed(STORAGE_ERROR)로 **FAIL**했습니다. 이 반례는 host JVM/coroutine 실험이며 Android/Room 자체 재현은 아닙니다. 임시 source set은 /private/tmp init script로만 연결했고 추적 파일은 수정하지 않았습니다. 결과 XML과 재현 소스 경로는 작업 기록에 남겼습니다. 정상 테스트는 임시 설정 없이 다시 실행해20 PASS를 확인했습니다.

정적 확인: v1 export 변경 없음/v2 nullable H3+인덱스와 ALTER migration·addMigrations 연결, legacy null도 조회 후 계산·검증, 고정UTC SQL 필터 없음, 신규 H3 실패 시 INSERT 전 거절, create/findById 계약 유지. currentCandidates는 raw를 직접 반환하지 않고 현재 위치 일치·세션·cover·단조 신선도·지원영역·현재UTC TTL·실거리로 재검사합니다. 같은 cover 이동은 공간 원본에서 재평가하며 null=사용불가/empty=유효빈목록을 구분합니다. 원시 버퍼 callback 경로는 유지했고05/06 기능은 섞이지 않았습니다. SpatialIndex/NearbyNoteStore는 실제 Fake 교체에 쓰여 불필요한 추상화로 보지 않았습니다.

실제 cached h34.5.1 JAR의 Android arm/arm64 .so를 확인했고, 기존 로컬 debug/release APK에서 libh3-java.so의 arm64 ELF machine183/armv7 machine40을 관찰했습니다. 기존 Compose native도 x86/x86_64에 있어 H3 ARM 포장만으로 앱의 다른 ABI 설치가 막혔다고 지적하지 않았습니다. Android system/host resource 로더 구분과 LinkageError→SpatialUnavailable 실패 격리도 대조했습니다. H3 경계 검사는 정의한 유한 fixture 관측이며 연속영역 증명이 아닙니다. APK/lint는 이번에 새로 빌드하지 않았으므로 작성자의 빌드 PASS와 구분합니다.

AC 연결: H01/H02 host H3 tests, H03/H05/H06/H09 후보 Fake, H04 위 동일키 취소 결함 때문에 미충족, H07 migration/legacy 계측 **작성·컴파일 기록과 정적 검토만**, H08 artifact/APK/ELF 정적 관측만. 전체 AC 실행 완료가 아닙니다. NOT_TESTED: 실제 Room migration/Flow/동시쓰기, Android JNI/16KB 동적로딩, SDK/서비스/Binder/알림·기기/UI/성능·주행. 앱/Studio/adb/에뮬레이터/계측 실행 금지를 준수했습니다. 기존 실제SDK 수명 스레드는 유지했습니다. 신규 앱 코드 수정/커밋/푸시 없음. 결과 보고 후 리뷰이 판단 대기.

## 인라인 필수 수정

**[P2 · 필수 수정] 취소된 이전 관찰의 늦은 실패가 같은 키의 새 관찰을 지웁니다**

현재 key는 `(tracking session, cover cells)`라 같은 세션에서 null/stale 뒤 같은 cover를 다시 관찰하면 재사용됩니다. 94~98행의 catch는 관찰 인스턴스의 취소 여부 없이 이 key만 비교합니다.

발생 순서: ① 관찰A 시작 ② null로 A 취소/clear ③ 같은 추적 세션·cover의 신선한 위치로 관찰B 시작, B가 Ready ④ 취소된 A의 정리가 늦게 IOException을 던짐. 이때 A의 nextKey가 B의 key와 같아 fail(STORAGE_ERROR)이 실행되고, clear()가 **B의 observation과 rows까지 취소/삭제**합니다. B 조회가 성공했는데 이전 요청 때문에 후보를 잃습니다.

추적 파일을 바꾸지 않고 `/private/tmp`의 작은 JVM Fake로 **실제 NearbyCandidates**를 호출해 재현했습니다. A는 취소 후 NonCancellable 정리를 대기하고, B의 Ready를 확인한 뒤 A 정리에서 IOException을 발생시켰습니다. 결과는 `Old cancelled failure cleared new observation: Failed(reason=STORAGE_ERROR)`였습니다. 실제 Room이 이 오류 순서를 내는지는 기기 미검증이며, 취소 지연을 허용한 Store/세션 경계에서 확인한 결함입니다. Kotlin은 취소가 협력적이고 finally 정리를 수행한다는 [공식 설명](https://kotlinlang.org/docs/cancellation-and-timeouts.html)이 있습니다.

가장 작은 수정은 결과 적용과 오류 반영 **직전** 해당 관찰 coroutine이 아직 active인지 확인해 취소된 작업이 상태를 바꾸지 못하게 하는 것입니다(ensureActive 등). 또는 관찰마다 고유한 세대/토큰을 두고 clear/재관찰 때 무효화해도 됩니다. 세션+cover라는 업무 키만 관찰 식별자로 사용하지 않으면 되고 새 coordinator는 필요 없습니다.

확인 조건: A 취소→같은 키 B Ready→A의 늦은 실패/결과에도 B 상태·후보·구독이 유지되고, B 자신의 실패는 정상 Failed가 되어야 합니다. 기존 다른 세션/cover 테스트만으로는 이 재사용 경계를 잡지 못합니다.

AI 독립코드리뷰어(Codex) · SHA `ef4f86006e09c67212a128cb11efaf9ebbf772b4` · NearbyCandidates.kt:94~98 (같은 결과 guard:86). clean-code-review 23~25/32. 실제 Android Room/SDK/서비스 재현은 NOT_TESTED.

## 근거와 재현

- 적용 pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review 11/15/23~26/28~32, android-compose-architecture §3/7/8, ponytail-review. UI 신규변경 없어 pleos-design N/A. 기존04스펙/정본 및 단계작업기록 대조.
- 반례: /private/tmp/roadnotes04-review-probe/OldObservationProbeTest.kt
- 임시 source set: /private/tmp/roadnotes04-review-probe.init.gradle
- 반례 결과: /private/tmp/roadnotes04-review-probe-result.xml (1 FAIL, 실제상태 Failed(STORAGE_ERROR)).
- 실행: ./gradlew --no-configuration-cache -I /private/tmp/roadnotes04-review-probe.init.gradle testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.notes.spatial.OldObservationProbeTest
- 최초 임시 source를 java.srcDir에 연결해 Kotlin test가 발견되지 않았음. kotlin.srcDir로 수정 후 반례가 실제 컴파일/실행됐고 위 assertion 실패를 확인. 이 초기 No tests found는 제품결함 근거에서 제외.
- 정상회귀: ./gradlew testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.notes.spatial.* --tests com.jinkim.hyundaiAndGretel.location.LocationTrackingTest :20 PASS.
- diff --check PASS. 스키마v1 보존/nullableH3/v2 인덱스/정적 이관대조.
- 공식근거: https://kotlinlang.org/docs/cancellation-and-timeouts.html ; https://developer.android.com/training/data-storage/room/async-queries ; https://developer.android.com/training/data-storage/room/migrating-db-versions ; https://developer.android.com/ndk/guides/abis ; H3 v4.5.1 artifact 및 이전스펙리뷰의공식원문 대조. SDK 새계약 추정 없음.

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867169591
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544756327

구현 완료 보류·리뷰이 판단 대기. 기존 미커밋 SDK polling과 작업파일 보존.


## 21783e9 P2 수정 재검토

AI 독립코드리뷰어(Codex) P2 재검토 · SHA `21783e9e670ca963bca069ac248b223435ff2268` (이전 ef4f860).

**수정 입증: P2 해소.** launch 진입에서 캡처한 해당 관찰 자신의 coroutineContext.ensureActive가 결과 적용 전과 H3/인덱스/저장 오류 세 catch의 fail 직전에 모두 있습니다. NonCancellable 정리 안에서 호출되더라도 캡처한 A context를 검사하므로 현재 B Job이나 NonCancellable context를 잘못 검사하지 않습니다. 기존 key/session/신선도 guard·취소 전파도 보존됐습니다.

직접 JVM 재검사: NearbyCandidatesTest9개 + 원 외부 OldObservationProbeTest1개 = **10 PASS/0 실패/0 오류/0 skip**. A의 늦은 결과와 세 오류에서 B Ready/후보/구독이 유지되고 B 후속 목록 갱신 및 B 자신의 오류 분류·진단은 정상 적용됩니다. ef4f860에서 Failed(STORAGE_ERROR)로 실패했던 원반례도 동일 소스로 통과했습니다. diff --check PASS. 앱 코드 수정 없이 임시 /private/tmp source set만 이용했습니다.

따라서 원 지적4544756327을 수정·허용 검증 근거로 resolve합니다. **코드리뷰 관점에서04 구현 및 허용 검사 완료 / 기기 검증 대기**로 판정합니다. 전체 JVM49개·APK빌드/계측컴파일/lint/harness PASS는 작성자 실행 기록과 대조했으며 이번 직접 재실행10개와 구분합니다. 생산 변경은 관찰 context 검사뿐이므로 APK/lint를 추가 재실행하지 않았습니다. 새 스타일/기능 범위는 추가하지 않습니다.

NOT_TESTED: 실제 Room에서 이 정리 오류 순서, migration/Flow 런타임, Android JNI/16KB/SDK/서비스/Binder/알림·UI/실주행. 전체 AC 실기기 완료를 뜻하지 않습니다. 앱/Studio/adb/기기/에뮬레이터/계측은 실행하지 않았고 기존 기기 검증 스레드는 유지합니다. 결과 보고 후 대기합니다.

게시 확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544802629
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867225863
4544756327 isResolved=true API 확인. 초기 원반례1FAIL과 이번동일소스1PASS 대조. 기존 미커밋 SDK/타역할변경 보존.

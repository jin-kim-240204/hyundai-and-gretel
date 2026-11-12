# PR4 05 독립 코드리뷰

AI 독립코드리뷰어(Codex) ·05 구현· SHA `a58efa249960bbaa6ecbca73777150bc2fb06812` / base5957bf2 (9c0c368/bd017fc/16dbea5/a58efa2).

**판정: P2 결함2건 수정·판단 필요. 정책 질문/선택 스타일 개선0.** ① 추적 중지 후 로컬 ACK의 location FGS 의존성(공식 Android14 조건+정적 호출 근거), ② B→C→B 뒤 옛 ACK 실패의 새 inbox 반영(실제 production Repository JVM 반례1 FAIL)을 해당 코드 줄에 게시했습니다. 두 번째는 현재A고정 UI가 아니라05의 주입 가능한 recipient/작업격리 경계에서 확인했습니다. 리뷰이가 범위/근거를 판단하고 수정·재검토 전 완료 판정은 보류합니다.

직접 검사: SyntheticRoute2,ReceiptRepository6,ProximityPickup10,RoadNotePickupFlow1,TrackingReceiptLifetime1,LocationConnection5 = **25 PASS/0실패/0오류**. 별도 AckOwnerProbeTest1 **FAIL**이며 XML에 old failure→new inbox.error=true가 기록됐습니다. 임시 Kotlin source set은 /private/tmp init script로만 연결했고 정상 검사25개는 임시 설정 없이 다시 실행했습니다. diff --check PASS. 전체69개·Debug/Release/계측컴파일/lint/harness PASS는 작성자 기록으로 이번 실행과 구분합니다.

코드/스펙 대조: 동일 SDK instance의 좁은 add/remove 어댑터·pending/tombstone 포함16·소유 SDK lateID 제거·등록전후 오류 Failed 고정/추가수락 차단·다른정리 지속, ENTER/등록성공/위치/후보변경의 공통 평가,04 currentCandidates/2초·5초/150m/route·directionSign/자기·TTL·기수신 검사를 확인했습니다. 합성 방향을 실제 heading/accuracy로 포장하지 않고 legacy 메타데이터 null을 보존합니다. receipt 복합PK/FK·트랜잭션재조회·IGNORE 뒤 실제존재확인·단일JOIN·최초ACK값유지·명시확인/RESUMED/현재키 및 커밋 전 pending유지가 구현돼 있습니다.2→3 ALTER와1→2→3 연결,구schema 불변도 대조했습니다. 수락쓰기 카운트가 서비스 종료 조건에 포함되지만 Android 실제수명은 별도 미검증입니다.

SDK1.1.3 소스에서 확인했던 등록전/후 onError 차이·remove의 원격 ACK 없음·release후새instance 계약과 어댑터 호출을 대조했습니다. 기존 미커밋 SDK polling은 보존/게시SHA 근거에서 제외했습니다. 새 Base/UseCase·상시서비스·서버/06역할UI는 요구하지 않았습니다. Room/SDK 구현을 Fake 통과로 입증했다고 하지 않습니다.

NOT_TESTED: 실제 SDK 등록/늦은콜백/해제/16물리상한,Room migration/동시쓰기/JOIN/ACK,FusedLocation/JNI·서비스FGS/마지막unbind·Compose확인/재생성·실주행/E2E. 계측테스트는 작성·컴파일기록과 정적 확인만이며 실행하지 않았습니다. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지 준수, 추적 소스 코드 수정·커밋·푸시 없음. 결과 기록 후 리뷰이 판단 대기.

## 인라인1

**[P2 · 실행 결함] 로컬 수신 확인을 위치 권한이 필요한 FGS로 시작합니다**

수락한 ACK가 마지막 unbind 뒤에도 끝나도록 bound-only 서비스를 started/foreground로 유지하려는 의도는 맞습니다. 하지만 추적이 꺼진 상태의 이 경로는 **Room 확인 저장만 할 때도 LOCATION 타입**을 시작합니다. Android14/target34에서 ACCESS_COARSE/FINE_LOCATION이 없으면 startForeground가 SecurityException을 던집니다. 현재 앱은 target34이며 Manifest의 서비스 타입도 location뿐입니다.

구체 상황은 과거 PENDING이 저장됨→위치 권한을 거절/회수한 상태로 앱 재실행→수신함 ‘확인’ 클릭입니다. 수신함 조회와 명시 확인은 GPS가 필요 없지만 holdStorageForeground가 먼저 실패하고, ReceiptRepository.confirm은 onWriteAccepted 예외를 error=true로 바꿔 **store.confirm을 호출하지 않습니다**. 위치 권한을 다시 허용하기 전에는 로컬 이력을 확인 처리할 수 없습니다. 이는 [Android 공식 런타임 조건](https://developer.android.google.cn/about/versions/14/changes/fgs-types-required?hl=en#system-runtime-checks)과 호출 경로로 확인한 결함이며 기기에서 재현했다고 주장하지 않습니다. Pleos 위치 API의 동작을 일반 위치 API로 추정한 지적도 아닙니다.

최소 수정은 동일 서비스 안에서도 수집 중 location 유지와 추적 중지 후 저장 전용 foreground 승격을 구분하는 것입니다. 예를 들어 공식 사용례에 local file processing이 포함된 dataSync를 채택한다면 Manifest 타입/권한과 실제 전달 타입을 함께 맞추고 API별 조건을 처리할 수 있습니다. 별도 상시 서비스/WorkManager/위치 재요청을 강제하지 않고 기존 accepted-write 수명은 보존하면 됩니다.

확인 조건: 위치 권한 없음+추적 STOPPED에서도 명시 ACK가 저장 경계에 도달하고, 수락 후 unbind에도 쓰기 완료·마지막쓰기 뒤 종료 요청이 유지돼야 합니다. 정상 위치 수집은 기존 location 타입 그대로여야 합니다. 허용 범위에서는 타입 선택/승격 실패 Fake를 검증하고 실제 Android foreground 동작은 NOT_TESTED로 남기면 됩니다.

AI 독립코드리뷰어(Codex) · SHA `a58efa249960bbaa6ecbca73777150bc2fb06812` · LocationTrackingService.kt:158~159, Manifest·ReceiptRepository.kt:72~74 대조.

## 인라인2

**[P2 · 계약 경계 결함] B→다른 사용자→B로 돌아오면 이전 ACK 실패가 새 수신함을 바꿉니다**

이 비교는 사용자 ID가 같으면 같은 관찰 소유자로 판단합니다. 하지만 수락한 ACK를 끝내면서 이전 세션의 UI 결과는 격리한다는05 계약에서는 사용자 ID가 재사용될 수 있습니다. B의 확인 쓰기 수락/DB 대기→수신자 C로 교체→새 B 관찰 시작→이전 B 쓰기 실패 순서에서 recipient.value가 다시 B이므로 **새 B 수신함의 error가 true**가 됩니다. finally의 confirming 반영도 같은 ID 비교를 사용합니다.

추적 코드를 바꾸지 않고 `/private/tmp/roadnotes05-review-probe/AckOwnerProbeTest.kt`에서 실제 ReceiptRepository와 지연 FakeStore를 사용해 재현했습니다. 새 B inbox의 error=false를 확인한 뒤 옛 쓰기를 실패시키면 `Old ACK failure changed new B inbox ... error=true`로1 FAIL입니다. 기존 acceptedAck 테스트는 B→C까지만 확인해 이 ID 재사용 경계를 잡지 못합니다. 현재 실앱 설정이 A 고정인 점은 알고 있으며, 현재 화면에 이미 B 전환 UI가 있다고 주장하지 않습니다.05가 제공한 recipient Flow/이전 수락 작업 격리 계약의 확인된 실패입니다.

작은 수정은 recipient 관찰/소유 전환마다 구별되는 지역 세대나 토큰을 두고 ACK 수락 시 캡처해, 오류/화면 상태 반영을 같은 소유 기간에만 허용하는 것입니다. 수락된 store.confirm 자체는 원래 키로 끝내고 writes/onFinished 정리는 항상 실행해야 합니다. 사용자 ID를 바꾸거나 이전 ACK를 취소해 회피할 필요는 없고 범용 SessionManager도 필요 없습니다.

확인 조건: B→C→B에서도 옛 성공/실패가 새 inbox의 오류/confirming 상태를 덮지 않으며, 원래 DB 키의 쓰기 완료·쓰기 카운트 정리는 유지돼야 합니다. 반대로 현재 B에서 수락한 새 ACK 실패는 정상 오류로 보여야 합니다. NOT_TESTED: 실제 Room/역할전환UI/기기.

AI 독립코드리뷰어(Codex) · SHA `a58efa249960bbaa6ecbca73777150bc2fb06812` · ReceiptRepository.kt:84·87 · clean-code-review25/32.

## 증거 및 적용

- 적용 pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review15/17/22~27/28~32, android-compose-architecture §3/5~8, pleos-vehicle 실제SDK수명, pleos-design 명시행동/가시성, ponytail-review.
- 대상 spec/00/정본/step05구현기록,호출자·UI·SDK어댑터·Room모델/DAO/이관·production테스트 대조.
- JVM반례: /private/tmp/roadnotes05-review-probe/AckOwnerProbeTest.kt
- init: /private/tmp/roadnotes05-review-probe.init.gradle
- 실패XML: /private/tmp/roadnotes05-ack-owner-probe-result.xml
- 실행: ./gradlew --no-configuration-cache -I /private/tmp/roadnotes05-review-probe.init.gradle testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.notes.pickup.AckOwnerProbeTest
- 정상25개: ./gradlew testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.notes.pickup.* --tests com.jinkim.hyundaiAndGretel.location.TrackingReceiptLifetimeTest --tests com.jinkim.hyundaiAndGretel.ui.location.LocationConnectionTest
- 공식근거: https://developer.android.google.cn/about/versions/14/changes/fgs-types-required?hl=en (공식 Android14 runtime checks/dataSync/location 절; .com service-types timeout 후 공식미러 확인), 기존검토의 Room/Compose 및 SDK1.1.3 sources.jar 선언. 일반 Android위치SDK동작을Pleos에전용하지않음.

## 게시 확인

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867564793
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545085650
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545085659

리뷰이판단/수정재리뷰전완료보류. 런타임미검증유지.


## fdd9bbf 수정 독립 재검토

AI 독립코드리뷰어(Codex) 재검토 · 고정 SHA `fdd9bbfb864ccde54c20bb30562be360ebcd9457`. **기존 P2 두 건 해소. 코드리뷰 관점에서05 구현 및 허용 검사 완료 / 기기 검증 대기**로 판정합니다. a58efa2 대비 수정·리뷰이답변·PR 구현답글을 대조했고 기존 지적 범위만 재검토했습니다.

직접 실행: ReceiptRepositoryTest9 + TrackingReceiptLifetimeTest2 + 원래 외부 AckOwnerProbeTest1 = **12 PASS/0 실패/0 오류/0 skip**. 원래 ACK 반례는 동일조건/동일소스로 통과했습니다. 원본·Debug/Release mergedManifest와 실제 타입선택 호출, 수락전 승격 실패/수락후 쓰기·잠금 정리, B→C→B UI소유 격리와 DB커밋 관찰을 확인했고 diff --check PASS입니다. 작성자의 전체JVM73/build/lint/harness PASS는 이번 직접12개와 구분합니다. 실제ServiceCompat 플랫폼호출을 JVM Fake가 실행한 것은 아니며 mergedManifest도 정적 근거입니다.

입증된4545085650/4545085659만 답글 후 resolve했고 기존 기기 검증 스레드는 유지합니다. 코드 수정·금지된 앱/adb/Studio/기기/에뮬레이터/계측 실행 없음. NOT_TESTED: 실제 Android FGS유형전환/권한회수/마지막unbind, Room migration/Flow/ACK·SDK/JNI·Compose/UI/E2E. 전체AC 실기기 완료 판정이 아닙니다. 결과 기록 후 대기합니다.

게시 확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5867650128
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545155235
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545155500
지정2스레드 isResolved=true API 확인.
직접검사명령: ./gradlew --no-configuration-cache -I /private/tmp/roadnotes05-review-probe.init.gradle testDebugUnitTest --tests com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptRepositoryTest --tests com.jinkim.hyundaiAndGretel.location.TrackingReceiptLifetimeTest --tests com.jinkim.hyundaiAndGretel.notes.pickup.AckOwnerProbeTest
코드변경없음·기존작업변경보존.

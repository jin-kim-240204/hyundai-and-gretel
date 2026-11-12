# 06 독립 코드 리뷰 P2 수정

기준 79516eb, 원격4545675100/5109와 수용 답글4545698690/8910 확인. pleos-connect ENV-02/VEH-03, clean-code-review23~25/28~32, MVVM 상태 정본·취소·자원 해제, ponytail 최소 수정 적용. DOC-9faba42791f0#기능별규제 및 승인06 UX04/§6의 기존 정책만 보완한다. 새 예산 계층·정책·07은 추가하지 않는다.

수정 전 production 반례를 먼저 실행한다. 입력은 독립 리뷰의 DemoGapProbeTest 원문을 재사용한다. ACK는 Android Binder 실행 없이 서비스의 현재 confirmReceipt 본문을 그대로 추출한 JVM 경계와 실제 ReceiptRepository/Fake 저장소로 최신 depth=0 수락을 확인한다. 이후 방문 정본 snapshot 전달·최신 서비스 규제 allowsContent 재검사와 소유 단일 입력 기한/정리를 회귀로 검증한다.

사용자 실행 금지 우선: 앱/Studio/adb/기기/에뮬레이터/계측 실행 없음. 실제 HTTP/SDK/CarUx/UI/Room/FGS는 NOT_TESTED. 기존 polling/문서 dirty 보존, 선택 파일만 커밋한다.

## 수정 전 반례와 수정

- 입력 RED: 독립 리뷰의 `/private/tmp/roadnotes06-review-probe/DemoGapProbeTest.kt`를 수정 없이 실행했다. production DemoRuntime의 read 대기 중 3001ms 뒤 기대 ERROR 대신 READY로 1 FAIL (`/private/tmp/step06-p2-red-input.log`). 수정 후 같은 소스·명령은 1 PASS (`/private/tmp/step06-p2-original-probe-green.log`).
- ACK RED: 수정 전 서비스 confirmReceipt 본문을 기계적으로 그대로 추출한 임시 JVM 경계 + 실제 ReceiptRepository/Fake 저장소로 확인했다. raw LIMIT_CONTENT/depth0 변경 후 UI 파생 Flow를 진행시키지 않은 클릭이 DB 쓰기1회를 수락해 기대0 대비 FAIL (`/private/tmp/step06-p2-red-ack.log`). Android Binder/CarUx 자체를 실행한 결과는 아니다.
- 입력 수정: prepare가 전체 응답을 검증한 단조시각을 보존한다. 현재 motion 소유권의 단일 기한 Job은 마지막 성공+2001ms(정수 ms에서2초 초과)까지의 남은 시간을 사용하며 tick 대기를 포함한다. 유효한 전체 응답 검증 후에만 갱신하고, 반환 직전에도 경과시간을 검사한다. SDK gate 수락이나 부분 응답으로 시각을 새로 만들지 않는다. 블로킹 읽기 완료를 기다리지 않고 먼저 ERROR·gate 차단·SDK 중지를 수행한다. 이후 motion을 취소하고 기존 읽기 종료/provider 정리를 기다리는 동안 재시작/새 설정을 막는다. 늦은 반환은 자신의 취소/소유권 검사로 주입하지 않는다. 사용자 취소는 CancellationException으로 전파하며 이전 기한은 취소한다. 기존 HTTP finally disconnect와 응답 크기/개별 read timeout은 보존했다. 별도 두 번째 감시기·HTTP 계층은 추가하지 않았다.
- ACK 수정: VM의 visited.value를 toSet한 snapshot을 현재 Connection/LocationSession/Binder로 전달한다. 클릭에서 markPresented를 제거해 거절 자체가 방문 예산을 늘리지 않는다. Binder는 최신 raw 규제를 읽고 기존 본문/문자/`allowsContent`를 재사용하는 `confirmVisibleReceipt`에서 판정한다. 이는 동일 Main 호출의 작은 함수 추출이며 새 예산 저장소/계층이 아니다. 현재 사용자/항목/RESUMED와 수락 쓰기 잠금은 기존 ReceiptRepository.confirm이 suspend 없이 재검사한다. 수락 후 쓰기/원래 키/카운트/onFinished/서비스 수명은 바꾸지 않았다.

## 검증 결과

- PASS: JVM 93/93. 기존86 + 입력4(기한 경계·읽기 종료 전 SDK 중지·정리 잠금/늦은 주입0·전체 성공만 갱신·명시 중지/재시작·옛 기한 격리·부분 응답·gate가 시각을 갱신하지 않음), ACK 수락2(raw depth0 UI 미전파 거절/항목 예산 및 수락 후 제한 변경 완료), 현재 Connection/snapshot 전달1. 입력 경계에서 정확히2000ms READY/2001ms ERROR를 구분한다. 부분 응답은 Fake의 부분 진행 모델이며 실제 HTTP 검증이 아니다.
- PASS: debug/release 빌드·계측 APK 컴파일·lintDebug, `python3 scripts/verify_harness.py --build`. SDK 중지 예외에도 입력 정리를 시도하는 finally를 유지했고 최종 production 변경 뒤 JVM/빌드/lint를 재실행해 PASS. 로그 `/private/tmp/step06-p2-final.log`, `step06-p2-final-unit-lint.log`, `step06-p2-harness.log`.
- PASS: schema1..4가79516eb와 byte 동일. release Manifest/DEX의 mock/HTTP/시연 코드·DO 제외 유지. 원래 PleosLocationSource polling +/-행 비교 동일. 새 의존성/권한/SDK API/공통 정책 변경 없음.
- NOT_TESTED: Android Binder/실제 VM·Compose 경합, CarUx 신호, 실제 HTTP/SDK/provider/서비스·FGS/Room/JNI/기기/E2E. 사용자 금지에 따라 앱/Studio/adb/기기/에뮬레이터/계측 실행0회. runtime 스킬 검증은 사용자 지시로 수행하지 않았다.

원격 두 스레드에 고정 SHA/반례와 검증 제한을 답글로 기록하고 독립 재검토를 기다린다. 스레드를 resolve하거나07을 시작하지 않는다. 원래 미추적 리뷰/응답 문서와 dirty 문서는 이번 커밋에 포함하지 않는다.

# 반경 기반 수신

기준은 `codex/radius-only-reception`의 `5201b2f`이다. 사용자 결정에 따라 후보 등록과 최종 판정에서 같은 도로·방향·전방 조건을 걷어낸다. H3 6km 후보 창, 합성 시연 영역, 500m 수신 거리, TTL, 명시 ACK, 수신자·시나리오 분리 및 Gleo 계약을 확인한다.

검증 범위는 JVM, 빌드·린트, Room 내보내기 스키마 및 호스트 SQLite다. 에뮬레이터, ADB, 기기, 앱 실행은 범위에서 제외한다.

- DOC-9844e9eaad3c#위치콜백·해제 및 위치 SDK 수명과 null·오류 처리를 `PleosLocationSource`에서 확인한다. [위치 데이터 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)
- DOC-2bec764daf11#스키마전달 및 assets 위치와 action filter를 검토한다. [Gleo 스키마 전달](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)
- Room v1/v2의 제보, 수신·확인 기록, 시나리오 및 제약·인덱스를 비교한다. [Room 마이그레이션](https://developer.android.com/training/data-storage/room/migrating-db-versions)

## 구현과 검토

- `RoomRoadNoteStore.observeNearby`는 활성 시나리오와 H3 셀로 조회한다. 기존 H3 미저장 행은 좌표로 셀을 재계산한다. `NearbyNoteCandidates`의 6km는 후보 조회 창이고 수신 반경은 500m다. `ProximityPickup`은 반경 진입 SDK 등록 후보를 거리순으로 고르고 위치 변화·등록 완료·진입을 처리한다.
- 등록과 실제 수신에는 시나리오, 자기 제보, 만료, 중복을 검사한다. 기존 H3 합성 시연 좌표 상자(위도 37.4900~37.5100, 경도 127.0200~127.0500)를 송수신 좌표에 적용한다. R1 ±20m 투영은 시연 시작점과 진행 표시용이다. 수신 판정에는 경로·전방·진행 방향·고도 조건이 없다. 인접 도로, 반대편 차로, 고가 아래에서도 수신할 수 있다.
- `eligibleDistance`는 콜백에서 사용하는 최신 위치와 Room 트랜잭션에서 다시 읽은 제보에 같은 500m(경계 포함) 판정을 적용한다. 트랜잭션은 요청의 시나리오, 당시 TTL과 수신자별 유일 키를 검증한다. 위치 신선도(2초)는 요청 생성 전에 확인한다. 명시 ACK와 Gleo 제보 원문·별도 안내문 계약을 유지한다.
- v3은 `direction` 열을 제거한다. 1→2의 nullable 안내문 추가를 유지하고, 2→3은 수신 기록을 임시 테이블에 복사한 뒤 FK가 참조하는 테이블을 재구축한다. 수신·ACK를 복원하고 인덱스를 재생성한다. `routeId`·`progressMeters`는 보존하며 일반 읽기 검증에서 제외한다. 새 쓰기는 제공된 시연 메타데이터를 계속 검증한다.
- 클린 코드 리뷰 규칙 6·9·26·29·31·32를 적용했다. 조회 창과 제품 반경을 구분하고 최종 판정을 재사용한다. 가짜 SDK 콜백 테스트와 실제 SQL을 실행하는 호스트 테스트의 증거 범위를 구분한다.

## 검증 결과

| 상태 | 범위 | 증거 |
|---|---|---|
| PASS | JVM 142개 | `./gradlew --offline :app:testDebugUnitTest`. 실패·오류·스킵 0. 뒤쪽/인접 위치, 경로 정보 없는 제보, 양쪽 499.99m/500.01m, 영역 이탈, 만료·수신자 분리·ACK 회귀를 확인했다. |
| PASS | Debug/release 빌드·린트 및 instrumentation 소스 컴파일 | JDK 21, SDK 35. `./gradlew --offline :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease :app:compileDebugAndroidTestKotlin`. 출력은 로컬 임시 디렉터리. |
| PASS | 호스트 SQL | `python3 scripts/test_receiver_guidance_migration.py`. SQLite 3.50.1, FK 활성 트랜잭션에서 1→2, 1→2→3, 2→3. Room export와 열·FK·인덱스를 비교했다. 제보 원문·안내문(NULL 포함), PENDING/ACK, 시나리오, 실제 후보 SQL 및 FK cascade를 확인했다. |
| PASS | Gleo 스키마 회귀 | `python3 scripts/test_receiver_guidance_schema.py`. 제보 텍스트와 수신 안내문 선언 확인. |
| PASS | 하네스 | `python3 scripts/verify_harness.py`, `python3 scripts/verify_harness.py --build`. 최초 Gradle wrapper 캐시 잠금 쓰기가 샌드박스에서 거부됐고 권한이 허용된 빌드 재실행에서 통과했다. |
| NOT_TESTED | Android Studio IDE 빌드 | 지정 worktree가 열려 있지 않다는 MCP 응답. CLI 빌드를 위에 기록했다. |
| NOT_TESTED | Pleos/Android 런타임 | 요청 범위에 따라 에뮬레이터·ADB·기기·앱을 실행하지 않았다. 기기 Room 오픈 시 마이그레이션 검증, proximity/음성, Gleo 스키마 승인·탐색과 실제 Action/Callback은 미확인이다. |
| N/A | 별도 출시·인증·화면/차량 제어 검증 | 수신·저장 정책 변경이며 해당 작업은 이번 범위 밖이다. |

## 후속 필드 정책 검토 순서

1. `routeId`·`progressMeters`를 먼저 검토한다. `DemoGeometry.start`는 B의 출발 기준을 고르고 `DemoDiagnostics`는 위치를 표시한다. 필요하면 저장 좌표에서 시연 단계에만 재투영하고, 기존 데이터의 동선 재현을 검증한 뒤 삭제 여부를 정한다.
2. `anchorOffsetMillis`는 Gleo 입력의 제보 기준 위치가 실제로 얼마나 이전 표본이었는지 기록한다. 시간 배치와 진단 계약에 근거해 유지하거나 재검토한다.
3. `h3CellId`는 후보 조회 인덱스, `scenarioId`는 데이터 격리, 수신자 키는 중복 방지·독립 ACK에 사용한다. 시연 영역 제한 또는 실서비스 도로 정합 요구가 별도로 바뀔 때 각각 검토한다.

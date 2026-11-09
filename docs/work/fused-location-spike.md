# Pleos FusedLocation ADB 주입 검증

요청: 공식 샘플을 근거로 ADB geo fix → Pleos SDK 콜백 → 30초 수집 가능 여부를 먼저 검증. 이번 브랜치 ny/feat-fused-location-notes에는 제품 기능을 아직 구현하지 않는다.

범위: 공식 샘플을 /tmp/pleos-fusedlocation-official에 clone, 기존 Pleos_Connect_2_1_0 AVD 데이터 삭제 없이 cold boot, 합성 GPS 주입과 샘플 콜백 관측. 필요 시 별도 임시 계측 코드를 샘플에만 추가. 우리 앱/Room/스키마 변경 없음. 단일 에뮬레이터, 두 차량/서버/음성 경고 구현 없음.

선택 규칙 및 원문:
- ENV-01 / DOC-447cb4d9b001#환경: Pleos 2.1.0/API34 및 cold boot, 기존 AVD 사용.
- VEH-01 / DOC-402de45b9f3d#권한: pleos.car.permission.FUSED_LOCATION dangerous 권한. 허용된 시험 앱에서만 사용하며 권한/차단을 구분.
- VEH-03 / DOC-1ba06876919b#생명주기: isSupported→initialize→동일 listener 등록/해제→release.
- VEH-03 / DOC-9844e9eaad3c#콜백: onChanged nullable, onError 처리. getLastKnownLocation을 새 측정으로 오해하지 않음.
- VEH-03 / DOC-ba9b83fc86f6#오류: null/error 관측·검증 실패 기록.
- PRIV-01 / DOC-72a5d7ab4ebb#최소수집: 합성 좌표만 사용. 실위치 경로 기록/서버 전송 없음. 로그는 증거에 필요한 범위로 한정.

공식 샘플: https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app , commit c955c3a811371892e8803f3b1d9ad3f0426e832c. README Send a test location to an emulator는 live updates 시작 후 `adb emu geo fix` (경도→위도) 안내. FusedLocation 1.1.3. proximity add/remove/ENTER/EXIT 예제도 있으나 이번 1차 목적과 별도 검증으로 구분.
공식 SDK 원문은 기존 references/sources/c5e08a80ebfd,402de45b9f3d,9844e9eaad3c,1ba06876919b,ba9b83fc86f6.md 및 Source URL. 2026-11-09 웹으로 수명·위치 접근 원문 재확인.

샘플 LocationData는 latitude/longitude/altitude/heading/confidence/timestampMillis를 사용한다. accuracy/elapsedRealtimeNanos를 준다고 가정하지 않는다. 실제 SDK 바이너리와 콜백 값으로 추가 확인 예정.
성공 기준: 서로 다른 합성 좌표를 주입했을 때 SDK 콜백에서 순서·좌표가 대응하고, 30초 이상 실제 수집 지속. ADB OK만으로 SDK 수신 PASS라고 하지 않음. 제품 버퍼/Room/주행 UI E2E는 별도 미검증.

초기 빌드 실패 수정: 샘플 gradlew 실행 비트가 없어 임시 clone에서만 +x. 기본 JDK25로 Gradle8.10.2/Kotlin 빌드가 25.0.4 오류로 종료되어 설치된 JBR21을 해당 명령에만 지정. 시스템 JDK/프로젝트 앱 설정 변경 없음.

## 실제 결과 (2026-11-09, emulator-5554 / user 10)

- 환경: Pleos Connect 2.1.0, Android 14/API34, FusedLocation SDK 1.1.3, 설치 서비스 versionName=1.0.0, location_mode=3. 원본 샘플 assembleDebug PASS, 임시 계측 assembleDebugAndroidTest PASS.
- 공식 샘플의 Activity를 실행하고 별도 계측에서 SDK 지원/권한 확인 → initialize → callback 등록. `READY_FOR_GEO_FIX` 후 합성 좌표 40개를 1초 간격(39.018초)으로 ADB 주입했다. mock appops/test provider 변경 없이 emulator geo fix만 사용했다.
- PASS: 실제 SDK 콜백 21개, 위경도는 주입 경로의 해당 인덱스와 일치하며 순서 역전 없음. 첫/마지막 콜백 간격 39.111초. SDK timestampMillis 차이도 39.111초. 이 실험에서 SDK timestamp와 콜백 UTC 차이는 1~4ms였다.
- FAIL(실험 가정): 40개 중 최소35개를 받을 것이라는 계측 가정은 충족하지 못해 `TOO_FEW_UNIQUE_POINTS: 21`로 종료. 1Hz 전부 전달은 공식 보장이 아니며 SDK 자체 결함이라고 판정하지 않는다. 어느 계층에서 중간 좌표가 빠졌는지는 이번 실험만으로 확정하지 않음.
- 수신 간격: 최소1.000초, 중앙1.006초, 최대4.010초. 콜백 시각 기준 최근30초 메모리 창에는 마지막 시점 15개, 실제 샘플 span26.074초. 가장 오래된 샘플은 목표30초 전보다3.926초 늦었다.
- FAIL(현재 계획의 가정): 연속성 공백2초 이하 및 target~target+2초 anchor 허용 정책을 이 환경에서 충족하지 못한다. 단순히 30초 창을 유지한다고 30초 전 표본이 항상 있는 것은 아니다. 실패를 숨기기 위해 허용치를 변경하거나 같은 검사를 반복하지 않았다.
- PASS: 테스트 finally에서 같은 listener를 unregister하고 SDK release 완료 로그 확인.
- NOT_TESTED: 제품 코드의 버퍼/심기/Room/B 접근, 정차·권한 거절·null/error 주입, proximity ENTER/EXIT, 주행 UI와 Gleo E2E. 이번 계측은 callback monotonic 시간 기준이며 SDK 측정 단조시각 지원을 입증하지 않는다.

## SDK 데이터와 다음 판단

실제 AAR javap으로 LocationData의 latitude(Double)/longitude(Double)/altitude(Double)/heading(Float)/confidence(Float)/timestampMillis(Long)를 확인했다. confidence=5, 북향 경로 heading=0이 관찰됐다. SDK가 별도 accuracyMeters 또는 elapsedRealtimeNanos를 준다고 가정할 수 없다. confidence의 단위/정확도 환산은 미확인이다. 공식 LocationData 페이지는 접근 가능하지만 공개 텍스트에 필드 의미 설명이 노출되지 않아 값의 이름만 보고 미터로 변환하지 않는다.

https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/location-data/entry

다음 작업 전에 버퍼 조회 정책을 합의한다. 후보는 30초 창의 경계를 감싸는 직전 표본1개도 보관하고 측정 간격/오차 한도를 명시해 가까운 표본 또는 보간을 사용하는 방식이다. 수신 콜백 주기 실측을 바탕으로 결정하며 이번 검증에서 전체 정본 spec을 임의 수정하지 않는다. SDK proximity API(add/remove/ENTER/EXIT)는 존재를 바이너리/샘플에서 확인했으나 실제 작동은 별도 실험으로 남긴다.

## 실험 파일 정리

사용자 요청으로 2026-11-09 원시 좌표 JSON, 계측 패치, 주입 스크립트, JSON 보고서, /tmp 공식 샘플 clone·시험 로그·추출 jar를 삭제했다. 앞의 측정 수치는 당시 관측 결론이며 원시 파일로 다시 대조할 수 없다. 재검증하려면 공식 샘플에서 새 실험을 수행한다. 에뮬레이터 및 설치된 시험 APK는 이번 파일 정리 범위에서 변경하지 않았다.

## 후속 브랜치 계획에 반영할 합의 (2026-11-09)

사용자 요청: 별도 디버그 패널 라이브러리를 설치하지 않고 작은 Compose 패널을 이번 브랜치 작업 계획에 포함한다. 아직 상세 구현 플랜 확정이나 제품 구현 단계는 아니다. ponytail 원칙으로 기존 Compose 구성 요소를 재사용한다.

- 패널 범위: debug 빌드 한정 A/B 전환, 테스트 데이터 초기화, 현재 좌표·버퍼 보관 시간·쪽지 수·최근 제보/수신 상태. 주행 제어는 Pleos Virtual Console, 좌표 이동은 검증된 ADB 주입 경로를 사용한다. 패널에서 차량 상태를 제어하지 않는다.
- SDK 기술성 결론: ADB→실제SDK 연속 콜백과30초 이상 수집은 PASS. 실제 SDK proximity ENTER/EXIT·권한 거절/차단·재연결·정차·주행 화면·제품 E2E는 NOT_TESTED이므로 전체 SDK 검증 완료라고 하지 않는다.
- 구현 순서 초안: (1) 실측 기반 시간/보관/표본 선택 정책과 SDK 필드 계약 정리 (2) 앱에SDK 수명·권한·위치 수집 연결 (3) 메모리 버퍼와 최소 debug 패널 (4) Room 쪽지 저장/제보 진입 및 저장후1회응답 (5) B의 위치+Room 후보 기반 반경·경로·방향 판정/중복방지와 수신표시 (6) Virtual Console+ADB로 A심기→B줍기 통합시연.
- 정책 미확정:30초보다 긴 보관은 반영하되 구체 보관시간·신선도·허용 공백·SDK timestamp와callback시간 역할은 상세 플랜에서 명시한다. 보간은 필수로 추가하지 않고 최근접 표본 선택을 단순 대안으로 검토한다. 기존 정본의2초/accuracy20m/측정단조시각 가정은 그대로 구현하지 않고 실제SDK 필드와 대조해 정본수정이 필요하다.
- B 접근은 우선 콜백마다 앱이 판정하는 기존 설계를 따른다. SDK proximity API 채택은 필수 의존성이 아니며 채택할 때만 ENTER/EXIT 실험을 추가한다. 진입 이벤트만으로 이미 반경 안에 있는 상태나 새 쪽지 생성을 놓치지 않는다.
- 각 단계에서 권한 거절·오류·수집중단·세션전환·실측 좌표 간격을 확인한다. 전체 정본 수정과 Gleo 기능 브랜치 통합은 상세 플랜에서 변경 범위를 구분한다.

이번 추가는 계획 합의 기록만 수정했다. 정적 문서 검사는 재실행하며 제품 동작 상태는 NOT_TESTED를 유지한다.

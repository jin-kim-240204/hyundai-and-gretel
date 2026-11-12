# 시연 입력 실제 디버깅 (2026-11-12)

사용자가 앱 조작을 포함한 디버깅을 승인했다. 기존 실행금지는 이번 진단에 필요한 앱/에뮬레이터 조작에 한해 해제되었으며 데이터 삭제·커밋·푸시는 요청받지 않았다.

- 기존 dirty 전체 보존. PleosLocationSource 사용자 polling 변경 보존.
- 적용: VEH 권한과 실제 SDK callback 검증, SAF CarUx 설정 제한 유지, 취소/세션 정리 유지. DOC-9844e9eaad3c 위치 callback, DOC-1ba06876919b 초기화/해제, 06 캐시 gate/실패 계약.
- 사용자10 mock app-op deny 확인 후 운영자 adb로 allow. 일반 권한 팝업과 별도이며 앱 자체 승격은 추가하지 않음.
- 실제 재현: PROBE에서 test GPS 좌표는 Android에 기록되지만 SDK 표본0, 15초 후 ERROR. Android fused 요청은1초 간격이며 준비점은 단발 주입. 반복 전달 필요 여부를 최소 수정과 회귀로 검증.
- NO_SETUP에서 설정 버튼 잠김은 기존 정책. 무단 우회하지 않음. 정차 준비 후 READY 주행 지속을 검증.
- 검사: 핵심 JVM 회귀/빌드, debug 실제 설치·SDK/웹콘솔/저장·수신 관찰. 실차·release 실제 동작은 미검증.

## 원인 및 최소 수정
1. 운전자10에 mock app-op가 없었음. 운영자 명령으로 allow; 앱 내부 자동 승격 없음.
2. PROBE/START 단발 GPS는 실제 Android 1초 전달 간격에서 누락될 수 있었음. 실제 단발 timeout과 반복 주입의 SDK 통과를 각각 관찰.
3. SDK gate 중에는 콘솔을 읽지 않아 준비 당시 성공시각이2초를 넘긴 뒤 READY→ERROR였음. 기존 startMotion을 PROBE부터 시작해 매초 **전체 HTTP 성공**을 확인하고 해당 gate 좌표를 반복 주입. 실제 SDK가 gate를 통과한 뒤에만 진행 거리 적분. 2초 기한/15초 gate/실패 중지/늦은 결과 차단 유지. 준비 중 입력 실패는 대기 work도 취소.
4. STOPPED 상태 문구와 중지 버튼이 동일했음. 상태는 ‘위치를 수집하지 않고 있습니다’, 버튼은 ‘위치 수집 중지’로 분리.

## 검사 결과
- RED: 원본 HEAD의 DemoRuntime을 이름만 현재 구조로 맞춰 새로운2회귀 실행→2실패. 수정 소스는 finally로 복원. `/private/tmp/road-note-live-red.log`.
- GREEN: 전체 JVM97/실패0/오류0/skipped0, debug APK. 준비점·목표 SDK 응답 지연 중 재주입/신선한 콘솔 갱신·준비 중 blocked read2초 실패·늦은 주입0·기존 취소/수명 회귀 포함. `/private/tmp/road-note-live-green.log`.
- PASS: release APK, AndroidTest APK 컴파일(계측 실행 안 함), lintDebug. `/private/tmp/road-note-live-verify.log`.
- PASS: harness, artifact 검사/검사기4테스트. artifact 검사 JSON의 runtime NOT_TESTED는 정적 검사기의 고정 범위 문구이며 아래 수동 관찰을 대체하지 않는다.
- 수동 PASS: 실제 Pleos 에뮬레이터 사용자10 debug 앱에서 PROBE→START→READY, SDK RECEIVING,45초 버퍼 교체, 콘솔 D36km/h 입력에 SDK 경도 변화, A 주행 중 테스트 제보 저장/빨강 표시, P에서 B역할 전환 후 실제 anchor600m 뒤 출발(SDK10m/anchor610m), B 주행 접근·후보1/등록1→수신1/PENDING1/제품 카드 표시.
- 수동 PASS: 36km/h에서도 제품 화면 유지. CarUx NO_SETUP에서 역할·모드/reset 잠김·제보/중지/닫기 유지. NO_VIDEO에서 합성 그래프 보류는 기존 정책이며 우회하지 않음.
- 콘솔도 브라우저 세션이 끊기면 DDS OFFLINE/emulator_inactive로 쓰기를 거절했음. 네이티브 Chrome 조작/새로고침으로 일시 빈 화면. 별도 테스트 HTTP 세션을 명시 열고 원래 콘솔 API로 D/P·속도 제어했으며 마지막에 세션 닫음. 앱 직접 위치 전달/Fake fallback 아님. 실제 브라우저 페달 지속 조작의 안정성은 NOT_TESTED.
- 종료 확인: 콘솔 P/속도0 확인, 앱 중지 버튼으로 STOPPED. 생성한 쪽지/수신 PENDING1은 보존, ACK/reset/앱 데이터 삭제 없음.
- 증거: `/private/tmp/road-note-live-{screen,driving,submitted,b-ready,b-drive,b-received,product-card,final}.png`와 RoadLocation 로그. 개인 위치가 아닌 기존 합성 경로.
- NOT_TESTED: 실차,release 실제 실행,계측 실행,이 SDK 실제 오류 수명 시나리오4541836293,명시ACK/reset의 이번 실행. 해당 미해결 스레드/전체AC 상태를 변경하지 않음.
- 기존 미커밋 파일·사용자 polling 보존. 커밋/푸시/원격 코멘트 미실행.
- 콘솔 후속 복구: 기존 탭의 빈 화면은 새로고침으로 복구되지 않았으나 같은127.0.0.1:8088을 새 탭에 열어 ‘DDS ONLINE / P / 0km/h’ 화면을 직접 확인했다. 새 정상 탭을 남겼으며 별도 테스트 HTTP 세션은 종료했다. 브라우저 자체 수정/외부 코드 변경 없음.
- 원래 dirty24개 SHA256 재확인: 변경0개. 오늘 추가한 테스트와 본 수정만 별도 기록, 기존 리팩터링도 미커밋 그대로 유지.
- Android 공식 LocationManager mock 권한/주입 계약과 Pleos 위치 데이터 접근 원문을 재확인(2026-11-12): https://developer.android.com/reference/android/location/LocationManager , https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access . mock 입력 성공과 SDK 실제 callback을 구별. 새 callback 외 getLastKnownLocation 캐시로 버퍼를 채우는 변경 없음.

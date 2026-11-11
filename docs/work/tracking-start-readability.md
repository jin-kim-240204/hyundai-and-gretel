# 위치 시작 절차·콜백 가독성 정리

요청: LocationTracking.start의 등록 절차/비동기 콜백 처리 분리, Source false=미지원/초기화실패=예외/nullable 수신시각 계약 명확화. mutableStatus·Main 실행문맥·세션검사·SDK정리·버퍼 정책 유지. 로그 삭제와 로그주입 범위밖 지시가 충돌해 확인 질문을 보냈으며, 답변 전에는 마지막 명시 지시에 따라 로그 주입을 보존한다.

기존 작업:03스펙/개요 진행상태·실행검증 문서·임시POLL_1S·pr4-implementation-feedback.md 미커밋 유지. 앱의 시작/서비스/Source와 모든 관련 단위·계측 호출을 rg로 확인. 새계층/이벤트타입/반환타입은 추가하지 않음.

적용 pleos-connect, android-compose-architecture §4/7/8, ponytail. ENV-02/VEH-03 및 DOC-1ba06876919b#수명(초기화→등록·동일리스너해제→release), DOC-9844e9eaad3c#콜백(실제처리시세션검사·nullable시각), DOC-ba9b83fc86f6#오류(실패/취소·정리보존). 기존원문/스펙을 재사용하며 SDK사용법을 재설계하지 않음.

범위: 작은 private helper로 등록과 콜백 처리를 구분, Boolean 유지+짧은KDoc/함수타입인자명. 기존 회귀 및 예약된 위치/오류콜백→stop/restart 경계 확인. SDK/runtime검증은 새결과와 미실행 구분. 커밋/푸시 없음.

후속 사용자 요청: 서비스 명령/알림/종료 흐름도 정리하고 이번 리팩터링만 커밋·푸시,03스펙은 제외. 로그 주입은 가장 나중의 범위 제외 지시대로 유지하며 삭제 결정은 확인 질문 상태로 남긴다.

서비스: onStartCommand의 when에 ACTION_START/ACTION_STOP/null·unknown을 명시. null/unknown은 기존대로 stopTracking. startTracking은 알림 준비→ServiceCompat.startForeground→tracking.start와 예외 처리를 소유. buildTrackingNotification은 구성만 반환. stopForegroundAndRequestServiceStop은 포그라운드/알림 제거와 stopSelf 요청을 이름에 표시. onDestroy의 정리/cancel은 그대로 유지.

오류 진단: fail의 operation 기본값SDK는 유지, 서비스 포그라운드 준비 실패만 FOREGROUND_START를 전달. 실패 상태/취소 정책을 바꾸지 않고 잘못된SDK_ERROR 표시를 방지한다. 초기화/등록 예외는 기존 Source.start→LocationTracking.start의 catch에서 SDK로 처리한다.

검증 PASS: 단위19개(기존17+예약오류의처리시점세션검사+FGS실패로그구분), debug/release assemble, 계측컴파일, lint, harness --build, 현재프로젝트 Studio build_project. 임시polling을 메모리 백업/HEAD 어댑터로 대체한 정확한 커밋 대상 상태에서도 같은 검사 PASS(3초), finally로 로컬원본 복원.

기기 PASS: Pleos2.1.0/API34/user10 기존SDK해제·시작/중지·화면재생성/연결·알림중지3개2.184초. 명시적시작 뒤 unknown 명령으로 STOPPED 전환하는 회귀 추가. 기기 APK는 harness가 재빌드한 로컬polling 포함 상태이며 cache로그는 이번 판정에 사용하지 않음. null 명령은 동일 기존 stop분기로 코드 대조했고 OS null인텐트를 실제 주입하지 않음.

NOT_TESTED: 실제OS FGS시작 거절/SDK privacy·연결장애주입, 실제권한팝업대기중재생성, 실차. 버퍼 정책은 변경하지 않아50초프로파일은 재실행하지 않고 기존경계 단위10개를 재사용했다. UI/권한/저장소/03단계 코드 변경 없음.

읽는 순서: start→prepareSession→createSource→registerCallbacks→handleRegistration. 나중에 SDK 콜백이 오면 registerCallbacks가 같은 scope에 예약→handleLocation에서 실제처리시generation확인→buffer.offer→결과별상태갱신. 오류도 같은scope의handleError에서 세대확인후fail. 반환Boolean과콜백 데이터형태는 바꾸지 않고 이름/KDoc만 명시했다.

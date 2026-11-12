# 06 승인 명세 구현

기준6f33659/부모2ca00c0. 독립재검토5867895005 blocker0/미결정정책0·지정3스레드resolve는사용자전달.06만구현,07/실행시연/Gleo/서버제외. 기존사용자polling·문서·와이어프레임보존,선택파일만stage.

적용pleos-connect ENV-02/VEH-03/PRIV-01/SAF-01/02/DES-04/06, android-compose-architecture·clean-code-review·ponytail 최소구조/취소/수명, pleos-design 기능별 제한/미확인Figma준수주장없음. DOC-9faba42791f0#기능별규제 실제CarUx와UI/Main명령재검사·미연결설정불가/중지유지, DOC-1ba06876919b#초기화해제 입력/등록/리스너/release대칭, DOC-402de45b9f3d#권한 mock/SDK별도·실패노출. 공식원문/1.1.3·ae11722 근거는승인06/기존기록재사용,실제설치심볼은필요접점에서확인.

묶음: v4/scenario atomicreset·전체receipt큐보존 → pure전환/gate/CarUx검증 → debug입력과기존서비스/MVVM/최소패널연결·release격리. JVM/Fake/debug·release/계측APK컴파일/lint/harness/schema·release정적만허용. 앱/Studio/adb/기기/에뮬레이터/계측실행절대금지. 실제SDK/UI/JNI/Room/CarUx/FGS/E2E는NOT_TESTED. 코드리뷰전전체AC완료아님.

묶음1: v4nullable scenarioId/활성상태행·현재시연원본만삭제+새ID선택트랜잭션. 기존v1/v2/v3 schema불변·legacy보존,recipient전체JOIN/ACK불변. 선택scope의주변Flow/신규수신재검사와Main제보수락의scenario고정계약추가. PASS 기존JVM73/73·계측APK컴파일. v3PENDING조회/새시연/reset뒤legacy명시ACK·다른소속보존·reset실패롤백계측작성/컴파일만,실제Room NOT_TESTED.

묶음2: 순수경로/두점gate/속도적분·debug전환Runtime,실제AndroidMock/loopbackHTTP입력,release의시뮬레이션없는연결점. 서비스는기존SDK에gate훅을사용하고새측정시각을만들지않음. gate전null포함raw차단·사용자중지입력취소/정리·역할쓰기잠금·수락reset독립수명·직전소유검사. CarUx설치SDK심볼(javap)확인·android.car optional라이브러리,현재값/리스너/끊김·소유권차단·해제대칭,DO추가없음.

PASS: JVM83(기존73+DemoRuntime8+UiRestrictions2),debug/release Kotlin컴파일. 최초서비스 lazy연결에서Kotlin추론순환으로컴파일FAIL→DemoRuntime/Lazy/ReceiptRepository 타입명시후PASS. 캐시gate/HTTP취소/권한실패/외부모드·timeout/리셋수명·상한은Fake실행근거이며실제SDK/CarUx/Room/UI는NOT_TESTED. 원래polling소스는변경하지않음.

최종 경계 회귀: JVM 86개 PASS. 추가로 수락 전 foreground 승격 실패 시 reset DB 호출 0회, Ready 수집은 NO_SETUP만으로 재초기화하지 않음, SDK 실패 뒤 늦은 위치 차단, null/옛 수신 시각/아직 명령하지 않은 target 거부를 확인했다. 정확한 5m 경계는 위경도 왕복 오차로 최초 FAIL → 1마이크로미터 계산 오차 허용(5.01m는 거부) 후 PASS. SDK 측정 정확도를 추가하거나 5m 정책을 확대하지 않았다.

서비스 ACK 진입도 최신 CarUx의 본문/문자 상한을 재검사한다. 기존 명시 ACK·원래 키 쓰기·관찰 소유권·STOPPED dataSync 수명은 유지한다. android.car 컴파일 클래스 경로와 별개로 공통 Manifest에 optional uses-library를 명시한다. DO는 추가하지 않는다. 원래 사용자 polling 변경의 +/- 행 비교 PASS, 이번 커밋에는 포함하지 않는다.

묶음3: 기존 제품 한 건 수신/명시 확인 화면과 debug 오버레이를 분리했다. 패널 닫기·재생성은 서비스/DB/ACK를 바꾸지 않는다. 역할/모드/출발점/reset과 별도 확인, SDK 실제 수락점/목표/합성 거리/버퍼/시연 원본·수신·전체 PENDING/쓰기·권한 진단, 실제 SDK 파랑과 시나리오 원본 빨강 축을 연결했다. 제품과 패널의 문자·내용 개수·깊이 제한은 플랫폼 값이며 패널 재개폐로 탐색 이력을 비우지 않는다. 같은 composition에서도 항목 예산을 예약해 여러 행의 동시 상한 초과를 막는다. 화면 가시 수집 중 KEEP_SCREEN_ON만 사용하고 DO를 선언하지 않는다.

검사 결과:
- PASS: `:app:testDebugUnitTest` 86/86. 기존73 + 전환/실패/수명/gate/경계11 + 규제2. 원래 사용자 dirty polling을 포함한 현재 작업 트리에서 컴파일했지만 실제 polling은 실행하지 않았다.
- PASS: `:app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lintDebug`. 최초 lint는 debug loopback domain의 includeSubdomains 누락 FAIL → false 명시 후 PASS. 최신 Manifest의 optional android.car 포함 빌드/패키징 PASS.
- PASS: `python3 scripts/verify_harness.py --build` (JAVA_HOME JBR21), 진입점/133 원문 인벤토리/27 규칙/정적 Manifest. 최종 변경의 추가 Manifest 검사는 별도 harness로 확인한다.
- PASS: schema1/2/3 원본과6f33659 byte 비교 일치, v4 scenario nullable/활성 행/원본·receipt 유지. release merged Manifest에 DO/mock/INTERNET/특례 networkSecurityConfig 없음. release DEX에 AndroidDemoInput/StartGate/DemoMode/DemoPhase/loopback URL/provider 소유 표식 없음, debug network XML 없음. H3 arm64-v8a/armeabi-v7a 패키징 있음(JNI 실행은 아님).
- NOT_TESTED: 실제 SDK 콜백/CarUx 연결·규제/UI·FGS·mock 권한/provider·웹콘솔 HTTP 형식 호환·Room migration/reset/legacy JOIN·JNI·화면 유지·E2E. 계측은 작성/컴파일만 했다. Studio/adb/기기/앱/에뮬레이터/계측을 실행하지 않았다. 검증 스킬의 Studio/실행 요청보다 사용자 실행 금지를 우선했다.

재현은 승인06 §11 절차를 향후 허용 시 사용한다. 웹콘솔 경로는 ae11722의 loopback 형식 재사용이며 공식 안정 API 보장이 아니다. AppOp 지정/단독 mock writer, 실제 콜백 gate, 두 모드 배타성, CarUx 기능별 제한과 reset 롤백은 실제 환경 검증이 남는다.07/지도 SDK/서버/Gleo/새 계층·의존성은 추가하지 않았다. 현재 상태는 구현 제출·독립 코드 리뷰 대기이며 기능 전체 검증 완료가 아니다.

06 코드 리뷰 P2 두 건은 [수정 기록](step06-review-fixes.md)에서 RED→GREEN을 확인했다. 입력 대기 기한과 ACK 최신 탐색 제한만 보완, JVM93/빌드·lint·정적 PASS. 실제 환경은 계속 NOT_TESTED이며 독립 재검토 대기다.

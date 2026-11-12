# 06 승인 명세 구현

기준6f33659/부모2ca00c0. 독립재검토5867895005 blocker0/미결정정책0·지정3스레드resolve는사용자전달.06만구현,07/실행시연/Gleo/서버제외. 기존사용자polling·문서·와이어프레임보존,선택파일만stage.

적용pleos-connect ENV-02/VEH-03/PRIV-01/SAF-01/02/DES-04/06, android-compose-architecture·clean-code-review·ponytail 최소구조/취소/수명, pleos-design 기능별 제한/미확인Figma준수주장없음. DOC-9faba42791f0#기능별규제 실제CarUx와UI/Main명령재검사·미연결설정불가/중지유지, DOC-1ba06876919b#초기화해제 입력/등록/리스너/release대칭, DOC-402de45b9f3d#권한 mock/SDK별도·실패노출. 공식원문/1.1.3·ae11722 근거는승인06/기존기록재사용,실제설치심볼은필요접점에서확인.

묶음: v4/scenario atomicreset·전체receipt큐보존 → pure전환/gate/CarUx검증 → debug입력과기존서비스/MVVM/최소패널연결·release격리. JVM/Fake/debug·release/계측APK컴파일/lint/harness/schema·release정적만허용. 앱/Studio/adb/기기/에뮬레이터/계측실행절대금지. 실제SDK/UI/JNI/Room/CarUx/FGS/E2E는NOT_TESTED. 코드리뷰전전체AC완료아님.

묶음1: v4nullable scenarioId/활성상태행·현재시연원본만삭제+새ID선택트랜잭션. 기존v1/v2/v3 schema불변·legacy보존,recipient전체JOIN/ACK불변. 선택scope의주변Flow/신규수신재검사와Main제보수락의scenario고정계약추가. PASS 기존JVM73/73·계측APK컴파일. v3PENDING조회/새시연/reset뒤legacy명시ACK·다른소속보존·reset실패롤백계측작성/컴파일만,실제Room NOT_TESTED.

묶음2: 순수경로/두점gate/속도적분·debug전환Runtime,실제AndroidMock/loopbackHTTP입력,release의시뮬레이션없는연결점. 서비스는기존SDK에gate훅을사용하고새측정시각을만들지않음. gate전null포함raw차단·사용자중지입력취소/정리·역할쓰기잠금·수락reset독립수명·직전소유검사. CarUx설치SDK심볼(javap)확인·android.car optional라이브러리,현재값/리스너/끊김·소유권차단·해제대칭,DO추가없음.

PASS: JVM83(기존73+DemoRuntime8+UiRestrictions2),debug/release Kotlin컴파일. 최초서비스 lazy연결에서Kotlin추론순환으로컴파일FAIL→DemoRuntime/Lazy/ReceiptRepository 타입명시후PASS. 캐시gate/HTTP취소/권한실패/외부모드·timeout/리셋수명·상한은Fake실행근거이며실제SDK/CarUx/Room/UI는NOT_TESTED. 원래polling소스는변경하지않음.

최종 경계 회귀: JVM 86개 PASS. 추가로 수락 전 foreground 승격 실패 시 reset DB 호출 0회, Ready 수집은 NO_SETUP만으로 재초기화하지 않음, SDK 실패 뒤 늦은 위치 차단, null/옛 수신 시각/아직 명령하지 않은 target 거부를 확인했다. 정확한 5m 경계는 위경도 왕복 오차로 최초 FAIL → 1마이크로미터 계산 오차 허용(5.01m는 거부) 후 PASS. SDK 측정 정확도를 추가하거나 5m 정책을 확대하지 않았다.

서비스 ACK 진입도 최신 CarUx의 본문/문자 상한을 재검사한다. 기존 명시 ACK·원래 키 쓰기·관찰 소유권·STOPPED dataSync 수명은 유지한다. android.car 컴파일 클래스 경로와 별개로 공통 Manifest에 optional uses-library를 명시한다. DO는 추가하지 않는다. 원래 사용자 polling 변경의 +/- 행 비교 PASS, 이번 커밋에는 포함하지 않는다.

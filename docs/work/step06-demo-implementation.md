# 06 승인 명세 구현

기준6f33659/부모2ca00c0. 독립재검토5867895005 blocker0/미결정정책0·지정3스레드resolve는사용자전달.06만구현,07/실행시연/Gleo/서버제외. 기존사용자polling·문서·와이어프레임보존,선택파일만stage.

적용pleos-connect ENV-02/VEH-03/PRIV-01/SAF-01/02/DES-04/06, android-compose-architecture·clean-code-review·ponytail 최소구조/취소/수명, pleos-design 기능별 제한/미확인Figma준수주장없음. DOC-9faba42791f0#기능별규제 실제CarUx와UI/Main명령재검사·미연결설정불가/중지유지, DOC-1ba06876919b#초기화해제 입력/등록/리스너/release대칭, DOC-402de45b9f3d#권한 mock/SDK별도·실패노출. 공식원문/1.1.3·ae11722 근거는승인06/기존기록재사용,실제설치심볼은필요접점에서확인.

묶음: v4/scenario atomicreset·전체receipt큐보존 → pure전환/gate/CarUx검증 → debug입력과기존서비스/MVVM/최소패널연결·release격리. JVM/Fake/debug·release/계측APK컴파일/lint/harness/schema·release정적만허용. 앱/Studio/adb/기기/에뮬레이터/계측실행절대금지. 실제SDK/UI/JNI/Room/CarUx/FGS/E2E는NOT_TESTED. 코드리뷰전전체AC완료아님.

묶음1: v4nullable scenarioId/활성상태행·현재시연원본만삭제+새ID선택트랜잭션. 기존v1/v2/v3 schema불변·legacy보존,recipient전체JOIN/ACK불변. 선택scope의주변Flow/신규수신재검사와Main제보수락의scenario고정계약추가. PASS 기존JVM73/73·계측APK컴파일. v3PENDING조회/새시연/reset뒤legacy명시ACK·다른소속보존·reset실패롤백계측작성/컴파일만,실제Room NOT_TESTED.

# 05 접근·수신 구현

기준5957bf2/부모fdc51fa. 사용자 전달: 독립 스펙 재검토5867373540 blocker0/미결정책0,4스레드 해결. 이번 단계만 구현하며06/Gleo/서버/가상GPS/오버레이 제외. 기존 사용자 PleosLocationSource polling 및 다른 역할 기록 보존, 명시 stage.

적용: pleos-connect ENV-02/VEH-03/PRIV-01, android-compose-architecture MVVM/Repository/서비스 수명, clean-code-review 취소/실패/단일상태, ponytail 최소구현. DOC-1ba06876919b#초기화해제: 동일 SDK 등록/해제/release 대칭; DOC-402de45b9f3d#권한: 기존 dangerous FUSED_LOCATION 사용; DOC-9844e9eaad3c#콜백: 실제 Main 처리 전 세션/요청 검사. 공식 원문·SDK1.1.3 배포물/ae11722 조사 근거는 승인05 §2와 기존 기록 재사용, 새 계약 발명 없음.

묶음1 합성 경로/nullable 메타데이터·DBv3 migration, 묶음2 receipts 트랜잭션/JOIN/명시ACK, 묶음3 proximity/서비스/최소UI 연결·수명. 각 묶음 허용 검사 뒤 기능별 커밋/push. JVM·Gradle빌드/lint/하네스·패키징·schema 정적 확인, 계측 작성/컴파일만. 앱/Studio/adb/기기/에뮬레이터/계측 실행 절대 금지. 실제 Room/SDK/UI/JNI/서비스 실행 NOT_TESTED, 독립 코드리뷰 전 전체 완료 아님.

## 묶음1

합성 R1 투영/방향 부호·메타데이터 경계 검증,03 anchor에만 투영·문맥없음 저장 허용. Room v3 nullable route3컬럼+receipts 복합PK/FK/인덱스, 명시2→3 migration. 기존1/2 schema 수정 없음. PASS: JVM51/51(합성2추가), 계측 APK 컴파일. migration1/2→3 보존 검사는 작성·컴파일만 하며 실제 Room 실행 NOT_TESTED.

## 묶음2

수신 자격 공통 함수/불변 수락 입력,Room트랜잭션 재조회·TTL/타인/경로·거리 재검사/중복 확인·INSERT OR IGNORE, 실패와중복구분. 단일JOIN·멱등명시ACK·확인시각, UI와분리된수락쓰기수명. PASS: JVM55/55(수신/ACK4추가), 계측APK컴파일. Room동시복합키/다른수신자/JOIN/만료/ACK/CASCADE 검사는 작성·컴파일만, 실제실행 NOT_TESTED.

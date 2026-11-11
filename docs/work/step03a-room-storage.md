# 03-a Room 원본 저장 경계

범위: 합의된03스펙의 모델·Room v1·단순 생성/식별자 조회·검증·실제 DB 테스트.03-b 서비스/03-c UI, Gleo, H3, Hilt는 제외. 기존 미커밋 파일은 보존한다. DI 연결은 서비스가 실제 저장을 쓰는03-b에서 규모를 판단한다.

적용 ENV-02/VEH-03/PRIV-01 및 android-compose-architecture §3/4/7/8/9. 공식 Room https://developer.android.com/training/data-storage/room 의 Entity/DAO/Database 분리, suspend DAO, schema export를 적용. PK INSERT는 중복을 실패로 처리하며 REPLACE/IGNORE나 사전 조회를 사용하지 않는다. 단일 INSERT의 원자성으로 충분하므로 다중 명령 트랜잭션/락은 추가하지 않는다. 모델은 Room 어노테이션 없이 두고 Entity 변환은 저장 구현 내부에 둔다. 모든 앱 원본 쓰기는 검증 경계를 통과한다. 미래 서버로 교체할 저장 인터페이스 하나만 둔다.

검증 계획: 순수 길이/좌표/시간/offset 검증, 실제 Room 중복 동시 생성·원본 불변·재열기·취소, export 스키마/빌드/lint/IDE. 위치 수집/서비스 기존 호출부는 변경하지 않는다. SDK·UI 동작 검증 N/A. 공통 정본의 최종 모델과 이번 단계 축소는 후속 확장/migration을 전제로 한 단계별 적용이며 공통 정책을 변경하지 않는다.

의존성 선택: Room2.8.5(캐시/공식 릴리스 확인), KSP2.3.12 채택 검토.2.3.13은 공식 릴리스의 동시 실행 회귀 안내로 피한다. Hilt/서버/빈 수신이력 테이블은 추가하지 않는다.

## 구현과 리뷰 포인트

- notes/RoadNote.kt: Room과 독립된 불변 원본·category enum, TTL·활성 경계, 저장 경계의 검증. UUID 정규 표기와 비어 있지 않은 작성자도 검사한다. content는 저장 진입에서 trim 후 코드 포인트 검증. offset이 실제02선택 결과인지 확인하는 연결은03-b 책임이다.
- RoadNoteStore: create와 findById만 노출. 정상 입력 거절은 예외가 아닌 Invalid/Duplicate, 저장 실패는 StorageFailure로 반환한다. Result를 무조건 감싸지 않고 UI에 필요한 명시적 결과만 둔다. 조회 실패는 호출자에게 전파한다. CancellationException/Error를 포착하는 범용 catch/runCatching은 없다.
- notes/storage: Entity/DAO/Database/Room 구현. PK ABORT INSERT가 원자적 생성/중복 거절을 보장. 제약 오류 후 ID 존재를 확인해 PK 중복만 Duplicate로 분류한다. 다른 제약·SQLite 오류는 실패. 갱신·삭제·원본 반환 성공·시나리오 인덱스 없음. 파일 DB는 applicationContext로 앱 범위 단일 인스턴스.
- DI/서비스/버튼·제출 상태는03-b/c에서 연결. 이번에는 위치 서비스·폴링·기존 미커밋 기록을 수정하지 않았다. 전체 설계 정본의 최종 모델은 단계별 도입 범위와 다르며, 구현 연결 전 정본의 단계별 계약 동기화는 별도 확인이 필요하다.

## 검증 결과

- PASS: 단위 테스트24개(기존 위치19 + 원본 규칙5). app/build/test-results/testDebugUnitTest 참조.
- PASS: Pleos_Connect_2_1_0 Android14/user10 실제 Room 계측5개, 0.184초. 같은 ID12개 경쟁 생성→1생성/11중복, 다른 내용 동시 경쟁→1생성/1중복·원본 불변, 저장 경계 직접 잘못된 입력 거절·500이모지 보존, 파일 DB 재열기·만료 원본 보존, PK 외 제약 오류를 성공/중복으로 바꾸지 않음.
- PASS: Debug/Release assemble, androidTest assemble, lintDebug, verify_harness.py --build, Studio build_project(isSuccess=true). 스키마 v1: noteId PK·10개 NOT NULL 컬럼, source/requestId/scenarioId/추가 인덱스 없음. git diff --check.
- 수정 후 PASS: 처음 계측 컴파일에서 RoomDatabase가 Closeable이 아니어서 use 불가. 테스트 내부 finally-close로 수정 후 빌드/계측/IDE 재검증. 스키마 확인 스크립트의 없는 indices 키 접근도 실제 export 구조에 맞춰 수정 후 확인.
- NOT_TESTED: 실제 디스크 가득 참·프로세스 강제 종료 중 쓰기·실행 도중 취소 주입. 조회 실패는 예외 전파 계약. 파일 재열기는 프로세스 강제 종료를 입증하지 않음. 기존 SDK 수명 계측은 이번에 재실행하지 않음(수집 코드 변경 없음).
- N/A: UI/SDK/Gleo/E2E·제출용 서명 APK. 서비스 저장 수명과 버튼 연타는03-b/c에서 구현/검증한다.

03-a 구현과 위 검증은 완료, 사용자 코드 리뷰 대기.03전체 완료 아님. 커밋·푸시 미실행.

원문 요구사항 추적: DOC-643d0cbc1aa4#entity-dao-database — https://developer.android.com/training/data-storage/room Primary components/Sample implementation. Entity로 테이블, DAO로 저장/조회, Database로 소유하며 suspend SQL은 Room에서 실행한다. storage 파일 및 Room 계측5개/빌드로 확인 PASS. Room 원문은 작업 기록의 참고 근거이며 Pleos 하네스 인벤토리 대상이 아니다. 보고서는 등록된 Pleos 콜백 항목을 이번 저장 전용 범위에 N/A로 명시했다.

## 커밋·푸시 요청 (2026-11-12)

사용자 승인으로03-a 코드·Gradle·스키마·테스트·단계 상태와 검증 기록만 커밋/푸시한다. 기존 폴링·별도 리뷰·실험/와이어프레임 기록은 제외한다. 이전 테스트 이후 구현 변경 없음, 검증 근거 재사용. diff/report를 재확인한다.

# 03-a 리뷰 반영 및 03-b 저장 연결

요청: PR4 피드백 반영/거절 이유 답글과 resolve, 다음 구현03-b. 기존 미커밋 변경 보존. 적용 ENV-02/VEH-03/PRIV-01, android-compose-architecture §3/7/8/9 및 ponytail 최소 변경. 원문 Room migration https://developer.android.com/training/data-storage/room/migrating-db-versions 의 AutoMigration/DeleteColumn으로 데이터 보존, Service https://developer.android.com/reference/android/app/Service 의 started/bound 수명에 맞춰 수락 저장 중 stopSelf 지연. SDK 초기화/해제 순서와 Main 반영 전 세션 검사 유지.

리뷰: TTL/버퍼 시간 정책을 Seconds→Millis 표현으로 통일(내부 단위는ms 유지). 임시 미커밋 polling은 건드리지 않음. isSynthetic 모델/Entity/fixture에서 삭제하되 v1 export 유지하고 v2 AutoMigration. Room/KSP 버전 카탈로그. 제약 오류+기존ID는 Duplicate라는 현재 분류 한계를 문서/겹침 테스트에 명시. 새로운 원인 분류 계층은 추가하지 않는다.

03-b: 서비스 소유 Repository가 Main에서 anchor/생성UTC/ID를 고정한 뒤 저장, 단일 StateFlow로 상태/연타 게이트. 중지/SDK 오류 뒤 신규 제출 금지, 수락한 저장은 최종결과까지 서비스/알림 유지. Application 전역 scope·이벤트버스·Gleo·버튼(03-c) 제외. DI는 한 저장소를 서비스에서 생성자로 연결하는 현재 규모에 맞춰 유지하며 Hilt를 위해 별도 Application/Module을 선제 추가하지 않는다.

검증 계획: 기존 단위/Room회귀, v1→v2 보존, 제약 겹침, 고정 anchor/TTL·연타·중지·실패/취소 순수 테스트, 실제 서비스의 저장 대기→중지/마지막 unbind→완료→종료 계측. 빌드/lint/harness/IDE. 구현·검증 중, 결과는 후속 기록.

사용자 후속 확정: 실제 생성/사용 DB 없음. v1 스키마를 직접 정리하고 버전 증가/데이터 보존 migration/destructive fallback 모두 제외. 기존 계측 fixture DB는 테스트 전용이며 제품 보존 대상 아님.

## 결과 및 범위

PASS: 단위29개(기존24+Repository5), Room6개(기존5+제약/기존ID겹침), 기존SDK해제/서비스수명3개, 서비스 저장수명1개(성공/실패 두 경로) 계측 총10개/5.546초. 서비스 계측은 실제 Pleos SDK 좌표 수신 후 테스트가 DB 트랜잭션을 잡아 저장을 대기시키고, 수집 중지/마지막 binder 해제/화면 종료 후 ‘제보 저장 중’ 알림 유지와 결과 후 서비스 종료를 확인했다. 합성GPS13회 주입. 단위는 새 좌표/시간 및 stop 후 버퍼 초기화에 흔들리지 않는 입력, Submitting 연타1건, 실패 뒤 새ID, 위치없음/시각초과 DB미접근, 실패/취소 및 완료콜백을 확인.

PASS: Debug/Release·androidTest 빌드, lintDebug, verify_harness.py --build, Studio build_project. 스키마는 v1/9개 NOT NULL 컬럼, noteId PK, isSynthetic 없음. 데이터 보존/자동 초기화 설정 없음. 첫 계측은 오래된 앱APK와 새 테스트APK를 설치해 NoSuchMethodError 발생, 두 APK를 최신으로 빌드 후 수정. 그 첫 실행이 만든 빈 이전 테스트DB의 스키마 불일치는 제가 생성한 파일만 run-as로 제거해 수정했다. run-as 옵션 순서도 수정 후 실제 정리 확인. 제품 코드에 삭제 경로를 넣지 않았다.

적용한 clean-code-review 스킬: 단위/명명인자, 상태에서 isSubmitting/isTracking 계산, 콜백 문맥·세션, 취소 재전파, Fake와 실제SDK/Room 증거 분리. 새 별도 상태 플래그·BaseRepository·락·전역scope 없음. Repository는 서비스 수명이며 Binder에는 상태와 debug 제보만 제공, 화면 연결은03-c에서 ViewModel에 추가한다. 수집 중지와 수락 저장 종료를 분리했고 저장만 남아 있으면 재시작 요청도 수락하지 않는다.

NOT_TESTED: 실제 SDK 자체 오류 주입 뒤 저장 수명(동일 finishWhenStorageIsIdle 경로 코드 검토/위치 단위 테스트, 실제 서비스에서는 사용자 중지 검증). 프로세스 강제 종료/디스크 가득 참/실차, 최종03-c UI·Gleo는 미구현.03-b 구현과 위 검사 완료/코드 리뷰 대기이며 모든 운영 환경 실패가 입증된 것은 아니다.

SSoT: 공통 설계 브랜치9081d62에 확정된03단계 계약만 별도 커밋·푸시 후 현재 기능 브랜치에 병합. 부모에 있던 clean-code-review 스킬 등도 함께 최신화하여 읽고 적용했다. 기능 구현 변경은 아직 미커밋. 기존 임시 polling/타 작업 문서 보존. 리뷰는 수정·검증된 항목에 답글/resolve하며 거절 항목 없음. 시간이 초로 선언되어도 저장/SDK 계약 단위는ms임을 명시한다.

최종 정리: 수정 제안4건과 구조 유지 확인1건 모두 답글·resolve 완료, 거절 항목 없음. 최신 단위/release 빌드도5초 PASS. 계측이 만든 합성 road-notes.db 파일만 강제종료 후 정리하고 이번에 시작한 에뮬레이터 종료. 기능 코드 커밋/푸시는 미실행이며 공통 설계 커밋/병합만 수행했다.

## 커밋·푸시 요청

사용자 승인으로 리뷰 반영(refactor)과03-b 저장 연결(feat)을 분리해 커밋·푸시한다. 직전 검증 이후 코드 변경 없음. 보고서/diff 검사 재실행, 이전 단위29·계측10·빌드 근거 재사용. 기존 polling 및 다른 작업 문서는 제외한다.

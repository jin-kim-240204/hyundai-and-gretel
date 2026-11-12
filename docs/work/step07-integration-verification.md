# 07 통합 구현·허용 검사

승인 명세3b844d1131671ad92429b5ba465d67264b398d78/부모0f0a7daa1189bcdba62cd27a58033a8659b43737. 독립 리뷰5868721026 blocker0/미결정0·리뷰이 답글4546027806/4546028016 및 pr4-07-spec-response.md 확인. 이번 역할은 구현자다. 다른 역할 review/response는 읽되 stage하지 않는다.

적용 pleos-connect ENV-02/VEH-03/DES-06·MVVM/최소 변경·취소/자원 정리/명시 ACK, DOC-6386c5462132#DO미선언Drive·DOC-9faba42791f0#DO선언후기능제한. 공식 원문/Activity metadata 위치는 승인07 §3.1과 직전 원문 재확인 근거를 재사용한다. 변경은 MainActivity 자식 최소 DO, read-only artifact 검사/반례, 기존 production/Fake 연결 통합 테스트, 시연/README/단계 상태·작업 기록/PR 정리뿐이다. 새 SDK/Gleo/자동 ACK/계층/의존성/DB 스키마 변경은 하지 않는다.

원래 tracked/untracked27개 해시/목록을 /private/tmp/step07-run-original-dirty.json에 보존했다. 원래 polling/피드백·타역할 문서는 수정·stage·stash하지 않는다. 최종40자리 HEAD의 별도 detached worktree에서 실제 commit 파일만 검증하며 원래 작업 디렉터리를 교체하지 않는다. 앱/Studio/adb/기기/에뮬레이터/계측 실행은 절대 하지 않는다. 실제SDK/Room/JNI/FGS/Drive/UI/CarUx는 NOT_TESTED,4541836293 unresolved 및 PR draft 유지. 전체 AC/기능 완료가 아닌 독립 코드 리뷰 제출이다.

구현 작성: production 변경은 MainActivity activity 자식 DO 한 줄뿐이다. 통합 테스트는 실제 Tracking/Buffer→Repository→현재 후보→Pickup→Receipt/규제 확인→DemoRuntime reset을 메모리 ports로 연결했다. 기존 레거시/다른 소속 이력과 단일 큐, 실제30초 anchor/600m B 간격, 늦은 옛 callback, SDK 오류 후 수락 저장 성공/실패·새 제출 거절 및 공유 종료 판단을 관찰한다. Fake DB/SDK의 한계를 명시하며 Android 수명/SQLite 원자성 증거로 쓰지 않는다.

DO 정적 검사 원래 source에서 FAIL 확인(`/private/tmp/step07-do-before.log`) 후 한 줄 추가. Manifest 검사에는 옳은 MainActivity 자식 단일 true와 누락/false/중복/잘못된 부모/debug만 선언 반례4개가 있다. read-only artifact 검사는 source/양 merged/APK Manifest·FGS/권한·release DEX/리소스 제외·v4 schema/FK·ABI ELF·APK 해시를 확인한다. apkanalyzer는 APK 파일 분석이며 앱 실행이 아니다. 서비스 타입은 패키징 도구의0x9 표기와 merged의 심볼 표기를 구분한다.

초기 smoke: 기존 작업 트리 JVM95 PASS(기존93+통합2), Python 반례4 PASS. 최초 통합 fixture compile은 순환 타입 추론 FAIL→Tracking/Repository/Pickup 선언 타입 명시 후 PASS. 기존 dirty 포함 worktree smoke이므로 clean HEAD 결과를 대체하지 않는다. docs/LOCATION-DEMO.md는 실제 시연 대본만 작성했다. 다음은 코드 커밋을 고정하고 별도 detached clean HEAD에서 전체 허용 검사를 실행한다.

첫 clean 지정 커밋8ce80c0dbf34e3ac9f5f6e570e64c22cdc0588c9에서 전체 JVM95·debug/release/계측 compile/lint/harness·Python4·source/merged/APK DO/release/schema/ABI 정적 PASS, 검증 worktree tracked/untracked 상태도 비어 있었다. 원래 polling source는 지정 commit blob과 byte 동일했다. 최종 재검증 전 NO_SETUP 회귀의 기존 숫자32(1 shl5)는 실제 NO_SETUP64 상수가 아니므로 설치 SDK 상수로 대체했다. 신규 통합에서도 실제 NO_SETUP으로 B의 역할/reset이 차단되면서 Ready 수집·접근·명시 ACK는 유지되는지 직접 관찰하도록 강화한다. production CarUx 코드는 바꾸지 않는다.

# 04 H3 주변 후보 스펙

요청: 조정자승인으로03-c 최종899fd88(blocker0/기기NOT_TESTED) 다음04스펙만작성. 운영지시/AGENTS→pleos-connect·android-compose-architecture·clean-code-review·ponytail 적용. 스펙독립리뷰전코드/의존성변경금지. 앱/설치/Studio/adb/기기/에뮬레이터/시뮬레이터/계측실행금지. 이번은문서/정적검사/커밋/푸시/PR게시까지만.

읽은자료:00-overview·정본의03최신계약/과거주기조회설명,03원본/DAO/Databasev1,lessons-learned,ae11722의SpatialIndex/RoadNote/RoomNotes/Gradle JNI. 현재03는단순create/noteIdPK/9컬럼·재시도없음이며참고의createOrGet/requestId/scenarioId를복원하지않음. 참고H3JNI통과는이번PASS아님. pr4-03c-code-review.md의최종재검토/resolve와code-response.md의판단을읽었고관련두파일만04문서와함께버전관리하도록승인됨. spec-review/response/원래다른untracked문서는보존.

범위:04-nearby-candidates.md 신규,00진행상태,이기록/JSON,관련03-c코드리뷰/판단기록. 부모정본수정/새라이브러리/Room버전·스키마/앱코드수정없음.05수신/06시연은경계만설명. H3res9·400m·10초/셀갱신·cover경계·거리정렬/방향책임·nullablelegacy보존v2·JNI패키징·실패/세션·검증정의.

적용 ENV-02/VEH-03/PRIV-01. DOC-9844e9eaad3c#콜백: 수신시각·SDK실행문맥·세션차단보존,후보조회만최신 위치로취소/교체하며버퍼콜백은누락시키지않는계획.원문은기존SDK자료재사용. 이번새차량API없음.

공식확인 2026-11-12: https://h3geo.org/docs/api/traversal/ 의gridDistance/gridDisk는셀hop이며미터반경이아님; https://h3geo.org/docs/core-library/restable/ 는위치별셀크기변동이있어평균면적/변길이로400m보장을할수없음; https://github.com/uber/h3-java 의Java/native구조. 실제선택4.5.1artifact/API/ABI는구현직전정적대조할계획. 참고SpatialIndex의평면사각형/centercover+neighbor는서울한정이며경계증명은미완료. 실제H3fixture/oracle로검증후채택한다. Room명시적migration·파괴적초기화금지는03정본최신계약을따른다.

독립리뷰쟁점: 서울보수적cover의입증범위,04에H3만영속화/방향필드05보류,legacy null조회시계산/향후backfill,정본구버전‘폴링없음’과최신overview‘10초’H3정책충돌. 채택후부모정본별도동기화전제이며이번에공통정책을독자변경하지않음.

금지실행/코드변경없음. 신규구현/JVM/ABI로딩/Room실제동작/성능/UI/시연은NOT_TESTED. 문서단계이므로Gradle/JVM실행N/A. 문서정적검사후docs커밋·push/PR4 AI스펙작성자·SHA·미검증·리뷰요청게시,04구현은하지않고조정자독립리뷰대기.

정적검사 PASS:04/00상대링크, diff검사, verify_harness.py --report(4checks/exit0). PR4 head899fd88/base·head이름확인. 앱/JVM/Gradle/기기실행없음(N/A또는상기NOT_TESTED). 이번관련6파일만명시stage한다.03-c독립리뷰의타역할실행결과는그기록의작성자검증이며이번직접실행으로주장하지않음.

## 04 리뷰이 채택5건 반영

pr4-04-spec-review/response를읽고P1현재시점소비진입점·P2명시영역/바깥후보/03저장보존·P3RoomFlow/폴링제거·O1cover실패시에만overlap비교·O2Androidsystem/hostresource로더구분을반영. 부모정본2ef41ce를먼저별도docs커밋push후기능브랜치에병합.04/00과관련작업기록을동기화하고독립재검토를위해5스레드반영SHA답글만남긴다. resolve·앱구현·의존성/기기실행없음. 원래미커밋/다른역할자료보존.

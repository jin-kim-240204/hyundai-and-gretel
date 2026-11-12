# 04 공간 후보 구현

승인: 조정자전달5a0dd01/정본2ef41ce 독립재검토 blocker0/미결정책0.04만구현하며05수신/06시연선행없음. 운영지시·AGENTS→pleos-connect/android-compose-architecture/clean-code-review/ponytail/verify/commit/pr 적용. 앱·설치·Studio·adb·기기·에뮬레이터·계측실행금지, JVM/빌드/정적만허용. 기존SDKpolling/타역할문서보존.

범위:04-a H3/cover/JNI,04-b v2nullableH3/legacy보존/RoomFlow,04-c 세션후보관찰/현재소비검사. ENV-02/VEH-03/PRIV-01, DOC-9844e9eaad3c#콜백(버퍼전달·수신단조시각·세션차단보존). H3공식regions/traversal/restable/h3-java4.5.1과ae11722 SpatialIndex/JNI를스펙검토근거로재사용. v4.5.1실제cachedJAR에android-arm/arm64.so,host리소스존재정적확인. Android system/host resource로더구분. Room공식observable쿼리/migration정책재사용. 새PleosAPI추정없음.

검사계획:실H3hostJVM/res9·명시영역과경계/oracle·거리순서,Fake Flow·시간·세션·consumer·실패, v1/v2export/migrationSQL·계측compile, debug/release/계측APK빌드·lint/harness·APKABI/ELF. 실제AndroidJNI/Room/SDK/UI/서비스는NOT_TESTED. 기능묶음별커밋push후PR보고/독립코드리뷰대기. 구현중,검증PASS선언전.

## 허용 검증 결과와 범위

PASS: JVM47개/실패0(기존36+실H3 3+후보7+현재위치세션1). H3는host newInstance를사용,명시중심격자/영역모서리·변·셀변/꼭짓점근처500개초과중심과각15도방위·0/200/399.99/400/400.01m fixture를검사. 원안centercover+neighbor가검사범위에서무누락으로통과해overlap대안은구현하지않음. 유한회귀이지연속영역증명아님. 정책거리수치오차oracle1e-6m는제품반경에가산하지않음.

PASS: 후보Fake RoomFlow의정지새원본/같은cover이동·단일구독, 오래된cover/세션결과/위치변경처리전소비차단, raw가만료되어도소비에서현재UTC로제외, TTL넘긴응답·stale기한/복구·H3/스토리지/영역/셀512상한/손상원본실패, 399.99/400.01m·경계밖원본·정역이동가까운순·tie/중복. 현재위치StateFlow는버퍼ACCEPTED만발행하며null/stop/세션교체/이전콜백차단을기존실행문맥에서검증.

PASS: Debug/Release/계측APKcompile·lintDebug, verify_harness.py --build. APK zip에서lib/arm64-v8a/libh3-java.so ELF machine183/armeabi-v7a40을debug/release모두확인. Linux/macOS/Windows/원래Androidjarresource경로는APK에없음. abiFilters로앱지원ABI를줄이지않음; H3기능은artifact에있는두Android ABI만지원하며다른ABI에서로딩실패는명시실패/SDK수집유지정책. JVM host로딩성공은Android성공이아님.

PASS: v1export불변·v2의기존9컬럼/PK동일·nullableH3/인덱스추가정적대조. SQL ALTER ADD/CREATE INDEX 명시,원본삭제없음. migration/legacy관찰·신규H3실패미삽입의계측2개작성/compile만함. Store는신규H3를저장경계에서검증/계산,외부H3입력없음. v1행은조회시계산하며DBbackfill하지않음. 캐시/관찰SQL에UTC바인딩없음.

수정한FAIL: H3격자상한fixture의부동소수점덧셈이명시경계를미세하게초과해IllegalArgumentException발생. 정수마이크로도기반fixture생성으로수정하고재검사PASS. 제품영역/반경허용치를늘리지않음. JSON nullable필드의notNull생략을정적검사에서고려하지않은KeyError도기본false대조로고쳐확인. 실제cover누락으로실패한것은아님.

NOT_TESTED: 실제Room v1→v2 migration/Flow/동시쓰기, Android JNI동적로드·16KB page size·SDK/서비스/Binder/알림·UI·성능·실주행. 계측/앱/설치/Studio/adb/에뮬레이터/시뮬레이터/GPS주입은전혀실행하지않음. 새도구/런타임에서통과했다고확대하지않는다. 03단계의기기실행결과재사용없음. H3 host native access/Gradledeprecation경고가있지만현재허용검사FAIL은없다.

production변경은04내공간인덱스/관찰·시간소비경계만.05proximity/수신/receipt·06가상주행·A/B·새UI/Gleo·서버없음. currentCandidates()는null=현재사용불가/empty=유효한빈후보이며rawReady직접소비금지. 다른기간의최신위치와오래된cover를조합하지않도록적용위치/세션/키검사. 기존사용자polling파일은변경·stage하지않음.

## PR 필수 P2 후속 — 동일 키 관찰 교체

적용 ENV-02/VEH-03/PRIV-01, clean-code-review 취소·수명 규칙. 대상 ef4f860 / PR 인라인4544756327. 범위는 NearbyCandidates 관찰 결과·세 catch, 순수 JVM 회귀와 이 기록. 기존 SDK/저장 정책·다른 미커밋 변경 보존. 앱·Studio·adb·기기·계측 실행 금지, resolve/05 진행 없음.

수정 전 반례를 직접 실행하여 FAIL 확인: OldObservationProbeTest 1개 중 1실패. `Old cancelled failure cleared new observation: Failed(reason=STORAGE_ERROR)`. `/private/tmp/step04-p2-before-result.xml`과 before.log에 증거 보관. 같은 session/cells B Ready 뒤 취소된 A 정리 IOException이 B를 지움. 관찰 launch 자신의 context를 결과 적용과 세 오류 처리 전에 검사하는 최소 수정으로 방어한다.

수정: 관찰 launch에서 자신의 coroutineContext를 잡고 collect 결과 적용 전 및 H3/인덱스/저장 오류 catch의 fail 전 ensureActive 검사 4곳 추가. 기존 key/session/신선도 guard와 CancellationException 재전파 유지. 서비스 scope/현재 observation Job 검사·새 token/계층 없음. 서비스 호출부·SDK·스키마 수정 없음.

검증 결과:
- PASS: 같은 외부 반례 수정 후 1/1 통과 (`/private/tmp/step04-p2-probe-after.log`). 수정 전 1실패와 대조.
- PASS: 기존 47+추가 2=전체 JVM49, 실패/오류/skip 0. 새 테스트는 A의 늦은 H3/IllegalArgument/IO 예외와 NonCancellable 문맥의 늦은 결과, 동일키 B Ready/후보/구독 유지·후속 목록 갱신·B 자신의 세 오류 분류·취소 진단 없음 검증.
- PASS: assembleDebug/assembleRelease/assembleDebugAndroidTest(컴파일만)/lintDebug (`/private/tmp/step04-p2-checks.log`), 하네스 정적 검사 및 diff --check.
- NOT_TESTED: 사용자 제한으로 Studio/앱/adb/기기/에뮬레이터/계측 실행 안 함. 실제 Room 정리 오류 순서·JNI/SDK/서비스 런타임 입증 아님. 기존 미커밋 PleosLocationSource polling은 보존하며 이번 커밋에 포함하지 않음.
- 다음: PR4544756327에 구현 SHA·결과 답글 후 독립 재검토 대기. resolve 및 05 진행하지 않음.

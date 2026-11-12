# 07 통합 구현·허용 검사

승인 명세3b844d1131671ad92429b5ba465d67264b398d78/부모0f0a7daa1189bcdba62cd27a58033a8659b43737. 독립 리뷰5868721026 blocker0/미결정0·리뷰이 답글4546027806/4546028016 및 pr4-07-spec-response.md 확인. 이번 역할은 구현자다. 다른 역할 review/response는 읽되 stage하지 않는다.

적용 pleos-connect ENV-02/VEH-03/DES-06·MVVM/최소 변경·취소/자원 정리/명시 ACK, DOC-6386c5462132#DO미선언Drive·DOC-9faba42791f0#DO선언후기능제한. 공식 원문/Activity metadata 위치는 승인07 §3.1과 직전 원문 재확인 근거를 재사용한다. 변경은 MainActivity 자식 최소 DO, read-only artifact 검사/반례, 기존 production/Fake 연결 통합 테스트, 시연/README/단계 상태·작업 기록/PR 정리뿐이다. 새 SDK/Gleo/자동 ACK/계층/의존성/DB 스키마 변경은 하지 않는다.

원래 tracked/untracked27개 해시/목록을 /private/tmp/step07-run-original-dirty.json에 보존했다. 원래 polling/피드백·타역할 문서는 수정·stage·stash하지 않는다. 최종40자리 HEAD의 별도 detached worktree에서 실제 commit 파일만 검증하며 원래 작업 디렉터리를 교체하지 않는다. 앱/Studio/adb/기기/에뮬레이터/계측 실행은 절대 하지 않는다. 실제SDK/Room/JNI/FGS/Drive/UI/CarUx는 NOT_TESTED,4541836293 unresolved 및 PR draft 유지. 전체 AC/기능 완료가 아닌 독립 코드 리뷰 제출이다.

구현 작성: production 변경은 MainActivity activity 자식 DO 한 줄뿐이다. 통합 테스트는 실제 Tracking/Buffer→Repository→현재 후보→Pickup→Receipt/규제 확인→DemoRuntime reset을 메모리 ports로 연결했다. 기존 레거시/다른 소속 이력과 단일 큐, 실제30초 anchor/600m B 간격, 늦은 옛 callback, SDK 오류 후 수락 저장 성공/실패·새 제출 거절 및 공유 종료 판단을 관찰한다. Fake DB/SDK의 한계를 명시하며 Android 수명/SQLite 원자성 증거로 쓰지 않는다.

DO 정적 검사 원래 source에서 FAIL 확인(`/private/tmp/step07-do-before.log`) 후 한 줄 추가. Manifest Python 검사는4개 unittest이며 옳은 MainActivity 자식 단일 true와 누락/false/중복/잘못된 부모/debug 전용의 여러 입력을 검증한다. read-only artifact 검사는 source/양 merged/APK Manifest·FGS/권한·release DEX/리소스 제외·v4 schema/FK·ABI ELF·APK 해시를 확인한다. apkanalyzer는 APK 파일 분석이며 앱 실행이 아니다. 서비스 타입은 패키징 도구의0x9 표기와 merged의 심볼 표기를 구분한다.

초기 smoke: 기존 작업 트리 JVM95 PASS(기존93+통합2), Python 검사4 PASS. 최초 통합 fixture compile은 순환 타입 추론 FAIL→Tracking/Repository/Pickup 선언 타입 명시 후 PASS. 기존 dirty 포함 worktree smoke이므로 clean HEAD 결과를 대체하지 않는다. docs/LOCATION-DEMO.md는 실제 시연 대본만 작성했다. 다음은 코드 커밋을 고정하고 별도 detached clean HEAD에서 전체 허용 검사를 실행한다.

첫 clean 지정 커밋8ce80c0dbf34e3ac9f5f6e570e64c22cdc0588c9에서 전체 JVM95·debug/release/계측 compile/lint/harness·Python4·source/merged/APK DO/release/schema/ABI 정적 PASS, 검증 worktree tracked/untracked 상태도 비어 있었다. 원래 polling source는 지정 commit blob과 byte 동일했다. 최종 재검증 전 NO_SETUP 회귀의 기존 숫자32(1 shl5)는 실제 NO_SETUP64 상수가 아니므로 설치 SDK 상수로 대체했다. 신규 통합에서도 실제 NO_SETUP으로 B의 역할/reset이 차단되면서 Ready 수집·접근·명시 ACK는 유지되는지 직접 관찰하도록 강화한다. production CarUx 코드는 바꾸지 않는다.


## clean 코드 커밋의 새 증거

검사 SHA: `ff92aa27b59f103d033eedfb785cc4cb34b9ff3f`. 원래 프로젝트와 분리한 `/private/tmp/road-notes-07-verify-8ce80c0`를 이40자리 SHA로 detached checkout했다. 시작/종료 `status --porcelain=v1 --untracked-files=all` 빈 상태, 원래 polling 없이 PleosLocationSource 파일이 지정 git blob과 byte 동일함을 확인했다. Gradle/local.properties/소스를 복사하지 않고 JAVA_HOME=설치 JBR21.0.11, ANDROID_HOME=설치 SDK를 사용했다. 외부 init script/임시 테스트 source 주입 없음.

- PASS: `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lintDebug` (로그 `/private/tmp/step07-clean-code-final.log`). JVM95/실패0/오류0/skip0. 계측은 컴파일/패키징만 했다.
- PASS: `python3 scripts/test_road_notes_artifacts.py` 4개. source DO 부재를 먼저 FAIL로 확인한 뒤 declaration과 양 variant의 실제 merged/APK를 긍정 검사했다.
- PASS: `python3 scripts/verify_harness.py --build` (133 원문/27 규칙·정적 Manifest), `python3 scripts/verify_road_notes_artifacts.py`. source·양 merged·APK의 MainActivity 직접 자식 DO 단일 literal true, 수집location/저장dataSync 타입/권한, release에서 debug 패널/mock/HTTP/특례 설정 제외, schema1~4 불변·v4nullable/복합키/FK cascade·Android H3 두 ABI ELF 포함. APK 분석은 SDK apkanalyzer의 정적 명령이다.
- PASS: 문서 링크/fence/JSON/diff 정적 검사, 원래 dirty27개 해시/목록 보존. app production 변경은 DO 한 줄이며 기존 CarUx/ACK/쓰기·취소 코드는 보존했다.
- NOT_TESTED: R01~07 전체, 실제SDK/provider/HTTP·CarUx/Drive/UI·AndroidJNI/Room·FGS/마지막unbind·시연. `4541836293` unresolved 유지. 07 스펙의 두 확인 의견4546012581/2592도 수정 요청이 없었던 검토 이력으로 보존하며 일괄 resolve하지 않는다. 앱/Studio/adb/기기/에뮬레이터/계측 실행0회.

검사 코드는 I01/I03/I05/I07 연결을 새 통합2건으로 보완하고, I02/I04/I06의 세부 경계·실패는 기존 버퍼/후보/Pickup/Receipt/DemoRuntime/최신 ACK 회귀를 재사용한다. I08은 새 Python/실제 산출물 검사다. Fake store의 중복/소속/삭제 모델은 SQL 트랜잭션·JOIN/원자 reset 실행이 아니고, 종료 판단 카운트는 Android stopSelf/알림/마지막 unbind가 아니다.

코드 커밋의 APK SHA256:
- debug: `7c31b601980091efebc314c54ca6dd8598a63afccd5492e54044994f73a91991`
- release: `0519343915ce7c653fc78586411a9f0d1c63d85eb0eb01a8afa7527166b5851a`

마지막 문서 커밋 후에도 **그 최종40자리 HEAD 자체**로 같은 clean worktree에서 JVM/빌드/lint/harness/schema/release 검사를 재실행한다. 자기 자신 SHA를 포함하는 문서 커밋 순환을 피하기 위해 위에는 검사 코드 커밋을 고정하고, 최종 HEAD의 실행 SHA/결과/APK 해시는 PR 구현 보고 및 `/private/tmp/step07-final-head-evidence.json`에 별도로 기록한다. 최종 HEAD의 app/scripts tree가 위 코드 커밋과 다르면 영향 검사를 다시 평가한다. 문서만 바뀌어도 사용자가 요구한 최종 HEAD 검사는 생략하지 않는다.

상태: 구현·허용 검사 및 시연 준비 제출, 독립 코드 리뷰 대기. 실제 시연/전체 AC/기능 완료 승인이 아니다. 부모 정책은 이미0f0a7da에 동기화되어 이번에 다시 바꾸지 않았다. PR 본문을 현황에 맞게 갱신하되 OPEN/DRAFT·base/head·미해결 runtime 스레드를 유지하고 Gleo/자동 ACK/ready/merge/배포를 추가하지 않는다.

# 공모전 1차 제출 준비

기준은 `codex/contest-submission-prep`의 `0fd9015048c379e38fa808d21df3423c93d187aa`입니다. 앱 소스와 UI 기여 커밋을 상속합니다. README·런북·점검표와 패키징 검사, 최신 UI를 따라가지 못한 정적 테스트만 갱신했습니다. 권한·Gleo 노출·위치 저장 정책의 변경 및 기기 연결은 이번 범위에서 제외했습니다.

## 공식 원문 및 적용

| 규칙 | 원문과 확인 범위 | 상태 |
|---|---|---|
| DOC-fdfbe109bf17#접수양식과선택제출 | 제출 안내의 이메일 지원 및 제출물 절. 접수양식 필수, 시연 영상·저장소·APK 선택, 참가자 개별 동의·서명·날짜와 팀원 참조. 공모전 README 제출 전 확인에 반영. | PASS |
| DOC-fdfbe109bf17#저장소와README | 저장소 제출 절. 소스와 구동 가능한 release, 기본 경로와 파일명, 용량 초과 시 Release 링크, 최종 커밋 안내. 소개·실행·환경·화면 규격·APK 경로를 README에 기재. | PASS (안내) |
| DOC-fdfbe109bf17#구동가능APK | 제출용으로 서명되었으며 기동과 핵심 기능이 확인된 release가 필요. | FAIL |
| DOC-e79a69247bd6#Gleo예외 | 운영 공지 §1. 참가팀의 Gleo App Schema 별도 검토·승인 면제. §2는 Pleos 에뮬레이터 PT와 App Market 심사 구분. 런북·설계 및 출시 점검표 정정. | PASS |
| INT-01 | Gleo 수신기 release 패키징 결과 `exported=false`. `GleoActionReceiver.sendCallback`은 release에서 `context.packageName`에 응답. `DemoRuntime`의 `enabled/canSubmit=false`, 서비스의 `DEMO_REQUIRED`, Room의 null 시연 범위 빈 목록 때문에 새 설치에서 핵심 흐름 재현이 차단됨. 발신자 신뢰와 Callback 계약, 실제 스캔 및 왕복 확인 필요. | FAIL |
| PRIV-01 | 이번 변경은 저장·권한 코드에 손대지 않음. 현재 release의 제보 저장 시연 가드는 기존 유지. 기기에서의 위치 정책과 저장 검증은 미실행. | NOT_TESTED |
| REL-01 | 기존 debug 키를 쓰는 로컬 debug와 unsigned release만 확인. 설치·기동 확인된 서명 release와 제출 링크 대기. | FAIL |

[공식 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission) · [최신 기술 범위 공지](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000008) · [출시 제한](../RELEASE-READINESS.md)

Pleos App Market 공식 문서는 별도 배포 범위입니다. 앱 제출 및 리뷰 총칙과 하위 기준 5종(기능·성능, 콘텐츠, 주의 분산, 등록정보, 개인정보), 태그·버전·보안·업데이트 지침을 확인했습니다. 향후 별도 심사 시 등록정보와 자료 준비, 실제 성능·안전·개인정보 근거, 공식 앱명 태그 적용 조건, 버전 갱신과 업데이트 UX를 확인해야 합니다. 이번 공모전 작업의 App Market 입점 검증은 N/A입니다. [심사 제출](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide) · [심사 기준](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide)

## 빌드 증거

JDK 21, Gradle 9.6.0, Android SDK Platform 35, Build-Tools 35.0.0. 기본 캐시는 잠금 파일 쓰기 제한이 있어 임시 캐시에 기존 의존성을 복사했습니다. Gradle의 로컬 소켓 사용은 승인된 샌드박스 외 실행으로 처리했고 의존성 다운로드는 `--offline`으로 제한했습니다.

- PASS · `./gradlew --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease :app:lintRelease :app:assembleDebug :app:lintDebug`. JVM 143개(23 XML), 실패·오류·건너뜀 0. debug/release assemble 및 lint 성공.
- PASS · `python3 -m unittest discover -s scripts`, 11개 통과. 기준의 UI 변경은 반투명 흰색 백드롭을 사용하는데 정적 테스트가 불투명 흰색을 요구해서 최초 실패했습니다. 테스트를 0.4 알파 기대값에 맞춰 재실행했습니다.
- PASS · `python3 scripts/verify_harness.py --build` 및 `./gradlew --offline --no-daemon :app:compileDebugAndroidTestKotlin`. 계측 테스트는 소스 컴파일만 확인했습니다.
- FAIL · `python3 scripts/verify_harness.py --report docs/work/contest-submission-prep.json`은 구조 검사를 통과하고 출시 판정으로 exit 1을 반환합니다. 공모전 원문 ID는 이 작업 기록에, 기존 하네스 인벤토리의 ID는 JSON에 기록했습니다.
- PASS · Build-Tools `apksigner verify --verbose` 및 `aapt dump badging/xmltree`. release는 비디버그 빌드, `GleoActionReceiver` 외부 비공개. 서명은 검증에 실패했습니다. debug는 APK 서명 v2 검증 성공 및 디버그 가능으로 확인했습니다.
- PASS · 패키징 검사에서 실제 unsigned APK, debug APK의 지문 불일치, debug APK의 지문이 일치해도 디버그 가능 상태로 각각 실패하는 것을 확인했습니다. 서명 APK의 성공 경로는 NOT_TESTED입니다.
- NOT_TESTED · 기기·에뮬레이터·ADB 실행, 새 설치부터 스캔·Callback·수신·TTS·화면 동작, release 전용 JVM 테스트, IDE 실행 및 App Market 심사. 서명된 APK, 접수양식과 팀원 동의·서명 및 접수 메일은 준비 범위 밖입니다.

| 로컬 산출물 | 바이트 | SHA-256 | 서명 검사 |
|---|---:|---|---|
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 43,461,581 | `283c141eb6abb001ee035f41ab783f68816909e0129decead7c642ec2ce968d7` | exit 1, Missing META-INF/MANIFEST.MF |
| `app/build/outputs/apk/debug/app-debug.apk` | 52,472,650 | `1afad8cc7e7f8507b4e61911eb53e4f301794e544a52652b44b7f6fadbfc3c12` | exit 0, v2 |

## 공개 전 추적 파일 리뷰

현재 기준 커밋의 tracked 이름과 텍스트를 대상으로 키·토큰 패턴, 연락처 및 절대경로, 빌드 생성물·키·로컬 설정 후보를 검사했습니다. 개인 연락처·대표 자격 증명 패턴·추적 APK 및 추적 ignore 파일이 검출되지 않았습니다. 연락처 후보는 공식 문서 연락처와 Kotlin 라벨이었습니다.

- 작업 로그의 로컬 절대경로를 일반화했습니다. 개인 홈 경로는 `docs/work/service-rename.md`, 홈 또는 임시 로그 경로는 `docs/work/gleo-tts-implementation.md`, 임시 경로는 `docs/work/gleo-spec-base.md`, `docs/work/radius-only-reception.md`에서 확인했습니다. 공식 원문 스냅샷 `.agents/skills/pleos-connect/references/sources/2ac36a28bccc.md`의 시스템 임시 경로 예시는 보존했습니다.
- 공식 참조 스냅샷과 내장 이미지 `.agents/skills/pleos-connect/references/sources/`, 폰트 `app/src/main/res/font/asta_sans.ttf` 및 앱 이미지 리소스는 공개 전 공개 범위·재배포 권리 확인 후보입니다. 로컬 IDE 실행 구성 `.run/app_Pleos.run.xml`과 Gradle Wrapper JAR는 앱 재현에 쓰이는 추적 파일입니다.
- 이 검사는 패턴과 현재 파일 중심입니다. 바이너리 내 의미·라이선스 및 이전 Git 이력 전체의 개인정보·자격 증명은 미검증입니다.

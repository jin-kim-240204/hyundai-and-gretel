# 통합 도로 쪽지 고도화

초기 HEAD `6fc61ffd5782fc6f7320cdf3429e131b88186ebb` · `codex/harden-integrated-road-notes`. UI `7d72b3a`는 조상이며 TTS 최종 `08838e3`의 stable patch-id는 초기 HEAD와 같다.

- ENV-01 / DOC-3240d46da07b#콘솔포트 · 공식 Virtual Console의 최소 v2.1.0과 호스트 8088 전달 요건을 런북에 반영한다. 기기 쪽 루프백과 브라우저 경로를 별도로 진단한다.
- SAF-01 / AUDIO-02 / INT-01 · 기존 주행 제한과 명시 ACK, AUDIO 정책 및 외부 Gleo 경계를 보존한다. JVM에서 합성 시나리오의 수신과 확인 흐름을 검증한다.
- REL-01 / REL-02 · 공모전 제출 안내의 실행 가능한 release 및 README 항목을 기준으로 새 설치 경로와 산출물의 상태를 기록한다.
- R1 좌표 생성·투영의 공식을 한곳에서 유지한다. 방향 ±1, 수신 거리, Room 구독, 실위치 저장 제한은 기존 정책을 사용한다.

## 변경과 근거

- `SyntheticRoute.fixAt`이 R1 좌표를 만들고 같은 상수·거리 공식으로 투영한다. 종점은 경도 상한을 투영해 산출하며 양 끝점, 610m, 양방향과 수신시각을 JVM에서 검사했다.
- `RoadNotesDemoFlowTest`가 합성 A 제보→B 접근, 수신 커밋 알림 1회, 명시 ACK, 시연 리셋을 통과했다. 동일 셀 집합에서 위치가 변하고 Room을 대신한 Fake Flow를 관측하는 동안 구독 수가 유지되는지 검사했다. 실제 Room·CarUxRestrictions·TTS SDK는 별도 확인 범위다.
- `scripts/pleos_demo_setup.py`는 기본 출력 전용으로 두었다. `--check`만 읽기 전용 진단을 실행하며 `--setup-commands`는 설치·포트 전달을 출력한다. 사용자 번호는 현재 운전자에서 얻으며, 호스트 전달은 해당 serial을 확인한다. 포트 재바인딩 방지 옵션을 사용한다.
- 공유 `.run/app_Pleos.run.xml`의 모듈·기본 Activity·저장소 초기화 비활성을 XML로 확인했다. IDE import와 설치 사용자 동작은 미검증이다.

## 검사 결과

| 명령 | 결과 | 증거 |
|---|---|---|
| `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease` | PASS | 초기 HEAD와 변경 후 모두 성공. 변경 후 XML `app/build/test-results/testDebugUnitTest/TEST-*.xml` 133개 테스트, 실패·오류·스킵 0. 로컬 캐시 잠금 쓰기가 기본 샌드박스에서 거부되어 비기기 Gradle 명령만 승인된 접근으로 재실행 |
| `python3 scripts/verify_harness.py --build` | PASS | 정적 Manifest·133개 원문·27개 규칙, assembleDebug 및 lintDebug |
| `python3 -m unittest discover -s scripts -v` | PASS | Fake runner 5개. 기본 출력·명령 생성 시 runner 미호출, 사용자 23, 설치·권한·serial 불일치 및 오류 경로 확인 |
| `python3 scripts/pleos_demo_setup.py --serial emulator-5554` | PASS | dry-run의 동적 현재 사용자 명령 출력 |
| `git diff --check` | PASS | 공백 오류 없음 |
| `python3 scripts/verify_harness.py --report docs/work/integrated-hardening.json` | FAIL | 보고서 구조를 통과하고 제출 차단 항목 때문에 종료코드 1 |

Gradle에서 기존 H3 configuration-time resolution 및 JVM native-access 경고와 Gradle 10 호환성 경고가 남았다. 빌드 산출물은 로컬 출력 디렉터리에 있다.

## 제출 차단과 미검증

- FAIL · `app-release-unsigned.apk`가 생성됐으나 서명·제출 파일·공개 링크가 없다. release의 시연 생성 비활성, 새 설치의 빈 주변 Flow, Gleo 수신기 비노출과 자기 패키지 Callback을 확인했다. [제출 준비 점검표](../RELEASE-READINESS.md) · [공식 공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)
- NOT_TESTED · 에뮬레이터/adb/기기/앱 실행 전부 미실행. 설치·Console 응답·권한·실제 Gleo 음성/Callback·Room 및 위치 SDK 계측·실제 음성/첫 음절·AUDIO 전원·렌더링·주행 규제·최신 Figma 대조 미검증. [시연 런북](../PLEOS-DEMO.md)


## PR #10 독립 리뷰 반영

- 리뷰 기준 `6fc61ff..2d0c50c` · 검토 커밋 `2d0c50cc43778e2f9b27a9e2976243788738b3a3`. 독립 검토에서 신규 확정 코드 결함 없음, Python fake runner 5개와 diff 검사 PASS. [원격 리뷰](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/10#pullrequestreview-5895737346)
- ENV-01 · [시연 런북](../PLEOS-DEMO.md)에 공유 구성의 범위와 사용자 0/10 환경의 제약을 명시했다. CLI로 현재 사용자 설치를 확인하고 필요하면 수동 설치 후 재진단하는 절차를 보완했다. IDE 구성 로드·실제 설치 사용자·Activity 실행은 NOT_TESTED.
- `git diff --check` 및 `python3 scripts/verify_harness.py` PASS. 보고서 검사는 구조를 통과했고 기존 제출 차단 항목으로 종료코드 1(FAIL)을 유지했다.

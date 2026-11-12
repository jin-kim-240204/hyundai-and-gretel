# hyundai-and-gretel

도로 상황을 쪽지로 남기고 같은 경로에서 접근하는 사용자에게 전달하는 Pleos Connect Android 프로토타입이다. 현재 브랜치는 위치 수집·45초 메모리 버퍼·Room 원본/수신이력·H3 후보·SDK proximity·명시 확인 ACK·debug A/B 시연 도구를 단계별 스펙으로 구현한다. Gleo 연결/서버는 별도 범위다.

[전체 계획](docs/specs/road-notes/00-overview.md) · [공통 정본](docs/plans/implementation-plan.md) · [시연 안내](docs/LOCATION-DEMO.md) · [07 검증 기록](docs/work/step07-integration-verification.md).

## 빌드와 허용 검사

JDK21, compileSdk35/대상34·최소28에 맞는 설치 Android SDK 및 SDK Command-line Tools의 apkanalyzer가 필요하다. JAVA_HOME/ANDROID_HOME은 로컬 설치 경로로 지정한다. 비밀정보나 local.properties를 커밋하지 않는다. 자세한 초기 구성은 [SETUP](docs/SETUP.md)을 참고한다.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lintDebug --console=plain
python3 scripts/verify_harness.py --build
python3 scripts/test_road_notes_artifacts.py
python3 scripts/verify_road_notes_artifacts.py
```

마지막 검사는 source/양 variant/패키징 Manifest의 MainActivity DO 선언, release 시연 코드·권한 제외, schema/복합키/FK·H3 ABI 패키징을 정적으로 확인하고 APK SHA256을 출력한다. 실제 앱을 실행하지 않는다. 정확한 커밋의 결과는 원래 polling dirty가 없는 detached clean worktree에서 생성한다. [07 §5](docs/specs/road-notes/07-integration-and-demo.md#5-정확한-커밋-head-검증-계획--실행은-승인-후)에 절차가 있다.

## 현재 검증 경계

MainActivity DO 선언은 주행 중 화면 사용을 위한 최소 보완이며 기존 CarUx 기능 제한을 유지한다. host/Fake와 빌드·정적 검사 통과는 실제 SDK·Room·Android JNI·CarUx·UI·FGS·주행 성공이 아니다. 앱/Studio/adb/기기/에뮬레이터/계측은 현재 실행 금지이며 실제 시연 절차는 작성만 했다. 전체 AC/기능 완료 판정은 보류하고 독립 코드 리뷰를 거친다.

제품 화면과 debug 패널은 분리한다. release에는 시연 패널·mock/웹콘솔 입력이 없으며 기존 제품 수신함/명시 확인은 유지한다. release APK 빌드는 unsigned이고 서명된 제출 APK·배포·설치 검증 완료가 아니다. 화면/음성 자동 ACK·Gleo/TTS는 추가하지 않는다.

AI 작업은 [AGENTS.md](AGENTS.md)에서 시작한다. 공유 스킬은 [.agents/skills](.agents/skills)에 있다. 작업별 PASS/FAIL/NOT_TESTED를 남기고 기존 사용자 변경을 보존한다.

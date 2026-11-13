# hyundai-and-gretel

AI 작업은 [AGENTS.md](AGENTS.md)에서 시작한다. Codex/Claude 공유 스킬은 [.agents/skills](.agents/skills)에 있으며, [문서 지도](.agents/skills/pleos-connect/references/source-map.md)에서 관련 공식 규정을 선택한다. 구현 후 `python3 scripts/verify_harness.py`와 작업별 검증 기록을 갱신한다.

Pleos Connect 차량용 도로 제보 앱. 목격 직후 Gleo로 제보하고 주변 도로 차량에 공유하는 [서비스 설계](docs/plans/implementation-plan.md)를 검토 중이다.

현재 Sprint 1의 입력 검증·미게시 초안 암호화 저장·동의/목록/삭제 UI를 연결했다. debug에서는 Gleo 외부 테스트 입력을 받아 저장 없이 검증/Callback과 실시간 JSON 화면·GleoProbe 로그를 제공한다. release 외부 수신은 비활성이고, 위치 지정·게시·차량 간 공유는 후속 스프린트다. [담당별 실행 명세](docs/plans/sprint-1-gleo-drafts.md)와 [자비스 검증 기록](docs/work/sprint-1-jarvis.md)을 참고한다.

현재 코드의 책임과 실행 흐름은 [Gleo 구조 정리 기록](docs/work/gleo-architecture-refactor.md)을 참고한다. `MainActivity`는 화면 선택, `DraftRoute`는 상태 수집, `DraftsViewModel`은 화면 상태, `DraftRepository`는 초안 데이터 접근을 담당한다. Hilt가 저장소·처리기·ViewModel 의존성을 연결한다. Gleo의 입력 파싱은 `GleoActionParser`, 응답 직렬화는 `GleoCallback`, Android 수신·전송은 `GleoActionReceiver`, 메모리 기록과 테스트 화면은 `GleoProbe` / `GleoProbeScreen`으로 나뉜다.

자세한 구성과 템플릿 적용 방식은 [docs/SETUP.md](docs/SETUP.md)를 참고한다.

수신 쪽지 TTS는 [구현 계획](docs/plans/gleo-tts.md)과 [검증 기록](docs/work/gleo-tts-implementation.md)을 참고한다. Gleo 입력의 `content`는 사실·부정·불확실성을 보존한 완결 문장으로 요청하고, 새 수신 저장 성공 시 분류별 접두사를 붙여 Pleos TTS 3.3.1 `ON_DEVICE`/한국어로 읽는다. 새 필드·수신 시 LLM 호출·마이크 권한은 추가하지 않는다. 기존 합성 A/B·Room 시연 범위이며 실제 차량 간 공유 기능은 아니다.

실행은 Pleos Speech Service가 있는 에뮬레이터/차량이 필요하다. 앱 설치 시 `TTS_SERVICE`, `READ_CAR_POWER_POLICY` 권한과 AUDIO 전원 정책 연결을 확인하고 기존 위치 권한을 허용한다. A 제보→B 접근 수신 시 안내하며, 중복·재진입·과거 수신함 로드는 재생하지 않는다. Gleo 문장형 생성 지침은 설치만으로 적용을 보장하지 않으므로 스키마 갱신/실제 음성 제보를 별도로 확인해야 한다. SDK onDone은 실제 청취/주행 적합성 통과 증거와 구분한다.

Debug의 `웹콘솔 GPS` 시연은 일반 위치 권한 외에 **mock GPS AppOp**가 필요하다. 앱 삭제·재설치 후에는 이 설정도 확인한다. 시연 도구에 “mock 위치 권한이 없습니다”가 표시되면 개발 에뮬레이터에서 다음처럼 현재 운전자 사용자를 확인하고 권한을 설정한다(아래 `10`은 실제 확인된 사용자 ID로 바꾼다).

```sh
adb shell am get-current-user
adb shell cmd appops set --user 10 com.jinkim.hyundaiAndGretel android:mock_location allow
adb shell cmd appops get --user 10 com.jinkim.hyundaiAndGretel android:mock_location
```

그다음 시연 도구의 `제보자 시작 위치 설정`을 다시 누른다. `READY` / `실제 SDK 출발점 확인`까지 확인한다. 새 설치라면 먼저 `새 시연 초기화`가 필요하며 현재 시연 데이터가 삭제되므로 확인 후 실행한다. 이 AppOp 설정은 가상 GPS를 쓰는 debug 에뮬레이터 시연용이며 실차 실행 절차가 아니다.

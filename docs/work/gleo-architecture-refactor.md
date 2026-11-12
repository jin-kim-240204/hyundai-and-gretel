# Gleo 기능 브랜치 구조 정리

2026-11-11. 기준 브랜치 `gm/feat-speech-to-text`, 시작 커밋 `38338f0`. 요청: f528aca 설계 스킬을 현재 기능에 적용해 흐름·파일 의미·책임 분리를 개선한다.

## 범위와 보존 계약

- MainActivity의 상태·저장 책임을 Draft Route/ViewModel/Repository로 분리하고 Hilt로 주입한다. 단일 모듈과 기존 기능 패키지를 유지한다.
- Gleo의 수신/처리/응답과 debug 관측/화면을 구분한다. JSON 입력 3개, enum·길이·타입 검증, unknown key 무시, success/fail 단발 응답, 5초 내부 제한과 PendingResult 종료를 유지한다.
- 기존 DraftStore는 Fake 및 파일/Android 저장 구현이 실제로 사용하므로 보존한다. 암호화 키 별칭·파일명·바이너리 버전·7일 만료·100건 제한·동의 철회 삭제·원자 교체는 유지한다. Room/DataStore 마이그레이션, 네트워크/Navigation, UseCase/Domain은 추가하지 않는다.
- 영향 범위: Activity·Application/Manifest·DI/Gradle, 초안 화면/미리보기/ViewModel/테스트, Gleo parser/callback/receiver/probe, 저장소 가독성과 관련 테스트·계약 문서. 화면 구성과 문자열 의미는 유지한다.
- debug 외부 수신/저장 없음, release 외부 비활성/동의 후 내부 저장을 보존한다. Hilt로 주입해도 debug 저장 호출은 금지한다. 커밋·푸시는 이번 요청에 포함하지 않는다.

## 적용 규칙과 원문

- ENV-02: ComponentActivity/Compose 구조 유지. Car App Library로 전환하지 않는다.
- INT-01 / DOC-24357cf5e59a#입력계약: App Schema 2.1/4의 string/enum/required 계약. 스키마를 바꾸지 않고 파서 타입·길이·허용값·미지정 키 무시 회귀 검증.
- DOC-c6fc94252d66#응답기한: Callback 2.1/2.2/4. functionName 일치, 영어 message, data는 JSON 객체 배열 문자열, 공식 7초 전 응답. 내부 5초 제한·실패 단발 응답·전송 요청과 실제 수신 구분 유지.
- DOC-f1d260bfb503#브로드캐스트: ACTION_PARAM으로 수신, APP_ACTION_RESPONSE에 packageName/response 제공. 수신과 callback 직렬화 경계를 나누고 계측 fixture 컴파일/가능한 runtime으로 확인.
- DOC-e68476b79d1c#생명주기: 포그라운드 복귀 상태 복원과 불필요한 백그라운드 수집 중단. Route에서 Lifecycle 기반 수집/복귀 새로고침, ViewModel scope 및 Receiver finally 종료 확인.
- PRIV-01 / DOC-72a5d7ab4ebb#최소노출: 수집·저장·전송 최소화와 동의/보안. 기존 암호화·동의 보존, debug 진단 화면은 사용자 요청한 원문 확인 기능을 유지하되 logcat 원문 출력은 민감정보 노출 방지를 위해 메타데이터로 제한한다.
- 공식 URL은 pleos-connect의 source-map 및 위 ID의 sources 원문을 사용. 2026-11-11 App Schema·Callback·앱 구조·개인정보 원문을 재확인한다. 프로젝트의 단발 계약은 공식 asking 지원과 구분한다.
- 프로젝트 설계 스킬 3~9절: 최소 MVVM/Repository, Hilt 생성자 주입, 불변 UiState/StateFlow.update, Route/Screen, 취소 전파, 동작 테스트.
- Hilt/KSP는 AGP 9.5의 내장 Kotlin과 호환되는 버전을 공식 릴리스 근거로 선택한다. 기존 캐시의 Hilt 2.57을 그대로 사용하지 않는다.

## 검증 계획

- 변경 전 기존 단위 테스트, 변경 후 기존 저장·검증 테스트와 신규 ViewModel 로딩/성공/실패/취소·동의 철회 테스트.
- verify_harness --build, debug/release Kotlin 및 AndroidTest 컴파일, Studio build_project, diff 공백 검사.
- 가능한 에뮬레이터/계측 검증을 확인한다. 사용자 실제 음성 호출은 자동 실행하지 않는다. 화면·Gleo E2E·실차 확인 불가 항목은 NOT_TESTED로 남긴다.
- PASS/FAIL/NOT_TESTED/N/A 및 실제 명령·결과는 아래에 기록한다.

## 최종 구조

```text
MainActivity                      debug/release 화면 선택, Hilt 진입점
  ├─ GleoProbeRoute → Screen       debug 진단 메모리 StateFlow 수집·렌더링
  └─ DraftRoute → DraftScreen      Lifecycle 수집·복귀 갱신·UiState 렌더링
       └─ DraftsViewModel          로딩·성공·실패·중복 요청 관리
            └─ DraftRepository    데이터 접근·변경 후 snapshot·Result 실패 반환
                 └─ DraftStore    기존 저장 인터페이스, Fake와 Android 구현 연결
                      └─ LocalDraftStore → DraftFileStore + DraftCipher

GleoActionReceiver                수신 → 기한 내 처리 → Callback → finally 종료
  ├─ GleoActionParser             JSON 해석·필수값/타입/길이/enum 검증
  ├─ GleoActionProcessor          release 동의 확인·저장, debug preview 함수
  ├─ GleoCallback                 GleoResponse → Callback JSON 직렬화
  └─ GleoProbe                    메모리 진단 기록, 개인정보 없는 logcat
```

- `RoadNoteContract.kt`는 실제 모델 `RoadNote.kt`와 기존 경계 `DraftStore.kt`로 나눴다. `GleoJson.kt`의 입력은 Parser, 출력은 Callback으로 옮겼다. 함수·모델의 기존 package와 외부 계약은 보존했다.
- `GleoProbe`는 debug 관측 버퍼이며 업무 UI 상태와 구분한다. 불필요한 ProbeRepository/ViewModel 위임 계층 없이 Route에서 읽기 전용 StateFlow를 수집한다. 초안 업무 UI는 ViewModel → Repository다.
- 저장 파일 read/write에서 초안 1건의 codec 함수를 추출했으며 필드 순서와 버전은 유지했다. 키 생성은 Early Return으로 정리했다. 실제 용도가 있는 DraftStore 외 신규 Interface/Mapper/Base/UseCase는 추가하지 않았다.
- Hilt 2.59.2 / KSP 2.3.6 사용. 공식 근거: https://github.com/google/dagger/releases/tag/dagger-2.59.2 , https://github.com/google/ksp/releases/tag/2.3.6 , https://developer.android.com/build/migrate-to-built-in-kotlin . AGP/기존 SDK 버전은 변경하지 않았다.

## 검증 결과

- PASS: 변경 전 `:app:testDebugUnitTest`. 변경 후 총 34개 테스트, failures=0/errors=0/skipped=0. 기존 27개 유지 + ViewModel/Repository 7개 추가. 로딩·중복 방지·미동의 조회 생략·실패 시 기존 내용 유지·재시도·철회 삭제·개별 삭제·취소 전파/해제를 확인했다.
- PASS: 최종 `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain` 성공. Release는 기존 서명 설정을 변경하지 않은 unsigned 빌드이며 제출용 실행 검증 완료를 뜻하지 않는다.
- PASS: Studio `build_project` isSuccess=true, problems=[]로 최종 확인.
- lint 범위: 오류 0개, 경고 40개. 버전/target 권장 갱신 외 `ObsoleteLintCustomCheck`로 Lifecycle 2.9.1의 `UnsafeLifecycleWhenUsage`/`RepeatOnLifecycleWrongUsage` 검사가 현재 lint API와 호환되지 않아 생략됐다. 라이브러리 일괄 업그레이드는 이번 구조 정리에서 하지 않았다. Route의 Lifecycle 연결은 코드 검토와 취소 테스트로 확인했으며 자동 lint 전체 검사 완료로 주장하지 않는다.
- PASS: Pleos_Connect_v2.1.0 / Android 34 / arm64 / 2560×1440 / 160dpi 읽기 전용 AVD에서 debug·계측 APK 설치, `am instrument -w -e phase storage` 성공. Keystore/GCM·JSON 파싱/콜백 직렬화·재조회/삭제/철회·Activity 기동 및 Hilt ViewModel/Repository/Store 그래프 확인. 격리 캐시 파일을 사용하고 종료 시 삭제한다. 합성 Gleo Broadcast와 음성 호출은 실행하지 않았다.
- 수정 후 PASS: 계측 코드에서 startActivitySync의 반환 타입 Activity를 ViewModelStoreOwner로 사용해 컴파일 오류 발생. 실제 MainActivity 타입으로 명시한 뒤 계측 APK 재빌드와 실행으로 해결했다. 같은 실패를 수정 없이 반복하지 않았다.
- PASS: 코드 검토에서 JSON/schema 필드·저장 형식/암호키·exported·callback 목적지·5초 제한·finally 종료가 보존됨을 비교했다. UI 배치·Theme·버튼 의미는 유지했다. debug 원문 logcat 제거에 맞춰 활성 연동/디버깅 문서를 갱신했다.
- N/A: 차량 API/권한·위치·게시·새 화면·Room/DataStore/Retrofit/Navigation 도입, 커밋/푸시, 제출 서명 변경.
- NOT_TESTED: 실제 Gleo 음성 → 앱 → 응답 수신/안내, timeout/전송 실패 E2E, release 설치·화면 복귀, 실차 및 모든 폼팩터의 시각/물리 규격. 자동·격리 테스트와 실제 연동 검증 완료를 구분한다.
- PASS: `verify_harness.py --build --report docs/work/gleo-architecture-refactor.json`의 하네스와 빌드 성공. 보고서 8개 항목은 위 Gleo 실제 연동 NOT_TESTED를 보존해 exit=2(미검증 포함)다. FAIL 항목은 없다. `git diff --check` 및 README/작업 기록 로컬 링크·코드 블록 검사도 통과했다.

## 2026-11-12 커밋·푸시 요청

- 사용자 요청으로 현재 기능 브랜치의 리팩터링·Hilt·한국어 주석·테스트·관련 기록(숫자 정책 조사 포함)을 함께 커밋하고 origin의 같은 브랜치로 푸시한다. 기존 미검증/리뷰 지적 사항은 그대로 기록하며 기능 검증 완료로 표현하지 않는다.
- PASS: 커밋 전 `:app:testDebugUnitTest` 34개, failures=0/errors=0/skipped=0. `:app:assembleRelease`와 `:app:compileDebugAndroidTestKotlin` 성공. Release는 기존 unsigned 산출물이다.
- PASS: `verify_harness.py --build --report docs/work/gleo-architecture-refactor.json` 하네스·assembleDebug/lintDebug 성공. 보고서는 실제 Gleo 미검증 항목 때문에 exit=2 유지. 한국어 주석 보고서 검사는 exit=0.
- PASS: 변경 파일을 개별 확인해 소스/설정/문서만 포함하고 APK·로컬 환경 설정·서명키를 제외한다. 알려진 토큰/비밀키 패턴 검사와 diff 공백 검사 통과. 기존 로그·Route 테스트·협업 명세 리뷰 지적은 미수정 상태로 포함한다.
- N/A: 새 기능 수정·리뷰 지적 해결·새 음성/실차 테스트·APK 배포. 커밋 해시와 최종 원격 반영 여부는 실행 후 사용자에게 보고한다.

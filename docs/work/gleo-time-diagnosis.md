# Gleo observedAt 과거 날짜 진단

2026-11-09. 요청: observedAt이 2023년인 원인 확인 및 에뮬레이터 시간 설정 안내. 앱 코드/시간 설정값 변경 없음. 날짜·시간 설정 화면만 연다.
INT-01: DOC-24357cf5e59a#시각입력 / DOC-c6fc94252d66#실패응답. 기존 스키마는 LLM에게 현재 시각에서 상대시간을 빼도록 요구한다. 앱은 생성된 값을 검증할 뿐 현재 시각을 Gleo에 주입하지 않는다. 공식 원문 스냅샷은 같은 해시의 references/sources 문서, URL은 Source 항목. Android 시간 설정 공식 근거: https://source.android.com/docs/automotive/time/automatic_time_detection

PASS 근거 (emulator-5554):
- 호스트 UTC 조회 2026-11-09T08:43:32Z, 에뮬레이터 UTC 조회 2026-11-09T08:43:06Z. 병행 조회 시간 차이가 있으므로 정확한 초 단위 오차로 해석하지 않는다.
- auto_time=1, auto_time_zone=1, persist.sys.timezone=America/Los_Angeles.
- dumpsys time_detector: systemClockMillis=2026-11-09T08:43:06.217Z, network suggestion 2026-11-09T07:53:23.737Z, systemClockConfidence=100. 자동 시각 공급 기록 정상.
- logcat 11-09 00:37:54.872 DICTATION_SERVER: “10초 전에 구덩이를 발견했거든? 제보해줘.” 인식 정상.
- 00:37:56.432 ami-sdk ACTIONPLANNER OUTPUT에서 observedAt=2023-10-03T15:23:50Z 이미 생성. refineToolOutput 및 sendActionCall에도 동일. 앱 GleoProbe INPUT_JSON 동일.
- 앱 CALLBACK_JSON은 STALE_CURRENT_ROAD. 손상 JSON이 원인이 아님. 사용자 붙여넣기 내용과 달리 실제 JSON은 문법 유효하며 content=구덩이.
- DATE_SETTINGS resolve: com.android.car.settings/.common.CarSettingActivities$DatetimeSettingsActivity. adb shell am start -a android.settings.DATE_SETTINGS 로 화면 열기.

판정: 에뮬레이터 시스템의 연도 오류가 아니라 Gleo 함수 인자 생성 단계의 잘못된 절대시각이다. LLM에 실제 시각이 제공되지 않았는지/내부 고정 문맥이 있는지는 Gleo 내부 프롬프트·구현을 확인할 수 없어 NOT_TESTED. 시간대 차이는 수년 오차를 설명하지 않는다.
권장 후속: LLM에서 경과 초만 추출하고 앱의 수신 UTC 시각에서 차감해 observedAt 생성. 현재 시각을 LLM에 제공하는 Gleo 공식 기능 유무는 미확인. 이번 진단에서 기능 변경 없음.

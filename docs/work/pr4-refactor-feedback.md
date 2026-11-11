# PR4 리팩터링 피드백

범위: 4541836246/6255/6264 및4541890376/0388 반영. pleos-connect/clean-code-review/android-compose-architecture/ponytail 기준으로 현재 코드·호출부·테스트 대조. ENV-02/VEH-03/PRIV-01 및 DOC-9844e9eaad3c#콜백: SDK등록/정리/처리시점 세션검사 유지. 진단은 debug에서 작업명/예외타입만, 원문/좌표/예외메시지 없음. 스키마/정책값/동작 변경 없음. 기존 미커밋 polling/다른기록 보존.

반영: 예상외 저장예외의 타입 진단 콜백, TrackingStatus.isCollecting 공통 계산, offer의 두단조시각 명명인자, 검증정책상수, Activity필수위치권한 판정 공통화. 검증은 단위/빌드/lint/IDE 및 기존 서비스계측 회귀.

보류: 4541836274 연결 구독 분리는 현재 AndroidViewModel/Application/Binder에 결합된 테스트 연결을 변경해야 함. 단순히 긴 테스트 일부만 옮기면 지적된 스케줄러 관찰 한계는 해결되지 않아03-c의 화면 연결 검증 시 함께 처리.4541836293 실제 SDK 오류 수명은03-c 이전 추가 검증으로 남김; 범용 DI나 테스트 전용 프로덕션 API를 이번 작은 리팩터링에 선제 추가하지 않음. 사용자 중지 계측 증거를 SDK오류 PASS로 확대하지 않음. 두 스레드는 미해결 유지.4541836302 구조유지 확인은 동작 보존 후 답글/resolve.

검증 PASS: 단위30개, SDK/Room/서비스 계측10개(5.325초·합성GPS14회), Debug/Release·계측compile·lintDebug·하네스--build/report·Studio build_project·diff검사.500/501 및35,000/35,001 경계 유지. 취소전파/완료콜백 유지. 권한 팝업 거절/설정복귀를 이번에 직접 재실행하지 않음(NOT_TESTED), 판정함수 추출만 했으며 기존 서비스재생성 계측은 회귀 통과.

수정5건+구조유지확인1건 답글/resolve 완료. 테스트구독분리·SDK오류수명보강2건은 미반영이유 답글 및 unresolved유지. 코드/기록은 미커밋, 기존 미커밋 파일 보존.03-c 구현은 이번 범위 밖이다. 이번 계측이 만든 합성 DB와 직접 시작한 에뮬레이터는 종료 전 정리한다.

# 함수별 한국어 설명 주석

요청: 현재 리팩터링 코드의 함수별 역할과 중요한 처리 이유를 한국어로 설명한다.

범위: app/src/main/java의 실행 코드와 Compose Preview 함수. 함수에는 짧은 KDoc, 입력 신뢰 경계·Callback JSON 형식·동의·암호화·원자 저장·취소에는 필요한 이유를 추가한다. 테스트 로직·함수 시그니처·실행문·설정은 변경하지 않는다. 기존 미커밋 리팩터링 및 사용자가 추가한 Duration 표현을 보존한다.

적용: ENV-02 / INT-01. 기존 gleo-architecture-refactor.md에 기록한 DOC-24357cf5e59a#입력계약, DOC-c6fc94252d66#응답기한, DOC-e68476b79d1c#생명주기 요구를 설명한다. 새로운 공식 규정이나 실제 연동 보장을 추가하지 않는다. JSON 파싱 검증·data 배열 문자열·5초 내부 제한과 협력적 취소·PendingResult 종료 의미를 구분한다.

결과:

- PASS: 실행 코드/Preview 함수 68개에 KDoc 추가. 기존 영어 설명도 한국어로 정리했다. 함수 주석 18개 파일과 모델 설명 번역 2개 파일, 총 20개 소스 파일에 주석만 변경했다.
- PASS: 작업 직전 main Kotlin 23개 파일을 스냅샷으로 보관해 문자열 리터럴을 보존하며 주석을 제거한 코드와 비교했다. 실행문·시그니처·기존 Duration 표현이 동일하며, 68개 함수 모두 설명 주석을 갖는지 확인했다.
- PASS: `python3 scripts/verify_harness.py --build`, assembleDebug/lintDebug 성공. 기존 lint 일부 Lifecycle 검사 생략/경고는 이전 리팩터링 기록의 한계가 그대로 적용된다.
- PASS: Studio build_project isSuccess=true, problems=[]. `git diff --check` 통과.
- N/A: 실행 로직·화면 배치·의존성·설정 변경, 새 테스트 추가, 음성/실차·Release 재검증. 주석 범위 완료이며 기존 기능의 NOT_TESTED 상태를 바꾸지 않는다. 커밋/푸시는 하지 않았다.

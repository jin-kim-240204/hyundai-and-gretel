# 디버그 패널 밝은 테마

사용자 요청: 백드롭 검정70%, 패널 흰색/본문 검정, 모달 외부32dp, 가상 주행 경로로 문구 변경. pleos-design의 밝은 배경 대비·터치 영역 유지 적용. 활성 본문 검정, 보조595959, 카드F5F5F5, 선택DDDDDD. 경로의 현재 위치 파랑/쪽지 진한 빨강으로 밝은 배경에서 구분. 기능/규제/권한/데이터 변경 없음.

PASS: assembleDebug/lintDebug, Studio build, harness, diff --check. 기기 업데이트/Activity 실행 확인. NOT_TESTED: 실제 모달 시각 검토/다중 해상도. 새 동작 없이 스타일·문구만 변경하여 별도 단위 테스트 추가 안 함. 커밋/푸시는 요청 전 미실행.

## 여백 재조정

실제 Studio Running Devices에서 모달이 검정 backdrop 가운데 maxWidth780dp로 표시되며 바깥 공간이 있는 것을 확인. 사용자 피드백에 따라 외부 최소48dp, 내부 본문 좌우/상하32dp, 헤더 시작32dp/상16dp, 하단24dp로 확대. 빌드/lint/harness PASS. 수정 APK 설치/실행 PASS. 수정 후 열린 모달 시각 검토는 NOT_TESTED. 기존 기능/터치 처리 유지.

## 커밋 및 병합 요청 (2026-11-15)

사용자 요청으로 UI 3개 파일과 이 기록을 `codex/harden-integrated-road-notes`에 커밋·푸시한 뒤 `codex/contest-submission-prep`에 병합한다. 다른 미추적 작업 문서는 제외한다. 직전 병합 검사에서 동일 UI 변경을 포함한 작업 트리의 assembleDebug/lintDebug, harness, testDebugUnitTest, 스키마 검사가 PASS였다. 시각 검토는 NOT_TESTED를 유지한다. 공식 디자인 적합성이나 release 준비 완료를 이번 Git 작업으로 판정하지 않는다.

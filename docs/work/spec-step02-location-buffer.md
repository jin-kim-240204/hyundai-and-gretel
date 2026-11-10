# 02 위치 버퍼 스펙 초안

요청:01단계 구현 피드백 반영 완료 후 다음 단계 스펙만 작성. ny/spec-driven-road-notes HEAD fd3140a와 현재 LocationTracking/Service의 좁은 Binder 계약 확인. ae11722 참고 버퍼는 정책 조사 자료로 읽고 코드 복사/재실험하지 않음.

범위:02-location-buffer.md와00진행상태, 이 기록/보고. 공통 정본 §5의30초보관/2초공백/측정단조시각·accuracy 가정이 후속 합의와 달라 차이를 명시. 새로운 경계 제안은 리뷰 후 spec-road-note-system에서 먼저 정리하고 동기화해야 함. 부모나 앱 코드 변경 없음.

적용 ENV-02/VEH-03/PRIV-01. DOC-9844e9eaad3c#콜백 원문의 연속 수신·null·마지막위치조회는 신규측정아님을 재사용. DOC-ba9b83fc86f6#오류 원문의 null/error를 유효 표본으로 사용하지 않을 요구를 적용. 보관45초·목표30초·공백/신선도/동률은 SDK 의무가 아닌 프로젝트 제안으로 분리. 메모리 전용·업로드/DB없음. 기존 원문 스냅샷 재확인, 새 API/런타임 보장 조사 없음.

검증 범위: 문서 링크·diff·harness/report, 단계01회귀 요구를 문서로 대조. 앱 빌드/SDK/버퍼 실행 N/A(문서만). 상세 정책은 리뷰 대기, 구현·커밋·푸시 미실행.

결과 PASS: 로컬 링크와 git diff --check, harness --report 5checks exit0. 버퍼 구현/실행 N/A, 정책 사용자 리뷰 대기.

후속 요청: 2026-11-11 사용자가 스펙 커밋·푸시 승인. 문서 검사5개 PASS 재확인. 별도 worktree의 detached HEAD(fd3140a)에서 문서4개만 커밋하고, 같은 HEAD의 실제 ny/spec-driven-road-notes worktree에서 fast-forward한다. 실제 worktree의 SDK/이전 리뷰 기록 미커밋 변경은 포함하지 않는다.

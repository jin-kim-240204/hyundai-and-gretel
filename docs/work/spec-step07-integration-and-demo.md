# 07 최종 통합·검증·시연 준비 명세

요청 범위는07 명세와 관련 상태·작업 기록, 명시 stage/한국어 docs 커밋·push/PR4 독립 스펙 리뷰 요청이다.07 구현·테스트/실행 스크립트 구현은 하지 않는다. 현재 명시 확인 ACK 유지, 자동 노출/음성 ACK·Gleo/TTS 선행 금지. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지가 스킬의 runtime/IDE 검증보다 우선한다.

기준 코드 e8ef1421f6561fa39fde83414c76ad14bd3ee66f, 부모2ca00c0ee411faa731390601619c38747be71279 로컬/원격 일치. 적용 pleos-connect ENV-02/VEH-03/DES-06, 검증·커밋·PR 스킬 및 기존 MVVM/최소 변경 원칙. DOC-1ba06876919b#초기화해제와 DOC-9faba42791f0#기능별규제는 기존01~06 승인 원문/정본의 소유권·해제/기능별 CarUx 계약을 추적하는 기준이며 이번에 SDK API/권한/정책을 새로 도입하지 않았다.

읽은 범위: 00·01~06 원문 AC/요구사항, 공통정본의 시간/저장/수신/시연/규제·확정 계약, lessons-learned, 단계별 구현 기록과06 P2 수정 기록, 현 RoadNotePickupFlowTest/SubmissionSdkFailureTest/TrackingReceiptLifetimeTest. 과거01/02 실제 실행은 당시 커밋/로컬 polling 포함 여부를 구분하고 현 HEAD runtime 결과로 쓰지 않는다.

원격 PR4 OPEN/DRAFT/head e8ef142/base spec-road-note-system 확인. reviewThreads 전체 조회 pageInfo.hasNextPage=false, unresolved는4541836293 하나다. 실제 SDK 오류 이후 수락 저장/마지막 unbind/알림·종료 근거가 남아 있다.06 P2 4545675100/5109 해결 및 재검토 답글4545793636/3786을 직접 읽었으며 사용자 전달의 원 반례1+관련25 JVM PASS와 일치한다. 작성자93개 보고와 별도다. 원격 본문은 초기 문서 전용 설명이 남아 있어07 승인 후 정리 대상으로 명세에 명시했다. 이번에 본문/ready/resolve/merge는 변경하지 않는다.

작성: 07-integration-and-demo.md에01~06 AC/미검증 인벤토리, 제품/패널 분리, 전체 Mermaid·보존/실패/수명, clean detached HEAD 검증 계획, 기존 production/Fake 재사용 host I01~07, 별도 실행 허용 뒤 runtime R01~07 및 비실행 시연 대본, 최종 문서/PR 정리·종료 조건을 연결했다.00/06의 현재 상태만 독립 재검토 통과/runtime 대기/07 명세 리뷰로 갱신했다. 부모 정책 변경은 없어 부모 파일/브랜치를 수정하지 않는다.

원래 dirty25개 파일의 내용 SHA256 목록은 /private/tmp/step07-original-dirty-hashes.json에 기록했다. polling/피드백/미추적 리뷰·wireframe·실행기록을 보존하고 이번 파일만 stage한다.07 계획은 원래 worktree를 고치지 않고 별도 지정 SHA worktree에서 검증하도록 하며, 그 명령/테스트/시연은 이번에 실행하지 않았다.

검사 결과는 아래에 갱신한다. 현재 코드 기준의 runtime·새 clean HEAD 자동 검증은 NOT_TESTED이며 문서 정적 검사 통과를06/07 기능 전체 PASS로 승격하지 않는다. 독립 스펙 리뷰와 구현 승인 후에만07 구현에 착수한다.


최종 정적 검사:
- PASS: `python3 scripts/verify_harness.py --report docs/work/spec-step07-integration-and-demo.json`의 진입점·133 원문·27 규칙·정적 Manifest. 보고서 exit2는 실제 SDK/UI 등 NOT_TESTED를 보존한 결과다.
- PASS: 변경 Markdown의 로컬 링크 대상/코드 fence 짝·JSON 파싱·git diff --check. 원래 dirty25개 파일의 해시 비교 동일. 이번 stage는00/06 상태·07 명세·이 작업 기록/JSON의 문서5개뿐이다.
- N/A: 새 JVM/빌드/lint/계측 compile — 문서만 수정해 이번에는 실행하지 않았다.06 이전 결과를 새로운07 검증 PASS로 쓰지 않는다.
- NOT_TESTED: 새 clean HEAD 빌드와 host 통합·모든 runtime R01~07. 검증 worktree/실행 스크립트를 생성·실행하지 않았다. 명세 내부 shell/adb 초안은 텍스트이며 실제 실행0회다.

다음 행동: docs 커밋/push 후 고정 명세 SHA로 PR4에 AI 스펙 작성자 리뷰 요청을 게시하고 대기한다. 독립 스펙 리뷰 전07 구현은 금지다.


07 사용자 추가 범위로 주행 중 제품 화면 사용/사라짐 문제 해결과 MainActivity DO 계획을 명세에 포함했다. 기존 ‘정책 변경 없음/DO 추가 없음’ 문장은 이전 요청 당시 기록이며 이 DO 범위는 부모0f0a7da 동기화와 [추가 작업 기록](spec-step07-driving-display.md)으로 대체한다. 코드/기기 실행 전 독립 스펙 리뷰 원칙 및 명시 ACK/Gleo 비범위는 유지한다.

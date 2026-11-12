# 07 추가 범위: 주행 중 제품 화면 사용

사용자 최신 지시를 반영해 주행 중 앱 사용/속도 증가 때 화면이 사라지는 문제를07에 포함했다. 이번 요청에서는 명세/공통 정본만 변경한다. 앱 Manifest·Activity·테스트·스크립트는 독립 스펙 리뷰 전 변경하지 않는다. 앱/Studio/adb/기기/에뮬레이터/계측 실행 금지와 현재 명시 확인 ACK/Gleo 비범위는 유지한다.

적용 pleos-connect/pleos-design/pleos-vehicle 및 commit/pr, ENV-02/VEH-03/DES-06. DOC-6386c5462132#DO미선언Drive: ‘주행 상황에 따른 앱 사용 여부’의 DO 미선언 Parking/Drive 차이를2026-11-12 원문으로 재확인했다. DOC-9faba42791f0#DO선언후기능제한: CarUx 연동 절의 DO 이후 ActiveRestrictions/기능별 적용 의무를 다시 읽었다. 기존2026-11-01 스냅샷과 핵심 요건은 같다. AAOS Activity metadata 공식 원문 https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en 은 올바른 activity 자식 위치를 확인하는 보조 근거다. 새 Pleos 원문은 추가하지 않았으며 기존 source-map의6386/9faba를 재사용한다.

git fetch origin 실행 후 b61ffd71fadc4b0d88625de85f664cdaf4e53565(작성자 GyeongminKimGyeongminKim)의 MainActivity metadata 추가 diff,819df06fbd7508ecdc4b75691f5143d3db0999da의 full UI 복원/DO 유지 파일을 읽었다. 두 커밋 포함 원격 브랜치는 origin/gm/feat-speech-to-text뿐이며 origin/main/현 feature에는 없다. 전체 gm merge/cherry-pick이나 UI/규제 모니터/Gleo 코드 복사는 하지 않았다.

부모 기존 worktree /private/tmp/hyundai-system-spec가 clean임을 확인한 뒤 정본의 DO 미선언 미래 제약만07 MainActivity DO 계획으로 대체했다. 정본과 부모 기록2파일을 별도 docs 커밋0f0a7daa1189bcdba62cd27a58033a8659b43737로 push하고 기능 브랜치에 병합(f35d21a, 충돌 없음)했다. 부모는 앱 코드를 포함하지 않는다. 새 정책은 사용자 추가 요구의 명세이며07 독립 리뷰 전 구현하지 않는다.

기능 문서00/06/07을 동기화했다.07 §3.1에 공식 의무/프로젝트 최소 변경/역할 분리, 올바른 기존 MainActivity activity 자식·단일 Boolean true·debug/release/패키징 Manifest 일치 검사를 명시했다. 부재/false/잘못된 부모/debug 전용 반례를 검사해야 하며 기존 DO 없음 회귀를 단순 삭제하지 않는다. 기존 제품 UI를 축소 UI로 치환하지 않고 CarUx NO_SETUP/문자·깊이·항목·본문/NO_VIDEO, 중지·취소·닫기 및 수락 쓰기/명시 ACK를 유지한다. DO·KEEP_SCREEN_ON·FGS·기능 제한은 별도 책임이다. R07의 P→D/속도 증가·정차·재진입/재생성/홈 복귀 시 실제 화면 유지 검증은 NOT_TESTED로 명시했다.

현재 코드의 DO 미선언은 사실이며 앱 파일을 바꾸지 않았다. 새 주행 UI/DO/심사 적합성을 PASS로 보고하지 않는다. 코드 선행/Gleo·자동 ACK/시연 실행 없음. 원래 dirty는 기존 /private/tmp/step07-original-dirty-hashes.json의25파일 해시로 보존 확인한다. 부모 문서 외 수정은00/06/07과 이 작업/기존07 기록 및 JSON만 선택 stage한다. 독립 리뷰 요청 후 대기한다.

검사: PASS 문서 harness/로컬 Markdown 링크·fence·JSON·diff 검사. Report exit2는 DO/실주행/CarUx NOT_TESTED를 보존한 결과다. 부모 harness도 PASS, 부모 커밋의 파일은 정본/기록 문서2개뿐이다. 원래 dirty25개 해시 동일, app/scripts의 HEAD tree가331d488과 동일하다. origin/main·현재 HEAD의 Manifest DO 부재와 b61ffd7/819df06의 올바른 MainActivity 자식 true를 XML로 정적 대조했다. 새 선언은 문서 예시이며 현재 구현 PASS가 아니다. N/A 새 빌드/JVM/lint·계측 compile(문서만 변경), NOT_TESTED 실제 DO/Drive 화면 유지·SDK/UI/서비스/CarUx. 금지된 실행은0회다.

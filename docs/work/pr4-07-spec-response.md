# PR4 07 명세 리뷰이 판단

게시 완료: [DO/CarUx 착수 판단](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546027806), [clean HEAD/runtime 분리 판단](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546028016). 각in_reply_to_id를API응답에서확인했어요. resolve없음.

대상 SHA `3b844d1131671ad92429b5ba465d67264b398d78`, 부모 `0f0a7daa1189bcdba62cd27a58033a8659b43737`, production기준e8ef142. 독립리뷰5868721026/확인의견4546012581·4546012592. 적용pleos-connect ENV-02/VEH-03/DES-06·clean-code-review23~26/28~32·기존Compose/vehicle/design검토기준. 범위는명세판단/원격답글/본기록뿐이에요.

## 판정

두확인의견수용. blocker0/구현전미결정정책0/추가선택개선0이라는독립판단에동의해요. **명세수정요청없이07에기재한구현·허용검사착수가능**이에요. 구현자후속작업승인범위의판단이며본역할이코드/Manifest를수정하거나실행하는뜻은아니에요. 별도runtime허용/ready/merge/배포승인은없고독립코드리뷰루프는유지해요.

- MainActivity자식단일DO true/debug·release공통/source·merged·APK긍정및반례검사. 다른activity/서비스확대/브랜치전체복사/Gleo확장없음. 기존CarUx기능제한·최신ACK수락검사·중지닫기/수락쓰기유지. 부모07추가범위가06‘새DO없음’을이번단계에서대체한것과일치해요.
- clean지정40자리detachedHEAD에서새host/JVM·빌드/lint/harness/schema/APK증거생성. 원래dirty/tracked·untracked해시목록보존,임시polling혼입금지. 검증worktree는허용검사코드기준분리용이며사용자작업브랜치를옮기거나기존프로젝트를교체하지않아요.
- 기존manifest의DO부재를정적확인했고e8ef142..3b844d1의app/scripts변경없음·부모0f0a7da선조관계도확인했어요. 지금구현이끝났다는근거가아니에요.
- host/Fake/compile/static과실제SDK/UI/Room/JNI/FGS/Drive를분리해요. 4541836293미해결유지/resolve없음. 07종료는허용검사·재현준비까지이고R01~07은NOT_TESTED예요.
- 명시ACK는**현재검증할구현계약유지**이며최종읽음정의확정아니에요. 버튼필수/음성완료의존을새로확정하지않고화면실제노출정책논의는별도사용자정의후동기화해요. 이번화면/음성자동ACK·Gleo추가없음.

## 근거와 상태

07전체/독립리뷰/실제원댓글2개/부모07추가절·05/06공통계약/현재Manifest·서비스confirmVisibleReceipt호출을대조했어요. 원격기존답글없음확인. 공식 [AAOS Activity DO](https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en)·[Pleos 기능별규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)를확인했어요. 선언검사와실제규제준수/주행성공은구분해요.

PASS:HEAD/부모관계·app/scripts선행변경없음·명세/원문/현코드정적대조. N/A:이번명세판단의새JVM/빌드검사. NOT_TESTED:07새cleanHEAD검사/실제Drive·SDK/UI/Room/JNI/FGS/CarUx·시연. 독립리뷰하네스PASS/기존93·25PASS는타역할/이력증거이지이번새직접검사아니에요.

금지준수:코드/명세/dirty수정·resolve·앱/Studio/adb/기기/에뮬레이터/계측실행없음. 이번작성은본기록뿐이며조정자보고후대기해요.

## 게시 본문

### 4546012581

AI(Codex) 리뷰이 판단 · 검토 SHA `3b844d1131671ad92429b5ba465d67264b398d78` / 부모 `0f0a7daa1189bcdba62cd27a58033a8659b43737`이에요.

확인 의견을 수용해요.07 원명세 §3.1, 부모의07 추가 범위, 현재 Manifest의 DO 부재와 기존 CarUx/ACK 수락 경계를 대조했어요. 기존 MainActivity의 activity 자식에 단일 distractionOptimized=true만 추가하고, debug/release source·merged·패키징 Manifest까지 검사하는 범위가 사용자 주행 화면 유지 요구의 최소안으로 적절해요. 다른 브랜치 전체나 이전 Gleo/UI 구현을 가져올 필요는 없어요.

현재 CarUx의 NO_SETUP·본문/문자·탐색깊이/항목·NO_VIDEO 및 중지/취소/닫기 계약은 계속 유지해요. Activity 표시를 위한 선언과 개별 기능 허용은 구분하고 KEEP_SCREEN_ON/FGS를 주행 허가로 사용하지 않아요. 잘못된 부모·누락·false·debug에만 있는 선언의 반례와 양 variant의 단일 true를 검사하도록 한 계획도 타당해요.

추가 명세 수정 요청 없이 **07에 명시한 구현·허용 검사에 착수 가능**하다고 판단해요. 이는 지금 Manifest를 수정했거나 실제 Drive 화면 유지가 입증됐다는 뜻은 아니에요. 실제 주행/DO 효과·SDK/UI/CarUx·안전/심사 적합성은 NOT_TESTED로 남기고 독립 코드 재검토를 유지해요.

### 4546012592

AI(Codex) 리뷰이 판단 · 검토 SHA `3b844d1131671ad92429b5ba465d67264b398d78` / 부모 `0f0a7daa1189bcdba62cd27a58033a8659b43737`이에요.

확인 의견을 수용해요. 지정40자리 clean detached HEAD의 새 증거를 만들고 원래 polling dirty/문서·미추적 파일은 해시·목록으로 보존하는 §5, host 통합 I01~08과 실제 환경 R01~07을 분리한 계약이 현재 요청과 맞아요. 이번 역할은 검토만 하며 새 worktree나 앱 실행 없이 판단했어요.

4541836293은 실제 SDK 오류→수락 쓰기→마지막 unbind→서비스/알림 종료 증거가 없는 동안 미해결로 유지해요. Fake/공유 종료 판단·계측 컴파일·host H3 성공으로 SDK/Room/JNI/FGS runtime을 PASS 처리하거나 resolve하지 않아요.07 결과도 허용 검사·재현 준비와 실제 시연 완료를 나눠 보고하고 DRAFT/미검증을 보존해야 해요.

현재 구현의 명시 확인 ACK와 DB 멱등성·수락 작업 수명은 이번07 회귀 범위로 유지하고 화면/음성 자동 ACK나 Gleo를 추가하지 않아요. 이 범위 유지는 최종 제품의 읽음 정의나 버튼 필수/음성 완료 의존 정책을 새로 확정한다는 뜻은 아니에요. 사용자와 논의 중인 실제 카드 노출/읽음 정책은 별도 정의 확정·문서 동기화 전 선행 변경하지 않아요.

**명세 수정 요청 없이07 구현·허용 검사 착수 가능**으로 판단해요. 이후 고정 SHA의 검사·독립 코드 리뷰가 필요하고 실제 SDK/UI/Room/서비스·Drive 재현은 NOT_TESTED예요. 이번에는 코드 수정·resolve·실행 검증을 하지 않았어요.

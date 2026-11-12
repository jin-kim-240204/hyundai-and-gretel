# PR4 · 07 독립 명세 리뷰

- AI 독립스펙리뷰어 · 2026-11-12.
- 고정 SHA `3b844d1131671ad92429b5ba465d67264b398d78`, 부모 `0f0a7daa1189bcdba62cd27a58033a8659b43737`, production 코드 기준 `e8ef142`.
- 요청 issuecomment-6813629860 및 07/00/06/부모 정본, 01~06 AC·구현/미검증 기록, 현재 production callers·기존 통합 테스트 대조.
- 판정: blocker0 / 구현 전 미결정 정책0 / 추가 선택 개선0. 스펙 관점 구현 착수 가능. 리뷰이 최종 판단 및 독립 재검토 루프 유지. 구현/런타임 승인 아님.

## 적용 규칙 및 공식 근거

pleos-connect ENV-02/VEH-03/DES-06, clean-code-review23~26/28~32, android-compose-architecture, pleos-design/vehicle/verify, ponytail-review. 사용자 실행 금지가 IDE/runtime 검사보다 우선. 새 추상화/기능·음성/자동 ACK 요구 없음.

| 추적 ID | 공식 요구/프로젝트 선택 | 명세 및 확인 | 상태 |
|---|---|---|---|
| DOC-6386c5462132#DO-Drive | Pleos 지역별 정책: DO 미선언 Drive 불가, 플랫폼/지역 차이 가능 | 07§3.1/00/부모의 사용자 정책 대체, gm b61ffd7/819df06 XML true 및 현재 Manifest 부재 | 정적 PASS, 실제 Drive NOT_TESTED |
| DOC-9faba42791f0#기능별규제 | DO 후에도 실제 ActiveRestrictions 반영 의무 | 07§3/UX 계약·I06/I08/R06/R07, CarRestrictionsSource/UiRestrictions/LocationView/Binder 최신 ACK | 명세 일관 PASS, 실제 규제 NOT_TESTED |
| DOC-AAOS#activity-metadata | 주행 허용 activity 자식 distractionOptimized=true, 선언은 실제 준수/심사 보장 아님 | 07:54~65 및 I08 source/merged/APK 양 variant 긍정·반례 검사 | 계획 적합 PASS, 아직 구현 안 함 |
| DOC-1ba06876919b#초기화해제 | SDK unregister/release 별개, 해제 후 재사용 금지 | 기존 PleosLocationSource/Tracking 및 07 I03/I04/R03, SDK1.1.3 의존성 유지 | 기존 계약 보존 정적 확인, SDK runtime NOT_TESTED |
| PROJECT-clean-head | polling dirty 혼입 없이 지정40자리 detached HEAD로 새 증거 생성 | 07§5, I01~08/F02~03, SDK path 환경 지정·원본 해시 보존·release/Manifest 검사 | 실행 가능한 계획, 새 검사 NOT_TESTED |
| PROJECT-runtime-evidence | Fake/compile로 Android Room/JNI/FGS/Drive 성공 주장 금지 | §7/R01~07/F05~06·4541836293 유지·DRAFT 보존/현행 PR 설명 | 명세 PASS, runtime NOT_TESTED |
| PROJECT-ACK | 명시 확인 유지, 화면/음성 자동 ACK/Gleo 추가 금지 | 07§1/3/4/7, 현재 confirmVisibleReceipt·ReceiptRepository·visited snapshot | 정적 일관 PASS |

공식 원문 직접 열람:
- https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy — 주행 상황에 따른 앱 사용 여부/지역·플랫폼 차이.
- https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide — DO 이후 CarUx/ActiveRestrictions 직접 적용.
- https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en — activity 자식 metadata/플랫폼은 선언 확인·실제 준수는 별개.
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release — unregister/release 구분/새 인스턴스. 최신 원문의 일반 설명을 SDK1.1.3 새 실동작 보장으로 확대하지 않음. 이번 새 SDK API/권한 도입 없음.

## 독립 정적 검사

- PASS `python3 scripts/verify_harness.py` —133 source pages/27 rule gates/static Manifest. 로그 `/private/tmp/pr4-07-spec-harness.log`. 현재 DO 부재 상태의 기존 정적 검사이지 새 DO 구현 검증이 아님.
- PASS `git diff --check 0f0a7da..3b844d1`.
- PASS `git diff --name-only e8ef142..3b844d1 -- app scripts` 빈 결과: 이번 명세가 앱/스크립트를 선행 변경하지 않음.
- 참고 두 commit Manifest를 git show+XML 파싱: b61ffd7/819df06 각각 .MainActivity 자식 metadata=true, 현재3b844d1 없음. remote contains는 origin/gm/feat-speech-to-text만 반환. 브랜치 merge/cherry-pick 없음.
- PR reviewThreads(first:100) pageInfo.hasNextPage=false; unresolved는4541836293 한 건. DRAFT=true. 상태/스레드 수정 없음.
- 기존 RoadNotePickupFlowTest/SubmissionSdkFailureTest/TrackingReceiptLifetimeTest의 Fake/공유 판단 범위를 읽고 I01~08/R03 분리가 적절함을 확인. 이들의 기존 PASS를 새 clean HEAD 결과로 복사하지 않음.
- N/A 새 JVM/빌드/lint 실행: 명세만 검토. 새 테스트/제품코드 작성 없음.
- 기존 polling/문서 dirty 보존, 검증 worktree·runtime script 생성 없음.

## 게시 확인

COMMENT 리뷰: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5868721026

- DO/CarUx 최소안 확인(수정 요청 없음), 07:59: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546012581
- 기존 SDK runtime 스레드 보류 확인(수정 요청 없음), 07:184: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4546012592
- API state COMMENTED/commit 일치 및 두 코멘트 등록 확인. 게시 직전 원격 SHA 일치. 억지 결함/추가 정책 질문 없음.

## NOT_TESTED / 후속

새 clean HEAD07 JVM/빌드/정적 검증은 구현 후 수행할 계획이다. 실제 Drive 표시/DO 효과·CarUx/UI·SDK/mock/provider/HTTP·Room/JNI·FGS·시연 R01~07은 NOT_TESTED. 앱/Studio/adb/기기/에뮬레이터/계측 실행0. 주행 목표는 구현 및 허용 검사를 제출하더라도 실제 화면 유지 성공과 구분한다. 제품코드/Manifest 미수정. 리뷰이 판단 후 후속 요청 대기.

## 정확한 게시 본문

AI 독립스펙리뷰어(Codex) · 07 명세 · 고정 SHA `3b844d1131671ad92429b5ba465d67264b398d78` / 부모 `0f0a7daa1189bcdba62cd27a58033a8659b43737` / 코드 기준 `e8ef142`.

**판정: blocker 0 · 구현 전 미결정 정책 0 · 추가 선택 개선 0. 스펙 관점에서 구현 착수 가능한 범위로 판단합니다.** 이는 구현 승인 역할의 최종 결정이나 runtime 완료 판정이 아닙니다. 구현 전 리뷰이 판단/독립 재검토 루프를 유지합니다.

두 줄별 확인 의견을 남겼습니다(수정 요청 아님). 사용자 추가 요구를 MainActivity 단일 DO metadata로 반영하면서 현재 CarUx 기능별 제한을 유지하는 방향은 공식 원문과 일치합니다.00/06/07·부모 정본의 과거 ‘새 DO 없음’ 대체도 일관됩니다. full UI 유지가 제한된 본문/설정까지 무조건 허용한다는 의미로 쓰이지 않았고, KEEP_SCREEN_ON/FGS/DO의 책임도 분리했습니다. [Pleos 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy), [CarUx 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [Activity 선언](https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en)을 직접 재확인했습니다.

I01~08은 production 경계와 기존 테스트를 재사용하고, 정확한 clean HEAD의 전체 JVM/빌드/lint/Manifest·release 정적 근거를 새로 만들도록 정의돼 있습니다. 기존 polling dirty 보존, v4/legacy 전체 큐·수락 쓰기·명시 ACK 유지, 실제 시연 대본과 R01~07 미검증 분리, PR 본문 현행화/DRAFT 유지 및 기존 runtime 스레드 보류 범위도 확인했습니다. Gleo/음성·화면 자동 ACK는 추가하지 않습니다.

독립 검사: 문서/현재 production callers·기존 AC/기록 대조, gm 참고 두 commit/current Manifest XML 및 브랜치 포함 관계, 문서 diff 공백, `python3 scripts/verify_harness.py` 정적 PASS. 원격 전체 스레드 페이지 끝 확인: unresolved4541836293 한 건, 변경/resolve 없음. app/scripts는 e8ef142 대비 변경 없음. 새 JVM/빌드 실행은 문서 리뷰에서 N/A이며 기존93/25 PASS를 이번 clean HEAD 결과로 쓰지 않았습니다.

**NOT_TESTED:** clean HEAD의 새07 검사(계획), DO 실제 효과·Drive 화면·SDK/mock/HTTP·CarUx/UI·Room·JNI·FGS/시연. 앱/Studio/adb/기기/에뮬레이터/계측 실행 및 제품코드 수정 없음. 기록 `docs/work/pr4-07-spec-review.md`. 리뷰이 후속 판단 대기.

AI 독립스펙리뷰어 · 고정 SHA `3b844d1131671ad92429b5ba465d67264b398d78` · 확인 의견(수정 요청 없음).

기존 MainActivity에만 선언하고 debug/release 최종 Manifest까지 확인하는 범위가 적절합니다. [AAOS 공식 설명](https://source.android.google.cn/docs/automotive/driver_distraction/guidelines?hl=en)은 metadata를 activity 자식에 두며, [Pleos 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)는 DO 이후에도 ActiveRestrictions 반영을 요구합니다. 따라서 DO로 Activity 표시를 허용하는 것과 본문/설정/탐색 제한을 유지하는 것은 양립합니다.

b61ffd7/819df06의 Activity true 선언과 현재 HEAD의 부재를 직접 XML로 대조했습니다. 기존 브랜치 전체를 가져오지 않고 선언만 보완하는 최소안에 동의합니다. 06의 ‘새 DO 없음’을 과거 범위로 한정한00/06/부모 정본도 일치합니다. 현재는 명세 적합성만 확인했으며, 실제 Drive 화면 유지·UI/CarUx·안전/심사 효과는 NOT_TESTED입니다.

AI 독립스펙리뷰어 · 고정 SHA `3b844d1131671ad92429b5ba465d67264b398d78` · 확인 의견(수정 요청 없음).

clean detached HEAD와 원래 polling dirty를 분리하고 Fake/host 통합과 실제 SDK·Room·마지막 unbind 검증을 나눈 점을 확인했습니다. 원격 reviewThreads를 마지막 페이지까지 조회한 결과 미해결은 기존4541836293 한 건이며, 이 줄처럼 실제 SDK 오류→수락 쓰기→서비스 종료 근거가 없으면 계속 보류하는 것이 맞습니다. I03/공유 종료 판단 PASS로 이를 resolve하지 않는 계약을 유지해주세요.

07의 종료를 ‘허용 검사 및 재현 준비 완료’로 보고하고 R01~07을 NOT_TESTED로 남기는 F05/F06·마지막 순서와도 일치합니다. 현재 명시 확인 ACK 유지, 화면/음성 자동 ACK 미추가도 확인했습니다. 이번에는 문서/호출부 정적 검토만 수행했으며 새로운 JVM/빌드/실제 runtime은 실행하지 않았습니다.
# PR4 03-c 독립 스펙 리뷰

대상: docs/specs/road-notes/03c-debug-submission-ui.md
SHA: 714387ba0e03c6a781b17a7ea321ff2a0a1ff049

판정: blocker 0 / 정책 질문 1 / 선택 개선 1. 최종 채택·구현 착수는 리뷰이 판단 대기.
구현 코드 변경 없음. 기존 미커밋 변경 보존. 운영 문서 필독 및 금지 실행 준수.
적용: pleos-connect, clean-code-review 9/15/17/20/28/29/32, android-compose-architecture §5~7, pleos-design, ponytail-review.

## 원격 게시용 요약

AI 스펙리뷰어(Codex) · 03-c 스펙 독립 리뷰 · SHA `714387ba0e03c6a781b17a7ea321ff2a0a1ff049`.

판정: 확정 blocker 없음. 구현 전 리뷰이 판단이 필요한 정책 질문 1건(저장 대기 중 기존 시작 버튼), 선택 개선 1건(연결 교체 테스트 입력)을 해당 문서 줄에 남깁니다. 이 리뷰는 작성자 자가승인이나 구현 착수 승인이 아닙니다. 리뷰이가 채택/기각·이유를 판단하고 필요하면 작성자가 문서를 반영한 뒤 재검토합니다.

03계약 및 해당 SHA의 LocationViewModel/LocationView/LocationTrackingService/RoadNoteRepository를 대조했습니다. 서비스가 살아 있을 때 현재 결과 재관찰, 새 서비스에서는 Idle, 준비/첫 표본 대기에서 클릭 허용 후 Repository 거절, 단일 저장 gate와 서비스 소유 수명 재사용은 기존 계약과 양립합니다. 명시된 내용을 다시 결함으로 지적하지 않았습니다. debug/release 슬롯도 [Android source set 공식 문서](https://developer.android.com/build/build-variants)에 맞는 구현 가능한 후보입니다. 서비스 수명은 [Android started/bound 설명](https://developer.android.com/develop/background-work/services/bound-services), 버튼 disabled/loading 의미는 [Pleos Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button)을 확인했습니다. Pleos SDK 자체의 새 계약이나 실제 동작을 검증했다는 뜻은 아닙니다.

정적 검토 PASS: 범위/상태 소유/거절·결과 정책/AC와 검증 한계 구분. NOT_TESTED: 03-c 구현·JVM/빌드(스펙 단계라 미실행), 실제 UI·재생성·Binder·SDK·Room·주행/대비/터치/물리 크기. 사용자 제한에 따라 앱/설치/Studio/adb/기기/에뮬레이터/계측은 일절 실행하지 않았습니다. 과거 PASS를 이번 런타임 증거로 재사용하지 않았습니다.

## 인라인 1 — docs/specs/road-notes/03c-debug-submission-ui.md:54

**정책 질문 · 저장만 남아 있을 때 기존 시작 버튼도 비활성화할까요?**

제보 버튼을 비활성화하고 수락한 저장은 유지하려는 정책은 분명합니다. 다만 기존 위치 시작 버튼과의 접점은 아직 비어 있습니다. 현재 LocationView.kt의 시작 버튼은 STOPPED에서 활성이고, LocationTrackingService.startTracking()은 `!isTracking && submissions.isSubmitting`이면 요청을 무시합니다. 따라서 제보 수락→수집 중지→DB 대기 중 화면 복귀→시작 클릭 순서에서 버튼은 눌리지만 시작되지 않습니다. 이 경로를 03-c에서도 그대로 둘지 구현자가 선택하게 됩니다.

작은 대안은 (A) 저장만 남은 동안 기존 시작 버튼도 비활성화하거나 (B) 기존 활성 조건을 유지하되 ‘저장 완료 후 시작할 수 있어요’라고 안내하는 것입니다. 저는 이미 제출 상태를 관찰하는 이번 변경에서 A를 추천합니다. 상태에서 계산한 조건 하나만 기존 시작 버튼에 전달하면 되고, 재시작 큐나 자동 시작은 필요 없습니다. 대신 main 화면 버튼 조건까지 변경 범위가 조금 넓어집니다. 기존 위치 제어를 이번에 보존하려는 의도라면 B 또는 현상 유지라는 판단을 명시해주시면 됩니다.

확인 조건: STOPPED+Submitting에는 위치 시작/제보 모두 정책대로 동작하고, 저장 완료 뒤 수동 시작은 다시 가능하며 자동 재시작은 없어야 합니다. 이는 프로젝트 UX 정책 질문이며 확정 플랫폼 결함은 아닙니다. AI 스펙리뷰어(Codex), SHA `714387ba0e03c6a781b17a7ea321ff2a0a1ff049`, 대상 문서 54행 및 17~21행. NOT_TESTED: 실제 버튼/서비스/Room 실행.

## 인라인 2 — docs/specs/road-notes/03c-debug-submission-ui.md:82

**선택 개선 · 연결 A/B를 다른 상태로 만들어 혼합 상태도 검출하면 좋겠습니다**

이미 ‘같은 연결의 추적/제출 상태’를 요구하고 있어 계약 누락은 아닙니다. 다만 테스트 입력이 A/B 모두 WAITING·Idle이면, 두 Flow를 따로 교체하면서 A의 추적 상태와 B의 제출 상태가 잠시 섞여도 검사가 통과할 수 있습니다.

82행의 테스트 계획에 A=RECEIVING/Saved, B=STOPPED/Idle처럼 서로 구별되는 입력과 교체 직전 예약된 A 갱신을 넣고, scheduler를 진행시키는 동안 관찰된 값이 어느 한 연결의 쌍인지 확인한다고 한 문장 보완하는 것을 추천합니다. 표시용 Flow만이 아니라 클릭 시 호출한 대상도 B인지 Fake 호출 수로 확인하면 오래된 제출 참조도 잡을 수 있습니다. 새 ConnectionManager나 세션 프레임워크를 만들 필요는 없습니다.

근거: 43~47행의 연결 일관성 계약과 [StateFlow 공식 설명](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)의 최신값 재전달·빠른 중간값 생략 특성입니다. 최종값만 확인하는 테스트와 모든 과도 상태가 안전한지는 구분하면 좋겠습니다. AI 스펙리뷰어(Codex), SHA `714387ba0e03c6a781b17a7ea321ff2a0a1ff049`, 대상 문서 82행. NOT_TESTED: 이 테스트는 아직 구현/실행하지 않았고 실제 Binder 전환도 확인하지 않았습니다.

## 게시 상태

조정자 후속 지시에 따라 직접 원격 게시하지 않고 게시 대행 요청. payload: /private/tmp/pr4-03c-spec-review-payload.json . 코멘트 링크는 게시 확인 후 조정자가 기록할 것. GitHub head 조회는 대상 SHA 일치 확인. 기존 해당 문서 인라인 없음.


## 4a0f332 수정 스펙 재검토

AI 스펙리뷰어(Codex) · 03-c 수정 스펙 재검토 SHA `4a0f3321a8e8341bc6d8ba4e0347a357002ae89f`. 채택 2건의 문서 반영을 확인했습니다(56/84/85행, C05/C10). blocker 0, 미결 정책 질문 0이며 **스펙 관점에서 03-c 구현 착수 가능**으로 판정합니다. 리뷰이의 기존 채택 판단을 반영한 수정본을 독립 확인한 결과이며 작성자 자가승인이 아닙니다. 두 스펙 스레드만 문서 반영 근거로 resolve했고, 코드/기기 증거가 필요한 다른 스레드는 정리하지 않았습니다. 구현 완료나 AC 실행 PASS 판정은 아닙니다. NOT_TESTED: 03-c 코드 구현·JVM/빌드·실제 UI/Binder/SDK/Room/재생성. 앱/Studio/adb/에뮬레이터/계측은 실행하지 않았습니다. 다음 단계는 조정자의 구현자 전달이며 저는 재검토 결과 보고 후 대기합니다.

게시 확인: https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#pullrequestreview-5864295551

https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542338245
https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4542338519
두 지정 스펙 스레드 isResolved=true API 확인. 구현 코드 변경 없음.

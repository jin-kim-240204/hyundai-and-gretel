# PR4 04 코드 리뷰이 판단

게시 완료: [필수 수정 수용 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4544765745). API의 in_reply_to_id=4544756327을 확인했어요. resolve하지 않았으며 조정자 보고 후 대기해요.

대상 SHA `ef4f86006e09c67212a128cb11efaf9ebbf772b4`, 리뷰5867169591/인라인4544756327. 적용: pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review23~25/28/32, 기존 Main 한정 구조·취소 전파. 범위는 재현 근거 검토·최소 수정 권고·기록·PR답글이에요. 코드/스펙/테스트 수정·resolve·앱/Studio/adb/에뮬레이터/계측 실행은 하지 않아요.

## 판정: 수용 / 필수 수정

실제 NearbyCandidates 84~98행은 관찰 Job의 유효성 없이 session+cells만 검사해요. null/stale로 A를 취소한 뒤 같은 session/cover의 B가 시작되면 key는 같아져요. A의 NonCancellable 정리가 늦게 IOException을 던질 때 A catch가 fail→clear를 실행하여 현재 observation인 B·rows·key까지 제거해요. B 조회 성공 뒤 오래된 작업이 새 관찰을 지우는 확인된 경계 결함이에요. 정책 변경이나 선택 스타일 개선으로 분류하지 않아요.

대조: production NearbyCandidates/NearbyNoteStore 및 서비스·Room 관찰 호출부 검색, NearbyCandidatesTest7개, `/private/tmp/roadnotes04-review-probe/OldObservationProbeTest.kt`, `/private/tmp/roadnotes04-review-probe-result.xml`, 독립 리뷰 기록·실제 원댓글. XML은1실패이며 B Ready 기대에 Failed(STORAGE_ERROR)가 기록돼 있어요. 기존 테스트는 다른 세션/cover 또는 즉시 취소 가능한 MutableStateFlow 중심이라 같은 키+취소 후 늦은 정리 오류 경계를 포함하지 않아요.

## 최소 권고와 범위

관찰 launch 자신의 context/Job에 ensureActive를 결과 적용 전과 세 오류 catch의 fail 직전에 적용하는 안을 우선해요. 필요하면 launch 진입에서 자신의 context를 지역 val로 잡아 중첩 수신자 혼동만 피하면 돼요. 서비스 scope.ensureActive나 observation 필드(이미 B일 수 있음)의 활성 검사는 해결책이 아니에요. CancellationException은 계속 전파하고 기존 key/session/위치 신선도 검사는 유지해요. 검사→상태 변경 사이 suspend 없이 Main 한정이라는 현재 전제를 보존해요. 고유token도 가능하지만 매 관찰 취소가 이미 보장되는 구조에서는 새 세대 상태보다 기존 Job 재사용이 작아요. 취소 지연을 기다리는 cancelAndJoin으로 위치 처리를 막거나 새 계층을 만들 필요는 없어요.

작성자 변경 범위: NearbyCandidates 관찰 collect/오류 경로, NearbyCandidatesTest의 동일키 반례와 정상 오류 회귀, 해당04 구현 작업/검증 기록. 공통정본·정책·스키마·SDK 필수순서/해제·03수락저장수명은 바뀌지 않아요.

확인 조건: A취소→동일키 B Ready→A늦은실패/결과에도 B후보·state·구독 유지; 이후 B목록 갱신 반영; B자신의 오류는 정상 Failed; 취소를실패진단으로기록하지않음; 기존 null/stale/다른session/cover회귀 유지. 늦은 결과 방어는 코드상 같은 약점이지만 현재 실패 XML은 IOException 경로 하나의 실행 근거예요. 결과까지 이미 실행 재현됐다고 쓰지 않아요.

## 증거 상태와 한계

- PASS: HEAD 일치·코드/반례/기존테스트/원댓글 대조, 답글 중복없음 확인.
- FAIL(독립 리뷰어 실행 기록): 위 실제 production+Fake JVM 반례1개. 이번 직접 재실행은 하지 않았어요.
- 참고: 독립 리뷰어의 기존20 PASS는 정상 회귀 범위의 기록이며 동일키 반례를 무효화하지 않아요.
- NOT_TESTED: 권고 수정·추가 회귀 실행/빌드, 실제 Room 오류 순서·Android SDK/서비스/기기 동작. H04/구현완료는 수정·검증·재리뷰 전 보류예요.

공식 [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html)는 해당 Job/context 비활성 시 CancellationException을 던져요. [협력적 취소](https://kotlinlang.org/docs/cancellation-and-timeouts.html)와 정리는 플랫폼 일반 계약이며 Fake와 같은 Room 오류순서 보장은 아니에요. 현재 공식 API 페이지 버전과 프로젝트 배포물 실행 검증도 구분해요. 새 SDK 계약 추정은 없어요.

## PR 답글 본문

AI(Codex) 리뷰이 판단 · 검토 SHA `ef4f86006e09c67212a128cb11efaf9ebbf772b4`이에요.

필수 수정으로 수용해요. 실제 NearbyCandidates의 결과/오류 guard와 임시 반례 소스·실패 XML을 대조했어요. 세션+cover는 위치가 null이 된 뒤 같은 위치로 복구되면 재사용될 수 있어서 관찰 인스턴스를 구별하지 못해요. A 취소→같은 키 B Ready→A 정리의 늦은 IOException 순서에서 A가 fail()을 호출하고, 그 안의 clear()가 현재 B의 Job과 rows까지 지우는 경로가 맞아요. 기존 다른 세션/cover 테스트만으로는 이 경계를 막았다고 볼 수 없겠어요.

최소 수정은 고유 토큰을 추가하는 대신, scope.launch로 만든 ‘해당 관찰 자신의’ Job/context에 ensureActive 검사를 두는 안을 권해요. 결과의 rows 대입/발행 직전과 세 오류 catch의 fail 호출 직전에 검사하고, 기존 session/cover/신선도 guard 및 CancellationException 재전파는 유지해요. 서비스 scope나 observation 필드의 현재 B Job을 검사하면 살아 있는 B를 보고 A를 통과시킬 수 있으므로, A가 자신의 launch context를 확인해야 해요. 검사부터 상태 반영까지는 기존 Main 직렬 문맥에서 suspend 없이 처리하는 전제예요. 별도 coordinator·세대 상태·cancelAndJoin 대기는 추가하지 않아요.

반례를 회귀로 옮겨 같은 키 B가 Ready인 상태에서 A의 늦은 실패와 결과가 B 상태·후보·관찰을 바꾸지 않는지, 이후 B가 새 목록을 계속 반영하는지 확인하도록 요청해요. 반대로 현재 B 자신의 오류는 기존 분류대로 Failed가 되어야 하고, 취소를 일반 실패 로그로 바꾸면 안 돼요.

이번에는 코드·반례·기존 실패 기록을 검토했으며 직접 재실행하거나 수정하지 않았어요. Kotlin의 협력적 취소/ensureActive 계약은 확인했지만 이 Fake가 실제 Room에서 같은 오류 순서가 발생함을 입증하지는 않아요. host JVM 경계의 확인된 결함으로 판단하며 Android Room/SDK/서비스 재현은 NOT_TESTED로 유지해요. H04와 구현 완료 판단은 작성자의 최소 수정·회귀 통과·재리뷰 전까지 보류해요.

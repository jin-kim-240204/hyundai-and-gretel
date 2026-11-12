# PR4 06 코드 리뷰이 판단

게시 완료: [입력 공백 수용 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545698690), [ACK 최신 탐색 제한 답글](https://github.com/jin-kim-240204/hyundai-and-gretel/pull/4#discussion_r4545698910). 각API응답의in_reply_to_id를확인했어요. resolve없음.

대상 `79516eb2b60874eb94830523f53c0f8b39560af8`, 리뷰5868267298. 적용 pleos-connect ENV-02/VEH-03/PRIV-01, clean-code-review23~25/28~32, 기존Main한정/상태정본/취소·자원해제/규제수락경계. 읽은스킬은pleos-connect/clean-code-review,기존Compose·vehicle·design검토기준유지. 제품코드·명세·테스트수정/resolve·앱/Studio/adb/기기/에뮬레이터/계측실행없음. 기존dirty보존,본기록만추가해요.

## 결정: 두 건 모두 필수 수정 수용

4545675100: 실제DemoRuntime는read반환전공백검사없음,반례XML에3001ms후READY/stops1→1(1FAIL). AndroidDemoInput의connect/readTimeout1초와본문64KiB는전체응답마감아님. 남은기한은마지막성공입력단조시각+2초에서계산하고tick대기포함,전체검증된응답만갱신. timeout은stickyERROR/수집정리/늦은응답주입0회/명시재시작. 사용자취소는CE전파. 블로킹IO에withTimeout만추가하면코루틴종료대기로오류전환이늦어질수있으므로입력취소/connection정리도같이검토해야해요. 필요시현재motion소유권의단일기한Job으로읽기대기중에도ERROR·SDK중지를수행하며읽기종료기다림으로실패반영을늦추지않아요. 복수watchdog/새네트워크계층은요구하지않아요. 정확히2초/초과기준은기존명세대로구분하고OS실시간정확성보장으로표현하지않아요.

4545675109: VM늦은uiState의allowsContent만으로수락금지. 기존visited정본은VM유지하고visited.value불변스냅샷을현재session의ACK계약에전달,서비스최신restrictions로본문/길이/깊이·항목예산을같은Main수락진입에서재검사하는최소안권고. 새중복예산/허용Boolean만전달/일반규제엔진없음. 사용자/현재항목/RESUMED재검사까지무suspend,거절시DB큐·ACK불변/거절클릭자체방문증가없음. 수락된ACK는이후제한변화와무관하게기존고정키쓰기/카운트/종료정리를끝내요.

## 작성자 수정·검증 범위

입력: debug DemoRuntime의motion기한/오류·정리,필요시AndroidDemoInput의취소가능HTTP정리,DemoRuntimeTest반례회귀. ACK: LocationViewModel/LocationConnection/LocationSession/서비스Binder의좁은계약·기존Fake/수락경계테스트. UiRestrictions.allowsContent기존정책재사용. 부모정본정책변경은없고06§6/UX04를충족하는구현수정이에요. 작업/검증기록은수정완료·검증완료를구분해갱신해야해요.

기한테스트:미완료/조각응답대기중기한초과ERROR/stop,늦은완료주입0,정상tick·기한경계·명시중지·옛기한의새소유작업격리. ACK테스트:raw규제LIMIT_CONTENT/depth0변경후UI스케줄러미진행queued클릭쓰기0/큐유지,항목상한·현재예산·수락전거절,수락후규제변경에도쓰기완료. 실제production경계검증이며테스트에규칙을재구현하지않아요.

## 근거/상태

HEAD일치·원격원댓글/답글없음확인,독립리뷰·06명세§6/UX04·DemoRuntime/AndroidDemoInput·UiRestrictions/CarRestrictionsSource·VM/Connection/서비스Binder/LocationSession호출부대조. 임시 `/private/tmp/roadnotes06-review-probe/DemoGapProbeTest.kt`와 `/private/tmp/roadnotes06-gap-probe-result.xml`를읽었어요. readMotion awaitCancellation인production orchestration+Fake의FAIL이며실제HTTP반례아니에요. ACK는정적호출반례이며CarUx실행재현으로표현하지않아요. 기존18PASS/작성자86PASS는타역할기록으로이번직접재실행하지않았어요.

공식 [URLConnection readTimeout](https://developer.android.com/reference/java/net/URLConnection#setReadTimeout(int))과[CarUxRestrictions content depth](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions#getMaxContentDepth())를확인했어요. HTTP전체마감/최신정책수락검사는프로젝트계약이며새PleosSDK계약추정없음.

PASS: 코드/명세/공식문서/반례기록정적대조. FAIL(독립리뷰실행):입력공백1건. NOT_TESTED:이번직접JVM/빌드,권고안실행·실제HTTP/SDK/provider·CarUx/Compose/Room/서비스/기기. 새수정·허용회귀·독립재검토전06완료판정보류예요. 보고후대기해요.

## 게시 본문

### 4545675100

AI(Codex) 리뷰이 판단 · 고정 SHA `79516eb2b60874eb94830523f53c0f8b39560af8`이에요.

필수 수정으로 수용해요. DemoRuntime의 공백 검사가 readMotion 반환 뒤 consoleDelta에 있고, AndroidDemoInput의 readTimeout은 개별 읽기 대기 제한인 점을 대조했어요. production DemoRuntime+지연 Fake의 반례 소스와 실패 XML도 확인했어요. 응답을 기다리는 동안 마지막 성공 입력의 기한이 지나도 READY와 수집이 유지되는 경로가 맞아요. 실제 HTTP에서 재현했다는 근거로 확대하지는 않아요.

최소 수정 계약은 ‘읽기를 시작한 시각’이 아니라 마지막 검증된 성공 입력의 단조시각부터 남은 2초 기한을 읽기 대기에도 적용하는 거예요. 기존 1초 tick 대기도 이 시간에 포함하고, 유효한 전체 응답만 성공 시각을 갱신해요. 조금씩 도착한 바이트로 기한을 연장하지 않아요. 기한 초과는 현재 입력 세션의 sticky ERROR·SDK 수집 정리·늦은 응답 주입 금지로 처리하고 명시 재시작만 허용해요. 사용자 중지/역할 교체의 CancellationException은 계속 취소로 전파해요.

withTimeout을 블로킹 withContext(IO)에 씌우는 것만으로 종료가 제때 관찰된다고 가정하면 안 되겠어요. 입력 전체 기한과 취소/연결 정리를 함께 맞추고, 읽기 종료를 기다리느라 ERROR/수집 중지가 미뤄지지 않게 해야 해요. 현재 HTTP 경계가 즉시 취소를 전달하기 어렵다면 현재 motion 소유권에 묶인 취소 가능한 기한 작업 하나로 그 전환을 수행하는 작은 안이면 충분해요. 두 감시 체계나 범용 네트워크 계층을 동시에 추가할 필요는 없어요. 정리 완료 전 새 입력을 시작하지 않고 기존 소유권/취소 검사도 유지해요.

반례를 회귀로 옮겨 미완료/조각 응답 대기, 기한 경계, 기한 뒤 늦은 완료 주입0회, 현재 작업만 실패·옛 기한이 새 작업에 영향 없음, 정상 tick과 명시 중지를 확인하도록 요청해요. 이번에는 코드·기존 실패 기록만 검토했고 수정·재실행은 하지 않았어요. 실제 HTTP/SDK/provider는 NOT_TESTED이며 수정·검증·재리뷰 전 완료 판단은 보류해요.

### 4545675109

AI(Codex) 리뷰이 판단 · 고정 SHA `79516eb2b60874eb94830523f53c0f8b39560af8`이에요.

필수 수정으로 수용해요. ViewModel의 uiState는 combine/stateIn으로 전달되는 표시 상태이고, Binder는 최신 정책을 읽어도 canShowBody/fits만 검사하므로 최신 maxDepth=0을 거절하지 못하는 정적 경로가 맞아요. 기존 LIMIT_CONTENT 계약을 충족하는 보완이며 새 규제 정책을 만들 필요는 없어요.

최소안은 ACK의 같은 Main 수락 경로에서 서비스의 최신 restrictions.state.value와 현재 방문 예산을 사용해 기존 allowsContent(현재 receipt 키, seen, depth=1)를 본문/길이 조건과 함께 다시 검사하는 거예요. 방문 예산의 소유자는 기존 ViewModel에 두고 uiState의 늦은 복사 대신 visited.value의 불변 스냅샷을 현재 연결의 ACK 호출에 전달하는 좁은 변경을 우선 권해요. UI가 미리 계산한 허용 Boolean만 전달하거나 예산 상태를 서비스에 중복 저장하지 않아요. LocationSession/Connection/Binder 및 Fake 호출부를 같은 계약으로 맞추면 돼요.

검사→현재 사용자/항목·RESUMED 확인→Repository 수락 사이 suspend 없이 처리하고, 거절된 클릭은 저장/ACK/DB 큐를 바꾸지 않아야 해요. 거절된 클릭 자체로 방문 예산을 추가하지도 않아요. 실제 노출로 기록한 방문은 별도 기존 의미를 유지해요. 수락 후 규제가 바뀐 ACK는 원래 키로 끝내고 쓰기 카운트/서비스 수명은 유지해요.

원본 규제를 LIMIT_CONTENT/maxDepth=0으로 바꾸고 UI 파생 상태를 일부러 진행시키지 않은 상태에서 클릭해 저장0회/큐 유지인지 production 수락 경계로 확인하도록 요청해요. 항목 예산 경계와 이미 수락된 ACK 완료도 회귀에 포함해요. 일반 권한 계층이나 새 예산 매니저는 추가하지 않아요. 이번에는 정적 대조만 했고 실제 CarUx/Compose 경합과 수정안 실행은 NOT_TESTED예요.

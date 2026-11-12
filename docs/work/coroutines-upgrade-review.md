# 코루틴 업데이트 시 flatMapLatest 재검토

사용자 결정: 현재 API는 유지하되 kotlinx.coroutines 버전을 변경할 때 다시 검토한다. 날짜 예약 알림이나 자동 업데이트 감시는 설정하지 않았고, 버전 선언과 실제 사용처에 TODO를 남긴다.

## 검토를 시작할 때

- app/build.gradle.kts의 coroutines-android/coroutines-test 선언 버전을 올리거나, 다른 의존성 변경으로 실제 해결된 코루틴 버전이 달라질 때 확인한다. 현재 직접 선언은 둘 다1.9.0이며 실제 버전은 Gradle dependencyInsight로 확인한다.
- `rg -n '코루틴 업데이트 검토' app`으로 버전 선언과 두 사용처를 찾는다. 이 표식은 임시 `리뷰용:` 주석과 별개로 유지한다.

## 확인할 내용

1. 공식 릴리스 노트와 API 문서에서 flatMapLatest의 ExperimentalCoroutinesApi 표시·취소·버퍼 계약 변경을 확인한다. 최신 API 상태를 추측해 OptIn만 제거하지 않는다.
2. RoadNoteServiceConnection: 연결 교체/해제 후 이전 서비스 상태가 화면에 남거나, 클릭이 이전 연결로 전달되지 않는지 확인한다. buffer(0) 필요성도 새 계약과 대조한다.
3. RoomRoadNoteStore: 활성 시나리오 교체 시 기존 DB 관찰이 취소되고 새 시나리오 원본만 전달되는지 확인한다. 취소 예외와 늦은 결과 차단을 유지한다.
4. RoadNoteServiceConnectionTest, NearbyNoteCandidatesTest, RoadNotesDemoFlowTest와 RoomScenarioStoreTest를 확인한다. JVM은 실행하고, Room 계측은 당시 실행 허용 범위에 따라 실행하거나 미검증으로 명시한다.
5. 실험 표시가 사라졌고 해당 범위에 다른 실험 API 사용이 없을 때만 OptIn과 불필요한 import를 제거한다. API 유지/대체/제거 판단과 결과를 이 문서에 갱신한다.

공식 API: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html

이번 변경 검증: 기존 미커밋 변경을 보존하고 주석3줄·검토 문서만 추가했다. 주석 삽입 전후 실행 코드 동일성 및 git diff --check PASS. 실행 코드가 바뀌지 않아 빌드/테스트/앱 실행은 추가하지 않았다. 커밋·푸시는 아직 수행하지 않았다.

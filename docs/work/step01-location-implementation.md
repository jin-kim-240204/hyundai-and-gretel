# 01 위치 수집 구현

요청: 합의된01 스펙 구현, MVVM·ponytail 최소 구조. 전체 설계/02버퍼/DB/proximity 구현 안 함. 작업 브랜치 ny/spec-driven-road-notes, worktree /private/tmp/hyundai-road-notes-sdd.

범위: SDK1.1.3/FGS 권한·서비스 선언, MainActivity 권한과 시작의도, ViewModel 서비스 상태 연결, 서비스 소유 추적 세션, 좁은 SDK 어댑터와 테스트. 앱 ID는 부모의 com.jinnayeong.wakeup 유지(기존 참고앱과 별도 설치). 추적은 화면과 독립, 상세 좌표는 debug에만 기록.

규칙 ENV-02/VEH-01/03/PRIV-01/DES-04. DOC-402de45b9f3d#권한: SDK dangerous 권한 요청, null/차단 제한. DOC-1ba06876919b#수명: initialize 직후 등록, 동일 리스너 해제 후 release/재시작 새 인스턴스. DOC-9844e9eaad3c#콜백: nullable·미지정 스레드·늦은 콜백 처리. DOC-ba9b83fc86f6#오류: 사용불가/실패/취소 구분. DOC-e12e49cc3efc#레이아웃 및 DOC-189d894e8124#윈도: 앱 영역 내 최소 시작/중지, 스크롤/여백. 기존 원문과01스펙 재사용. android-compose-architecture 스킬은 저장소에 없어 기존 MVVM 원칙 적용.

Android FGS location 유형은 SDK권한 외 FOREGROUND_SERVICE/FOREGROUND_SERVICE_LOCATION 및 Android 위치 권한 필요. 이번에는 COARSE만 요청(타입 요건 충족 목적, SDK 정확도 계약과 별개). POST_NOTIFICATIONS는 요청하되 거절로 FGS 자체를 차단하지 않음. 화면에서만 시작, START_NOT_STICKY, 명시적 중지 및 지속알림 중지 액션. 홈/재생성에서 유지. 프로세스 사망 복원 없음.

검증 예정: 순수 세션 단위(중복/늦은콜백/null/오류/정리), debug/release/lint/harness, IDE빌드, Pleos 실수신·홈·재생성·권한. SDK 직접 initialize→register로 실패 시 조사·리뷰하며 임의 고정 대기 추가 안 함.

## 최종 구조와 코드 리뷰 진입점

운영 Kotlin5파일: MainActivity(권한/시작 의도 및 최소 Compose View), ui/location/LocationViewModel(화면 상태·서비스 연결/명령), location/LocationTrackingService(FGS/알림·추적 소유), LocationTracking(세션 상태·직렬 콜백·검증·정리), PleosLocationSource(공식 SDK 순서). 별도 UseCase/Repository/DI/버퍼/접근 API 없음. 세션 테스트를 위해 좁은 LocationSource 계약만 사용한다.

실행 흐름: 버튼→Activity 권한 결과를 RESUMED에서 유효 의도일 때만 처리→ViewModel startForegroundService→FGS 알림→새 SDK initialize 직후 register→콜백 진입 단조시각→Main scope에서 세션 재검사→debug RoadLocation 로그. ViewModel은 서비스 상태를 읽고 Compose는 그 상태만 표시한다. Activity onStop은 대기 의도만 폐기한다. 시작된 수집은 서비스에 남는다. onCleared는 화면 구독/바인딩만 해제한다.

수집 시작은 화면 조작만 제공하며 서비스 exported=false. location FGS 요건을 위한 ACCESS_COARSE_LOCATION과 Pleos SDK권한을 모두 허용해야 한다. 알림 거절은 시작을 막지 않는다. 중지는 화면 버튼과 알림 액션에서 가능하다. 프로세스 사망 복구 없음(START_NOT_STICKY).

## 실제 검증 결과 (2026-11-10)

환경: 기존 Pleos Connect2.1.0/API34/user10 AVD 데이터 wipe 없이 실행, SDK1.1.3. 앱 ID com.jinnayeong.wakeup, 참고앱 com.jinkim.hyundaiAndGretel은 덮어쓰지 않음. Android Studio는 이 worktree를 별도 프로젝트로 열어 빌드 확인.

- PASS: LocationTrackingTest5개. 중복 시작1회, null/NaN/위경도 범위와(0,0) 복구, 중지 후 예약/이전세션 콜백 차단, SDK실패와 정리실패, 미지원·초기화실패, CancellationException 재전달.
- PASS: TrackingLifecycleTest1개 최종1.441초. 실제 SDK/FGS 시작, Activity CREATED로 중단해도 유지, ActivityScenario.recreate 후 같은 tracking 인스턴스 유지, 지속알림 중지 PendingIntent로 STOPPED 확인. 계측은 위치 주입을 사람이 기다리는 테스트가 아니라 자체 수행하는 수명 테스트.
- PASS: SDK 고정 대기 없이 최초/재시작 등록·실제 수신. ADB127.0276→127.0281 경로의 좌표가 콜백 로그에 대응. 최초등록 session1 약20:56:10.978, 첫좌표20:56:12.946. 별도 준비 폴링·지연 없음.
- PASS: 홈 조작 후에도 수집 유지. 단, Pleos 홈 버튼만으로는 Activity가 계속 visible/RESUMED일 수 있으므로 이것만을 백그라운드 증거로 쓰지 않음. 설정 앱이 완전히 표시되어 우리 Activity STOPPED/mVisible=false인 상태에서 새127.041/37.501좌표가 수신됨을 별도 확인.
- PASS: 일반 권한 허용(최종 PERMISSION_RESULT pending=true state=STARTED에서 RESUMED 전이 후21:06:38.279 STARTING→REGISTERED→21:06:39.054 LOCATION), 실제 거절 후 설명/무수집, 권한 대기 중 설정앱 전환으로 onStop 확인→pm grant→복귀 시 무수집. 설정에서 권한을 허용한 효과만 확인하고 권한 화면 버튼 조작 전 과정을 자동화한 것은 아님.
- PASS: POST_NOTIFICATIONS 거절 상태에서 location FGS 시작, 명시적 중지 후 isForeground 제거. 알림의 중지는 계측에서 별도 확인.
- PASS: debug/release assemble, 단위5개, lintDebug, harness --build, Studio build_project. lint 오류0/경고40, 기존 Lifecycle 라이브러리 lint API 불일치로 일부 검사가 생략된 한계는 남음. 새 Service 참조 누수 경고와 API28 상수 경고는 제거.
- PASS(제한 범위): 기본2560x1440/density160 화면에서 앱 영역 안 최소 상태/시작/중지 표시, 시스템 영역 침범·잘림 없음. evidence/step01-location/minimal-controls.png. 물리 텍스트 크기·모든 폼팩터/대비 정량/주야간 인증은 미검증.

## 실패와 수정/제한

1. 최초 기기 계측은 AndroidJUnitRunner 클래스 누락으로 Process crashed. androidx.test:runner:1.7.0을 androidTest에만 추가 후 통과.
2. 호스트 임시 검증의 좌표 문자열 일치 두 건은 SDK 소수 표현(예37.5009983333) 때문에 실패. 숫자값으로 주입 좌표와1e-5도 이내 대조하여 확인. SDK/앱 좌표를 바꾸거나 표시 보정하지 않았음. 임시 검증 스크립트는 제품에 포함하지 않음.
3. 권한 결과를 STARTED에서 바로 실행하던 초안은 RESUMED까지 기다리도록 보완. Lifecycle ON_RESUME과 결과 도착 양쪽에서 같은 함수를 호출하며, onStop은 의도를 폐기. 최종 정상 허용과 실제 onStop 후 늦은 허용을 구분해 확인.
4. 최초 IDE 빌드 호출은 worktree가 IDE에 열려 있지 않아 거절. 해당 worktree를 별도 프로젝트로 연 후 실제 IDE 빌드 PASS. 원래 참고 브랜치의 빌드를 증거로 사용하지 않음.

NOT_TESTED: 권한 팝업 대기 중 실제 Activity 재생성 조합, privacy 차단·실제 SDK null/error/강제 연결 장애, 실제 SDK 해제 실패, 백그라운드에서 외부 호출에 의한 FGS 시작 거절 재현, 실차·전체 폼팩터. 가짜 입력의 실패/취소 테스트와 실제 SDK 장애 재현을 구분한다. 서비스 비공개·시작 진입점/상태 검사는 코드로 확인했으나 OS 거절 전체를 입증하지 않는다. 필수 AC의 일부 실행 근거가 남아 있으므로 전체 기능 검증 완료로 보고하지 않는다.

이번에는 구현과 확인 결과를 코드 리뷰에 제공한다. 커밋·푸시·다음단계 구현은 하지 않는다.01단계 버퍼는 없으며 화면 밖45초 갱신은02단계에서 검증한다.

## 재현

1. 이 worktree의 debug APK 설치 후 앱에서 위치 수집 시작. 필요한 SDK/대략적 위치 권한 허용. SDK수집 상태만 화면에 표시.
2. `adb logcat -s RoadLocation:D '*:S'`로 로그 확인. `adb emu geo fix 127.0276 37.4979`처럼 경도→위도 순으로 서로 다른 합성 좌표 주입.
3. 설정 앱 등 다른 앱으로 우리 Activity를 완전히 가린 상태에서 새좌표 주입해 같은 세션 로그 확인. 홈이 실제 비가시인지 dumpsys로 구분.
4. 화면 또는 지속 알림에서 중지. STOPPED 뒤 좌표 로그가 새로 반영되지 않음 확인. 다시 시작은 새 SDK 인스턴스 사용.
5. 자동 수명 계측: `adb shell am instrument -w -e class com.jinkim.hyundaiAndGretel.location.TrackingLifecycleTest com.jinnayeong.wakeup.test/androidx.test.runner.AndroidJUnitRunner` (test APK 설치 필요).

마무리: 설정 복귀 시 이미 허용된 권한의 오래된 안내만 지우고 자동 시작하지 않도록 정리. 최종 debug/release·단위·lint·Studio 재검사 PASS. 테스트 후 수집 중지 상태 확인, 이번 실행의 에뮬레이터 종료. 설치 APK/테스트 APK와 허용된 앱 권한은 AVD에 남아 있다. 보고서 exit2는 명시된 미검증을 나타내며 실패를 PASS로 바꾸지 않는다.

후속 요청: 사용자가 `feat:-` 접두사의 단일 구현 커밋 및 푸시를 승인했다. 위 소스 검증 결과를 재사용하고 diff 검사를 재확인한다. 미검증 항목은 그대로 유지하며 다음 단계는 진행하지 않는다.

## PR 구현 리뷰 후속

이번 리뷰 반영과 새 검증 범위는 [후속 기록](pr4-implementation-feedback.md)에 분리했다. 기존 ‘같은 tracking 인스턴스 유지’ 검증을 새 화면 재연결 근거로 확대하지 않는다. 기존 FakeSource.stop 예외도 SDK unregister/release 실행 증거가 아니다. 이번 후속 계측에서 화면 ViewModel 상태·등록 횟수와 실제 SDK의 unregister 권한거절 후 release 경로를 별도 확인했다. 권한 팝업 대기 중 재생성은 여전히 NOT_TESTED다.

# 제출 준비 점검표

현재 판정은 **FAIL**입니다. 실행 가능한 release APK는 APK 선택 제출 조건입니다. 저장소 제출 시 README에 앱 소개, 실행 방법, 테스트 환경과 설정, Pleos Emulator 버전·화면 규격, APK 위치 또는 Release 링크를 기재합니다. 제출용 APK의 기본 위치는 `apk/팀명_앱이름_release.apk`이며, 저장소 업로드 용량을 초과하면 GitHub Releases를 사용합니다. [공식 공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)

| 항목 | 상태 | 근거와 남은 확인 |
|---|---|---|
| release 빌드 | PASS | `:app:assembleRelease`로 `app/build/outputs/apk/release/app-release-unsigned.apk` 생성 |
| 서명·제출 산출물 | FAIL | 현재 release 서명 설정과 제출용 APK/Release 링크가 없음. 제출 경로와 서명을 확정하고 설치 가능한 release를 검증해야 함 |
| 새 설치에서 제보·수신 | FAIL | release `DemoRuntime`은 비활성이며 시연 생성·A/B 전환을 제공하지 않음. `RoadNoteService`는 활성 시연 Flow를 `RoomRoadNoteStore`에 전달하며, 시연 ID가 없으면 `observeNearby`는 빈 목록을 내보냄. 제보 저장은 활성 시연과 준비된 제보자 상태를 요구하므로 release에서는 `DEMO_REQUIRED`로 차단함 |
| 외부 Gleo 경로 | FAIL | release 병합 Manifest에서 `GleoActionReceiver`의 `exported=false`. `GleoActionReceiver`의 release Callback은 자기 패키지로 지정됨. 외부 연동을 제공하려면 공식 발신자 신뢰·등록 및 응답 계약부터 확정해야 함 |
| README 실행·테스트 환경 | PASS | 앱 소개, debug 실행·테스트 환경, 이전 개발 AVD 버전과 화면 규격 기재 완료 |
| README 제출 링크 | FAIL | 로컬 빌드 경로만 기재. 제출용 APK 경로 또는 공개 링크 없음 |
| 기기 동작 | NOT_TESTED | 깨끗한 release 설치, Gleo 실음성/Callback, 실제 수신 TTS 청취·AUDIO 정책, 화면 렌더링·기기 주행 규제 및 최신 Figma 대조 미검증 |
| App Market 심사 | NOT_TESTED | 등록·심사용 메타데이터와 실제 기능, 성능, 콘텐츠, 주의 분산, 개인정보/보안 심사 증거 미확인. [Pleos 앱 심사 제출](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide) · [App Review Guidelines](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide) |

후속 release 검증은 debug 이력이 없는 설치에서 시작해야 합니다. 공모전의 GitHub 선택 제출 절차는 접수 메일에 최종 커밋을 기재하도록 요구합니다. 비공개 저장소의 경우 공모전 안내의 공개 기간 또는 압축 파일 링크 대안을 적용합니다. [공식 공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)


## release 기능화에 필요한 조건

- **신뢰된 Gleo 계약과 등록** · 공개 예시는 Action과 Callback의 broadcast 형식을 설명합니다. 출시 발신자를 검증할 수 있는 권한·서명 계약과 검증된 Callback 대상 패키지는 이 예시에서 확인되지 않았습니다. 공급자에게 출시 계약을 확인하고 스키마 검토·승인을 거쳐야 합니다. 현재 release 수신기의 외부 노출과 Callback 대상은 기존 설정을 유지합니다. [Gleo 코드 예시](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples) · [스키마 전달 및 승인](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)
- **독립 합성 재생과 시연 UX** · 새 설치에서 사용자가 명시적으로 합성 시연을 시작하고, SDK 실위치와 분리된 재생 입력으로 제보·접근·수신을 재현할 수 있어야 합니다. 화면에 합성 재생과 SDK 미통과를 표시하고 실제 SDK 콜백은 그 출처를 유지합니다. 실위치 영속화는 별도 목적 고지·동의·보안·보존 결정 전까지 비활성입니다. [구현 계획](plans/implementation-plan.md) · [Pleos 개인정보 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy)
- **서명과 깨끗한 설치 검증** · 제출용으로 서명된 release와 접근 가능한 제출 경로를 준비하고, 기존 debug 데이터가 없는 기기에서 설치·기동·권한·제보·수신·ACK·음성·화면 규제를 확인해야 합니다. [공식 공모전 제출 안내](https://appdevchallenge.auto-hmg.io/guide/submission)

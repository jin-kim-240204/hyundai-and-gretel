# PR #4 최종 답변 반영

요청: gh로 리뷰/답변 최종 확인 후 문서 수정, 반영한8개 스레드에 ‘반영했습니다.’ 답글. 구현은 하지 않는다.

범위: 01 스펙,00 전체 계획, 공통 implementation-plan과 수명/버퍼 Mermaid 원본, 조사 기록의 후속 정책 구분. 후속 사용자 정정에 따라 공통 설계 수정은 spec-road-note-system의 d038d8e로 분리·푸시하고 기능 브랜치에 pull/merge했다. 기능별 스펙은 기능 PR에 유지한다. 과거 SVG는 재생성하지 않고 변경된 flow/states는 최신 Mermaid 원본을 본문에 포함한다.

합의: 홈/다른 앱/재생성에서도 추적 유지·02단계45초 버퍼 지속 갱신. FGS 우선 검토하되 유형/권한/Pleos 화면 밖 동작 미검증. 권한 대기 중 onStop/재생성은 시작 의도 폐기.01 좌표 UI/5초 표시 만료 제거·로그 진단. 제보 신선도02 유지. null은 등록 유지·중지만 제공. initialize 직후 등록·사전 실험/고정 대기 없음. 실행 문맥 직렬화·세션 재확인, CancellationException 재전달·해제 실패에도 release. 네트워크 정책 정교화·강제종료 복구는 후속.

근거: 스펙§9의8개 답변 링크를 전부 조회. 공식 Pleos 샘플 c955c3a의 FusedLocationClient initialize/startUpdates를 gh로 확인. Android FGS service-types의 Android14/type/location 요건 및 restrictions-bg-start를 공식 웹으로 확인(2026-11-10). location 유형 일반 런타임 요건과 Pleos FUSED_LOCATION 권한을 동일시하지 않음. 공식 SDK nullable/error·수명 요구는 기존 스냅샷 재사용.

규칙 ENV-02/VEH-01/03/PRIV-01. 확인은 문서/원문 대조 및 링크·diff·하네스. FGS 및 화면 밖 수신 구현 검증은 NOT_TESTED이며 이번 문서 수정 완료와 구분. 새 런타임 코드/빌드/기기 실행 N/A.

결과 PASS: 문서 링크·diff 검사, harness --report(8 checks)。구현·기기 검증은 미착수. PR에서 반영을 확인할 수 있도록 문서를 sepc: 커밋·푸시 후8개 원 스레드에 지정 문구로 답글을 남긴다. 리뷰 스레드 resolve/merge는 하지 않는다.

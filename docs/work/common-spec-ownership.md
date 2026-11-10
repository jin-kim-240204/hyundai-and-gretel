# 공통 설계 변경 소유 브랜치 정정

요청: PR #4에 섞였던 공통 implementation-plan 및 flow/states 도표 수정을 spec-road-note-system으로 이동. 앱/단계별 스펙은 가져오지 않는다. 기존 공개 커밋을 재작성하지 않고 부모에 문서 커밋 후 기능 브랜치에 merge하여 PR diff에서 공통 설계를 제외한다.

범위: 공통 설계·부속 Mermaid3개와 이 검증 기록. 공통 문서에서 기능 브랜치에만 있는 세부 스펙으로 향하는 로컬 링크는 고정 커밋 GitHub 링크로 변경해 부모 단독 열람을 보장한다.

규칙 ENV-02/VEH-03: 코드·SDK 변경 없음, 기존 합의의 문서 소유 위치 정정만 수행. 새 공식 요구 판정이나 런타임 검증 없음. 기존 PR #4의 최종 답변 및 spec-step01-review-feedback 문서 대조 근거 재사용.

검증: 부모와 기능 브랜치에서 harness 및 diff 검사, 로컬 링크 확인. 최종 PR #4의 docs/plans diff가 비어 있음 확인. 빌드·기기 N/A(문서만).

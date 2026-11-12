# 07 주행 중 제품 화면 공통 계약 반영

사용자 명시 추가 범위에 따라 주행 중 화면 사용/사라짐 문제를07에 포함한다. 부모2ca00c0에서 docs/plans/implementation-plan.md의 기존 ‘06 새 DO 없음’ 미래 제약을 MainActivity activity metadata DO 계획으로 대체했다.06 당시 코드 사실은 이력으로 구분한다. 앱 코드는 수정하지 않는다. 기능07 스펙 독립 리뷰 후 최소 구현하며 CarUx/안전 명령/명시 ACK/수락 쓰기·DB 보존 계약을 유지한다.

적용 ENV-02/VEH-03/DES-06, DOC-6386c5462132#DO미선언Drive 및 DOC-9faba42791f0#DO선언후기능제한. 2026-11-12 공식 두 원문을 다시 읽었고 기존2026-11-01 스냅샷과 핵심 요건이 동일하다. AAOS의 Activity metadata 원문도 대조했다. 공식 의무와07의 최소 Activity/기존 UI 보완 선택을 구분한다. b61ffd7/819df06의 변경과 포함 브랜치를 fetch 후 대조했으며 Gleo 코드/브랜치를 통합하지 않는다.

문서 정적 검사만 수행한다. 앱/Studio/adb/기기/에뮬레이터/계측 실행 없음. 실제 DO 적용/Drive 표시/규제 효과는 NOT_TESTED. 부모 문서만 별도 commit/push 후 기능 브랜치로 병합하고00/06/07을 동기화한다. 정책·명세 독립 리뷰 전 코드 선행 없음.

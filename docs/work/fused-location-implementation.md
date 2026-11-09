# 위치 쪽지 스프린트 구현

요청: S0~S6을 순서대로 구현하고 검증 단위로 한국어 커밋. push는 요청 범위 밖. MVVM + ponytail: UI는 상태/이벤트, ViewModel은 화면 조합, 위치/저장/판정은 별도 책임. 범용 프레임워크·서버·상시추적 추가 없음.

선택 규칙: ENV-01/02/03, VEH-01/03/04, PRIV-01, DES-01/04/06, SAF-01, INT-01. 원문: DOC-c5e08a80ebfd#의존성, DOC-402de45b9f3d#권한, DOC-1ba06876919b#수명, DOC-9844e9eaad3c#콜백, DOC-ba9b83fc86f6#오류, DOC-72a5d7ab4ebb#최소수집, DOC-e12e49cc3efc#레이아웃, DOC-189d894e8124#윈도. 앞선 상세 플랜의 검토 내용과 공식 SDK 샘플을 적용하고 설치 AAR 심볼 확인. 새 proximity 원문은 API 존재와 실험 결과를 구분한다.

수정 범위: Gradle/Manifest, location/domain/data/ui/debug, schema/Receiver, 단위·Room·SDK 계측, 시연 스크립트·문서. 보관45초/목표30초/허용5초, callback 단조시각 기반 근사배치. 위치버퍼 메모리, 공유쪽지와 수신이력 Room. H3와proximity를 사용하되 검증 실패 시 완료라 하지 않는다.

S0: 상세 설계·원본 Mermaid·앞선 실험 결론 기록을 커밋한다. 구현 숫자는 프로젝트 초기값이며 실차 정확성을 보장하지 않는다.
S1~S6: 진행 중. 자동검사·런타임·미검증을 아래에 별도 기록한다.

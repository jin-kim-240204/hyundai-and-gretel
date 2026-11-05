---
name: pleos-vehicle
description: hyundai-and-gretel에서 Pleos 차량 API, 권한, 지원 범위, 콜백 및 휴식·기상 제어를 구현하거나 검토할 때 사용한다.
---
# 차량 기능 검증

`../pleos-connect/references/source-map.md`에서 기능에 해당하는 API 문서를 찾는다. 기본 필독 sources는 4803b04e4b28(permission), 988646a72e67(capability), 5a7f23474152(get), 55b831074ebf(set), 59bfb7b425fc(callback), 5e45302cb838(호환성), c954277e376f(하드웨어), 44f8864cb3d1(SDK정책)이다.

- API 이름/타입/권한은 설치 SDK와 공식 심볼 레퍼런스에서 확인한다. 좌석·카메라·트렁크가 지원된다고 추정하지 않는다.
- 조회 CAR_*와 제어 CONTROL_CAR_*를 구분하고 함수 @RequiresPermission을 확인한다. 필요한 권한만 선언하며 승인·거절·미지원 UX를 설계한다.
- Capability의 READ/WRITE/NONE, area, 범위/허용값을 확인한다. null/unsupported 값으로 현재 상태를 꾸며내지 않는다. 에러 코드를 처리한다.
- 서비스 initialize/release, listener 등록/해제를 앱 생명주기와 대칭으로 관리한다. 연결 끊김·프로세스 재시작·실패 콜백을 확인한다.
- 허용된 공개 SDK 경로를 사용하고 숨겨진 차량 API로 우회하지 않는다.
- 규제 문서 3790286d55ee, 6386c5462132, 12839eb0a523, 9faba42791f0을 함께 확인한다. 앱 카테고리·지역·상태별 정책과 CarUxRestrictions를 반영한다. 제어 조건을 속도==0만으로 대체하지 않는다.
- 자동 좌석 제어는 대상좌석/사용자동의/중단/동작조건을 명시한 프로젝트 안전 요구사항으로 다룬다. 공식 SDK가 보장하는 조건과 추가 제품 설계를 분리한다.
- 기상 기능은 잠금·백그라운드·suspend·강제종료·차량전원·재시작에서 각각 예약 유지/알림/취소/중복/복구를 검증한다. 이 항목은 hyundai-and-gretel 제품 신뢰성 요구사항이며 공식 API 지원 보장이 아니다.

선택 규칙 VEH-*와 SAF-* 및 원문 세부 항목을 작업 기록에 연결하고 `../pleos-verify/SKILL.md`로 검증한다.

---
name: pleos-connect
description: hyundai-and-gretel의 Pleos Connect Android 구현·설계·리뷰에서 공식 규정과 검증 루프를 적용한다. 저장소 Android/Pleos 작업에 사용한다.
---
# Pleos Connect 작업 루프

루트 AGENTS.md와 현재 작업 기록을 읽는다. [규칙 목록](references/rules.json)에서 적용 규칙 ID를 고르고 [문서 지도](references/source-map.md)로 관련 페이지를 읽는다. ID의 요약만으로 원문 검토를 대체하지 않는다.

## 작업별 로딩
- 프로젝트/SDK/에뮬레이터: [환경 규칙](references/setup.md).
- 차량 조회·제어·권한·생명주기: `../pleos-vehicle/SKILL.md`.
- 화면/인터랙션/디자인: `../pleos-design/SKILL.md`.
- 모든 구현 완료 전, 리뷰·배포 준비: `../pleos-verify/SKILL.md`.
- 음성/Gleo/내비/위치 등: 문서 지도에서 해당 SDK의 import → permission → initialization/release → events/error 페이지까지 선택한다. 기능을 쓰지 않으면 의존성/권한을 선제 추가하지 않는다.

## 요구사항 추적
선택한 각 원문의 의무·권장·조건·예외를 하나씩 작업 기록에 옮겨 적고 고유 ID `DOC-<페이지해시>#<항목명>`을 부여한다. 공식 URL과 절/문단 또는 도해, 구현 파일/줄, 검증 방법, 상태와 증거를 연결한다. 인용은 필요한 최소 길이로 하고 요구사항은 자기 말로 정리한다.
새 페이지가 확인되면 source-map/index 및 규칙 목록에 반영한다. 스냅샷은 탐색용이며 출시·SDK 변경·규정 판단 시 공식 원문을 다시 확인한다. 새 원문과 스냅샷이 다르면 날짜와 차이를 기록한다.

## 구현 → 검증 → 수정
1. 적용 조건과 예외를 판정하고 사용자 요청 범위 내 최소 구현을 한다.
2. verify-harness 검사와 변경에 필요한 빌드/동작 검증을 실행한다.
3. 원문 요구사항별로 코드와 동작을 다시 비교한다. 기존 구현의 성공 문구를 증거로 삼지 않는다.
4. 실패를 수정하고 영향받은 검사를 재실행한다. 미확인 항목은 NOT_TESTED로 남긴다.
5. 결과 기록을 갱신하고 통과한 것과 남은 제한을 보고한다. 자동 검사 통과는 전체 Pleos 적합성 또는 심사 통과를 의미하지 않는다.

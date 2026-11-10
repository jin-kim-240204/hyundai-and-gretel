# 30초 제보 대기 제한 제거

요청: 위치 버퍼를30초채워야쪽지를심을수있는제한제거. pleos-connect/ponytail 적용. 선택 규칙 VEH-03,PRIV-01. DOC-9844e9eaad3c#콜백:실제유효SDK표본만사용, DOC-ba9b83fc86f6#사용불가:빈/만료위치에서는저장금지. 기존 공식 원문 검토 내용 재사용, 새SDK/권한없음.

범위: selectAnchor·RoadNote offset검증·버튼준비문구·단위/기기테스트·시연정책. 최소1개신선한표본으로즉시심기허용.30초이력이없으면가장오래된표본,있으면기존30초전최근접표본. 실제offset저장. 공백/오류/권한/세션검증은유지. Room스키마변경없음.

검증 PASS: LocationBufferTest5개·PickupTest2개·RoutePlotTest2개 총9개. 첫표본offset0/짧은이력실제offset/빈·미래·오래된위치거절·offset범위검증. 기존30초이력통합테스트는기록30초를명시적으로기다려이전배치검증유지. StartPositionTest는첫SDK표본후3초내심기가능·저장offset0~5초assert PASS(동일소스기기검증8.481초 중). 사용자실제화면에서도30초전테스트제보성공확인. debug/release assemble·lint·Studio빌드PASS. 30초대기는없애되위치없는저장은허용하지않는다.

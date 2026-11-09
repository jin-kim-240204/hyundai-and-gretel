# 위치 브랜치에서 범위 밖 Gleo 구현 제거

요청: 이번 브랜치는 쪽지 심기/수신만 구현한다. 기존 gm/feat-speech-to-text의 Gleo 작업을 중복 구현한 부분을 제거한다. 기존 브랜치/PR 및 Git 이력은 변경하지 않는다.

범위: 새로 추가한 Gleo receiver/입력검증/JSON콜백/schema/Gleo전용테스트·Manifest등록 삭제. 통합계측은 debug 제보와 같은 controller.report를 호출하도록 되돌린다. 시연안내/브랜치계획/검증기록에서 Gleo를 이번 작업 범위로 제시한 내용을 정정한다. 위치버퍼·Room·H3·SDK proximity·MVVM UI·디버그버튼·가상주행은 유지한다.

선택규칙 ENV-02(기존Activity유지),VEH-03(위치수명유지),INT-01(N/A:기존Gleo브랜치책임). DOC-1ba06876919b#수명/DOC-9844e9eaad3c#콜백은 기존검토기준그대로유지. 새공식API도입없음. pleos-connect/verify 및 ponytail에 따라 삭제 후 전체참조검색,debug/release빌드·단위·lint,실제SDK심기→B수신계측,최종Manifest/asset확인을실행한다.

초기상태: git status clean. 예정삭제는 이번브랜치에서만추가한파일로한정한다. Gleo와무관한domain ReportContext/ReportResult 및 저장snapshot검증은 쪽지심기의 입력/결과계약이므로 유지한다. 주행UI에필요한DO선언·현재앱식별자는불필요한변경/데이터단절을막기위해유지한다.

정적/자동검증 PASS: app/src와scripts에Gleo/ACTION_PARAM/CREATE_ROAD_NOTE/schema.json참조없음. debug/releaseAPK에서assets/schema.json없음·DEX의앱gleo패키지없음·최종Manifest의GleoActionReceiver없음을assert했다. debug/release assemble·lintDebug·단위7개PASS. 기존gm/feat-speech-to-text브랜치내용수정없음. 위치제보경계의ReportContext/결과는유지했고GleoReceiver전용BuildConfig생성옵션은제거했다.

최종PASS: Gleo없는디버그제보controller.report→실제SDK과거위치Room저장→B등록/ENTER→수신1행통합계측55.208초PASS. 7개단위·debug/release빌드·lint·Studio빌드PASS. 보고서최초형식검사에서원문별항목누락을지적받아DOC-c6fc94252d66#Gleo제외 N/A항목을추가한뒤verify_harness --report exit0. baseline c00b3c2 대비앱gleo패키지/스키마diff없음. 기존gm브랜치HEAD 83c602d0dad6a9e9f45043ffc486754cd975b3bd 유지. 삭제반영APK설치완료. 푸시없음.

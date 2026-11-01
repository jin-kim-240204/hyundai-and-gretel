# Source: https://document.pleos.ai/docs/connect/guide/docs-design/components/fields/number-field
Retrieved: 2026-11-01

Pleos Connect 개발자 가이드
앱 기획·설계 가이드
디자인 가이드
Components
Fields
Number Field
Number Field
숫자 정보를 입력하기 위한 필드입니다.​



1. Root : 텍스트 필드 전체를 감싸는 최상위 컨테이너 영역 

2. Label : 사용자가 입력한 요소 

3. Cursor : 텍스트를 입력할 수 있는 상태에 제공되는 커서 요소 

4. Field : 사용자가 정보를 입력하는 영역

States​

 | State | Value | Description
 | Value | String | Text Field에 입력된 텍스트 정보
 | Focused | True / False | 터치 또는 보이스오버로 접근시 Text Field영역을 인식하고있는 상태일 때 True로 전환
 | Disabled | True / False | True일 경우, Text Field 입력 및 접근이 불가능한 상태. Focused state가 작동하지 않음
 | Read Only | True / False | True일 경우, Text Field가 입력값에 접근할 순 있지만 변경할 순 없음 Focused state는 작동하는 상태
 | Invalid | True / False | 입력 값이 유효한 값인지 아닌지 여부를 체크하는 상태



Properties​

 | State | Value | Description
 | Size | Large, Medium | Text field의 사이즈

## Original diagrams and visual specifications
https://document.pleos.ai/assets/images/num_anatomy-f467f22fce1085b86ed6a259f483fb9b.svg
https://document.pleos.ai/assets/images/num_states-a00fd36843fa7ababdd575e86a33a275.svg
assets/cf20c64b255464d3.svg

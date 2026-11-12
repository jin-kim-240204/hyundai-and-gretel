# Compose 디자인 시스템

원본: [Pleos Connect Figma](https://www.figma.com/design/UMhLHbSurVcDVwaYsiwAKz/?node-id=39675-4521)

## 사용

기존 `HyundaiAndGretelTheme` 내부에서 사용한다. 주야간은 시스템 설정을 따르며 Preview에서는 `darkTheme`로 지정할 수 있다. 배경화면 기반 Dynamic Color는 디자인 색상을 바꾸므로 사용하지 않는다.

```kotlin
HyundaiAndGretelTheme {
    PleosButton(
        text = "시작",
        onClick = onStart,
        type = PleosButtonType.Secondary,
        size = PleosButtonSize.Small,
        enabled = canStart,
        loading = isStarting,
    )
}
```

컴포저블은 값과 콜백만 받는다. 저장·차량 API·ViewModel·화면 이동을 포함하지 않는다. 호출자가 문자열 리소스와 상태를 제공한다.

| 정의 | 사용 위치 | Figma 근거 |
|---|---|---|
| `PleosTheme.colors` | 배경·표면·텍스트·구분선·버튼 의미 색상 | Design 39675:4528, 4537 / Components 67:22525 |
| `MaterialTheme.typography` | Headline·Title·Body·Label 크기·행간, Asta Sans | Foundation 67:12330, 12341, 12352, 12363 |
| `PleosSpacing` | 확인된 spacing-03/04/05 = 8/12/16dp | Foundation 타이포그래피 표 |
| `PleosButtonSize` | 기본 버튼 크기·최소 너비·패딩·아이콘·간격·모서리 | Components 67:22525 |
| `RoadNoteTypography`, `RoadNoteDimensions` | 앱 화면 시안에서 시작해 가독성 피드백을 반영한 값 | Design 39675:4528, 4537, 4548, 4608 |

Foundation Label/Medium은 Material `labelMedium`에 연결하고, 대응 원본이 없는 `labelLarge`는 같은 값을 사용한다. Title/Body의 다른 굵기는 `copy(fontWeight = ...)`로 선택한다. Foundation에 확인되지 않은 Material display 스타일은 이식 대상으로 포함하지 않았다.

## 공용 UI

- `PleosButton`: Primary(밝은 기본형), Secondary(채움형), L/M/S. 실제 터치의 pressed, enabled, disabled, loading 표현. 로딩과 비활성은 클릭 차단. 아이콘은 leading/trailing 슬롯으로 전달한다.
- `RoadNoteHeader`: 제목과 구분선.
- `RoadNoteStateCard`: 제목과 설명.
- `RoadNoteMessageCard`: 주의/정보 배지, 라벨, 메시지. `compact`는 분할 창 시안의 메시지 크기·폭을 선택한다. 야간과 분할 여부는 별개이므로 실제 화면에서는 창 크기에 맞춰 호출자가 지정한다.
- `RoadNoteInfoCard`: 아이콘 옆에 제목과 설명을 표시하는 작은 정보 카드.
- `RoadNoteBadge`: 원본의 48×48 아이콘. 주변 라벨과 의미가 중복되어 장식 이미지로 처리한다.
- `RoadNoteStatus`: 상태 문구와 선택적 로딩 표시.
- `RoadNoteAction`: 가독성 피드백에 맞춰 64dp 최소 높이와 28sp 액션 텍스트를 사용하는 앱 화면 버튼.

`PleosPreviews.kt`에서 주야간 수신 카드, 시작 카드, 버튼 크기/상태를 확인할 수 있다. 이번 변경은 공용 UI 준비이며 MainActivity의 시작 화면을 서비스 화면으로 교체하지 않는다.

## 리소스와 제한

Asta Sans 원본: Google Fonts `ofl/astasans/AstaSans[wght].ttf`, [OFL 라이선스](../licenses/AstaSans-OFL.txt). 가변 폰트의 굵기 축을 400/500/600/700/800에 연결한다.

아이콘 원본 SVG는 `assets/note_*.svg`, 앱용 리소스는 `app/src/main/res/drawable/note_*.xml`에 보관한다. Android SDK `Svg2Vector`로 48×48 크기와 패스를 변환했으며 임시 Figma URL을 앱에서 사용하지 않는다.

Figma Starter 호출 한도로 Foundation 색상 표 전체, 나머지 Components와 야간 버튼 원본을 확보하지 못했다. 야간 버튼은 임시 색상을 사용하는 `shortcut:`이다. 이식 완료나 원본 1:1 일치로 판단하지 않는다. 비활성 버튼 색은 원본의 낮은 대비를 유지하며 주행 적합성 검증 완료를 뜻하지 않는다.

작업·검증·후속 항목: [작업 기록](../work/compose-design-system.md).


## 앱 글자 크기 조정

사용자 에뮬레이터 검토 후 앱용 스타일을 Foundation의 큰 단계로 변경했다. 공식 의무가 특정 화면의 16~29sp를 고정한다는 뜻은 아니다.

| 용도 | 크기/행간(sp) |
|---|---|
| 화면 제목 | 36/44 |
| 카드 제목 | 32/40 |
| 설명·상태 | 26/34 |
| 경고 라벨·액션 | 28/36 |
| 수신 메시지 | 48/64 |
| compact 수신 메시지 | 40/52 |

수신 경고 라벨은 아이콘과 함께 "주의"만 표시한다. compact 메시지 폭은 280dp이며 가로·분할 창에서 3줄 이내 및 넘침을 확인한다. 실물 5mm/3mm 충족 여부는 별도 하드웨어 측정이 필요하다.

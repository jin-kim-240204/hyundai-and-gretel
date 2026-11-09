---
name: commit
description: hyundai-and-gretel 변경을 확인하고 한국어 커밋 메시지를 작성하거나 요청받은 git commit을 실행한다. 커밋 메시지 작성·커밋 요청에 사용한다.
---
# 커밋

루트 AGENTS.md와 현재 작업 기록을 읽는다. 메시지만 요청받으면 메시지만 작성한다. 스킬 추가·구현 요청을 커밋 실행 권한으로 해석하지 않는다.

## 변경 범위 확인

- `git status --short`, `git diff`, `git diff --cached`, `git log -10 --oneline`으로 작업 내용과 기존 스타일을 확인한다. 새 파일은 내용도 읽는다.
- 사용자 요청에 속한 파일만 명시적으로 stage한다. 범위가 명확하면 추가 확인 없이 진행하고, 다른 작업이 섞여 구분할 수 없으면 범위를 확인한다. 기존 staged 변경도 임의로 포함하거나 unstage하지 않는다.
- 비밀정보·로컬 설정·생성물 포함 여부와 staged diff를 확인한다. `git add .`와 `git add -A`는 사용하지 않는다.
- [pleos-verify](../pleos-verify/SKILL.md)에 따라 변경에 필요한 검사를 수행하고 작업 기록에 결과를 남긴다. 기존 결과는 현재 변경에도 유효할 때만 재사용한다. 실패·미검증을 숨기거나 검증 완료로 표현하지 않는다.

## 메시지

프로젝트 선택으로 `<type>(<선택 scope>): <한국어 설명>` 형식을 사용한다. scope가 불명확하면 생략한다.

- type은 변경에 맞는 `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`, `style`, `revert`를 선택한다.
- 제목은 짧고 구체적으로 쓰고, 필요할 때만 빈 줄 뒤 본문에 이유와 영향을 적는다.
- scope는 실제 변경 영역을 사용한다. 예: `app`, `vehicle`, `design`, `harness`, `skills`.
- 호환성을 깨는 변경이면 `!`와 `BREAKING CHANGE:`에 영향·이행 방법을 적는다. 티켓·공동 저자는 추정해 넣지 않는다.

```text
docs(skills): 커밋과 PR 작성 스킬 추가
```

## 실행과 확인

명시적으로 커밋을 요청받았을 때만 실행한다. 여러 줄 메시지는 임시 파일에 기록해 `git commit -F <파일>`로 전달한다. 훅을 우회하지 않고, amend·reset 등 이력 변경과 push는 별도 요청 범위에 있을 때만 수행한다.

완료 후 `git log -1 --stat`과 `git status --short`로 확인하고 커밋 해시, 검사 결과, 남은 변경을 알린다.

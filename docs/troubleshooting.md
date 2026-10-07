# 트러블슈팅 기록

## [2026-10-07] git 커밋 시 LF → CRLF 변환 경고

- **증상**: `git add`/`git commit` 할 때마다 아래 경고가 출력됐다.
  ```
  warning: in the working copy of 'CLAUDE.md', LF will be replaced by CRLF the next time Git touches it
  ```
- **원인**
  - Git for Windows가 설치 시 시스템 설정(`C:/Program Files/Git/etc/gitconfig`)에 `core.autocrlf=true`를 넣는다. → 저장소에는 LF, 작업 폴더로 꺼낼 때는 CRLF로 변환.
  - 작업 폴더의 파일은 에디터가 LF로 저장했기 때문에, Git이 "다음 checkout 때 CRLF로 바꾸겠다"고 예고한 것.
  - `git ls-files --eol`로 확인하니 저장소(`i/lf`)와 작업 폴더(`w/lf`) 모두 LF였다. 당장 문제는 없지만, 줄바꿈 결과가 PC마다 다른 `autocrlf` 설정에 따라 달라질 수 있었다.
- **해결**
  - `.gitattributes` 맨 위에 `* text=auto eol=lf` 추가. 저장소에 포함되는 설정이라 개인 PC의 `autocrlf`보다 우선 적용된다.
  - Windows 전용인 `*.bat`은 기존 규칙(`eol=crlf`)을 유지.
  - `git add --renormalize .` 로 기존 파일에 새 규칙을 적용 (이미 LF라 바뀐 파일 없음).
- **배운 점**
  - 줄바꿈 규칙은 개인 설정(`core.autocrlf`)이 아니라 `.gitattributes`로 저장소에 고정해야 어느 환경에서든 같은 결과가 나온다.
  - `git ls-files --eol`로 저장소(i)/작업 폴더(w)/속성(attr)의 줄바꿈 상태를 한눈에 볼 수 있다.

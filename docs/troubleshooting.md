# 트러블슈팅 기록

구현 중 겪은 문제를 **증상 → 원인 분석 → 해결 방안 → 결정 이유** 순서로 기록한다.

- 상태: `해결` 원인을 찾아 해결됨 · `우회` 임시 조치로 넘어감(근본 원인 미해결) · `미해결` 아직 해결하지 못함
- 같은 문제가 다시 생기면 기존 항목을 지우지 않고 새 항목을 추가한 뒤 **관련**에 기존 번호를 적는다.
- `우회`·`미해결` 항목이 해결되면 같은 번호에서 상태와 내용을 갱신한다.
- 설계 결정으로 이어진 경우 **관련**에 `decisions.md`의 번호(D-XX)를 적는다.

## 목차

| 번호 | 제목 | 상태 | 날짜 |
| --- | --- | --- | --- |
| T-01 | git 커밋 시 LF → CRLF 변환 경고 | 해결 | 2026-10-07 |

---

### T-01. git 커밋 시 LF → CRLF 변환 경고
`해결` · 2026-10-07

- **증상**: `git add`/`git commit` 할 때마다 아래 경고가 출력됐다.
  ```
  warning: in the working copy of 'CLAUDE.md', LF will be replaced by CRLF the next time Git touches it
  ```
- **원인 분석**
  - Git for Windows가 설치 시 시스템 설정(`C:/Program Files/Git/etc/gitconfig`)에 `core.autocrlf=true`를 넣는다. → 저장소에는 LF, 작업 폴더로 꺼낼 때는 CRLF로 변환.
  - 작업 폴더의 파일은 에디터가 LF로 저장했기 때문에, Git이 "다음 checkout 때 CRLF로 바꾸겠다"고 예고한 것.
  - `git ls-files --eol`로 저장소(i)/작업 폴더(w)/속성(attr)의 줄바꿈 상태를 확인하니 저장소(`i/lf`)와 작업 폴더(`w/lf`) 모두 LF였다. 당장 문제는 없지만, 줄바꿈 결과가 PC마다 다른 `autocrlf` 설정에 따라 달라질 수 있었다.
- **해결 방안**
  - A. 경고 무시: 지금은 모두 LF라 실제 문제는 없다.
  - B. 개인 설정 변경: 내 PC의 `core.autocrlf`를 `false`나 `input`으로 바꾼다.
  - C. 저장소 설정: `.gitattributes`에 줄바꿈 규칙을 명시한다.
- **결정 이유**: C를 선택했다.
  - A는 경고만 숨길 뿐, 다른 PC에서 checkout하면 CRLF로 바뀔 수 있는 위험이 그대로 남는다.
  - B는 내 PC에서만 해결된다. 다른 환경에서 clone하면 그 PC의 `autocrlf`에 따라 결과가 또 달라진다.
  - C는 저장소에 포함되는 설정이라 개인 PC의 `autocrlf`보다 우선 적용되고, 어느 환경에서든 같은 결과가 나온다.
  - **적용**
    - `.gitattributes` 맨 위에 `* text=auto eol=lf` 추가.
    - Windows 전용인 `*.bat`은 기존 규칙(`eol=crlf`)을 유지.
    - `git add --renormalize .` 로 기존 파일에 새 규칙을 적용 (이미 LF라 바뀐 파일 없음).

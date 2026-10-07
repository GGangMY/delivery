# 설계 결정 기록

## [2026-10-07] 문서 위치: 기록 문서는 docs/, 루트 문서는 그대로

- **상황**: 설계 결정·트러블슈팅을 기록할 위치가 필요했다. 기존 md 파일(CLAUDE.md, HELP.md)도 함께 옮길지 고민했다.
- **선택지**
  - A. 모든 md 파일을 docs/로 모은다.
  - B. 작업하며 쌓이는 기록만 docs/에 두고, 루트 문서는 그대로 둔다.
- **결정**: B
- **이유**
  - CLAUDE.md는 루트에 있어야 Claude Code가 세션 시작 시 자동으로 읽는다.
  - README.md는 루트에 있어야 GitHub 저장소 첫 화면에 표시된다.
  - HELP.md는 Spring Initializr가 생성한 참고 링크 모음이고 .gitignore에 포함되어 있어 과제 문서가 아니다.
- **결과**: docs/decisions.md, docs/troubleshooting.md를 만들고, 이후 API 명세·ERD 등도 docs/에 추가한다.

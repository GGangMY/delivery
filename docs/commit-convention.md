# 커밋 규칙

이 프로젝트의 커밋 메시지와 커밋 단위 규칙이다.
Claude Code는 커밋 메시지를 추천할 때 이 문서를 따른다.

## 1. 역할

- 커밋과 push는 **사용자가 직접** 한다. 요청하지 않으면 `git commit`, `git push`를 실행하지 않는다.
- 커밋 메시지를 요청받으면 **실제 변경 내용(`git diff`, `git status`)을 확인한 뒤** 추천한다.
- 커밋 메시지에 `Co-Authored-By` 등 AI 작성 표시를 넣지 않는다.

## 2. 메시지 형식

```
타입: 내용
```

- 한국어로 쓴다.
- 내용은 명사형으로 끝낸다. (`추가`, `수정`, `구현`, `작성`, `변경`, `분리`)
- 끝에 마침표를 붙이지 않는다.
- 여러 대상을 나열할 때는 **쉼표**를 쓴다. (`·`, `/`는 쓰지 않는다)
  - ✅ `feat: User, Menu, Payment 엔티티 생성자 추가`
  - ❌ `feat: User·Menu·Payment 엔티티 생성자 추가`
- 변경이 여러 가지면 첫 줄은 요약하고, 한 줄 띄운 뒤 본문에 목록으로 적을 수 있다.

```
fix: 연관관계 지연 로딩 적용 및 매핑 정리

- @ManyToOne 4개에 FetchType.LAZY 지정
- 연관 필드명 변경: ownerId → owner
- @JoinColumn에 nullable = false 추가
```

## 3. 타입

| 타입 | 언제 쓰나 | 예시 |
| --- | --- | --- |
| `feat` | 새 기능, 새 코드 추가 | `feat: 회원가입 API 구현` |
| `fix` | 잘못된 동작·요구사항 위반 수정 | `fix: 연관관계 지연 로딩 적용` |
| `refactor` | 동작은 그대로, 코드 구조만 개선 | `refactor: 주문 상태 검증 로직을 엔티티로 이동` |
| `docs` | 문서만 변경 | `docs: API 명세서 작성` |
| `test` | 테스트 코드 추가·수정 | `test: 엔티티 저장 및 연관관계 매핑 테스트 추가` |
| `chore` | 설정, 의존성, 패키지 구조 등 기능과 무관한 작업 | `chore: Order 패키지 구조 생성` |
| `style` | 포맷팅, 공백 등 로직 변화 없음 | `style: 코드 포맷 정리` |
| `perf` | 성능 개선 | `perf: 주문 목록 조회 N+1 문제 해결` |

### 헷갈리는 타입 구분

- **fix vs refactor**: 고치기 전에 잘못 동작했으면(또는 요구사항을 어겼으면) `fix`, 잘 동작했는데 코드만 낫게 바꿨으면 `refactor`.
- **feat vs chore**: 기능 동작에 쓰이는 코드(엔티티, Service, 설정 빈 등)면 `feat`, 빈 패키지·클래스 생성처럼 구조만 만든 거면 `chore`.
- **refactor vs style**: 이름·구조가 바뀌면 `refactor`, 공백·줄바꿈만 바뀌면 `style`.

## 4. 커밋 단위

- **작게 나눈다.** 성격이 다른 변경은 따로 커밋한다.
- 같은 작업이라도 **코드 / 테스트 / 문서**는 분리한다.
  ```
  feat: Order 생성 시 총액 계산 및 기본 상태 지정
  test: 엔티티 저장 및 연관관계 매핑 테스트 추가
  docs: 주문 총액 계산 위치 변경 기록 (D-04 → D-07) 및 테이블 명세 수정
  ```
- 설계 결정과 연결된 커밋은 `decisions.md`의 번호를 적는다. (`D-07` 등)
- 메시지만 읽고 무엇이 바뀌었는지 짐작할 수 있어야 한다. 메시지가 실제 변경을 다 담지 못하면 범위를 넓히거나 커밋을 나눈다.
- 여러 기능이 섞인 큰 변경이 생기면, 나눠서 커밋하자고 먼저 제안한다.

## 5. 커밋 전 확인

- [ ] `application.yml`의 `ddl-auto`가 `update`인지 (`create`로 남아 있지 않은지)
- [ ] 실제 비밀번호, JWT 비밀키가 포함되지 않았는지 → 포함되려 하면 경고한다
- [ ] 관련 테스트가 통과하는지
- [ ] 의도하지 않은 파일(`.idea`, `HELP.md`, 빌드 결과물 등)이 섞이지 않았는지

## 6. push 전 수정

- push 전 마지막 커밋 메시지 수정: `git commit --amend -m "새 메시지"` 또는 IntelliJ Log → Edit Commit Message
- push 전 커밋 취소(변경 유지): `git reset --soft HEAD~1` 또는 IntelliJ Log → Undo Commit
- push한 커밋은 `--amend`, `reset`을 쓰지 않는다. 되돌릴 때는 `git revert`로 새 커밋을 만든다.

## 7. 실제 커밋 예시 (이 프로젝트)

```
chore: PostgreSQL 연결 설정 추가 및 application.yml로 변경
docs: CLAUDE.md에 기록, 커밋 규칙 추가
docs: ERD 및 테이블 명세서 작성
docs: API 명세서 작성
feat: User 엔티티 및 UserRole(Enum) 추가
fix: 연관관계 지연 로딩 적용
feat: BaseEntity 및 JPA Auditing 적용
fix: Repository에 엔티티, ID 타입 지정
feat: User, Menu, Payment 엔티티 생성자 추가
feat: Order 생성 시 총액 계산 및 기본 상태 지정
test: 엔티티 저장 및 연관관계 매핑 테스트 추가
feat: Security 기본 설정 및 PasswordConfig 분리
```

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
| T-02 | 서명 없는 토큰(alg:none)이 validateToken에서 false가 아니라 예외로 빠져나감 | 해결 | 2026-10-08 |
| T-03 | JWT_SECRET을 실행 설정에 넣었는데 앱이 뜨지 않음 | 해결 | 2026-10-08 |

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

### T-02. 서명 없는 토큰(alg:none)이 validateToken에서 false가 아니라 예외로 빠져나감
`해결` · 2026-10-08

- **증상**: `JwtUtilTest`의 "잘못된 토큰은 예외 없이 false" 8개 케이스 중 `서명 없는 토큰 (alg:none)`만 실패했다. `false`가 반환되지 않고 예외가 테스트까지 올라왔다.
  ```
  io.jsonwebtoken.UnsupportedJwtException: Unsecured JWSs (those with an 'alg' (Algorithm) header value of 'none') are disallowed by default ...
  ```
- **원인 분석**
  - 서명 부분을 비우고 header를 `{"alg":"none"}`으로 바꾼 토큰은, 서명 검증을 건너뛰게 만들려는 대표적인 공격 형태다.
  - `parseSignedClaims()`는 서명된 토큰만 받기 때문에 이런 토큰을 `UnsupportedJwtException`으로 거절한다. 즉 라이브러리는 막아주고 있었다.
  - 문제는 `validateToken`의 catch 목록(`Expired`, `Signature`, `Malformed`, `IllegalArgument`)에 `UnsupportedJwtException`이 없었던 것. 잡히지 않은 예외가 `validateToken` 밖으로 던져졌다.
  - 이대로 필터에 연결하면, 이 토큰을 붙인 요청은 403이 아니라 **500**이 된다. 필터에서 난 예외는 컨트롤러 앞단이라 처리되지 않기 때문이다. 토큰이 필요 없는 메뉴 목록 조회도 이 토큰을 붙이면 500이 난다.
  - 기존 테스트는 `alg:none` 케이스를 `parseClaims`(예외를 던지는 쪽)로만 확인했고, `validateToken`(false를 돌려줘야 하는 쪽)으로는 확인하지 않아서 놓쳤다.
- **해결 방안**
  - A. `UnsupportedJwtException` catch를 따로 추가한다.
  - B. 공통 부모인 `JwtException` 하나로 묶어서 잡는다.
  - C. `Exception`으로 전부 잡는다.
- **결정 이유**: A를 선택했다.
  - 기존 catch가 원인별로 나뉘어 있어, 같은 방식으로 추가하면 로그만 보고도 "서명 없는 토큰이 들어왔다"는 걸 알 수 있다. 위조 시도인지 단순 만료인지 서버에서 구분하는 것이 catch를 나눈 목적이다.
  - B는 원인 구분이 사라진다.
  - C는 JWT와 상관없는 버그(예: NPE)까지 "잘못된 토큰"으로 숨겨버린다.
- **적용**
  - `validateToken`에 `catch (UnsupportedJwtException e)` 추가.
  - 목록 밖의 JJWT 예외가 같은 문제를 일으키지 않도록, 마지막에 `catch (JwtException e)`를 안전망으로 추가.
  - catch를 JJWT의 검사 순서(입력 → 형식 → 서명 여부 → 서명 일치 → 만료)대로 정리하고, 원인별 로그 문구를 넣음.
  - `JwtUtilTest` 11개 모두 통과.

### T-03. JWT_SECRET을 실행 설정에 넣었는데 앱이 뜨지 않음
`해결` · 2026-10-08

- **증상**
  - IntelliJ에서 `DeliveryApplication`을 실행하자 앱이 뜨지 않았다. 이전에 실행 설정(Run Configuration)의 Environment variables에 `JWT_SECRET`을 넣어 두었는데도 실패했다.
  - 같은 원인으로 `contextLoads`, `EntityMappingTest`처럼 JWT와 무관한 테스트도 실패했다.
  ```
  UnsatisfiedDependencyException: Error creating bean with name 'securityConfig' ... constructor parameter 0
  Caused by: BeanCreationException: Error creating bean with name 'jwtUtil'
  Caused by: PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}" <-- "${jwt.secret}"
  ```
- **원인 분석**
  - `Caused by`를 아래부터 읽으면: `${JWT_SECRET}` 값을 찾지 못함 → `jwtUtil` 빈 생성 실패 → 생성자로 `JwtUtil`을 받는 `securityConfig`도 실패 → 빈 하나가 실패해 애플리케이션 컨텍스트 전체가 뜨지 못함.
  - 무관한 테스트가 실패한 것도 같은 이유다. `@SpringBootTest`는 전체 컨텍스트를 띄우므로 `jwtUtil` 하나가 실패하면 함께 실패한다.
  - 실행 설정에 넣은 값이 사라진 이유: `.idea/workspace.xml`을 확인하니 실행 설정이 모두 `temporary="true"`였다. 클래스·테스트 옆 ▶ 버튼으로 실행하면 IntelliJ가 **임시 실행 설정**을 만드는데, 개수 제한(기본 5개)을 넘으면 오래된 것부터 자동 삭제된다. 테스트를 여러 번 새로 실행하면서 기존 `DeliveryApplication` 설정이 밀려났고, 다시 ▶로 실행하자 환경변수가 없는 새 임시 설정이 만들어졌다.
  - 실행 설정의 환경변수는 설정마다 따로 저장되기 때문에, 새 테스트 클래스를 실행할 때마다 다시 넣어야 하는 문제도 있었다.
- **해결 방안**
  - A. 실행 설정을 영구 저장(Save Configuration)하고 환경변수를 넣는다.
  - B. Windows 사용자 환경변수로 등록한다(`setx JWT_SECRET "값"`).
  - C. `application.yml`에 기본값을 둔다(`${JWT_SECRET:기본값}`).
  - D. `application-local.yml`에 값을 넣고 `.gitignore`에 추가한다.
- **결정 이유**: B를 선택했다.
  - A는 IntelliJ 실행에만 적용되고 터미널의 `gradlew bootRun`, `gradlew test`에는 적용되지 않는다. 테스트 실행 설정마다 반복해서 넣어야 한다.
  - B는 실행 방식(IntelliJ, 터미널, 테스트)과 실행 설정 삭제 여부에 상관없이 항상 적용된다.
  - C는 기본값이 곧 비밀키가 되어 GitHub에 올라간다. 과제 요구사항(비밀키를 GitHub에 올리지 않음)에 어긋난다.
  - D는 안전하지만 파일 관리와 프로파일 설정이 늘어난다. 개인 PC 하나에서 진행하는 과제에는 B로 충분하다.
  - **적용**
    - 32바이트 랜덤 값을 Base64로 만들어 `setx JWT_SECRET "값"`으로 등록.
    - `setx`는 이미 실행 중인 프로그램에 반영되지 않으므로 IntelliJ를 완전히 종료 후 재시작.
    - IntelliJ에서 앱 구동 확인.
- **주의**: 다른 PC에서 clone해 실행하려면 같은 환경변수 등록이 필요하다. README 실행 방법에 이 단계를 추가해야 한다.

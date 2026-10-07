# Delivery API (온보딩 개인 과제)

사장님(OWNER)이 메뉴를 올리고, 손님(CUSTOMER)이 주문·결제하고, 사장님이 주문을 처리하는 배달 주문 서비스의 백엔드 API.

## 작업 방식 (중요)
- 이 과제의 목적은 내가 직접 구현하며 배우는 것이다. 요청하지 않으면 전체 코드를 대신 작성하지 말고, 방향 설명·리뷰·힌트 위주로 도와줘.
- 코드를 직접 수정할 때는 무엇을 왜 바꾸는지 먼저 설명해줘.
- 설명은 한국어로.
- 설계 결정은 docs/decisions.md에, 트러블슈팅은 docs/troubleshooting.md에 기록한다. 기록할 만한 결정이나 문제 해결이 나오면 기록을 제안해줘.
- 커밋은 작은 단위로 나눈다. 여러 기능이 섞인 큰 변경이 생기면 나눠서 커밋하자고 알려줘.

## 기술 스택
- Java 21, Spring Boot 4.1.1, Gradle(Groovy)
- Spring Web, Spring Data JPA, PostgreSQL 18(Docker), Spring Security, Validation, Lombok, JJWT 0.12.6
- 패키지: com.example.delivery

## 명령어 (Windows PowerShell)
- DB 켜기: `docker start delivery-db`
- 빌드: `.\gradlew.bat build`
- 테스트: `.\gradlew.bat test`
- 실행: `.\gradlew.bat bootRun`

## 구조
- 도메인별 패키지(user, menu, order, payment) 안을 controller/service/repository/entity/dto로 나눈다.
- 공통은 global(config, security, entity/BaseEntity).
- 의존 방향: Controller → Service → Repository. Controller가 Repository를 직접 부르지 않는다.

## 필수 규칙
- 요청·응답은 DTO로. Entity를 그대로 응답하지 않는다(비밀번호 노출 금지).
- 연관관계 4개(메뉴→사장님, 주문→주문자, 주문→메뉴, 결제→주문)는 모두 @ManyToOne + FetchType.LAZY.
- enum은 @Enumerated(EnumType.STRING).
- 테이블명은 예약어 피하기: users, orders (user, order 금지).
- 모든 엔티티는 BaseEntity 상속(JPA Auditing, @EnableJpaAuditing 필요).
- 필수 컬럼 nullable = false, 아이디 unique = true.
- 메뉴 삭제는 Soft Delete. 삭제된 메뉴는 목록 제외, 단건·수정·주문 시 404.
- 금액(총액·결제 금액)은 항상 서버가 DB의 메뉴 가격으로 계산. 요청으로 받지 않는다.
- 사용자 정보(주문자, 메뉴 주인)는 요청 본문이 아니라 JWT 토큰에서 꺼낸다.
- 비밀번호는 BCrypt. JWT 비밀키는 32바이트 이상이며 GitHub에 올리지 않는다.
- open-in-view: false 이므로 Entity → DTO 변환은 Service 안에서 끝낸다.

## 주문 상태 흐름
ORDERED →(손님 결제)→ PAID →(사장님 수락)→ ACCEPTED →(사장님 완료)→ COMPLETED
ORDERED →(손님 취소, 결제 전만)→ CANCELED
이 외의 상태 변경은 거절한다(400 또는 409 중 하나로 일관되게).

## 알려진 함정
- SecurityConfig에서 `/error`를 permitAll 하지 않으면 모든 에러가 빈 403으로 나온다.
- JWT API이므로 CSRF는 disable.
- enum 값 추가 시 ddl-auto: update는 CHECK 제약을 안 고친다 → 한 번 create로 재생성.

## 커밋 규칙
- 커밋과 push는 내가 직접 터미널에서 한다. 요청하지 않으면 git commit, git push를 실행하지 마.
- 커밋 메시지를 요청하면 실제 변경 내용(git diff)을 보고 추천해줘.
- 형식: `타입: 내용` (한국어). 타입은 feat / fix / refactor / chore / docs / test.
  - 예: `feat: 회원가입 API 구현`, `fix: Security 설정에서 /error 경로 허용`
- 커밋은 작은 단위로. 여러 기능이 섞인 변경이면 나눠서 커밋하자고 알려줘.
- 커밋 메시지에 Co-Authored-By 같은 AI 표시를 넣지 마.
- 실제 비밀번호, JWT 비밀키가 커밋에 포함되려 하면 경고해줘.
# 🍱 Delivery API

사장님이 메뉴를 올리고, 손님이 주문·결제하고, 사장님이 주문을 처리하는 **배달 주문 서비스의 백엔드 API**입니다.

```
👤 회원·로그인 → 🍜 메뉴 → 🧾 주문 → 💳 결제 → ✅ 주문 처리
```

> 진행 중인 프로젝트입니다. 현재 진행 상황은 [체크리스트](docs/checklist.md)에서 확인할 수 있습니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1, Spring Web, Spring Data JPA, Spring Security, Validation |
| Database | PostgreSQL 18 (Docker) |
| Auth | JWT (JJWT 0.12.6), BCrypt |
| Build | Gradle |
| Etc | Lombok, Postman |

## 주요 기능

| 역할 | 할 수 있는 것 |
| --- | --- |
| 손님 (`CUSTOMER`) | 메뉴 조회 · 주문 생성 · 주문 취소 · 결제 · 내 주문 조회 |
| 사장님 (`OWNER`) | 메뉴 등록·수정·삭제 · 내 메뉴에 들어온 주문 조회 · 주문 상태 변경 |

### 주문 상태 흐름

```
ORDERED ──(손님 결제)──▶ PAID ──(사장님 수락)──▶ ACCEPTED ──(사장님 완료)──▶ COMPLETED
   │
   └──(손님 취소, 결제 전만)──▶ CANCELED
```

이 외의 상태 변경은 거절합니다(409).

## ERD

![ERD](docs/erd.png)

- 연관관계 4개는 모두 N:1이며 `@ManyToOne(fetch = LAZY)`로 매핑했습니다.
- 메뉴는 주문 기록 보존을 위해 Soft Delete(`is_deleted`)로 삭제합니다.
- 결제는 한 주문에 여러 기록이 쌓일 수 있는 구조로 두었습니다(결제 취소 후 재결제 대비).

자세한 테이블 명세는 [ERD 문서](docs/erd.md)를 참고하세요.

## API

| # | 기능 | Method · URL | 권한 |
| --- | --- | --- | --- |
| 1 | 회원가입 | `POST /api/users` | 누구나 |
| 2 | 로그인 | `POST /api/auth/login` | 누구나 |
| 3 | 메뉴 등록 | `POST /api/menus` | OWNER |
| 4 | 메뉴 목록 조회 | `GET /api/menus` | 누구나 |
| 5 | 메뉴 단건 조회 | `GET /api/menus/{menuId}` | 누구나 |
| 6 | 메뉴 수정 | `PUT /api/menus/{menuId}` | OWNER (본인) |
| 7 | 메뉴 삭제 | `DELETE /api/menus/{menuId}` | OWNER (본인) |
| 8 | 주문 생성 | `POST /api/orders` | CUSTOMER |
| 9 | 주문 목록 조회 | `GET /api/orders` | 로그인 |
| 10 | 주문 취소 | `PATCH /api/orders/{orderId}/cancel` | CUSTOMER (본인) |
| 11 | 주문 상태 변경 | `PATCH /api/orders/{orderId}/status` | OWNER (본인 메뉴) |
| 12 | 결제 | `POST /api/orders/{orderId}/payments` | CUSTOMER (본인) |

**상태 코드 기준**: 입력을 고쳐야 하면 `400`, 입력은 맞지만 현재 상태와 충돌하면 `409`

요청·응답 예시와 실패 코드는 [API 명세서](docs/api.md)를 참고하세요.

## 프로젝트 구조

도메인별로 패키지를 나누고, 그 안을 계층별로 나눴습니다.

```
src/main/java/com/example/delivery
├── global          # 공통 (Security·Password 설정, BaseEntity)
├── user            # 회원
├── menu            # 메뉴
├── order           # 주문
└── payment         # 결제
    ├── controller  # 요청·응답, @Valid 검증
    ├── service     # 비즈니스 규칙, 트랜잭션
    ├── repository  # DB 접근 (Spring Data JPA)
    ├── entity
    └── dto
        ├── request
        └── response
```

## 설계 결정

구현하며 고민한 지점과 그 이유를 [설계 결정 기록](docs/decisions.md)에 남기고 있습니다. 대표적인 결정은 다음과 같습니다.

- **주문 총액은 Order 엔티티가 생성 시점에 계산** — 클라이언트가 보낸 금액을 믿지 않고, 메뉴 가격이 바뀌어도 주문 당시 금액이 남도록 (D-04 → D-07)
- **아이디 길이 검증은 DTO, DB는 상한만** — 바뀔 수 있는 규칙은 코드에, DB는 최소한의 안전장치만 (D-03)
- **PasswordEncoder를 별도 설정으로 분리** — JWT 필터 도입 시 생길 수 있는 순환 참조 예방 (D-08)

## 실행 방법

**1. PostgreSQL 실행 (Docker)**

```bash
docker run --name delivery-db \
  -e POSTGRES_USER=delivery \
  -e POSTGRES_PASSWORD=delivery1234 \
  -e POSTGRES_DB=delivery \
  -p 5432:5432 -d postgres:18
```

이후에는 `docker start delivery-db`로 켭니다.

**2. JWT 비밀키 환경변수 등록 (처음 한 번)**

토큰 서명에 쓰는 비밀키를 환경변수 `JWT_SECRET`으로 등록합니다. 비밀키는 저장소에 포함하지 않으며, 등록하지 않으면 애플리케이션이 시작되지 않습니다.
값은 디코딩했을 때 32바이트 이상인 Base64 문자열이어야 합니다(HS256).

Windows (PowerShell)

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
setx JWT_SECRET "위에서_출력된_값"
```

macOS / Linux

```bash
echo "export JWT_SECRET=$(openssl rand -base64 32)" >> ~/.zshrc   # bash라면 ~/.bashrc
```

등록 후 터미널과 IDE를 **완전히 종료했다가 다시 실행**해야 적용됩니다.

**3. 애플리케이션 실행**

```bash
./gradlew bootRun
```

`http://localhost:8080`에서 실행됩니다. DB 계정은 로컬 연습용 값입니다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [ERD · 테이블 명세](docs/erd.md) | 테이블 구조와 컬럼 제약 |
| [API 명세](docs/api.md) | 12개 API의 요청·응답·실패 코드 |
| [설계 결정 기록](docs/decisions.md) | 고민한 선택지와 결정 이유 |
| [트러블슈팅](docs/troubleshooting.md) | 증상 → 원인 분석 → 해결 방안 → 결정 이유 |
| [체크리스트](docs/checklist.md) | 요구사항 진행 상황 |
| [커밋 규칙](docs/commit-convention.md) | 커밋 메시지 형식과 단위 |

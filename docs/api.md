# API 명세

## 공통

| 항목 | 내용 |
| --- | --- |
| Base URL | `http://localhost:8080` |
| 형식 | 요청·응답 모두 `application/json` |
| 인증 | 로그인이 필요한 요청은 `Authorization: Bearer {토큰}` 헤더 |
| 시각 형식 | `2026-10-07T14:05:00` (ISO-8601) |

### 상태 코드 규칙

| 코드 | 언제 |
| --- | --- |
| 200 OK | 조회·수정·상태 변경 성공 |
| 201 Created | 생성 성공 (회원가입, 메뉴 등록, 주문, 결제) |
| 204 No Content | 삭제 성공 |
| 400 Bad Request | **입력값이 잘못됨** (검증 실패, 허용되지 않는 값) |
| 401 Unauthorized | 로그인 실패 (아이디 없음, 비밀번호 틀림) |
| 403 Forbidden | 역할이 맞지 않거나 남의 자원에 접근. 토큰 없음·잘못된 토큰도 기본 설정에선 403 |
| 404 Not Found | 대상이 없음 (삭제된 메뉴 포함) |
| 409 Conflict | **입력은 맞지만 지금 상태와 충돌** (중복 아이디, 이미 결제된 주문 결제 등) |

> 기준: 입력을 고쳐야 하면 400, 입력은 맞는데 지금 상태와 안 맞으면 409.

### 에러 응답 예시

```json
{
  "timestamp": "2026-10-07T14:05:00.000+09:00",
  "status": 409,
  "error": "Conflict",
  "path": "/api/orders/1/payments"
}
```

---

## 전체 목록

| # | 기능 | Method · URL | 권한 | 성공 |
| --- | --- | --- | --- | --- |
| 1 | 회원가입 | `POST /api/users` | 누구나 | 201 |
| 2 | 로그인 | `POST /api/auth/login` | 누구나 | 200 |
| 3 | 메뉴 등록 | `POST /api/menus` | OWNER | 201 |
| 4 | 메뉴 목록 조회 | `GET /api/menus` | 누구나 | 200 |
| 5 | 메뉴 단건 조회 | `GET /api/menus/{menuId}` | 누구나 | 200 |
| 6 | 메뉴 수정 | `PUT /api/menus/{menuId}` | OWNER (본인 메뉴) | 200 |
| 7 | 메뉴 삭제 | `DELETE /api/menus/{menuId}` | OWNER (본인 메뉴) | 204 |
| 8 | 주문 생성 | `POST /api/orders` | CUSTOMER | 201 |
| 9 | 주문 목록 조회 | `GET /api/orders` | 로그인한 사용자 (역할별로 다른 목록) | 200 |
| 10 | 주문 취소 | `PATCH /api/orders/{orderId}/cancel` | CUSTOMER (본인 주문) | 200 |
| 11 | 주문 상태 변경 | `PATCH /api/orders/{orderId}/status` | OWNER (본인 메뉴 주문) | 200 |
| 12 | 결제 | `POST /api/orders/{orderId}/payments` | CUSTOMER (본인 주문) | 201 |

---

## 👤 회원

### 1. 회원가입

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /api/users` |
| 권한 | 누구나 |
| 성공 | `201 Created` |
| 실패 | `400` 값 누락·길이 위반·잘못된 역할 · `409` 이미 있는 아이디 |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `username` | String | O | 아이디 (4~20자) |
| `password` | String | O | 비밀번호 (8자 이상) |
| `role` | String | O | `CUSTOMER` 또는 `OWNER` |

**요청 예시**

```
POST /api/users
Content-Type: application/json

{
  "username": "owner1",
  "password": "password1234",
  "role": "OWNER"
}
```

**응답 예시** — `201 Created`

```json
{
  "userId": 1,
  "username": "owner1",
  "role": "OWNER",
  "createdAt": "2026-10-07T14:05:00"
}
```

> 비밀번호는 BCrypt로 해시해서 저장하고, 응답에는 담지 않는다.

### 2. 로그인

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /api/auth/login` |
| 권한 | 누구나 |
| 성공 | `200 OK` |
| 실패 | `400` 값 누락 · `401` 아이디 없음 또는 비밀번호 틀림 |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `username` | String | O | 아이디 |
| `password` | String | O | 비밀번호 |

**요청 예시**

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "owner1",
  "password": "password1234"
}
```

**응답 예시** — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

> 토큰에는 아이디·역할·만료 시간만 담는다. 비밀번호 같은 민감정보는 담지 않는다.
> 아이디가 없는 경우와 비밀번호가 틀린 경우를 구분하지 않고 같은 401로 응답한다(어떤 아이디가 존재하는지 노출하지 않기 위해).

---

## 🍜 메뉴

### 3. 메뉴 등록

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /api/menus` |
| 권한 | OWNER |
| 성공 | `201 Created` |
| 실패 | `400` 이름 없음·가격 1원 미만 · `403` 토큰 없음 또는 CUSTOMER |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `name` | String | O | 메뉴 이름 |
| `price` | Long | O | 가격 (1원 이상) |
| `description` | String | X | 메뉴 설명 |

**요청 예시**

```
POST /api/menus
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "name": "김밥",
  "price": 3000,
  "description": "참치김밥"
}
```

**응답 예시** — `201 Created`

```json
{
  "menuId": 1,
  "name": "김밥",
  "price": 3000,
  "description": "참치김밥",
  "owner": "owner1",
  "createdAt": "2026-10-07T14:10:00",
  "updatedAt": "2026-10-07T14:10:00"
}
```

> 메뉴의 주인은 요청 본문이 아니라 토큰에서 꺼낸다.

### 4. 메뉴 목록 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /api/menus` |
| 권한 | 누구나 (토큰 불필요) |
| 성공 | `200 OK` |

**응답 예시** — `200 OK`

```json
[
  {
    "menuId": 1,
    "name": "김밥",
    "price": 3000,
    "description": "참치김밥",
    "owner": "owner1",
    "createdAt": "2026-10-07T14:10:00",
    "updatedAt": "2026-10-07T14:10:00"
  }
]
```

> 삭제된 메뉴(`is_deleted = true`)는 제외한다.

### 5. 메뉴 단건 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /api/menus/{menuId}` |
| 권한 | 누구나 (토큰 불필요) |
| 성공 | `200 OK` |
| 실패 | `404` 없거나 삭제된 메뉴 |

**요청 예시**

```
GET /api/menus/1
```

**응답 예시** — `200 OK`: 메뉴 등록 응답과 같은 형식

### 6. 메뉴 수정

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PUT /api/menus/{menuId}` |
| 권한 | OWNER (본인 메뉴만) |
| 성공 | `200 OK` |
| 실패 | `400` 값 검증 실패 · `403` 다른 사장님의 메뉴 또는 CUSTOMER · `404` 없거나 삭제된 메뉴 |

**요청 필드**: 메뉴 등록과 같음 (이름·가격·설명을 모두 보내 통째로 교체)

**요청 예시**

```
PUT /api/menus/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "name": "김밥",
  "price": 3500,
  "description": "참치김밥"
}
```

**응답 예시** — `200 OK`

```json
{
  "menuId": 1,
  "name": "김밥",
  "price": 3500,
  "description": "참치김밥",
  "owner": "owner1",
  "createdAt": "2026-10-07T14:10:00",
  "updatedAt": "2026-10-07T14:20:00"
}
```

> `updatedAt`은 JPA Auditing으로 자동 갱신된다.
> `description`을 보내지 않으면 설명이 비워진다(PUT은 전체 교체).

### 7. 메뉴 삭제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `DELETE /api/menus/{menuId}` |
| 권한 | OWNER (본인 메뉴만) |
| 성공 | `204 No Content` (본문 없음) |
| 실패 | `403` 다른 사장님의 메뉴 또는 CUSTOMER · `404` 없거나 이미 삭제된 메뉴 |

**요청 예시**

```
DELETE /api/menus/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

> 실제로 지우지 않고 `is_deleted = true`로 표시만 한다(Soft Delete). 이 메뉴로 들어온 주문 기록은 그대로 남는다.

---

## 🧾 주문

### 8. 주문 생성

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /api/orders` |
| 권한 | CUSTOMER |
| 성공 | `201 Created` |
| 실패 | `400` 수량 1 미만·주소 없음 · `403` 토큰 없음 또는 OWNER · `404` 없거나 삭제된 메뉴 |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `menuId` | Long | O | 주문할 메뉴 |
| `quantity` | Long | O | 수량 (1 이상) |
| `address` | String | O | 배송 주소 |

**요청 예시**

```
POST /api/orders
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "menuId": 1,
  "quantity": 2,
  "address": "서울시 강남구 테헤란로 1"
}
```

**응답 예시** — `201 Created`

```json
{
  "orderId": 1,
  "menuId": 1,
  "menuName": "김밥",
  "customer": "cust1",
  "quantity": 2,
  "totalPrice": 7000,
  "address": "서울시 강남구 테헤란로 1",
  "status": "ORDERED",
  "createdAt": "2026-10-07T14:30:00",
  "updatedAt": "2026-10-07T14:30:00"
}
```

> 금액은 요청으로 받지 않는다. 총액 = 메뉴 가격 × 수량을 Service에서 계산해 저장한다.
> 주문자는 토큰에서 꺼내고, 처음 상태는 `ORDERED`.

### 9. 주문 목록 조회

| 항목 | 내용 |
| --- | --- |
| Method · URL | `GET /api/orders` |
| 권한 | 로그인한 사용자 |
| 성공 | `200 OK` |
| 실패 | `403` 토큰 없음 |

**역할별 결과**

| 역할 | 보이는 주문 |
| --- | --- |
| CUSTOMER | 본인이 한 주문 |
| OWNER | 본인 메뉴에 들어온 주문 |

**응답 예시** — `200 OK`: 주문 생성 응답과 같은 형식의 배열

```json
[
  {
    "orderId": 1,
    "menuId": 1,
    "menuName": "김밥",
    "customer": "cust1",
    "quantity": 2,
    "totalPrice": 7000,
    "address": "서울시 강남구 테헤란로 1",
    "status": "PAID",
    "createdAt": "2026-10-07T14:30:00",
    "updatedAt": "2026-10-07T14:35:00"
  }
]
```

> 삭제된 메뉴로 들어온 주문도 그대로 보인다.

### 10. 주문 취소

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PATCH /api/orders/{orderId}/cancel` |
| 권한 | CUSTOMER (본인 주문만) |
| 성공 | `200 OK` |
| 실패 | `403` 다른 손님의 주문 또는 OWNER · `404` 없는 주문 · `409` `ORDERED`가 아닌 주문 (결제됨, 이미 취소됨 등) |

**요청 예시** (본문 없음)

```
PATCH /api/orders/2/cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**응답 예시** — `200 OK`: 주문 생성 응답과 같은 형식, `status`가 `CANCELED`

### 11. 주문 상태 변경

| 항목 | 내용 |
| --- | --- |
| Method · URL | `PATCH /api/orders/{orderId}/status` |
| 권한 | OWNER (본인 메뉴에 들어온 주문만) |
| 성공 | `200 OK` |
| 실패 | `400` 값 누락·존재하지 않는 상태 값 · `403` 다른 사장님 메뉴의 주문 또는 CUSTOMER · `404` 없는 주문 · `409` 허용되지 않는 상태 변경 |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | String | O | 바꿀 상태: `ACCEPTED` 또는 `COMPLETED` |

**허용되는 변경 (그 외는 409)**

| 현재 상태 | → 바꿀 상태 |
| --- | --- |
| `PAID` | `ACCEPTED` |
| `ACCEPTED` | `COMPLETED` |

**요청 예시**

```
PATCH /api/orders/1/status
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "status": "ACCEPTED"
}
```

**응답 예시** — `200 OK`: 주문 생성 응답과 같은 형식, `status`가 `ACCEPTED`

> `"HELLO"`처럼 상태 값 자체가 없으면 입력 오류라 400, `"CANCELED"`처럼 값은 있지만 사장님이 바꿀 수 없는 상태면 409.

---

## 💳 결제

### 12. 결제

| 항목 | 내용 |
| --- | --- |
| Method · URL | `POST /api/orders/{orderId}/payments` |
| 권한 | CUSTOMER (본인 주문만) |
| 성공 | `201 Created` |
| 실패 | `400` 결제 수단 누락·카드 외 값 · `403` 다른 손님의 주문 또는 OWNER · `404` 없는 주문 · `409` `ORDERED`가 아닌 주문 (이미 결제됨, 취소됨 등) |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `method` | String | O | 결제 수단: `CARD` |

**요청 예시**

```
POST /api/orders/1/payments
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "method": "CARD"
}
```

**응답 예시** — `201 Created`

```json
{
  "paymentId": 1,
  "orderId": 1,
  "amount": 7000,
  "method": "CARD",
  "status": "COMPLETED",
  "orderStatus": "PAID",
  "createdAt": "2026-10-07T14:35:00"
}
```

> 결제 금액은 요청으로 받지 않고 주문의 `totalPrice`를 그대로 쓴다.
> 성공하면 결제 내역을 저장하고 주문 상태를 `PAID`로 바꾼다.

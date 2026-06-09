# 會員 API 規格

## API 呼叫順序

1. *前端呼叫 `POST /api/auth/register` 建立未開通帳號。*
2. *使用者從 Email 點擊 `GET /api/auth/activate?token=...` 完成開通。*
3. *前端呼叫 `POST /api/auth/login` 驗證 Email 與密碼，成功後取得 `challengeId`。*
4. *前端呼叫 `POST /api/auth/2fa/verify` 驗證 Email 二階段驗證碼，成功後取得 JWT。*
5. *前端帶 `Authorization: Bearer <jwt>` 呼叫 `GET /api/users/me/last-login` 查詢本人最後登入時間。*

## POST /api/auth/register

*狀態：已實作。*

### Request

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd123"
}
```

### Success Response

*HTTP status：`201 Created`*

```json
{
  "message": "REGISTRATION_CREATED",
  "email": "user@example.com"
}
```

### Error Response

*Email 已被註冊：`409 Conflict`*

```json
{
  "code": "EMAIL_ALREADY_REGISTERED",
  "message": "Email is already registered."
}
```

## GET /api/auth/activate?token=...

*狀態：待實作。*

### Success Response

*HTTP status：`200 OK`*

```json
{
  "message": "ACCOUNT_ACTIVATED"
}
```

## POST /api/auth/login

*狀態：待實作。*

### Request

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd123"
}
```

### Success Response

*HTTP status：`200 OK`*

```json
{
  "message": "TWO_FACTOR_REQUIRED",
  "challengeId": "22222222-2222-2222-2222-222222222222",
  "expiresAt": "2026-06-09T08:10:00Z"
}
```

## POST /api/auth/2fa/verify

*狀態：待實作。*

### Request

```json
{
  "challengeId": "22222222-2222-2222-2222-222222222222",
  "code": "123456"
}
```

### Success Response

*HTTP status：`200 OK`*

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "expiresIn": 3600
}
```

## GET /api/users/me/last-login

*狀態：待實作。*

### Request Header

```http
Authorization: Bearer <jwt>
```

### Success Response

*HTTP status：`200 OK`*

```json
{
  "email": "user@example.com",
  "lastLoginAt": "2026-06-09T08:05:30Z"
}
```

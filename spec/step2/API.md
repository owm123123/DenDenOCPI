# 會員 API 規格

## API 呼叫順序

1. *前端呼叫 `POST /api/auth/register` 建立未開通帳號。*
2. *使用者從 Email 點擊 `GET /api/auth/activate?token=...` 完成開通。*
3. *前端呼叫 `POST /api/auth/login` 驗證 Email 與密碼，成功後取得 `challengeId`。*
4. *前端呼叫 `POST /api/auth/2fa/verify` 驗證 Email 二階段驗證碼，成功後取得 JWT。*
5. *前端帶 `Authorization: Bearer <jwt>` 呼叫 `GET /api/users/me/last-login` 查詢本人最後登入時間。*

## Email 寄送服務

*正式 Email provider 選用 Mailjet API。*

*預設設定為 `app.email.provider=in-memory`，本機開發與測試不會呼叫外部寄信服務。若要改用 Mailjet，需設定 `app.email.provider=mailjet`，並透過環境變數提供以下值：*

- *`MAILJET_API_KEY`*
- *`MAILJET_API_SECRET`*
- *`MAILJET_SENDER_EMAIL`*
- *`MAILJET_SENDER_NAME`*
- *`APP_EMAIL_ACTIVATION_BASE_URL`*

*Email 寄送不是獨立 API，而是 auth 流程的內部基礎能力：*

- *`POST /api/auth/register` 成功後寄送 Email 開通信。*
- *`POST /api/auth/login` 帳密驗證成功後寄送 Email 二階段驗證碼。*

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

*狀態：已實作。*

### Request

*使用者從 Email 點擊開通連結，token 以 query string 傳入。*

```http
GET /api/auth/activate?token=<activation-token>
```

### Success Response

*HTTP status：`200 OK`*

```json
{
  "message": "ACCOUNT_ACTIVATED"
}
```

### Error Response

*Token 不存在、已過期或已使用：`400 Bad Request`*

```json
{
  "code": "INVALID_ACTIVATION_TOKEN",
  "message": "Activation token is invalid or expired."
}
```

## POST /api/auth/login

*狀態：已實作。*

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

### Error Response

*Email 不存在或密碼錯誤：`401 Unauthorized`*

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Email or password is invalid."
}
```

*帳號尚未完成 Email 開通：`403 Forbidden`*

```json
{
  "code": "ACCOUNT_NOT_ACTIVATED",
  "message": "Account is not activated."
}
```

## POST /api/auth/2fa/verify

*狀態：已實作。*

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

### Error Response

*Challenge 不存在、已驗證或驗證碼錯誤：`400 Bad Request`*

```json
{
  "code": "INVALID_TWO_FACTOR_CODE",
  "message": "Two-factor verification code is invalid."
}
```

### JWT 設定

*Access token 使用 HMAC SHA-256 簽章。正式環境需透過環境變數設定：*

- *`APP_JWT_ISSUER`：建議使用 URI，例如 `http://localhost:8080`。*
- *`APP_JWT_SECRET`：至少 32 字元，不可提交真實 secret。*
- *`APP_JWT_ACCESS_TOKEN_EXPIRES_IN`：預設 `PT1H`。*

*驗證碼已過期：`400 Bad Request`*

```json
{
  "code": "TWO_FACTOR_CODE_EXPIRED",
  "message": "Two-factor verification code is expired."
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

# 會員 API 規格

## API 呼叫順序

1. *前端呼叫 `POST /api/auth/register` 建立未開通帳號。*
2. *使用者從 Email 點擊前端開通確認頁，前端呼叫 `POST /api/auth/activate` 完成開通。*
3. *前端呼叫 `POST /api/auth/login` 驗證 Email 與密碼，成功後取得 `challengeId`。*
4. *前端呼叫 `POST /api/auth/2fa/verify` 驗證 Email 二階段驗證碼，成功後取得 JWT。*
5. *前端帶 `Authorization: Bearer <jwt>` 呼叫 `GET /api/users/last-login` 查詢本人最後登入時間。*

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

## 全域錯誤格式

*所有已定義的 API 錯誤 response 使用一致 JSON 格式。`code` 是穩定、可測試的機器可讀錯誤碼；`message` 是給人閱讀的固定說明，不直接暴露 raw exception message。*

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message."
}
```

### Common Error Response

*Request body 欄位驗證失敗：`400 Bad Request`*

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed."
}
```

*Request body 缺失、JSON 格式錯誤或無法解析：`400 Bad Request`*

```json
{
  "code": "INVALID_REQUEST",
  "message": "Request body is invalid."
}
```

*未帶 JWT、JWT 過期、JWT 簽章錯誤或格式錯誤：`401 Unauthorized`*

```json
{
  "code": "UNAUTHENTICATED",
  "message": "Authentication is required or invalid."
}
```

*未預期錯誤：`500 Internal Server Error`*

```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred."
}
```

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

## POST /api/auth/activate

*狀態：已實作。*

### Request

*Email 內容中的開通連結應導向前端確認頁，例如 `https://frontend.example.com/activate?activationToken=...`。使用者確認後，由前端呼叫後端 API，將 `activationToken` 放在 request body。*

```http
POST /api/auth/activate
Content-Type: application/json
```

```json
{
  "activationToken": "activation-token-from-email"
}
```

### Success Response

*HTTP status：`200 OK`*

```json
{
  "message": "ACCOUNT_ACTIVATED"
}
```

### Error Response

*`activationToken` 不存在、已過期或已使用：`400 Bad Request`*

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

*此階段代表帳密驗證通過，並已建立 Email 二階段驗證 challenge。這不代表正式登入成功；後端不會在此階段簽發 JWT，也不會更新 `lastLoginAt`。*

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

*此階段才視為正式登入成功。後端會簽發 JWT access token，並更新會員的 `lastLoginAt`。*

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

*驗證碼已過期：`400 Bad Request`*

```json
{
  "code": "TWO_FACTOR_CODE_EXPIRED",
  "message": "Two-factor verification code is expired."
}
```

### JWT 設定

*Access token 使用 HMAC SHA-256 簽章。正式環境需透過環境變數設定：*

- *`APP_JWT_ISSUER`：建議使用 URI，例如 `http://localhost:8080`。*
- *`APP_JWT_SECRET`：至少 32 字元，不可提交真實 secret。*
- *`APP_JWT_ACCESS_TOKEN_EXPIRES_IN`：預設 `PT1H`。*

## GET /api/users/last-login

*狀態：已實作。*

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

### Error Response

*JWT subject 對應不到會員：`404 Not Found`*

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User is not found."
}
```

*此 API 不接受 user id 或 email query parameter；後端只會從 JWT subject 判斷目前使用者，因此前端無法指定查詢其他會員。*

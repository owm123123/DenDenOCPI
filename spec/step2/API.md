# 會員 API 規格

## API 呼叫順序

1. *前端呼叫 `POST /api/auth/register` 建立未開通帳號。*
2. *使用者從 Email 點擊前端開通確認頁，前端呼叫 `POST /api/auth/activate` 完成開通。*
3. *前端呼叫 `POST /api/auth/login` 驗證 Email 與密碼，成功後取得 `challengeId`。*
4. *前端呼叫 `POST /api/auth/2fa/verify` 驗證 Email 二階段驗證碼，成功後取得 JWT。*
5. *前端帶 `Authorization: Bearer <jwt>` 呼叫 `GET /api/users/last-login` 查詢本人最後登入時間。*

## Swagger / OpenAPI

*狀態：已實作。*

*本機啟動 Spring Boot 後，可使用以下網址檢視與測試 API：*

- *Swagger UI：`http://localhost:8080/swagger-ui.html`*
- *OpenAPI JSON：`http://localhost:8080/v3/api-docs`*

*受保護 API 使用 Swagger UI 右上角 `Authorize` 輸入 JWT。登入二階段驗證成功後取得的 token 只需要貼 access token 本體，不需要手動加 `Bearer ` 前綴。*

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

*本機 Mailjet 測試狀態：Mailjet 帳號暫時封鎖問題已處理完成；後續可使用上述環境變數切換 `app.email.provider=mailjet`，實測註冊開通信與 Email 二階段驗證碼寄送。*

## 全域錯誤格式

*所有已定義的 API 錯誤 response 使用一致 JSON 格式。`code` 是穩定、可測試的機器可讀錯誤碼；`message` 是給人閱讀的固定說明，不直接暴露 raw exception message。*

*欄位驗證錯誤會額外回傳 `fieldErrors`，讓前端可以標示到對應欄位；非欄位驗證錯誤不回傳此欄位。*

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message.",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

### Common Error Response

*Request body 欄位驗證失敗：`400 Bad Request`*

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    },
    {
      "field": "password",
      "message": "size must be between 8 and 72"
    }
  ]
}
```

*Request body 缺失、JSON 格式錯誤或無法解析：`400 Bad Request`*

```json
{
  "code": "INVALID_REQUEST",
  "message": "Request body is invalid."
}
```

*未帶 JWT：`401 Unauthorized`*

```json
{
  "code": "UNAUTHENTICATED",
  "message": "Authentication is required or invalid."
}
```

*JWT 過期：`401 Unauthorized`*

```json
{
  "code": "TOKEN_EXPIRED",
  "message": "Access token is expired."
}
```

*JWT 簽章錯誤：`401 Unauthorized`*

```json
{
  "code": "INVALID_TOKEN_SIGNATURE",
  "message": "Access token signature is invalid."
}
```

*JWT 格式錯誤或無法解析：`401 Unauthorized`*

```json
{
  "code": "INVALID_TOKEN",
  "message": "Access token is invalid."
}
```

*Email provider 寄送失敗：`502 Bad Gateway`*

```json
{
  "code": "EMAIL_DELIVERY_FAILED",
  "message": "Email delivery failed. Please try again later."
}
```

*未預期錯誤：`500 Internal Server Error`*

```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred."
}
```

### Error Code / HTTP Status 對照

| *Error Code* | *HTTP Status* | *使用情境* |
| --- | --- | --- |
| *`VALIDATION_ERROR`* | *`400 Bad Request`* | *Request 欄位驗證失敗。* |
| *`INVALID_REQUEST`* | *`400 Bad Request`* | *Request body 缺失、JSON 格式錯誤或無法解析。* |
| *`EMAIL_ALREADY_REGISTERED`* | *`409 Conflict`* | *註冊 Email 已存在。* |
| *`INVALID_ACTIVATION_TOKEN`* | *`400 Bad Request`* | *開通 token 不存在、已過期或已使用。* |
| *`INVALID_CREDENTIALS`* | *`401 Unauthorized`* | *Email 不存在或密碼錯誤。* |
| *`ACCOUNT_NOT_ACTIVATED`* | *`403 Forbidden`* | *帳密正確但帳號尚未完成 Email 開通。* |
| *`INVALID_TWO_FACTOR_CODE`* | *`400 Bad Request`* | *Challenge 不存在、已驗證或二階段驗證碼錯誤。* |
| *`TWO_FACTOR_CODE_EXPIRED`* | *`400 Bad Request`* | *二階段驗證碼已過期。* |
| *`UNAUTHENTICATED`* | *`401 Unauthorized`* | *未帶 JWT 或 authentication 不存在。* |
| *`TOKEN_EXPIRED`* | *`401 Unauthorized`* | *JWT 已過期。* |
| *`INVALID_TOKEN_SIGNATURE`* | *`401 Unauthorized`* | *JWT 簽章錯誤。* |
| *`INVALID_TOKEN`* | *`401 Unauthorized`* | *JWT 格式錯誤或無法解析。* |
| *`USER_NOT_FOUND`* | *`404 Not Found`* | *JWT `sub` 合法，但 DB 找不到對應會員。* |
| *`EMAIL_DELIVERY_FAILED`* | *`502 Bad Gateway`* | *Email provider 寄送失敗。* |
| *`INTERNAL_ERROR`* | *`500 Internal Server Error`* | *未預期錯誤；server log 保留細節，response 不暴露 raw exception message。* |

### Authorization Boundary

*目前唯一受保護 API 是 `GET /api/users/last-login`，屬於查詢本人資料的 `/me` 類型 API。此 API 不接受 user id、public id 或 email query parameter，因此 client 無法指定查詢其他會員。*

*因此目前沒有 authenticated-but-forbidden 的實際 API 場景，也就是「已登入，但因角色或資源所有權不足而被拒絕」的情境尚不存在。若未來新增 admin API、角色權限、或跨使用者資源存取 API，才會正式引入通用 `FORBIDDEN` 錯誤碼。*

*目前 `403 Forbidden` 僅用於明確的業務狀態錯誤：`ACCOUNT_NOT_ACTIVATED`。*

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

*Response header：不回傳 `Location`。註冊成功後建立的是尚未開通帳號，目前未提供公開 user resource URL，因此不使用 `Location` 指向新資源。*

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

*2FA 錯誤狀態目前不再進一步拆分。原因是 challenge 不存在、已驗證或驗證碼錯誤都屬於驗證失敗情境，若回傳過細的狀態，client 或攻擊者可以更容易推測 challenge 是否存在或是否已被使用。*

*目前只保留「驗證碼已過期」作為獨立錯誤碼，因為這是前端最需要明確引導使用者重新登入並取得新驗證碼的情境。其他驗證失敗則統一回 `INVALID_TWO_FACTOR_CODE`。*

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

### JWT Claims Contract

*Access token 目前使用以下 claims：*

- *`sub`：會員的 `users.public_id`，格式為 UUID 字串。這是對外穩定識別碼，不使用可遞增的內部 `users.id`，也不使用可能變更的 Email。*
- *`iss`：必須等於 `APP_JWT_ISSUER`。*
- *`iat`：token 簽發時間，必填。*
- *`exp`：token 過期時間，必填。*
- *`email`：簽發當下的會員 Email，只供 client 顯示或除錯輔助，不作為授權或 DB 查詢依據。*
- *`type`：目前固定為 `access`。*

*後端驗證 JWT 簽章、issuer 與 expiration 後，使用 `sub` 查詢 DB。會員最新狀態與 `lastLoginAt` 一律以 DB 為準，不從 JWT claim 直接回傳。*

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

*未帶 JWT、JWT 過期、JWT 簽章錯誤或 JWT 格式錯誤：`401 Unauthorized`，錯誤格式與錯誤碼請參考「全域錯誤格式」的 authentication failure 規格。*

*JWT `sub` 對應不到會員：`404 Not Found`*

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User is not found."
}
```

*此 API 不接受 user id、public id 或 email query parameter；後端只會從 JWT `sub` 判斷目前使用者，因此前端無法指定查詢其他會員。*

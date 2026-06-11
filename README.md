# DenDenOCPI

## 交付內容

- *Java 21 / Spring Boot 4 會員驗證 API。*
- *PostgreSQL schema migration，使用 Flyway 管理資料庫結構。*
- *Swagger / OpenAPI 文件，可直接用瀏覽器測試 API。*
- *Mailjet / SendGrid Email provider adapter，用於正式展示環境寄送開通信與二階段驗證碼。*
- *本機開發預設使用 in-memory email sender，方便不依賴外部寄信服務驗證主要流程。*

## Swagger 測試入口

*GCP 部署完成後，Swagger UI 入口會是：*

- *`https://<cloud-run-url>/swagger-ui.html`*

*本機啟動方式與環境設定請看 [DEVOPS.md](spec/step2/DEVOPS.md)。若用本機啟動，Swagger UI 預設是：*

- *`http://localhost:8080/swagger-ui.html`*

## Swagger API 測試流程

1. *開啟 Swagger UI。*
2. *呼叫 `POST /api/auth/register` 建立帳號。*
3. *到註冊 Email 收開通信；如果沒有看到，請先檢查垃圾郵件。*
4. *從開通信中的連結取得 `activationToken`，在 Swagger 呼叫 `POST /api/auth/activate` 完成開通。*
5. *呼叫 `POST /api/auth/login`，輸入 Email 與密碼。成功後會取得 `challengeId`，此時尚未正式登入。*
6. *到 Email 收二階段驗證碼；如果沒有看到，請先檢查垃圾郵件。*
7. *呼叫 `POST /api/auth/2fa/verify`，輸入 `challengeId` 與 Email 驗證碼。成功後會取得 JWT access token。*
8. *點 Swagger UI 右上角 `Authorize`，貼上 access token 本體，不需要手動加 `Bearer `。*
9. *呼叫 `GET /api/users/last-login`，查詢本人最後登入時間。*

## API 摘要

- *`POST /api/auth/register`：註冊未開通帳號，寄送 Email 開通信。*
- *`POST /api/auth/activate`：使用 `activationToken` 完成帳號開通。*
- *`POST /api/auth/login`：驗證 Email 與密碼，建立 Email 二階段驗證 challenge。*
- *`POST /api/auth/2fa/verify`：驗證 Email 二階段驗證碼，成功後簽發 JWT。*
- *`GET /api/users/last-login`：使用 JWT 查詢本人最後登入時間。*

*完整 API contract 請看 [spec/step2/API.md](spec/step2/API.md)。*

## 簡化假設

- *本題後端只實作 API，不實作前端頁面。Email 開通連結設計為導向前端確認頁，再由前端呼叫 `POST /api/auth/activate`；面試測試時可直接從 Email 取得 token 後用 Swagger 呼叫。*
- *本機開發預設 `app.email.provider=in-memory`，不會真的寄信；正式展示環境可使用 SendGrid 或 Mailjet。*
- *Email 可能進入垃圾郵件，測試開通信與二階段驗證碼時請一併檢查。*
- *目前沒有 admin API、角色權限或跨使用者資料查詢 API；唯一受保護 API 是查詢本人最後登入時間。*

## 補充文件

- *API 規格：[spec/step2/API.md](spec/step2/API.md)。*
- *資料庫設計：[spec/step2/DATABASE.md](spec/step2/DATABASE.md)。*
- *開發與部署操作：[DEVOPS.md](spec/step2/DEVOPS.md)。*
- *開發紀錄：[spec/step2/RECORD.md](spec/step2/RECORD.md)。*

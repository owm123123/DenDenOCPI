# 後端作業題型 2026 TODO List

來源需求：`spec/後端作業題型2026.md`

## 交付總覽

- [ ] *完成 OCPI 2.2.1 概念流程時序圖。*
- [ ] *完成 Java Spring Boot 會員註冊、開通、登入、Email 二階段驗證與最後登入時間查詢 API。*
- [x] *提供 Swagger 或 Postman collection 作為 API 測試文件。*
- [x] *整理 README，讓面試官可以快速啟動、測試與理解簡化假設。*
- [ ] *確認 GitHub repository 內容完整且可審查。*

## 第一部分：OCPI 時序圖

- [ ] *確認角色定義：User、SCSP、EMSP、CPO。*
- [ ] *補充簡化假設：題目允許忽略 token 認證流程。*
- [ ] *繪製 User 透過 SCSP 發起啟動充電的流程。*
- [ ] *標示 SCSP → EMSP：Start Session command。*
- [ ] *標示 EMSP → CPO：CommandForward 或 `POST /commands/START_SESSION`。*
- [ ] *標示 CPO → EMSP：Session 狀態更新 `PENDING → ACTIVE`。*
- [ ] *繪製 User 透過 SCSP 發起停止充電的流程。*
- [ ] *標示 SCSP → EMSP：Stop Session command。*
- [ ] *標示 EMSP → CPO：CommandForward 或 `POST /commands/STOP_SESSION`。*
- [ ] *標示 CPO → EMSP：Session 狀態更新為 `COMPLETED`。*
- [ ] *標示 CPO → EMSP：回傳 CDR。*
- [ ] *標示 EMSP → SCSP：帳單結果通知。*
- [ ] *標示 SCSP → User：顯示帳單結果。*
- [ ] *檢查圖中是否包含主要 API 名稱、response 與狀態轉換。*
- [ ] *將 Mermaid.js 或 UML 成品放到適合的文件中，例如 README 或 `spec/ocpi-sequence-diagram.md`。*

## 第二部分：會員 API 共用基礎建設

- [x] *確認實際 build file 使用的 Java、Spring Boot、Maven dependency 與測試工具版本。*
- [x] *確認使用 Flyway 管理 schema migration。*
- [x] *新增或調整 users table。*
- [x] *新增必要欄位：Email、password hash、activation status、last login time。*
- [x] *新增開通 token 與二階段驗證碼相關資料表。*
- [x] *確認 Email 有唯一約束。*
- [x] *確認必要欄位有 NOT NULL 約束。*
- [x] *確認 token、code、過期時間有可追蹤欄位。*
- [x] *新增必要 index，例如 Email、token lookup 或驗證流程查詢欄位。*
- [x] *確認測試環境可使用 H2 in-memory database，不依賴 Docker 或 PostgreSQL。*
- [x] *建立會員資料模型，支援 Email、密碼雜湊、帳號開通狀態與最後登入時間。*
- [x] *建立 User、Email activation token、login two-factor code entity。*
- [x] *建立會員驗證流程需要的 repository。*
- [x] *建立集中式 exception handling。*
- [x] *統一錯誤 response 格式。*
- [x] *確認錯誤 response 不直接暴露 exception message。*
- [x] *測試時優先驗證 HTTP status 與穩定錯誤碼。*
- [x] *測試環境使用 fake 或 mock email sender。*
- [x] *確認不 log 密碼、token、驗證碼或敏感個資。*
- [x] *選定正式 Email provider：Mailjet、SendGrid、Mailtrap 或同類型服務。*
- [x] *Email API key、secret、SMTP 密碼一律使用環境變數或本機設定注入。*
- [x] *文件範例只能使用 placeholder，不 hardcode 真實 secret。*
- [x] *Mailjet 帳號暫時封鎖問題已處理完成，可切換 `app.email.provider=mailjet` 進行本機寄信驗證。*
- [x] *建立 JWT 簽發、驗證與 Spring Security 整合。*

## API 1：註冊 `POST /api/auth/register`

- [x] *在 API 文件定義 request、success response 與 error response。*
- [x] *建立 request / response DTO，且 API response 不直接回傳 Entity。*
- [x] *建立 controller endpoint。*
- [x] *Controller 只負責 request validation、呼叫 service 與回傳 response。*
- [x] *建立 registration service flow。*
- [x] *註冊時檢查 Email 不可重複。*
- [x] *支援 `EMAIL_ALREADY_REGISTERED`。*
- [x] *註冊時使用 password encoder 儲存安全雜湊後的密碼。*
- [x] *註冊成功後產生 Email 開通 token。*
- [x] *確認開通 token 有過期時間。*
- [x] *註冊成功後呼叫 Email sender 寄送開通信。*
- [x] *測試註冊成功。*
- [x] *測試重複 Email 註冊被拒絕。*
- [x] *測試註冊後產生開通信流程。*

## API 2：Email 開通 `POST /api/auth/activate`

- [x] *在 API 文件定義 request、success response 與 error response。*
- [x] *建立 activation response DTO。*
- [x] *建立 activation request DTO，使用 `activationToken` 欄位避免與 JWT `accessToken` 混淆。*
- [x] *建立 controller endpoint。*
- [x] *建立 Email activation service flow。*
- [x] *用 raw token hash 查詢資料庫中的 token hash。*
- [x] *驗證 token 存在、未使用且未過期。*
- [x] *開通 token 正確且未過期時，將帳號狀態改為已開通。*
- [x] *開通成功後標記 token 已使用。*
- [x] *支援 `INVALID_ACTIVATION_TOKEN`。*
- [x] *測試開通 token 正確時帳號變為已開通。*
- [x] *測試無效開通 token 會被拒絕。*
- [x] *測試過期開通 token 會被拒絕。*
- [x] *測試已使用開通 token 會被拒絕。*

## API 3：登入 `POST /api/auth/login`

- [x] *在 API 文件定義 request、success response 與 error response。*
- [x] *建立 login request / response DTO。*
- [x] *建立 controller endpoint。*
- [x] *建立 login service flow。*
- [x] *登入時驗證 Email 與密碼。*
- [x] *未開通帳號不可完成正式登入。*
- [x] *支援 `ACCOUNT_NOT_ACTIVATED`。*
- [x] *支援 `INVALID_CREDENTIALS`。*
- [x] *密碼驗證成功後產生 Email 二階段驗證碼。*
- [x] *確認二階段驗證碼有過期時間。*
- [x] *密碼驗證成功後呼叫 Email sender 寄送二階段驗證碼。*
- [x] *測試未開通帳號不可登入。*
- [x] *測試密碼錯誤不可登入。*
- [x] *測試密碼正確後必須進入 Email 二階段驗證流程。*

## API 4：二階段驗證 `POST /api/auth/2fa/verify`

- [x] *在 API 文件定義 request、success response 與 error response。*
- [x] *建立 two-factor verification request / response DTO。*
- [x] *建立 controller endpoint。*
- [x] *建立 two-factor verification service flow。*
- [x] *二階段驗證碼正確且未過期時，才視為登入成功。*
- [x] *登入成功後更新該會員的最後登入時間。*
- [x] *登入成功後簽發 JWT。*
- [x] *支援 `INVALID_TWO_FACTOR_CODE`。*
- [x] *支援 `TWO_FACTOR_CODE_EXPIRED`。*
- [x] *測試二階段驗證碼錯誤會被拒絕。*
- [x] *測試二階段驗證碼過期會被拒絕。*
- [x] *測試二階段驗證成功後更新最後登入時間。*
- [x] *測試二階段驗證成功後回傳 JWT。*

## API 5：查詢本人最後登入時間 `GET /api/users/last-login`

- [x] *在 API 文件定義 request header、success response 與 error response。*
- [x] *建立 last-login response DTO。*
- [x] *建立 user controller endpoint。*
- [x] *從 JWT authenticated principal 取得目前使用者。*
- [x] *建立查詢本人最後登入時間 service flow。*
- [x] *確認非本人用戶不可查詢他人的最後登入時間。*
- [x] *支援 `USER_NOT_FOUND`。*
- [ ] *支援 `FORBIDDEN`。*
- [x] *測試使用者可查詢自己的最後登入時間。*
- [x] *測試使用者不可查詢他人的最後登入時間。*

## 第二部分：API 文件與 README

- [x] *提供 Swagger OpenAPI endpoint，或提供 Postman collection。*
- [x] *文件列出主要 API endpoint 與呼叫順序。*
- [x] *文件說明註冊、Email 開通、登入、二階段驗證、查詢最後登入時間的完整測試流程。*
- [x] *文件說明本機啟動方式。*
- [x] *文件說明測試指令。*
- [x] *文件說明 PostgreSQL 或 Docker Compose 使用方式，如專案需要。*
- [x] *文件說明 Email provider 設定方式與環境變數。*
- [x] *文件說明面試題時程下採用的簡化假設。*

## 第二部分：整體測試與收尾

- [x] *Repository 整合測試通過。*
- [x] *Email sender 使用 fake 或 mock，不依賴真實外部寄信服務。*
- [x] *執行 `.\mvnw.cmd test` 並確認通過。*
- [x] *所有會員 API controller / service 測試通過。*
- [ ] *錯誤碼與 HTTP status 覆蓋完整。*

## 最終交付檢查

- [ ] *確認 production code 沒有 `TODO`、placeholder 或省略式實作。*
- [x] *確認 API response 沒有直接回傳 Entity。*
- [x] *確認 Controller 沒有商業邏輯。*
- [x] *確認沒有 field injection。*
- [x] *確認沒有 hardcode secret、API key 或 SMTP 密碼。*
- [x] *確認 README、Swagger 或 Postman collection 與實作一致。*
- [ ] *確認 OCPI 時序圖與會員 API domain 沒有混用。*
- [ ] *確認 Git working tree 只包含預期修改。*
- [ ] *確認 GitHub repository 已包含所有交付文件與程式碼。*

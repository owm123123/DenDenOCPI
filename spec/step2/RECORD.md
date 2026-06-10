# 第二部分會員 API 開發紀錄

## Feature

- *Feature1：全域錯誤 response 擴充*
  - *已將 validation error response 擴充為 `{ code, message, fieldErrors }`，讓前端可依欄位顯示錯誤。*
  - *已將 JWT authentication failure 細分為 `UNAUTHENTICATED`、`TOKEN_EXPIRED`、`INVALID_TOKEN_SIGNATURE`、`INVALID_TOKEN`。*
  - *目前尚未設計 authenticated-but-forbidden 的實際 API 場景，因此 `FORBIDDEN` 先保留為未來權限功能再處理。*

## 批次紀錄

- *批次 1：啟用 Flyway migration 設定。*
  - *主程式設定 `spring.flyway.enabled=true`、`spring.jpa.hibernate.ddl-auto=validate`、`spring.jpa.open-in-view=false`。*
  - *測試環境改用 H2 in-memory database 並啟用 Flyway，不依賴 Docker 或 PostgreSQL。*

- *批次 2：建立會員資料表 migration。*
  - *新增 `spec/step2/DATABASE.md` 作為 DB schema 說明。*
  - *新增 `V1__create_users_table.sql`，建立 `users` table。*

- *批次 3：建立 Email 開通 token migration。*
  - *新增 `V2__create_email_activation_tokens_table.sql`。*
  - *保存 `token_hash`、過期時間與使用狀態，不保存明文 token。*

- *批次 4：建立二階段驗證碼 migration。*
  - *新增 `V3__create_login_two_factor_codes_table.sql`。*
  - *保存 `challenge_id`、`code_hash`、過期時間、驗證狀態與失敗次數。*

- *批次 5：整理 package 結構並建立 JPA entity。*
  - *已清掉空 scaffold package。*
  - *已新增 `entity/`，包含 `User`、`EmailActivationToken`、`LoginTwoFactorCode`、`UserStatus`。*
  - *已用 `.\mvnw.cmd test` 驗證 Flyway migration 與 JPA entity mapping 可啟動。*

- *批次 5 修正：套用 Lombok entity 樣板碼規則。*
  - *已在 `AGENTS.md` 記錄 Lombok 使用規則。*
  - *JPA entity 改用 `@Getter` 與 protected no-args constructor，避免使用 `@Data`。*

- *批次 6：建立 Spring Data JPA repository。*
  - *新增 `repository/` package。*
  - *新增 `UserRepository`，支援 Email 查詢與唯一性檢查。*
  - *新增 `EmailActivationTokenRepository`，支援 token hash 查詢。*
  - *新增 `LoginTwoFactorCodeRepository`，支援 challenge id 與會員近期登入挑戰查詢。*

- *批次 7：補資料層整合測試。*
  - *新增 repository integration test，驗證 users Email 查詢與唯一約束。*
  - *驗證 Email activation token 可用 token hash 查詢。*
  - *驗證 two-factor challenge 可用 challenge id 查詢，並可依會員查近期挑戰。*

- *批次 8：記錄 API 規格並實作註冊 API。*
  - *新增 `spec/step2/API.md`，記錄五支會員 API 的呼叫順序與 contract。*
  - *已實作 `POST /api/auth/register`。*
  - *註冊流程會建立未開通會員、儲存 password hash、產生開通 token hash，並呼叫 fake activation email sender。*

- *批次 9：實作 Email 開通 API。*
  - *當時先實作 `GET /api/auth/activate?token=...`；後續在批次 15 調整為 `POST /api/auth/activate`。*
  - *開通流程用 raw token hash 查詢 DB，只接受存在、未使用且未過期的 token。*
  - *開通成功會將會員狀態改為 `ACTIVE`，並標記 token 已使用。*
  - *無效、過期、已使用 token 統一回傳 `INVALID_ACTIVATION_TOKEN`，避免洩漏 token 狀態細節。*

- *批次 10：建立 Email 寄送抽象與 Mailjet adapter。*
  - *將 activation-only sender 調整為 `AuthEmailSender`，讓開通信與二階段驗證碼共用同一個寄信邊界。*
  - *保留 `InMemoryAuthEmailSender` 作為本機開發與測試預設實作，不依賴真實外部寄信服務。*
  - *新增 Mailjet API adapter，正式寄信時透過環境變數注入 API key、secret、sender email 與開通連結 base URL。*
  - *更新 API 文件、TODO 與 AGENTS 規則，說明 Email provider 設定與不可 hardcode secret。*

- *批次 11：實作登入 API。*
  - *建立 `POST /api/auth/login` 的 request / response DTO、controller endpoint 與 service flow。*
  - *驗證 Email 與密碼，只允許已開通會員進入二階段驗證流程。*
  - *產生二階段驗證 challenge 與驗證碼 hash，並透過 `AuthEmailSender` 寄送驗證碼。*
  - *Email 不存在或密碼錯誤統一回傳 `INVALID_CREDENTIALS`，避免洩漏帳號是否存在。*
  - *補 service / controller tests，驗證未開通帳號、錯誤密碼、validation error 與成功進入二階段驗證流程。*

- *批次 12：實作二階段驗證本體。*
  - *建立 `POST /api/auth/2fa/verify` 的 request / response DTO、controller endpoint 與 service flow。*
  - *驗證 challenge 存在、未過期、未驗證，並用 code hash 比對二階段驗證碼。*
  - *驗證成功後標記 challenge `verifiedAt`，並更新會員 `lastLoginAt`。*
  - *JWT 簽發與 Bearer token response 保留到批次 13。*

- *批次 13：建立 JWT 簽發、驗證與 Spring Security 整合。*
  - *新增 JWT 設定與 token service，二階段驗證成功後回傳 Bearer access token。*
  - *Spring Security 改為 stateless resource server，後續受保護 API 可用 `Authorization: Bearer <jwt>` 驗證。*
  - *JWT secret 透過 `APP_JWT_SECRET` 注入，正式環境不可使用本機開發預設值。*

- *批次 14：實作查詢本人最後登入時間 API。*
  - *新增 `GET /api/users/last-login`，從 JWT subject 取得目前使用者識別碼。*
  - *新增 `controller/user`、`service/user` 與 user response DTO，避免把 user 查詢 API 混入 auth controller。*
  - *API 不接受 user id 或 email query parameter，因此 client 不能指定查詢其他會員。*
  - *補 service / controller tests，驗證成功 response 與 `USER_NOT_FOUND` 錯誤碼。*

- *批次 14 修正：JWT subject 改用會員公開 UUID。*
  - *新增 `users.public_id` 作為對外穩定識別碼，保留內部 `users.id` 作為資料表 PK 與 FK。*
  - *JWT `sub` 改為 `users.public_id`，不再使用 Email 或可遞增內部 id。*
  - *`GET /api/users/last-login` 改用 JWT `sub` 查詢 `users.public_id`，會員最新資料仍以 DB 為準。*

- *批次 15：修正 Email 開通 API 的 HTTP 語意。*
  - *已將原本會改變帳號狀態的 `GET /api/auth/activate?token=...` 調整為 `POST /api/auth/activate`。*
  - *Email 內容中的開通連結應導向前端確認頁，使用者確認後再由前端呼叫後端 `POST` 完成開通。*
  - *request body 欄位命名使用 `activationToken`，避免與登入成功後回傳的 JWT `accessToken` 混淆。*
  - *同步更新 `API.md`、controller、request DTO、service / controller tests 與 todo-list。*
  - *保留 activation token 短效、一次性、使用後失效的安全規則。*
  - *若前端與後端不同 origin，需要在 Spring Security / MVC CORS 設定允許前端 origin 呼叫 `POST /api/auth/activate`。*

- *批次 16：GCP 部署決策與環境規劃。*
  - *部署方向選定 GCP，目標是符合職缺對 AWS / GCP 雲端平台管理經驗的期待。*
  - *規劃使用 Cloud Run 部署 Spring Boot container。*
  - *規劃使用 Cloud SQL for PostgreSQL 作為雲端 RDBMS。*
  - *規劃使用 Secret Manager 或 Cloud Run environment variables 管理 `APP_JWT_SECRET`、Mailjet key / secret 與 DB password。*
  - *規劃將 `min instances` 設為 0，並建立 billing alert，降低面試展示環境的成本風險。*
  - *此批次先記錄部署決策，不先實作 GCP 部署。*

- *批次 17：Mailjet 本地實際開通與驗證。*
  - *由使用者申請或提供 Mailjet sandbox / 正式 API key。*
  - *本機用環境變數設定 `MAILJET_API_KEY`、`MAILJET_API_SECRET`、`MAILJET_SENDER_EMAIL`。*
  - *設定 `app.email.provider=mailjet`，並確認不把任何真實 secret commit。*
  - *實際測試註冊開通信與 2FA 驗證碼寄送。*
  - *確認 Email 開通連結導向前端確認頁，並帶 `activationToken` query parameter。*
  - *本地驗證完成後再整理 `todo-list.md` 的 Email provider 相關勾選狀態。*

- *批次 18：Swagger / OpenAPI API 文件。*
  - *已新增 Swagger / OpenAPI 支援，讓面試官可直接在瀏覽器檢視與測試 API contract。*
  - *使用 `springdoc-openapi-starter-webmvc-ui`，並確認可在 Spring Boot 4.0.6 專案編譯。*
  - *已開放 `/swagger-ui.html`、`/swagger-ui/**` 與 `/v3/api-docs/**`，避免 API 文件被 JWT 驗證擋住。*
  - *已在 Swagger 定義 Bearer JWT security scheme，`GET /api/users/last-login` 可透過 Swagger UI Authorize 測試。*
  - *已同步更新 `API.md` 與 `todo-list.md`。*

- *批次 19：README、todo-list 與交付收尾。*
  - *整理 README 啟動方式。*
  - *整理本機與 GCP 部署設定方式。*
  - *整理 API 測試流程。*
  - *確認 `todo-list.md` 勾選狀態。*
  - *將 API contract review 的剩餘邊界檢查併入交付前檢查。*
  - *依使用者指定再跑 integration test 或完整 `.\mvnw.cmd test`。*

- *批次 20：Cloud Run + Cloud SQL 部署驗證。*
  - *依批次 16 的決策建立 GCP 展示環境。*
  - *設定 Cloud SQL PostgreSQL、Cloud Run service、Secret Manager 或 Cloud Run environment variables。*
  - *確認 Flyway migration 可在雲端 DB 正常執行。*
  - *使用 Mailjet provider 在雲端環境測試註冊開通信與 2FA 驗證碼寄送。*
  - *驗證受保護 API 可透過 `Authorization: Bearer <jwt>` 正常呼叫。*

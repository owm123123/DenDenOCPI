# 第二部分會員 API 開發紀錄

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

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
  - *預計清掉空 scaffold package。*
  - *預計新增 `entity/`，讓 Java entity 對齊 `spec/step2/DATABASE.md`。*

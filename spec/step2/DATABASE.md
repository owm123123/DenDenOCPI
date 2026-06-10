# 會員 API 資料庫設計

## 設計原則

- *資料庫 schema 以 Flyway migration 為準，JPA 不自動建立或更新資料表。*
- *重要 invariant 優先用 database constraint 保護，例如 Email 唯一、必要欄位不可為 null。*
- *測試環境使用 H2 in-memory database，migration SQL 需維持 PostgreSQL 與 H2 PostgreSQL mode 相容。*
- *Email 開通 token 與二階段驗證碼不長期保存明文，後續實作會保存 hash。*
- *時間欄位使用 `TIMESTAMP WITH TIME ZONE`，Java 端預計對應 `OffsetDateTime` 或 `Instant`。*

## users

*用途：保存會員帳號、密碼雜湊、開通狀態與最後登入時間。*

| 欄位 | 型別 | Null | 說明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | No | Primary key，identity 自動產生 |
| `public_id` | `UUID` | No | 對外穩定識別碼，用於 JWT `sub`，避免暴露可遞增內部 id |
| `email` | `VARCHAR(254)` | No | 登入帳號；application 層統一轉小寫後保存 |
| `password_hash` | `VARCHAR(255)` | No | Password encoder 產生的密碼雜湊 |
| `status` | `VARCHAR(32)` | No | 帳號狀態，目前允許 `PENDING_ACTIVATION`、`ACTIVE` |
| `activated_at` | `TIMESTAMP WITH TIME ZONE` | Yes | Email 開通完成時間 |
| `last_login_at` | `TIMESTAMP WITH TIME ZONE` | Yes | 二階段驗證完成後的正式登入時間 |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | 建立時間 |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | 最後更新時間 |
| `version` | `BIGINT` | No | JPA optimistic locking 版本欄位 |

### Constraints / Indexes

- *Primary key：`pk_users` on `id`。*
- *Unique constraint：`uk_users_public_id` on `public_id`。*
- *Unique constraint：`uk_users_email` on `email`。*
- *Check constraint：`ck_users_status` 限制 `status` 必須是 `PENDING_ACTIVATION` 或 `ACTIVE`。*
- *`version` 預設為 `0`。*

## email_activation_tokens

*用途：保存 Email 開通 token 的 hash、過期時間與使用狀態。*

| 欄位 | 型別 | Null | 說明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | No | Primary key，identity 自動產生 |
| `user_id` | `BIGINT` | No | 對應要開通的會員 |
| `token_hash` | `VARCHAR(128)` | No | 開通 token hash，不保存明文 token |
| `expires_at` | `TIMESTAMP WITH TIME ZONE` | No | token 過期時間 |
| `used_at` | `TIMESTAMP WITH TIME ZONE` | Yes | token 成功使用時間 |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | 建立時間 |

### Constraints / Indexes

- *Primary key：`pk_email_activation_tokens` on `id`。*
- *Foreign key：`fk_email_activation_tokens_user` from `user_id` to `users.id`。*
- *Unique constraint：`uk_email_activation_tokens_token_hash` on `token_hash`。*
- *Index：`idx_email_activation_tokens_user_id` on `user_id`。*

## login_two_factor_codes

*用途：保存一次登入挑戰的二階段驗證碼 hash 與驗證狀態。*

| 欄位 | 型別 | Null | 說明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | No | Primary key，identity 自動產生 |
| `user_id` | `BIGINT` | No | 對應正在登入的會員 |
| `challenge_id` | `VARCHAR(36)` | No | 一次登入挑戰的公開識別值，預計使用 UUID 字串 |
| `code_hash` | `VARCHAR(128)` | No | 二階段驗證碼 hash，不保存明文 code |
| `expires_at` | `TIMESTAMP WITH TIME ZONE` | No | 驗證碼過期時間 |
| `verified_at` | `TIMESTAMP WITH TIME ZONE` | Yes | 二階段驗證成功時間 |
| `failed_attempts` | `INTEGER` | No | 驗證失敗次數 |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | 建立時間 |

### Constraints / Indexes

- *Primary key：`pk_login_two_factor_codes` on `id`。*
- *Foreign key：`fk_login_two_factor_codes_user` from `user_id` to `users.id`。*
- *Unique constraint：`uk_login_two_factor_codes_challenge_id` on `challenge_id`。*
- *Check constraint：`ck_login_two_factor_codes_failed_attempts` 限制 `failed_attempts >= 0`。*
- *Index：`idx_login_two_factor_codes_user_id` on `user_id`。*
- *Index：`idx_login_two_factor_codes_user_created_at` on `user_id, created_at`，用於查詢會員近期登入挑戰。*

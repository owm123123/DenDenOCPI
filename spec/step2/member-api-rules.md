# 會員 API 需求規則

第二部分至少需要支援：

- 使用 Email 註冊帳號。
- 註冊時儲存安全雜湊後的密碼，不可儲存明文密碼。
- 註冊後寄送開通 Email。
- 未完成 Email 開通前，不可完成正式登入。
- 登入使用 Email 與密碼。
- 密碼驗證成功後，需進入 Email 二階段驗證流程。
- Email 二階段驗證完成後，才視為登入成功。
- 登入成功後需更新最後登入時間。
- 提供 API 讓用戶查詢自己的最後登入時間。
- 非本人用戶不可查詢他人的最後登入時間。

若需要簡化登入狀態或 token 機制，必須在 README 或文件中說明簡化假設與測試方式。

預設 API 命名方向：

- `POST /api/auth/register`
- `POST /api/auth/activate`
- `POST /api/auth/login`
- `POST /api/auth/2fa/verify`
- `GET /api/users/last-login`

若後續要調整 endpoint 命名，需同步更新 Swagger / Postman / README，避免文件與實作不一致。

## Email 服務規則

Email 服務可使用 Mailjet、SendGrid、Mailtrap 或同類型服務。

實作時：

- API key、secret、SMTP 密碼不可 hardcode。
- 使用環境變數或本機設定檔注入敏感設定。
- 範例設定只能提供 placeholder。
- 測試環境可使用 fake email sender、mock email sender 或 Mailtrap sandbox。
- Email 開通信與二階段驗證碼需可被測試驗證，不要讓測試依賴真實外部寄信服務。

## 安全與權限規則

- 不讀取或顯示憑證、token、登入資訊、私鑰、cookie 等敏感內容。
- 密碼必須使用合適的 password encoder。
- Email 開通 token 與二階段驗證碼需有過期時間。
- 驗證 token 或 code 不應以明文形式長期保存，若有保存需說明取捨。
- 查詢最後登入時間時，必須檢查目前登入者只能查自己的資料。
- 非本人查詢應回傳穩定錯誤，例如 `FORBIDDEN` 或對應錯誤碼。

## Error Handling

錯誤 response 應一致、穩定、可測試。

常見錯誤碼可包含：

- `EMAIL_ALREADY_REGISTERED`
- `ACCOUNT_NOT_ACTIVATED`
- `INVALID_CREDENTIALS`
- `INVALID_ACTIVATION_TOKEN`
- `INVALID_TWO_FACTOR_CODE`
- `TWO_FACTOR_CODE_EXPIRED`
- `USER_NOT_FOUND`
- `FORBIDDEN`

測試錯誤處理時，優先驗證穩定錯誤碼與 HTTP status，不要依賴脆弱的 exception message。

## Database / Migration

若專案使用資料庫 migration，任何 schema 變更都應建立 migration。

測試環境預設使用 `src/test/resources/application.properties` 的 H2 in-memory database，不依賴 Docker 或 PostgreSQL。正式與本機開發環境可使用 `compose.yaml` 啟動 PostgreSQL。

重要 invariant 優先用 DB constraint 保護，例如：

- Email 唯一。
- 必要欄位不可為 null。
- token / code 對應資料需能追蹤過期時間。
- 使用者狀態需清楚表達是否已開通。

新增 query pattern 時需檢查是否需要 index。

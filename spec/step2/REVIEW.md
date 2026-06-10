# API.md 規格 Review

## 結論

`spec/step2/API.md` 目前大方向符合題目要求，已涵蓋註冊、Email 開通、登入、Email 二階段驗證、JWT 與查詢本人最後登入時間。

不過如果以資深 Java 後端工程師或正式 API contract review 的角度來看，這份規格仍有幾個容易讓 client、Swagger/Postman 測試或實作者產生歧義的地方。主要問題集中在 HTTP 語意、安全流程狀態、錯誤格式完整性與 JWT contract 明確度。

## 主要問題

### 1. `GET /api/auth/activate` 會改變帳號狀態

目前規格定義：

- 使用者從 Email 點擊 `GET /api/auth/activate?token=...`。
- 呼叫成功後完成帳號開通。

問題原因：

- `GET` 在 HTTP 語意上應該是 safe method，不應造成伺服器狀態變更。
- Email client、安全掃描器、瀏覽器預讀或防毒服務可能自動打開連結。
- 若開通連結被自動請求，帳號可能在使用者未主動確認的情況下被開通。

建議修正：

- 較完整做法：Email 連到前端確認頁，前端再送 `POST /api/auth/activate` 完成開通。
- 若為了作業與操作簡化仍保留 `GET`，文件應明確註記這是 email link flow 的取捨，並要求 activation token 使用後立即失效、具備過期時間。

### 2. `POST /api/auth/login` 的成功語意容易被誤解成正式登入成功

處理狀態：

- 已在 `spec/step2/API.md` 補充：`POST /api/auth/login` 成功只代表帳密驗證通過並建立 2FA challenge，不簽發 JWT、不更新 `lastLoginAt`。
- 已在 `spec/step2/API.md` 補充：`POST /api/auth/2fa/verify` 成功才視為正式登入成功，並簽發 JWT、更新 `lastLoginAt`。

目前規格定義：

- `POST /api/auth/login` 驗證 Email 與密碼。
- 成功後回傳 `TWO_FACTOR_REQUIRED` 與 `challengeId`。

問題原因：

- 這一步只是帳密驗證成功並建立二階段驗證 challenge。
- 真正登入成功應該發生在 `POST /api/auth/2fa/verify` 驗證通過並取得 JWT 之後。
- 若文件只寫 `Success Response`，容易讓實作者把「帳密驗證成功」誤認為「登入成功」。
- 這會影響最後登入時間 `lastLoginAt` 的更新時機。依需求，應在 Email 二階段驗證完成後才視為登入成功並更新最後登入時間。

建議修正：

- 將 `POST /api/auth/login` 的成功描述改成「Password accepted / 2FA challenge created」。
- 明確註記此階段不發 JWT、不更新最後登入時間。
- 在 `POST /api/auth/2fa/verify` 成功 response 補充：「此時才視為正式登入成功，並更新 `lastLoginAt`。」

### 3. 錯誤 response 規格不完整

處理狀態：

- 已在 `spec/step2/API.md` 補全域錯誤格式 `{ code, message }`。
- 已補 common error：`VALIDATION_ERROR`、`INVALID_REQUEST`、`UNAUTHENTICATED`、`INTERNAL_ERROR`。
- 已補實作：malformed JSON / missing body 回 `INVALID_REQUEST`，未帶或無效 JWT 回 `UNAUTHENTICATED`，未預期錯誤回 `INTERNAL_ERROR` 且不暴露 raw exception message。
- 暫不實作 `fieldErrors` 與 token 過期 / token 格式錯的細分錯誤碼，已記錄於 `spec/step2/RECORD.md` 的 Feature。

目前每個 endpoint 只列出部分業務錯誤，例如：

- register 只列 Email 已被註冊。
- login 只列帳密錯誤與帳號未開通。
- last-login 只列 JWT subject 找不到會員。

問題原因：

- 實際 API 還會遇到 request validation、JSON 格式錯誤、缺少欄位、Email 格式錯、密碼不符規則等錯誤。
- 受保護 API 還會遇到未帶 token、token 過期、token 簽章錯誤、token 格式錯誤。
- 若規格沒有定義，Swagger/Postman 測試時會出現文件外 response，降低 API contract 的穩定性。

建議修正：

- 補一節全域錯誤格式，例如：
    - `code`：穩定、可測試的機器可讀錯誤碼。
    - `message`：給人看的錯誤訊息。
    - `fieldErrors`：欄位驗證錯誤時使用。
- 補 common error：
    - `400 Bad Request`：request body 格式錯誤、欄位驗證失敗。
    - `401 Unauthorized`：未登入、JWT 無效或過期。
    - `403 Forbidden`：已登入但無權限。
    - `500 Internal Server Error`：未預期錯誤，且不可暴露 raw exception message。

### 4. 2FA 錯誤狀態切分不夠一致

目前規格定義：

- Challenge 不存在、已驗證或驗證碼錯誤：`INVALID_TWO_FACTOR_CODE`
- 驗證碼已過期：`TWO_FACTOR_CODE_EXPIRED`

問題原因：

- 如果安全性優先，通常會避免讓攻擊者分辨 challenge 是否存在、是否過期、是否 code 錯誤。
- 如果測試可觀察性優先，則可以切出不同錯誤碼，但文件需要明確說明每種狀態。
- 目前文件介於兩者之間：部分狀態合併，過期狀態又獨立，容易讓實作者不知道判斷邏輯應該如何切分。

建議修正：

- 選擇一種策略並寫清楚：
    - 安全一致策略：challenge 不存在、已驗證、code 錯誤、過期都回同一個錯誤碼。
    - 測試清楚策略：明確列出每種狀態對應的錯誤碼與 HTTP status。
- 若保留 `TWO_FACTOR_CODE_EXPIRED`，應明確定義 challenge 過期、code 過期與已使用 challenge 的差異。

### 5. 受保護 API 缺少 authentication failure 規格

目前 `GET /api/users/last-login` 只定義：

- JWT subject 對應不到會員：`404 Not Found`

問題原因：

- 此 API 需要 `Authorization: Bearer <jwt>`。
- 但規格沒有定義未帶 token、token 過期、token 簽章錯、token 格式錯要回什麼。
- 這些錯誤是 client 測試最常遇到的 authentication failure，應明確納入 contract。

建議修正：

- 補充以下錯誤：
    - 未帶 `Authorization` header：`401 Unauthorized`
    - JWT 過期：`401 Unauthorized`
    - JWT 簽章錯或格式錯：`401 Unauthorized`
- 錯誤碼可統一為 `UNAUTHENTICATED` 或拆成 `INVALID_TOKEN`、`TOKEN_EXPIRED`，但要保持文件與實作一致。

### 6. JWT contract 不夠明確

目前規格有寫：

- HMAC SHA-256 簽章。
- `APP_JWT_ISSUER`
- `APP_JWT_SECRET`
- `APP_JWT_ACCESS_TOKEN_EXPIRES_IN`

問題原因：

- 沒有明確定義 JWT `sub` 是 member id、email，還是其他 identifier。
- `GET /api/users/last-login` 又依賴 JWT subject 判斷目前使用者。
- 若 subject contract 不清楚，實作者、測試資料與未來 client 容易產生不一致。

建議修正：

- 明確定義 JWT claims：
    - `sub` 使用什麼值，例如 member UUID 或 member id。
    - `iss` 是否必須等於 `APP_JWT_ISSUER`。
    - `iat`、`exp` 是否必填。
    - 是否包含 `email` 或 role/scope。
- 建議不要把 email 當成唯一身份依據，若未來 email 可變更，會造成 token subject 語意不穩。

### 7. 使用 JWT 裡的資料作為 DB 查詢依據需要明確界線

目前流程定義：

- 前端帶 `Authorization: Bearer <jwt>` 呼叫 `GET /api/users/last-login`。
- 後端從 JWT subject 判斷目前使用者，再查詢本人的最後登入時間。

判斷：

- 這個方向是合理且常見的。
- 但合理的前提是：JWT 只提供穩定的身份識別，真正會變動的會員狀態仍以 DB 為準。

問題原因：

- JWT 適合放身份識別，例如 immutable member id、member UUID。
- DB 適合保存最新業務狀態，例如 `lastLoginAt`、帳號是否停用、帳號是否刪除。
- 若直接使用 JWT 裡的 email 查 DB，未來 email 可變更時，token subject 語意會變得不穩。
- 若把 `lastLoginAt` 放進 JWT 後直接回傳，會得到 token 簽發當下的舊資料，不一定是 DB 最新狀態。
- 若完全只相信 JWT 而不查 DB，帳號被停用、刪除或狀態異動時，API 無法反映最新權限與資料狀態。

建議修正：

- JWT `sub` 使用穩定不可變的 member id 或 member UUID。
- 後端驗證 JWT 簽章、issuer 與 expiration 後，使用 `sub` 查詢 DB。
- `lastLoginAt` 應從 DB 讀取最新值，不應直接使用 JWT claim 內的值。
- 若 token 合法但 DB 找不到會員，文件需明確定義回 `404 USER_NOT_FOUND`，或依安全策略改回 `401/403`，但要保持規格與實作一致。
- 此 endpoint 不接受 user id 或 email query parameter 是好的設計，因為可以避免 client 指定他人身份造成越權查詢風險。

### 8. `201 Created` 未說明是否回 `Location` header

目前 `POST /api/auth/register` 成功回：

- HTTP status：`201 Created`
- response body 包含 `message` 與 `email`

問題原因：

- `201 Created` 通常會搭配 `Location` header 指向新建資源。
- 但本專案尚未定義公開的 user resource endpoint，而且帳號註冊後仍未開通。
- 不說明 `Location` header 會讓 API contract 有一點不完整。

建議修正：

- 若不回 `Location`，文件明確註記：「註冊後帳號尚未開通，且未提供公開 user resource URL，因此不回傳 `Location` header。」
- 或若要更標準，可定義可查詢的新資源位置，但本題不一定需要，避免過度設計。

## 建議優先修正順序

1. 修正或說明 Email activation 使用 `GET` 造成狀態變更的取捨。
2. 明確定義 `POST /api/auth/login` 不是正式登入成功，`POST /api/auth/2fa/verify` 成功後才更新 `lastLoginAt`。
3. 補全域錯誤格式與 validation、JWT authentication failure 規格。
4. 補 JWT subject 與 claims contract。
5. 明確定義 `GET /api/users/last-login` 使用 JWT subject 查 DB 的資料邊界。
6. 補 2FA 錯誤切分策略。
7. 補 `201 Created` 是否回 `Location` header 的說明。

## 總評

這份 API 規格的功能骨架是正確的，已能支援面試題要求的主要流程。

目前問題不是方向錯，而是 contract 還不夠精確。若要讓面試官用 Swagger 或 Postman 嚴格測試，建議把上述邊界情境補齊，讓文件、實作與測試能對齊。

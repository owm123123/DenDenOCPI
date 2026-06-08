# AGENTS.md

This file provides guidance to Codex when working with code in this repository.

## 專案定位

這是 **2026 後端工程師面試實作專案**，不是單純 Java 練習沙盒。

開發時要把題目視為真實需求，目標是做出可審查、可測試、可說明、可交付的作品。除非使用者明確要求，不要把程式碼寫成教學範例或練習題形式。

本專案主要包含兩個交付方向：

- OCPI 2.2.1 概念流程：繪製 User、SCSP、EMSP、CPO 之間從啟動充電到結束充電並收到帳單的時序圖。
- Java Spring Boot API：實作會員註冊、Email 開通、登入、Email 二階段驗證，以及查詢本人最後登入時間的 RESTful API。

## 需求來源

主要需求以以下檔案為準：

- `spec/後端作業題型2026.md`

OCPI 流程與名詞補充參考：

- `resource/本專案會用到的 OCPI 摘要.md`

OCPI 摘要只用於第一部分時序圖，不應混入第二部分會員 API 的 domain model、endpoint 或資料表設計。

文件放置規則：

- `spec/` 放題目原始需求與整理後的需求文件。
- `resource/` 放 OCPI PDF、OCPI 摘要等非 runtime 參考資料。

若 PDF、Markdown、README 或程式碼之間有衝突，先回到題目需求確認，不要自行擴張需求。

## 互動規則

- 永遠使用繁體中文回答，包含實作計畫、進度追蹤、測試回報與 commit message 建議。
- 回答先講結論，再補充必要細節。
- 技術說明要具體、可執行，避免空泛建議。
- 修改前先讀相關檔案，確認既有結構、命名、package、測試與文件風格。
- 不確定就查檔案，不要憑印象猜專案結構、API、module 名稱或 build tool。
- 不要使用 `TODO`、`省略`、`... existing code ...` 代替實作。
- 完成前要驗證。能跑測試就跑；不能跑要明確說原因與建議指令。

## 交付原則

開發時優先考慮：

- 需求是否完整對應題目。
- API 是否清楚、穩定、容易用 Swagger 或 Postman 測試。
- 權限與安全邏輯是否合理。
- Email 開通與 Email 二階段驗證流程是否可被清楚驗證。
- 錯誤 response 是否一致且可追蹤。
- README 或文件是否足以讓面試官快速啟動與測試。

避免過度設計。除非需求明確需要，不要主動引入微服務、Kafka、Redis、Kubernetes、複雜 DDD 分層或過重架構。

## OCPI 時序圖規則

第一部分交付需包含完整流程：

- User 透過 SCSP 發起啟動充電。
- SCSP → EMSP：Start Session command。
- EMSP → CPO：CommandForward 或 `POST /commands/START_SESSION`。
- CPO → EMSP：Session 更新，至少包含 `PENDING → ACTIVE`。
- User 透過 SCSP 發起停止充電。
- SCSP → EMSP：Stop Session command。
- EMSP → CPO：CommandForward 或 `POST /commands/STOP_SESSION`。
- CPO → EMSP：Session 更新為 `COMPLETED`。
- CPO → EMSP：回傳 CDR。
- EMSP → SCSP：帳單結果通知。
- SCSP → User：顯示帳單結果。

時序圖可使用 Mermaid.js。圖中需標示角色、API 名稱、主要 response 與狀態轉換。

題目允許忽略 token 認證流程；若文件或圖中省略 token / credentials，需明確標註這是題目允許的簡化假設。

## Java / Spring Boot 原則

技術棧以實際 build file 為準。未確認版本前，不要硬套特定 Spring Boot 或 Java API。

目前 scaffold 使用：

- Java 21。
- Spring Boot 4.0.6。
- Maven Wrapper，Windows 環境優先使用 `.\mvnw.cmd`。
- Spring Web MVC。
- Spring Security。
- Spring Data JPA。
- Flyway。
- PostgreSQL。
- H2 for tests。

常用指令：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

基本原則：

- 禁止 field injection，一律使用 constructor injection。
- Controller 不放商業邏輯。
- Controller 只負責 request validation、呼叫 application service / use case、回傳 response。
- 新 DTO、request、response、command、event 優先使用 `record`。
- Entity 不直接作為 API response。
- 寫入流程要有清楚的 transaction boundary。
- 查詢流程可使用 read-only transaction。
- 使用集中式 exception handling。
- 錯誤 response 要穩定，不直接把 exception message 暴露給 client。
- 重要狀態轉換可加 SLF4J log，但不可記錄密碼、token、驗證碼或敏感資訊。

## 可用輔助 Skills

本專案已安裝下列 Java / Spring 相關 Codex skills。遇到對應任務時，優先讀取並套用：

- `spring-boot-patterns`：Spring Boot controller、service、configuration、validation 與常見實作模式。
- `security-audit`：密碼、token、Email 驗證、2FA、權限檢查與 OWASP 風險檢查。
- `api-contract-review`：REST API 語意、HTTP status、request / response contract、Swagger / OpenAPI 檢查。
- `test-quality`：JUnit 5、AssertJ、測試命名、測試覆蓋與 fake / mock 設計。
- `jpa-patterns`：Spring Data JPA、Hibernate、transaction、lazy loading、N+1 與資料存取設計。
- `java-code-review`：交付前 Java 程式碼審查與整體品質檢查。

這些 skills 已確認為純 Markdown 指引，沒有可執行腳本。使用時仍以本專案需求、`AGENTS.md` 規則與題目文件為最高優先，不要因通用 skill 建議而過度設計。

## 會員 API 需求規則

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
- `GET /api/auth/activate?token=...`
- `POST /api/auth/login`
- `POST /api/auth/2fa/verify`
- `GET /api/users/me/last-login`

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

## 測試規則

修改 production code 時，原則上要補或更新測試。

優先測試 business behavior：

- 註冊成功。
- 重複 Email 註冊被拒絕。
- 註冊後產生開通信流程。
- 未開通帳號不可登入。
- 開通 token 正確時帳號變為已開通。
- 密碼錯誤不可登入。
- 登入後必須完成 Email 二階段驗證。
- 二階段驗證成功後更新最後登入時間。
- 使用者可查詢自己的最後登入時間。
- 使用者不可查詢他人的最後登入時間。

若需要測試 Email，優先使用 fake / mock sender，不依賴真實外部服務。

## API 文件與交付

題目要求提供 Swagger 或 Postman collection。

若實作 Spring Boot API，優先提供：

- Swagger / OpenAPI endpoint。
- 或 Postman collection。
- 若新增 Swagger，優先使用 springdoc-openapi starter，且需確認支援 Spring Boot 4。
- README 中列出啟動方式、測試帳號建立方式、主要 API 流程。
- README 中說明環境變數與 Email provider 設定方式。
- README 中說明哪些地方是因面試題時程做的簡化假設。

## 修改前檢查

開始實作前先確認：

1. 這次修改對應題目的哪一個需求。
2. 是否需要更新 README 或 API 文件。
3. 是否涉及 Email、登入、token、密碼或權限。
4. 是否需要新增或調整資料表。
5. 是否需要 migration。
6. 測試應該放在哪裡。
7. 是否有外部服務依賴，測試時是否可替換成 fake / mock。
8. 是否會影響面試官執行與驗證流程。

## 完成修改後 SOP

完成一個邏輯區塊後：

1. 跑 relevant tests。
2. 若改 schema，確認 migration 檔存在。
3. 若改 API，確認 Swagger / Postman / README 是否同步。
4. 若改登入、Email、驗證碼或權限流程，確認安全性與測試都有覆蓋。
5. 若新增簡化假設，補進 README 或文件。
6. 回報改了什麼、對應哪個需求、跑了哪些測試、哪些沒跑與原因。

## Commit 規則

commit message 使用繁體中文。可以保留常見 conventional commit 前綴，但描述內容需使用繁體中文。

範例：

- `docs: 新增 OCPI 充電流程時序圖`
- `feat: 新增會員註冊 API`
- `feat: 新增 Email 開通流程`
- `feat: 新增登入二階段驗證`
- `test: 補齊最後登入時間權限測試`
- `fix: 修正非本人可查詢登入時間問題`

使用者要求 commit 時，先執行 `git status`，只 stage 本次任務修改的檔案。

## Hard Prohibitions

除非使用者明確要求，否則禁止：

- 將本專案寫成電商範例或 Java 練習沙盒。
- 主動加入與題目無關的大型架構。
- field injection。
- Controller 寫商業邏輯。
- API response 直接回傳 Entity。
- 儲存明文密碼。
- hardcode secret、API key、SMTP 密碼。
- log password、token、驗證碼或敏感個資。
- 在 production code 用 `TODO`、placeholder 或省略內容代替實作；文件與 env 範例可使用明確標示的 placeholder。
- disable test 讓 build pass。
- 引入新 dependency 卻不說明原因。

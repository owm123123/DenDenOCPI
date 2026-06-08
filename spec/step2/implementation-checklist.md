# 第二部分實作與交付檢查清單

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

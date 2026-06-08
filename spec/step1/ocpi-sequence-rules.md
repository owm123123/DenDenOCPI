# OCPI 時序圖規則

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

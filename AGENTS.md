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

## 交付原則

開發時優先考慮：

- 需求是否完整對應題目。
- API 是否清楚、穩定、容易用 Swagger 或 Postman 測試。
- 權限與安全邏輯是否合理。
- Email 開通與 Email 二階段驗證流程是否可被清楚驗證。
- 錯誤 response 是否一致且可追蹤。
- README 或文件是否足以讓面試官快速啟動與測試。

避免過度設計。除非需求明確需要，不要主動引入微服務、Kafka、Redis、Kubernetes、複雜 DDD 分層或過重架構。

## 階段需求規則索引

處理特定階段時，先讀取對應需求規則：

- [第一部分 OCPI 時序圖規則](spec/step1/ocpi-sequence-rules.md)
- [第二部分會員 API、Email、安全、錯誤處理與資料庫規則](spec/step2/member-api-rules.md)
- [第二部分實作與交付檢查清單](spec/step2/implementation-checklist.md)
- 若要查詢或調整第二部分 DB schema，先參考 [資料庫設計文件](spec/step2/DATABASE.md)。
- 若要理解第二部分各批次已完成事項與後續切分，先參考 [開發紀錄](spec/step2/RECORD.md)。

## 目前技術棧

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

## Lombok 使用規則

- 本專案已設定 Lombok annotation processor，可用 Lombok 減少樣板碼。
- JPA entity 禁止使用 `@Data`，避免 `equals`、`hashCode`、`toString` 觸發 lazy association 或造成 entity identity 問題。
- JPA entity 優先使用 `@Getter` 與 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`。
- DTO 仍優先使用 Java `record`；只有在需要 class 型別或框架限制時才考慮 Lombok。

## 可用輔助 Skills

本專案已安裝下列 Java / Spring 相關 Codex skills。遇到對應任務時，優先讀取並套用：

- `spring-boot-patterns`：Spring Boot controller、service、configuration、validation 與常見實作模式。
- `security-audit`：密碼、token、Email 驗證、2FA、權限檢查與 OWASP 風險檢查。
- `api-contract-review`：REST API 語意、HTTP status、request / response contract、Swagger / OpenAPI 檢查。
- `test-quality`：JUnit 5、AssertJ、測試命名、測試覆蓋與 fake / mock 設計。
- `jpa-patterns`：Spring Data JPA、Hibernate、transaction、lazy loading、N+1 與資料存取設計。
- `java-code-review`：交付前 Java 程式碼審查與整體品質檢查。

這些 skills 已確認為純 Markdown 指引，沒有可執行腳本。使用時仍以本專案需求、`AGENTS.md` 規則與題目文件為最高優先，不要因通用 skill 建議而過度設計。

## 專案禁止事項

除非使用者明確要求，否則禁止：

- 將本專案寫成電商範例或 Java 練習沙盒。
- 主動加入與題目無關的大型架構。

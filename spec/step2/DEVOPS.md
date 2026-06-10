# DEVOPS

*這份文件記錄本機開發、測試、環境變數與 GCP 部署方向。面試官若只想看功能與 Swagger 操作流程，請先看 [README.md](../../README.md)。*

## 本機需求

- *Java 21。*
- *Docker Desktop，用於本機 PostgreSQL。*
- *Windows 環境建議使用 Maven Wrapper：`.\mvnw.cmd`。*

## 本機啟動

*本專案有 `compose.yaml`，並引入 `spring-boot-docker-compose`。本機執行 Spring Boot 時，Spring Boot 可協助啟動 PostgreSQL container。*

```powershell
cd C:\Users\owm123123\Desktop\DenDenOCPI
$env:APP_JWT_SECRET="local-development-jwt-secret-32-bytes"
.\mvnw.cmd spring-boot:run
```

*啟動後可開啟：*

- *Swagger UI：`http://localhost:8080/swagger-ui.html`*
- *OpenAPI JSON：`http://localhost:8080/v3/api-docs`*

*如果 8080 被占用，可以改用其他 port：*

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## PostgreSQL / Docker Compose

*本機 PostgreSQL 設定在 `compose.yaml`：*

- *Database：`mydatabase`*
- *Username：`myuser`*
- *Password：`secret`*

*資料庫 schema 由 Flyway migration 建立，migration 檔案位於：*

- *`src/main/resources/db/migration`*

*Spring Boot 啟動時會執行 Flyway，並使用 `spring.jpa.hibernate.ddl-auto=validate` 驗證 JPA entity 與 schema 對齊。*

## 測試指令

*小改 service 商業邏輯時，優先跑 service unit test：*

```powershell
.\mvnw.cmd "-Dtest=AuthServiceTests,UserServiceTests,JwtTokenServiceTests" test
```

*改 API contract、request / response、validation 或 exception handler 時，跑 controller test：*

```powershell
.\mvnw.cmd "-Dtest=AuthControllerTests,UserControllerTests" test
```

*改 DB、repository、security、transaction 或完整流程時，跑 integration / repository test：*

```powershell
.\mvnw.cmd "-Dtest=AuthFlowIntegrationTests,MemberAuthRepositoryTests" test
```

*交付前或使用者指定時，跑完整測試：*

```powershell
.\mvnw.cmd test
```

*目前完整測試結果：51 tests passed、0 failures、0 errors。*

## Email Provider 設定

*本機預設：*

- *`app.email.provider=in-memory`*
- *不呼叫外部寄信服務，適合本機開發與自動化測試。*

*正式展示環境使用 Mailjet：*

- *`app.email.provider=mailjet`*
- *`MAILJET_API_KEY`*
- *`MAILJET_API_SECRET`*
- *`MAILJET_SENDER_EMAIL`*
- *`MAILJET_SENDER_NAME`*
- *`APP_EMAIL_ACTIVATION_BASE_URL`*

*`MAILJET_SENDER_EMAIL` 必須是 Mailjet 後台已驗證的 sender address。`APP_EMAIL_ACTIVATION_BASE_URL` 是開通信中的前端確認頁 base URL，例如 `https://<frontend-url>/activate`。*

*在本機 PowerShell 測 Mailjet 時，請在同一個 shell 設定環境變數再啟動：*

```powershell
$env:MAILJET_API_KEY="<mailjet-api-key>"
$env:MAILJET_API_SECRET="<mailjet-api-secret>"
$env:MAILJET_SENDER_EMAIL="<verified-sender-email>"
$env:MAILJET_SENDER_NAME="DenDen Auth"
$env:APP_EMAIL_ACTIVATION_BASE_URL="http://localhost:3000/activate"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.email.provider=mailjet"
```

*不要將任何真實 API key、secret 或 sender credential commit 到 repository。*

## JWT 設定

*正式環境應透過環境變數設定：*

- *`APP_JWT_ISSUER`：建議使用部署後的 API base URL。*
- *`APP_JWT_SECRET`：至少 32 字元；主設定檔不提供預設值，避免 repository 內存在可被正式環境誤用的 JWT secret。*
- *`APP_JWT_ACCESS_TOKEN_EXPIRES_IN`：預設 `PT1H`。*

## GCP 部署方向

*預計使用：*

- *Cloud Run：部署 Spring Boot container。*
- *Cloud SQL for PostgreSQL：正式展示用 RDBMS。*
- *Secret Manager 或 Cloud Run environment variables：管理 JWT secret、Mailjet secret 與 DB password。*

*部署時需確認：*

- *Cloud Run service 設定 `min instances = 0`，降低展示環境成本。*
- *GCP 專案設定 billing alert。*
- *Cloud SQL 已建立 PostgreSQL database。*
- *Cloud Run 可連線 Cloud SQL。*
- *Flyway migration 可在雲端 DB 正常執行。*
- *`app.email.provider=mailjet`，並已設定 Mailjet 相關環境變數。*

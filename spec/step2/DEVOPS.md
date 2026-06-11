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

*正式展示環境建議使用 SendGrid：*

- *`app.email.provider=sendgrid` 或 `APP_EMAIL_PROVIDER=sendgrid`*
- *`SENDGRID_API_KEY`*
- *`SENDGRID_SENDER_EMAIL`*
- *`SENDGRID_SENDER_NAME`*
- *`APP_EMAIL_ACTIVATION_BASE_URL`*

*`SENDGRID_SENDER_EMAIL` 必須是 SendGrid 後台已完成 Single Sender Verification 的寄件地址，或是已完成 Domain Authentication 的網域底下地址。`APP_EMAIL_ACTIVATION_BASE_URL` 是開通信中的前端確認頁 base URL，例如 `https://<frontend-url>/activate`。*

*在本機 PowerShell 測 SendGrid 時，請在同一個 shell 設定環境變數再啟動：*

```powershell
$env:SENDGRID_API_KEY="<sendgrid-api-key>"
$env:SENDGRID_SENDER_EMAIL="<verified-sender-email>"
$env:SENDGRID_SENDER_NAME="DenDen Auth"
$env:APP_EMAIL_ACTIVATION_BASE_URL="http://localhost:3000/activate"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.email.provider=sendgrid"
```

*Mailjet 仍保留為備選 provider：*

- *`app.email.provider=mailjet`*
- *`MAILJET_API_KEY`*
- *`MAILJET_API_SECRET`*
- *`MAILJET_SENDER_EMAIL`*
- *`MAILJET_SENDER_NAME`*
- *`APP_EMAIL_ACTIVATION_BASE_URL`*

*`MAILJET_SENDER_EMAIL` 必須是 Mailjet 後台已驗證的 sender address。*

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
- *Secret Manager 或 Cloud Run environment variables：管理 JWT secret、Email provider secret 與 DB password。*

*部署時需確認：*

- *Cloud Run service 設定 `min instances = 0`，降低展示環境成本。*
- *GCP 專案設定 billing alert。*
- *Cloud SQL 已建立 PostgreSQL database。*
- *Cloud Run 可連線 Cloud SQL。*
- *Flyway migration 可在雲端 DB 正常執行。*
- *`app.email.provider=sendgrid` 或 `mailjet`，並已設定對應 Email provider 環境變數。*

## Cloud Run 部署前檢查

*批次 20A 先完成部署前 readiness，實際建立 GCP 資源會放在後續批次。*

*目前程式已具備：*

- *Cloud Run port readiness：`server.port=${PORT:8080}`，本機仍預設 8080，Cloud Run 可用平台注入的 `PORT`。*
- *正式 DB readiness：主設定檔沒有 hardcode datasource；Cloud Run 需透過環境變數注入 Cloud SQL PostgreSQL 連線資訊。*
- *Migration readiness：`spring.flyway.enabled=true` 與 `spring.jpa.hibernate.ddl-auto=validate`，啟動時會先跑 Flyway，再驗證 schema。*
- *JWT readiness：`APP_JWT_SECRET` 無正式預設值，部署時必須設定至少 32 字元 secret。*
- *Email readiness：正式展示可設定 `APP_EMAIL_PROVIDER=sendgrid`，並透過環境變數注入 SendGrid 設定。*
- *Source deploy readiness：已新增 `.gcloudignore`，避免把 `.git`、IDE 設定與 `target/` 等本機檔案送上 GCP。*

*Cloud Run 預計需要的環境變數：*

- *`SPRING_DOCKER_COMPOSE_ENABLED=false`：雲端不使用本機 Docker Compose。*
- *`SPRING_DATASOURCE_URL`：Cloud SQL PostgreSQL JDBC URL。*
- *`SPRING_DATASOURCE_USERNAME`：Cloud SQL database user。*
- *`SPRING_DATASOURCE_PASSWORD`：Cloud SQL database password，建議放 Secret Manager。*
- *`APP_JWT_ISSUER`：部署後 API base URL，例如 `https://<cloud-run-url>`。*
- *`APP_JWT_SECRET`：至少 32 字元，建議放 Secret Manager。*
- *`APP_EMAIL_PROVIDER=sendgrid`。*
- *`SENDGRID_API_KEY`：建議放 Secret Manager。*
- *`SENDGRID_SENDER_EMAIL`：SendGrid 已驗證 sender address。*
- *`SENDGRID_SENDER_NAME`：寄件者顯示名稱，例如 `DenDen Auth`。*
- *`APP_EMAIL_ACTIVATION_BASE_URL`：開通信導向的前端確認頁 base URL；若暫時沒有前端，展示時可用文件說明直接從 Email 複製 `activationToken` 到 Swagger 測試。*

*批次 20B 建議處理：*

- *建立 GCP project 與 billing alert。*
- *選定 region。*
- *建立 Cloud SQL PostgreSQL instance、database 與 user。*
- *決定 Cloud Run 到 Cloud SQL 的連線方式。*
- *建立 Secret Manager secrets，避免將 secret 直接寫在指令或文件中。*
- *整理實際部署指令或 Console 操作步驟。*

## GCP 展示環境資源

*批次 20B 已先建立或確認下列非敏感資源資訊：*

- *Project ID：`denden-member-auth`。*
- *Region：`asia-east1`。*
- *Cloud SQL instance ID：`denden-member-auth-postgres`。*
- *Cloud SQL connection name：預期完整格式為 `denden-member-auth:asia-east1:denden-member-auth-postgres`，實際值以 Cloud SQL instance overview 顯示為準。*
- *Database name：`member_auth`。*
- *Database username：`postgres`。*
- *Secret Manager secret names：依本文件建議建立，包含 JWT secret、datasource password、SendGrid API key 與 sender email。*

*注意：`denden-member-auth-postgres` 是 Cloud SQL instance ID；Cloud Run 設定 Cloud SQL connection 時通常需要完整 connection name，也就是 `project-id:region:instance-id`。*

*批次 20C 會處理：*

- *選定 Cloud Run 連 Cloud SQL 的 Java / JDBC 實作方式。*
- *已選定 Google Cloud SQL PostgreSQL Socket Factory，透過 JDBC URL 連線 Cloud SQL。*
- *補 Cloud Run datasource URL 與 Secret Manager 綁定方式。*
- *實際 deploy Cloud Run service。*
- *用部署後 Swagger URL 驗證 API 與 SendGrid 寄信流程。*

## Cloud Run 連 Cloud SQL 設定

*批次 20C-1 已新增 Google Cloud SQL PostgreSQL Socket Factory runtime dependency：*

- *Group ID：`com.google.cloud.sql`。*
- *Artifact ID：`postgres-socket-factory`。*
- *Version：`1.28.4`。*

*Cloud Run datasource URL 使用：*

```text
jdbc:postgresql:///member_auth?cloudSqlInstance=denden-member-auth:asia-east1:denden-member-auth-postgres&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

*Cloud Run service 建立時需設定 Cloud SQL connection：*

- *`denden-member-auth:asia-east1:denden-member-auth-postgres`。*

*Cloud Run environment variables 建議設定：*

- *`SPRING_DOCKER_COMPOSE_ENABLED=false`。*
- *`SPRING_DATASOURCE_URL=jdbc:postgresql:///member_auth?cloudSqlInstance=denden-member-auth:asia-east1:denden-member-auth-postgres&socketFactory=com.google.cloud.sql.postgres.SocketFactory`。*
- *`SPRING_DATASOURCE_USERNAME=postgres`。*
- *`APP_EMAIL_PROVIDER=sendgrid`。*
- *`SENDGRID_SENDER_NAME=DenDen Auth`。*

*Cloud Run secrets 建議以 environment variables 掛載：*

- *`SPRING_DATASOURCE_PASSWORD` ← `spring-datasource-password`。*
- *`APP_JWT_SECRET` ← `app-jwt-secret`。*
- *`SENDGRID_API_KEY` ← `sendgrid-api-key`。*
- *`SENDGRID_SENDER_EMAIL` ← `sendgrid-sender-email`。*

*第一次 deploy 前若還沒有 Cloud Run URL，可先暫設：*

- *`APP_JWT_ISSUER=https://placeholder`。*
- *`APP_EMAIL_ACTIVATION_BASE_URL=https://placeholder/activate`。*

*第一次部署成功後，回到 Cloud Run 更新成實際 service URL：*

- *`APP_JWT_ISSUER=https://<cloud-run-url>`。*
- *`APP_EMAIL_ACTIVATION_BASE_URL=https://<cloud-run-url>/activate`。*

## Cloud Run Dockerfile Build

*Cloud Run / Cloud Build 若選擇 Dockerfile build，會在 repository 根目錄尋找 `Dockerfile`。本專案已新增 multi-stage `Dockerfile`：*

- *Build stage：使用 Maven + Java 21 建置 Spring Boot jar。*
- *Runtime stage：使用 Java 21 JRE 執行 `member-auth-api-0.0.1-SNAPSHOT.jar`。*
- *測試不在 Docker build 階段執行；測試仍依本文件「測試指令」於部署前或使用者指定時執行。*
- *已新增 `.dockerignore`，避免本機 `.git`、IDE 設定、`target/` 與 log 檔進入 Docker build context。*

*Cloud Run 會透過 `PORT` 環境變數指定服務 port；主設定檔已設定 `server.port=${PORT:8080}`。*

*Cloud Run 對外使用 HTTPS，但 request 會先經過 Google 的 proxy 再進 Spring Boot。主設定檔已設定 `server.forward-headers-strategy=framework`，讓 Swagger / OpenAPI 產生的 server URL 使用外部 forwarded scheme，避免 Swagger UI 從 HTTPS 頁面送出 HTTP API request。*

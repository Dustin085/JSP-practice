# JSP-practice

書籍申請／採購／對帳的內部管理系統。用傳統手刻 Servlet/Spring MVC + MyBatis 堆疊練習，**刻意不用 Spring Boot、不用 JPA**，目的是先搞懂底層機制（`web.xml`/`WebApplicationInitializer`、DispatcherServlet 設定、容器托管的 filter chain、SQL 層 MyBatis mapper 手刻）再談框架自動化，對應台灣公部門／金融／保險業常見的維護型 Java 後端職缺會遇到的技術棧。

## 技術棧

- **Web**：Spring MVC 5.3.x（`DispatcherServlet` + JSP/JSTL 視圖，無 Spring Boot）
- **驗證/授權**：Spring Security 5.8.x（停在 5.8，因為 6.x 起改用 `jakarta.servlet`，跟本專案的 Servlet 4.0/Tomcat 9 不相容）
- **資料存取**：MyBatis 3（XML mapper）；`repository` package 底下另外留了一份 `JdbcTemplate` 版本的 `AuthorRepository` 作對照練習
- **資料庫**：H2 embedded，開發/測試都用記憶體模式（每次重啟 context 都是全新的資料庫 + `DataSeeder` 灌測試資料，沒有持久化）
- **其他**：Bean Validation（Hibernate Validator）、Jackson、Apache POI（Excel 匯出）、Lombok、JUnit 4 + Mockito

## 執行方式

1. Eclipse 匯入為 Dynamic Web Project，或直接用 Maven 打包成 war 丟到 Tomcat 9。
2. Java 21，Servlet 容器需支援 Servlet 4.0（對應 Tomcat 9）。
3. 不需要另外裝資料庫或跑 migration——H2 是記憶體內建的，`SeedDataListener` 會在 context 啟動時自動建表、灌測試資料。
4. 啟動後訪問 `/`，會被導向 `/login`。

### 測試帳號（`DataSeeder` 灌入，密碼皆為 `password123`）

| Email | 角色 | 說明 |
|---|---|---|
| `admin@example.com` | ADMIN | 書籍/作者/分類管理、申請審核、稽核紀錄、對帳 |
| `procurement@example.com` | PROCUREMENT | 採購作業（完成採購） |
| `user@example.com` | USER | 一般使用者，送出申請 |

## 功能

- 書籍／作者／分類 CRUD（`/books`、`/authors`、`/categories`）、Excel 匯出
- 書籍申請流程：送出 → 審核（核准/拒絕）→ 採購 → 入庫（`/requests`、`/procurement`）
- 對帳補救（`/reconciliations`）：處理「已核准卻缺採購項目」「已完成採購卻缺書籍」兩種資料不一致，支援手動解決/沖銷/重新連結/退回待處理
- 稽核紀錄（`/audit-logs`）：關鍵動作留痕，游標分頁（keyset pagination，非 OFFSET）

## 權限模型

角色（`RoleType`）：`ADMIN`／`PROCUREMENT`／`USER`，一人可多角色（`user_roles` 多對多表），`USER` 是每個帳號都會有的 baseline，`ADMIN`/`PROCUREMENT` 是額外加掛的角色。角色本身仍是寫死的 enum（不能在畫面上動態新增），判斷邏輯集中在 [`SecurityConfig`](src/main/java/com/example/jsppractice/config/SecurityConfig.java) 的 URL 規則 + JSP 裡的 `${currentUser.hasRole('...')}`。「給其他人角色」的管理畫面還沒做，見 [TODO.md](TODO.md)。

## 建置/測試

```bash
mvn -o test
```

（離線模式；相依套件已在本機 repo）

## 已知之後想做的方向

見 [TODO.md](TODO.md)：RBAC 資料庫化、給其他人角色的功能、Remember-me、Book 館藏數量、login CSRF、audit_log 顯示優化。

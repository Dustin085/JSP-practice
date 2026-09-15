# 之後想做的事

現階段刻意不做，但值得之後練習/擴充的方向。

## 企業級後端技能練習（依貼合度分級）

背景：為了準備台灣公部門/金融/保險的後端職缺，額外評估過 MQ/批次/鎖機制/舊系統整合/資安稽核這幾類
常見需求，哪些適合直接做在這個書籍系統上。依「跟現有堆疊相不相容」「網域裡有沒有自然的需求」分三級：

**第一級：直接做，貼合度高、不用加新基礎設施**（依序進行）
- [x] 排程對帳：新增 `ReconciliationScheduler`（`@Scheduled(cron = "0 0 2 * * *")`，每天凌晨 2 點跑），
      呼叫跟手動觸發（`/reconciliations/book-request-items`、`/reconciliations/procurement-items`）
      同一套 `ReconciliationService` 方法，兩者並存不衝突；`RootConfig` 加了 `@EnableScheduling`
- [ ] DB 鎖與隔離層級：`ProcurementServiceConcurrencyTest` 那個併發場景加碼——悲觀鎖用
      `SELECT ... FOR UPDATE`（H2 支援）；樂觀鎖因為沒有 JPA，`@Version` 用不上，改成手刻
      「`UPDATE ... WHERE version = ?` 判斷影響筆數」；隔離層級直接在 `@Transactional(isolation=...)` 上試
- [ ] MDC traceId：Filter 幫每個請求塞 `traceId` 進 MDC，改 logback pattern，跟 `audit_logs`
      是互補（誰做了什麼 vs. 一次請求的完整軌跡）的兩種追溯機制

**第二級：值得做，但要先幫這個書籍系統「發明」一個合理情境，不是照搬**
- SFTP + 固定長度電文解析：掰一個「廠商每天半夜丟一份採購到貨清單到 SFTP，排程去抓、解析、更新
  `procurement_items`」的情境，可以跟上面的排程/批次練習串起來
- 資料遮罩/AES 加密：現在網域沒有身分證字號/信用卡號這類敏感欄位，`password_hash` 已經是 bcrypt
  不需要再加密，要做的話要先想清楚要加密哪個欄位，不要硬塞一個假欄位進業務流程
- OpenAPI/Swagger：目前只有 `HealthController` 回 JSON，其餘都是 JSP 伺服器端渲染，文件化的東西
  太少，等真的加一組 JSON API 才值得上

**第三級：先不要塞進這個專案，另外開一個 repo 練**
- RabbitMQ/Kafka、SOAP、Saga/2PC：都需要外接一整套基礎設施或協定，而且很難在書籍申請/採購這個
  網域裡長出自然的需求，硬塞會變成「為了用技術而用技術」，違背這個專案「網域驅動、不無中生有」的
  做法（職務分離、對帳補救都是先有真實情境才做）

## RBAC 資料庫化

目前角色權限判斷是寫死在程式碼裡（`RoleType` enum + `AdminCheckInterceptor` 裡的 `if (role != ADMIN)`），
適合角色數量少、權限切分穩定的情境。

之後如果想練習更貼近大型企業系統的做法，可以改成資料庫驅動：
- 新增 `roles`、`permissions`、`role_permissions` 表
- 角色本身也能透過畫面新增/編輯（不再是寫死的 enum）
- 權限判斷改成查表，而不是寫死的 if 判斷

這個改動的價值在於「不改程式碼、不重新部署就能調整權限」，目前專案角色少（ADMIN/USER，之後可能加
簽核流程的 SUPERVISOR/FINANCE）用 enum 硬判斷還夠用，先不做。

## Remember-me

登入/權限/CSRF/登出已經全部換成 Spring Security（見 `SecurityConfig`），手刻的
`LoginCheckInterceptor`/`RoleAccessInterceptor`/`CsrfInterceptor`/`LogoutController` 都已經刪除，
JSP 的 `currentUser` 也已經改成從 `Authentication`（透過 `CurrentUserModelAdvice`）取得，不再靠 session
手動橋接。Remember-me 是唯一還沒做的內建機制，之後想練習的話可以加。

## 給其他人角色的功能（角色指派畫面）

一人多角色（`user_roles` 多對多）已經做完：`users.role` 單一欄位換成 `user_roles` 表，
每個帳號一定有 `USER` 這個 baseline 角色，`ADMIN`/`PROCUREMENT` 是額外加掛的角色，
`CustomUserDetails.getAuthorities()`/JSP 的 `currentUser.hasRole(...)` 都已經看的是角色集合。
`role` 仍然是寫死的 `RoleType` enum，不是資料庫可以動態新增的東西（那是更大的「RBAC 資料庫化」題目）。

目前 `user_roles` 只能靠 `DataSeeder`/直接改資料庫調整，還沒有「管理員在畫面上把某個角色加/移除到
某個使用者身上」的功能。之後要做的話：
- 找一支路徑，例如 `POST /users/{id}/roles`、`DELETE /users/{id}/roles/{role}`，限 `hasRole("ADMIN")`
- 異動要記進 `audit_logs`（`AuditActionType` 加 `GRANT_ROLE`/`REVOKE_ROLE`，`AuditEntityType` 加 `USER`），
  不要在 `user_roles` 本身加 `granted_by`/`granted_at` 欄位——跟其他異動一樣統一走 audit_log 機制
- 要決定：可不可以拿掉自己身上最後一個角色、可不可以拿掉自己的 ADMIN（防呆，不然可能把自己鎖在外面）

## Book 加上館藏數量（庫存管理）

目前 `books` 是「目錄型」設計（一列 = 一種書目），沒有追蹤館藏副本數量。簽核流程的採購請求目前也
只支援「一個 item = 一本書」，不支援「同一本書買多本」。

要支援數量，需要決定：`books` 加一個數量欄位（同書目查到就累加），還是允許同一書目在 `books` 出現
多列（不建議，作者/分類關聯要重複建立）。這牽動到之後如果要做「借閱/歸還」功能時「剩幾本可借」的
邏輯，是一個獨立於簽核流程之外的功能主題，先不做。

## CSRF：login CSRF

`JSESSIONID` 的 `SameSite=Lax` 已經做完，靠 Tomcat 的 `CookieProcessor` 設定（Servlet 4.0 的
`web.xml` 沒有標準寫法），當作 Spring Security CSRF token 之外的 defense-in-depth 第二層。
`src/main/webapp/META-INF/context.xml` 有加這個屬性，是給「真的打包成 war 丟進 Tomcat webapps/」
這種標準部署方式用的；但本機用 Eclipse 開發時，Eclipse 是直接把 Context 寫死在它自己管理、workspace
底下的 `Servers/Tomcat vX.x Server at localhost-config/server.xml`，不會去讀 war 裡的 context.xml，
所以本機那份也要手動加 `sameSiteCookies="lax"`（不在 git 版控範圍，重灌 workspace 要記得重設）。

還沒做的：`/login` 表單本身目前沒有 CSRF 保護（送出前 session 還沒有 token），理論上有「login CSRF」
風險（騙使用者用攻擊者的帳號登入），優先度較低，先不做。

## audit_log 列表頁：顯示優化

`audit-logs/list.jsp` 目前 `userId` 直接顯示數字、`detail` 直接用 `Map.toString()` 顯示，是刻意先
從簡的版本。之後如果要好看一點：
- `userId` 要顯示成 email/姓名，需要另外做一個 `AuditLogSummary` DTO 加 join `users` 表（跟
  `BookRequestSummary`/`ProcurementSummary` 同一套做法）。
- `detail` 的 JSON 內容可以用 JS 或後端排版成更易讀的格式，而不是原始的 `Map.toString()`。

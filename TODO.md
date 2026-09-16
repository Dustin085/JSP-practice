# 之後想做的事

現階段刻意不做，但值得之後練習/擴充的方向。

## 企業級後端技能練習（依貼合度分級）

背景：為了準備台灣公部門/金融/保險的後端職缺，額外評估過 MQ/批次/鎖機制/舊系統整合/資安稽核這幾類
常見需求，哪些適合直接做在這個書籍系統上。依「跟現有堆疊相不相容」「網域裡有沒有自然的需求」分三級：

**第一級：直接做，貼合度高、不用加新基礎設施**（依序進行）
- [x] 排程對帳：新增 `ReconciliationScheduler`（`@Scheduled(cron = "0 0 2 * * *")`，每天凌晨 2 點跑），
      呼叫跟手動觸發（`/reconciliations/book-request-items`、`/reconciliations/procurement-items`）
      同一套 `ReconciliationService` 方法，兩者並存不衝突；`RootConfig` 加了 `@EnableScheduling`
- [x] DB 鎖：悲觀鎖（`ProcurementServiceImpl.completeProcurement()` 的 `findByIdForUpdate`）、
      `book_requests.approve()/reject()` 的 compare-and-swap 式樂觀鎖（拿 status 當版本判斷）都已經有了。
      新增的是 `books.version`：真正的教科書式樂觀鎖，`Book.update()` 任意欄位編輯都受保護（不像
      approve/reject 只保護狀態轉換），衝突時丟 `org.springframework.dao.OptimisticLockingFailureException`
      （跟 JPA `@Version` 衝突丟的是同一個 Spring 例外）
      - [x] 編輯衝突時保留使用者輸入：`BookController.update()` 撞到 `OptimisticLockingFailureException`
            時改成重新渲染 `books/form`（不是 redirect 回清單），`book` 這個表單綁定物件本身就帶著
            使用者剛剛輸入的內容，直接放回 model 就好，不用重查資料庫。`version` 換成資料庫目前真正的
            最新值（不是使用者送出的那個舊值）——不然使用者原封不動再送一次還是會撞到同一個衝突。
            另外用 `conflictMessage` 顯示資料庫目前最新的書名，讓使用者自己決定要不要蓋過去，
            而不是靜默覆蓋。表單按鈕在有衝突時換成「確認覆蓋並儲存」+ `onclick="return confirm(...)"`
            （跟 categories/authors 刪除按鈕、requests 核准/拒絕同一種既有寫法），逼使用者對第二次送出
            按一次確認，不會因為反射性連點兩下就把別人的修改静默蓋掉
- [x] 隔離層級：新增 `TransactionIsolationLevelTest`，繞過 Spring 的 `@Transactional`/service 層，
      直接控制 JDBC `Connection` 自己開交易，用 `CountDownLatch` 卡住「A 交易讀一次 → B 交易 commit →
      A 交易再讀一次」的時序，驗證 H2 在 `READ_COMMITTED`（會看到 B 剛 commit 的新值）跟
      `REPEATABLE_READ`（維持交易開始那一刻的快照，看不到 B 的異動）下行為確實不同
- [x] MDC traceId：新增 `TraceIdFilter`（`com.example.jsppractice.filter`），透過
      `SecurityWebApplicationInitializer.beforeSpringSecurityFilterChain()` 掛在最前面（比
      `CharacterEncodingFilter` 還前面，範圍要包住整個請求），logback pattern 加了
      `[traceId=%X{traceId}]`，跟 `audit_logs` 是互補（誰做了什麼 vs. 一次請求的完整軌跡）的
      兩種追溯機制

**第二級：值得做，但要先幫這個書籍系統「發明」一個合理情境，不是照搬**
- SFTP + 固定長度電文解析：掰一個「廠商每天半夜丟一份採購到貨清單到 SFTP，排程去抓、解析、更新
  `procurement_items`」的情境，可以跟上面的排程/批次練習串起來
- [x] 資料遮罩：挑了 email，新增 `/users`（ADMIN 限定）使用者列表頁，`EmailMasker`（`util` package，
      跟 `DisplayTime` 同一種靜態工具類 pattern）+ `User.getMaskedEmail()`——只留本地部分第一/最後一個字，
      網域不遮。header 加了 ADMIN 才看得到的「使用者管理」連結
- [x] AES 加密（存放加密）：`users.email` 現在存的是 AES-256/GCM 密文，不是明文。
      - `AesEncryptor`/`EmailLookupHasher`（`crypto` package）：加密跟盲索引雜湊用兩把分開來源的密鑰
        （`AES_SECRET_KEY`/`EMAIL_HASH_KEY` 環境變數，SHA-256 雜湊成 32 bytes 當金鑰；沒設環境變數
        時退回明確標記「僅供本機開發」的預設值，正式環境一定要蓋掉）
      - `EncryptedStringTypeHandler`（`mybatis` package）：掛在 `UserMapper.xml` 的 `email` 欄位，
        讓 `User`/service/controller 全程只看到明文，加解密完全在 JDBC 存取這層做掉
      - `email_lookup_hash`（`schema.sql` 新欄位，`HMAC-SHA256(email)` 的 hex 字串）：登入查找、
        唯一性約束都改靠它（`WHERE email = ?` 對密文沒用，`UserMapper.findByEmail` 改名
        `findByEmailHash`），真正的 email 明文只在讀出來、要顯示/使用時才解密
      - `AuthServiceImpl` 原本直接注入 `UserMapper` 查重複註冊，改成一律走
        `UserService.findByEmail()`（雜湊查找邏輯只寫在 `UserServiceImpl` 一個地方）
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

## ~~Remember-me~~（已完成）

登入/權限/CSRF/登出已經全部換成 Spring Security（見 `SecurityConfig`），手刻的
`LoginCheckInterceptor`/`RoleAccessInterceptor`/`CsrfInterceptor`/`LogoutController` 都已經刪除，
JSP 的 `currentUser` 也已經改成從 `Authentication`（透過 `CurrentUserModelAdvice`）取得，不再靠 session
手動橋接——這個風險排除之後，Remember-me 也補上了：用 `PersistentTokenRepository`
（`JdbcTokenRepositoryImpl` + 新表 `persistent_logins`），不是單純簽章 cookie 的
`TokenBasedRememberMeServices`，多了 series/token 輪替的偷竊偵測能力，是 Spring Security 建議的
正式做法。登入頁多一個「記住我」checkbox（`name="remember-me"`，Security 預設參數名稱）。

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

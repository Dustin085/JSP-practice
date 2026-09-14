# 之後想做的事

現階段刻意不做，但值得之後練習/擴充的方向。

## 對帳補救：procurement_book（已完成採購但缺書籍）重新連結 / 退回待處理

`/reconciliations` 的對帳功能（`ReconciliationService`/`ReconciliationChecker`/`ReconciliationItem.status`）已經做完
兩種檢查、加上「標記已處理」「沖銷」兩個通用補救動作，另外針對「已核准但缺採購項目」這種落差做了「補建」
（`backfillProcurementItem`，直接建缺的那筆 `procurement_item`）。

但「已完成採購但缺書籍」（`PROCUREMENT_BOOK` 這個檢查）目前**沒有**對應的自動補建，只能標記已處理或沖銷，
原因是系統猜不出「該補哪一本書」。討論後決定：沖銷應該是調查過後、確認真的沒辦法/不值得修才用的最後手段，
不該是遇到問題的直接反應，中間應該先加兩個更貼近真實原因的補救動作：

1. **重新連結（relink）**：很多時候不是書真的沒建，是 `procurement_items.book_id` 這個關聯斷了（書其實已經在
   `books` 表裡）。用該筆 `procurement_item` 對應的 `book_request_item`（title/isbn）去 `books` 查有沒有相符的
   既有紀錄，有的話讓使用者確認後把 `book_id` 接上，明細標記 `RESOLVED`，寫一筆 audit log。
2. **退回待處理（revert to pending）**：如果書真的從來沒建立過（採購被誤標記完成、流程沒走完），比起憑空生一本
   書，更穩妥的做法是把 `procurement_items.status` 退回 `PENDING`（清掉 `procured_at`/`procured_by`/`book_id`），
   讓它重新走回 `/procurement` 既有的「完成採購」畫面，由採購人員用正常流程補上正確的書籍資料。
3. **沖銷**：調查完兩種都不適用（例如書確定不會再進、或是舊資料本身就有缺陷不值得追）才使用，`detail`/備註要
   留下「為什麼決定放棄」的理由。

實作上要新增的東西跟「補建」是同一個量級：`AuditActionType` 可能要加一個新值（如 `RELINK`）、`books` 查詢比對
邏輯（title/isbn 相似度要多寬鬆是個要想清楚的細節）、`ReconciliationController`/JSP 加對應按鈕與（重新連結要）
一個選書的小表單。

## RBAC 資料庫化

目前角色權限判斷是寫死在程式碼裡（`RoleType` enum + `AdminCheckInterceptor` 裡的 `if (role != ADMIN)`），
適合角色數量少、權限切分穩定的情境。

之後如果想練習更貼近大型企業系統的做法，可以改成資料庫驅動：
- 新增 `roles`、`permissions`、`role_permissions` 表
- 角色本身也能透過畫面新增/編輯（不再是寫死的 enum）
- 權限判斷改成查表，而不是寫死的 if 判斷

這個改動的價值在於「不改程式碼、不重新部署就能調整權限」，目前專案角色少（ADMIN/USER，之後可能加
簽核流程的 SUPERVISOR/FINANCE）用 enum 硬判斷還夠用，先不做。

## 換裝 Spring Security

目前登入/權限/密碼雜湊都是手刻的（`AuthService`、`LoginCheckInterceptor`、`AdminCheckInterceptor`、
`BCryptPasswordEncoder` 單獨引入 `spring-security-crypto`），用意是先搞懂機制原理。

之後可以練習把這一整套換成正式的 Spring Security，體驗看看框架幫忙自動化了哪些事：
- `UserDetailsService`、`SecurityFilterChain`（或 XML 版的 `<http>` 設定）
- CSRF 防護（目前專案完全沒有，是已知缺口）
- Remember-me、session 固定攻擊防護等內建機制

跟專案一路以來「先手刻理解、再看框架自動化了什麼」的學習模式一致（JDBC → MyBatis → JPA 的練習方式
也是同樣邏輯）。

## 一人多角色（user_roles 多對多）

簽核流程討論時發現：如果同一人需要同時具備審核（ADMIN）跟採購（PROCUREMENT）身分，現在 `users.role`
單一欄位存不下。目前先採用「審核人跟採購人不能是同一人」當成職務分離（segregation of duties）原則，
不用真的改資料庫。

之後如果想練習，可以拆成 `users`、`roles`、`user_roles` 三張表（跟 `books_categories` 同一種多對多
pattern），一個使用者能同時掛多個角色，權限判斷從「role 是不是等於 X」改成「角色清單裡有沒有 X」。
這個改動比「RBAC 資料庫化」小，只解決「一人多角色」，不解決「角色/權限可以動態設定」。

## Book 加上館藏數量（庫存管理）

目前 `books` 是「目錄型」設計（一列 = 一種書目），沒有追蹤館藏副本數量。簽核流程的採購請求目前也
只支援「一個 item = 一本書」，不支援「同一本書買多本」。

要支援數量，需要決定：`books` 加一個數量欄位（同書目查到就累加），還是允許同一書目在 `books` 出現
多列（不建議，作者/分類關聯要重複建立）。這牽動到之後如果要做「借閱/歸還」功能時「剩幾本可借」的
邏輯，是一個獨立於簽核流程之外的功能主題，先不做。

## CSRF：SameSite cookie 與 login CSRF

今天做的 CSRF token（Synchronizer Token Pattern）只涵蓋登入後的操作區域（`/books/**` 等，跟
`LoginCheckInterceptor` 同範圍）。兩個可以之後加強的方向：
- 幫 Tomcat 的 `JSESSIONID` cookie 加上 `SameSite=Lax`（`web.xml` 的 `<cookie-config>` 或
  Tomcat 的 `CookieProcessor`），當作 defense-in-depth 的第二層防護，不是取代 CSRF token。
- `/login` 表單本身目前沒有 CSRF 保護（送出前 session 還沒有 token），理論上有「login CSRF」風險
  （騙使用者用攻擊者的帳號登入），優先度較低，先不做。

## audit_log 列表頁：顯示優化

`audit-logs/list.jsp` 目前 `userId` 直接顯示數字、`detail` 直接用 `Map.toString()` 顯示，是刻意先
從簡的版本。之後如果要好看一點：
- `userId` 要顯示成 email/姓名，需要另外做一個 `AuditLogSummary` DTO 加 join `users` 表（跟
  `BookRequestSummary`/`ProcurementSummary` 同一套做法）。
- `detail` 的 JSON 內容可以用 JS 或後端排版成更易讀的格式，而不是原始的 `Map.toString()`。

# 之後想做的事

現階段刻意不做，但值得之後練習/擴充的方向。

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

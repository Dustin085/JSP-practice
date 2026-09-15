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

CREATE TABLE IF NOT EXISTS users (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	-- AES-256/GCM 密文（含隨機 IV），不是明文，長度會比原本的 email 長不少（Base64 編碼 IV+密文+認證標籤）
	email VARCHAR(255) NOT NULL,
	-- 盲索引：HMAC-SHA256(email) 的 hex 字串，固定 64 字元。email 加密後不能再拿來查找
	-- （同一個 email 每次加密結果都不一樣），登入改成查這個確定性雜湊欄位，唯一性也改靠它保證
	email_lookup_hash VARCHAR(64) NOT NULL,
	name VARCHAR(200),
	password_hash VARCHAR(200) NOT NULL,
	CONSTRAINT uk_user_email_lookup_hash UNIQUE (email_lookup_hash)
);

-- 一人多角色：USER 是每個帳號都會有的 baseline，ADMIN/PROCUREMENT 是額外加掛的角色。
-- role 還是沿用 RoleType enum 的字串值（跟舊的 users.role 一樣用 CHECK 限制），不是另外拆一張
-- roles 表——角色本身可不可以動態新增是「RBAC 資料庫化」那個更大的題目，這裡不處理。
CREATE TABLE IF NOT EXISTS user_roles (
	user_id BIGINT NOT NULL,
	role VARCHAR(20) NOT NULL CONSTRAINT chk_user_roles_role CHECK (role IN ('ADMIN', 'USER', 'PROCUREMENT')),
	PRIMARY KEY (user_id, role),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Remember-me（PersistentTokenRepository）用，欄位名稱/型別是 Spring Security 的
-- JdbcTokenRepositoryImpl 內建 SQL 指定的固定格式，不能自己改名。series 是每張 cookie（每次
-- 「記住我」登入）的身分，token 每次被拿來自動登入都會輪替一次；series 對得上但 token 對不上，
-- 代表這張 cookie 已經被用過一次卻又出現第二次，視為被偷、整個 series 作廢——這是它比單純
-- 雜湊簽章 cookie 多出來的偷竊偵測能力。
CREATE TABLE IF NOT EXISTS persistent_logins (
	username VARCHAR(64) NOT NULL,
	series VARCHAR(64) PRIMARY KEY,
	token VARCHAR(64) NOT NULL,
	last_used TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	user_id BIGINT NOT NULL,
	audited_at TIMESTAMP NOT NULL,
	action VARCHAR(100) NOT NULL,
	entity_type VARCHAR(100) NOT NULL,
	entity_id BIGINT NOT NULL,
	detail JSON NOT NULL,
	CONSTRAINT fk_audit_log_user
	FOREIGN KEY (user_id)
    REFERENCES users(id)
);

CREATE INDEX idx_audit_log_entity ON audit_logs(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    isbn VARCHAR(20),
    author_id BIGINT ,
    published_year INT,
    -- 樂觀鎖：UPDATE 時一併檢查 WHERE version = 舊值、SET version = version + 1，
    -- 兩個人同時編輯同一本書時，後 commit 的那個人會因為 version 對不上而被擋下來
    version INT NOT NULL,

    CONSTRAINT fk_book_author
    FOREIGN KEY (author_id)
    REFERENCES authors(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS books_categories (
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, category_id),
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS book_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    approver_id BIGINT,
    status VARCHAR(20) NOT NULL CONSTRAINT chk_book_request_status CHECK (status IN ('PENDING', 'REJECTED','APPROVED')),
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    idempotency_key VARCHAR(100) NOT NULL,

    CONSTRAINT fk_book_requests_requester
    FOREIGN KEY (requester_id)
    REFERENCES users(id),

    CONSTRAINT fk_book_requests_approver
    FOREIGN KEY (approver_id)
    REFERENCES users(id),

    CONSTRAINT uk_book_request_idempotency_key
    UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS book_request_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_request_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    isbn VARCHAR(20),
    author_id BIGINT,
    published_year INT,
    estimated_price DECIMAL(10,2),

    CONSTRAINT fk_book_request_item_book_request
    FOREIGN KEY (book_request_id)
    REFERENCES book_requests(id),

    CONSTRAINT fk_book_request_item_author
    FOREIGN KEY (author_id)
    REFERENCES authors(id)
);

CREATE TABLE IF NOT EXISTS procurement_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_request_item_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT chk_procurement_item_status CHECK (status IN ('PENDING', 'COMPLETED')),
    procured_by BIGINT,
    procured_at TIMESTAMP,
    book_id BIGINT,

    CONSTRAINT fk_procurement_item_request_item
    FOREIGN KEY (book_request_item_id)
    REFERENCES book_request_items(id),

    CONSTRAINT fk_procurement_item_procured_by
    FOREIGN KEY (procured_by)
    REFERENCES users(id),

    CONSTRAINT fk_procurement_item_book
    FOREIGN KEY (book_id)
    REFERENCES books(id)
);

CREATE TABLE IF NOT EXISTS reconciliations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciled_at TIMESTAMP NOT NULL,
    reconciliation_type VARCHAR(50) NOT NULL
        CONSTRAINT chk_reconciliation_type CHECK (reconciliation_type IN ('BOOK_REQUEST_PROCUREMENT', 'PROCUREMENT_BOOK')),
    status VARCHAR(30) NOT NULL
        CONSTRAINT chk_reconciliation_status CHECK (status IN ('COMPLETED_NO_DISCREPANCY', 'COMPLETED_WITH_DISCREPANCY', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS reconciliation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciliation_id BIGINT NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    discrepancy_type VARCHAR(30) NOT NULL
        CONSTRAINT chk_reconciliation_item_discrepancy_type CHECK (discrepancy_type IN ('ONLY_IN_SOURCE', 'ONLY_IN_TARGET', 'MISMATCHED')),
    status VARCHAR(20) NOT NULL
        CONSTRAINT chk_reconciliation_item_status CHECK (status IN ('UNRESOLVED', 'RESOLVED', 'WRITTEN_OFF')),
    detail JSON NOT NULL,

    CONSTRAINT fk_reconciliation_item_reconciliation
    FOREIGN KEY (reconciliation_id)
    REFERENCES reconciliations(id)
);

CREATE INDEX idx_reconciliation_item_reconciliation ON reconciliation_items(reconciliation_id);
CREATE INDEX idx_reconciliation_item_entity ON reconciliation_items(entity_type, entity_id);

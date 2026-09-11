CREATE TABLE IF NOT EXISTS users (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(100) NOT NULL,
	name VARCHAR(200),
	password_hash VARCHAR(200) NOT NULL,
	role VARCHAR(20) NOT NULL CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'USER', 'PROCUREMENT')),
	CONSTRAINT uk_user_email UNIQUE (email)
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
    detail JSON NOT NULL,

    CONSTRAINT fk_reconciliation_item_reconciliation
    FOREIGN KEY (reconciliation_id)
    REFERENCES reconciliations(id)
);

CREATE INDEX idx_reconciliation_item_reconciliation ON reconciliation_items(reconciliation_id);

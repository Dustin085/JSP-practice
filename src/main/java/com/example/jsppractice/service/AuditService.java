package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.AuditLogCursor;
import com.example.jsppractice.model.CursorPage;

public interface AuditService {
	CursorPage<AuditLog> findNextPage(AuditLogCursor cursor, int pageSize);

	CursorPage<AuditLog> findPreviousPage(AuditLogCursor cursor, int pageSize);

	AuditLog create(AuditLog auditLog);

	List<AuditLog> findByEntity(AuditEntityType entityType, Long entityId);

	AuditLog createWhenFailure(AuditLog auditLog);
}

package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.CursorPage;

public interface AuditService {
	CursorPage<AuditLog> findNextPage(Long cursorId, int pageSize);

	CursorPage<AuditLog> findPreviousPage(Long cursorId, int pageSize);

	AuditLog create(AuditLog auditLog);

	List<AuditLog> findByEntity(AuditEntityType entityType, Long entityId);
}

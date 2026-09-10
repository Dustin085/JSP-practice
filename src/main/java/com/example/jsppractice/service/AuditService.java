package com.example.jsppractice.service;

import java.util.List;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

public interface AuditService {
	PageRes<AuditLog> findAll(PageReq pageReq);

	AuditLog create(AuditLog auditLog);

	List<AuditLog> findByEntity(AuditEntityType entityType, Long entityId);
}

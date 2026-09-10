package com.example.jsppractice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.PageReq;

public interface AuditLogMapper {
	List<AuditLog> findAllPaged(@Param("pageReq") PageReq pageReq);

	long count();

	List<AuditLog> findByEntity(@Param("entityType") AuditEntityType entityType, @Param("entityId") Long entityId);

	void insert(AuditLog auditLog);
}

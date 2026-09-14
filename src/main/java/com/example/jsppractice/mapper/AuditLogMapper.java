package com.example.jsppractice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.AuditLogCursor;

public interface AuditLogMapper {
	List<AuditLog> findNext(@Param("cursor") AuditLogCursor cursor, @Param("limit") int limit);

	List<AuditLog> findPrevious(@Param("cursor") AuditLogCursor cursor, @Param("limit") int limit);

	List<AuditLog> findByEntity(@Param("entityType") AuditEntityType entityType, @Param("entityId") Long entityId);

	void insert(AuditLog auditLog);
}

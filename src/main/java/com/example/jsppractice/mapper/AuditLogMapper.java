package com.example.jsppractice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;

public interface AuditLogMapper {
	List<AuditLog> findNext(@Param("cursorId") Long cursorId, @Param("limit") int limit);

	List<AuditLog> findPrevious(@Param("cursorId") Long cursorId, @Param("limit") int limit);

	List<AuditLog> findByEntity(@Param("entityType") AuditEntityType entityType, @Param("entityId") Long entityId);

	void insert(AuditLog auditLog);
}

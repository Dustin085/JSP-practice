package com.example.jsppractice.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.mapper.AuditLogMapper;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.CursorPage;

@Service
public class AuditServiceImpl implements AuditService {
	private final AuditLogMapper auditLogMapper;

	public AuditServiceImpl(AuditLogMapper auditLogMapper) {
		this.auditLogMapper = auditLogMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<AuditLog> findNextPage(Long cursorId, int pageSize) {
		// 多抓一筆，用來判斷「下一頁」是否還有資料，不用另外下一次 COUNT
		List<AuditLog> rows = auditLogMapper.findNext(cursorId, pageSize + 1);
		boolean hasNext = rows.size() > pageSize;
		if (hasNext) {
			rows = rows.subList(0, pageSize);
		}
		boolean hasPrev = cursorId != null;
		return new CursorPage<>(rows, hasNext, hasPrev);
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<AuditLog> findPreviousPage(Long cursorId, int pageSize) {
		// ASC 抓「緊接在 cursorId 之後」最近的 pageSize+1 筆，反轉回 DESC 給畫面顯示
		List<AuditLog> rows = auditLogMapper.findPrevious(cursorId, pageSize + 1);
		boolean hasPrev = rows.size() > pageSize;
		if (hasPrev) {
			rows = rows.subList(0, pageSize);
		}
		Collections.reverse(rows);
		// 這裡一定是從某一頁按「上一頁」過來的，代表原本那頁（更舊的資料）必然存在
		boolean hasNext = true;
		return new CursorPage<>(rows, hasNext, hasPrev);
	}

	@Override
	@Transactional
	public AuditLog create(AuditLog auditLog) {
		auditLogMapper.insert(auditLog);
		return auditLog;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> findByEntity(AuditEntityType entityType, Long entityId) {
		return auditLogMapper.findByEntity(entityType, entityId);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AuditLog createWhenFailure(AuditLog auditLog) {
		auditLogMapper.insert(auditLog);
		return auditLog;
	}

}

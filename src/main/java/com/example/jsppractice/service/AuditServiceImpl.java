package com.example.jsppractice.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.AuditLogSummary;
import com.example.jsppractice.mapper.AuditLogMapper;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.AuditLogCursor;
import com.example.jsppractice.model.CursorPage;
import com.example.jsppractice.model.User;

@Service
public class AuditServiceImpl implements AuditService {
	private final AuditLogMapper auditLogMapper;
	private final UserMapper userMapper;

	public AuditServiceImpl(AuditLogMapper auditLogMapper, UserMapper userMapper) {
		this.auditLogMapper = auditLogMapper;
		this.userMapper = userMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<AuditLogSummary> findNextPage(AuditLogCursor cursor, int pageSize) {
		// 多抓一筆，用來判斷「下一頁」是否還有資料，不用另外下一次 COUNT
		List<AuditLog> rows = auditLogMapper.findNext(cursor, pageSize + 1);
		boolean hasNext = rows.size() > pageSize;
		if (hasNext) {
			rows = rows.subList(0, pageSize);
		}
		boolean hasPrev = cursor != null;
		return new CursorPage<>(withUserEmail(rows), hasNext, hasPrev);
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<AuditLogSummary> findPreviousPage(AuditLogCursor cursor, int pageSize) {
		// Mapper 已經用 SQL 排好 DESC 顯示順序：抓「緊接在 cursor 之後」最近的 pageSize+1 筆，
		// 多抓的那 1 筆是離 cursor 最遠（最新）的，DESC 排序下會排在最前面（index 0）
		List<AuditLog> rows = auditLogMapper.findPrevious(cursor, pageSize + 1);
		boolean hasPrev = rows.size() > pageSize;
		if (hasPrev) {
			rows = rows.subList(1, rows.size());
		}
		// 這裡一定是從某一頁按「上一頁」過來的，代表原本那頁（更舊的資料）必然存在
		boolean hasNext = true;
		return new CursorPage<>(withUserEmail(rows), hasNext, hasPrev);
	}

	// 先照 audit_logs 自己的邏輯分頁完，才批次查這一頁涉及到的 user email（deferred join，
	// 跟 BookMapper.search() 分頁時先只查 id 再批次撈完整資料是同一個道理）——不要在分頁 SQL
	// 裡直接 JOIN users，遊標分頁的排序/邊界比較只認 audit_logs 自己的 audited_at/id，
	// JOIN 進來的欄位跟這個無關，只會讓那條已經夠複雜的分頁 SQL 更難懂。
	private List<AuditLogSummary> withUserEmail(List<AuditLog> rows) {
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> userIds = rows.stream().map(AuditLog::getUserId).distinct().collect(Collectors.toList());
		Map<Long, String> emailByUserId = userMapper.findByIds(userIds).stream()
				.collect(Collectors.toMap(User::getId, User::getEmail));
		return rows.stream().map(row -> AuditLogSummary.from(row, emailByUserId.get(row.getUserId())))
				.collect(Collectors.toList());
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

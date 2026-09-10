package com.example.jsppractice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.mapper.AuditLogMapper;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;

@Service
public class AuditServiceImpl implements AuditService {
	private final AuditLogMapper auditLogMapper;

	public AuditServiceImpl(AuditLogMapper auditLogMapper) {
		this.auditLogMapper = auditLogMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public PageRes<AuditLog> findAll(PageReq pageReq) {
		return new PageRes<AuditLog>(auditLogMapper.findAllPaged(pageReq), pageReq.pageNumber(), pageReq.pageSize(),
				auditLogMapper.count());
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

}

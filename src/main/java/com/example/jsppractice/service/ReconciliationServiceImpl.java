package com.example.jsppractice.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.mapper.ReconciliationItemMapper;
import com.example.jsppractice.mapper.ReconciliationMapper;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationItemStatus;
import com.example.jsppractice.model.ReconciliationType;
import com.example.jsppractice.model.User;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {
	private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

	private final ReconciliationChecker reconciliationChecker;
	private final ReconciliationFailureRecorder reconciliationFailureRecorder;
	private final ReconciliationMapper reconciliationMapper;
	private final ReconciliationItemMapper reconciliationItemMapper;
	private final ProcurementItemMapper procurementItemMapper;
	private final AuditService auditService;

	public ReconciliationServiceImpl(ReconciliationChecker reconciliationChecker,
			ReconciliationFailureRecorder reconciliationFailureRecorder, ReconciliationMapper reconciliationMapper,
			ReconciliationItemMapper reconciliationItemMapper, ProcurementItemMapper procurementItemMapper,
			AuditService auditService) {
		this.reconciliationChecker = reconciliationChecker;
		this.reconciliationFailureRecorder = reconciliationFailureRecorder;
		this.reconciliationMapper = reconciliationMapper;
		this.reconciliationItemMapper = reconciliationItemMapper;
		this.procurementItemMapper = procurementItemMapper;
		this.auditService = auditService;
	}

	@Override
	public Reconciliation reconcileBookRequestItems() {
		try {
			return reconciliationChecker.checkBookRequestItems();
		} catch (Exception e) {
			log.error("對帳失敗：type={}", ReconciliationType.BOOK_REQUEST_PROCUREMENT, e);
			return reconciliationFailureRecorder.recordFailure(ReconciliationType.BOOK_REQUEST_PROCUREMENT);
		}
	}

	@Override
	public Reconciliation reconcileProcurementItems() {
		try {
			return reconciliationChecker.checkProcurementItems();
		} catch (Exception e) {
			log.error("對帳失敗：type={}", ReconciliationType.PROCUREMENT_BOOK, e);
			return reconciliationFailureRecorder.recordFailure(ReconciliationType.PROCUREMENT_BOOK);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReconciliationSummary> findSummary() {
		return reconciliationMapper.findSummary();
	}

	@Override
	@Transactional
	public void resolve(Long reconciliationItemId, User currentUser) {
		ReconciliationItem item = findReconciliationItemOrThrow(reconciliationItemId);
		reconciliationItemMapper.updateStatus(reconciliationItemId, ReconciliationItemStatus.RESOLVED);

		auditService.create(buildStatusChangeAuditLog(currentUser, AuditActionType.RESOLVE, item));
	}

	@Override
	@Transactional
	public void writeOff(Long reconciliationItemId, User currentUser) {
		ReconciliationItem item = findReconciliationItemOrThrow(reconciliationItemId);
		reconciliationItemMapper.updateStatus(reconciliationItemId, ReconciliationItemStatus.WRITTEN_OFF);

		auditService.create(buildStatusChangeAuditLog(currentUser, AuditActionType.WRITE_OFF, item));
	}

	@Override
	@Transactional
	public void backfillProcurementItem(Long reconciliationItemId, User currentUser) {
		ReconciliationItem item = findReconciliationItemOrThrow(reconciliationItemId);
		if (item.getEntityType() != AuditEntityType.BOOK_REQUEST_ITEM) {
			throw new IllegalStateException("只有「已核准但缺採購項目」這種對帳差異可以補建：id=" + reconciliationItemId);
		}
		if (item.getStatus() != ReconciliationItemStatus.UNRESOLVED) {
			throw new IllegalStateException("這筆對帳差異已經處理過了：id=" + reconciliationItemId);
		}

		ProcurementItem procurementItem = ProcurementItem.builder().bookRequestItemId(item.getEntityId())
				.status(ProcurementStatus.PENDING).build();
		procurementItemMapper.insert(procurementItem);

		reconciliationItemMapper.updateStatus(reconciliationItemId, ReconciliationItemStatus.RESOLVED);

		Map<String, Object> detail = new HashMap<>();
		detail.put("reason", "對帳手動補建");
		detail.put("reconciliationItemId", reconciliationItemId);
		AuditLog auditLog = AuditLog.builder().userId(currentUser.getId()).auditedAt(Instant.now())
				.action(AuditActionType.CREATE).entityType(AuditEntityType.PROCUREMENT_ITEM)
				.entityId(procurementItem.getId()).detail(detail).build();
		auditService.create(auditLog);
	}

	private ReconciliationItem findReconciliationItemOrThrow(Long reconciliationItemId) {
		return reconciliationItemMapper.findById(reconciliationItemId)
				.orElseThrow(() -> new NoSuchElementException("找不到對帳明細：id=" + reconciliationItemId));
	}

	private AuditLog buildStatusChangeAuditLog(User currentUser, AuditActionType action, ReconciliationItem item) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("entityType", item.getEntityType());
		detail.put("entityId", item.getEntityId());

		return AuditLog.builder().userId(currentUser.getId()).auditedAt(Instant.now()).action(action)
				.entityType(AuditEntityType.RECONCILIATION_ITEM).entityId(item.getId()).detail(detail).build();
	}
}

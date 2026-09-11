package com.example.jsppractice.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.mapper.ReconciliationItemMapper;
import com.example.jsppractice.mapper.ReconciliationMapper;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationDiscrepancyType;
import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.ReconciliationType;

// 實際跑對帳查詢、寫入結果的邏輯。刻意獨立於 ReconciliationServiceImpl 之外
// （即使 REQUIRES_NEW 用不到這裡），單純因為它的呼叫端需要在「不同的 bean」，
// 才能讓 ReconciliationFailureRecorder 的 REQUIRES_NEW 生效，見那個類別上的註解。
@Service
public class ReconciliationChecker {
	private final BookRequestItemMapper bookRequestItemMapper;
	private final ProcurementItemMapper procurementItemMapper;
	private final ReconciliationMapper reconciliationMapper;
	private final ReconciliationItemMapper reconciliationItemMapper;

	public ReconciliationChecker(ProcurementItemMapper procurementItemMapper,
			BookRequestItemMapper bookRequestItemMapper, ReconciliationMapper reconciliationMapper,
			ReconciliationItemMapper reconciliationItemMapper) {
		this.bookRequestItemMapper = bookRequestItemMapper;
		this.procurementItemMapper = procurementItemMapper;
		this.reconciliationMapper = reconciliationMapper;
		this.reconciliationItemMapper = reconciliationItemMapper;
	}

	@Transactional
	public Reconciliation checkBookRequestItems() {
		List<BookRequestItem> itemsWithoutProcurement = bookRequestItemMapper.findApprovedWithoutProcurementItem();
		Instant now = Instant.now();
		Reconciliation reconciliation;
		if (itemsWithoutProcurement.isEmpty()) {
			reconciliation = Reconciliation.builder()
					.reconciledAt(now)
					.reconciliationType(ReconciliationType.BOOK_REQUEST_PROCUREMENT)
					.status(ReconciliationStatus.COMPLETED_NO_DISCREPANCY)
					.build();
			reconciliationMapper.insert(reconciliation);
		} else {
			reconciliation = Reconciliation.builder()
					.reconciledAt(now)
					.reconciliationType(ReconciliationType.BOOK_REQUEST_PROCUREMENT)
					.status(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY)
					.build();
			reconciliationMapper.insert(reconciliation);

			for (BookRequestItem item : itemsWithoutProcurement) {
				ReconciliationItem reconciliationItem = ReconciliationItem.builder()
						.reconciliationId(reconciliation.getId())
						.entityType(AuditEntityType.BOOK_REQUEST_ITEM)
						.entityId(item.getId())
						.discrepancyType(ReconciliationDiscrepancyType.ONLY_IN_SOURCE)
						.detail(Map.of("reason", "找不到對應的 procurement_item"))
						.build();
				reconciliationItemMapper.insert(reconciliationItem);
			}
		}
		return reconciliation;
	}

	@Transactional
	public Reconciliation checkProcurementItems() {
		List<ProcurementItem> itemsWithoutBook = procurementItemMapper.findCompletedWithoutBook();
		Instant now = Instant.now();
		Reconciliation reconciliation;
		if (itemsWithoutBook.isEmpty()) {
			reconciliation = Reconciliation.builder()
					.reconciledAt(now)
					.reconciliationType(ReconciliationType.PROCUREMENT_BOOK)
					.status(ReconciliationStatus.COMPLETED_NO_DISCREPANCY)
					.build();
			reconciliationMapper.insert(reconciliation);
		} else {
			reconciliation = Reconciliation.builder()
					.reconciledAt(now)
					.reconciliationType(ReconciliationType.PROCUREMENT_BOOK)
					.status(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY)
					.build();
			reconciliationMapper.insert(reconciliation);
			for (ProcurementItem item : itemsWithoutBook) {
				ReconciliationItem reconciliationItem = ReconciliationItem.builder()
						.reconciliationId(reconciliation.getId())
						.entityType(AuditEntityType.PROCUREMENT_ITEM)
						.entityId(item.getId())
						.discrepancyType(ReconciliationDiscrepancyType.ONLY_IN_SOURCE)
						.detail(Map.of("reason", "找不到對應的 book"))
						.build();
				reconciliationItemMapper.insert(reconciliationItem);
			}
		}
		return reconciliation;
	}
}

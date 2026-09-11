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

import com.example.jsppractice.dto.ProcurementSummary;
import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.User;

@Service
public class ProcurementServiceImpl implements ProcurementService {
	private static final Logger log = LoggerFactory.getLogger(ProcurementServiceImpl.class);

	private final ProcurementItemMapper procurementItemMapper;
	private final BookRequestItemMapper bookRequestItemMapper;
	private final BookService bookService;
	private final AuditService auditService;

	private static final String PROCUREMENT_ITEM_NOT_FOUND_MESSAGE = "找不到採購清單";
	private static final String BOOK_REQUEST_ITEM_NOT_FOUND_MESSAGE = "找不到申請明細";

	public ProcurementServiceImpl(ProcurementItemMapper procurementItemMapper,
			BookRequestItemMapper bookRequestItemMapper, BookService bookService, AuditService auditService) {
		this.procurementItemMapper = procurementItemMapper;
		this.bookRequestItemMapper = bookRequestItemMapper;
		this.bookService = bookService;
		this.auditService = auditService;
	}

	@Override
	@Transactional
	public ProcurementItem completeProcurement(Long procurementItemId, User currentUser) {
		// 上悲觀鎖
		ProcurementItem procurementItem = procurementItemMapper.findByIdForUpdate(procurementItemId)
				.orElseThrow(() -> new NoSuchElementException(PROCUREMENT_ITEM_NOT_FOUND_MESSAGE));
		if (procurementItem.getStatus() == ProcurementStatus.COMPLETED) {
			recordCompletionFailure(procurementItem, currentUser, "無法修改已完成的採購清單");
			throw new ProcurementAlreadyCompletedException("無法修改已完成的採購清單");
		}

		BookRequestItem bookRequestItem = bookRequestItemMapper.findById(procurementItem.getBookRequestItemId());
		if (bookRequestItem == null) {
			throw new NoSuchElementException(BOOK_REQUEST_ITEM_NOT_FOUND_MESSAGE);
		}

		Book book = bookService.save(Book.builder().title(bookRequestItem.getTitle()).isbn(bookRequestItem.getIsbn())
				.authorId(bookRequestItem.getAuthorId()).publishedYear(bookRequestItem.getPublishedYear()).build());

		ProcurementStatus oldStatus = procurementItem.getStatus();
		Instant now = Instant.now();
		procurementItem.setStatus(ProcurementStatus.COMPLETED);
		procurementItem.setBookId(book.getId());
		procurementItem.setProcuredBy(currentUser.getId());
		procurementItem.setProcuredAt(now);
		procurementItemMapper.update(procurementItem);

		// audit log
		Map<String, Object> statusDetail = new HashMap<>();
		statusDetail.put("old", oldStatus);
		statusDetail.put("new", ProcurementStatus.COMPLETED);

		Map<String, Object> detail = new HashMap<>();
		detail.put("status", statusDetail);
		detail.put("bookId", book.getId());

		AuditLog auditLog = AuditLog.builder()
				.userId(currentUser.getId())
				.auditedAt(now)
				.action(AuditActionType.COMPLETE_PROCUREMENT)
				.entityType(AuditEntityType.PROCUREMENT_ITEM)
				.entityId(procurementItem.getId())
				.detail(detail)
				.build();
		auditService.create(auditLog);

		log.info("採購完成，書籍已入庫：procurementItemId={}, bookId={}, procuredBy={}", procurementItemId, book.getId(),
				currentUser.getId());
		return procurementItem;
	}

	@Override
	public List<ProcurementItem> findByStatus(ProcurementStatus status) {
		return procurementItemMapper.findByStatus(status);
	}

	// 失敗嘗試也要留稽核紀錄：用 REQUIRES_NEW 寫入，即使外層交易最後 rollback 也不會跟著消失。
	private void recordCompletionFailure(ProcurementItem procurementItem, User currentUser, String reason) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("result", "FAILED");
		detail.put("reason", reason);

		AuditLog auditLog = AuditLog.builder()
				.userId(currentUser.getId())
				.auditedAt(Instant.now())
				.action(AuditActionType.COMPLETE_PROCUREMENT)
				.entityType(AuditEntityType.PROCUREMENT_ITEM)
				.entityId(procurementItem.getId())
				.detail(detail)
				.build();
		auditService.createWhenFailure(auditLog);
	}

	@Override
	public List<ProcurementSummary> findSummary(ProcurementStatus status) {
		return procurementItemMapper.findSummary(status);
	}

}

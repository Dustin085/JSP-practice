package com.example.jsppractice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.dto.BookRequestSummary;
import com.example.jsppractice.exception.BookRequestAlreadyProcessedException;
import com.example.jsppractice.exception.SelfReviewNotAllowedException;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.User;

@Service
public class BookRequestServiceImpl implements BookRequestService {
	private static final Logger log = LoggerFactory.getLogger(BookRequestServiceImpl.class);

	private final BookRequestMapper bookRequestMapper;
	private final BookRequestItemMapper bookRequestItemMapper;
	private final ProcurementItemMapper procurementItemMapper;
	private final AuditService auditService;

	private static final String BOOK_REQUEST_NOT_FOUND_MESSAGE = "書籍請求不存在";

	public BookRequestServiceImpl(BookRequestMapper bookRequestMapper, BookRequestItemMapper bookRequestItemMapper,
			ProcurementItemMapper procurementItemMapper, AuditService auditService) {
		this.bookRequestMapper = bookRequestMapper;
		this.bookRequestItemMapper = bookRequestItemMapper;
		this.procurementItemMapper = procurementItemMapper;
		this.auditService = auditService;
	}

	@Override
	@Transactional
	public BookRequest submit(User requestUser, List<BookRequestBookInfo> bookInfos, String idempotencyKey) {
		// 建立 BookRequest，並且寫入
		Instant now = Instant.now();
		BookRequest bookRequest = BookRequest.builder()
				.requesterId(requestUser.getId())
				.status(BookRequestStatus.PENDING)
				.requestedAt(now)
				.idempotencyKey(idempotencyKey)
				.build();
		try {
			bookRequestMapper.insert(bookRequest);
		} catch (DuplicateKeyException e) {
			// 重複送出（idempotency_key 撞到 UNIQUE 約束）：不建立新的一筆，
			// 直接回傳原本那筆，讓呼叫端拿到跟第一次送出一樣的結果。
			log.warn("偵測到重複送出，直接回傳原始請求：idempotencyKey={}", idempotencyKey);
			return bookRequestMapper.findByIdempotencyKey(idempotencyKey)
					.orElseThrow(() -> new IllegalStateException("重複送出但找不到原始請求：idempotencyKey=" + idempotencyKey));
		}
		// 建立 BookRequestItem，並且寫入
		for (BookRequestBookInfo bookInfo : bookInfos) {
			BookRequestItem bookRequestItem = BookRequestItem.builder()
					.bookRequestId(bookRequest.getId())
					.title(bookInfo.title())
					.isbn(bookInfo.isbn())
					.authorId(bookInfo.authorId())
					.publishedYear(bookInfo.publishedYear())
					.estimatedPrice(bookInfo.estimatedPrice())
					.build();
			bookRequestItemMapper.insert(bookRequestItem);
		}

		// audit log
		BigDecimal totalEstimatedPrice = bookInfos.stream()
				.map(BookRequestBookInfo::estimatedPrice)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<String, Object> statusDetail = new HashMap<>();
		statusDetail.put("old", null);
		statusDetail.put("new", BookRequestStatus.PENDING);

		Map<String, Object> detail = new HashMap<>();
		detail.put("status", statusDetail);
		detail.put("totalEstimatedPrice", totalEstimatedPrice);

		AuditLog auditLog = AuditLog.builder()
				.userId(requestUser.getId())
				.auditedAt(now)
				.action(AuditActionType.SUBMIT)
				.entityType(AuditEntityType.BOOK_REQUEST)
				.entityId(bookRequest.getId())
				.detail(detail)
				.build();
		auditService.create(auditLog);

		log.info("送出申請：requestId={}, requesterId={}, 書籍數={}", bookRequest.getId(), requestUser.getId(),
				bookInfos.size());
		return bookRequest;
	}

	@Override
	@Transactional
	public BookRequest approve(Long requestId, User currentUser) {
		Instant now = Instant.now();
		int approveResult = bookRequestMapper.approve(requestId, currentUser.getId(), now);
		BookRequest bookRequest = bookRequestMapper.findById(requestId)
				.orElseThrow(() -> new NoSuchElementException(BOOK_REQUEST_NOT_FOUND_MESSAGE));
		if (approveResult == 0) {
			validateBookRequest(bookRequest, currentUser, AuditActionType.APPROVE);
		}

		// 產生採購清單
		List<BookRequestItem> bookRequestItems = bookRequestItemMapper.findByBookRequestId(bookRequest.getId());
		for (BookRequestItem bookRequestItem : bookRequestItems) {
			ProcurementItem procurementItem = ProcurementItem.from(bookRequestItem);
			procurementItemMapper.insert(procurementItem);

			// audit log
			Map<String, Object> statusDetail = new HashMap<>();
			statusDetail.put("old", null);
			statusDetail.put("new", ProcurementStatus.PENDING);

			Map<String, Object> detail = new HashMap<>();
			detail.put("status", statusDetail);

			AuditLog auditLog = AuditLog.builder()
					.userId(currentUser.getId())
					.auditedAt(now)
					.action(AuditActionType.CREATE)
					.entityType(AuditEntityType.PROCUREMENT_ITEM)
					.entityId(procurementItem.getId())
					.detail(detail)
					.build();
			auditService.create(auditLog);
		}

		// audit log
		Map<String, Object> statusDetail = new HashMap<>();
		statusDetail.put("old", BookRequestStatus.PENDING);
		statusDetail.put("new", BookRequestStatus.APPROVED);

		Map<String, Object> detail = new HashMap<>();
		detail.put("status", statusDetail);

		AuditLog auditLog = AuditLog.builder()
				.userId(currentUser.getId())
				.auditedAt(now)
				.action(AuditActionType.APPROVE)
				.entityType(AuditEntityType.BOOK_REQUEST)
				.entityId(bookRequest.getId())
				.detail(detail)
				.build();
		auditService.create(auditLog);
		log.info("申請已核准：requestId={}, approverId={}", requestId, currentUser.getId());
		return bookRequest;
	}

	@Override
	@Transactional
	public BookRequest reject(Long requestId, User currentUser) {
		Instant now = Instant.now();
		int rejectResult = bookRequestMapper.reject(requestId, currentUser.getId(), now);
		BookRequest bookRequest = bookRequestMapper.findById(requestId)
				.orElseThrow(() -> new NoSuchElementException(BOOK_REQUEST_NOT_FOUND_MESSAGE));
		if (rejectResult == 0) {
			validateBookRequest(bookRequest, currentUser, AuditActionType.REJECT);
		}

		// audit log
		Map<String, Object> statusDetail = new HashMap<>();
		statusDetail.put("old", BookRequestStatus.PENDING);
		statusDetail.put("new", BookRequestStatus.REJECTED);

		Map<String, Object> detail = new HashMap<>();
		detail.put("status", statusDetail);

		AuditLog auditLog = AuditLog.builder()
				.userId(currentUser.getId())
				.auditedAt(now)
				.action(AuditActionType.REJECT)
				.entityType(AuditEntityType.BOOK_REQUEST)
				.entityId(bookRequest.getId())
				.detail(detail)
				.build();
		auditService.create(auditLog);

		log.info("申請已拒絕：requestId={}, approverId={}", requestId, currentUser.getId());
		return bookRequest;
	}

	@Override
	public List<BookRequest> findAll() {
		return bookRequestMapper.findAll();
	}

	@Override
	public List<BookRequest> findByStatus(BookRequestStatus status) {
		return bookRequestMapper.findByStatus(status);
	}

	@Override
	public List<BookRequestItem> findItemsByRequestId(Long requestId) {
		return bookRequestItemMapper.findByBookRequestId(requestId);
	}

	@Override
	public List<BookRequestSummary> findSummary(BookRequestStatus status) {
		return bookRequestMapper.findSummary(status);
	}

	private void validateBookRequest(BookRequest bookRequest, User currentUser, AuditActionType attemptedAction) {
		if (bookRequest.getRequesterId().equals(currentUser.getId())) {
			log.warn("審核被拒絕，審核人與申請人相同：requestId={}, userId={}", bookRequest.getId(), currentUser.getId());
			recordValidationFailure(bookRequest, currentUser, attemptedAction, "審查人與請求人必須為不同人");
			throw new SelfReviewNotAllowedException("審查人與請求人必須為不同人");
		}
		if (bookRequest.getStatus() == BookRequestStatus.APPROVED) {
			recordValidationFailure(bookRequest, currentUser, attemptedAction, "此請求已被同意");
			throw new BookRequestAlreadyProcessedException("此請求已被同意");
		}
		if (bookRequest.getStatus() == BookRequestStatus.REJECTED) {
			recordValidationFailure(bookRequest, currentUser, attemptedAction, "此請求已被拒絕");
			throw new BookRequestAlreadyProcessedException("此請求已被拒絕");
		}
	}

	// 失敗嘗試也要留稽核紀錄：用 REQUIRES_NEW 寫入，即使外層 approve/reject 的交易最後
	// 因為丟出例外而 rollback，這筆失敗紀錄依然會單獨提交、不會跟著消失。
	private void recordValidationFailure(BookRequest bookRequest, User currentUser, AuditActionType attemptedAction,
			String reason) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("result", "FAILED");
		detail.put("reason", reason);

		AuditLog auditLog = AuditLog.builder()
				.userId(currentUser.getId())
				.auditedAt(Instant.now())
				.action(attemptedAction)
				.entityType(AuditEntityType.BOOK_REQUEST)
				.entityId(bookRequest.getId())
				.detail(detail)
				.build();
		auditService.createWhenFailure(auditLog);
	}

}

package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.jsppractice.config.RootConfig;
import com.example.jsppractice.dto.BookRequestSummary;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class BookRequestWorkflowMapperTest {

	@Autowired
	private BookRequestMapper bookRequestMapper;

	@Autowired
	private BookRequestItemMapper bookRequestItemMapper;

	@Autowired
	private ProcurementItemMapper procurementItemMapper;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private DataBaseCleaner dataBaseCleaner;

	@Before
	public void setUp() {
		dataBaseCleaner = new DataBaseCleaner(jdbcTemplate);
	}

	@After
	public void tearDown() {
		dataBaseCleaner.clean();
	}

	private Long insertUser(String email) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("users")
				.usingGeneratedKeyColumns("id");
		Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		params.put("password_hash", "hashed-password");
		params.put("role", "USER");
		return insert.executeAndReturnKey(params).longValue();
	}

	@Test
	public void bookRequestInsertAndFindByStatusWork() {
		Long requesterId = insertUser("requester@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.PENDING)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();

		bookRequestMapper.insert(bookRequest);

		BookRequest found = bookRequestMapper.findById(bookRequest.getId()).get();
		assertEquals(requesterId, found.getRequesterId());
		assertEquals(BookRequestStatus.PENDING, found.getStatus());
		assertNull(found.getApproverId());

		List<BookRequest> pending = bookRequestMapper.findByStatus(BookRequestStatus.PENDING);
		assertEquals(1, pending.size());
	}

	@Test
	public void approveMapperMethodBindsParametersCorrectly() {
		Long requesterId = insertUser("requester5@example.com");
		Long approverId = insertUser("approver5@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.PENDING)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		int affectedRows = bookRequestMapper.approve(bookRequest.getId(), approverId, LocalDateTime.now());

		assertEquals(1, affectedRows);
	}

	@Test
	public void findSummaryGroupsMultipleItemsUnderOneRequestWithoutDuplicatingRows() {
		Long requesterId = insertUser("summaryRequester@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.PENDING)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		BookRequestItem item1 = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Effective Java")
				.build();
		BookRequestItem item2 = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Clean Code")
				.build();
		bookRequestItemMapper.insert(item1);
		bookRequestItemMapper.insert(item2);

		List<BookRequestSummary> all = bookRequestMapper.findSummary(null);

		assertEquals("同一張申請單只能出現一次，不能因為 JOIN 多筆明細被重複列出", 1, all.size());
		BookRequestSummary summary = all.get(0);
		assertEquals(requesterId, summary.getRequesterId());
		assertEquals(2, summary.getBookRequestItems().size());
	}

	@Test
	public void findSummaryFiltersByStatusAndHandlesRequestWithNoItems() {
		Long requesterId = insertUser("summaryRequester2@example.com");
		BookRequest pendingRequest = BookRequest.builder().requesterId(requesterId)
				.status(BookRequestStatus.PENDING).requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(pendingRequest);
		BookRequest approvedRequest = BookRequest.builder().requesterId(requesterId)
				.status(BookRequestStatus.APPROVED).requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(approvedRequest);

		List<BookRequestSummary> pendingOnly = bookRequestMapper.findSummary(BookRequestStatus.PENDING);

		assertEquals(1, pendingOnly.size());
		assertEquals(pendingRequest.getId(), pendingOnly.get(0).getId());
		assertEquals("沒有任何明細時，不該是 null，應該是空 list", 0, pendingOnly.get(0).getBookRequestItems().size());
	}

	@Test
	public void bookRequestItemInsertAndFindByBookRequestIdWork() {
		Long requesterId = insertUser("requester3@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.PENDING)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		BookRequestItem item = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Effective Java")
				.isbn("9780134685991").publishedYear(2018).estimatedPrice(new BigDecimal("1200.00")).build();
		bookRequestItemMapper.insert(item);

		List<BookRequestItem> items = bookRequestItemMapper.findByBookRequestId(bookRequest.getId());
		assertEquals(1, items.size());
		assertEquals("Effective Java", items.get(0).getTitle());
		assertEquals(new BigDecimal("1200.00"), items.get(0).getEstimatedPrice());
	}

	@Test
	public void procurementItemInsertAndUpdateWork() {
		Long requesterId = insertUser("requester4@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.APPROVED)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);
		BookRequestItem item = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Clean Code").build();
		bookRequestItemMapper.insert(item);

		ProcurementItem procurementItem = ProcurementItem.builder().bookRequestItemId(item.getId())
				.status(ProcurementStatus.PENDING).build();
		procurementItemMapper.insert(procurementItem);

		List<ProcurementItem> pending = procurementItemMapper.findByStatus(ProcurementStatus.PENDING);
		assertEquals(1, pending.size());

		Long procuredBy = insertUser("procurement@example.com");
		procurementItem.setStatus(ProcurementStatus.COMPLETED);
		procurementItem.setProcuredBy(procuredBy);
		procurementItem.setProcuredAt(LocalDateTime.now());
		procurementItemMapper.update(procurementItem);

		ProcurementItem found = procurementItemMapper.findById(procurementItem.getId()).get();
		assertEquals(ProcurementStatus.COMPLETED, found.getStatus());
		assertEquals(procuredBy, found.getProcuredBy());
	}

	@Test
	public void findSummaryJoinsBookRequestItemDetailsAndFiltersByStatus() {
		Long requesterId = insertUser("requester5@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.APPROVED)
				.requestedAt(LocalDateTime.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		BookRequestItem item = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Effective Java")
				.isbn("9780134685991").publishedYear(2018).estimatedPrice(new BigDecimal("1200.00")).build();
		bookRequestItemMapper.insert(item);

		ProcurementItem pending = ProcurementItem.builder().bookRequestItemId(item.getId())
				.status(ProcurementStatus.PENDING).build();
		procurementItemMapper.insert(pending);

		BookRequestItem completedItem = BookRequestItem.builder().bookRequestId(bookRequest.getId())
				.title("Clean Code").build();
		bookRequestItemMapper.insert(completedItem);
		ProcurementItem completed = ProcurementItem.builder().bookRequestItemId(completedItem.getId())
				.status(ProcurementStatus.COMPLETED).build();
		procurementItemMapper.insert(completed);

		List<com.example.jsppractice.dto.ProcurementSummary> summaries = procurementItemMapper
				.findSummary(ProcurementStatus.PENDING);

		assertEquals("只該撈到 PENDING 那一筆，COMPLETED 的不該出現", 1, summaries.size());
		com.example.jsppractice.dto.ProcurementSummary summary = summaries.get(0);
		assertEquals(pending.getId(), summary.getId());
		assertEquals(ProcurementStatus.PENDING, summary.getStatus());
		assertEquals("Effective Java", summary.getBookRequestItem().getTitle());
		assertEquals("9780134685991", summary.getBookRequestItem().getIsbn());
		assertEquals(Integer.valueOf(2018), summary.getBookRequestItem().getPublishedYear());
		assertEquals(new BigDecimal("1200.00"), summary.getBookRequestItem().getEstimatedPrice());
	}
}

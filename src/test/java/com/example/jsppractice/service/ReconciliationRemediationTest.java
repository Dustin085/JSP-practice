package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
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
import com.example.jsppractice.dto.ReconciliationSummary;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationItemStatus;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class ReconciliationRemediationTest {

	@Autowired
	private ReconciliationService reconciliationService;
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
	private User currentUser;

	@Before
	public void setUp() {
		dataBaseCleaner = new DataBaseCleaner(jdbcTemplate);
		currentUser = User.builder().id(insertUser("reviewer@example.com")).build();
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
		params.put("password_hash", "hash");
		params.put("role", "ADMIN");
		return insert.executeAndReturnKey(params).longValue();
	}

	private BookRequestItem createOrphanApprovedItem() {
		BookRequest approved = BookRequest.builder().requesterId(currentUser.getId())
				.status(BookRequestStatus.APPROVED).requestedAt(Instant.now())
				.idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(approved);
		BookRequestItem item = BookRequestItem.builder().bookRequestId(approved.getId()).title("Missing PI").build();
		bookRequestItemMapper.insert(item);
		return item;
	}

	private ReconciliationItem firstItemOf(Reconciliation reconciliation) {
		List<ReconciliationSummary> summaries = reconciliationService.findSummary();
		return summaries.stream().filter(s -> s.getId().equals(reconciliation.getId())).findFirst().get()
				.getReconciliationItems().get(0);
	}

	@Test
	public void writeOffSuppressesTheSameDiscrepancyOnNextRun() {
		createOrphanApprovedItem();
		Reconciliation firstRun = reconciliationService.reconcileBookRequestItems();
		assertEquals(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY, firstRun.getStatus());
		ReconciliationItem item = firstItemOf(firstRun);

		reconciliationService.writeOff(item.getId(), currentUser);

		Reconciliation secondRun = reconciliationService.reconcileBookRequestItems();
		assertEquals("沖銷過的差異，下次對帳不該再回報", ReconciliationStatus.COMPLETED_NO_DISCREPANCY, secondRun.getStatus());
	}

	@Test
	public void backfillProcurementItemCreatesItemAndResolvesDiscrepancy() {
		BookRequestItem orphan = createOrphanApprovedItem();
		Reconciliation firstRun = reconciliationService.reconcileBookRequestItems();
		ReconciliationItem item = firstItemOf(firstRun);

		reconciliationService.backfillProcurementItem(item.getId(), currentUser);

		List<ProcurementItem> procurementItems = procurementItemMapper.findByStatus(ProcurementStatus.PENDING);
		assertTrue("補建後應該要多一筆 PENDING 的採購項目，指向原本缺漏的 book_request_item",
				procurementItems.stream().anyMatch(p -> p.getBookRequestItemId().equals(orphan.getId())));

		Reconciliation secondRun = reconciliationService.reconcileBookRequestItems();
		assertEquals("補建後，下次對帳不該再回報這筆", ReconciliationStatus.COMPLETED_NO_DISCREPANCY, secondRun.getStatus());

		Long auditLogCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_logs WHERE action = 'CREATE' AND entity_type = 'PROCUREMENT_ITEM'",
				Long.class);
		assertTrue("補建動作應該要留一筆稽核紀錄", auditLogCount > 0);
	}

	@Test(expected = IllegalStateException.class)
	public void backfillProcurementItemRejectsWrongEntityType() {
		Long userId = insertUser("orphan@example.com");
		BookRequest approved = BookRequest.builder().requesterId(userId).status(BookRequestStatus.APPROVED)
				.requestedAt(Instant.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(approved);
		BookRequestItem brokenItem = BookRequestItem.builder().bookRequestId(approved.getId())
				.title("Completed no book").build();
		bookRequestItemMapper.insert(brokenItem);
		procurementItemMapper.insert(
				ProcurementItem.builder().bookRequestItemId(brokenItem.getId()).status(ProcurementStatus.COMPLETED)
						.build());

		Reconciliation run = reconciliationService.reconcileProcurementItems();
		ReconciliationItem item = firstItemOf(run);

		reconciliationService.backfillProcurementItem(item.getId(), currentUser);
	}

	@Test
	public void resolveUpdatesStatusAndWritesAuditLog() {
		createOrphanApprovedItem();
		Reconciliation run = reconciliationService.reconcileBookRequestItems();
		ReconciliationItem item = firstItemOf(run);

		reconciliationService.resolve(item.getId(), currentUser);

		ReconciliationItemStatus status = jdbcTemplate.queryForObject(
				"SELECT status FROM reconciliation_items WHERE id = ?",
				(rs, rowNum) -> ReconciliationItemStatus.valueOf(rs.getString("status")), item.getId());
		assertEquals(ReconciliationItemStatus.RESOLVED, status);

		Long auditLogCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_logs WHERE action = 'RESOLVE' AND entity_type = 'RECONCILIATION_ITEM' AND entity_id = ?",
				Long.class, item.getId());
		assertTrue("標記已處理應該要留一筆稽核紀錄", auditLogCount > 0);
	}
}

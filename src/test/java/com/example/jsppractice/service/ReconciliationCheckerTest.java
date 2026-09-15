package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;

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
import com.example.jsppractice.model.ReconciliationStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class ReconciliationCheckerTest {

	@Autowired
	private ReconciliationChecker reconciliationChecker;
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
		params.put("password_hash", "hash");
		Long id = insert.executeAndReturnKey(params).longValue();
		jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, "ADMIN");
		return id;
	}

	@Test
	public void checkBookRequestItemsWithNoDiscrepancy() {
		Reconciliation result = reconciliationChecker.checkBookRequestItems();

		assertEquals(ReconciliationStatus.COMPLETED_NO_DISCREPANCY, result.getStatus());
		Long itemCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM reconciliation_items WHERE reconciliation_id = ?", Long.class, result.getId());
		assertEquals(Long.valueOf(0), itemCount);
	}

	@Test
	public void checkBookRequestItemsWithDiscrepancyPersistsHeaderAndItems() {
		Long userId = insertUser("checker1@example.com");
		BookRequest approved = BookRequest.builder().requesterId(userId).status(BookRequestStatus.APPROVED)
				.requestedAt(Instant.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(approved);
		BookRequestItem missing = BookRequestItem.builder().bookRequestId(approved.getId()).title("Missing PI")
				.build();
		bookRequestItemMapper.insert(missing);

		Reconciliation result = reconciliationChecker.checkBookRequestItems();

		assertEquals(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY, result.getStatus());
		List<Map<String, Object>> items = jdbcTemplate
				.queryForList("SELECT * FROM reconciliation_items WHERE reconciliation_id = ?", result.getId());
		assertEquals(1, items.size());
		assertEquals(missing.getId().longValue(), ((Number) items.get(0).get("ENTITY_ID")).longValue());
	}

	@Test
	public void checkProcurementItemsWithDiscrepancyPersistsHeaderAndItems() {
		Long userId = insertUser("checker2@example.com");
		BookRequest approved = BookRequest.builder().requesterId(userId).status(BookRequestStatus.APPROVED)
				.requestedAt(Instant.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(approved);
		BookRequestItem brokenItem = BookRequestItem.builder().bookRequestId(approved.getId())
				.title("Completed no book").build();
		bookRequestItemMapper.insert(brokenItem);
		ProcurementItem broken = ProcurementItem.builder().bookRequestItemId(brokenItem.getId())
				.status(ProcurementStatus.COMPLETED).build();
		procurementItemMapper.insert(broken);

		Reconciliation result = reconciliationChecker.checkProcurementItems();

		assertEquals(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY, result.getStatus());
		List<Map<String, Object>> items = jdbcTemplate
				.queryForList("SELECT * FROM reconciliation_items WHERE reconciliation_id = ?", result.getId());
		assertEquals(1, items.size());
		assertEquals(broken.getId().longValue(), ((Number) items.get(0).get("ENTITY_ID")).longValue());
	}
}

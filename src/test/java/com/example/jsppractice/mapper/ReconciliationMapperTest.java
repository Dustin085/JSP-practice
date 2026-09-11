package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.jsppractice.config.RootConfig;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.Reconciliation;
import com.example.jsppractice.model.ReconciliationDiscrepancyType;
import com.example.jsppractice.model.ReconciliationItem;
import com.example.jsppractice.model.ReconciliationItemStatus;
import com.example.jsppractice.model.ReconciliationStatus;
import com.example.jsppractice.model.ReconciliationType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class ReconciliationMapperTest {

	@Autowired
	private ReconciliationMapper reconciliationMapper;

	@Autowired
	private ReconciliationItemMapper reconciliationItemMapper;

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

	@Test
	public void insertGeneratesIdAndPersistsFields() {
		Reconciliation reconciliation = Reconciliation.builder().reconciledAt(Instant.now())
				.reconciliationType(ReconciliationType.BOOK_REQUEST_PROCUREMENT)
				.status(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY).build();

		reconciliationMapper.insert(reconciliation);

		assertNotNull("insert 後應該要有 useGeneratedKeys 帶回來的 id", reconciliation.getId());
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM reconciliations WHERE id = ?",
				reconciliation.getId());
		assertEquals("BOOK_REQUEST_PROCUREMENT", row.get("RECONCILIATION_TYPE"));
		assertEquals("COMPLETED_WITH_DISCREPANCY", row.get("STATUS"));
	}

	@Test
	public void reconciliationItemInsertRoundTripsDetailJsonAndLinksToParent() {
		Reconciliation reconciliation = Reconciliation.builder().reconciledAt(Instant.now())
				.reconciliationType(ReconciliationType.PROCUREMENT_BOOK)
				.status(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY).build();
		reconciliationMapper.insert(reconciliation);

		Map<String, Object> detail = new HashMap<>();
		detail.put("reason", "找不到對應的 procurement_item");

		ReconciliationItem item = ReconciliationItem.builder().reconciliationId(reconciliation.getId())
				.entityType(AuditEntityType.BOOK_REQUEST_ITEM).entityId(99L)
				.discrepancyType(ReconciliationDiscrepancyType.ONLY_IN_SOURCE)
				.status(ReconciliationItemStatus.UNRESOLVED).detail(detail).build();

		reconciliationItemMapper.insert(item);

		assertNotNull("insert 後應該要有 useGeneratedKeys 帶回來的 id", item.getId());
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM reconciliation_items WHERE id = ?",
				item.getId());
		assertEquals(reconciliation.getId().longValue(), ((Number) row.get("RECONCILIATION_ID")).longValue());
		assertEquals("BOOK_REQUEST_ITEM", row.get("ENTITY_TYPE"));
		assertEquals(99L, ((Number) row.get("ENTITY_ID")).longValue());
		assertEquals("ONLY_IN_SOURCE", row.get("DISCREPANCY_TYPE"));
		assertEquals("UNRESOLVED", row.get("STATUS"));
	}

	@Test
	public void findByIdReturnsItemWithStatus() {
		Reconciliation reconciliation = Reconciliation.builder().reconciledAt(Instant.now())
				.reconciliationType(ReconciliationType.PROCUREMENT_BOOK)
				.status(ReconciliationStatus.COMPLETED_WITH_DISCREPANCY).build();
		reconciliationMapper.insert(reconciliation);

		ReconciliationItem item = ReconciliationItem.builder().reconciliationId(reconciliation.getId())
				.entityType(AuditEntityType.PROCUREMENT_ITEM).entityId(5L)
				.discrepancyType(ReconciliationDiscrepancyType.ONLY_IN_SOURCE)
				.status(ReconciliationItemStatus.UNRESOLVED).detail(Map.of("reason", "找不到對應的 book")).build();
		reconciliationItemMapper.insert(item);

		ReconciliationItem found = reconciliationItemMapper.findById(item.getId()).get();
		assertEquals(ReconciliationItemStatus.UNRESOLVED, found.getStatus());

		reconciliationItemMapper.updateStatus(item.getId(), ReconciliationItemStatus.WRITTEN_OFF);

		ReconciliationItem updated = reconciliationItemMapper.findById(item.getId()).get();
		assertEquals(ReconciliationItemStatus.WRITTEN_OFF, updated.getStatus());
	}
}

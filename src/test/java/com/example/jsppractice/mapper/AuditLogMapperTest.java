package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.example.jsppractice.model.AuditActionType;
import com.example.jsppractice.model.AuditEntityType;
import com.example.jsppractice.model.AuditLog;
import com.example.jsppractice.model.AuditLogCursor;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class AuditLogMapperTest {

	@Autowired
	private AuditLogMapper auditLogMapper;

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
		params.put("role", "ADMIN");
		return insert.executeAndReturnKey(params).longValue();
	}

	@Test
	public void insertThenFindByEntityRoundTripsDetailJson() {
		Long userId = insertUser("auditor@example.com");
		Map<String, Object> statusChange = new HashMap<>();
		statusChange.put("oldValue", "PENDING");
		statusChange.put("newValue", "APPROVED");
		Map<String, Object> detail = new HashMap<>();
		detail.put("status", statusChange);

		AuditLog auditLog = AuditLog.builder().userId(userId).auditedAt(Instant.now())
				.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId(42L).detail(detail)
				.build();

		auditLogMapper.insert(auditLog);

		List<AuditLog> found = auditLogMapper.findByEntity(AuditEntityType.BOOK_REQUEST, 42L);

		assertEquals(1, found.size());
		AuditLog result = found.get(0);
		assertEquals(AuditActionType.APPROVE, result.getAction());
		assertEquals(userId, result.getUserId());
		@SuppressWarnings("unchecked")
		Map<String, Object> resultStatus = (Map<String, Object>) result.getDetail().get("status");
		assertEquals("PENDING", resultStatus.get("oldValue"));
		assertEquals("APPROVED", resultStatus.get("newValue"));
	}

	private AuditLogCursor cursorOf(AuditLog auditLog) {
		return new AuditLogCursor(auditLog.getAuditedAt(), auditLog.getId());
	}

	@Test
	public void findNextReturnsRowsOlderThanCursorInDescendingOrder() {
		Long userId = insertUser("auditor@example.com");
		List<AuditLog> logs = new ArrayList<>();
		// 截斷到毫秒：H2 的 TIMESTAMP 欄位儲存奈秒精度時會四捨五入，如果測試直接拿
		// insert 前、記憶體裡帶完整奈秒精度的 Instant 當 cursor，可能比 DB 實際存的值還小，
		// 導致這一列自己被判定成「比自己新」而被撈回來。截斷到毫秒可以完全避開這個誤差。
		Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		for (int i = 0; i < 3; i++) {
			AuditLog auditLog = AuditLog.builder().userId(userId).auditedAt(base.plusSeconds(i))
					.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId((long) i)
					.detail(Collections.emptyMap()).build();
			auditLogMapper.insert(auditLog);
			logs.add(auditLog);
		}

		// 第一頁：cursor 是 null，從最新的開始抓
		List<AuditLog> firstPage = auditLogMapper.findNext(null, 2);
		assertEquals(2, firstPage.size());
		assertEquals(logs.get(2).getId(), firstPage.get(0).getId());
		assertEquals(logs.get(1).getId(), firstPage.get(1).getId());

		// 第二頁：用第一頁最後一筆的 (audited_at, id) 當 cursor，往更舊的資料翻
		List<AuditLog> secondPage = auditLogMapper.findNext(cursorOf(firstPage.get(1)), 2);
		assertEquals(1, secondPage.size());
		assertEquals(logs.get(0).getId(), secondPage.get(0).getId());
	}

	@Test
	public void findPreviousReturnsRowsNewerThanCursorInDescendingOrder() {
		Long userId = insertUser("auditor@example.com");
		List<AuditLog> logs = new ArrayList<>();
		// 截斷到毫秒：H2 的 TIMESTAMP 欄位儲存奈秒精度時會四捨五入，如果測試直接拿
		// insert 前、記憶體裡帶完整奈秒精度的 Instant 當 cursor，可能比 DB 實際存的值還小，
		// 導致這一列自己被判定成「比自己新」而被撈回來。截斷到毫秒可以完全避開這個誤差。
		Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		for (int i = 0; i < 3; i++) {
			AuditLog auditLog = AuditLog.builder().userId(userId).auditedAt(base.plusSeconds(i))
					.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId((long) i)
					.detail(Collections.emptyMap()).build();
			auditLogMapper.insert(auditLog);
			logs.add(auditLog);
		}

		// 從最舊那筆往回抓比它新的紀錄，SQL 端已經排回 DESC 給畫面直接顯示用
		List<AuditLog> newer = auditLogMapper.findPrevious(cursorOf(logs.get(0)), 2);
		assertEquals(2, newer.size());
		assertEquals(logs.get(2).getId(), newer.get(0).getId());
		assertEquals(logs.get(1).getId(), newer.get(1).getId());
	}

	@Test
	public void sortsByAuditedAtNotByIdWhenTheyDisagree() {
		// 模擬併發情境：audited_at 在應用層先算好，但實際 INSERT 執行順序（決定 id 大小）
		// 可能因為中間夾雜別的邏輯而跟 audited_at 的先後順序顛倒——id 較大的那筆，
		// audited_at 反而較早。對帳/稽核排序應該以 audited_at 為準，不是 id。
		Long userId = insertUser("auditor@example.com");
		Instant earlier = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant later = earlier.plusSeconds(10);

		AuditLog insertedFirstButLaterTimestamp = AuditLog.builder().userId(userId).auditedAt(later)
				.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId(1L)
				.detail(Collections.emptyMap()).build();
		auditLogMapper.insert(insertedFirstButLaterTimestamp); // 先 insert，id 較小，但時間戳記較晚

		AuditLog insertedSecondButEarlierTimestamp = AuditLog.builder().userId(userId).auditedAt(earlier)
				.action(AuditActionType.REJECT).entityType(AuditEntityType.BOOK_REQUEST).entityId(2L)
				.detail(Collections.emptyMap()).build();
		auditLogMapper.insert(insertedSecondButEarlierTimestamp); // 後 insert，id 較大，但時間戳記較早

		List<AuditLog> firstPage = auditLogMapper.findNext(null, 10);

		assertEquals("排序應該照 audited_at（較晚的在前），不是照 id（較晚 insert 的在前）", 2, firstPage.size());
		assertEquals(insertedFirstButLaterTimestamp.getId(), firstPage.get(0).getId());
		assertEquals(insertedSecondButEarlierTimestamp.getId(), firstPage.get(1).getId());
	}
}

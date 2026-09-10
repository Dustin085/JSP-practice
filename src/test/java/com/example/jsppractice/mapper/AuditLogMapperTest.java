package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
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
import com.example.jsppractice.model.PageReq;

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

		AuditLog auditLog = AuditLog.builder().userId(userId).auditedAt(LocalDateTime.now())
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

	@Test
	public void findAllPagedAndCountRespectPageSize() {
		Long userId = insertUser("auditor@example.com");
		for (int i = 0; i < 3; i++) {
			auditLogMapper.insert(AuditLog.builder().userId(userId).auditedAt(LocalDateTime.now())
					.action(AuditActionType.APPROVE).entityType(AuditEntityType.BOOK_REQUEST).entityId((long) i)
					.detail(Collections.emptyMap()).build());
		}

		assertEquals(3, auditLogMapper.count());

		List<AuditLog> firstPage = auditLogMapper.findAllPaged(new PageReq(0, 2));
		assertEquals(2, firstPage.size());

		List<AuditLog> secondPage = auditLogMapper.findAllPaged(new PageReq(1, 2));
		assertEquals(1, secondPage.size());
	}
}

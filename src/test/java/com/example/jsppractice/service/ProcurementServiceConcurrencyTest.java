package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.example.jsppractice.exception.ProcurementAlreadyCompletedException;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class ProcurementServiceConcurrencyTest {

	@Autowired
	private ProcurementService procurementService;

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
		java.util.Map<String, Object> params = new java.util.HashMap<>();
		params.put("email", email);
		params.put("password_hash", "hashed-password");
		Long id = insert.executeAndReturnKey(params).longValue();
		jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, "PROCUREMENT");
		return id;
	}

	@Test
	public void onlyOneConcurrentCallerCanCompleteTheSameProcurementItem() throws Exception {
		Long requesterId = insertUser("requester@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.APPROVED)
				.requestedAt(Instant.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		BookRequestItem item = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Effective Java")
				.build();
		bookRequestItemMapper.insert(item);

		ProcurementItem procurementItem = ProcurementItem.from(item);
		procurementItemMapper.insert(procurementItem);

		Long procurer1Id = insertUser("procurer1@example.com");
		Long procurer2Id = insertUser("procurer2@example.com");
		User procurer1 = User.builder().id(procurer1Id).roles(Set.of(RoleType.PROCUREMENT)).build();
		User procurer2 = User.builder().id(procurer2Id).roles(Set.of(RoleType.PROCUREMENT)).build();

		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);

		Callable<Object> task1 = raceTask(procurementItem.getId(), procurer1, readyLatch, startLatch);
		Callable<Object> task2 = raceTask(procurementItem.getId(), procurer2, readyLatch, startLatch);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> future1 = executor.submit(task1);
			Future<Object> future2 = executor.submit(task2);

			readyLatch.await();
			startLatch.countDown();

			Object result1 = future1.get();
			Object result2 = future2.get();

			List<Object> results = List.of(result1, result2);
			long successCount = results.stream().filter(r -> r instanceof ProcurementItem).count();
			long failureCount = results.stream().filter(r -> r instanceof ProcurementAlreadyCompletedException).count();

			assertEquals("恰好只有一個呼叫者應該成功", 1, successCount);
			assertEquals("另一個呼叫者應該收到已完成的例外", 1, failureCount);
		} finally {
			executor.shutdown();
		}

		List<ProcurementItem> completed = procurementItemMapper.findByStatus(ProcurementStatus.COMPLETED);
		assertEquals("最終只能有一筆完成紀錄", 1, completed.size());

		int bookCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Integer.class);
		assertTrue("不能因為併發呼叫而建立出兩本重複的書", bookCount == 1);
	}

	private Callable<Object> raceTask(Long procurementItemId, User currentUser, CountDownLatch readyLatch,
			CountDownLatch startLatch) {
		return () -> {
			readyLatch.countDown();
			startLatch.await();
			try {
				return procurementService.completeProcurement(procurementItemId, currentUser);
			} catch (ProcurementAlreadyCompletedException e) {
				return e;
			}
		};
	}
}

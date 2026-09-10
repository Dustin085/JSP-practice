package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.BookRequestService.BookRequestBookInfo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class BookRequestIdempotencyTest {

	@Autowired
	private BookRequestService bookRequestService;

	@Autowired
	private BookRequestMapper bookRequestMapper;

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
	public void sameKeySubmittedTwiceSequentiallyOnlyCreatesOneBookRequest() {
		Long requesterId = insertUser("requester@example.com");
		User requester = User.builder().id(requesterId).role(RoleType.USER).build();
		String idempotencyKey = UUID.randomUUID().toString();
		List<BookRequestBookInfo> bookInfos = List
				.of(new BookRequestBookInfo("Effective Java", null, null, null, null));

		BookRequest first = bookRequestService.submit(requester, bookInfos, idempotencyKey);
		BookRequest second = bookRequestService.submit(requester, bookInfos, idempotencyKey);

		assertEquals("第二次送出應該回傳跟第一次同一筆申請", first.getId(), second.getId());
		assertEquals("不該因為重複送出多建立一筆", 1, bookRequestMapper.findAll().size());
	}

	@Test
	public void concurrentSubmitsWithSameIdempotencyKeyOnlyCreateOneBookRequest() throws Exception {
		Long requesterId = insertUser("requester2@example.com");
		User requester = User.builder().id(requesterId).role(RoleType.USER).build();
		String idempotencyKey = UUID.randomUUID().toString();
		List<BookRequestBookInfo> bookInfos = List
				.of(new BookRequestBookInfo("Effective Java", null, null, null, null));

		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);

		Callable<BookRequest> task = () -> {
			readyLatch.countDown();
			startLatch.await();
			return bookRequestService.submit(requester, bookInfos, idempotencyKey);
		};

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<BookRequest> future1 = executor.submit(task);
			Future<BookRequest> future2 = executor.submit(task);

			readyLatch.await();
			startLatch.countDown();

			BookRequest result1 = future1.get();
			BookRequest result2 = future2.get();

			assertEquals("兩個並發呼叫應該回傳同一筆申請的 id", result1.getId(), result2.getId());
		} finally {
			executor.shutdown();
		}

		List<BookRequest> all = bookRequestMapper.findAll();
		assertEquals("不管真的併發送出幾次，只能建立一筆 book_request", 1, all.size());
	}
}

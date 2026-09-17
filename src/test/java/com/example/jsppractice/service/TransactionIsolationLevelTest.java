package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
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

// 隔離層級真正會有感覺的情境是「同一個交易內分兩次讀同一列，中間夾了另一個交易的 commit」，
// 跟報表/JOIN 查幾張表無關（單一 SELECT 在任何隔離層級下都是同一時間點的快照）。這個專案
// 目前沒有業務流程長這樣，所以這裡刻意繞過 Spring 的 @Transactional/service 層，直接控制
// JDBC Connection 自己開交易、自己 commit——這樣才能精準卡住「A 交易讀一次 → B 交易 commit →
// A 交易再讀一次」這個時序，純粹是在證明資料庫隔離層級這個概念本身，跟業務程式碼寫法無關。
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class TransactionIsolationLevelTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private DataBaseCleaner dataBaseCleaner;
	private Long bookId;

	@Before
	public void setUp() {
		dataBaseCleaner = new DataBaseCleaner(jdbcTemplate);

		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("books")
				.usingGeneratedKeyColumns("id");
		Map<String, Object> params = new HashMap<>();
		params.put("title", "Original Title");
		params.put("version", 0);
		bookId = insert.executeAndReturnKey(params).longValue();
	}

	@After
	public void tearDown() {
		dataBaseCleaner.clean();
	}

	@Test
	public void readCommittedSeesTheOtherTransactionsCommittedChangeOnSecondRead() throws Exception {
		String secondRead = runIsolationScenario(Connection.TRANSACTION_READ_COMMITTED);

		assertEquals("non-repeatable read：B commit 之後，A 交易內第二次讀應該看到新值", "Updated Title", secondRead);
	}

	@Test
	public void repeatableReadKeepsSeeingTheOriginalSnapshotOnSecondRead() throws Exception {
		String secondRead = runIsolationScenario(Connection.TRANSACTION_REPEATABLE_READ);

		assertEquals("REPEATABLE READ：A 交易不管讀幾次，看到的都是交易開始那一刻的快照，" + "B 已經 commit 的異動要等 A 交易結束才看得到", "Original Title",
				secondRead);
	}

	// 回傳 A 交易「第二次讀」讀到的 title；兩個測試只差在傳進來的隔離層級，時序都一樣：
	// A 先讀一次 → 訊號通知 B → B 更新並 commit → 訊號通知 A → A 再讀一次 → A commit
	private String runIsolationScenario(int isolationLevel) throws Exception {
		CountDownLatch firstReadDone = new CountDownLatch(1);
		CountDownLatch otherTransactionCommitted = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<String> transactionA = executor.submit(() -> {
				try (Connection connection = dataSource.getConnection()) {
					connection.setAutoCommit(false);
					connection.setTransactionIsolation(isolationLevel);

					readTitle(connection);
					firstReadDone.countDown();

					otherTransactionCommitted.await();
					String secondRead = readTitle(connection);

					connection.commit();
					return secondRead;
				}
			});

			Future<?> transactionB = executor.submit(() -> {
				try {
					firstReadDone.await();
					try (Connection connection = dataSource.getConnection()) {
						connection.setAutoCommit(false);
						updateTitle(connection, "Updated Title");
						connection.commit();
					}
					otherTransactionCommitted.countDown();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			transactionB.get();
			return transactionA.get();
		} finally {
			executor.shutdown();
		}
	}

	private String readTitle(Connection connection) throws Exception {
		try (PreparedStatement ps = connection.prepareStatement("SELECT title FROM books WHERE id = ?")) {
			ps.setLong(1, bookId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString("title");
			}
		}
	}

	private void updateTitle(Connection connection, String newTitle) throws Exception {
		try (PreparedStatement ps = connection.prepareStatement("UPDATE books SET title = ? WHERE id = ?")) {
			ps.setString(1, newTitle);
			ps.setLong(2, bookId);
			ps.executeUpdate();
		}
	}
}

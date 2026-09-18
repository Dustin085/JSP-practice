package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import com.example.jsppractice.crypto.AesEncryptor;
import com.example.jsppractice.crypto.EmailLookupHasher;
import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;

// 不用 Mockito——之前 Mockito 版的 DeliveryImportServiceImplTest 一直是綠燈，
// 卻沒有測到「其中一筆 completeProcurement() 丟例外，會不會把整批一起拖下水」這個真正的坑，
// 因為 mock 根本不會經過 Spring 的交易 proxy。這裡接真正的 Spring context/H2，
// 才測得到 @Transactional 傳播行為本身有沒有問題。
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class DeliveryImportServiceIntegrationTest {

	@Autowired
	private DeliveryImportService deliveryImportService;

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

	private static final Charset BIG5 = Charset.forName("Big5");

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
		params.put("email", AesEncryptor.encrypt(email));
		params.put("email_lookup_hash", EmailLookupHasher.hash(email));
		params.put("password_hash", "hashed-password");
		return insert.executeAndReturnKey(params).longValue();
	}

	private static byte[] anField(String value, int length) {
		byte[] valueBytes = value.getBytes(BIG5);
		byte[] field = new byte[length];
		Arrays.fill(field, (byte) ' ');
		System.arraycopy(valueBytes, 0, field, 0, valueBytes.length);
		return field;
	}

	private static byte[] nField(long value, int length) {
		return String.format("%0" + length + "d", value).getBytes(BIG5);
	}

	private static byte[] concat(byte[]... parts) {
		int totalLength = 0;
		for (byte[] part : parts) {
			totalLength += part.length;
		}
		byte[] result = new byte[totalLength];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, result, offset, part.length);
			offset += part.length;
		}
		return result;
	}

	private static byte[] joinLines(byte[]... lines) {
		int totalLength = 0;
		for (int i = 0; i < lines.length; i++) {
			totalLength += lines[i].length;
			if (i < lines.length - 1) {
				totalLength += 1;
			}
		}
		byte[] result = new byte[totalLength];
		int offset = 0;
		for (int i = 0; i < lines.length; i++) {
			System.arraycopy(lines[i], 0, result, offset, lines[i].length);
			offset += lines[i].length;
			if (i < lines.length - 1) {
				result[offset] = 0x0A;
				offset++;
			}
		}
		return result;
	}

	@Test
	public void importDeliveriesCommitsSuccessfulItemsEvenWhenAnotherLineInTheSameFileFails() {
		Long systemUserId = insertUser(SystemAccount.EMAIL);
		jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", systemUserId, "USER");

		Long requesterId = insertUser("requester@example.com");
		BookRequest bookRequest = BookRequest.builder().requesterId(requesterId).status(BookRequestStatus.APPROVED)
				.requestedAt(Instant.now()).idempotencyKey(UUID.randomUUID().toString()).build();
		bookRequestMapper.insert(bookRequest);

		BookRequestItem item = BookRequestItem.builder().bookRequestId(bookRequest.getId()).title("Effective Java")
				.isbn("9780134685991").build();
		bookRequestItemMapper.insert(item);

		ProcurementItem procurementItem = ProcurementItem.from(item);
		procurementItemMapper.insert(procurementItem);

		byte[] header = concat(anField("H", 1), anField("V00001", 6), nField(20260917, 8));
		// 第一筆對到真實存在、PENDING 的採購項目，第二筆用一個不存在的單號，
		// 故意讓 completeProcurement() 對第二筆丟 NoSuchElementException
		byte[] detailOk = concat(anField("D", 1), nField(procurementItem.getId(), 10), anField("9780134685991", 20),
				nField(5, 4), nField(120000, 8), nField(20260917, 8), anField("1", 1), anField("測試書名", 40),
				anField("", 20));
		byte[] detailMissing = concat(anField("D", 1), nField(999999, 10), anField("0000000000000", 20), nField(1, 4),
				nField(10000, 8), nField(20260917, 8), anField("1", 1), anField("查無此單", 40), anField("", 20));
		byte[] trailer = concat(anField("T", 1), nField(2, 6));
		byte[] file = joinLines(header, detailOk, detailMissing, trailer);

		// 修 bug 之前這裡會丟 UnexpectedRollbackException：completeProcurement() 對第二筆
		// 丟的例外會把整個 importDeliveries() 的外層交易標記成 rollback-only，即使被 catch 住，
		// 最後 commit 時還是會整批失敗，連第一筆本來該成功的也一起被吞掉
		DeliveryImportResult result = deliveryImportService.importDeliveries(file);

		assertEquals(1, result.appliedCount());
		assertEquals(1, result.skipped().size());

		Optional<ProcurementItem> updated = procurementItemMapper.findById(procurementItem.getId());
		assertEquals(ProcurementStatus.COMPLETED, updated.get().getStatus());
	}
}

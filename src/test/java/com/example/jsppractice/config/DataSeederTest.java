package com.example.jsppractice.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.service.AuthService;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookRequestService;
import com.example.jsppractice.service.BookService;
import com.example.jsppractice.service.CategoryService;
import com.example.jsppractice.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class DataSeederTest {

	@Autowired
	private AuthorService authorService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private BookService bookService;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private BookRequestService bookRequestService;

	@Autowired
	private BookRequestMapper bookRequestMapper;

	@Autowired
	private BookRequestItemMapper bookRequestItemMapper;

	@Autowired
	private ProcurementItemMapper procurementItemMapper;

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
	public void seedRunsWithoutErrorsAndProducesAllThreeStatuses() {
		new DataSeeder(authorService, categoryService, bookService, authService, userService, passwordEncoder,
				bookRequestService, bookRequestMapper, bookRequestItemMapper, procurementItemMapper).seed();

		assertTrue(bookRequestService.findByStatus(BookRequestStatus.PENDING).size() > 0);
		assertTrue(bookRequestService.findByStatus(BookRequestStatus.APPROVED).size() > 0);
		assertTrue(bookRequestService.findByStatus(BookRequestStatus.REJECTED).size() > 0);
		// 12 筆走正常 submit/approve/reject 流程 + 2 筆刻意繞過流程製造對帳落差的測試資料
		assertEquals(14, bookRequestService.findAll().size());
		assertTrue("應該要有至少一筆已核准但缺採購項目的測試資料，供 /reconciliations 手動測試用",
				bookRequestItemMapper.findApprovedWithoutProcurementItem().size() > 0);
		assertTrue("應該要有至少一筆已完成採購但缺書籍的測試資料，供 /reconciliations 手動測試用",
				procurementItemMapper.findCompletedWithoutBook().size() > 0);
	}
}

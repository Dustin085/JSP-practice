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
				bookRequestService).seed();

		assertTrue(bookRequestService.findByStatus(BookRequestStatus.PENDING).size() > 0);
		assertTrue(bookRequestService.findByStatus(BookRequestStatus.APPROVED).size() > 0);
		assertTrue(bookRequestService.findByStatus(BookRequestStatus.REJECTED).size() > 0);
		assertEquals(12, bookRequestService.findAll().size());
	}
}

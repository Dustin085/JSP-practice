package com.example.jsppractice.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.AuthService;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookRequestService;
import com.example.jsppractice.service.BookRequestService.BookRequestBookInfo;
import com.example.jsppractice.service.BookService;
import com.example.jsppractice.service.CategoryService;
import com.example.jsppractice.service.UserService;

public class DataSeeder {

	private final AuthorService authorService;
	private final CategoryService categoryService;
	private final BookService bookService;
	private final AuthService authService;
	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final BookRequestService bookRequestService;

	public DataSeeder(AuthorService authorService, CategoryService categoryService, BookService bookService,
			AuthService authService, UserService userService, PasswordEncoder passwordEncoder,
			BookRequestService bookRequestService) {
		this.authorService = authorService;
		this.categoryService = categoryService;
		this.bookService = bookService;
		this.authService = authService;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.bookRequestService = bookRequestService;
	}

	public void seed() {
		Author joshua = authorService.save(new Author(null, "Joshua Bloch"));
		Author martin = authorService.save(new Author(null, "Robert C. Martin"));
		Author eric = authorService.save(new Author(null, "Eric Evans"));

		Category programming = categoryService.save(new Category(null, "Programming"));
		Category architecture = categoryService.save(new Category(null, "Architecture"));
		Category bestPractice = categoryService.save(new Category(null, "Best Practice"));

		Book effectiveJava = bookService.save(new Book(null, "Effective Java", "9780134685991", joshua.getId(), 2018));
		bookService.saveCategories(effectiveJava.getId(), List.of(programming.getId(), bestPractice.getId()));

		Book cleanCode = bookService.save(new Book(null, "Clean Code", "9780132350884", martin.getId(), 2008));
		bookService.saveCategories(cleanCode.getId(), List.of(bestPractice.getId()));

		Book ddd = bookService.save(new Book(null, "Domain-Driven Design", "9780321125217", eric.getId(), 2003));
		bookService.saveCategories(ddd.getId(), List.of(architecture.getId()));

		Book cleanArchitecture = bookService
				.save(new Book(null, "Clean Architecture", "9780134494166", martin.getId(), 2017));
		bookService.saveCategories(cleanArchitecture.getId(), List.of(architecture.getId(), bestPractice.getId()));

		for (int i = 0; i < 20; i++) {
			Book javaConcurrency = bookService
					.save(new Book(null, "Java Concurrency in Practice", "9780321349606", joshua.getId(), 2006));
			bookService.saveCategories(javaConcurrency.getId(), List.of(programming.getId()));
		}

		User requester = authService.register("user@example.com", "測試使用者", "password123");

		User admin = User.builder().email("admin@example.com").name("管理員")
				.passwordHash(passwordEncoder.encode("password123")).role(RoleType.ADMIN).build();
		userService.save(admin);

		User procurementStaff = User.builder().email("procurement@example.com").name("採購人員")
				.passwordHash(passwordEncoder.encode("password123")).role(RoleType.PROCUREMENT).build();
		userService.save(procurementStaff);

		seedBookRequests(requester, admin);
	}

	private void seedBookRequests(User requester, User admin) {
		// PENDING：還沒被審核
		bookRequestService.submit(requester, List.of(new BookRequestBookInfo("Refactoring", null, null, null, null)),
				newIdempotencyKey());
		bookRequestService.submit(requester,
				List.of(new BookRequestBookInfo("The Pragmatic Programmer", "9780135957059", null, 2019,
						new BigDecimal("850")),
						new BookRequestBookInfo("Head First Design Patterns", null, null, null, null)),
				newIdempotencyKey());
		bookRequestService.submit(requester,
				List.of(new BookRequestBookInfo("Test-Driven Development", "9780321146533", null, 2002, null)),
				newIdempotencyKey());
		bookRequestService.submit(requester,
				List.of(new BookRequestBookInfo("Working Effectively with Legacy Code", null, null, null, null)),
				newIdempotencyKey());
		bookRequestService.submit(requester,
				List.of(new BookRequestBookInfo("Continuous Delivery", null, null, null, null)), newIdempotencyKey());

		// APPROVED：送出後由 admin 核准
		BookRequestBookInfo sre = new BookRequestBookInfo("Site Reliability Engineering", null, null, 2016,
				new BigDecimal("1200"));
		bookRequestService.approve(bookRequestService.submit(requester, List.of(sre), newIdempotencyKey()).getId(),
				admin);

		BookRequestBookInfo databaseInternals = new BookRequestBookInfo("Database Internals", null, null, null, null);
		BookRequestBookInfo ddia = new BookRequestBookInfo("Designing Data-Intensive Applications", "9781449373320",
				null, 2017, new BigDecimal("1500"));
		bookRequestService.approve(
				bookRequestService.submit(requester, List.of(databaseInternals, ddia), newIdempotencyKey()).getId(),
				admin);

		bookRequestService.approve(
				bookRequestService
						.submit(requester, List.of(new BookRequestBookInfo("The Clean Coder", null, null, null, null)),
								newIdempotencyKey())
						.getId(),
				admin);
		bookRequestService.approve(
				bookRequestService
						.submit(requester, List.of(new BookRequestBookInfo("Release It!", null, null, null, null)),
								newIdempotencyKey())
						.getId(),
				admin);

		// REJECTED：送出後由 admin 拒絕
		bookRequestService.reject(bookRequestService
				.submit(requester,
						List.of(new BookRequestBookInfo("Overpriced Book", null, null, null, new BigDecimal("99999"))),
						newIdempotencyKey())
				.getId(), admin);
		bookRequestService.reject(bookRequestService
				.submit(requester,
						List.of(new BookRequestBookInfo("Duplicate Request Example", null, null, null, null)),
						newIdempotencyKey())
				.getId(), admin);
		bookRequestService.reject(bookRequestService
				.submit(requester, List.of(new BookRequestBookInfo("Out of Scope Book", null, null, null, null)),
						newIdempotencyKey())
				.getId(), admin);
	}

	private String newIdempotencyKey() {
		return UUID.randomUUID().toString();
	}
}

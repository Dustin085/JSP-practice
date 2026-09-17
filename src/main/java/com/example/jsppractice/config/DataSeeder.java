package com.example.jsppractice.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.jsppractice.mapper.BookRequestItemMapper;
import com.example.jsppractice.mapper.BookRequestMapper;
import com.example.jsppractice.mapper.ProcurementItemMapper;
import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.BookRequest;
import com.example.jsppractice.model.BookRequestItem;
import com.example.jsppractice.model.BookRequestStatus;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.ProcurementItem;
import com.example.jsppractice.model.ProcurementStatus;
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
	private final BookRequestMapper bookRequestMapper;
	private final BookRequestItemMapper bookRequestItemMapper;
	private final ProcurementItemMapper procurementItemMapper;

	public DataSeeder(AuthorService authorService, CategoryService categoryService, BookService bookService,
			AuthService authService, UserService userService, PasswordEncoder passwordEncoder,
			BookRequestService bookRequestService, BookRequestMapper bookRequestMapper,
			BookRequestItemMapper bookRequestItemMapper, ProcurementItemMapper procurementItemMapper) {
		this.authorService = authorService;
		this.categoryService = categoryService;
		this.bookService = bookService;
		this.authService = authService;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.bookRequestService = bookRequestService;
		this.bookRequestMapper = bookRequestMapper;
		this.bookRequestItemMapper = bookRequestItemMapper;
		this.procurementItemMapper = procurementItemMapper;
	}

	public void seed() {
		Author joshua = authorService.save(new Author(null, "Joshua Bloch"));
		Author martin = authorService.save(new Author(null, "Robert C. Martin"));
		Author eric = authorService.save(new Author(null, "Eric Evans"));

		Category programming = categoryService.save(new Category(null, "Programming"));
		Category architecture = categoryService.save(new Category(null, "Architecture"));
		Category bestPractice = categoryService.save(new Category(null, "Best Practice"));

		Book effectiveJava = bookService
				.save(new Book(null, "Effective Java", "9780134685991", joshua.getId(), 2018, 0));
		bookService.saveCategories(effectiveJava.getId(), List.of(programming.getId(), bestPractice.getId()));

		Book cleanCode = bookService.save(new Book(null, "Clean Code", "9780132350884", martin.getId(), 2008, 0));
		bookService.saveCategories(cleanCode.getId(), List.of(bestPractice.getId()));

		Book ddd = bookService.save(new Book(null, "Domain-Driven Design", "9780321125217", eric.getId(), 2003, 0));
		bookService.saveCategories(ddd.getId(), List.of(architecture.getId()));

		Book cleanArchitecture = bookService
				.save(new Book(null, "Clean Architecture", "9780134494166", martin.getId(), 2017, 0));
		bookService.saveCategories(cleanArchitecture.getId(), List.of(architecture.getId(), bestPractice.getId()));

		for (int i = 0; i < 20; i++) {
			Book javaConcurrency = bookService
					.save(new Book(null, "Java Concurrency in Practice", "9780321349606", joshua.getId(), 2006, 0));
			bookService.saveCategories(javaConcurrency.getId(), List.of(programming.getId()));
		}

		User requester = authService.register("user@example.com", "測試使用者", "password123");

		User admin = User.builder().email("admin@example.com").name("管理員")
				.passwordHash(passwordEncoder.encode("password123")).roles(Set.of(RoleType.ADMIN, RoleType.USER))
				.build();
		userService.save(admin);

		User procurementStaff = User.builder().email("procurement@example.com").name("採購人員")
				.passwordHash(passwordEncoder.encode("password123"))
				.roles(Set.of(RoleType.PROCUREMENT, RoleType.USER)).build();
		userService.save(procurementStaff);

		seedBookRequests(requester, admin);
		seedReconciliationDiscrepancies(requester);
	}

	// 故意繞過 BookRequestServiceImpl/ProcurementServiceImpl 正常的核准/採購流程，
	// 直接用 mapper 造出「已核准卻缺採購項目」「已完成採購卻缺書籍」這兩種資料不一致，
	// 因為正常流程本來就會保證這兩件事一致，唯一能製造出落差的方式就是繞過它、直接動資料庫，
	// 這樣才有東西讓 /reconciliations 的兩個對帳功能可以在瀏覽器上測試。
	private void seedReconciliationDiscrepancies(User requester) {
		BookRequest orphanApproval = BookRequest.builder().requesterId(requester.getId())
				.status(BookRequestStatus.APPROVED).requestedAt(Instant.now()).idempotencyKey(newIdempotencyKey())
				.build();
		bookRequestMapper.insert(orphanApproval);
		bookRequestItemMapper.insert(BookRequestItem.builder().bookRequestId(orphanApproval.getId())
				.title("[測試用] 已核准但缺採購項目").build());

		BookRequest anotherApproval = BookRequest.builder().requesterId(requester.getId())
				.status(BookRequestStatus.APPROVED).requestedAt(Instant.now()).idempotencyKey(newIdempotencyKey())
				.build();
		bookRequestMapper.insert(anotherApproval);
		BookRequestItem brokenItem = BookRequestItem.builder().bookRequestId(anotherApproval.getId())
				.title("[測試用] 已完成採購但缺書籍紀錄").build();
		bookRequestItemMapper.insert(brokenItem);
		procurementItemMapper.insert(
				ProcurementItem.builder().bookRequestItemId(brokenItem.getId()).status(ProcurementStatus.COMPLETED)
						.build());
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

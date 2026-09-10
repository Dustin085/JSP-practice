package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

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
import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class BookMapperTest {
	@Autowired
	private BookMapper bookMapper;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private DataBaseCleaner dataBaseCleaner;
	private Long authorId;

	@Before
	public void setUp() {
		dataBaseCleaner = new DataBaseCleaner(jdbcTemplate);

		SimpleJdbcInsert insertAuthor = new SimpleJdbcInsert(dataSource).withTableName("authors")
				.usingGeneratedKeyColumns("id");
		authorId = insertAuthor.executeAndReturnKey(Collections.singletonMap("name", "Joshua Bloch")).longValue();
	}

	private Book sampleBook() {
		return new Book(null, "Effective Java", "978-0134685991", authorId, 2018);
	}

	private Long insertCategory(String name) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("categories")
				.usingGeneratedKeyColumns("id");
		return insert.executeAndReturnKey(Collections.singletonMap("name", name)).longValue();
	}

	@After
	public void tearDown() {
		dataBaseCleaner.clean();
	}

	@Test
	public void findAllTest() {
		List<Long> categoryIds = List.of(insertCategory("Nature"), insertCategory("Coding"));
		Book bookWithCategories = sampleBook();
		bookMapper.insert(bookWithCategories);
		bookMapper.insertBookCategories(bookWithCategories.getId(), categoryIds);
		Book bookWithoutCategories = sampleBook();
		bookMapper.insert(bookWithoutCategories);

		List<BookSummary> bookSummaries = bookMapper.findAll();

		assertEquals(bookWithCategories.getId(), bookSummaries.get(0).getId());
		assertEquals(bookWithoutCategories.getId(), bookSummaries.get(1).getId());

		assertEquals(2, bookSummaries.get(0).getCategories().size());
		assertEquals(0, bookSummaries.get(1).getCategories().size());
	}

	@Test
	public void findByAuthorIdTest() {
		int BOOK_COUNT = 2;
		for (int i = 0; i < BOOK_COUNT; i++) {
			bookMapper.insert(sampleBook());
		}
		List<Book> books = bookMapper.findByAuthorId(authorId);
		assertEquals(BOOK_COUNT, books.size());
	}

	@Test
	public void updateTest() {
		String NEW_TITLE = "My New Title";
		Book saved = sampleBook();
		bookMapper.insert(saved);
		Long bookId = saved.getId();
		saved.setTitle(NEW_TITLE);
		bookMapper.update(saved);
		assertEquals(NEW_TITLE, bookMapper.findById(bookId).getTitle());
		assertEquals(1, bookMapper.findAll().size());
	}

	@Test
	public void insertCategoriesTest() {
		Long categoryId = insertCategory("Nature");
		Book savedBook = sampleBook();
		bookMapper.insert(savedBook);
		bookMapper.insertBookCategories(savedBook.getId(), List.of(categoryId));
		List<Category> categories = bookMapper.findCategoriesById(savedBook.getId());

		assertEquals(categoryId, categories.get(0).getId());
		assertEquals("Nature", categories.get(0).getName());
	}

	@Test
	public void deleteCategoriesTest() {
		Long categoryId = insertCategory("Nature");
		Book savedBook = sampleBook();
		bookMapper.insert(savedBook);
		bookMapper.insertBookCategories(savedBook.getId(), List.of(categoryId));
		bookMapper.deleteCategoriesByBookId(savedBook.getId());
		List<Category> categories = bookMapper.findCategoriesById(savedBook.getId());

		assertEquals(0, categories.size());
	}

	@Test
	public void findCategoriesByIdTest() {
		Long categoryId = insertCategory("Nature");
		Book savedBook = sampleBook();
		bookMapper.insert(savedBook);
		bookMapper.insertBookCategories(savedBook.getId(), List.of(categoryId));
		List<Category> categories = bookMapper.findCategoriesById(savedBook.getId());

		assertEquals(categoryId, categories.get(0).getId());
		assertEquals("Nature", categories.get(0).getName());
		assertEquals(1, categories.size());
	}

	@Test
	public void searchMatchesByTitle() {
		bookMapper.insert(new Book(null, "Effective Java", "978-0134685991", authorId, 2018));
		bookMapper.insert(new Book(null, "Unrelated Book", "9999999999999", authorId, 2000));

		List<BookSummary> results = bookMapper.search("Effective");

		assertEquals(1, results.size());
		assertEquals("Effective Java", results.get(0).getTitle());
	}

	@Test
	public void searchMatchesByAuthorName() {
		SimpleJdbcInsert insertAuthor = new SimpleJdbcInsert(dataSource).withTableName("authors")
				.usingGeneratedKeyColumns("id");
		Long otherAuthorId = insertAuthor.executeAndReturnKey(Collections.singletonMap("name", "Robert C. Martin"))
				.longValue();

		bookMapper.insert(sampleBook());
		bookMapper.insert(new Book(null, "Clean Code", "9780132350884", otherAuthorId, 2008));

		List<BookSummary> results = bookMapper.search("Bloch");

		assertEquals(1, results.size());
		assertEquals("Effective Java", results.get(0).getTitle());
	}

	@Test
	public void searchWithBlankKeywordReturnsAllBooks() {
		bookMapper.insert(sampleBook());
		bookMapper.insert(new Book(null, "Unrelated Book", "9999999999999", authorId, 2000));

		List<BookSummary> results = bookMapper.search("");

		assertEquals(2, results.size());
	}

	@Test
	public void paginationIsNotThrownOffByBooksWithMultipleCategories() {
		// 這本書掛 3 個分類，JOIN 展開後底層會是 3 列，用來驗證分頁不會被分類數量灌水
		List<Long> categoryIds = List.of(insertCategory("A"), insertCategory("B"), insertCategory("C"));
		Book bookWithManyCategories = new Book(null, "Book With Many Categories", null, authorId, 2020);
		bookMapper.insert(bookWithManyCategories);
		bookMapper.insertBookCategories(bookWithManyCategories.getId(), categoryIds);

		Book bookWithoutCategories = new Book(null, "Book Without Categories", null, authorId, 2021);
		bookMapper.insert(bookWithoutCategories);

		long totalElements = bookMapper.countBySearch(null);
		assertEquals("COUNT 不該被分類 JOIN 灌水，應該是 2 本書，不是 4 列", 2, totalElements);

		PageReq firstPage = new PageReq(0, 1);
		List<Long> firstPageIds = bookMapper.findIdsBySearch(null, firstPage);
		assertEquals("pageSize=1 應該恰好撈到 1 本書的 id，不受分類數量影響", 1, firstPageIds.size());
		assertEquals(bookWithManyCategories.getId(), firstPageIds.get(0));

		List<BookSummary> firstPageContent = bookMapper.findByIds(firstPageIds);
		assertEquals(1, firstPageContent.size());
		assertEquals(3, firstPageContent.get(0).getCategories().size());

		PageReq secondPage = new PageReq(1, 1);
		List<Long> secondPageIds = bookMapper.findIdsBySearch(null, secondPage);
		assertEquals(1, secondPageIds.size());
		assertEquals(bookWithoutCategories.getId(), secondPageIds.get(0));
	}
}

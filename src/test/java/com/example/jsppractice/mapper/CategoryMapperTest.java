package com.example.jsppractice.mapper;

import static org.junit.Assert.assertEquals;

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
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.Category;
import com.example.jsppractice.model.PageReq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class CategoryMapperTest {

	@Autowired
	private CategoryMapper categoryMapper;

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

	private Long insertAuthor(String name) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("authors")
				.usingGeneratedKeyColumns("id");
		return insert.executeAndReturnKey(Collections.singletonMap("name", name)).longValue();
	}

	private Long insertCategory(String name) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("categories")
				.usingGeneratedKeyColumns("id");
		return insert.executeAndReturnKey(Collections.singletonMap("name", name)).longValue();
	}

	private Long insertBook(String title, Long authorId) {
		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("books")
				.usingGeneratedKeyColumns("id");
		Map<String, Object> params = new HashMap<>();
		params.put("title", title);
		params.put("isbn", "978-0134685991");
		params.put("author_id", authorId);
		params.put("published_year", 2018);
		params.put("version", 0);
		return insert.executeAndReturnKey(params).longValue();
	}

	private void linkBookToCategory(Long bookId, Long categoryId) {
		Map<String, Object> params = new HashMap<>();
		params.put("book_id", bookId);
		params.put("category_id", categoryId);
		new SimpleJdbcInsert(dataSource).withTableName("books_categories").execute(params);
	}

	@Test
	public void findAllReturnsAllCategories() {
		insertCategory("Fiction");
		insertCategory("Non-Fiction");

		List<Category> categories = categoryMapper.findAll();

		assertEquals(2, categories.size());
	}

	@Test
	public void findByIdReturnsMatchingCategory() {
		Long categoryId = insertCategory("Fiction");

		Category found = categoryMapper.findById(categoryId);

		assertEquals("Fiction", found.getName());
	}

	@Test
	public void findBooksByCategoryIdReturnsOnlyLinkedBooks() {
		Long authorId = insertAuthor("Joshua Bloch");
		Long fictionId = insertCategory("Fiction");
		Long historyId = insertCategory("History");

		Long linkedBookId = insertBook("Effective Java", authorId);
		Long unlinkedBookId = insertBook("Unrelated Book", authorId);

		linkBookToCategory(linkedBookId, fictionId);
		linkBookToCategory(unlinkedBookId, historyId);

		List<Book> books = categoryMapper.findBooksByCategoryId(fictionId);

		assertEquals(1, books.size());
		assertEquals("Effective Java", books.get(0).getTitle());
	}

	@Test
	public void insertAssignsGeneratedId() {
		Category category = new Category(null, "Fiction");

		categoryMapper.insert(category);

		assertEquals("Fiction", categoryMapper.findById(category.getId()).getName());
	}

	@Test
	public void updateChangesExistingRow() {
		Long categoryId = insertCategory("Fiction");
		Category category = new Category(categoryId, "Updated Name");

		categoryMapper.update(category);

		assertEquals("Updated Name", categoryMapper.findById(categoryId).getName());
	}

	@Test
	public void deleteByIdRemovesRow() {
		Long categoryId = insertCategory("Fiction");

		categoryMapper.deleteById(categoryId);

		assertEquals(0, categoryMapper.findAll().size());
	}

	@Test
	public void findAllPagedAndCountRespectPageSize() {
		insertCategory("Fiction");
		insertCategory("Non-Fiction");
		insertCategory("History");

		assertEquals(3, categoryMapper.count());

		List<Category> firstPage = categoryMapper.findAllPaged(new PageReq(0, 2));
		assertEquals(2, firstPage.size());

		List<Category> secondPage = categoryMapper.findAllPaged(new PageReq(1, 2));
		assertEquals(1, secondPage.size());
	}
}

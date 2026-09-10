package com.example.jsppractice.repository;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.jsppractice.config.RootConfig;
import com.example.jsppractice.helper.DataBaseCleaner;
import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class JdbcAuthorRepositoryTest {

	@Autowired
	private AuthorRepository authorRepository;

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
	public void findAllPagedAndCountRespectPageSize() {
		authorRepository.insert(new Author(null, "Author A"));
		authorRepository.insert(new Author(null, "Author B"));
		authorRepository.insert(new Author(null, "Author C"));

		assertEquals(3, authorRepository.count());

		List<Author> firstPage = authorRepository.findAll(new PageReq(0, 2));
		assertEquals(2, firstPage.size());

		List<Author> secondPage = authorRepository.findAll(new PageReq(1, 2));
		assertEquals(1, secondPage.size());
	}
}

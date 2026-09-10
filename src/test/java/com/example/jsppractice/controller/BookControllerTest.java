package com.example.jsppractice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.dto.BookSummary;
import com.example.jsppractice.model.Book;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookService;
import com.example.jsppractice.service.CategoryService;

@RunWith(MockitoJUnitRunner.class)
public class BookControllerTest {

	@Mock
	private BookService bookService;

	@Mock
	private AuthorService authorService;

	@Mock
	private CategoryService categoryService;

	@InjectMocks
	private BookController bookController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();
		when(authorService.findAll()).thenReturn(Collections.emptyList());
		when(categoryService.findAll()).thenReturn(Collections.emptyList());
	}

	@Test
	public void newFormShowsEmptyBookAndOptionLists() throws Exception {
		mockMvc.perform(get("/books/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("books/form"))
				.andExpect(model().attributeExists("book"))
				.andExpect(model().attribute("authors", Collections.emptyList()))
				.andExpect(model().attribute("categories", Collections.emptyList()));
	}

	@Test
	public void createWithBlankTitleReturnsToFormWithoutSaving() throws Exception {
		mockMvc.perform(post("/books")
						.param("title", "")
						.param("publishedYear", "2020"))
				.andExpect(status().isOk())
				.andExpect(view().name("books/form"));

		verify(bookService, never()).save(any());
	}

	@Test
	public void createWithValidDataRedirectsToList() throws Exception {
		Book saved = new Book(1L, "Effective Java", "9780134685991", 2L, 2018);
		when(bookService.save(any(Book.class))).thenReturn(saved);

		mockMvc.perform(post("/books")
						.param("title", "Effective Java")
						.param("isbn", "9780134685991")
						.param("authorId", "2")
						.param("publishedYear", "2018"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/books"))
				.andExpect(flash().attribute("flashMessage", "書籍新增成功"));

		verify(bookService).saveCategories(eq(1L), any());
	}

	@Test
	public void listWithoutKeywordSearchesWithNull() throws Exception {
		PageRes<BookSummary> emptyPage = new PageRes<>(Collections.emptyList(), 0, 10, 0);
		when(bookService.search(eq(null), any(PageReq.class))).thenReturn(emptyPage);

		mockMvc.perform(get("/books"))
				.andExpect(status().isOk())
				.andExpect(view().name("books/list"))
				.andExpect(model().attribute("books", Collections.emptyList()));

		verify(bookService).search(isNull(), any(PageReq.class));
	}

	@Test
	public void listWithKeywordSearchesAndKeepsKeywordInModel() throws Exception {
		PageRes<BookSummary> emptyPage = new PageRes<>(Collections.emptyList(), 0, 10, 0);
		when(bookService.search(eq("Java"), any(PageReq.class))).thenReturn(emptyPage);

		mockMvc.perform(get("/books").param("keyword", "Java"))
				.andExpect(status().isOk())
				.andExpect(view().name("books/list"))
				.andExpect(model().attribute("keyword", "Java"));

		verify(bookService).search(eq("Java"), any(PageReq.class));
	}
}

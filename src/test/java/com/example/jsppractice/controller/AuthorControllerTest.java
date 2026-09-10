package com.example.jsppractice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.example.jsppractice.model.Author;
import com.example.jsppractice.model.PageReq;
import com.example.jsppractice.model.PageRes;
import com.example.jsppractice.service.AuthorService;

@RunWith(MockitoJUnitRunner.class)
public class AuthorControllerTest {
	@Mock
	private AuthorService authorService;

	@InjectMocks
	private AuthorController authorController;

	private MockMvc mockMvc;

	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.standaloneSetup(authorController).build();
		when(authorService.findAll(any(PageReq.class)))
				.thenReturn(new PageRes<>(Collections.emptyList(), 0, 10, 0));
	}

	@Test
	public void testList() throws Exception {
		mockMvc.perform(get("/authors")).andExpect(view().name("authors/list"))
				.andExpect(model().attribute("authors", Collections.emptyList()));
	}

	@Test
	public void testNewForm() throws Exception {
		mockMvc.perform(get("/authors/new")).andExpect(view().name("authors/form"))
				.andExpect(model().attributeExists("author"));
	}

	@Test
	public void testCreateWithValidParams() throws Exception {
		mockMvc.perform(post("/authors").param("name", "Alex")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/authors"));
	}

	@Test
	public void testCreateWithBlankParams() throws Exception {
		mockMvc.perform(post("/authors").param("name", "")).andExpect(status().isOk())
				.andExpect(view().name("authors/form"));

		verify(authorService, never()).save(any());
	}

	@Test
	public void testDelete() throws Exception {
		mockMvc.perform(post("/authors/1/delete")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/authors"));

		verify(authorService).deleteById(1L);
	}

	@Test
	public void testEditForm() throws Exception {
		Author author = new Author(1L, "Alex");
		when(authorService.findById(1L)).thenReturn(author);

		mockMvc.perform(get("/authors/1/edit"))
				.andExpect(view().name("authors/form"))
				.andExpect(model().attribute("author", author));
	}

	@Test
	public void testUpdateWithValidParams() throws Exception {
		mockMvc.perform(post("/authors/1").param("name", "Alex Updated"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/authors"));

		verify(authorService).save(argThat(a -> a.getId().equals(1L) && a.getName().equals("Alex Updated")));
	}

	@Test
	public void testUpdateWithBlankParamsReturnsToFormWithoutSaving() throws Exception {
		mockMvc.perform(post("/authors/1").param("name", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("authors/form"));

		verify(authorService, never()).save(any());
	}

}

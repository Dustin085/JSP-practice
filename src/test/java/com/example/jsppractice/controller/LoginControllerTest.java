package com.example.jsppractice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LoginControllerTest {

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new LoginController()).build();
	}

	@Test
	public void testLoginForm() throws Exception {
		mockMvc.perform(get("/login")).andExpect(view().name("login/form"));
	}
}

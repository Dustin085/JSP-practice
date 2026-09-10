package com.example.jsppractice.controller;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LogoutControllerTest {

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new LogoutController()).build();
	}

	@Test
	public void testLogoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("currentUser", "someUser");

		mockMvc.perform(post("/logout").session(session)).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		assertTrue(session.isInvalid());
	}
}

package com.example.jsppractice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.exception.EmailAlreadyExistsException;
import com.example.jsppractice.service.AuthService;

@RunWith(MockitoJUnitRunner.class)
public class RegisterControllerTest {

	@Mock
	private AuthService authService;

	@InjectMocks
	private RegisterController registerController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(registerController).build();
	}

	@Test
	public void testRegisterForm() throws Exception {
		mockMvc.perform(get("/register")).andExpect(view().name("register/form"));
	}

	@Test
	public void testRegisterSuccessRedirectsToLoginWithFlashMessage() throws Exception {
		mockMvc.perform(post("/register").param("email", "new@example.com").param("name", "New User")
				.param("password", "password123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"))
				.andExpect(flash().attribute("flashMessage", "註冊成功，請登入"));
	}

	@Test
	public void testRegisterWithExistingEmailRedirectsBackWithFlashMessage() throws Exception {
		when(authService.register("taken@example.com", "Someone", "password123"))
				.thenThrow(new EmailAlreadyExistsException("taken@example.com"));

		mockMvc.perform(post("/register").param("email", "taken@example.com").param("name", "Someone")
				.param("password", "password123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/register"))
				.andExpect(flash().attributeExists("flashMessage"));
	}
}

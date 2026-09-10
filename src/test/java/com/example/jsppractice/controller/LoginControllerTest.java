package com.example.jsppractice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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

import com.example.jsppractice.exception.InvalidCredentialsException;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.AuthService;

@RunWith(MockitoJUnitRunner.class)
public class LoginControllerTest {

	@Mock
	private AuthService authService;

	@InjectMocks
	private LoginController loginController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(loginController).build();
	}

	@Test
	public void testLoginForm() throws Exception {
		mockMvc.perform(get("/login")).andExpect(view().name("login/form"));
	}

	@Test
	public void testLoginSuccessSetsSessionAndRedirects() throws Exception {
		User user = User.builder().id(1L).email("alex@example.com").name("Alex").role(RoleType.USER).build();
		when(authService.login("alex@example.com", "password123")).thenReturn(user);

		mockMvc.perform(post("/login").param("email", "alex@example.com").param("password", "password123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andExpect(request().sessionAttribute("currentUser", user));
	}

	@Test
	public void testLoginFailureRedirectsWithFlashMessage() throws Exception {
		when(authService.login("alex@example.com", "wrong-password"))
				.thenThrow(new InvalidCredentialsException("帳號或密碼錯誤"));

		mockMvc.perform(post("/login").param("email", "alex@example.com").param("password", "wrong-password"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"))
				.andExpect(flash().attribute("flashMessage", "帳號或密碼錯誤"));
	}
}

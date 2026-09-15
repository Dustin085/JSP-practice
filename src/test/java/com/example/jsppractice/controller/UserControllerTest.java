package com.example.jsppractice.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.model.User;
import com.example.jsppractice.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController userController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
	}

	@Test
	public void listShowsAllUsersWithMaskedEmail() throws Exception {
		User admin = User.builder().id(1L).name("管理員").email("admin@example.com").build();
		when(userService.findAll()).thenReturn(List.of(admin));

		mockMvc.perform(get("/users"))
				.andExpect(status().isOk())
				.andExpect(view().name("users/list"))
				.andExpect(model().attribute("users", List.of(admin)));

		verify(userService).findAll();
	}
}

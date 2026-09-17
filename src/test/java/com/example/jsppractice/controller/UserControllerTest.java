package com.example.jsppractice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.exception.BaselineRoleCannotBeRevokedException;
import com.example.jsppractice.exception.LastRoleCannotBeRemovedException;
import com.example.jsppractice.exception.SelfAdminRevocationNotAllowedException;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;
import com.example.jsppractice.security.CustomUserDetails;
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

	private RequestPostProcessor asAdmin(Long id) {
		User admin = User.builder().id(id).email("admin@example.com").build();
		Authentication authentication = new UsernamePasswordAuthenticationToken(new CustomUserDetails(admin), null);
		return request -> {
			request.setUserPrincipal(authentication);
			return request;
		};
	}

	@Test
	public void listShowsAllUsersAndRolesExcludingUser() throws Exception {
		User admin = User.builder().id(1L).name("管理員").email("admin@example.com").build();
		when(userService.findAll()).thenReturn(List.of(admin));

		mockMvc.perform(get("/users"))
				.andExpect(status().isOk())
				.andExpect(view().name("users/list"))
				.andExpect(model().attribute("users", List.of(admin)))
				.andExpect(model().attribute("manageableRoles", List.of(RoleType.ADMIN, RoleType.PROCUREMENT)));

		verify(userService).findAll();
	}

	@Test
	public void grantRoleRedirectsToListWithSuccessMessage() throws Exception {
		mockMvc.perform(post("/users/roles/grant").with(asAdmin(1L)).param("userId", "5")
						.param("role", "PROCUREMENT"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/users"))
				.andExpect(flash().attribute("flashMessage", "已新增角色：PROCUREMENT"));

		verify(userService).grantRole(eq(5L), eq(RoleType.PROCUREMENT), any(User.class));
	}

	@Test
	public void grantRoleShowsErrorMessageWhenAlreadyGranted() throws Exception {
		doThrow(new IllegalStateException("這個帳號已經有這個角色了")).when(userService).grantRole(eq(5L),
				eq(RoleType.PROCUREMENT), any(User.class));

		mockMvc.perform(post("/users/roles/grant").with(asAdmin(1L)).param("userId", "5")
						.param("role", "PROCUREMENT"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/users"))
				.andExpect(flash().attribute("flashMessage", "這個帳號已經有這個角色了"));
	}

	@Test
	public void revokeRoleRedirectsToListWithSuccessMessage() throws Exception {
		mockMvc.perform(post("/users/roles/revoke").with(asAdmin(1L)).param("userId", "5")
						.param("role", "PROCUREMENT"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/users"))
				.andExpect(flash().attribute("flashMessage", "已移除角色：PROCUREMENT"));

		verify(userService).revokeRole(eq(5L), eq(RoleType.PROCUREMENT), any(User.class));
	}

	@Test
	public void revokeRoleShowsErrorMessageWhenItWouldRemoveLastRole() throws Exception {
		doThrow(new LastRoleCannotBeRemovedException("每個帳號至少要保留一個角色")).when(userService).revokeRole(eq(5L),
				eq(RoleType.PROCUREMENT), any(User.class));

		mockMvc.perform(post("/users/roles/revoke").with(asAdmin(1L)).param("userId", "5")
						.param("role", "PROCUREMENT"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashMessage", "每個帳號至少要保留一個角色"));
	}

	@Test
	public void revokeRoleShowsErrorMessageWhenRevokingOwnAdminRole() throws Exception {
		doThrow(new SelfAdminRevocationNotAllowedException("不能移除自己的 ADMIN 角色，請改由其他管理員操作")).when(userService)
				.revokeRole(eq(1L), eq(RoleType.ADMIN), any(User.class));

		mockMvc.perform(post("/users/roles/revoke").with(asAdmin(1L)).param("userId", "1").param("role", "ADMIN"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashMessage", "不能移除自己的 ADMIN 角色，請改由其他管理員操作"));
	}

	@Test
	public void revokeRoleShowsErrorMessageWhenRevokingBaselineUserRole() throws Exception {
		doThrow(new BaselineRoleCannotBeRevokedException("USER 是每個帳號都必須有的基礎角色，不能被移除")).when(userService)
				.revokeRole(eq(5L), eq(RoleType.USER), any(User.class));

		mockMvc.perform(post("/users/roles/revoke").with(asAdmin(1L)).param("userId", "5").param("role", "USER"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashMessage", "USER 是每個帳號都必須有的基礎角色，不能被移除"));
	}
}

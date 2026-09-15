package com.example.jsppractice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.jsppractice.exception.EmailAlreadyExistsException;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceImplTest {

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserService userService;

	@InjectMocks
	private AuthServiceImpl authService;

	@Test
	public void registerSavesNewUserWithHashedPasswordAndUserRole() {
		when(userMapper.findByEmail("new@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("raw-password")).thenReturn("hashed-password");
		when(userService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register("new@example.com", "New User", "raw-password");

		verify(userService).save(argThat(u -> u.getEmail().equals("new@example.com") && u.getName().equals("New User")
				&& u.getPasswordHash().equals("hashed-password") && u.getRoles().equals(Set.of(RoleType.USER))));
	}

	@Test(expected = EmailAlreadyExistsException.class)
	public void registerThrowsWhenEmailAlreadyExists() {
		User existing = User.builder().id(1L).email("taken@example.com").roles(Set.of(RoleType.USER)).build();
		when(userMapper.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

		authService.register("taken@example.com", "Someone", "raw-password");

		verify(userService, never()).save(any());
	}
}

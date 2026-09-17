package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.jsppractice.crypto.EmailLookupHasher;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	public void findAllDelegatesToMapper() {
		User alex = User.builder().id(1L).email("alex@example.com").name("Alex").build();
		when(userMapper.findAll()).thenReturn(List.of(alex));

		List<User> users = userService.findAll();

		assertEquals(List.of(alex), users);
	}

	// findByEmail 不能直接拿明文去問 mapper——email 欄位在 DB 裡是密文，mapper 只認得
	// 盲索引雜湊，所以這裡驗證的是「有沒有先算雜湊、拿雜湊去查」，而不是拿明文去查
	@Test
	public void findByEmailQueriesMapperByHashNotPlainEmail() {
		String email = "admin@example.com";
		String expectedHash = EmailLookupHasher.hash(email);
		User admin = User.builder().id(1L).email(email).name("管理員").build();
		when(userMapper.findByEmailHash(expectedHash)).thenReturn(Optional.of(admin));
		when(userMapper.findRolesByUserId(1L)).thenReturn(Set.of());

		Optional<User> found = userService.findByEmail(email);

		assertEquals(admin, found.get());
		verify(userMapper).findByEmailHash(eq(expectedHash));
	}

	@Test
	public void findByEmailReturnsEmptyWhenHashNotFound() {
		when(userMapper.findByEmailHash(EmailLookupHasher.hash("nobody@example.com"))).thenReturn(Optional.empty());

		Optional<User> found = userService.findByEmail("nobody@example.com");

		assertFalse(found.isPresent());
	}
}

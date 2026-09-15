package com.example.jsppractice.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
}

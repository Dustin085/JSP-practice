package com.example.jsppractice.service;

import org.springframework.stereotype.Service;

import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.User;

@Service
public class UserServiceImpl implements UserService {
	private UserMapper userMapper;

	public UserServiceImpl(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	public User save(User user) {
		if (user.getId() == null) {
			userMapper.insert(user);
			return user;
		}
		userMapper.update(user);
		return user;
	}

}

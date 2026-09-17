package com.example.jsppractice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jsppractice.crypto.EmailLookupHasher;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.User;

@Service
public class UserServiceImpl implements UserService {
	private UserMapper userMapper;

	public UserServiceImpl(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	@Transactional
	public User save(User user) {
		if (user.getId() == null) {
			userMapper.insert(user, EmailLookupHasher.hash(user.getEmail()));
		} else {
			// email 目前沒有編輯功能，update() 也不會動 email 欄位，不需要重算/更新 email_lookup_hash
			userMapper.update(user);
			userMapper.deleteRolesByUserId(user.getId());
		}
		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			userMapper.insertUserRoles(user.getId(), user.getRoles());
		}
		return user;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return userMapper.findByEmailHash(EmailLookupHasher.hash(email)).map(user -> {
			user.setRoles(userMapper.findRolesByUserId(user.getId()));
			return user;
		});
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userMapper.findAll();
	}

}

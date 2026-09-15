package com.example.jsppractice.service;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.jsppractice.exception.EmailAlreadyExistsException;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@Service
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;

	public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, UserService userService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
	}

	@Override
	public User register(String email, String name, String password) {
		Optional<User> existingUser = userMapper.findByEmail(email);
		if (existingUser.isPresent()) {
			throw new EmailAlreadyExistsException(email);
		}
		String passwordHash = passwordEncoder.encode(password);
		User newUser = User.builder().email(email).name(name).passwordHash(passwordHash)
				.roles(Set.of(RoleType.USER)).build();
		User saved = userService.save(newUser);
		log.info("新使用者註冊：userId={}, email={}", saved.getId(), email);
		return saved;
	}
}

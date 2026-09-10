package com.example.jsppractice.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.jsppractice.exception.EmailAlreadyExistsException;
import com.example.jsppractice.exception.InvalidCredentialsException;
import com.example.jsppractice.mapper.UserMapper;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@Service
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	private static String LOGIN_ERROR_MESSAGE = "帳號或密碼錯誤";

	public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, UserService userService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
	}

	@Override
	public User login(String email, String rawPassword) {
		Optional<User> user = userMapper.findByEmail(email);
		if (user.isEmpty()) {
			log.warn("登入失敗，帳號不存在：email={}", email);
			throw new InvalidCredentialsException(LOGIN_ERROR_MESSAGE);
		}
		if (!passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
			log.warn("登入失敗，密碼錯誤：email={}", email);
			throw new InvalidCredentialsException(LOGIN_ERROR_MESSAGE);
		}
		log.info("登入成功：userId={}, email={}", user.get().getId(), email);
		return user.get();
	}

	@Override
	public User register(String email, String name, String password) {
		Optional<User> existingUser = userMapper.findByEmail(email);
		if (existingUser.isPresent()) {
			throw new EmailAlreadyExistsException(email);
		}
		String passwordHash = passwordEncoder.encode(password);
		User newUser = User.builder().email(email).name(name).passwordHash(passwordHash).role(RoleType.USER).build();
		User saved = userService.save(newUser);
		log.info("新使用者註冊：userId={}, email={}", saved.getId(), email);
		return saved;
	}
}

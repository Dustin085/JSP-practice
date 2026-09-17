package com.example.jsppractice.service;

import java.util.List;
import java.util.Optional;

import com.example.jsppractice.model.User;

public interface UserService {
	User save(User user);

	Optional<User> findByEmail(String email);

	List<User> findAll();
}

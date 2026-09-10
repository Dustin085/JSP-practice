package com.example.jsppractice.mapper;

import java.util.Optional;

import com.example.jsppractice.model.User;

public interface UserMapper {
	Optional<User> findByEmail(String email);

	void insert(User user);

	void update(User user);
}

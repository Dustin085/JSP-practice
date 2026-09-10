package com.example.jsppractice.service;

import com.example.jsppractice.model.User;

public interface AuthService {
	User login(String email, String rawPassword);

	User register(String email, String name, String password);
}

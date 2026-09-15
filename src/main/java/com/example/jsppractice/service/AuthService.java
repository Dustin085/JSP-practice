package com.example.jsppractice.service;

import com.example.jsppractice.model.User;

public interface AuthService {
	User register(String email, String name, String password);
}

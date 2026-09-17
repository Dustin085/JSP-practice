package com.example.jsppractice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// POST /login 已經交給 Spring Security 的 UsernamePasswordAuthenticationFilter 處理
// （見 SecurityConfig 的 formLogin().loginPage("/login")），這個 class 現在只負責顯示登入頁。
@Controller
@RequestMapping("/login")
public class LoginController {

	@GetMapping
	public String loginForm() {
		return "login/form";
	}
}

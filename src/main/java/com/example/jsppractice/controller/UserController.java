package com.example.jsppractice.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.jsppractice.model.User;
import com.example.jsppractice.service.UserService;

@Controller
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public String list(Model model) {
		List<User> users = userService.findAll();
		model.addAttribute("users", users);
		return "users/list";
	}
}

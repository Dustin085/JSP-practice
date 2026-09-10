package com.example.jsppractice.controller;

import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.InvalidCredentialsException;
import com.example.jsppractice.model.User;
import com.example.jsppractice.service.AuthService;

@Controller
@RequestMapping("/login")
public class LoginController {
	private final AuthService authService;

	public LoginController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping
	public String loginForm() {
		return "login/form";
	}

	@PostMapping
	public String login(@RequestParam() String email, @RequestParam() String password, HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			User user = authService.login(email, password);
			session.setAttribute("currentUser", user);
			session.setAttribute("csrfToken", UUID.randomUUID().toString());
			return "redirect:/";
		} catch (InvalidCredentialsException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
			return "redirect:/login";
		}
	}
}

package com.example.jsppractice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.EmailAlreadyExistsException;
import com.example.jsppractice.service.AuthService;

@Controller
@RequestMapping("/register")
public class RegisterController {
	private final AuthService authService;

	public RegisterController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping
	public String registerForm() {
		return "register/form";
	}

	@PostMapping
	public String register(@RequestParam String email, @RequestParam String name, @RequestParam String password,
			RedirectAttributes redirectAttributes) {
		try {
			authService.register(email, name, password);
			redirectAttributes.addFlashAttribute("flashMessage", "註冊成功，請登入");
			return "redirect:/login";
		} catch (EmailAlreadyExistsException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
			return "redirect:/register";
		}
	}
}

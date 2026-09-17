package com.example.jsppractice.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jsppractice.exception.BaselineRoleCannotBeRevokedException;
import com.example.jsppractice.exception.LastRoleCannotBeRemovedException;
import com.example.jsppractice.exception.SelfAdminRevocationNotAllowedException;
import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;
import com.example.jsppractice.security.CustomUserDetails;
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
		// USER 不給畫面選：每個帳號一定有、不能手動加也不能移除，讓下拉選單只列真的能管理的角色
		model.addAttribute("manageableRoles",
				Arrays.stream(RoleType.values()).filter(role -> role != RoleType.USER).toList());
		return "users/list";
	}

	@PostMapping("/roles/grant")
	public String grantRole(@RequestParam Long userId, @RequestParam RoleType role, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		User currentUser = CustomUserDetails.currentUser(authentication);
		try {
			userService.grantRole(userId, role, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "已新增角色：" + role);
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/users";
	}

	@PostMapping("/roles/revoke")
	public String revokeRole(@RequestParam Long userId, @RequestParam RoleType role, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		User currentUser = CustomUserDetails.currentUser(authentication);
		try {
			userService.revokeRole(userId, role, currentUser);
			redirectAttributes.addFlashAttribute("flashMessage", "已移除角色：" + role);
		} catch (IllegalStateException | LastRoleCannotBeRemovedException | SelfAdminRevocationNotAllowedException
				| BaselineRoleCannotBeRevokedException e) {
			redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
		}
		return "redirect:/users";
	}
}

package com.example.jsppractice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.jsppractice.security.CustomUserDetails;

// 取代原本登入時寫進 session 的 currentUser：每個 controller 方法執行前都會先跑這個，
// 把目前登入者放進 Model，JSP 用 ${currentUser} 就能讀到（EL 找不到 request/page scope
// 的變數時才會找 session/application scope，所以不用加 requestScope. 前綴）。
//
// 這裡涵蓋所有頁面，包含 /login、/register 這種匿名也能進的頁面，所以不能假設一定是
// 已登入使用者——principal 可能是 Security 預設的匿名 "anonymousUser" 字串，不是
// CustomUserDetails，用 instanceof 檢查過再轉型，不能直接 cast（那樣會在匿名頁面噴
// ClassCastException）。
@ControllerAdvice(basePackages = "com.example.jsppractice.controller")
public class CurrentUserModelAdvice {

	@ModelAttribute
	public void addCurrentUser(Authentication authentication, Model model) {
		if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
			model.addAttribute("currentUser", principal.getUser());
		}
	}
}

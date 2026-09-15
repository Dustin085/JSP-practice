package com.example.jsppractice.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.jsppractice.model.User;

// 包住現有的 User model，讓 Controller 之後能從 Authentication principal 直接拿到
// userId/name/role，不用只靠 Spring Security 內建的 User（只有 username/password/authorities）。
public class CustomUserDetails implements UserDetails {

	private final User user;

	public CustomUserDetails(User user) {
		this.user = user;
	}

	// 只給「一定是已登入使用者」的路徑用（Security 的 authorizeHttpRequests 已經擋掉匿名請求），
	// principal 這裡保證是 CustomUserDetails，不會是 Security 預設的匿名 "anonymousUser" 字串，
	// 所以直接 cast 是安全的。匿名也可能存取的頁面不要用這個，改用 CurrentUserModelAdvice 那種
	// instanceof 檢查過的寫法。
	public static User currentUser(Authentication authentication) {
		return ((CustomUserDetails) authentication.getPrincipal()).getUser();
	}

	public Long getId() {
		return user.getId();
	}

	public User getUser() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}

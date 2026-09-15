package com.example.jsppractice.model;

import java.util.HashSet;
import java.util.Set;

import com.example.jsppractice.util.EmailMasker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	private Long id;

	private String email;

	private String name;

	private String passwordHash;

	@Builder.Default
	private Set<RoleType> roles = new HashSet<>();

	// EL 沒辦法直接對 Set<RoleType> 做 contains('ADMIN') 型別比對（字串跟 enum 不會相等），
	// 供 JSP 用的判斷統一走這個方法：${currentUser.hasRole('ADMIN')}
	public boolean hasRole(String roleName) {
		return roles.stream().anyMatch(r -> r.name().equals(roleName));
	}

	// 使用者列表頁用：${user.maskedEmail}
	public String getMaskedEmail() {
		return EmailMasker.mask(email);
	}
}

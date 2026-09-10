package com.example.jsppractice.interceptor;

import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import com.example.jsppractice.model.RoleType;

/**
 * 一條「路徑 + HTTP method 對應允許角色」的規則。
 * httpMethod 為 null 代表：不限 method。allowedRoles 為 null 代表：只要有登入就好，不限角色。
 */
class RoleAccessRule {
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final HttpMethod httpMethod;
	private final String pathPattern;
	private final Set<RoleType> allowedRoles;

	RoleAccessRule(HttpMethod httpMethod, String pathPattern, Set<RoleType> allowedRoles) {
		this.httpMethod = httpMethod;
		this.pathPattern = pathPattern;
		this.allowedRoles = allowedRoles;
	}

	boolean matches(String method, String path) {
		boolean methodMatches = httpMethod == null || httpMethod == HttpMethod.resolve(method);
		return methodMatches && PATH_MATCHER.match(pathPattern, path);
	}

	boolean isAllowed(RoleType role) {
		return allowedRoles == null || allowedRoles.contains(role);
	}
}

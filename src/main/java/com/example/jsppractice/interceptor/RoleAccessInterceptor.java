package com.example.jsppractice.interceptor;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

/**
 * 必須註冊在 LoginCheckInterceptor 之後執行，
 * 這樣進到這裡時 session 裡一定已經有 currentUser，不用再判斷 null。
 *
 * 規則由上而下比對，第一條「method + 路徑」都匹配的規則生效；
 * 完全沒有規則匹配的路徑，預設開放（只要求登入，不限角色）。
 */
public class RoleAccessInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(RoleAccessInterceptor.class);

	private static final List<RoleAccessRule> RULES = List.of(
			// 明確開放的例外，要寫在下面 /books/**、/authors/**、/categories/** 萬用規則之前
			new RoleAccessRule(HttpMethod.GET, "/books", null),
			new RoleAccessRule(HttpMethod.GET, "/books/export", null),
			new RoleAccessRule(HttpMethod.GET, "/authors", null),
			new RoleAccessRule(HttpMethod.GET, "/categories", null),

			// 限制特定角色的動作
			new RoleAccessRule(HttpMethod.POST, "/requests/*/approve", Set.of(RoleType.ADMIN)),
			new RoleAccessRule(HttpMethod.POST, "/requests/*/reject", Set.of(RoleType.ADMIN)),
			new RoleAccessRule(HttpMethod.POST, "/procurement/*/complete", Set.of(RoleType.PROCUREMENT)),

			// 這三個資源，其餘操作（新增/編輯/刪除）預設限 ADMIN
			new RoleAccessRule(null, "/books/**", Set.of(RoleType.ADMIN)),
			new RoleAccessRule(null, "/authors/**", Set.of(RoleType.ADMIN)),
			new RoleAccessRule(null, "/categories/**", Set.of(RoleType.ADMIN)),

			// 稽核紀錄僅限 ADMIN 查看
			new RoleAccessRule(null, "/audit-logs/**", Set.of(RoleType.ADMIN)),

			// 對帳僅限 ADMIN 查看/觸發
			new RoleAccessRule(null, "/reconciliations/**", Set.of(RoleType.ADMIN)));

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		String method = request.getMethod();

		for (RoleAccessRule rule : RULES) {
			if (!rule.matches(method, path)) {
				continue;
			}
			User currentUser = (User) request.getSession().getAttribute("currentUser");
			if (rule.isAllowed(currentUser.getRole())) {
				return true;
			}
			log.warn("權限不足被擋下：userId={}, role={}, {} {}", currentUser.getId(), currentUser.getRole(), method, path);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "權限不足");
			return false;
		}
		return true;
	}
}

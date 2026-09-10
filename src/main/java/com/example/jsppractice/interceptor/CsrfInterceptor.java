package com.example.jsppractice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Synchronizer Token Pattern：token 在登入當下產生一次，存進 session，
 * 整個 session 共用同一個值（不像冪等性 key 需要用一次就作廢）。
 * 只檢查會修改資料的 POST 請求，GET 不檢查。
 *
 * 必須註冊在 LoginCheckInterceptor 之後執行，這樣進到這裡時 session 一定存在。
 */
public class CsrfInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(CsrfInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		HttpSession session = request.getSession(false);
		String sessionToken = session == null ? null : (String) session.getAttribute("csrfToken");
		String requestToken = request.getParameter("csrfToken");

		if (sessionToken == null || !sessionToken.equals(requestToken)) {
			log.warn("CSRF token 驗證失敗：{} {}", request.getMethod(), request.getRequestURI());
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token 驗證失敗");
			return false;
		}
		return true;
	}
}

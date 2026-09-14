package com.example.jsppractice.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

// 過渡期橋接：LoginCheckInterceptor/RoleAccessInterceptor/所有 JSP 的 sessionScope.currentUser
// 目前都還是直接讀 session 的 currentUser，還沒有改成從 SecurityContext 讀。
// Spring Security 的 formLogin 認證成功後不會知道要設這個 attribute，
// 這裡在認證成功當下手動補上，讓舊機制在換裝期間繼續正常運作。
// CSRF 那份 session attribute 已經不需要了（Day 3 起改用 Security 自己的 CsrfFilter/_csrf）。
// 等 Day 4 把上述地方全部改用 Authentication 讀取後，這個 class 就可以整個刪掉。
// 沒有依賴需要注入，跟其他攔截器一樣直接在 SecurityConfig 手動 new，不用另外納入 @ComponentScan。
public class LegacySessionBridgeAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
		HttpSession session = request.getSession();
		session.setAttribute("currentUser", principal.getUser());
		super.onAuthenticationSuccess(request, response, authentication);
	}
}

package com.example.jsppractice.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

// 刻意保留的橋接層（不是暫時性的）：所有 JSP 的 sessionScope.currentUser 直接讀 session 的
// currentUser，沒有改成從 SecurityContext/Authentication 讀。Spring Security 的 formLogin
// 認證成功後不會知道要設這個 attribute，這裡在認證成功當下手動補上。
// 這個 attribute 只在登入當下寫一次、整個 session 有效不會過期，跟 Authentication 的 principal
// 一樣是登入時的快照、不會中途更新，兩種做法在正確性上沒有差異——純粹是風格選擇，
// 保留這層是為了不用把全部 JSP + 好幾個 Controller 都改成注入 Authentication/@AuthenticationPrincipal。
// 沒有依賴需要注入，直接在 SecurityConfig 手動 new，不用另外納入 @ComponentScan。
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

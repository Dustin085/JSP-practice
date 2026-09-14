package com.example.jsppractice.config;

import javax.servlet.ServletContext;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;

public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

	// CharacterEncodingFilter 一定要在 springSecurityFilterChain 之前跑：CsrfFilter 會呼叫
	// request.getParameter(...) 讀 CSRF token 來驗證，Servlet 的 request 參數一旦被讀取過一次，
	// 編碼就定案了、之後任何 setCharacterEncoding() 都沒用——如果 CsrfFilter 先讀到，
	// 表單裡其他欄位（例如書名這種中文字）就會用錯的編碼被解析，變成亂碼。
	// insertFilters() 是 AbstractSecurityWebApplicationInitializer 專門開的 hook，
	// 用跟 springSecurityFilterChain 同一種 url-pattern（/*）掛 filter，
	// 這樣才能真正保證它排在 springSecurityFilterChain 前面（見 WebAppInitializer 的註解，
	// 用 servlet-name 掛的 filter 沒辦法透過 @Order 排到 url-pattern 掛的 filter 前面）。
	@Override
	protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
		encodingFilter.setEncoding("UTF-8");
		encodingFilter.setForceEncoding(true);
		insertFilters(servletContext, encodingFilter);
	}
}

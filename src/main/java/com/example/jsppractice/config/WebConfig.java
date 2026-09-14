package com.example.jsppractice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.jsppractice.interceptor.CsrfInterceptor;
import com.example.jsppractice.interceptor.LoginCheckInterceptor;
import com.example.jsppractice.interceptor.RoleAccessInterceptor;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.example.jsppractice.controller")
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.jsp("/WEB-INF/views/", ".jsp");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginCheckInterceptor()).addPathPatterns("/books/**", "/authors/**",
				"/categories/**", "/requests/**", "/procurement/**", "/audit-logs/**", "/reconciliations/**");
		registry.addInterceptor(new CsrfInterceptor()).addPathPatterns("/books/**", "/authors/**", "/categories/**",
				"/requests/**", "/procurement/**", "/audit-logs/**", "/reconciliations/**");
		// Day 2：暫時關掉，改用 SecurityConfig 的 authorizeRequests() 規則做角色檢查，
		// 驗證通過、規則對得上之後，Day 4 收尾時把這行跟整個 RoleAccessInterceptor/RoleAccessRule 一起刪掉
		// registry.addInterceptor(new RoleAccessInterceptor()).addPathPatterns("/books/**", "/authors/**",
		// 		"/categories/**", "/requests/**", "/procurement/**", "/audit-logs/**", "/reconciliations/**");
	}
}

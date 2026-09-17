package com.example.jsppractice.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		// 一定要在 super.onStartup() 之後才註冊,確保 ContextLoaderListener 先掛上去、
		// 排在它後面觸發 contextInitialized(),root context 才會已經建好。
		// 只有真正部署到 Servlet 容器時才會跑到這裡,任何測試都不會觸發,不用擔心汙染測試資料。
		servletContext.addListener(SeedDataListener.class);
	}

	@Override
	protected Class<?>[] getRootConfigClasses() {
		// SecurityConfig 一定要放 root context，不是 servlet context：
		// DelegatingFilterProxy 預設只會去 root WebApplicationContext 找 springSecurityFilterChain 這個 bean
		return new Class<?>[] { RootConfig.class, SecurityConfig.class, SftpConfig.class };
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { WebConfig.class };
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	// CharacterEncodingFilter 不在這裡註冊：AbstractAnnotationConfigDispatcherServletInitializer.getServletFilters()
	// 是用 servlet-name 掛的 filter mapping，Servlet 規範規定 url-pattern 掛的 filter（Spring Security 的
	// springSecurityFilterChain，掛在 /*）一定會先跑過，不管 @Order 或註冊順序怎麼設都改變不了這個順序。
	// 拿掉這個 override 之後 CharacterEncodingFilter 改在 SecurityWebApplicationInitializer 註冊，
	// 兩者都是 url-pattern 掛的，@Order/註冊順序才真的管得到誰先跑。見那個 class 的註解。
}

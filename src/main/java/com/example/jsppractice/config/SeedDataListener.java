package com.example.jsppractice.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.example.jsppractice.service.AuthService;
import com.example.jsppractice.service.AuthorService;
import com.example.jsppractice.service.BookRequestService;
import com.example.jsppractice.service.BookService;
import com.example.jsppractice.service.CategoryService;
import com.example.jsppractice.service.UserService;

/**
 * 註冊時機一定要在 ContextLoaderListener 之後(見 WebAppInitializer),
 * 這樣 contextInitialized() 觸發時 root context 才保證已經建立完成。
 */
public class SeedDataListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext servletContext = sce.getServletContext();
		WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		new DataSeeder(context.getBean(AuthorService.class), context.getBean(CategoryService.class),
				context.getBean(BookService.class), context.getBean(AuthService.class),
				context.getBean(UserService.class), context.getBean(PasswordEncoder.class),
				context.getBean(BookRequestService.class)).seed();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}

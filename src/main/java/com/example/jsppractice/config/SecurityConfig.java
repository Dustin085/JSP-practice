package com.example.jsppractice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.jsppractice.security.LegacySessionBridgeAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	public SecurityConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests()
				// 刻意維持 .antMatchers(...)（雖然標了 @Deprecated），不要換成非棄用的 .requestMatchers(String...)：
				// 後者在偵測到 Spring MVC 存在時，預設會走 MvcRequestMatcher，需要 mvcHandlerMappingIntrospector
				// 這個 bean（由 @EnableWebMvc 註冊）在「同一個」ApplicationContext 裡才能用。但這個專案的
				// SecurityConfig 刻意放在 root context（DelegatingFilterProxy 找 bean 的地方），
				// @EnableWebMvc 卻是在 WebConfig、屬於 servlet 的 child context，兩者本來就不共用，
				// 跑起來會直接噴 NoSuchBeanDefinitionException。.antMatchers(...) 只走純 AntPathRequestMatcher，
				// 不需要這個 bean，剛好也是舊 RoleAccessRule 用的同一套 AntPathMatcher 語意，翻譯起來更忠實。
				// 舊系統的三個攔截器本來就沒註冊在這些路徑上，明確列出來維持原本不受限的行為
				.antMatchers("/", "/login", "/login/**", "/register", "/register/**", "/logout", "/health")
				.permitAll()

				// ── books/authors/categories：GET 列表頁登入即可、不限角色，要排在下面的萬用規則之前
				.antMatchers(HttpMethod.GET, "/books")
				.authenticated()
				.antMatchers(HttpMethod.GET, "/books/export")
				.authenticated()
				.antMatchers(HttpMethod.GET, "/authors")
				.authenticated()
				.antMatchers(HttpMethod.GET, "/categories")
				.authenticated()
				.antMatchers("/books/**")
				.hasRole("ADMIN")
				.antMatchers("/authors/**")
				.hasRole("ADMIN")
				.antMatchers("/categories/**")
				.hasRole("ADMIN")

				// ── requests：核准/駁回限 ADMIN，其餘登入即可
				.antMatchers(HttpMethod.POST, "/requests/*/approve")
				.hasRole("ADMIN")
				.antMatchers(HttpMethod.POST, "/requests/*/reject")
				.hasRole("ADMIN")
				.antMatchers("/requests/**")
				.authenticated()

				// ── procurement：完成採購限 PROCUREMENT，其餘登入即可
				.antMatchers(HttpMethod.POST, "/procurement/*/complete")
				.hasRole("PROCUREMENT")
				.antMatchers("/procurement/**")
				.authenticated()

				// ── 稽核紀錄、對帳：全部限 ADMIN
				.antMatchers("/audit-logs/**")
				.hasRole("ADMIN")
				.antMatchers("/reconciliations/**")
				.hasRole("ADMIN")

				// 收尾用 authenticated() 而不是 permitAll()：以後不管漏加哪條規則，
				// 新路徑預設「至少要登入」而不是「預設任何人都能看」
				.anyRequest()
				.authenticated()
				.and()
				.formLogin()
				.loginPage("/login") // 沿用現有的 GET /login 頁面，不用 Security 內建的表單
				.usernameParameter("email") // 表單欄位叫 email，不是預設的 username
				.passwordParameter("password")
				// 用這個而不是 defaultSuccessUrl：認證成功後要補上舊系統還在用的 session currentUser，見該 class 上的註解
				.successHandler(new LegacySessionBridgeAuthenticationSuccessHandler())
				.permitAll()
				.and()
				// Day 3：CSRF 改用 Security 自己的保護（預設 HttpSessionCsrfTokenRepository，
				// 整個 session 共用同一個值，語意跟舊的 CsrfInterceptor 一樣），不再手動 disable
				.logout()
				.disable(); // 先關掉：Security 預設 logout 也是攔 POST /logout，
							// 會跟現有的 LogoutController 撞同一個 URL
	}
}

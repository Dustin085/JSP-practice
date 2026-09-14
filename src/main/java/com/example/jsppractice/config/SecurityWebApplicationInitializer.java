package com.example.jsppractice.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

// 不用寫任何內容——繼承這個類別本身就會讓容器啟動時自動註冊 springSecurityFilterChain
// 這個 DelegatingFilterProxy，掛在 /*。預設 order 比 WebAppInitializer 低（會後註冊），
// 搭配 WebAppInitializer 的 @Order(HIGHEST_PRECEDENCE) 確保執行順序正確。
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
}

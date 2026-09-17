package com.example.jsppractice.filter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.MDC;

// 每個請求進來第一件事就是塞一個 traceId 進 MDC，logback pattern 印出來後，同一個請求觸發的所有
// log（不管是哪個 class 印的）都能用這個 id 串起來——跟 audit_logs 是互補的兩種追溯機制：
// audit_log 是「對哪筆資料做了什麼動作」，這個是「一次請求從頭到尾經過哪些地方」。
// finally 一定要 remove：Tomcat 用執行緒池處理請求，MDC 是 ThreadLocal，不清掉的話下一個
// 剛好用到同一條執行緒的請求會沿用到上一個請求的 traceId。
public class TraceIdFilter implements Filter {

	static final String TRACE_ID_KEY = "traceId";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_KEY);
		}
	}
}

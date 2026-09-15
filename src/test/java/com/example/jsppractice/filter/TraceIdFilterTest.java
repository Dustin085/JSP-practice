package com.example.jsppractice.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.slf4j.MDC;

public class TraceIdFilterTest {

	@Test
	public void putsTraceIdIntoMdcDuringChainAndRemovesItAfterward() throws Exception {
		TraceIdFilter filter = new TraceIdFilter();
		ServletRequest request = mock(ServletRequest.class);
		ServletResponse response = mock(ServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		doAnswer(invocation -> {
			assertNotNull("chain 執行期間 MDC 應該已經有 traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY));
			return null;
		}).when(chain).doFilter(request, response);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		assertNull("請求結束後 MDC 應該清乾淨，不留給下一個沿用同一條執行緒的請求",
				MDC.get(TraceIdFilter.TRACE_ID_KEY));
	}

	@Test
	public void removesTraceIdEvenWhenChainThrows() throws Exception {
		TraceIdFilter filter = new TraceIdFilter();
		ServletRequest request = mock(ServletRequest.class);
		ServletResponse response = mock(ServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

		try {
			filter.doFilter(request, response, chain);
		} catch (ServletException expected) {
			// 預期會往外丟，這裡只確認丟出去之前 finally 已經清過 MDC
		}

		assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
	}
}

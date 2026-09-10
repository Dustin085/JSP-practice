package com.example.jsppractice.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoginCheckInterceptorTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private HttpSession session;

	private LoginCheckInterceptor interceptor;

	@Before
	public void setUp() {
		interceptor = new LoginCheckInterceptor();
		when(request.getContextPath()).thenReturn("");
	}

	@Test
	public void preHandleReturnsTrueWhenLoggedIn() throws Exception {
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute("currentUser")).thenReturn("someUser");

		boolean result = interceptor.preHandle(request, response, new Object());

		assertTrue(result);
		verify(response, never()).sendRedirect(anyString());
	}

	@Test
	public void preHandleRedirectsWhenNoSession() throws Exception {
		when(request.getSession(false)).thenReturn(null);

		boolean result = interceptor.preHandle(request, response, new Object());

		assertFalse(result);
		verify(response).sendRedirect("/login");
	}

	@Test
	public void preHandleRedirectsWhenSessionHasNoCurrentUser() throws Exception {
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute("currentUser")).thenReturn(null);

		boolean result = interceptor.preHandle(request, response, new Object());

		assertFalse(result);
		verify(response).sendRedirect("/login");
	}
}

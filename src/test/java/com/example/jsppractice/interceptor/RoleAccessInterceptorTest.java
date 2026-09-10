package com.example.jsppractice.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import com.example.jsppractice.model.RoleType;
import com.example.jsppractice.model.User;

@RunWith(MockitoJUnitRunner.class)
public class RoleAccessInterceptorTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private HttpSession session;

	private RoleAccessInterceptor interceptor;

	@Before
	public void setUp() {
		interceptor = new RoleAccessInterceptor();
		when(request.getContextPath()).thenReturn("");
	}

	private boolean call(String method, String uri, RoleType role) throws Exception {
		when(request.getMethod()).thenReturn(method);
		when(request.getRequestURI()).thenReturn(uri);
		if (role != null) {
			when(request.getSession()).thenReturn(session);
			when(session.getAttribute("currentUser")).thenReturn(User.builder().id(1L).role(role).build());
		}
		return interceptor.preHandle(request, response, new Object());
	}

	@Test
	public void publicListPagesAreOpenToAnyRole() throws Exception {
		assertTrue(call("GET", "/books", RoleType.USER));
		assertTrue(call("GET", "/books/export", RoleType.USER));
		assertTrue(call("GET", "/authors", RoleType.USER));
		assertTrue(call("GET", "/categories", RoleType.USER));
	}

	@Test
	public void bookMutationsRequireAdmin() throws Exception {
		assertTrue(call("GET", "/books/new", RoleType.ADMIN));
		assertFalse(call("GET", "/books/new", RoleType.USER));
		verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "權限不足");
	}

	@Test
	public void approveAndRejectRequireAdmin() throws Exception {
		assertTrue(call("POST", "/requests/1/approve", RoleType.ADMIN));
		assertFalse(call("POST", "/requests/1/approve", RoleType.PROCUREMENT));
		assertFalse(call("POST", "/requests/1/reject", RoleType.USER));
	}

	@Test
	public void completeProcurementRequiresProcurementRole() throws Exception {
		assertTrue(call("POST", "/procurement/1/complete", RoleType.PROCUREMENT));
		assertFalse(call("POST", "/procurement/1/complete", RoleType.ADMIN));
		assertFalse(call("POST", "/procurement/1/complete", RoleType.USER));
	}

	@Test
	public void pathsWithNoMatchingRuleAreOpenToAnyLoggedInUser() throws Exception {
		assertTrue(call("GET", "/requests", RoleType.USER));
		assertTrue(call("GET", "/requests/new", RoleType.USER));
		assertTrue(call("POST", "/requests", RoleType.USER));
		assertTrue(call("GET", "/procurement", RoleType.USER));
	}
}

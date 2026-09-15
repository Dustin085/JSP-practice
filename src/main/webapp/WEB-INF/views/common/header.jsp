<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<header>
	<c:choose>
		<c:when test="${empty currentUser}">
			<p>
				<a href="${pageContext.request.contextPath}/login">登入</a>
			</p>
		</c:when>
		<c:otherwise>
			<p>${currentUser.name}，您好</p>
			<form action="${pageContext.request.contextPath}/logout" method="post">
				<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
				<button type="submit">登出</button>
			</form>
		</c:otherwise>
	</c:choose>
</header>
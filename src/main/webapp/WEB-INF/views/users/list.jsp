<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="使用者列表" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>使用者列表</h1>
	<p>
		<a href="${pageContext.request.contextPath}/">首頁</a>
	</p>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>姓名</th>
			<th>Email</th>
			<th>角色</th>
		</tr>
		<c:forEach var="user" items="${users}">
			<tr>
				<td>${user.id}</td>
				<td>${user.name}</td>
				<td>${user.maskedEmail}</td>
				<td>
					<c:forEach var="role" items="${user.roles}" varStatus="status">
						${role}<c:if test="${!status.last}">, </c:if>
					</c:forEach>
				</td>
			</tr>
		</c:forEach>
	</table>

	<%-- USER 不列在下拉選單裡：每個帳號一定有、不能手動加也不能移除 --%>
	<h2>新增角色</h2>
	<form action="${pageContext.request.contextPath}/users/roles/grant" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<label for="grant-user">使用者：</label>
		<select id="grant-user" name="userId">
			<c:forEach var="user" items="${users}">
				<option value="${user.id}">${user.name}（${user.maskedEmail}）</option>
			</c:forEach>
		</select>
		<label for="grant-role">角色：</label>
		<select id="grant-role" name="role">
			<c:forEach var="role" items="${manageableRoles}">
				<option value="${role}">${role}</option>
			</c:forEach>
		</select>
		<button type="submit">新增</button>
	</form>

	<h2>移除角色</h2>
	<form action="${pageContext.request.contextPath}/users/roles/revoke" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<label for="revoke-user">使用者：</label>
		<select id="revoke-user" name="userId">
			<c:forEach var="user" items="${users}">
				<option value="${user.id}">${user.name}（${user.maskedEmail}）</option>
			</c:forEach>
		</select>
		<label for="revoke-role">角色：</label>
		<select id="revoke-role" name="role">
			<c:forEach var="role" items="${manageableRoles}">
				<option value="${role}">${role}</option>
			</c:forEach>
		</select>
		<button type="submit" onclick="return confirm('確定移除這個角色?');">移除</button>
	</form>

	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

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
	<table border="1">
		<tr>
			<th>ID</th>
			<th>姓名</th>
			<th>Email</th>
		</tr>
		<c:forEach var="user" items="${users}">
			<tr>
				<td>${user.id}</td>
				<td>${user.name}</td>
				<td>${user.maskedEmail}</td>
			</tr>
		</c:forEach>
	</table>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

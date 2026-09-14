<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="註冊" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>註冊</h1>
	<form action="${pageContext.request.contextPath}/register" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
        <label for="email">Email：</label>
        <input type="email" id="email" name="email" required><br><br>

        <label for="name">姓名：</label>
        <input type="text" id="name" name="name" required><br><br>

        <label for="password">密碼：</label>
        <input type="password" id="password" name="password" required><br><br>

        <button type="submit">註冊</button>
	</form>
	<p><a href="${pageContext.request.contextPath}/login">已經有帳號？前往登入</a></p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp" %>
</body>
</html>

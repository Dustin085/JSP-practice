<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="登入" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>登入</h1>
	<form action="${pageContext.request.contextPath}/login" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<!-- 單行文字方塊 -->
        <label for="email">Email：</label>
        <input type="email" id="email" name="email" required><br><br>

        <!-- 密碼方塊 -->
        <label for="password">密碼：</label>
        <input type="password" id="password" name="password" required><br><br>
        
        <button type="submit">登入</button>
	</form>
	<p><a href="${pageContext.request.contextPath}/register">還沒有帳號?前往註冊</a></p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp" %>
</body>
</html>
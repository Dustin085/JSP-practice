<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="錯誤" />
</jsp:include>
<body>
	<p>${message}</p>
	<a href="${pageContext.request.contextPath}/">回到首頁</a>
</body>
</html>
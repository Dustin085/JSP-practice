<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="找不到頁面" />
</jsp:include>
<body>
	<p>找不到您要的頁面</p>
	<a href="${pageContext.request.contextPath}/">回到首頁</a>
</body>
</html>

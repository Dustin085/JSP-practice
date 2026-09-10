<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="JSP Practice" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>${message}</h1>
	<p><a href="${pageContext.request.contextPath}/books">前往 Book 管理</a></p>
</body>
</html>

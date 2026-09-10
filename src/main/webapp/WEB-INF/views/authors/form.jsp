<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="Author 表單" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<c:choose>
		<c:when test="${author.id == null}">
			<c:set var="formAction" value="${pageContext.request.contextPath}/authors" />
			<h1>新增作者</h1>
		</c:when>
		<c:otherwise>
			<c:set var="formAction" value="${pageContext.request.contextPath}/authors/${author.id}" />
			<h1>編輯作者</h1>
		</c:otherwise>
	</c:choose>

	<form:form modelAttribute="author" action="${formAction}" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<div>
			<form:label path="name">名稱:</form:label>
			<form:input path="name" />
			<form:errors path="name" cssClass="error" element="p" />
		</div>
		<button type="submit">儲存</button>
		<a href="${pageContext.request.contextPath}/authors">取消</a>
	</form:form>
</body>
</html>

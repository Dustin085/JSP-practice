<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="稽核紀錄" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>稽核紀錄</h1>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>時間</th>
			<th>操作人 ID</th>
			<th>動作</th>
			<th>對象類型</th>
			<th>對象 ID</th>
			<th>詳細內容</th>
		</tr>
		<c:forEach var="log" items="${auditLogs}">
			<tr>
				<td>${log.id}</td>
				<td>${log.auditedAt}</td>
				<td>${log.userId}</td>
				<td>${log.action}</td>
				<td>${log.entityType}</td>
				<td>${log.entityId}</td>
				<td>${log.detail}</td>
			</tr>
		</c:forEach>
	</table>
	<p>
		<c:if test="${pageNumber > 0}">
			<a href="${pageContext.request.contextPath}/audit-logs?page=${pageNumber - 1}">上一頁</a>
		</c:if>
		第 ${pageNumber + 1} / ${totalPages} 頁
		<c:if test="${pageNumber + 1 < totalPages}">
			<a href="${pageContext.request.contextPath}/audit-logs?page=${pageNumber + 1}">下一頁</a>
		</c:if>
	</p>
	<p><a href="${pageContext.request.contextPath}/books">回 Book 列表</a></p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp" %>
</body>
</html>

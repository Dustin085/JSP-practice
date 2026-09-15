<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="Category 列表" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>Category 列表</h1>
	<p><c:if test="${currentUser.role == 'ADMIN'}">
		<a href="${pageContext.request.contextPath}/categories/new">新增分類</a>
	</c:if></p>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>名稱</th>
			<th>操作</th>
		</tr>
		<c:forEach var="category" items="${categories}">
			<tr>
				<td>${category.id}</td>
				<td>${category.name}</td>
				<td><c:if test="${currentUser.role == 'ADMIN'}">
					<a href="${pageContext.request.contextPath}/categories/${category.id}/edit">編輯</a>
					<form action="${pageContext.request.contextPath}/categories/${category.id}/delete" method="post" style="display:inline">
						<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
						<button type="submit" onclick="return confirm('確定刪除?')">刪除</button>
					</form>
				</c:if></td>
			</tr>
		</c:forEach>
	</table>
	<p>
		<c:if test="${pageNumber > 0}">
			<a href="${pageContext.request.contextPath}/categories?page=${pageNumber - 1}">上一頁</a>
		</c:if>
		第 ${pageNumber + 1} / ${totalPages} 頁
		<c:if test="${pageNumber + 1 < totalPages}">
			<a href="${pageContext.request.contextPath}/categories?page=${pageNumber + 1}">下一頁</a>
		</c:if>
	</p>
	<p><a href="${pageContext.request.contextPath}/books">回 Book 列表</a></p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp" %>
</body>
</html>

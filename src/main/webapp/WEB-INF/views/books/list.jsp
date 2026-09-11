<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="Book 列表" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<h1>Book 列表</h1>
	<p>
		<c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
			<a href="${pageContext.request.contextPath}/books/new">新增書籍</a> |
		</c:if>
		<a href="${pageContext.request.contextPath}/authors">管理作者</a> | <a
			href="${pageContext.request.contextPath}/categories">管理分類</a> | <a
			href="${pageContext.request.contextPath}/books/export?keyword=${keyword}">匯出 Excel</a> | 
			<a href="${pageContext.request.contextPath}/requests">新書申請</a> | 
			<a href="${pageContext.request.contextPath}/procurement">採購清單</a>
		<c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
			| <a href="${pageContext.request.contextPath}/audit-logs">稽核紀錄</a>
		</c:if>
		<c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
			| <a href="${pageContext.request.contextPath}/reconciliations">資料對帳</a>
		</c:if>
	</p>
	<form method="get" action="${pageContext.request.contextPath}/books">
		<input type="text" name="keyword" value="${keyword}"
			placeholder="搜尋書名或作者" />
		<button type="submit">搜尋</button>
		<a href="${pageContext.request.contextPath}/books">清除</a>
	</form>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>書名</th>
			<th>ISBN</th>
			<th>作者</th>
			<th>出版年</th>
			<th>分類</th>
			<th>操作</th>
		</tr>
		<c:forEach var="book" items="${books}">
			<tr>
				<td>${book.id}</td>
				<td>${book.title}</td>
				<td>${book.isbn}</td>
				<td>${book.authorName}</td>
				<td>${book.publishedYear}</td>
				<td><c:forEach var="category" items="${book.categories}"
						varStatus="status">
					${category.name}<c:if test="${!status.last}">, </c:if>
					</c:forEach></td>
				<td><c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
					<a
						href="${pageContext.request.contextPath}/books/${book.id}/edit">編輯</a>
					<form
						action="${pageContext.request.contextPath}/books/${book.id}/delete"
						method="post" style="display: inline">
						<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
						<button type="submit" onclick="return confirm('確定刪除?')">刪除</button>
					</form>
				</c:if></td>
			</tr>
		</c:forEach>
	</table>
	<p>
		<c:if test="${pageNumber > 0}">
			<a href="${pageContext.request.contextPath}/books?keyword=${keyword}&page=${pageNumber - 1}">上一頁</a>
		</c:if>
		第 ${pageNumber + 1} / ${totalPages} 頁
		<c:if test="${pageNumber + 1 < totalPages}">
			<a href="${pageContext.request.contextPath}/books?keyword=${keyword}&page=${pageNumber + 1}">下一頁</a>
		</c:if>
	</p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp" %>
</body>
</html>

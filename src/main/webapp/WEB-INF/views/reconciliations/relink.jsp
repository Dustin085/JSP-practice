<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="重新連結書籍" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>重新連結書籍（對帳明細 #${reconciliationItemId}）</h1>

	<c:if test="${empty candidates}">
		<p>找不到符合書名/ISBN 的候選書籍，可以直接輸入書籍 ID。</p>
	</c:if>
	<c:if test="${not empty candidates}">
		<table border="1">
			<tr>
				<th>ID</th>
				<th>書名</th>
				<th>ISBN</th>
				<th>出版年</th>
				<th></th>
			</tr>
			<c:forEach var="book" items="${candidates}">
				<tr>
					<td>${book.id}</td>
					<td>${book.title}</td>
					<td>${book.isbn}</td>
					<td>${book.publishedYear}</td>
					<td>
						<form action="${pageContext.request.contextPath}/reconciliations/items/${reconciliationItemId}/relink"
							method="post" style="display: inline">
							<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp"%>
							<input type="hidden" name="bookId" value="${book.id}" />
							<button type="submit" onclick="return confirm('確定連結到這本書?')">選這本</button>
						</form>
					</td>
				</tr>
			</c:forEach>
		</table>
	</c:if>

	<h2>或手動輸入書籍 ID</h2>
	<form action="${pageContext.request.contextPath}/reconciliations/items/${reconciliationItemId}/relink"
		method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp"%>
		書籍 ID：<input type="number" name="bookId" required />
		<button type="submit" onclick="return confirm('確定連結到這個書籍 ID?')">確定重新連結</button>
	</form>

	<p><a href="${pageContext.request.contextPath}/reconciliations">回對帳列表</a></p>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

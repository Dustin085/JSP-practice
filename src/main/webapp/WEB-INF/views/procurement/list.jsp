<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="待採購清單" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>待採購清單</h1>
	<p>
		<a href="${pageContext.request.contextPath}/requests">回申請列表</a>
	</p>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>書名</th>
			<th>ISBN</th>
			<th>出版年</th>
			<th>預估金額</th>
			<th>操作</th>
		</tr>
		<c:forEach var="item" items="${procurementItems}">
			<tr>
				<td>${item.id}</td>
				<td>${item.bookRequestItem.title}</td>
				<td>${item.bookRequestItem.isbn}</td>
				<td>${item.bookRequestItem.publishedYear}</td>
				<td>${item.bookRequestItem.estimatedPrice}</td>
				<td>
					<c:if test="${sessionScope.currentUser.role == 'PROCUREMENT'}">
						<form action="${pageContext.request.contextPath}/procurement/${item.id}/complete"
							method="post" style="display: inline">
							<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
							<button type="submit" onclick="return confirm('確定完成採購並將書籍入庫?')">完成採購</button>
						</form>
					</c:if>
				</td>
			</tr>
		</c:forEach>
	</table>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

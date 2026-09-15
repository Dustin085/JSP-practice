<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="申請 列表" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>申請 列表</h1>
	<p>
		<a href="${pageContext.request.contextPath}/requests/new">新增申請</a> | <a
			href="${pageContext.request.contextPath}/books">管理書籍</a> | 
			<a href="${pageContext.request.contextPath}/procurement">採購清單</a>
	</p>
	<p>
		<a href="${pageContext.request.contextPath}/requests">全部</a> <a
			href="${pageContext.request.contextPath}/requests?status=PENDING">待審查</a>
		<a href="${pageContext.request.contextPath}/requests?status=APPROVED">已同意</a>
		<a href="${pageContext.request.contextPath}/requests?status=REJECTED">已拒絕</a>
	</p>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>申請者ID</th>
			<th>審核者ID</th>
			<th>狀態</th>
			<th>申請書籍</th>
			<th>申請時間</th>
			<th>審核時間</th>
			<th>操作</th>
		</tr>
		<c:forEach var="request" items="${bookRequests}">
			<tr>
				<td>${request.id}</td>
				<td>${request.requesterId}</td>
				<td>${request.approverId}</td>
				<td>${request.status}</td>
				<td>
					<ul>
						<c:forEach var="item" items="${request.bookRequestItems}">
							<li>${item.title}<c:if test="${not empty item.estimatedPrice}"> (預估 ${item.estimatedPrice})</c:if></li>
						</c:forEach>
					</ul>
				</td>
				<td>${request.requestedAtDisplay}</td>
				<td>${request.approvedAtDisplay}</td>
				<td>
					<c:if test="${currentUser.role == 'ADMIN' && request.status == 'PENDING'}">
						<form
							action="${pageContext.request.contextPath}/requests/${request.id}/approve"
							method="post" style="display: inline">
							<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
							<button type="submit" onclick="return confirm('確定核准?')">核准</button>
						</form>
						<form
							action="${pageContext.request.contextPath}/requests/${request.id}/reject"
							method="post" style="display: inline">
							<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
							<button type="submit" onclick="return confirm('確定拒絕?')">拒絕</button>
						</form>
					</c:if>
				</td>
			</tr>
		</c:forEach>
	</table>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

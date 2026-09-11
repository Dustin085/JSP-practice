<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="資料對帳" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>資料對帳</h1>
	<p>
		<a href="${pageContext.request.contextPath}/audit-logs">稽核紀錄</a> | <a
			href="${pageContext.request.contextPath}/books">回 Book 列表</a>
	</p>
	<p>
		<form action="${pageContext.request.contextPath}/reconciliations/book-request-items" method="post"
			style="display: inline">
			<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp"%>
			<button type="submit">執行對帳：已核准申請 vs 採購清單</button>
		</form>
		<form action="${pageContext.request.contextPath}/reconciliations/procurement-items" method="post"
			style="display: inline">
			<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp"%>
			<button type="submit">執行對帳：已完成採購 vs 書籍</button>
		</form>
	</p>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>類型</th>
			<th>狀態</th>
			<th>執行時間</th>
			<th>差異明細</th>
		</tr>
		<c:forEach var="reconciliation" items="${reconciliations}">
			<tr>
				<td>${reconciliation.id}</td>
				<td>${reconciliation.reconciliationType}</td>
				<td>${reconciliation.status}</td>
				<td>${reconciliation.reconciledAtDisplay}</td>
				<td>
					<c:if test="${empty reconciliation.reconciliationItems}">-</c:if>
					<ul>
						<c:forEach var="item" items="${reconciliation.reconciliationItems}">
							<li>${item.entityType} #${item.entityId}（${item.discrepancyType}）：${item.detail.reason}</li>
						</c:forEach>
					</ul>
				</td>
			</tr>
		</c:forEach>
	</table>
	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

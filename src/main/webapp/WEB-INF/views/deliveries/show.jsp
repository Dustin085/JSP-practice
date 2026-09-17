<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="到貨清單匯入" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>到貨清單匯入</h1>
	<p>
		<a href="${pageContext.request.contextPath}/reconciliations">資料對帳</a> | <a
			href="${pageContext.request.contextPath}/books">回 Book 列表</a>
	</p>
	<p>手動立即抓取 SFTP 上待處理的到貨清單、解析並匯入，跟每天 01:00 的排程走同一套邏輯。</p>
	<form action="${pageContext.request.contextPath}/deliveries/run" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp"%>
		<button type="submit">立即執行</button>
	</form>

	<c:if test="${not empty outcomes}">
		<h2>本次執行結果</h2>
		<table border="1">
			<tr>
				<th>檔案</th>
				<th>結果</th>
				<th>成功筆數</th>
				<th>備註</th>
			</tr>
			<c:forEach var="outcome" items="${outcomes}">
				<tr>
					<td>${outcome.fileName}</td>
					<c:choose>
						<c:when test="${outcome.succeeded}">
							<td>成功</td>
							<td>${outcome.result.appliedCount}</td>
							<td>
								<c:if test="${empty outcome.result.skipped}">-</c:if>
								<ul>
									<c:forEach var="skip" items="${outcome.result.skipped}">
										<li>單號 ${skip.referenceId}：${skip.reason}</li>
									</c:forEach>
								</ul>
							</td>
						</c:when>
						<c:otherwise>
							<td>失敗</td>
							<td>-</td>
							<td>${outcome.errorMessage}</td>
						</c:otherwise>
					</c:choose>
				</tr>
			</c:forEach>
		</table>
	</c:if>

	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

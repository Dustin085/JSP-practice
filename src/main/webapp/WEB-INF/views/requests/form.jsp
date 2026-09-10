<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="申請書籍" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp"%>
	<h1>申請書籍</h1>

	<form action="${pageContext.request.contextPath}/requests" method="post" onsubmit="return validateForm()">
		<input type="hidden" name="idempotencyKey" value="${idempotencyKey}">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<table border="1">
			<tr>
				<th>書名</th>
				<th>ISBN</th>
				<th>作者</th>
				<th>出版年</th>
				<th>預估金額</th>
				<th></th>
			</tr>
			<tbody id="itemRows">
				<tr>
					<td><input type="text" name="title"></td>
					<td><input type="text" name="isbn"></td>
					<td><select name="authorId">
							<option value="">請選擇作者</option>
							<c:forEach var="author" items="${authors}">
								<option value="${author.id}">${author.name}</option>
							</c:forEach>
					</select></td>
					<td><input type="number" name="publishedYear"></td>
					<td><input type="number" step="0.01" name="estimatedPrice"></td>
					<td><button type="button" onclick="removeRow(this)">移除</button></td>
				</tr>
			</tbody>
		</table>

		<p>
			<button type="button" onclick="addRow()">新增一列</button>
		</p>

		<button type="submit">送出申請</button>
		<a href="${pageContext.request.contextPath}/requests">取消</a>
	</form>

	<template id="itemRowTemplate">
		<tr>
			<td><input type="text" name="title"></td>
			<td><input type="text" name="isbn"></td>
			<td><select name="authorId">
					<option value="">請選擇作者</option>
					<c:forEach var="author" items="${authors}">
						<option value="${author.id}">${author.name}</option>
					</c:forEach>
			</select></td>
			<td><input type="number" name="publishedYear"></td>
			<td><input type="number" step="0.01" name="estimatedPrice"></td>
			<td><button type="button" onclick="removeRow(this)">移除</button></td>
		</tr>
	</template>

	<script>
		function addRow() {
			var template = document.getElementById('itemRowTemplate');
			var clone = template.content.cloneNode(true);
			document.getElementById('itemRows').appendChild(clone);
		}

		function removeRow(button) {
			var row = button.closest('tr');
			row.parentNode.removeChild(row);
		}

		function validateForm() {
			var titles = document.getElementsByName('title');
			for (var i = 0; i < titles.length; i++) {
				if (titles[i].value.trim() !== '') {
					return true;
				}
			}
			alert('至少需要填寫一本書的書名');
			return false;
		}
	</script>

	<%@ include file="/WEB-INF/views/common/flashMessage.jsp"%>
</body>
</html>

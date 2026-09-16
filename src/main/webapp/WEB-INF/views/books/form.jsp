<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<jsp:include page="/WEB-INF/views/common/head.jsp">
	<jsp:param name="title" value="Book 表單" />
</jsp:include>
<body>
	<%@ include file="/WEB-INF/views/common/header.jsp" %>
	<c:choose>
		<c:when test="${book.id == null}">
			<c:set var="formAction" value="${pageContext.request.contextPath}/books" />
			<h1>新增書籍</h1>
		</c:when>
		<c:otherwise>
			<c:set var="formAction" value="${pageContext.request.contextPath}/books/${book.id}" />
			<h1>編輯書籍</h1>
		</c:otherwise>
	</c:choose>

	<c:if test="${not empty conflictMessage}">
		<p style="color: red;">${conflictMessage}</p>
	</c:if>

	<form:form modelAttribute="book" action="${formAction}" method="post">
		<%@ include file="/WEB-INF/views/common/csrfTokenInput.jsp" %>
		<form:hidden path="version" />
		<div>
			<form:label path="title">書名:</form:label>
			<form:input path="title" />
			<form:errors path="title" cssClass="error" element="p" />
		</div>
		<div>
			<form:label path="isbn">ISBN:</form:label>
			<form:input path="isbn" />
			<form:errors path="isbn" cssClass="error" element="p" />
		</div>
		<div>
			<form:label path="authorId">作者:</form:label>
			<form:select path="authorId">
				<form:option value="" label="請選擇作者" />
				<form:options items="${authors}" itemValue="id" itemLabel="name" />
			</form:select>
			<form:errors path="authorId" cssClass="error" element="p" />
			<a href="${pageContext.request.contextPath}/authors/new">沒有想要的作者？新增一個</a>
		</div>
		<div>
			<form:label path="publishedYear">出版年:</form:label>
			<form:input path="publishedYear" />
			<form:errors path="publishedYear" cssClass="error" element="p" />
		</div>
		<div>
			<label>分類:</label><br>
			<c:forEach var="category" items="${categories}">
				<label>
					<input type="checkbox" name="categoryIds" value="${category.id}"
						<c:if test="${selectedCategoryIds.contains(category.id)}">checked="checked"</c:if> />
					${category.name}
				</label>
			</c:forEach>
			<a href="${pageContext.request.contextPath}/categories/new">沒有想要的分類？新增一個</a>
		</div>
		<c:choose>
			<c:when test="${not empty conflictMessage}">
				<button type="submit" onclick="return confirm('確定要用你剛剛輸入的內容覆蓋別人的修改嗎?');">確認覆蓋並儲存</button>
			</c:when>
			<c:otherwise>
				<button type="submit">儲存</button>
			</c:otherwise>
		</c:choose>
		<a href="${pageContext.request.contextPath}/books">取消</a>
	</form:form>
</body>
</html>

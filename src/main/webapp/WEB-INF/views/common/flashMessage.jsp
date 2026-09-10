<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${not empty flashMessage}">
	<div id="flashMessage" data-message="${fn:escapeXml(flashMessage)}" hidden="hidden"></div>
	<script>
		var flashEl = document.getElementById('flashMessage');
		if (flashEl) {
			alert(flashEl.dataset.message);
		}
	</script>
</c:if>

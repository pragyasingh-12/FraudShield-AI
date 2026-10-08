<c:set var="pageTitle" value="Error"/>
<%@ include file="fragments/header.jspf" %>
<div class="panel">
  <h1>
    <c:choose>
      <c:when test="${requestScope['javax.servlet.error.status_code'] == 404}">Page not found</c:when>
      <c:when test="${requestScope['javax.servlet.error.status_code'] == 403}">Access denied</c:when>
      <c:otherwise>Something went wrong</c:otherwise>
    </c:choose>
  </h1>
  <p>
    <c:choose>
      <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
      <c:when test="${not empty requestScope['javax.servlet.error.message']}"><c:out value="${requestScope['javax.servlet.error.message']}"/></c:when>
      <c:otherwise>The request could not be completed. Try again, or go back to the dashboard.</c:otherwise>
    </c:choose>
  </p>
  <a class="btn" href="${pageContext.request.contextPath}/dashboard">Back to dashboard</a>
</div>
<%@ include file="fragments/footer.jspf" %>

<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Transaction analysis</h1>
  <p>What-if check: scores a payment with the full engine but saves nothing</p></div></div>
<c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
<section class="panel">
  <c:set var="formAction" value="/analyze"/>
  <c:set var="submitLabel" value="Analyze (do not save)"/>
  <c:set var="showBurst" value="${false}"/>
  <%@ include file="fragments/txn-form.jspf" %>
</section>
<c:if test="${not empty result}">
  <div class="notice notice-info">Dry run &mdash; nothing was stored. Use <a href="${ctx}/transactions/add">Add transaction</a> to save a payment.</div>
  <%@ include file="fragments/risk-result.jspf" %>
</c:if>
<%@ include file="fragments/footer.jspf" %>

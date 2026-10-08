<%@ include file="fragments/header.jspf" %>
<div class="page-head">
  <div><h1>Risk analysis ${txn.reference}</h1>
    <p><c:out value="${txn.userName}"/> &middot; ${txn.transactionTimeFormatted} &middot; status
      <span class="badge badge-${txn.status.cssClass}">${txn.status}</span></p></div>
  <div><a class="btn btn-secondary" href="${ctx}/history">Back to history</a></div>
</div>

<c:if test="${justSubmitted}"><div class="notice notice-success">Transaction saved. Analysis runs on a background worker thread.</div></c:if>

<c:choose>
  <c:when test="${empty result}">
    <div class="panel">
      <h2>Analysis in progress</h2>
      <p>This transaction is stored with status PENDING. This page reloads automatically every 2 seconds until the worker thread finishes.</p>
    </div>
  </c:when>
  <c:otherwise>
    <%@ include file="fragments/risk-result.jspf" %>
  </c:otherwise>
</c:choose>

<section class="panel">
  <h3>Transaction details</h3>
  <dl class="kv">
    <dt>Reference</dt><dd>${txn.reference}</dd>
    <dt>Customer</dt><dd><a href="${ctx}/users?id=${txn.userId}"><c:out value="${txn.userName}"/></a></dd>
    <dt>Amount</dt><dd>&#8377;<fmt:formatNumber value="${txn.amount}" pattern="#,##0.00"/></dd>
    <dt>Type</dt><dd>${txn.transactionType}</dd>
    <dt>Receiver</dt><dd><c:out value="${txn.receiver}"/></dd>
    <dt>Device</dt><dd class="mono"><c:out value="${txn.deviceId}"/></dd>
    <dt>Location</dt><dd><c:out value="${txn.location}"/></dd>
    <dt>Time</dt><dd>${txn.transactionTimeFormatted}</dd>
  </dl>
</section>
<%@ include file="fragments/footer.jspf" %>

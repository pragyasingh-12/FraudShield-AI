<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Add transaction</h1>
  <p>Stores the payment, then analyses it on a background worker thread</p></div></div>
<c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
<section class="panel">
  <c:set var="formAction" value="/transactions/add"/>
  <c:set var="submitLabel" value="Save and analyze"/>
  <c:set var="showBurst" value="${true}"/>
  <%@ include file="fragments/txn-form.jspf" %>
</section>
<section class="panel">
  <h3>How the simulator works</h3>
  <p class="muted">The payment is saved as PENDING, a worker thread builds the customer's behaviour profile, runs the rule engine and the Weka model, then stores the score, status and (if needed) an alert in one database transaction. The risk page refreshes itself until the result is ready.
  <strong>Simulate rapid burst</strong> stores several payments 20 seconds apart and analyses them in parallel.</p>
</section>
<%@ include file="fragments/footer.jspf" %>

<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1><c:out value="${subject.name}"/></h1><p><c:out value="${subject.email}"/> &middot; ${subject.role}</p></div>
  <a class="btn btn-secondary" href="${ctx}/users">Back to users</a></div>
<div class="grid grid-2">
  <section class="panel">
    <h2>Behaviour profile</h2>
    <c:choose>
      <c:when test="${empty profile}"><p class="empty">No profile yet - it is built after the first approved transactions.</p></c:when>
      <c:otherwise>
        <dl class="kv">
          <dt>Average amount</dt><dd>&#8377;<fmt:formatNumber value="${profile.avgAmount}" pattern="#,##0.00"/></dd>
          <dt>Std deviation</dt><dd>&#8377;<fmt:formatNumber value="${profile.stdDevAmount}" pattern="#,##0.00"/></dd>
          <dt>Largest payment</dt><dd>&#8377;<fmt:formatNumber value="${profile.maxAmount}" pattern="#,##0.00"/></dd>
          <dt>Usual hours</dt><dd>${profile.usualStartHour}:00 - ${profile.usualEndHour}:59</dd>
          <dt>Usual location</dt><dd><c:out value="${profile.usualLocation}"/></dd>
          <dt>Payments per active day</dt><dd><fmt:formatNumber value="${profile.transactionFrequency}" maxFractionDigits="2"/></dd>
          <dt>Approved history</dt><dd>${profile.totalTransactions} transactions</dd>
          <dt>Updated</dt><dd>${profile.updatedAtFormatted}</dd>
        </dl>
      </c:otherwise>
    </c:choose>
  </section>
  <section class="panel">
    <h2>Trusted devices</h2>
    <c:if test="${empty devices}"><p class="empty">No trusted devices registered.</p></c:if>
    <c:if test="${not empty devices}">
      <table><thead><tr><th>Device ID</th><th>Type</th><th>First seen</th></tr></thead><tbody>
      <c:forEach items="${devices}" var="d"><tr><td class="mono"><c:out value="${d.deviceId}"/></td><td>${d.deviceType}</td><td>${d.firstSeenFormatted}</td></tr></c:forEach>
      </tbody></table>
    </c:if>
    <c:if test="${sessionScope.user.admin}">
      <form method="post" action="${ctx}/users" style="margin-top:1rem">
        <%@ include file="fragments/csrf.jspf" %>
        <input type="hidden" name="action" value="addDevice"><input type="hidden" name="id" value="${subject.id}">
        <div class="form-grid">
          <div><label for="deviceId">Register device ID</label><input id="deviceId" name="deviceId" required maxlength="64"></div>
          <div><label for="deviceType">Type</label><select id="deviceType" name="deviceType"><option>MOBILE</option><option>BROWSER</option><option>TABLET</option></select></div>
        </div>
        <div class="actions"><button class="btn btn-secondary" type="submit">Add trusted device</button></div>
      </form>
    </c:if>
  </section>
</div>
<section class="panel">
  <h2>Recent transactions</h2>
  <c:if test="${empty recent}"><p class="empty">No transactions yet.</p></c:if>
  <c:if test="${not empty recent}">
    <div class="table-wrap"><table>
      <thead><tr><th>ID</th><th>Receiver</th><th class="num">Amount</th><th>Time</th><th>Device</th><th>Location</th><th class="num">Score</th><th>Status</th></tr></thead><tbody>
      <c:forEach items="${recent}" var="t"><tr>
        <td><a href="${ctx}/risk?id=${t.id}">${t.reference}</a></td><td><c:out value="${t.receiver}"/></td>
        <td class="num">&#8377;<fmt:formatNumber value="${t.amount}" pattern="#,##0.00"/></td><td>${t.transactionTimeFormatted}</td>
        <td class="mono"><c:out value="${t.deviceId}"/></td><td><c:out value="${t.location}"/></td>
        <td class="num"><c:out value="${empty t.riskScore ? '-' : t.riskScore}"/></td>
        <td><span class="badge badge-${t.status.cssClass}">${t.status}</span></td></tr></c:forEach>
      </tbody></table></div>
  </c:if>
</section>
<%@ include file="fragments/footer.jspf" %>

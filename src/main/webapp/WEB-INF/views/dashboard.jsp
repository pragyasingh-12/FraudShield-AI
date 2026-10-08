<%@ include file="fragments/header.jspf" %>
<div class="page-head">
  <div><h1>Dashboard</h1><p>Live summary of analysed payments and open alerts</p></div>
  <a class="btn" href="${ctx}/transactions/add">Add transaction</a>
</div>

<c:if test="${stats.pendingTransactions > 0}">
  <div class="notice notice-info">${stats.pendingTransactions} transaction(s) are still waiting for analysis (the background worker is processing them). Reload in a moment.</div>
</c:if>

<section class="stats" aria-label="Summary">
  <div class="stat"><div class="value">${stats.totalTransactions}</div><div class="label">Total transactions</div></div>
  <div class="stat s-medium"><div class="value">${stats.suspiciousTransactions}</div><div class="label">Suspicious (medium and above)</div></div>
  <div class="stat s-high"><div class="value">${stats.highRiskTransactions}</div><div class="label">High risk</div></div>
  <div class="stat s-critical"><div class="value">${stats.criticalTransactions}</div><div class="label">Critical / blocked</div></div>
  <div class="stat"><div class="value"><fmt:formatNumber value="${stats.averageRiskScore}" maxFractionDigits="1"/></div><div class="label">Average risk score</div></div>
  <div class="stat s-critical"><div class="value">${stats.openAlerts}</div><div class="label">Open alerts</div></div>
</section>

<div class="grid grid-2">
  <section class="panel">
    <h2>Risk level distribution</h2>
    <c:forEach items="${stats.levelCounts}" var="e">
      <c:set var="pct" value="${stats.totalTransactions > 0 ? e.value * 100 / stats.totalTransactions : 0}"/>
      <div style="display:grid;grid-template-columns:90px 1fr 50px;gap:.6rem;align-items:center;margin:.45rem 0">
        <span class="badge badge-${fn:toLowerCase(e.key)}">${e.key}</span>
        <div class="bar ${e.key == 'CRITICAL' ? 'b-critical' : e.key == 'HIGH' ? 'b-high' : e.key == 'MEDIUM' ? 'b-medium' : ''}"><i style="width:${pct}%"></i></div>
        <span class="num">${e.value}</span>
      </div>
    </c:forEach>
  </section>
  <section class="panel">
    <h2>Detection engine</h2>
    <dl class="kv">
      <dt>Rule engine</dt><dd>6 weighted factors (amount, frequency, time, device, location, behaviour)</dd>
      <dt>Weka model</dt>
      <dd><c:choose>
        <c:when test="${modelReport.ready}">J48 trained on ${modelReport.rows} rows &mdash; hold-out accuracy <fmt:formatNumber value="${modelReport.holdoutAccuracy}" maxFractionDigits="1"/>%</c:when>
        <c:when test="${modelReport.status == 'FAILED'}">Not available: <c:out value="${modelReport.error}"/></c:when>
        <c:otherwise>Training in background...</c:otherwise></c:choose></dd>
      <dt>Analysed this session</dt><dd>${analysedCount}</dd>
    </dl>
  </section>
</div>

<section class="panel">
  <h2>Recent alerts</h2>
  <c:choose>
    <c:when test="${empty stats.recentAlerts}"><p class="empty">No alerts yet. Submit a suspicious transaction to see one.</p></c:when>
    <c:otherwise>
      <div class="table-wrap"><table>
        <thead><tr><th>Transaction</th><th>Customer</th><th class="num">Amount</th><th>Level</th><th>Why it was flagged</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach items="${stats.recentAlerts}" var="a">
          <tr>
            <td><a href="${ctx}/risk?id=${a.transactionId}">${a.transactionReference}</a></td>
            <td><c:out value="${a.userName}"/></td>
            <td class="num">&#8377;<fmt:formatNumber value="${a.amount}" pattern="#,##0"/></td>
            <td><span class="badge badge-${a.riskLevel.cssClass}">${a.riskLevel}</span></td>
            <td><c:out value="${fn:substring(a.reason, 0, 110)}"/><c:if test="${fn:length(a.reason) > 110}">&hellip;</c:if></td>
            <td><span class="badge badge-${fn:toLowerCase(a.alertStatus)}">${a.alertStatus.label}</span></td>
          </tr>
        </c:forEach>
        </tbody></table></div>
    </c:otherwise>
  </c:choose>
  <p><a href="${ctx}/alerts">View all alerts</a></p>
</section>

<section class="panel">
  <h2>Recent transactions</h2>
  <c:choose>
    <c:when test="${empty stats.recentTransactions}"><p class="empty">No transactions yet. Load database/sample_data.sql or add one.</p></c:when>
    <c:otherwise>
      <div class="table-wrap"><table>
        <thead><tr><th>ID</th><th>Customer</th><th>Receiver</th><th class="num">Amount</th><th>Time</th><th class="num">Score</th><th>Risk</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach items="${stats.recentTransactions}" var="t">
          <tr>
            <td><a href="${ctx}/risk?id=${t.id}">${t.reference}</a></td>
            <td><c:out value="${t.userName}"/></td>
            <td><c:out value="${t.receiver}"/></td>
            <td class="num">&#8377;<fmt:formatNumber value="${t.amount}" pattern="#,##0.00"/></td>
            <td>${t.transactionTimeFormatted}</td>
            <td class="num"><c:out value="${empty t.riskScore ? '-' : t.riskScore}"/></td>
            <td><c:set var="badgeLevel" value="${t.riskLevel}"/><%@ include file="fragments/badge.jspf" %></td>
            <td><span class="badge badge-${t.status.cssClass}">${t.status}</span></td>
          </tr>
        </c:forEach>
        </tbody></table></div>
    </c:otherwise>
  </c:choose>
</section>
<%@ include file="fragments/footer.jspf" %>

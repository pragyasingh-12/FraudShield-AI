<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Fraud alerts</h1><p>Medium, high and critical transactions with the reasons they were flagged</p></div></div>
<section class="panel">
  <form method="get" action="${ctx}/alerts" class="filters">
    <div><label for="level">Risk level</label>
      <select id="level" name="level"><option value="">Any</option>
        <c:forEach items="MEDIUM,HIGH,CRITICAL" var="l"><option value="${l}" ${param.level == l ? 'selected' : ''}>${l}</option></c:forEach></select></div>
    <div><label for="status">Alert status</label>
      <select id="status" name="status"><option value="">Any</option>
        <c:forEach items="${alertStatuses}" var="s"><option value="${s}" ${param.status == s ? 'selected' : ''}>${s.label}</option></c:forEach></select></div>
    <div><button class="btn" type="submit">Filter</button> <a class="btn btn-secondary" href="${ctx}/alerts">Clear</a></div>
  </form>
</section>
<section class="panel">
  <c:choose>
    <c:when test="${empty alerts}"><p class="empty">No alerts match. Alerts appear when a transaction scores above 30.</p></c:when>
    <c:otherwise>
      <div class="table-wrap"><table>
        <thead><tr><th>Raised</th><th>Transaction</th><th>Customer</th><th class="num">Amount</th><th class="num">Score</th><th>Level</th><th>Reasons</th><th>Decision</th></tr></thead>
        <tbody>
        <c:forEach items="${alerts}" var="a">
          <tr>
            <td>${a.createdAtFormatted}</td>
            <td><a href="${ctx}/risk?id=${a.transactionId}">${a.transactionReference}</a></td>
            <td><c:out value="${a.userName}"/></td>
            <td class="num">&#8377;<fmt:formatNumber value="${a.amount}" pattern="#,##0"/></td>
            <td class="num">${a.riskScore}</td>
            <td><span class="badge badge-${a.riskLevel.cssClass}">${a.riskLevel}</span></td>
            <td style="max-width:360px"><c:out value="${a.reason}"/></td>
            <td>
              <form method="post" action="${ctx}/alerts">
                <%@ include file="fragments/csrf.jspf" %>
                <input type="hidden" name="alertId" value="${a.id}">
                <select name="status" data-autosubmit aria-label="Alert decision">
                  <c:forEach items="${alertStatuses}" var="s"><option value="${s}" ${a.alertStatus == s ? 'selected' : ''}>${s.label}</option></c:forEach>
                </select>
              </form>
            </td>
          </tr>
        </c:forEach>
        </tbody></table></div>
      <p class="muted">Marking an alert FALSE POSITIVE releases the transaction (APPROVED). CONFIRMED FRAUD blocks it and marks its device as suspicious.</p>
    </c:otherwise>
  </c:choose>
</section>
<%@ include file="fragments/footer.jspf" %>

<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Transaction history</h1><p>${pageResult.total} transaction(s) match</p></div>
  <a class="btn" href="${ctx}/transactions/add">Add transaction</a></div>

<section class="panel">
  <form method="get" action="${ctx}/history" class="filters">
    <div><label for="q">Search</label><input id="q" name="q" placeholder="TXN1024, receiver, customer" value="<c:out value='${param.q}'/>"></div>
    <div><label for="level">Risk level</label>
      <select id="level" name="level"><option value="">Any</option>
        <c:forEach items="LOW,MEDIUM,HIGH,CRITICAL" var="l" varStatus="s">
          <option value="${l}" ${param.level == l ? 'selected' : ''}>${l}</option></c:forEach></select></div>
    <div><label for="status">Status</label>
      <select id="status" name="status"><option value="">Any</option>
        <c:forEach items="PENDING,APPROVED,REVIEW,BLOCKED" var="s">
          <option value="${s}" ${param.status == s ? 'selected' : ''}>${s}</option></c:forEach></select></div>
    <div><label for="type">Type</label>
      <select id="type" name="type"><option value="">Any</option>
        <c:forEach items="${types}" var="t"><option value="${t}" ${param.type == t ? 'selected' : ''}>${t}</option></c:forEach></select></div>
    <div><label for="from">From</label><input id="from" name="from" type="date" value="<c:out value='${param.from}'/>"></div>
    <div><label for="to">To</label><input id="to" name="to" type="date" value="<c:out value='${param.to}'/>"></div>
    <div><label for="min">Min amount</label><input id="min" name="min" type="number" min="0" value="<c:out value='${param.min}'/>"></div>
    <div><label for="max">Max amount</label><input id="max" name="max" type="number" min="0" value="<c:out value='${param.max}'/>"></div>
    <div><button class="btn" type="submit">Apply filters</button> <a class="btn btn-secondary" href="${ctx}/history">Clear</a></div>
  </form>
</section>

<section class="panel">
  <c:choose>
    <c:when test="${empty pageResult.items}"><p class="empty">No transactions match these filters.</p></c:when>
    <c:otherwise>
      <div class="table-wrap"><table>
        <thead><tr><th>ID</th><th>Customer</th><th>Type</th><th>Receiver</th><th class="num">Amount</th><th>Time</th><th>Location</th><th class="num">Score</th><th>Risk</th><th>Status</th><c:if test="${sessionScope.user.admin}"><th></th></c:if></tr></thead>
        <tbody>
        <c:forEach items="${pageResult.items}" var="t">
          <tr>
            <td><a href="${ctx}/risk?id=${t.id}">${t.reference}</a></td>
            <td><c:out value="${t.userName}"/></td>
            <td>${t.transactionType}</td>
            <td><c:out value="${t.receiver}"/></td>
            <td class="num">&#8377;<fmt:formatNumber value="${t.amount}" pattern="#,##0.00"/></td>
            <td>${t.transactionTimeFormatted}</td>
            <td><c:out value="${t.location}"/></td>
            <td class="num"><c:out value="${empty t.riskScore ? '-' : t.riskScore}"/></td>
            <td><c:set var="badgeLevel" value="${t.riskLevel}"/><%@ include file="fragments/badge.jspf" %></td>
            <td><span class="badge badge-${t.status.cssClass}">${t.status}</span></td>
            <c:if test="${sessionScope.user.admin}">
              <td><form method="post" action="${ctx}/history" class="inline-form" data-confirm="Delete ${t.reference}? This also removes its analysis and alert.">
                <%@ include file="fragments/csrf.jspf" %>
                <input type="hidden" name="id" value="${t.id}">
                <button class="btn btn-danger btn-small" type="submit">Delete</button></form></td>
            </c:if>
          </tr>
        </c:forEach>
        </tbody></table></div>
      <div class="pager">
        <c:if test="${pageResult.hasPrevious}"><a class="btn btn-secondary btn-small" href="${ctx}/history?page=${pageResult.page - 1}<c:out value='${filterQuery}'/>">Previous</a></c:if>
        <span class="muted">Page ${pageResult.page} of ${pageResult.totalPages}</span>
        <c:if test="${pageResult.hasNext}"><a class="btn btn-secondary btn-small" href="${ctx}/history?page=${pageResult.page + 1}<c:out value='${filterQuery}'/>">Next</a></c:if>
      </div>
    </c:otherwise>
  </c:choose>
</section>
<%@ include file="fragments/footer.jspf" %>

<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Analytics</h1><p>How risk is distributed across the stored transactions</p></div></div>

<div class="grid grid-2">
  <section class="panel">
    <h2>Risk levels</h2>
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
    <h2>Average factor score (0-100)</h2>
    <c:forEach items="${factorAverages}" var="e">
      <div style="display:grid;grid-template-columns:120px 1fr 50px;gap:.6rem;align-items:center;margin:.45rem 0">
        <span>${e.key}</span><div class="bar"><i style="width:${e.value}%"></i></div>
        <span class="num"><fmt:formatNumber value="${e.value}" maxFractionDigits="1"/></span>
      </div>
    </c:forEach>
  </section>
</div>

<section class="panel">
  <h2>Transactions by hour of day</h2>
  <c:set var="maxH" value="1"/>
  <c:forEach items="${byHour}" var="e"><c:if test="${e.value > maxH}"><c:set var="maxH" value="${e.value}"/></c:if></c:forEach>
  <div style="display:flex;align-items:flex-end;gap:4px;height:140px" role="img" aria-label="Transactions per hour">
    <c:forEach items="${byHour}" var="e">
      <div style="flex:1;display:flex;flex-direction:column;justify-content:flex-end;align-items:center;height:100%">
        <div title="${e.value} transaction(s) at ${e.key}:00" style="width:100%;background:${e.key < 5 ? 'var(--critical)' : 'var(--accent)'};height:${e.value * 100 / maxH}%;min-height:${e.value > 0 ? 2 : 0}px"></div>
      </div>
    </c:forEach>
  </div>
  <div style="display:flex;gap:4px;font-size:.7rem;color:var(--muted)">
    <c:forEach items="${byHour}" var="e"><span style="flex:1;text-align:center">${e.key}</span></c:forEach>
  </div>
  <p class="muted">Red bars are night hours (00:00-04:59), which the time rule treats as higher risk.</p>
</section>

<div class="grid grid-2">
  <section class="panel">
    <h2>By payment type</h2>
    <table><thead><tr><th>Type</th><th class="num">Transactions</th><th class="num">Avg risk</th></tr></thead><tbody>
      <c:forEach items="${byType}" var="e"><tr><td>${e.key}</td><td class="num"><fmt:formatNumber value="${e.value[0]}" maxFractionDigits="0"/></td>
        <td class="num"><fmt:formatNumber value="${e.value[1]}" maxFractionDigits="1"/></td></tr></c:forEach>
    </tbody></table>
  </section>
  <section class="panel">
    <h2>Highest average risk by location</h2>
    <table><thead><tr><th>Location</th><th class="num">Transactions</th><th class="num">Avg risk</th></tr></thead><tbody>
      <c:forEach items="${byLocation}" var="e"><tr><td><c:out value="${e.key}"/></td><td class="num"><fmt:formatNumber value="${e.value[0]}" maxFractionDigits="0"/></td>
        <td class="num"><fmt:formatNumber value="${e.value[1]}" maxFractionDigits="1"/></td></tr></c:forEach>
    </tbody></table>
  </section>
</div>

<section class="panel">
  <h2>Weka model evaluation</h2>
  <c:choose>
    <c:when test="${modelReport.ready}">
      <p>J48 decision tree trained on the synthetic dataset <span class="mono">data/transactions.csv</span>: ${modelReport.rows} rows
        (${modelReport.fraudRows} fraud). Trained at ${modelReport.trainedAtFormatted}. These figures are measured on synthetic data and say nothing about real-world accuracy.</p>
      <table><thead><tr><th>Evaluation</th><th class="num">Accuracy %</th><th class="num">Fraud precision %</th><th class="num">Fraud recall %</th><th class="num">Fraud F1 %</th></tr></thead><tbody>
        <tr><td>Hold-out test (${modelReport.trainRows} train / ${modelReport.testRows} test)</td>
          <td class="num"><fmt:formatNumber value="${modelReport.holdoutAccuracy}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.holdoutPrecision}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.holdoutRecall}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.holdoutF1}" maxFractionDigits="1"/></td></tr>
        <tr><td>10-fold cross-validation</td>
          <td class="num"><fmt:formatNumber value="${modelReport.cvAccuracy}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.cvPrecision}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.cvRecall}" maxFractionDigits="1"/></td>
          <td class="num"><fmt:formatNumber value="${modelReport.cvF1}" maxFractionDigits="1"/></td></tr>
      </tbody></table>
      <p class="muted">Hold-out confusion counts: <c:out value="${modelReport.confusion}"/> &middot; cross-validated ROC area <fmt:formatNumber value="${modelReport.cvAuc}" maxFractionDigits="3"/></p>
      <details><summary>Show the learned decision tree</summary><pre class="tree"><c:out value="${modelReport.treeText}"/></pre></details>
    </c:when>
    <c:otherwise><p class="empty">Model status: ${modelReport.status}. <c:out value="${modelReport.error}"/></p></c:otherwise>
  </c:choose>
</section>
<%@ include file="fragments/footer.jspf" %>

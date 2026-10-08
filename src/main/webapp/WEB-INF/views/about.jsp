<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>About FraudShield AI</h1><p>An Explainable AI-Powered UPI &amp; Digital Payment Fraud Detection System</p></div></div>
<section class="panel">
  <h2>What it does</h2>
  <p>FraudShield AI simulates a digital-payment fraud desk. Each transaction is compared with the customer's own history, scored by transparent rules, checked by a Weka machine-learning model, and returned with a 0-100 risk score and the exact reasons behind it. It uses synthetic data only and never connects to a real UPI or banking system.</p>
</section>
<div class="grid grid-2">
  <section class="panel">
    <h2>Risk score</h2>
    <table><thead><tr><th>Factor</th><th class="num">Weight</th></tr></thead><tbody>
      <tr><td>Amount anomaly</td><td class="num">25%</td></tr><tr><td>Frequency anomaly</td><td class="num">20%</td></tr>
      <tr><td>Time anomaly</td><td class="num">15%</td></tr><tr><td>Device anomaly</td><td class="num">15%</td></tr>
      <tr><td>Location anomaly</td><td class="num">15%</td></tr><tr><td>Behaviour deviation</td><td class="num">10%</td></tr></tbody></table>
    <p class="muted">Final = 75% rule score + 25% Weka fraud probability. Levels: 0-30 LOW, 31-60 MEDIUM, 61-80 HIGH, 81-100 CRITICAL.</p>
  </section>
  <section class="panel">
    <h2>Architecture</h2>
    <p>Browser &rarr; JSP &rarr; Servlet &rarr; Service layer &rarr; Fraud engine (rules + Weka) &rarr; DAO &rarr; JDBC &rarr; MySQL.</p>
    <p><strong>Stack:</strong> Core Java 17, Servlets, JSP/JSTL, JDBC, MySQL, Weka, Maven, Tomcat 9.</p>
    <p><strong>ML model:</strong>
      <c:choose><c:when test="${modelReport.ready}">Weka J48, trained on ${modelReport.rows} synthetic rows.</c:when><c:otherwise>${modelReport.status}</c:otherwise></c:choose></p>
  </section>
</div>
<%@ include file="fragments/footer.jspf" %>

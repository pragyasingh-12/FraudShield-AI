<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sign in | FraudShield AI</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="login-wrap">
  <section class="login-side">
    <h1>FraudShield AI</h1>
    <p>An explainable fraud detection console for UPI and digital payments.</p>
    <ul>
      <li>Every flagged payment comes with the reasons behind its risk score</li>
      <li>Rules, customer behaviour and a Weka machine-learning model work together</li>
      <li>Academic simulation: synthetic data only, no real banking connection</li>
    </ul>
  </section>
  <section class="login-main">
    <form class="login-card" method="post" action="${pageContext.request.contextPath}/login">
      <h2>Sign in</h2>
      <c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
      <p><label for="email">E-mail</label>
         <input id="email" name="email" type="email" required autofocus value="<c:out value='${email}'/>"></p>
      <p><label for="password">Password</label>
         <input id="password" name="password" type="password" required></p>
      <button class="btn" type="submit">Sign in</button>
      <div class="demo-box">
        <strong>Demo accounts</strong><br>
        Admin: admin@fraudshield.local / Admin@123<br>
        Analyst: analyst@fraudshield.local / Analyst@123
      </div>
    </form>
  </section>
</div>
</body>
</html>

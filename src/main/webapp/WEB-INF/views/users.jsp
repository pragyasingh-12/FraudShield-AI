<%@ include file="fragments/header.jspf" %>
<div class="page-head"><div><h1>Users</h1><p>Console staff and monitored customer accounts</p></div></div>
<section class="panel">
  <div class="table-wrap"><table>
    <thead><tr><th>ID</th><th>Name</th><th>E-mail</th><th>Phone</th><th>Role</th><th>Created</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${users}" var="u">
      <tr>
        <td>${u.id}</td>
        <td><c:choose><c:when test="${u.role == 'CUSTOMER'}"><a href="${ctx}/users?id=${u.id}"><c:out value="${u.name}"/></a></c:when><c:otherwise><c:out value="${u.name}"/></c:otherwise></c:choose></td>
        <td><c:out value="${u.email}"/></td><td><c:out value="${u.phone}"/></td>
        <td><span class="badge badge-pending">${u.role}</span></td>
        <td>${u.createdAt}</td>
        <td><c:if test="${sessionScope.user.admin and u.id != sessionScope.user.id}">
          <form method="post" action="${ctx}/users" class="inline-form" data-confirm="Delete ${u.name} and all of their transactions?">
            <%@ include file="fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${u.id}">
            <button class="btn btn-danger btn-small" type="submit">Delete</button></form></c:if></td>
      </tr>
    </c:forEach>
    </tbody></table></div>
</section>
<c:if test="${sessionScope.user.admin}">
<section class="panel">
  <h2>Add user</h2>
  <form method="post" action="${ctx}/users">
    <%@ include file="fragments/csrf.jspf" %>
    <input type="hidden" name="action" value="add">
    <div class="form-grid">
      <div><label for="name">Name</label><input id="name" name="name" required maxlength="100"></div>
      <div><label for="email">E-mail</label><input id="email" name="email" type="email" required></div>
      <div><label for="phone">Phone</label><input id="phone" name="phone" maxlength="15"></div>
      <div><label for="role">Role</label><select id="role" name="role"><option>CUSTOMER</option><option>ANALYST</option><option>ADMIN</option></select></div>
      <div><label for="password">Password (min 8 characters)</label><input id="password" name="password" type="password" minlength="8" required></div>
    </div>
    <div class="actions"><button class="btn" type="submit">Create user</button></div>
  </form>
</section>
</c:if>
<%@ include file="fragments/footer.jspf" %>

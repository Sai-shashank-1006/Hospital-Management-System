<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Staff accounts — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Staff accounts</h1>
    <p>Who can sign in, and what each role may reach.</p>
  </div>
  <a class="btn" href="${ctx}/users/new">Add account</a>
</div>

<div class="card">
  <div class="table-scroll">
    <table>
      <thead>
      <tr>
        <th>Name</th>
        <th>Username</th>
        <th>Role</th>
        <th>Status</th>
        <th>Last sign-in</th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <c:forEach var="u" items="${users}">
        <tr>
          <td>
            <strong><c:out value="${u.fullName}" /></strong>
            <c:if test="${u.id == currentUser.id}">
              <span class="badge badge-scheduled">You</span>
            </c:if>
          </td>
          <td class="num"><c:out value="${u.username}" /></td>
          <td><c:out value="${u.role.label}" /></td>
          <td>
            <span class="badge ${u.active ? 'badge-yes' : 'badge-no'}">
              ${u.active ? 'Active' : 'Disabled'}
            </span>
          </td>
          <td class="num muted"><c:out value="${u.formattedLastLogin}" /></td>
          <td>
            <div class="actions">
              <a class="btn-link" href="${ctx}/users/edit?id=${u.id}">Edit</a>
              <c:if test="${u.id != currentUser.id}">
                <form class="inline-form" method="post" action="${ctx}/users/delete"
                      data-confirm="Delete the account for ${u.fullName}?">
                  <input type="hidden" name="csrfToken" value="${csrfToken}">
                  <input type="hidden" name="id" value="${u.id}">
                  <button class="btn-link danger" type="submit">Delete</button>
                </form>
              </c:if>
            </div>
          </td>
        </tr>
      </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<h2 class="section-title">What each role may reach</h2>

<div class="card">
  <div class="table-scroll">
    <table>
      <thead>
      <tr>
        <th>Role</th>
        <th>Access</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td><strong>Administrator</strong></td>
        <td class="muted">Everything, including staff accounts and the doctor roster.</td>
      </tr>
      <tr>
        <td><strong>Doctor</strong></td>
        <td class="muted">
          Patients, appointments, prescriptions and reports. No billing,
          no doctor roster, no staff accounts. Sees only their own prescriptions.
        </td>
      </tr>
      <tr>
        <td><strong>Receptionist</strong></td>
        <td class="muted">
          Patients, appointments, billing and reports. No prescriptions,
          because prescribing is a medical act.
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</div>

<%@ include file="fragments/footer.jspf" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Patients — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Patients</h1>
    <p>Registered patient records.</p>
  </div>
  <a class="btn" href="${ctx}/patients/new">Register patient</a>
</div>

<div class="card">

  <form class="searchbar" method="get" action="${ctx}/patients/">
    <input type="search" name="q" value="<c:out value='${search}' />"
           placeholder="Search by name, phone or email" aria-label="Search patients">
    <button class="btn btn-secondary" type="submit">Search</button>
    <c:if test="${not empty search}">
      <a class="btn btn-secondary" href="${ctx}/patients/">Clear</a>
    </c:if>
  </form>

  <c:choose>
    <c:when test="${empty patients}">
      <div class="empty">
        <strong>No patients found</strong>
        <c:choose>
          <c:when test="${not empty search}">Nothing matched &ldquo;<c:out value="${search}" />&rdquo;.</c:when>
          <c:otherwise>Register the first patient to get started.</c:otherwise>
        </c:choose>
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>#</th>
            <th>Name</th>
            <th>Gender</th>
            <th>Age</th>
            <th>Date of birth</th>
            <th>Phone</th>
            <th>Blood group</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="p" items="${patients}">
            <tr>
              <td class="num muted">${p.id}</td>
              <td>
                <strong><c:out value="${p.fullName}" /></strong>
                <c:if test="${not empty p.email}">
                  <div class="muted"><c:out value="${p.email}" /></div>
                </c:if>
              </td>
              <td><c:out value="${p.gender}" /></td>
              <td class="num"><c:out value="${empty p.age ? '--' : p.age}" /></td>
              <td class="num"><c:out value="${empty p.formattedDob ? '--' : p.formattedDob}" /></td>
              <td class="num"><c:out value="${empty p.phone ? '--' : p.phone}" /></td>
              <td><c:out value="${empty p.bloodGroup ? '--' : p.bloodGroup}" /></td>
              <td>
                <div class="actions">
                  <a class="btn-link" href="${ctx}/patients/edit?id=${p.id}">Edit</a>
                  <form class="inline-form" method="post" action="${ctx}/patients/delete"
                        data-confirm="Delete this patient? Their appointments, prescriptions and invoices will be removed too.">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${p.id}">
                    <button class="btn-link danger" type="submit">Delete</button>
                  </form>
                </div>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</div>

<%@ include file="fragments/footer.jspf" %>

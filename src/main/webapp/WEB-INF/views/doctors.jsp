<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Doctors — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Doctors</h1>
    <p>Consulting roster. Only doctors marked available can be booked.</p>
  </div>
  <a class="btn" href="${ctx}/doctors/new">Add doctor</a>
</div>

<div class="card">
  <c:choose>
    <c:when test="${empty doctors}">
      <div class="empty">
        <strong>No doctors on the roster</strong>
        Add a doctor before booking appointments.
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>#</th>
            <th>Name</th>
            <th>Specialization</th>
            <th>Contact</th>
            <th>Fee</th>
            <th>Available</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="d" items="${doctors}">
            <tr>
              <td class="num muted">${d.id}</td>
              <td><strong>Dr. <c:out value="${d.fullName}" /></strong></td>
              <td><c:out value="${d.specialization}" /></td>
              <td>
                <c:out value="${empty d.phone ? '--' : d.phone}" />
                <c:if test="${not empty d.email}">
                  <div class="muted"><c:out value="${d.email}" /></div>
                </c:if>
              </td>
              <td class="num">${d.consultationFee}</td>
              <td>
                <span class="badge ${d.available ? 'badge-yes' : 'badge-no'}">
                  ${d.available ? 'Available' : 'Off duty'}
                </span>
              </td>
              <td>
                <div class="actions">
                  <a class="btn-link" href="${ctx}/doctors/edit?id=${d.id}">Edit</a>
                  <form class="inline-form" method="post" action="${ctx}/doctors/delete"
                        data-confirm="Remove this doctor? Their appointments will be removed too.">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${d.id}">
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

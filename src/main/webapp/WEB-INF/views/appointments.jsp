<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Appointments - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Appointments</h1>
    <p>All bookings, most recent first.</p>
  </div>
  <a class="btn" href="${ctx}/appointments/new">Book appointment</a>
</div>

<div class="card">
  <c:choose>
    <c:when test="${empty appointments}">
      <div class="empty">
        <strong>No appointments yet</strong>
        Book one once at least one patient and one available doctor exist.
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>#</th>
            <th>When</th>
            <th>Patient</th>
            <th>Doctor</th>
            <th>Reason</th>
            <th>Status</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="a" items="${appointments}">
            <tr>
              <td class="num muted">${a.id}</td>
              <td class="num"><c:out value="${a.formattedTime}" /></td>
              <td><c:out value="${a.patientName}" /></td>
              <td>
                Dr. <c:out value="${a.doctorName}" />
                <div class="muted"><c:out value="${a.doctorSpecialization}" /></div>
              </td>
              <td class="muted"><c:out value="${empty a.reason ? '--' : a.reason}" /></td>
              <td>
                <c:set var="badgeClass" value="badge-cancelled" />
                <c:if test="${a.status == 'SCHEDULED'}"><c:set var="badgeClass" value="badge-scheduled" /></c:if>
                <c:if test="${a.status == 'COMPLETED'}"><c:set var="badgeClass" value="badge-completed" /></c:if>
                <span class="badge ${badgeClass}"><c:out value="${a.status}" /></span>
              </td>
              <td>
                <div class="actions">
                  <c:if test="${a.status == 'SCHEDULED'}">
                    <form class="inline-form" method="post" action="${ctx}/appointments/status">
                      <input type="hidden" name="csrfToken" value="${csrfToken}">
                      <input type="hidden" name="id" value="${a.id}">
                      <input type="hidden" name="status" value="COMPLETED">
                      <button class="btn-link" type="submit">Complete</button>
                    </form>
                    <form class="inline-form" method="post" action="${ctx}/appointments/status">
                      <input type="hidden" name="csrfToken" value="${csrfToken}">
                      <input type="hidden" name="id" value="${a.id}">
                      <input type="hidden" name="status" value="CANCELLED">
                      <button class="btn-link danger" type="submit">Cancel</button>
                    </form>
                  </c:if>
                  <a class="btn-link" href="${ctx}/appointments/edit?id=${a.id}">Edit</a>
                  <form class="inline-form" method="post" action="${ctx}/appointments/delete"
                        data-confirm="Delete this appointment?">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${a.id}">
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

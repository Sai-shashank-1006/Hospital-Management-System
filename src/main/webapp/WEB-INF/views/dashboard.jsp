<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dashboard — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Welcome back, <c:out value="${currentUser.fullName}" /></h1>
    <p>Signed in as <c:out value="${currentUser.role.label}" />.</p>
  </div>
  <c:if test="${currentUser.role.canAccess('/appointments')}">
    <a class="btn" href="${ctx}/appointments/new">Book appointment</a>
  </c:if>
</div>

<div class="stats">
  <div class="stat">
    <div class="label">Patients</div>
    <div class="value">${patientCount}</div>
  </div>
  <div class="stat">
    <div class="label">Doctors</div>
    <div class="value">${doctorCount}</div>
  </div>
  <div class="stat">
    <div class="label">Appointments</div>
    <div class="value">${appointmentCount}</div>
  </div>
  <div class="stat">
    <div class="label">Scheduled</div>
    <div class="value">${scheduledCount}</div>
  </div>

  <%-- Billing and prescribing figures appear only for roles that can open
       those areas, so the dashboard never shows a number you cannot follow. --%>
  <c:if test="${not empty prescriptionCount}">
    <div class="stat">
      <div class="label">Prescriptions</div>
      <div class="value">${prescriptionCount}</div>
    </div>
  </c:if>
  <c:if test="${not empty outstandingTotal}">
    <div class="stat">
      <div class="label">Outstanding</div>
      <div class="value">&#8377;${outstandingTotal}</div>
      <div class="stat-note">${unpaidCount} unpaid invoice${unpaidCount == 1 ? '' : 's'}</div>
    </div>
  </c:if>
</div>

<c:if test="${not empty myUpcoming}">
  <h2 class="section-title">Your next appointments</h2>
  <div class="card">
    <div class="table-scroll">
      <table>
        <thead>
        <tr>
          <th>When</th>
          <th>Patient</th>
          <th>Reason</th>
          <th></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${myUpcoming}">
          <tr>
            <td class="num"><c:out value="${a.formattedTime}" /></td>
            <td><c:out value="${a.patientName}" /></td>
            <td class="muted"><c:out value="${empty a.reason ? '--' : a.reason}" /></td>
            <td>
              <c:if test="${currentUser.role.canAccess('/prescriptions')}">
                <a class="btn-link" href="${ctx}/prescriptions/new">Write prescription</a>
              </c:if>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </div>
</c:if>

<h2 class="section-title">Next appointments across the hospital</h2>

<div class="card">
  <c:choose>
    <c:when test="${empty upcoming}">
      <div class="empty">
        <strong>Nothing scheduled</strong>
        Booked appointments with a future date will appear here.
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>When</th>
            <th>Patient</th>
            <th>Doctor</th>
            <th>Reason</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="a" items="${upcoming}">
            <tr>
              <td class="num"><c:out value="${a.formattedTime}" /></td>
              <td><c:out value="${a.patientName}" /></td>
              <td>
                Dr. <c:out value="${a.doctorName}" />
                <div class="muted"><c:out value="${a.doctorSpecialization}" /></div>
              </td>
              <td class="muted"><c:out value="${empty a.reason ? '--' : a.reason}" /></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>
</div>

<%@ include file="fragments/footer.jspf" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dashboard - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Dashboard</h1>
    <p>Overview of registered patients, the doctor roster and appointment activity.</p>
  </div>
  <a class="btn" href="${ctx}/appointments/new">Book appointment</a>
</div>

<div class="stats">
  <div class="stat">
    <div class="label">Patients</div>
    <div class="value num">${patientCount}</div>
  </div>
  <div class="stat">
    <div class="label">Doctors</div>
    <div class="value num">${doctorCount}</div>
  </div>
  <div class="stat">
    <div class="label">Appointments</div>
    <div class="value num">${appointmentCount}</div>
  </div>
  <div class="stat">
    <div class="label">Scheduled</div>
    <div class="value num">${scheduledCount}</div>
  </div>
</div>

<h2 class="section-title">Next appointments</h2>

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
                <c:out value="${a.doctorName}" />
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

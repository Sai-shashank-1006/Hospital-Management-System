<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${appointment.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit appointment' : 'Book appointment'} — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit appointment' : 'Book appointment'}</h1>
    <p>A doctor cannot hold two active appointments at the same time.</p>
  </div>
</div>

<c:if test="${empty patients or empty doctors}">
  <div class="alert alert-error">
    <strong>Cannot book yet.</strong>
    <ul>
      <c:if test="${empty patients}">
        <li>No patients are registered &mdash; <a href="${ctx}/patients/new">register one first</a>.</li>
      </c:if>
      <c:if test="${empty doctors}">
        <li>No doctors are marked available &mdash; <a href="${ctx}/doctors/">check the roster</a>.</li>
      </c:if>
    </ul>
  </div>
</c:if>

<div class="card card-pad">
  <form method="post" action="${ctx}/appointments/save">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="id" value="${appointment.id}">

    <div class="form-grid">

      <div class="field">
        <label for="patientId">Patient <span class="req">*</span></label>
        <select id="patientId" name="patientId" required>
          <option value="">Select a patient...</option>
          <c:forEach var="p" items="${patients}">
            <option value="${p.id}" ${appointment.patientId == p.id ? 'selected' : ''}>
              <c:out value="${p.fullName}" />
              <c:if test="${not empty p.phone}"> (<c:out value="${p.phone}" />)</c:if>
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="doctorId">Doctor <span class="req">*</span></label>
        <select id="doctorId" name="doctorId" required>
          <option value="">Select a doctor...</option>
          <c:forEach var="d" items="${doctors}">
            <option value="${d.id}" ${appointment.doctorId == d.id ? 'selected' : ''}>
              Dr. <c:out value="${d.fullName}" /> &mdash; <c:out value="${d.specialization}" />
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="appointmentTime">Date and time <span class="req">*</span></label>
        <input type="datetime-local" id="appointmentTime" name="appointmentTime" required
               value="${appointment.inputValue}">
      </div>

      <c:if test="${editing}">
        <div class="field">
          <label for="status">Status</label>
          <select id="status" name="status">
            <c:forEach var="s" items="${['SCHEDULED', 'COMPLETED', 'CANCELLED']}">
              <option value="${s}" ${appointment.status == s ? 'selected' : ''}>${s}</option>
            </c:forEach>
          </select>
        </div>
      </c:if>

      <div class="field full">
        <label for="reason">Reason for visit</label>
        <textarea id="reason" name="reason" maxlength="255"><c:out value="${appointment.reason}" /></textarea>
      </div>

    </div>

    <div class="form-actions">
      <button class="btn" type="submit" ${(empty patients or empty doctors) ? 'disabled' : ''}>
        ${editing ? 'Save changes' : 'Book appointment'}
      </button>
      <a class="btn btn-secondary" href="${ctx}/appointments/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

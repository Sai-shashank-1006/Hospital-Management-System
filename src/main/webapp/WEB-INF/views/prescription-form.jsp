<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${prescription.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit prescription' : 'Write prescription'} — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit prescription' : 'Write prescription'}</h1>
    <p>Fields marked <span class="req">*</span> are required.</p>
  </div>
</div>

<div class="card card-pad">
  <form method="post" action="${ctx}/prescriptions/save">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="id" value="${prescription.id}">

    <div class="form-grid">

      <div class="field">
        <label for="patientId">Patient <span class="req">*</span></label>
        <select id="patientId" name="patientId" required>
          <option value="">Select a patient...</option>
          <c:forEach var="p" items="${patients}">
            <option value="${p.id}" ${prescription.patientId == p.id ? 'selected' : ''}>
              <c:out value="${p.fullName}" />
              <c:if test="${not empty p.allergies}"> - allergic to <c:out value="${p.allergies}" /></c:if>
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="doctorId">Prescribing doctor <span class="req">*</span></label>
        <select id="doctorId" name="doctorId" required>
          <option value="">Select a doctor...</option>
          <c:forEach var="d" items="${doctors}">
            <option value="${d.id}" ${prescription.doctorId == d.id ? 'selected' : ''}>
              Dr. <c:out value="${d.fullName}" /> &mdash; <c:out value="${d.specialization}" />
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="issuedAt">Issued</label>
        <input type="datetime-local" id="issuedAt" name="issuedAt"
               value="${prescription.inputValue}">
      </div>

      <div class="field">
        <label for="appointmentId">Linked appointment</label>
        <select id="appointmentId" name="appointmentId">
          <option value="">Not linked</option>
          <c:forEach var="a" items="${appointments}">
            <option value="${a.id}" ${prescription.appointmentId == a.id ? 'selected' : ''}>
              #${a.id} &mdash; <c:out value="${a.patientName}" /> with Dr. <c:out value="${a.doctorName}" />
              (<c:out value="${a.formattedTime}" />)
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field full">
        <label for="diagnosis">Diagnosis <span class="req">*</span></label>
        <input type="text" id="diagnosis" name="diagnosis" required maxlength="255"
               value="<c:out value='${prescription.diagnosis}' />">
      </div>

    </div>

    <h2 class="section-title">Medicines <span class="req">*</span></h2>
    <p class="hint">
      Rows left blank are ignored. At least one medicine is required.
    </p>

    <div class="table-scroll">
      <table class="item-table">
        <thead>
        <tr>
          <th style="width: 26%">Medicine</th>
          <th style="width: 14%">Dosage</th>
          <th style="width: 16%">Frequency</th>
          <th style="width: 14%">Duration</th>
          <th>Instructions</th>
        </tr>
        </thead>
        <tbody id="medicineRows">
        <%-- Existing medicines, then spare blank rows so the form is usable
             even with JavaScript unavailable. --%>
        <c:forEach var="item" items="${prescription.items}">
          <tr>
            <td><input type="text" name="itemMedicine" maxlength="120"
                       value="<c:out value='${item.medicine}' />"></td>
            <td><input type="text" name="itemDosage" maxlength="60"
                       value="<c:out value='${item.dosage}' />"></td>
            <td><input type="text" name="itemFrequency" maxlength="60"
                       value="<c:out value='${item.frequency}' />"></td>
            <td><input type="text" name="itemDuration" maxlength="60"
                       value="<c:out value='${item.duration}' />"></td>
            <td><input type="text" name="itemInstructions" maxlength="255"
                       value="<c:out value='${item.instructions}' />"></td>
          </tr>
        </c:forEach>
        <c:forEach begin="1" end="4">
          <tr>
            <td><input type="text" name="itemMedicine" maxlength="120" placeholder="Medicine"></td>
            <td><input type="text" name="itemDosage" maxlength="60" placeholder="500 mg"></td>
            <td><input type="text" name="itemFrequency" maxlength="60" placeholder="Twice daily"></td>
            <td><input type="text" name="itemDuration" maxlength="60" placeholder="5 days"></td>
            <td><input type="text" name="itemInstructions" maxlength="255" placeholder="After food"></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>

    <p>
      <button type="button" class="btn btn-secondary btn-sm" data-add-row="medicineRows">
        Add another row
      </button>
    </p>

    <div class="form-grid">
      <div class="field full">
        <label for="notes">Notes</label>
        <textarea id="notes" name="notes" rows="3"><c:out value="${prescription.notes}" /></textarea>
      </div>
    </div>

    <div class="form-actions">
      <button class="btn" type="submit">${editing ? 'Save changes' : 'Save prescription'}</button>
      <a class="btn btn-secondary" href="${ctx}/prescriptions/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

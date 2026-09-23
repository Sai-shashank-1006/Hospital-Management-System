<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${doctor.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit doctor' : 'Add doctor'} - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit doctor' : 'Add doctor'}</h1>
    <p>Fields marked <span class="req">*</span> are required.</p>
  </div>
</div>

<div class="card card-pad">
  <form method="post" action="${ctx}/doctors/save">
    <input type="hidden" name="id" value="${doctor.id}">

    <div class="form-grid">

      <div class="field">
        <label for="fullName">Full name <span class="req">*</span></label>
        <input type="text" id="fullName" name="fullName" required maxlength="120"
               value="<c:out value='${doctor.fullName}' />">
      </div>

      <div class="field">
        <label for="specialization">Specialization <span class="req">*</span></label>
        <input type="text" id="specialization" name="specialization" required maxlength="80"
               list="specializations" value="<c:out value='${doctor.specialization}' />">
        <datalist id="specializations">
          <option value="General Medicine"></option>
          <option value="Cardiology"></option>
          <option value="Orthopaedics"></option>
          <option value="Paediatrics"></option>
          <option value="Dermatology"></option>
          <option value="Neurology"></option>
          <option value="ENT"></option>
          <option value="Radiology"></option>
        </datalist>
      </div>

      <div class="field">
        <label for="phone">Phone</label>
        <input type="tel" id="phone" name="phone" maxlength="20"
               value="<c:out value='${doctor.phone}' />">
      </div>

      <div class="field">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" maxlength="120"
               value="<c:out value='${doctor.email}' />">
      </div>

      <div class="field">
        <label for="consultationFee">Consultation fee</label>
        <input type="number" id="consultationFee" name="consultationFee"
               min="0" step="0.01" value="${doctor.consultationFee}">
      </div>

      <div class="field checkbox">
        <input type="checkbox" id="available" name="available" value="true"
               ${doctor.available ? 'checked' : ''}>
        <label for="available">Currently accepting appointments</label>
      </div>

    </div>

    <div class="form-actions">
      <button class="btn" type="submit">${editing ? 'Save changes' : 'Add doctor'}</button>
      <a class="btn btn-secondary" href="${ctx}/doctors/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${patient.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit patient' : 'Register patient'} - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit patient' : 'Register patient'}</h1>
    <p>Fields marked <span class="req">*</span> are required.</p>
  </div>
</div>

<div class="card card-pad">
  <form method="post" action="${ctx}/patients/save">
    <input type="hidden" name="id" value="${patient.id}">

    <div class="form-grid">

      <div class="field">
        <label for="fullName">Full name <span class="req">*</span></label>
        <input type="text" id="fullName" name="fullName" required maxlength="120"
               value="<c:out value='${patient.fullName}' />">
      </div>

      <div class="field">
        <label for="gender">Gender <span class="req">*</span></label>
        <select id="gender" name="gender" required>
          <option value="">Select...</option>
          <c:forEach var="g" items="${['Male', 'Female', 'Other']}">
            <option value="${g}" ${patient.gender == g ? 'selected' : ''}>${g}</option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="dateOfBirth">Date of birth</label>
        <input type="date" id="dateOfBirth" name="dateOfBirth" value="${patient.inputValue}">
      </div>

      <div class="field">
        <label for="bloodGroup">Blood group</label>
        <select id="bloodGroup" name="bloodGroup">
          <option value="">Unknown</option>
          <c:forEach var="bg" items="${['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']}">
            <option value="${bg}" ${patient.bloodGroup == bg ? 'selected' : ''}>${bg}</option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="phone">Phone</label>
        <input type="tel" id="phone" name="phone" maxlength="20"
               value="<c:out value='${patient.phone}' />">
        <span class="hint">Digits, spaces and + - ( ) only.</span>
      </div>

      <div class="field">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" maxlength="120"
               value="<c:out value='${patient.email}' />">
      </div>

      <div class="field full">
        <label for="address">Address</label>
        <textarea id="address" name="address" maxlength="255"><c:out value="${patient.address}" /></textarea>
      </div>

    </div>

    <div class="form-actions">
      <button class="btn" type="submit">${editing ? 'Save changes' : 'Register patient'}</button>
      <a class="btn btn-secondary" href="${ctx}/patients/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

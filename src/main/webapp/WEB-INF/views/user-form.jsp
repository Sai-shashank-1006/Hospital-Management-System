<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${user.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit account' : 'Add account'} — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit account' : 'Add account'}</h1>
    <p>Fields marked <span class="req">*</span> are required.</p>
  </div>
</div>

<div class="card card-pad">
  <form method="post" action="${ctx}/users/save" autocomplete="off">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="id" value="${user.id}">

    <div class="form-grid">

      <div class="field">
        <label for="fullName">Full name <span class="req">*</span></label>
        <input type="text" id="fullName" name="fullName" required maxlength="120"
               value="<c:out value='${user.fullName}' />">
      </div>

      <div class="field">
        <label for="username">Username <span class="req">*</span></label>
        <%-- The username identifies the account and is not editable afterwards,
             so past actions stay attributable to a stable name. --%>
        <input type="text" id="username" name="username" required maxlength="50"
               pattern="[a-zA-Z0-9._-]{3,50}"
               value="<c:out value='${user.username}' />"
               ${editing ? 'readonly' : ''}>
        <span class="hint">
          <c:choose>
            <c:when test="${editing}">Usernames cannot be changed.</c:when>
            <c:otherwise>Letters, digits, dot, underscore and hyphen. 3 to 50 characters.</c:otherwise>
          </c:choose>
        </span>
      </div>

      <div class="field">
        <label for="role">Role <span class="req">*</span></label>
        <select id="role" name="role" required>
          <option value="">Select a role...</option>
          <c:forEach var="r" items="${roles}">
            <option value="${r}" ${user.role == r ? 'selected' : ''}>${r.label}</option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="doctorId">Linked doctor</label>
        <select id="doctorId" name="doctorId">
          <option value="">Not linked</option>
          <c:forEach var="d" items="${doctors}">
            <option value="${d.id}" ${user.doctorId == d.id ? 'selected' : ''}>
              Dr. <c:out value="${d.fullName}" /> &mdash; <c:out value="${d.specialization}" />
            </option>
          </c:forEach>
        </select>
        <span class="hint">Required for a Doctor account, so their own prescriptions can be identified.</span>
      </div>

      <div class="field">
        <label for="password">
          Password <c:if test="${not editing}"><span class="req">*</span></c:if>
        </label>
        <input type="password" id="password" name="password"
               autocomplete="new-password" minlength="8"
               ${editing ? '' : 'required'}>
        <span class="hint">
          <c:choose>
            <c:when test="${editing}">Leave blank to keep the current password.</c:when>
            <c:otherwise>At least 8 characters.</c:otherwise>
          </c:choose>
        </span>
      </div>

      <div class="field checkbox">
        <input type="checkbox" id="active" name="active" value="true"
               ${user.id == 0 or user.active ? 'checked' : ''}>
        <label for="active">Account enabled</label>
      </div>

    </div>

    <div class="form-actions">
      <button class="btn" type="submit">${editing ? 'Save changes' : 'Create account'}</button>
      <a class="btn btn-secondary" href="${ctx}/users/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

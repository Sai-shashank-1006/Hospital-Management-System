<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="editing" value="${invoice.id > 0}" />
<c:set var="pageTitle" value="${editing ? 'Edit invoice' : 'New invoice'} - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>${editing ? 'Edit invoice' : 'New invoice'}</h1>
    <p>Totals are calculated from the charges below.</p>
  </div>
</div>

<c:if test="${not editing and not empty invoice.appointmentId}">
  <div class="alert alert-info">
    Generated from appointment #${invoice.appointmentId}. The consultation fee has
    been added as the first charge &mdash; add any procedures below.
  </div>
</c:if>

<div class="card card-pad">
  <form method="post" action="${ctx}/invoices/save">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <input type="hidden" name="id" value="${invoice.id}">
    <input type="hidden" name="invoiceNumber" value="<c:out value='${invoice.invoiceNumber}' />">

    <div class="form-grid">

      <div class="field">
        <label for="patientId">Patient <span class="req">*</span></label>
        <select id="patientId" name="patientId" required>
          <option value="">Select a patient...</option>
          <c:forEach var="p" items="${patients}">
            <option value="${p.id}" ${invoice.patientId == p.id ? 'selected' : ''}>
              <c:out value="${p.fullName}" />
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="appointmentId">Linked appointment</label>
        <select id="appointmentId" name="appointmentId">
          <option value="">Not linked</option>
          <c:forEach var="a" items="${appointments}">
            <option value="${a.id}" ${invoice.appointmentId == a.id ? 'selected' : ''}>
              #${a.id} &mdash; <c:out value="${a.patientName}" /> (<c:out value="${a.formattedTime}" />)
            </option>
          </c:forEach>
        </select>
      </div>

      <div class="field">
        <label for="issuedAt">Issued</label>
        <input type="datetime-local" id="issuedAt" name="issuedAt"
               value="${invoice.inputValue}">
      </div>

      <div class="field">
        <label for="taxPercent">Tax (%)</label>
        <input type="number" id="taxPercent" name="taxPercent"
               min="0" max="100" step="0.01" value="${invoice.taxPercent}">
      </div>

      <div class="field">
        <label for="status">Status</label>
        <select id="status" name="status">
          <c:forEach var="s" items="${['UNPAID', 'PAID', 'CANCELLED']}">
            <option value="${s}" ${invoice.status == s ? 'selected' : ''}>${s}</option>
          </c:forEach>
        </select>
      </div>

    </div>

    <h2 class="section-title">Charges <span class="req">*</span></h2>
    <p class="hint">Rows left blank are ignored. At least one charge is required.</p>

    <div class="table-scroll">
      <table class="item-table">
        <thead>
        <tr>
          <th style="width: 58%">Description</th>
          <th style="width: 14%">Quantity</th>
          <th>Unit price</th>
        </tr>
        </thead>
        <tbody id="chargeRows">
        <c:forEach var="item" items="${invoice.items}">
          <tr>
            <td><input type="text" name="itemDescription" maxlength="160"
                       value="<c:out value='${item.description}' />"></td>
            <td><input type="number" name="itemQuantity" min="1" step="1"
                       value="${item.quantity}"></td>
            <td><input type="number" name="itemUnitPrice" min="0" step="0.01"
                       value="${item.unitPrice}"></td>
          </tr>
        </c:forEach>
        <c:forEach begin="1" end="4">
          <tr>
            <td><input type="text" name="itemDescription" maxlength="160"
                       placeholder="Procedure or service"></td>
            <td><input type="number" name="itemQuantity" min="1" step="1" value="1"></td>
            <td><input type="number" name="itemUnitPrice" min="0" step="0.01" placeholder="0.00"></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>

    <p>
      <button type="button" class="btn btn-secondary btn-sm" data-add-row="chargeRows">
        Add another row
      </button>
    </p>

    <div class="form-grid">
      <div class="field full">
        <label for="notes">Notes</label>
        <input type="text" id="notes" name="notes" maxlength="255"
               value="<c:out value='${invoice.notes}' />">
      </div>
    </div>

    <div class="form-actions">
      <button class="btn" type="submit">${editing ? 'Save changes' : 'Create invoice'}</button>
      <a class="btn btn-secondary" href="${ctx}/invoices/">Cancel</a>
    </div>
  </form>
</div>

<%@ include file="fragments/footer.jspf" %>

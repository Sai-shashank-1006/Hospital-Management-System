<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Invoice - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Invoice <c:out value="${invoice.invoiceNumber}" /></h1>
    <p>Issued <c:out value="${invoice.formattedIssuedAt}" /></p>
  </div>
  <div class="actions">
    <a class="btn btn-secondary" href="${ctx}/invoices/">Back</a>
    <c:if test="${invoice.editable}">
      <a class="btn" href="${ctx}/invoices/edit?id=${invoice.id}">Edit</a>
    </c:if>
  </div>
</div>

<div class="card card-pad document">

  <div class="doc-head">
    <div>
      <div class="doc-label">Billed to</div>
      <div class="doc-value"><c:out value="${invoice.patientName}" /></div>
      <c:if test="${not empty patient.insuranceProvider}">
        <div class="muted">
          <c:out value="${patient.insuranceProvider}" />
          <c:if test="${not empty patient.insuranceNumber}">
            &mdash; <c:out value="${patient.insuranceNumber}" />
          </c:if>
        </div>
      </c:if>
    </div>
    <div>
      <div class="doc-label">Status</div>
      <c:set var="badgeClass" value="badge-cancelled" />
      <c:if test="${invoice.status == 'UNPAID'}"><c:set var="badgeClass" value="badge-scheduled" /></c:if>
      <c:if test="${invoice.status == 'PAID'}"><c:set var="badgeClass" value="badge-completed" /></c:if>
      <span class="badge ${badgeClass}"><c:out value="${invoice.status}" /></span>
      <c:if test="${not empty invoice.formattedPaidAt}">
        <div class="muted">Paid <c:out value="${invoice.formattedPaidAt}" /></div>
      </c:if>
    </div>
  </div>

  <div class="doc-section">
    <div class="table-scroll">
      <table>
        <thead>
        <tr>
          <th>Description</th>
          <th class="right">Qty</th>
          <th class="right">Unit price</th>
          <th class="right">Amount</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="item" items="${invoice.items}">
          <tr>
            <td><c:out value="${item.description}" /></td>
            <td class="num right">${item.quantity}</td>
            <td class="num right">&#8377;${item.unitPrice}</td>
            <td class="num right">&#8377;${item.amount}</td>
          </tr>
        </c:forEach>
        </tbody>
        <tfoot>
        <tr>
          <td colspan="3" class="right">Subtotal</td>
          <td class="num right">&#8377;${invoice.subtotal}</td>
        </tr>
        <tr>
          <td colspan="3" class="right muted">Tax (${invoice.taxPercent}%)</td>
          <td class="num right muted">&#8377;${invoice.taxAmount}</td>
        </tr>
        <tr class="total-row">
          <td colspan="3" class="right"><strong>Total</strong></td>
          <td class="num right"><strong>&#8377;${invoice.total}</strong></td>
        </tr>
        </tfoot>
      </table>
    </div>
  </div>

  <c:if test="${not empty invoice.notes}">
    <div class="doc-section">
      <div class="doc-label">Notes</div>
      <p><c:out value="${invoice.notes}" /></p>
    </div>
  </c:if>

</div>

<%@ include file="fragments/footer.jspf" %>

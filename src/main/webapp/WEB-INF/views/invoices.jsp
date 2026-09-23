<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Billing - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Billing</h1>
    <p>Invoices raised against patient visits and procedures.</p>
  </div>
  <a class="btn" href="${ctx}/invoices/new">New invoice</a>
</div>

<div class="stats stats-two">
  <div class="stat">
    <div class="label">Outstanding</div>
    <div class="value">&#8377;${outstandingTotal}</div>
  </div>
  <div class="stat">
    <div class="label">Collected</div>
    <div class="value">&#8377;${collectedTotal}</div>
  </div>
</div>

<div class="card">

  <form class="searchbar" method="get" action="${ctx}/invoices/">
    <label class="sr-only" for="status">Filter by status</label>
    <select id="status" name="status">
      <c:forEach var="s" items="${['ALL', 'UNPAID', 'PAID', 'CANCELLED']}">
        <option value="${s}" ${statusFilter == s ? 'selected' : ''}>
          ${s == 'ALL' ? 'All invoices' : s}
        </option>
      </c:forEach>
    </select>
    <button class="btn btn-secondary" type="submit">Filter</button>
  </form>

  <c:choose>
    <c:when test="${empty invoices}">
      <div class="empty">
        <strong>No invoices</strong>
        Raise one from scratch, or generate it from a completed appointment.
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>Number</th>
            <th>Issued</th>
            <th>Patient</th>
            <th>Items</th>
            <th class="right">Subtotal</th>
            <th class="right">Tax</th>
            <th class="right">Total</th>
            <th>Status</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="i" items="${invoices}">
            <tr>
              <td><strong><c:out value="${i.invoiceNumber}" /></strong></td>
              <td class="num"><c:out value="${i.formattedIssuedAt}" /></td>
              <td><c:out value="${i.patientName}" /></td>
              <td class="num muted">${i.itemCount}</td>
              <td class="num right">&#8377;${i.subtotal}</td>
              <td class="num right muted">&#8377;${i.taxAmount}</td>
              <td class="num right"><strong>&#8377;${i.total}</strong></td>
              <td>
                <c:set var="badgeClass" value="badge-cancelled" />
                <c:if test="${i.status == 'UNPAID'}"><c:set var="badgeClass" value="badge-scheduled" /></c:if>
                <c:if test="${i.status == 'PAID'}"><c:set var="badgeClass" value="badge-completed" /></c:if>
                <span class="badge ${badgeClass}"><c:out value="${i.status}" /></span>
              </td>
              <td>
                <div class="actions">
                  <a class="btn-link" href="${ctx}/invoices/view?id=${i.id}">View</a>

                  <c:if test="${i.status == 'UNPAID'}">
                    <form class="inline-form" method="post" action="${ctx}/invoices/status">
                      <input type="hidden" name="csrfToken" value="${csrfToken}">
                      <input type="hidden" name="id" value="${i.id}">
                      <input type="hidden" name="status" value="PAID">
                      <button class="btn-link" type="submit">Mark paid</button>
                    </form>
                  </c:if>

                  <c:if test="${i.editable}">
                    <a class="btn-link" href="${ctx}/invoices/edit?id=${i.id}">Edit</a>
                  </c:if>

                  <form class="inline-form" method="post" action="${ctx}/invoices/delete"
                        data-confirm="Delete invoice ${i.invoiceNumber}?">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${i.id}">
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

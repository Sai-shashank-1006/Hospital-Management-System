<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Prescriptions - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Prescriptions</h1>
    <p>
      <c:choose>
        <c:when test="${scopedToSelf}">Prescriptions you have written.</c:when>
        <c:otherwise>All prescriptions, most recent first.</c:otherwise>
      </c:choose>
    </p>
  </div>
  <a class="btn" href="${ctx}/prescriptions/new">Write prescription</a>
</div>

<div class="card">
  <c:choose>
    <c:when test="${empty prescriptions}">
      <div class="empty">
        <strong>No prescriptions yet</strong>
        A prescription records the diagnosis and the medicines given at a consultation.
      </div>
    </c:when>
    <c:otherwise>
      <div class="table-scroll">
        <table>
          <thead>
          <tr>
            <th>#</th>
            <th>Issued</th>
            <th>Patient</th>
            <th>Doctor</th>
            <th>Diagnosis</th>
            <th>Medicines</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="p" items="${prescriptions}">
            <tr>
              <td class="num muted">${p.id}</td>
              <td class="num"><c:out value="${p.formattedIssuedAt}" /></td>
              <td><c:out value="${p.patientName}" /></td>
              <td>
                Dr. <c:out value="${p.doctorName}" />
                <div class="muted"><c:out value="${p.doctorSpecialization}" /></div>
              </td>
              <td><c:out value="${empty p.diagnosis ? '--' : p.diagnosis}" /></td>
              <td class="num muted">${p.itemCount}</td>
              <td>
                <div class="actions">
                  <a class="btn-link" href="${ctx}/prescriptions/view?id=${p.id}">View</a>
                  <a class="btn-link" href="${ctx}/prescriptions/edit?id=${p.id}">Edit</a>
                  <form class="inline-form" method="post" action="${ctx}/prescriptions/delete"
                        data-confirm="Delete this prescription? This removes a clinical record.">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${p.id}">
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

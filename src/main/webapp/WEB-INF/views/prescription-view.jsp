<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Prescription - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Prescription #${prescription.id}</h1>
    <p>Issued <c:out value="${prescription.formattedIssuedAt}" /></p>
  </div>
  <div class="actions">
    <a class="btn btn-secondary" href="${ctx}/prescriptions/">Back</a>
    <a class="btn" href="${ctx}/prescriptions/edit?id=${prescription.id}">Edit</a>
  </div>
</div>

<div class="card card-pad document">

  <div class="doc-head">
    <div>
      <div class="doc-label">Patient</div>
      <div class="doc-value"><c:out value="${prescription.patientName}" /></div>
      <c:if test="${not empty patient.allergies}">
        <%-- Allergies belong next to the medicines, not buried in the record. --%>
        <div class="allergy-warning" role="alert">
          Allergies: <c:out value="${patient.allergies}" />
        </div>
      </c:if>
    </div>
    <div>
      <div class="doc-label">Prescribed by</div>
      <div class="doc-value">Dr. <c:out value="${prescription.doctorName}" /></div>
      <div class="muted"><c:out value="${prescription.doctorSpecialization}" /></div>
    </div>
  </div>

  <div class="doc-section">
    <div class="doc-label">Diagnosis</div>
    <p><c:out value="${empty prescription.diagnosis ? '--' : prescription.diagnosis}" /></p>
  </div>

  <div class="doc-section">
    <div class="doc-label">Medicines</div>
    <div class="table-scroll">
      <table>
        <thead>
        <tr>
          <th>Medicine</th>
          <th>Dosage</th>
          <th>Frequency</th>
          <th>Duration</th>
          <th>Instructions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="item" items="${prescription.items}">
          <tr>
            <td><strong><c:out value="${item.medicine}" /></strong></td>
            <td><c:out value="${empty item.dosage ? '--' : item.dosage}" /></td>
            <td><c:out value="${empty item.frequency ? '--' : item.frequency}" /></td>
            <td><c:out value="${empty item.duration ? '--' : item.duration}" /></td>
            <td class="muted"><c:out value="${empty item.instructions ? '--' : item.instructions}" /></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </div>

  <c:if test="${not empty prescription.notes}">
    <div class="doc-section">
      <div class="doc-label">Notes</div>
      <p><c:out value="${prescription.notes}" /></p>
    </div>
  </c:if>

</div>

<%@ include file="fragments/footer.jspf" %>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reports - Hospital Management System" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Reports &amp; analytics</h1>
    <p>Operational and clinical summary across the whole hospital.</p>
  </div>
</div>

<%-- Headline figures: four numbers, so stat tiles rather than a four-bar chart. --%>
<div class="stats">
  <div class="stat">
    <div class="label">Patients</div>
    <div class="value">${patientCount}</div>
  </div>
  <div class="stat">
    <div class="label">Prescriptions</div>
    <div class="value">${prescriptionCount}</div>
  </div>
  <div class="stat">
    <div class="label">Revenue collected</div>
    <div class="value">&#8377;${collectedTotal}</div>
  </div>
  <div class="stat">
    <div class="label">Outstanding</div>
    <div class="value">&#8377;${outstandingTotal}</div>
  </div>
</div>

<%--
  Appointments and revenue are two separate charts on purpose. Plotting a count
  and a currency amount against one shared axis would invite comparisons between
  two quantities that have nothing in common.
--%>
<div class="chart-grid">
  <div class="card card-pad">
    <c:set var="chartSeries" value="${appointmentsByMonth}" />
    <c:set var="chartTitle" value="Appointments by month" />
    <c:set var="chartNote" value="Last six months, all statuses." />
    <%@ include file="fragments/column-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${revenueByMonth}" />
    <c:set var="chartTitle" value="Revenue by month" />
    <c:set var="chartNote" value="Paid invoices only, including tax." />
    <%@ include file="fragments/column-chart.jspf" %>
  </div>
</div>

<h2 class="section-title">Appointments</h2>

<div class="chart-grid">
  <div class="card card-pad">
    <c:set var="chartSeries" value="${appointmentsByStatus}" />
    <c:set var="chartTitle" value="By status" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${appointmentsByDoctor}" />
    <c:set var="chartTitle" value="Busiest doctors" />
    <c:set var="chartNote" value="Top five by appointment count." />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${appointmentsBySpecialization}" />
    <c:set var="chartTitle" value="By specialization" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>
</div>

<h2 class="section-title">Patients</h2>

<div class="chart-grid">
  <div class="card card-pad">
    <c:set var="chartSeries" value="${patientsByGender}" />
    <c:set var="chartTitle" value="By gender" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${patientsByBloodGroup}" />
    <c:set var="chartTitle" value="By blood group" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${patientsByInsurance}" />
    <c:set var="chartTitle" value="Insurance on file" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>
</div>

<h2 class="section-title">Billing and prescribing</h2>

<div class="chart-grid">
  <div class="card card-pad">
    <c:set var="chartSeries" value="${invoicesByStatus}" />
    <c:set var="chartTitle" value="Invoices by status" />
    <c:set var="chartNote" value="" />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>

  <div class="card card-pad">
    <c:set var="chartSeries" value="${topMedicines}" />
    <c:set var="chartTitle" value="Most prescribed medicines" />
    <c:set var="chartNote" value="Top five by number of prescriptions." />
    <%@ include file="fragments/bar-chart.jspf" %>
  </div>
</div>

<%@ include file="fragments/footer.jspf" %>

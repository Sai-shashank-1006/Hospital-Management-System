<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Something went wrong — MediFlow" />
<%@ include file="fragments/header.jspf" %>

<div class="page-head">
  <div>
    <h1>Something went wrong</h1>
    <p>The request could not be completed.</p>
  </div>
</div>

<div class="card card-pad">
  <p>
    <c:choose>
      <c:when test="${requestScope['jakarta.servlet.error.status_code'] == 404}">
        The page you asked for does not exist.
      </c:when>
      <c:otherwise>
        The application hit an unexpected error. If this keeps happening, check the
        Tomcat logs and confirm the database is reachable at
        <code>${ctx}/health</code>.
      </c:otherwise>
    </c:choose>
  </p>

  <c:if test="${not empty requestScope['jakarta.servlet.error.message']}">
    <p class="muted"><c:out value="${requestScope['jakarta.servlet.error.message']}" /></p>
  </c:if>

  <div class="form-actions">
    <a class="btn" href="${ctx}/dashboard">Back to dashboard</a>
  </div>
</div>

<%@ include file="fragments/footer.jspf" %>

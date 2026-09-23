<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sign in - Hospital Management System</title>
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="login-body">

<main class="login-shell">

  <div class="login-card">
    <div class="login-head">
      <span class="brand-mark brand-mark-lg" aria-hidden="true">+</span>
      <h1>Hospital Management System</h1>
      <p>Sign in to continue.</p>
    </div>

    <c:if test="${not empty info}">
      <div class="alert alert-info" role="status"><c:out value="${info}" /></div>
    </c:if>
    <c:if test="${not empty error}">
      <div class="alert alert-error" role="alert"><c:out value="${error}" /></div>
    </c:if>

    <form method="post" action="${ctx}/login" autocomplete="on">
      <div class="field">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" required autofocus
               autocomplete="username" value="<c:out value='${username}' />">
      </div>

      <div class="field">
        <label for="password">Password</label>
        <input type="password" id="password" name="password" required
               autocomplete="current-password">
      </div>

      <button class="btn btn-block" type="submit">Sign in</button>
    </form>

    <%-- Development convenience. Remove this block before the system holds
         any real patient data. --%>
    <div class="login-hint">
      <strong>Demo accounts</strong>
      <table class="hint-table">
        <tr><td>admin</td><td>admin123</td><td class="muted">Administrator</td></tr>
        <tr><td>dr.menon</td><td>doctor123</td><td class="muted">Doctor</td></tr>
        <tr><td>reception</td><td>reception123</td><td class="muted">Receptionist</td></tr>
      </table>
    </div>
  </div>

  <p class="login-foot">
    Built with Maven, deployed by Jenkins to Apache Tomcat.
  </p>

</main>

</body>
</html>

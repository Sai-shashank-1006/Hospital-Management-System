<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#1f6feb">
  <title>Sign in &mdash; MediFlow</title>
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="auth-body">

<main class="auth-shell">

  <%-- Brand panel. Hidden below 1000px, where the form is the whole point
       and a decorative column would only push it off the screen. --%>
  <aside class="auth-aside">
    <div class="brand">
      <%-- Two marks on this page, so each needs its own gradient id. --%>
      <c:set var="logoVariant" value="aside" />
      <%@ include file="fragments/logo.jspf" %>
      <span class="brand-text">Medi<span class="flow">Flow</span></span>
    </div>

    <h2 class="auth-tagline">Care that flows,<br>records that keep up.</h2>
    <p class="auth-sub">
      One place for patients, consultations, prescriptions and billing &mdash;
      built for the people at the desk and the doctor at the bedside.
    </p>

    <ul class="auth-points">
      <li><span class="tick" aria-hidden="true">&#128101;</span> Patient records with history and insurance</li>
      <li><span class="tick" aria-hidden="true">&#128197;</span> Scheduling that refuses double bookings</li>
      <li><span class="tick" aria-hidden="true">&#129534;</span> Invoices generated straight from a visit</li>
      <li><span class="tick" aria-hidden="true">&#128274;</span> Role-based access for every member of staff</li>
    </ul>
  </aside>

  <section class="auth-main">

    <div class="brand auth-mobile-brand">
      <c:set var="logoVariant" value="mobile" />
      <%@ include file="fragments/logo.jspf" %>
      <span class="brand-text">Medi<span class="flow">Flow</span></span>
    </div>

    <h1>Welcome back</h1>
    <p class="lede">Sign in to continue to your dashboard.</p>

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
               autocomplete="username" placeholder="your.name"
               value="<c:out value='${username}' />">
      </div>

      <div class="field">
        <label for="password">Password</label>
        <input type="password" id="password" name="password" required
               autocomplete="current-password" placeholder="••••••••">
      </div>

      <button class="btn btn-block" type="submit">Sign in</button>
    </form>

    <%-- Development convenience. Remove this block before the system holds
         any real patient data. --%>
    <div class="demo-accounts">
      <strong>Demo accounts</strong>
      <table>
        <tr><td class="cred">admin</td><td class="cred">admin123</td><td class="muted">Administrator</td></tr>
        <tr><td class="cred">dr.menon</td><td class="cred">doctor123</td><td class="muted">Doctor</td></tr>
        <tr><td class="cred">reception</td><td class="cred">reception123</td><td class="muted">Receptionist</td></tr>
      </table>
    </div>

    <p class="auth-foot">
      Built with Maven &middot; deployed by Jenkins to Apache Tomcat
    </p>

  </section>

</main>

</body>
</html>

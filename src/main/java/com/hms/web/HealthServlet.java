package com.hms.web;

import com.hms.db.DataSourceProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/**
 * Liveness/readiness endpoint returning JSON.
 *
 * <p>The Jenkins pipeline polls this after deploying to confirm that Tomcat has
 * started the application <em>and</em> that it can reach MySQL, which catches a
 * deployment that unpacks successfully but is misconfigured.
 */
@WebServlet(name = "HealthServlet", urlPatterns = "/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        boolean dbUp;
        String detail;
        try (Connection c = DataSourceProvider.get().getConnection()) {
            dbUp = c.isValid(3);
            detail = dbUp ? "connected" : "connection invalid";
        } catch (Exception e) {
            dbUp = false;
            detail = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        resp.setStatus(dbUp ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.getWriter().printf(
                "{\"status\":\"%s\",\"database\":\"%s\",\"detail\":\"%s\"}%n",
                dbUp ? "UP" : "DOWN",
                dbUp ? "UP" : "DOWN",
                detail.replace("\"", "'"));
    }
}

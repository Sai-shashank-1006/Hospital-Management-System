package com.hms.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Shared request-parsing and view-dispatch helpers for the application servlets. */
public abstract class BaseServlet extends HttpServlet {

    protected static final String VIEW_PREFIX = "/WEB-INF/views/";

    /** Renders a JSP from {@code /WEB-INF/views}, which is not reachable by direct URL. */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEW_PREFIX + view + ".jsp").forward(req, resp);
    }

    /** Redirects to a path relative to the application context. */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected String trimmed(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    protected int intParam(HttpServletRequest req, String name, int fallback) {
        String value = trimmed(req, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    protected BigDecimal decimalParam(HttpServletRequest req, String name, BigDecimal fallback) {
        String value = trimmed(req, name);
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    protected LocalDate dateParam(HttpServletRequest req, String name) {
        String value = trimmed(req, name);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses the value of an {@code <input type="time">} field, e.g. "09:30". */
    protected LocalTime timeParam(HttpServletRequest req, String name) {
        String value = trimmed(req, name);
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value.length() > 5 ? value.substring(0, 5) : value);
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses the value of an {@code <input type="datetime-local">} field. */
    protected LocalDateTime dateTimeParam(HttpServletRequest req, String name) {
        String value = trimmed(req, name);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.length() == 16 ? value : value.substring(0, 16));
        } catch (Exception e) {
            return null;
        }
    }

    /** The part of the URL after the servlet mapping, normalised to "/" when absent. */
    protected String action(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        return (pathInfo == null || pathInfo.isEmpty()) ? "/" : pathInfo;
    }
}

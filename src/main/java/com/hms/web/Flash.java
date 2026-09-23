package com.hms.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * One-shot messages that survive the redirect after a successful POST.
 *
 * <p>Stored on the session and cleared by the layout JSP as soon as they are rendered,
 * so a browser refresh does not repeat them.
 */
public final class Flash {

    public static final String SUCCESS = "flashSuccess";
    public static final String ERROR = "flashError";

    private Flash() {
    }

    public static void success(HttpServletRequest req, String message) {
        req.getSession().setAttribute(SUCCESS, message);
    }

    public static void error(HttpServletRequest req, String message) {
        req.getSession().setAttribute(ERROR, message);
    }
}

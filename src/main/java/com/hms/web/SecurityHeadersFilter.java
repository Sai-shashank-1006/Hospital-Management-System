package com.hms.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds defensive response headers to every page.
 *
 * <p>These are defence in depth: the application already escapes its output, but a
 * Content-Security-Policy limits the damage if an escaping bug ever slips through.
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse resp = (HttpServletResponse) response;

        // script-src 'self' with no 'unsafe-inline': the only JavaScript is
        // /js/app.js, served from this origin. Inline handlers and injected
        // <script> blocks are therefore refused by the browser, which is what
        // makes this a real defence rather than decoration.
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                        + "script-src 'self'; frame-ancestors 'none'; form-action 'self'; "
                        + "base-uri 'self'; object-src 'none'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");

        chain.doFilter(request, response);
    }
}

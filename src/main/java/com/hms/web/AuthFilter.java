package com.hms.web;

import com.hms.model.User;
import com.hms.security.CsrfToken;
import com.hms.security.SessionUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * The single enforcement point for authentication, authorisation and CSRF.
 *
 * <p>Every request passes through here. Putting all three checks in one filter means a
 * new servlet is protected the moment it is added — there is no per-servlet annotation
 * to forget. Menu items are hidden by role for convenience, but this filter is what
 * actually decides.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** Reachable without signing in. */
    private static final List<String> PUBLIC_PATHS = List.of("/login", "/health");

    /**
     * Reachable by anyone who is signed in, whatever their role.
     *
     * <p>Sign-out is not public — it is a POST that must carry a CSRF token — but it
     * must also not be subject to the role check, or a role whose allow-list happens
     * not to mention it could not sign out at all.
     */
    private static final List<String> AUTHENTICATED_PATHS = List.of("/logout");

    /** Static assets, served before the session check so the login page can be styled. */
    private static final List<String> PUBLIC_PREFIXES = List.of("/css/", "/js/", "/images/");

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        User user = SessionUser.current(req);

        if (user == null) {
            // Remember where they were heading, so sign-in can return them there
            // instead of dumping everyone on the dashboard.
            String target = path + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
            req.getSession(true).setAttribute("redirectAfterLogin", target);

            if (isAjax(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login?expired=1");
            }
            return;
        }

        // CSRF: only state-changing methods need a token. GET must stay safe to replay.
        if (!SAFE_METHODS.contains(req.getMethod())) {
            String expected = SessionUser.csrfToken(req);
            String provided = req.getParameter("csrfToken");

            if (!CsrfToken.matches(expected, provided)) {
                log.warn("Rejected {} {} for user '{}': CSRF token missing or invalid",
                        req.getMethod(), path, user.getUsername());
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Your session could not be verified. Please reload the page and try again.");
                return;
            }
        }

        if (!AUTHENTICATED_PATHS.contains(path) && !user.getRole().canAccess(path)) {
            log.warn("Denied {} {} for user '{}' with role {}",
                    req.getMethod(), path, user.getUsername(), user.getRole());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Your role does not have access to this area.");
            return;
        }

        // Stop browsers caching authenticated pages, so Back after sign-out
        // does not redisplay patient data from the cache.
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return PUBLIC_PATHS.contains(path)
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isAjax(HttpServletRequest req) {
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }
}

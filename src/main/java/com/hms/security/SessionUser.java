package com.hms.security;

import com.hms.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads and writes the signed-in user on the session.
 *
 * <p>Only the {@link User} is stored; the session never holds a password hash, because
 * the object placed here is stripped of it first.
 */
public final class SessionUser {

    public static final String ATTRIBUTE = "currentUser";
    public static final String CSRF_TOKEN = "csrfToken";

    private SessionUser() {
    }

    /**
     * Establishes the signed-in session.
     *
     * <p>The existing session is invalidated and a new one started, which prevents
     * session fixation: an attacker who planted a known session id before sign-in
     * finds that id no longer valid afterwards.
     */
    public static void signIn(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }

        User safe = copyWithoutSecret(user);

        HttpSession session = req.getSession(true);
        session.setAttribute(ATTRIBUTE, safe);
        session.setAttribute(CSRF_TOKEN, CsrfToken.generate());
        session.setMaxInactiveInterval(30 * 60);
    }

    public static void signOut(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** The signed-in user, or {@code null} when nobody is signed in. */
    public static User current(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ATTRIBUTE);
        return (value instanceof User user) ? user : null;
    }

    public static String csrfToken(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(CSRF_TOKEN);
    }

    private static User copyWithoutSecret(User user) {
        User safe = new User();
        safe.setId(user.getId());
        safe.setUsername(user.getUsername());
        safe.setFullName(user.getFullName());
        safe.setRole(user.getRole());
        safe.setDoctorId(user.getDoctorId());
        safe.setActive(user.isActive());
        safe.setLastLoginAt(user.getLastLoginAt());
        // passwordHash deliberately omitted.
        return safe;
    }
}

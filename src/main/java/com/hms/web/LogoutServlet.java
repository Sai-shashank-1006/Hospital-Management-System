package com.hms.web;

import com.hms.model.User;
import com.hms.security.SessionUser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Ends the session.
 *
 * <p>Sign-out is a POST, not a link: a GET would let another site sign the user out
 * with an image tag, and browsers may prefetch links.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = SessionUser.current(req);
        if (user != null) {
            log.info("User '{}' signed out", user.getUsername());
        }

        SessionUser.signOut(req);
        redirect(req, resp, "/login");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        redirect(req, resp, "/login");
    }
}

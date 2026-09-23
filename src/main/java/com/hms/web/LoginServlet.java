package com.hms.web;

import com.hms.dao.UserDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.User;
import com.hms.security.PasswordHasher;
import com.hms.security.SessionUser;
import com.hms.security.UserBootstrap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (SessionUser.current(req) != null) {
            redirect(req, resp, "/dashboard");
            return;
        }

        // "expired" distinguishes a timed-out session from a first visit, so the
        // page can explain why the user suddenly landed here.
        if (req.getParameter("expired") != null) {
            req.setAttribute("info", "Your session has ended. Please sign in again.");
        }
        render(req, resp, "login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Retry the account bootstrap if it could not run at startup, for instance
        // because the database was still coming up. Without this, a stack whose
        // database was slow would present a sign-in page that nobody can get past.
        try {
            UserBootstrap.run(DataSourceProvider.get());
        } catch (RuntimeException e) {
            log.warn("Account bootstrap is still failing: {}", e.getMessage());
        }

        String username = trimmed(req, "username");
        String password = req.getParameter("password");

        Optional<User> found = username == null
                ? Optional.empty()
                : userDAO.findByUsername(username);

        // Deliberately identical message for unknown user, wrong password and
        // disabled account: distinguishing them would let an attacker enumerate
        // valid usernames.
        boolean ok = found.isPresent()
                && found.get().isActive()
                && PasswordHasher.matches(password, found.get().getPasswordHash());

        if (!ok) {
            log.warn("Failed sign-in attempt for username '{}' from {}",
                    username, req.getRemoteAddr());
            req.setAttribute("error", "Incorrect username or password.");
            req.setAttribute("username", username);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            render(req, resp, "login");
            return;
        }

        User user = found.get();

        // Capture the pre-login target before signIn() invalidates the session.
        String target = pendingTarget(req);

        SessionUser.signIn(req, user);
        userDAO.touchLastLogin(user.getId());

        log.info("User '{}' ({}) signed in", user.getUsername(), user.getRole());

        // Only follow the remembered target if this role may actually reach it.
        if (target != null && user.getRole().canAccess(stripQuery(target))) {
            redirect(req, resp, target);
        } else {
            redirect(req, resp, "/dashboard");
        }
    }

    private String pendingTarget(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("redirectAfterLogin");
        if (!(value instanceof String s)) {
            return null;
        }
        // Must be a path on this site. "//host" and "/\host" are browser-accepted
        // protocol-relative forms, so rejecting a bare "/" prefix is not enough.
        boolean sameSite = s.startsWith("/")
                && !s.startsWith("//")
                && !s.startsWith("/\\");
        return sameSite ? s : null;
    }

    private String stripQuery(String path) {
        int mark = path.indexOf('?');
        return mark < 0 ? path : path.substring(0, mark);
    }
}

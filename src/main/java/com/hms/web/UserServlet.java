package com.hms.web;

import com.hms.dao.DoctorDAO;
import com.hms.dao.UserDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Role;
import com.hms.model.User;
import com.hms.security.PasswordHasher;
import com.hms.security.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Staff account management. Reachable only by administrators, which
 * {@link com.hms.model.Role} enforces through {@link AuthFilter}.
 */
@WebServlet(name = "UserServlet", urlPatterns = "/users/*")
public class UserServlet extends BaseServlet {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private UserDAO userDAO;
    private DoctorDAO doctorDAO;

    @Override
    public void init() {
        userDAO = new UserDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "users");

        switch (action(req)) {
            case "/new" -> {
                req.setAttribute("user", new User());
                req.setAttribute("doctors", doctorDAO.findAll());
                req.setAttribute("roles", Role.values());
                render(req, resp, "user-form");
            }
            case "/edit" -> {
                Optional<User> found = userDAO.findById(intParam(req, "id", 0));
                if (found.isEmpty()) {
                    Flash.error(req, "That account no longer exists.");
                    redirect(req, resp, "/users/");
                    return;
                }
                req.setAttribute("user", found.get());
                req.setAttribute("doctors", doctorDAO.findAll());
                req.setAttribute("roles", Role.values());
                render(req, resp, "user-form");
            }
            default -> {
                req.setAttribute("users", userDAO.findAll());
                render(req, resp, "users");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "users");
        String action = action(req);
        User actor = SessionUser.current(req);

        if ("/delete".equals(action)) {
            deleteAccount(req, resp, actor);
            return;
        }

        if ("/password".equals(action)) {
            resetPassword(req, resp);
            return;
        }

        saveAccount(req, resp, actor);
    }

    private void deleteAccount(HttpServletRequest req, HttpServletResponse resp, User actor)
            throws IOException {

        int id = intParam(req, "id", 0);

        if (actor.getId() == id) {
            Flash.error(req, "You cannot delete the account you are signed in with.");
            redirect(req, resp, "/users/");
            return;
        }

        // Refuse to remove the last administrator, which would lock everyone out
        // of account management with no way back in through the interface.
        Optional<User> target = userDAO.findById(id);
        if (target.isPresent() && target.get().getRole() == Role.ADMIN
                && userDAO.countActiveAdmins() <= 1) {
            Flash.error(req, "This is the only administrator account. Create another before removing it.");
            redirect(req, resp, "/users/");
            return;
        }

        if (userDAO.delete(id)) {
            Flash.success(req, "Account deleted.");
        } else {
            Flash.error(req, "Could not delete that account.");
        }
        redirect(req, resp, "/users/");
    }

    private void resetPassword(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        int id = intParam(req, "id", 0);
        String password = req.getParameter("password");

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            Flash.error(req, "The password must be at least "
                    + MIN_PASSWORD_LENGTH + " characters.");
        } else if (userDAO.updatePassword(id, PasswordHasher.hash(password))) {
            Flash.success(req, "Password updated.");
        } else {
            Flash.error(req, "Could not update that password.");
        }
        redirect(req, resp, "/users/");
    }

    private void saveAccount(HttpServletRequest req, HttpServletResponse resp, User actor)
            throws ServletException, IOException {

        User user = bindFrom(req);
        String password = req.getParameter("password");
        boolean creating = user.getId() == 0;

        List<String> errors = validate(user, password, creating, actor);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("user", user);
            req.setAttribute("doctors", doctorDAO.findAll());
            req.setAttribute("roles", Role.values());
            render(req, resp, "user-form");
            return;
        }

        if (creating) {
            user.setPasswordHash(PasswordHasher.hash(password));
            userDAO.insert(user);
            Flash.success(req, "Account for " + user.getFullName() + " created.");
        } else {
            userDAO.update(user);
            if (password != null && !password.isEmpty()) {
                userDAO.updatePassword(user.getId(), PasswordHasher.hash(password));
            }
            Flash.success(req, "Account for " + user.getFullName() + " updated.");
        }
        redirect(req, resp, "/users/");
    }

    private User bindFrom(HttpServletRequest req) {
        User u = new User();
        u.setId(intParam(req, "id", 0));
        u.setUsername(trimmed(req, "username"));
        u.setFullName(trimmed(req, "fullName"));
        u.setRole(Role.from(trimmed(req, "role")));
        u.setActive(req.getParameter("active") != null);

        int doctorId = intParam(req, "doctorId", 0);
        u.setDoctorId(doctorId > 0 ? doctorId : null);
        return u;
    }

    private List<String> validate(User user, String password, boolean creating, User actor) {
        List<String> errors = new ArrayList<>();

        if (user.getUsername() == null) {
            errors.add("A username is required.");
        } else if (!user.getUsername().matches("[a-zA-Z0-9._-]{3,50}")) {
            errors.add("Usernames may use letters, digits, dot, underscore and hyphen, "
                    + "and must be 3 to 50 characters.");
        } else if (creating && userDAO.usernameExists(user.getUsername())) {
            errors.add("That username is already taken.");
        }

        if (user.getFullName() == null) {
            errors.add("A full name is required.");
        }
        if (user.getRole() == null) {
            errors.add("Please choose a role.");
        }

        if (creating && (password == null || password.length() < MIN_PASSWORD_LENGTH)) {
            errors.add("The password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!creating && password != null && !password.isEmpty()
                && password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("The new password must be at least "
                    + MIN_PASSWORD_LENGTH + " characters.");
        }

        if (user.getRole() == Role.DOCTOR && user.getDoctorId() == null) {
            errors.add("A doctor account must be linked to a roster entry, "
                    + "so their own prescriptions can be identified.");
        }

        // Stop an administrator from removing their own access mid-session.
        if (!creating && actor.getId() == user.getId()) {
            if (user.getRole() != Role.ADMIN) {
                errors.add("You cannot change your own role away from Administrator.");
            }
            if (!user.isActive()) {
                errors.add("You cannot disable the account you are signed in with.");
            }
        }
        return errors;
    }
}

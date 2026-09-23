package com.hms.security;

import com.hms.dao.UserDAO;
import com.hms.model.Role;
import com.hms.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Creates the initial staff accounts when the users table is empty.
 *
 * <p>Passwords must be hashed, which the seed SQL cannot do, so the first accounts are
 * created here instead. This runs only when there are no users at all: once anyone
 * exists, including after the defaults are renamed or deleted, it does nothing.
 *
 * <p>The default passwords are development conveniences and are logged loudly as such.
 * Set {@code HMS_BOOTSTRAP_ADMIN_PASSWORD} to choose the administrator password, or
 * {@code HMS_BOOTSTRAP=false} to disable seeding entirely and create the first account
 * by another route.
 */
public final class UserBootstrap {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrap.class);

    private UserBootstrap() {
    }

    public static void run(DataSource dataSource) {
        if ("false".equalsIgnoreCase(System.getenv("HMS_BOOTSTRAP"))) {
            log.info("Account bootstrap disabled by HMS_BOOTSTRAP=false");
            return;
        }

        UserDAO userDAO = new UserDAO(dataSource);

        if (userDAO.count() > 0) {
            return;
        }

        String adminPassword = envOr("HMS_BOOTSTRAP_ADMIN_PASSWORD", "admin123");

        create(userDAO, "admin", adminPassword, "System Administrator", Role.ADMIN, null);
        create(userDAO, "dr.menon", "doctor123", "Priya Menon", Role.DOCTOR, 1);
        create(userDAO, "reception", "reception123", "Front Desk", Role.RECEPTIONIST, null);

        log.warn("=================================================================");
        log.warn(" No staff accounts existed, so default accounts were created:");
        log.warn("   admin     / {}", maskUnlessDefault(adminPassword));
        log.warn("   dr.menon  / doctor123      (Doctor)");
        log.warn("   reception / reception123   (Receptionist)");
        log.warn(" These are development credentials. Change them before this");
        log.warn(" system holds any real patient data.");
        log.warn("=================================================================");
    }

    private static void create(UserDAO dao, String username, String password,
                               String fullName, Role role, Integer doctorId) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setDoctorId(doctorId);
        user.setActive(true);
        dao.insert(user);
    }

    /** Never print an operator-supplied password, even at warn level. */
    private static String maskUnlessDefault(String password) {
        return "admin123".equals(password) ? "admin123" : "(set via environment)";
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}

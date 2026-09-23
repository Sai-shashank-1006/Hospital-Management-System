package com.hms.dao;

import com.hms.model.Role;
import com.hms.model.User;
import com.hms.security.PasswordHasher;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOTest {

    private UserDAO dao;

    @BeforeEach
    void setUp() {
        dao = new UserDAO(TestDatabase.create());
    }

    private User user(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordHasher.hash("password123"));
        u.setFullName("Test " + username);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    @Test
    @DisplayName("an account saves and loads with its role intact")
    void insertAndFind() {
        User saved = dao.insert(user("admin", Role.ADMIN));

        assertTrue(saved.getId() > 0);

        User found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("admin", found.getUsername());
        assertEquals(Role.ADMIN, found.getRole());
        assertTrue(found.isActive());
    }

    @Test
    @DisplayName("usernames are stored and matched in lower case")
    void usernameIsCaseInsensitive() {
        dao.insert(user("Dr.Menon", Role.DOCTOR));

        // Stored lowercase, so "Admin" and "admin" can never become two accounts.
        assertTrue(dao.findByUsername("dr.menon").isPresent());
        assertTrue(dao.findByUsername("DR.MENON").isPresent());
        assertTrue(dao.findByUsername("  Dr.Menon  ").isPresent());
    }

    @Test
    @DisplayName("looking up an unknown or blank username finds nothing")
    void unknownUsername() {
        assertTrue(dao.findByUsername("nobody").isEmpty());
        assertTrue(dao.findByUsername("").isEmpty());
        assertTrue(dao.findByUsername(null).isEmpty());
    }

    @Test
    @DisplayName("the stored hash verifies the original password")
    void storedHashVerifies() {
        dao.insert(user("admin", Role.ADMIN));

        User found = dao.findByUsername("admin").orElseThrow();

        assertTrue(PasswordHasher.matches("password123", found.getPasswordHash()));
        assertFalse(PasswordHasher.matches("wrong", found.getPasswordHash()));
    }

    @Test
    @DisplayName("changing the password does not disturb the profile")
    void updatePassword() {
        User saved = dao.insert(user("admin", Role.ADMIN));

        assertTrue(dao.updatePassword(saved.getId(), PasswordHasher.hash("newpassword")));

        User found = dao.findById(saved.getId()).orElseThrow();
        assertTrue(PasswordHasher.matches("newpassword", found.getPasswordHash()));
        assertFalse(PasswordHasher.matches("password123", found.getPasswordHash()));
        assertEquals(Role.ADMIN, found.getRole());
    }

    @Test
    @DisplayName("a profile update leaves the password alone")
    void updateProfileKeepsPassword() {
        User saved = dao.insert(user("reception", Role.RECEPTIONIST));

        saved.setFullName("Front Desk Two");
        saved.setActive(false);
        assertTrue(dao.update(saved));

        User found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("Front Desk Two", found.getFullName());
        assertFalse(found.isActive());
        assertTrue(PasswordHasher.matches("password123", found.getPasswordHash()));
    }

    @Test
    @DisplayName("a doctor account can be linked to a roster entry, or not")
    void doctorLink() {
        User unlinked = dao.insert(user("admin", Role.ADMIN));
        assertNull(dao.findById(unlinked.getId()).orElseThrow().getDoctorId());

        // No doctors table row is needed here: the column is nullable and the
        // foreign key is only checked when a value is supplied.
        User plain = dao.insert(user("reception", Role.RECEPTIONIST));
        assertNull(plain.getDoctorId());
    }

    @Test
    @DisplayName("recording a sign-in stamps the time")
    void touchLastLogin() {
        User saved = dao.insert(user("admin", Role.ADMIN));
        assertNull(saved.getLastLoginAt());

        dao.touchLastLogin(saved.getId());

        assertNotNull(dao.findById(saved.getId()).orElseThrow().getLastLoginAt());
    }

    @Test
    @DisplayName("active administrators are counted, disabled ones are not")
    void countActiveAdmins() {
        dao.insert(user("admin", Role.ADMIN));
        assertEquals(1, dao.countActiveAdmins());

        User second = dao.insert(user("admin2", Role.ADMIN));
        assertEquals(2, dao.countActiveAdmins());

        second.setActive(false);
        dao.update(second);
        assertEquals(1, dao.countActiveAdmins(),
                "a disabled administrator cannot sign in, so must not count");

        dao.insert(user("reception", Role.RECEPTIONIST));
        assertEquals(1, dao.countActiveAdmins(), "other roles are not administrators");
    }

    @Test
    @DisplayName("usernameExists reports whether the name is taken")
    void usernameExists() {
        dao.insert(user("admin", Role.ADMIN));

        assertTrue(dao.usernameExists("admin"));
        assertTrue(dao.usernameExists("ADMIN"));
        assertFalse(dao.usernameExists("someone.else"));
    }

    @Test
    @DisplayName("delete removes the account and reports whether anything went")
    void delete() {
        User saved = dao.insert(user("temp", Role.RECEPTIONIST));

        assertTrue(dao.delete(saved.getId()));
        assertTrue(dao.findById(saved.getId()).isEmpty());
        assertFalse(dao.delete(saved.getId()));
        assertEquals(0, dao.count());
    }
}

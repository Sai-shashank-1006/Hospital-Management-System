package com.hms.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests pin down the access rules themselves. The same method is used by
 * AuthFilter to allow a request and by the layout to show a menu item, so what is
 * asserted here is exactly what the running application enforces.
 */
class RoleTest {

    @Test
    @DisplayName("an administrator reaches everything")
    void adminReachesEverything() {
        for (String path : new String[]{
                "/dashboard", "/patients/", "/doctors/", "/appointments/",
                "/prescriptions/", "/invoices/", "/reports/", "/users/"}) {
            assertTrue(Role.ADMIN.canAccess(path), "admin should reach " + path);
        }
    }

    @Test
    @DisplayName("a doctor reaches clinical areas but not billing or staff accounts")
    void doctorScope() {
        assertTrue(Role.DOCTOR.canAccess("/patients/"));
        assertTrue(Role.DOCTOR.canAccess("/appointments/"));
        assertTrue(Role.DOCTOR.canAccess("/prescriptions/new"));
        assertTrue(Role.DOCTOR.canAccess("/reports/"));

        assertFalse(Role.DOCTOR.canAccess("/invoices/"), "billing is not a clinical area");
        assertFalse(Role.DOCTOR.canAccess("/users/"), "only administrators manage accounts");
        assertFalse(Role.DOCTOR.canAccess("/doctors/"), "the roster is administrative");
    }

    @Test
    @DisplayName("a receptionist reaches billing but not prescriptions")
    void receptionistScope() {
        assertTrue(Role.RECEPTIONIST.canAccess("/patients/"));
        assertTrue(Role.RECEPTIONIST.canAccess("/appointments/new"));
        assertTrue(Role.RECEPTIONIST.canAccess("/invoices/"));
        assertTrue(Role.RECEPTIONIST.canAccess("/reports/"));

        assertFalse(Role.RECEPTIONIST.canAccess("/prescriptions/"),
                "prescribing is a medical act");
        assertFalse(Role.RECEPTIONIST.canAccess("/users/"));
        assertFalse(Role.RECEPTIONIST.canAccess("/doctors/"));
    }

    @Test
    @DisplayName("a prefix match does not leak into a different area with the same start")
    void prefixMatchingIsExact() {
        // "/users" must not be granted by an allowance for a path that merely
        // begins with the same letters.
        assertFalse(Role.DOCTOR.canAccess("/patients-secret"),
                "'/patients' must not also grant '/patients-secret'");
        assertFalse(Role.RECEPTIONIST.canAccess("/invoicesarchive"));

        assertTrue(Role.DOCTOR.canAccess("/patients"), "the bare path is allowed");
        assertTrue(Role.DOCTOR.canAccess("/patients/edit"), "a child path is allowed");
    }

    @Test
    @DisplayName("everyone reaches the dashboard and the context root")
    void commonPaths() {
        for (Role role : Role.values()) {
            assertTrue(role.canAccess("/dashboard"));
            assertTrue(role.canAccess("/"));
            assertTrue(role.canAccess(""));
        }
    }

    @Test
    @DisplayName("sign-out is not covered by any role's permissions")
    void signOutIsNotARolePermission() {
        // Regression guard. Sign-out is not in the non-admin allow-lists, so if
        // AuthFilter ever runs its role check over /logout, doctors and
        // receptionists get a 403 and are unable to sign out at all. The filter
        // keeps /logout in AUTHENTICATED_PATHS, which is checked before the role.
        assertFalse(Role.DOCTOR.canAccess("/logout"));
        assertFalse(Role.RECEPTIONIST.canAccess("/logout"));
    }

    @Test
    @DisplayName("role lookup is case-insensitive and safe for unknown values")
    void lookup() {
        assertSame(Role.ADMIN, Role.from("ADMIN"));
        assertSame(Role.ADMIN, Role.from("admin"));
        assertSame(Role.DOCTOR, Role.from(" Doctor "));

        assertNull(Role.from("SUPERUSER"));
        assertNull(Role.from(null));
        assertNull(Role.from(""));
    }
}

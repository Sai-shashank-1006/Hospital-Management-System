package com.hms.model;

import java.util.Arrays;
import java.util.List;

/**
 * Staff roles and what each may reach.
 *
 * <p>Access is expressed as URL path prefixes and enforced in one place, by
 * {@link com.hms.web.AuthFilter}. The navigation menu asks the same question via
 * {@link #canAccess(String)}, so a hidden menu item and a blocked request can never
 * disagree — hiding a link is a convenience, the filter is the actual control.
 */
public enum Role {

    /** Full access, including staff accounts and the doctor roster. */
    ADMIN("Administrator", List.of("/")),

    /**
     * Clinical staff: sees patients and appointments and writes prescriptions,
     * but not billing, the doctor roster, or user accounts.
     */
    DOCTOR("Doctor", List.of(
            "/dashboard", "/patients", "/appointments", "/prescriptions", "/reports")),

    /**
     * Front desk: registration, scheduling and billing. No clinical records,
     * because prescriptions are a medical act.
     */
    RECEPTIONIST("Receptionist", List.of(
            "/dashboard", "/patients", "/appointments", "/invoices", "/reports"));

    private final String label;
    private final List<String> allowedPrefixes;

    Role(String label, List<String> allowedPrefixes) {
        this.label = label;
        this.allowedPrefixes = allowedPrefixes;
    }

    public String getLabel() {
        return label;
    }

    /** True when this role may reach {@code path}, which is relative to the context root. */
    public boolean canAccess(String path) {
        if (this == ADMIN) {
            return true;
        }
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return true;
        }
        return allowedPrefixes.stream().anyMatch(
                prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /** Case-insensitive lookup that returns {@code null} rather than throwing. */
    public static Role from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}

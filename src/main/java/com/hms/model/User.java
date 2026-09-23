package com.hms.model;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    /** Set when this account belongs to a doctor, linking it to their roster entry. */
    private Integer doctorId;
    private boolean active = true;
    private LocalDateTime lastLoginAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /** Initials for the avatar in the top bar. */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public String getFormattedLastLogin() {
        if (lastLoginAt == null) {
            return "First sign-in";
        }
        return lastLoginAt.format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }
}

package com.hms.dao;

import com.hms.model.Role;
import com.hms.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY role, full_name";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list users", e);
        }
    }

    /** Looks up by username for sign-in. Usernames are stored and compared lowercase. */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load user " + username, e);
        }
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load user " + id, e);
        }
    }

    public User insert(User user) {
        String sql = "INSERT INTO users "
                + "(username, password_hash, full_name, role, doctor_id, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername().trim().toLowerCase());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            setNullableInt(ps, 5, user.getDoctorId());
            ps.setBoolean(6, user.isActive());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert user", e);
        }
    }

    /** Updates the profile fields. The password is changed separately. */
    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, role = ?, doctor_id = ?, active = ? "
                + "WHERE id = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole().name());
            setNullableInt(ps, 3, user.getDoctorId());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user " + user.getId(), e);
        }
    }

    public boolean updatePassword(int id, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update password for user " + id, e);
        }
    }

    public void touchLastLogin(int id) {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record sign-in for user " + id, e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user " + id, e);
        }
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count users", e);
        }
    }

    /** Number of enabled administrators, used to refuse removing the last one. */
    public int countActiveAdmins() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ? AND active = TRUE";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, Role.ADMIN.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count administrators", e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value)
            throws SQLException {
        if (value == null || value <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.from(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));

        int doctorId = rs.getInt("doctor_id");
        u.setDoctorId(rs.wasNull() ? null : doctorId);

        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        u.setLastLoginAt(lastLogin == null ? null : lastLogin.toLocalDateTime());

        return u;
    }
}

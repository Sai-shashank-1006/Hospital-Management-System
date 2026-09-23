package com.hms.dao;

import com.hms.model.Appointment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDAO {

    /** Every read joins the parent rows so the views can show names instead of ids. */
    private static final String SELECT_WITH_NAMES =
            "SELECT a.*, p.full_name AS patient_name, "
                    + "d.full_name AS doctor_name, d.specialization AS doctor_specialization "
                    + "FROM appointments a "
                    + "JOIN patients p ON p.id = a.patient_id "
                    + "JOIN doctors d ON d.id = a.doctor_id ";

    private final DataSource dataSource;

    public AppointmentDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Appointment> findAll() {
        String sql = SELECT_WITH_NAMES + "ORDER BY a.appointment_time DESC";
        return query(sql);
    }

    /** Scheduled appointments that have not yet happened, soonest first. */
    public List<Appointment> findUpcoming(int limit) {
        String sql = SELECT_WITH_NAMES
                + "WHERE a.status = ? AND a.appointment_time >= CURRENT_TIMESTAMP "
                + "ORDER BY a.appointment_time ASC LIMIT ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, Appointment.STATUS_SCHEDULED);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list upcoming appointments", e);
        }
    }

    /** Upcoming appointments for one doctor, for their own dashboard. */
    public List<Appointment> findUpcomingForDoctor(int doctorId, int limit) {
        String sql = SELECT_WITH_NAMES
                + "WHERE a.doctor_id = ? AND a.status = ? "
                + "AND a.appointment_time >= CURRENT_TIMESTAMP "
                + "ORDER BY a.appointment_time ASC LIMIT ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setString(2, Appointment.STATUS_SCHEDULED);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to list upcoming appointments for doctor " + doctorId, e);
        }
    }

    public Optional<Appointment> findById(int id) {
        String sql = SELECT_WITH_NAMES + "WHERE a.id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load appointment " + id, e);
        }
    }

    public Appointment insert(Appointment appointment) {
        String sql = "INSERT INTO appointments "
                + "(patient_id, doctor_id, appointment_time, reason, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, appointment);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    appointment.setId(keys.getInt(1));
                }
            }
            return appointment;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert appointment", e);
        }
    }

    public boolean update(Appointment appointment) {
        String sql = "UPDATE appointments SET "
                + "patient_id = ?, doctor_id = ?, appointment_time = ?, reason = ?, status = ? "
                + "WHERE id = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bind(ps, appointment);
            ps.setInt(6, appointment.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update appointment " + appointment.getId(), e);
        }
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update status of appointment " + id, e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete appointment " + id, e);
        }
    }

    /**
     * True when the doctor already has a non-cancelled appointment at that exact time.
     * {@code excludeId} lets an edit skip the row being edited; pass 0 when creating.
     */
    public boolean hasConflict(int doctorId, java.time.LocalDateTime time, int excludeId) {
        String sql = "SELECT COUNT(*) FROM appointments "
                + "WHERE doctor_id = ? AND appointment_time = ? AND status <> ? AND id <> ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, doctorId);
            ps.setTimestamp(2, Timestamp.valueOf(time));
            ps.setString(3, Appointment.STATUS_CANCELLED);
            ps.setInt(4, excludeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check appointment conflict", e);
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE status = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count appointments", e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM appointments";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count appointments", e);
        }
    }

    private List<Appointment> query(String sql) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Appointment> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list appointments", e);
        }
    }

    private void bind(PreparedStatement ps, Appointment a) throws SQLException {
        ps.setInt(1, a.getPatientId());
        ps.setInt(2, a.getDoctorId());
        ps.setTimestamp(3, Timestamp.valueOf(a.getAppointmentTime()));
        ps.setString(4, a.getReason());
        ps.setString(5, a.getStatus());
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setAppointmentTime(rs.getTimestamp("appointment_time").toLocalDateTime());
        a.setReason(rs.getString("reason"));
        a.setStatus(rs.getString("status"));
        a.setPatientName(rs.getString("patient_name"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setDoctorSpecialization(rs.getString("doctor_specialization"));
        return a;
    }
}

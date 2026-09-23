package com.hms.dao;

import com.hms.model.Doctor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDAO {

    private final DataSource dataSource;

    public DoctorDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Doctor> findAll() {
        String sql = "SELECT * FROM doctors ORDER BY full_name";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Doctor> doctors = new ArrayList<>();
            while (rs.next()) {
                doctors.add(map(rs));
            }
            return doctors;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list doctors", e);
        }
    }

    /** Only doctors currently accepting appointments; used to populate the booking form. */
    public List<Doctor> findAvailable() {
        String sql = "SELECT * FROM doctors WHERE available = TRUE ORDER BY full_name";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Doctor> doctors = new ArrayList<>();
            while (rs.next()) {
                doctors.add(map(rs));
            }
            return doctors;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list available doctors", e);
        }
    }

    public Optional<Doctor> findById(int id) {
        String sql = "SELECT * FROM doctors WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load doctor " + id, e);
        }
    }

    public Doctor insert(Doctor doctor) {
        String sql = "INSERT INTO doctors "
                + "(full_name, specialization, phone, email, consultation_fee, available, "
                + "available_days, available_from, available_to, room_number) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, doctor);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    doctor.setId(keys.getInt(1));
                }
            }
            return doctor;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert doctor", e);
        }
    }

    public boolean update(Doctor doctor) {
        String sql = "UPDATE doctors SET "
                + "full_name = ?, specialization = ?, phone = ?, email = ?, "
                + "consultation_fee = ?, available = ?, "
                + "available_days = ?, available_from = ?, available_to = ?, room_number = ? "
                + "WHERE id = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bind(ps, doctor);
            ps.setInt(11, doctor.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update doctor " + doctor.getId(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM doctors WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete doctor " + id, e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM doctors";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count doctors", e);
        }
    }

    private void bind(PreparedStatement ps, Doctor d) throws SQLException {
        ps.setString(1, d.getFullName());
        ps.setString(2, d.getSpecialization());
        ps.setString(3, d.getPhone());
        ps.setString(4, d.getEmail());
        ps.setBigDecimal(5, d.getConsultationFee() == null ? BigDecimal.ZERO : d.getConsultationFee());
        ps.setBoolean(6, d.isAvailable());
        ps.setString(7, d.getAvailableDays());
        ps.setTime(8, d.getAvailableFrom() == null ? null : Time.valueOf(d.getAvailableFrom()));
        ps.setTime(9, d.getAvailableTo() == null ? null : Time.valueOf(d.getAvailableTo()));
        ps.setString(10, d.getRoomNumber());
    }

    private Doctor map(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setFullName(rs.getString("full_name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setConsultationFee(rs.getBigDecimal("consultation_fee"));
        d.setAvailable(rs.getBoolean("available"));
        d.setAvailableDays(rs.getString("available_days"));

        Time from = rs.getTime("available_from");
        d.setAvailableFrom(from == null ? null : from.toLocalTime());

        Time to = rs.getTime("available_to");
        d.setAvailableTo(to == null ? null : to.toLocalTime());

        d.setRoomNumber(rs.getString("room_number"));
        return d;
    }
}

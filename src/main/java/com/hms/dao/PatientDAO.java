package com.hms.dao;

import com.hms.model.Patient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDAO {

    private final DataSource dataSource;

    public PatientDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Patient> findAll() {
        String sql = "SELECT * FROM patients ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Patient> patients = new ArrayList<>();
            while (rs.next()) {
                patients.add(map(rs));
            }
            return patients;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list patients", e);
        }
    }

    /** Case-insensitive match on name, phone or email. A blank term returns everything. */
    public List<Patient> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String sql = "SELECT * FROM patients "
                + "WHERE LOWER(full_name) LIKE ? OR phone LIKE ? OR LOWER(email) LIKE ? "
                + "ORDER BY id DESC";
        String pattern = "%" + term.trim().toLowerCase() + "%";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                List<Patient> patients = new ArrayList<>();
                while (rs.next()) {
                    patients.add(map(rs));
                }
                return patients;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to search patients", e);
        }
    }

    public Optional<Patient> findById(int id) {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load patient " + id, e);
        }
    }

    /** Inserts the patient and returns it with the generated id populated. */
    public Patient insert(Patient patient) {
        String sql = "INSERT INTO patients "
                + "(full_name, gender, date_of_birth, phone, email, address, blood_group) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, patient);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getInt(1));
                }
            }
            return patient;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert patient", e);
        }
    }

    public boolean update(Patient patient) {
        String sql = "UPDATE patients SET "
                + "full_name = ?, gender = ?, date_of_birth = ?, phone = ?, "
                + "email = ?, address = ?, blood_group = ? "
                + "WHERE id = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bind(ps, patient);
            ps.setInt(8, patient.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update patient " + patient.getId(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete patient " + id, e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count patients", e);
        }
    }

    private void bind(PreparedStatement ps, Patient p) throws SQLException {
        ps.setString(1, p.getFullName());
        ps.setString(2, p.getGender());
        ps.setDate(3, p.getDateOfBirth() == null ? null : Date.valueOf(p.getDateOfBirth()));
        ps.setString(4, p.getPhone());
        ps.setString(5, p.getEmail());
        ps.setString(6, p.getAddress());
        ps.setString(7, p.getBloodGroup());
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setFullName(rs.getString("full_name"));
        p.setGender(rs.getString("gender"));

        Date dob = rs.getDate("date_of_birth");
        p.setDateOfBirth(dob == null ? null : dob.toLocalDate());

        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setAddress(rs.getString("address"));
        p.setBloodGroup(rs.getString("blood_group"));
        return p;
    }
}

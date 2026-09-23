package com.hms.dao;

import com.hms.model.Prescription;
import com.hms.model.PrescriptionItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrescriptionDAO {

    private static final String SELECT_WITH_NAMES =
            "SELECT pr.*, p.full_name AS patient_name, "
                    + "d.full_name AS doctor_name, d.specialization AS doctor_specialization "
                    + "FROM prescriptions pr "
                    + "JOIN patients p ON p.id = pr.patient_id "
                    + "JOIN doctors d ON d.id = pr.doctor_id ";

    private final DataSource dataSource;

    public PrescriptionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Prescription> findAll() {
        return query(SELECT_WITH_NAMES + "ORDER BY pr.issued_at DESC", ps -> { });
    }

    /** Prescriptions written by one doctor, for the doctor's own view. */
    public List<Prescription> findByDoctor(int doctorId) {
        return query(SELECT_WITH_NAMES + "WHERE pr.doctor_id = ? ORDER BY pr.issued_at DESC",
                ps -> ps.setInt(1, doctorId));
    }

    public List<Prescription> findByPatient(int patientId) {
        return query(SELECT_WITH_NAMES + "WHERE pr.patient_id = ? ORDER BY pr.issued_at DESC",
                ps -> ps.setInt(1, patientId));
    }

    /** Loads one prescription together with its medicines. */
    public Optional<Prescription> findById(int id) {
        List<Prescription> found = query(SELECT_WITH_NAMES + "WHERE pr.id = ?",
                ps -> ps.setInt(1, id));

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Prescription prescription = found.get(0);
        prescription.setItems(findItems(id));
        return Optional.of(prescription);
    }

    public List<PrescriptionItem> findItems(int prescriptionId) {
        String sql = "SELECT * FROM prescription_items WHERE prescription_id = ? ORDER BY id";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load prescription items", e);
        }
    }

    /**
     * Saves the prescription and its medicines as one unit.
     *
     * <p>Header and items are written in a single transaction: a prescription that
     * saved its header but lost its medicines would be a clinically misleading record,
     * so either all of it lands or none of it does.
     */
    public Prescription insert(Prescription prescription) {
        String sql = "INSERT INTO prescriptions "
                + "(patient_id, doctor_id, appointment_id, issued_at, diagnosis, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, prescription);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        prescription.setId(keys.getInt(1));
                    }
                }
            }

            replaceItems(c, prescription.getId(), prescription.getItems());

            c.commit();
            return prescription;

        } catch (SQLException e) {
            rollback(c);
            throw new DataAccessException("Failed to insert prescription", e);
        } finally {
            close(c);
        }
    }

    public boolean update(Prescription prescription) {
        String sql = "UPDATE prescriptions SET "
                + "patient_id = ?, doctor_id = ?, appointment_id = ?, issued_at = ?, "
                + "diagnosis = ?, notes = ? WHERE id = ?";

        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            int updated;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, prescription);
                ps.setInt(7, prescription.getId());
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                c.rollback();
                return false;
            }

            replaceItems(c, prescription.getId(), prescription.getItems());

            c.commit();
            return true;

        } catch (SQLException e) {
            rollback(c);
            throw new DataAccessException(
                    "Failed to update prescription " + prescription.getId(), e);
        } finally {
            close(c);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM prescriptions WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete prescription " + id, e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM prescriptions";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count prescriptions", e);
        }
    }

    /** Wholesale replace: simpler and safer than diffing an edited medicine list. */
    private void replaceItems(Connection c, int prescriptionId, List<PrescriptionItem> items)
            throws SQLException {

        try (PreparedStatement del =
                     c.prepareStatement("DELETE FROM prescription_items WHERE prescription_id = ?")) {
            del.setInt(1, prescriptionId);
            del.executeUpdate();
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO prescription_items "
                + "(prescription_id, medicine, dosage, frequency, duration, instructions) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (PrescriptionItem item : items) {
                ps.setInt(1, prescriptionId);
                ps.setString(2, item.getMedicine());
                ps.setString(3, item.getDosage());
                ps.setString(4, item.getFrequency());
                ps.setString(5, item.getDuration());
                ps.setString(6, item.getInstructions());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Prescription> query(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<Prescription> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list prescriptions", e);
        }
    }

    private void bind(PreparedStatement ps, Prescription p) throws SQLException {
        ps.setInt(1, p.getPatientId());
        ps.setInt(2, p.getDoctorId());
        if (p.getAppointmentId() == null || p.getAppointmentId() <= 0) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, p.getAppointmentId());
        }
        ps.setTimestamp(4, Timestamp.valueOf(p.getIssuedAt()));
        ps.setString(5, p.getDiagnosis());
        ps.setString(6, p.getNotes());
    }

    private Prescription map(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("id"));
        p.setPatientId(rs.getInt("patient_id"));
        p.setDoctorId(rs.getInt("doctor_id"));

        int appointmentId = rs.getInt("appointment_id");
        p.setAppointmentId(rs.wasNull() ? null : appointmentId);

        p.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
        p.setDiagnosis(rs.getString("diagnosis"));
        p.setNotes(rs.getString("notes"));
        p.setPatientName(rs.getString("patient_name"));
        p.setDoctorName(rs.getString("doctor_name"));
        p.setDoctorSpecialization(rs.getString("doctor_specialization"));
        return p;
    }

    private PrescriptionItem mapItem(ResultSet rs) throws SQLException {
        PrescriptionItem item = new PrescriptionItem();
        item.setId(rs.getInt("id"));
        item.setPrescriptionId(rs.getInt("prescription_id"));
        item.setMedicine(rs.getString("medicine"));
        item.setDosage(rs.getString("dosage"));
        item.setFrequency(rs.getString("frequency"));
        item.setDuration(rs.getString("duration"));
        item.setInstructions(rs.getString("instructions"));
        return item;
    }

    private void rollback(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // The original failure is the one worth reporting.
            }
        }
    }

    private void close(Connection c) {
        if (c != null) {
            try {
                // Restore autocommit before the pool takes the connection back.
                c.setAutoCommit(true);
                c.close();
            } catch (SQLException ignored) {
                // Nothing useful to do if returning the connection fails.
            }
        }
    }
}

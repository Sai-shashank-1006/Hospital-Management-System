package com.hms.dao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate queries backing the Reports & Analytics page.
 *
 * <p>These are read-only summaries. Aggregation happens in SQL rather than by loading
 * rows and counting in Java, so the work stays close to the data and the page does not
 * grow slower as the tables grow.
 */
public class ReportDAO {

    /** One labelled figure in a report, such as "Cardiology: 12". */
    public record Count(String label, long value) {
    }

    /** One labelled money figure, such as a month's revenue. */
    public record Money(String label, BigDecimal value) {
    }

    private final DataSource dataSource;

    public ReportDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Count> appointmentsByStatus() {
        return counts("SELECT status, COUNT(*) FROM appointments GROUP BY status ORDER BY status");
    }

    public List<Count> appointmentsByDoctor(int limit) {
        String sql = "SELECT d.full_name, COUNT(a.id) AS total "
                + "FROM doctors d LEFT JOIN appointments a ON a.doctor_id = d.id "
                + "GROUP BY d.id, d.full_name "
                + "ORDER BY total DESC, d.full_name "
                + "LIMIT ?";
        return counts(sql, ps -> ps.setInt(1, limit));
    }

    public List<Count> appointmentsBySpecialization() {
        String sql = "SELECT d.specialization, COUNT(a.id) AS total "
                + "FROM doctors d LEFT JOIN appointments a ON a.doctor_id = d.id "
                + "GROUP BY d.specialization "
                + "ORDER BY total DESC, d.specialization";
        return counts(sql);
    }

    public List<Count> patientsByGender() {
        return counts("SELECT gender, COUNT(*) FROM patients GROUP BY gender ORDER BY gender");
    }

    public List<Count> patientsByBloodGroup() {
        String sql = "SELECT COALESCE(blood_group, 'Unknown'), COUNT(*) "
                + "FROM patients GROUP BY blood_group ORDER BY COUNT(*) DESC";
        return counts(sql);
    }

    /** How many patients have insurance on file, versus not. */
    public List<Count> patientsByInsurance() {
        String sql = "SELECT CASE WHEN insurance_provider IS NULL OR insurance_provider = '' "
                + "            THEN 'Uninsured' ELSE 'Insured' END AS bucket, COUNT(*) "
                + "FROM patients GROUP BY bucket ORDER BY bucket";
        return counts(sql);
    }

    public List<Count> invoicesByStatus() {
        return counts("SELECT status, COUNT(*) FROM invoices GROUP BY status ORDER BY status");
    }

    /**
     * Appointment volume for each of the last {@code months} calendar months.
     *
     * <p>Months with no appointments are filled in as zero in Java, so the chart shows
     * a continuous axis instead of silently skipping quiet months.
     */
    public List<Count> appointmentsByMonth(int months) {
        // YEAR()/MONTH() rather than DATE_FORMAT(): the latter is MySQL-only and
        // would not run against H2 in the test suite.
        String sql = "SELECT YEAR(appointment_time), MONTH(appointment_time), COUNT(*) "
                + "FROM appointments GROUP BY YEAR(appointment_time), MONTH(appointment_time)";

        Map<String, Long> found = new LinkedHashMap<>();

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                found.put(monthKey(rs.getInt(1), rs.getInt(2)), rs.getLong(3));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to summarise appointments by month", e);
        }

        List<Count> series = new ArrayList<>();
        java.time.YearMonth cursor = java.time.YearMonth.now().minusMonths(months - 1L);

        for (int i = 0; i < months; i++) {
            String key = cursor.toString();
            String label = cursor.format(java.time.format.DateTimeFormatter.ofPattern("MMM yy"));
            series.add(new Count(label, found.getOrDefault(key, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    /** Revenue from paid invoices for each of the last {@code months} calendar months. */
    public List<Money> revenueByMonth(int months) {
        String sql = "SELECT YEAR(i.issued_at), MONTH(i.issued_at), "
                + "       COALESCE(SUM(t.line_total * (1 + i.tax_percent / 100)), 0) "
                + "FROM invoices i "
                + "JOIN (SELECT invoice_id, SUM(quantity * unit_price) AS line_total "
                + "      FROM invoice_items GROUP BY invoice_id) t ON t.invoice_id = i.id "
                + "WHERE i.status = 'PAID' "
                + "GROUP BY YEAR(i.issued_at), MONTH(i.issued_at)";

        Map<String, BigDecimal> found = new LinkedHashMap<>();

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                found.put(monthKey(rs.getInt(1), rs.getInt(2)), rs.getBigDecimal(3));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to summarise revenue by month", e);
        }

        List<Money> series = new ArrayList<>();
        java.time.YearMonth cursor = java.time.YearMonth.now().minusMonths(months - 1L);

        for (int i = 0; i < months; i++) {
            String key = cursor.toString();
            String label = cursor.format(java.time.format.DateTimeFormatter.ofPattern("MMM yy"));
            series.add(new Money(label, found.getOrDefault(key, BigDecimal.ZERO)));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    /** Most frequently prescribed medicines. */
    public List<Count> topMedicines(int limit) {
        String sql = "SELECT medicine, COUNT(*) AS total FROM prescription_items "
                + "GROUP BY medicine ORDER BY total DESC, medicine LIMIT ?";
        return counts(sql, ps -> ps.setInt(1, limit));
    }

    /** Builds the "yyyy-MM" key that the month series are aligned on. */
    private static String monthKey(int year, int month) {
        return java.time.YearMonth.of(year, month).toString();
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Count> counts(String sql) {
        return counts(sql, ps -> { });
    }

    private List<Count> counts(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<Count> list = new ArrayList<>();
                while (rs.next()) {
                    String label = rs.getString(1);
                    list.add(new Count(label == null ? "Unknown" : label, rs.getLong(2)));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run report query", e);
        }
    }
}

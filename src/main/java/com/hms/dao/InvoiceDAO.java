package com.hms.dao;

import com.hms.model.Invoice;
import com.hms.model.InvoiceItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class InvoiceDAO {

    private static final String SELECT_WITH_NAMES =
            "SELECT i.*, p.full_name AS patient_name "
                    + "FROM invoices i "
                    + "JOIN patients p ON p.id = i.patient_id ";

    private final DataSource dataSource;

    public InvoiceDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Lists invoices with their line items loaded.
     *
     * <p>Totals are derived from the items, so the list view cannot show a total
     * without them. Items for the whole page are fetched in one extra query rather
     * than one per invoice, which would be a classic N+1.
     */
    public List<Invoice> findAll() {
        List<Invoice> invoices = query(SELECT_WITH_NAMES + "ORDER BY i.issued_at DESC, i.id DESC",
                ps -> { });
        attachItems(invoices);
        return invoices;
    }

    public List<Invoice> findByStatus(String status) {
        List<Invoice> invoices = query(
                SELECT_WITH_NAMES + "WHERE i.status = ? ORDER BY i.issued_at DESC",
                ps -> ps.setString(1, status));
        attachItems(invoices);
        return invoices;
    }

    public List<Invoice> findByPatient(int patientId) {
        List<Invoice> invoices = query(
                SELECT_WITH_NAMES + "WHERE i.patient_id = ? ORDER BY i.issued_at DESC",
                ps -> ps.setInt(1, patientId));
        attachItems(invoices);
        return invoices;
    }

    public Optional<Invoice> findById(int id) {
        List<Invoice> found = query(SELECT_WITH_NAMES + "WHERE i.id = ?",
                ps -> ps.setInt(1, id));

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Invoice invoice = found.get(0);
        invoice.setItems(findItems(id));
        return Optional.of(invoice);
    }

    public List<InvoiceItem> findItems(int invoiceId) {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY id";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                List<InvoiceItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load invoice items", e);
        }
    }

    /** Header and items are written together, so a total always has its rows. */
    public Invoice insert(Invoice invoice) {
        String sql = "INSERT INTO invoices "
                + "(invoice_number, patient_id, appointment_id, issued_at, status, "
                + "tax_percent, notes, paid_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
                invoice.setInvoiceNumber(nextInvoiceNumber(c));
            }

            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, invoice);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        invoice.setId(keys.getInt(1));
                    }
                }
            }

            replaceItems(c, invoice.getId(), invoice.getItems());

            c.commit();
            return invoice;

        } catch (SQLException e) {
            rollback(c);
            throw new DataAccessException("Failed to insert invoice", e);
        } finally {
            close(c);
        }
    }

    public boolean update(Invoice invoice) {
        String sql = "UPDATE invoices SET "
                + "invoice_number = ?, patient_id = ?, appointment_id = ?, issued_at = ?, "
                + "status = ?, tax_percent = ?, notes = ?, paid_at = ? WHERE id = ?";

        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            int updated;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, invoice);
                ps.setInt(9, invoice.getId());
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                c.rollback();
                return false;
            }

            replaceItems(c, invoice.getId(), invoice.getItems());

            c.commit();
            return true;

        } catch (SQLException e) {
            rollback(c);
            throw new DataAccessException("Failed to update invoice " + invoice.getId(), e);
        } finally {
            close(c);
        }
    }

    /** Marking an invoice paid also stamps when, which the reports use. */
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE invoices SET status = ?, paid_at = ? WHERE id = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            if (Invoice.STATUS_PAID.equals(status)) {
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setInt(3, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update status of invoice " + id, e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM invoices WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete invoice " + id, e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM invoices";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count invoices", e);
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM invoices WHERE status = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count invoices", e);
        }
    }

    /**
     * Sums invoice totals for one status, computed in SQL.
     *
     * <p>Tax is applied per invoice before summing, matching how each invoice
     * displays its own total; summing first and taxing after would round differently.
     */
    public BigDecimal sumTotalByStatus(String status) {
        String sql = "SELECT COALESCE(SUM(line_total * (1 + i.tax_percent / 100)), 0) "
                + "FROM invoices i "
                + "JOIN (SELECT invoice_id, SUM(quantity * unit_price) AS line_total "
                + "      FROM invoice_items GROUP BY invoice_id) t "
                + "  ON t.invoice_id = i.id "
                + "WHERE i.status = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                if (total == null) {
                    return BigDecimal.ZERO;
                }
                // Dividing by 100 for the tax leaves the driver's own scale on the
                // result, which surfaced in the UI as "2205.00000000". Money is
                // rounded to two places here, at the edge of the data layer, so
                // every caller gets a value that is already fit to display.
                return total.setScale(2, RoundingMode.HALF_UP);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to total invoices", e);
        }
    }

    /**
     * Allocates the next invoice number inside the caller's transaction.
     *
     * <p>The counter lives in its own table rather than being derived from the
     * invoices themselves. Taking {@code MAX(invoice_number)} would hand the same
     * number out twice once an invoice had been deleted — the highest surviving
     * number goes back down — and two invoices sharing a number is an accounting
     * problem, not a cosmetic one.
     *
     * <p>{@code FOR UPDATE} holds the counter row for the rest of the transaction,
     * so two concurrent inserts cannot read the same value; the second waits.
     */
    private String nextInvoiceNumber(Connection c) throws SQLException {
        int value;

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT next_value FROM invoice_sequence WHERE name = 'invoice' FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new SQLException("The invoice number counter row is missing");
            }
            value = rs.getInt(1);
        }

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE invoice_sequence SET next_value = ? WHERE name = 'invoice'")) {
            ps.setInt(1, value + 1);
            ps.executeUpdate();
        }

        return String.format("INV-%06d", value);
    }

    /** Loads items for a page of invoices in one query, avoiding an N+1. */
    private void attachItems(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", invoices.stream().map(i -> "?").toList());
        String sql = "SELECT * FROM invoice_items WHERE invoice_id IN (" + placeholders + ") "
                + "ORDER BY invoice_id, id";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < invoices.size(); i++) {
                ps.setInt(i + 1, invoices.get(i).getId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = mapItem(rs);
                    invoices.stream()
                            .filter(inv -> inv.getId() == item.getInvoiceId())
                            .findFirst()
                            .ifPresent(inv -> inv.getItems().add(item));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load invoice items", e);
        }
    }

    private void replaceItems(Connection c, int invoiceId, List<InvoiceItem> items)
            throws SQLException {

        try (PreparedStatement del =
                     c.prepareStatement("DELETE FROM invoice_items WHERE invoice_id = ?")) {
            del.setInt(1, invoiceId);
            del.executeUpdate();
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO invoice_items (invoice_id, description, quantity, unit_price) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (InvoiceItem item : items) {
                ps.setInt(1, invoiceId);
                ps.setString(2, item.getDescription());
                ps.setInt(3, item.getQuantity());
                ps.setBigDecimal(4, item.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Invoice> query(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list invoices", e);
        }
    }

    private void bind(PreparedStatement ps, Invoice i) throws SQLException {
        ps.setString(1, i.getInvoiceNumber());
        ps.setInt(2, i.getPatientId());
        if (i.getAppointmentId() == null || i.getAppointmentId() <= 0) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, i.getAppointmentId());
        }
        ps.setTimestamp(4, Timestamp.valueOf(i.getIssuedAt()));
        ps.setString(5, i.getStatus());
        ps.setBigDecimal(6, i.getTaxPercent());
        ps.setString(7, i.getNotes());
        ps.setTimestamp(8, i.getPaidAt() == null ? null : Timestamp.valueOf(i.getPaidAt()));
    }

    private Invoice map(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setId(rs.getInt("id"));
        i.setInvoiceNumber(rs.getString("invoice_number"));
        i.setPatientId(rs.getInt("patient_id"));

        int appointmentId = rs.getInt("appointment_id");
        i.setAppointmentId(rs.wasNull() ? null : appointmentId);

        i.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
        i.setStatus(rs.getString("status"));
        i.setTaxPercent(rs.getBigDecimal("tax_percent"));
        i.setNotes(rs.getString("notes"));

        Timestamp paidAt = rs.getTimestamp("paid_at");
        i.setPaidAt(paidAt == null ? null : paidAt.toLocalDateTime());

        i.setPatientName(rs.getString("patient_name"));
        return i;
    }

    private InvoiceItem mapItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setDescription(rs.getString("description"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
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
                c.setAutoCommit(true);
                c.close();
            } catch (SQLException ignored) {
                // Nothing useful to do if returning the connection fails.
            }
        }
    }
}

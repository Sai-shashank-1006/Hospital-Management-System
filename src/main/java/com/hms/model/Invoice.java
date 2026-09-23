package com.hms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Invoice {

    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private String invoiceNumber;
    private int patientId;
    private Integer appointmentId;
    private LocalDateTime issuedAt;
    private String status = STATUS_UNPAID;
    private BigDecimal taxPercent = BigDecimal.ZERO;
    private String notes;
    private LocalDateTime paidAt;

    /** Joined in by the DAO for display; not persisted on this table. */
    private String patientName;

    private List<InvoiceItem> items = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(BigDecimal taxPercent) {
        this.taxPercent = (taxPercent == null) ? BigDecimal.ZERO : taxPercent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    public int getItemCount() {
        return items.size();
    }

    // -----------------------------------------------------------------------
    // Money. Totals are computed from the line items every time rather than
    // stored, so an invoice can never show a total that its rows do not add up
    // to. All values are rounded to two decimal places at the point of display.
    // -----------------------------------------------------------------------

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTaxAmount() {
        return getSubtotal()
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getTaxAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Only an unpaid invoice counts as money owed; cancelled ones do not. */
    public boolean isOutstanding() {
        return STATUS_UNPAID.equals(status);
    }

    public boolean isEditable() {
        return !STATUS_PAID.equals(status);
    }

    public String getFormattedIssuedAt() {
        if (issuedAt == null) {
            return "";
        }
        return issuedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    /** Value for an {@code <input type="datetime-local">}, which needs "yyyy-MM-ddTHH:mm". */
    public String getInputValue() {
        if (issuedAt == null) {
            return "";
        }
        return issuedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public String getFormattedPaidAt() {
        if (paidAt == null) {
            return "";
        }
        return paidAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
}

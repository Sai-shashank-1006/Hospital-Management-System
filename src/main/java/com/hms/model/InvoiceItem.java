package com.hms.model;

import java.math.BigDecimal;

public class InvoiceItem {

    private int id;
    private int invoiceId;
    private String description;
    private int quantity = 1;
    private BigDecimal unitPrice = BigDecimal.ZERO;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = (unitPrice == null) ? BigDecimal.ZERO : unitPrice;
    }

    /**
     * Line total, always derived rather than stored, so it cannot drift out of step
     * with the quantity and price it is made of.
     */
    public BigDecimal getAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

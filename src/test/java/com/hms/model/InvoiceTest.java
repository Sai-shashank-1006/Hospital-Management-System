package com.hms.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The money arithmetic, which is the part of billing that must not be wrong. */
class InvoiceTest {

    private InvoiceItem item(String description, int quantity, String unitPrice) {
        InvoiceItem i = new InvoiceItem();
        i.setDescription(description);
        i.setQuantity(quantity);
        i.setUnitPrice(new BigDecimal(unitPrice));
        return i;
    }

    @Test
    @DisplayName("a line amount is quantity times unit price")
    void lineAmount() {
        assertEquals(0, new BigDecimal("1350.00")
                .compareTo(item("X-ray", 3, "450.00").getAmount()));
    }

    @Test
    @DisplayName("subtotal, tax and total agree with the line items")
    void totals() {
        Invoice invoice = new Invoice();
        invoice.setTaxPercent(new BigDecimal("5.00"));
        invoice.setItems(List.of(
                item("Consultation", 1, "900.00"),
                item("ECG", 1, "450.00"),
                item("Lipid profile", 1, "800.00")));

        assertEquals(0, new BigDecimal("2150.00").compareTo(invoice.getSubtotal()));
        assertEquals(0, new BigDecimal("107.50").compareTo(invoice.getTaxAmount()));
        assertEquals(0, new BigDecimal("2257.50").compareTo(invoice.getTotal()));
    }

    @Test
    @DisplayName("an invoice with no items totals zero rather than failing")
    void emptyInvoice() {
        Invoice invoice = new Invoice();
        invoice.setTaxPercent(new BigDecimal("18.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getTaxAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getTotal()));
    }

    @Test
    @DisplayName("zero tax leaves the total equal to the subtotal")
    void zeroTax() {
        Invoice invoice = new Invoice();
        invoice.setTaxPercent(BigDecimal.ZERO);
        invoice.setItems(List.of(item("Dressing", 2, "125.50")));

        assertEquals(0, new BigDecimal("251.00").compareTo(invoice.getSubtotal()));
        assertEquals(0, new BigDecimal("251.00").compareTo(invoice.getTotal()));
    }

    @Test
    @DisplayName("tax is rounded to two decimal places, half up")
    void taxRounding() {
        Invoice invoice = new Invoice();
        invoice.setTaxPercent(new BigDecimal("7.50"));
        invoice.setItems(List.of(item("Consultation", 1, "333.33")));

        // 333.33 * 0.075 = 24.99975 -> 25.00
        assertEquals(0, new BigDecimal("25.00").compareTo(invoice.getTaxAmount()));
        assertEquals(0, new BigDecimal("358.33").compareTo(invoice.getTotal()));
        assertEquals(2, invoice.getTotal().scale(), "money is always to two places");
    }

    @Test
    @DisplayName("a null unit price is treated as zero, never a null pointer")
    void nullUnitPrice() {
        InvoiceItem blank = new InvoiceItem();
        blank.setDescription("Unpriced");
        blank.setUnitPrice(null);

        Invoice invoice = new Invoice();
        invoice.setItems(List.of(blank));

        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getSubtotal()));
    }

    @Test
    @DisplayName("only an unpaid invoice counts as outstanding")
    void outstanding() {
        Invoice invoice = new Invoice();

        invoice.setStatus(Invoice.STATUS_UNPAID);
        assertTrue(invoice.isOutstanding());

        invoice.setStatus(Invoice.STATUS_PAID);
        assertFalse(invoice.isOutstanding(), "paid money is not owed");

        invoice.setStatus(Invoice.STATUS_CANCELLED);
        assertFalse(invoice.isOutstanding(), "a cancelled invoice is not owed either");
    }

    @Test
    @DisplayName("a paid invoice is locked against editing")
    void paidInvoiceIsNotEditable() {
        Invoice invoice = new Invoice();

        invoice.setStatus(Invoice.STATUS_UNPAID);
        assertTrue(invoice.isEditable());

        invoice.setStatus(Invoice.STATUS_PAID);
        assertFalse(invoice.isEditable(),
                "changing the amount after payment would break the accounting trail");
    }
}

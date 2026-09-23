package com.hms.dao;

import com.hms.model.Invoice;
import com.hms.model.InvoiceItem;
import com.hms.model.Patient;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceDAOTest {

    private InvoiceDAO dao;
    private PatientDAO patientDAO;
    private int patientId;

    @BeforeEach
    void setUp() {
        DataSource ds = TestDatabase.create();
        dao = new InvoiceDAO(ds);
        patientDAO = new PatientDAO(ds);

        Patient p = new Patient();
        p.setFullName("Ananya Sharma");
        p.setGender("Female");
        patientId = patientDAO.insert(p).getId();
    }

    private Invoice invoice(String... descriptionsAndPrices) {
        Invoice i = new Invoice();
        i.setPatientId(patientId);
        i.setIssuedAt(LocalDateTime.now());
        i.setStatus(Invoice.STATUS_UNPAID);
        i.setTaxPercent(new BigDecimal("5.00"));

        List<InvoiceItem> items = new ArrayList<>();
        for (int k = 0; k < descriptionsAndPrices.length; k += 2) {
            InvoiceItem item = new InvoiceItem();
            item.setDescription(descriptionsAndPrices[k]);
            item.setQuantity(1);
            item.setUnitPrice(new BigDecimal(descriptionsAndPrices[k + 1]));
            items.add(item);
        }
        i.setItems(items);
        return i;
    }

    @Test
    @DisplayName("an invoice and its charges save and load together")
    void insertWithItems() {
        Invoice saved = dao.insert(invoice("Consultation", "900.00", "ECG", "450.00"));

        assertTrue(saved.getId() > 0);

        Invoice found = dao.findById(saved.getId()).orElseThrow();
        assertEquals(2, found.getItems().size());
        assertEquals("Ananya Sharma", found.getPatientName());
        assertEquals(0, new BigDecimal("1350.00").compareTo(found.getSubtotal()));
        assertEquals(0, new BigDecimal("1417.50").compareTo(found.getTotal()));
    }

    @Test
    @DisplayName("invoice numbers are allocated in sequence and zero padded")
    void invoiceNumbering() {
        Invoice first = dao.insert(invoice("Consultation", "500.00"));
        Invoice second = dao.insert(invoice("Consultation", "500.00"));

        assertEquals("INV-000001", first.getInvoiceNumber());
        assertEquals("INV-000002", second.getInvoiceNumber());
    }

    @Test
    @DisplayName("a deleted invoice does not cause its number to be reused")
    void numbersAreNotReused() {
        Invoice first = dao.insert(invoice("Consultation", "500.00"));
        Invoice second = dao.insert(invoice("Consultation", "500.00"));
        assertEquals("INV-000002", second.getInvoiceNumber());

        dao.delete(second.getId());

        Invoice third = dao.insert(invoice("Consultation", "500.00"));
        assertEquals("INV-000003", third.getInvoiceNumber(),
                "reusing a number would make two different invoices share an identity");
        assertNotNull(first.getInvoiceNumber());
    }

    @Test
    @DisplayName("an explicitly supplied invoice number is kept")
    void explicitNumberIsRespected() {
        Invoice manual = invoice("Consultation", "500.00");
        manual.setInvoiceNumber("MANUAL-01");

        assertEquals("MANUAL-01", dao.insert(manual).getInvoiceNumber());

        // A non-sequence number must not disturb the generated sequence.
        assertEquals("INV-000001", dao.insert(invoice("X", "10.00")).getInvoiceNumber());
    }

    @Test
    @DisplayName("updating replaces the charge rows wholesale")
    void updateReplacesItems() {
        Invoice saved = dao.insert(invoice("Consultation", "900.00", "ECG", "450.00"));

        Invoice edited = dao.findById(saved.getId()).orElseThrow();
        edited.setItems(List.of(invoice("Consultation", "900.00").getItems().get(0)));

        assertTrue(dao.update(edited));

        Invoice found = dao.findById(saved.getId()).orElseThrow();
        assertEquals(1, found.getItems().size(), "the removed charge should be gone");
        assertEquals(0, new BigDecimal("900.00").compareTo(found.getSubtotal()));
    }

    @Test
    @DisplayName("marking an invoice paid stamps when it was paid")
    void markingPaidStampsTheDate() {
        Invoice saved = dao.insert(invoice("Consultation", "900.00"));
        assertNull(saved.getPaidAt());

        assertTrue(dao.updateStatus(saved.getId(), Invoice.STATUS_PAID));

        Invoice found = dao.findById(saved.getId()).orElseThrow();
        assertEquals(Invoice.STATUS_PAID, found.getStatus());
        assertNotNull(found.getPaidAt(), "a paid invoice records when payment landed");
    }

    @Test
    @DisplayName("reverting an invoice to unpaid clears the payment date")
    void revertingClearsPaidDate() {
        Invoice saved = dao.insert(invoice("Consultation", "900.00"));
        dao.updateStatus(saved.getId(), Invoice.STATUS_PAID);
        dao.updateStatus(saved.getId(), Invoice.STATUS_UNPAID);

        assertNull(dao.findById(saved.getId()).orElseThrow().getPaidAt(),
                "an unpaid invoice must not keep a payment date");
    }

    @Test
    @DisplayName("status totals apply each invoice's own tax before summing")
    void sumTotalByStatus() {
        dao.insert(invoice("Consultation", "1000.00"));          // unpaid, 5% -> 1050
        Invoice paid = dao.insert(invoice("Consultation", "2000.00"));
        dao.updateStatus(paid.getId(), Invoice.STATUS_PAID);     // paid,   5% -> 2100

        assertEquals(0, new BigDecimal("1050.00")
                .compareTo(dao.sumTotalByStatus(Invoice.STATUS_UNPAID)));
        assertEquals(0, new BigDecimal("2100.00")
                .compareTo(dao.sumTotalByStatus(Invoice.STATUS_PAID)));
    }

    @Test
    @DisplayName("totalling a status with no invoices gives zero, not null")
    void sumWithNoRows() {
        assertEquals(0, BigDecimal.ZERO
                .compareTo(dao.sumTotalByStatus(Invoice.STATUS_CANCELLED)));
    }

    @Test
    @DisplayName("listing loads the charges, so totals are correct in the list view")
    void listLoadsItems() {
        dao.insert(invoice("Consultation", "900.00", "ECG", "450.00"));
        dao.insert(invoice("Dressing", "250.00"));

        List<Invoice> all = dao.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().allMatch(i -> !i.getItems().isEmpty()),
                "every listed invoice should carry its line items");
        assertTrue(all.stream().anyMatch(
                i -> new BigDecimal("1350.00").compareTo(i.getSubtotal()) == 0));
    }

    @Test
    @DisplayName("filtering by status returns only that status")
    void findByStatus() {
        Invoice paid = dao.insert(invoice("Consultation", "900.00"));
        dao.updateStatus(paid.getId(), Invoice.STATUS_PAID);
        dao.insert(invoice("Consultation", "500.00"));

        assertEquals(1, dao.findByStatus(Invoice.STATUS_PAID).size());
        assertEquals(1, dao.findByStatus(Invoice.STATUS_UNPAID).size());
        assertEquals(2, dao.count());
    }

    @Test
    @DisplayName("deleting a patient cascades to their invoices")
    void deletingPatientCascades() {
        dao.insert(invoice("Consultation", "900.00"));
        assertEquals(1, dao.count());

        patientDAO.delete(patientId);

        assertEquals(0, dao.count());
    }

    @Test
    @DisplayName("deleting an invoice removes its charge rows too")
    void deleteCascadesToItems() {
        Invoice saved = dao.insert(invoice("Consultation", "900.00", "ECG", "450.00"));

        assertTrue(dao.delete(saved.getId()));
        assertTrue(dao.findItems(saved.getId()).isEmpty());
        assertFalse(dao.delete(saved.getId()));
    }
}

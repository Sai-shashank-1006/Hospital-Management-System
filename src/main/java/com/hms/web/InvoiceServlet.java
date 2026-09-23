package com.hms.web;

import com.hms.dao.AppointmentDAO;
import com.hms.dao.DoctorDAO;
import com.hms.dao.InvoiceDAO;
import com.hms.dao.PatientDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Appointment;
import com.hms.model.Doctor;
import com.hms.model.Invoice;
import com.hms.model.InvoiceItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "InvoiceServlet", urlPatterns = "/invoices/*")
public class InvoiceServlet extends BaseServlet {

    private InvoiceDAO invoiceDAO;
    private PatientDAO patientDAO;
    private AppointmentDAO appointmentDAO;
    private DoctorDAO doctorDAO;

    @Override
    public void init() {
        invoiceDAO = new InvoiceDAO(DataSourceProvider.get());
        patientDAO = new PatientDAO(DataSourceProvider.get());
        appointmentDAO = new AppointmentDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "invoices");

        switch (action(req)) {
            case "/new" -> {
                Invoice invoice = new Invoice();
                invoice.setIssuedAt(LocalDateTime.now());
                invoice.setTaxPercent(new BigDecimal("5.00"));

                // "Generate from appointment": pre-fill the consultation fee as the
                // first line item, which is the automated-billing path from the brief.
                int appointmentId = intParam(req, "appointmentId", 0);
                if (appointmentId > 0) {
                    prefillFromAppointment(invoice, appointmentId);
                }

                req.setAttribute("invoice", invoice);
                populateFormLists(req);
                render(req, resp, "invoice-form");
            }
            case "/edit" -> {
                Optional<Invoice> found = invoiceDAO.findById(intParam(req, "id", 0));
                if (found.isEmpty()) {
                    Flash.error(req, "That invoice no longer exists.");
                    redirect(req, resp, "/invoices/");
                    return;
                }
                if (!found.get().isEditable()) {
                    Flash.error(req, "A paid invoice cannot be edited. Cancel it first.");
                    redirect(req, resp, "/invoices/");
                    return;
                }
                req.setAttribute("invoice", found.get());
                populateFormLists(req);
                render(req, resp, "invoice-form");
            }
            case "/view" -> {
                Optional<Invoice> found = invoiceDAO.findById(intParam(req, "id", 0));
                if (found.isEmpty()) {
                    Flash.error(req, "That invoice no longer exists.");
                    redirect(req, resp, "/invoices/");
                    return;
                }
                req.setAttribute("invoice", found.get());
                patientDAO.findById(found.get().getPatientId())
                        .ifPresent(p -> req.setAttribute("patient", p));
                render(req, resp, "invoice-view");
            }
            default -> {
                String status = trimmed(req, "status");
                List<Invoice> invoices = (status == null || "ALL".equals(status))
                        ? invoiceDAO.findAll()
                        : invoiceDAO.findByStatus(status);

                req.setAttribute("invoices", invoices);
                req.setAttribute("statusFilter", status == null ? "ALL" : status);
                req.setAttribute("outstandingTotal",
                        invoiceDAO.sumTotalByStatus(Invoice.STATUS_UNPAID));
                req.setAttribute("collectedTotal",
                        invoiceDAO.sumTotalByStatus(Invoice.STATUS_PAID));
                render(req, resp, "invoices");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "invoices");
        String action = action(req);

        if ("/delete".equals(action)) {
            if (invoiceDAO.delete(intParam(req, "id", 0))) {
                Flash.success(req, "Invoice deleted.");
            } else {
                Flash.error(req, "Could not delete that invoice.");
            }
            redirect(req, resp, "/invoices/");
            return;
        }

        if ("/status".equals(action)) {
            int id = intParam(req, "id", 0);
            String status = trimmed(req, "status");

            if (isKnownStatus(status) && invoiceDAO.updateStatus(id, status)) {
                Flash.success(req, "Invoice marked as " + status.toLowerCase() + ".");
            } else {
                Flash.error(req, "Could not update that invoice.");
            }
            redirect(req, resp, "/invoices/");
            return;
        }

        Invoice invoice = bindFrom(req);
        List<String> errors = validate(invoice);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("invoice", invoice);
            populateFormLists(req);
            render(req, resp, "invoice-form");
            return;
        }

        if (invoice.getId() > 0) {
            invoiceDAO.update(invoice);
            Flash.success(req, "Invoice " + invoice.getInvoiceNumber() + " updated.");
        } else {
            Invoice saved = invoiceDAO.insert(invoice);
            Flash.success(req, "Invoice " + saved.getInvoiceNumber() + " created.");
        }
        redirect(req, resp, "/invoices/");
    }

    /** Seeds a new invoice from a completed consultation. */
    private void prefillFromAppointment(Invoice invoice, int appointmentId) {
        Optional<Appointment> appointment = appointmentDAO.findById(appointmentId);
        if (appointment.isEmpty()) {
            return;
        }

        Appointment a = appointment.get();
        invoice.setAppointmentId(a.getId());
        invoice.setPatientId(a.getPatientId());

        Optional<Doctor> doctor = doctorDAO.findById(a.getDoctorId());
        if (doctor.isPresent()) {
            InvoiceItem consultation = new InvoiceItem();
            consultation.setDescription("Consultation - " + doctor.get().getSpecialization());
            consultation.setQuantity(1);
            consultation.setUnitPrice(doctor.get().getConsultationFee());
            invoice.getItems().add(consultation);
        }
    }

    private void populateFormLists(HttpServletRequest req) {
        req.setAttribute("patients", patientDAO.findAll());
        req.setAttribute("appointments", appointmentDAO.findAll());
    }

    private boolean isKnownStatus(String status) {
        return Invoice.STATUS_UNPAID.equals(status)
                || Invoice.STATUS_PAID.equals(status)
                || Invoice.STATUS_CANCELLED.equals(status);
    }

    private Invoice bindFrom(HttpServletRequest req) {
        Invoice i = new Invoice();
        i.setId(intParam(req, "id", 0));
        i.setInvoiceNumber(trimmed(req, "invoiceNumber"));
        i.setPatientId(intParam(req, "patientId", 0));

        int appointmentId = intParam(req, "appointmentId", 0);
        i.setAppointmentId(appointmentId > 0 ? appointmentId : null);

        LocalDateTime issuedAt = dateTimeParam(req, "issuedAt");
        i.setIssuedAt(issuedAt == null ? LocalDateTime.now() : issuedAt);

        String status = trimmed(req, "status");
        i.setStatus(isKnownStatus(status) ? status : Invoice.STATUS_UNPAID);

        i.setTaxPercent(decimalParam(req, "taxPercent", BigDecimal.ZERO));
        i.setNotes(trimmed(req, "notes"));
        i.setItems(bindItems(req));

        // Keep paid_at consistent with the status the form submitted.
        if (Invoice.STATUS_PAID.equals(i.getStatus())) {
            i.setPaidAt(LocalDateTime.now());
        }
        return i;
    }

    /** Reads the repeating charge rows, dropping any the user left blank. */
    private List<InvoiceItem> bindItems(HttpServletRequest req) {
        String[] descriptions = req.getParameterValues("itemDescription");
        if (descriptions == null) {
            return List.of();
        }

        String[] quantities = req.getParameterValues("itemQuantity");
        String[] prices = req.getParameterValues("itemUnitPrice");

        List<InvoiceItem> items = new ArrayList<>();

        for (int i = 0; i < descriptions.length; i++) {
            String description = descriptions[i] == null ? "" : descriptions[i].trim();
            if (description.isEmpty()) {
                continue;
            }

            InvoiceItem item = new InvoiceItem();
            item.setDescription(description);
            item.setQuantity(Math.max(1, parseInt(quantities, i, 1)));
            item.setUnitPrice(parseDecimal(prices, i));
            items.add(item);
        }
        return items;
    }

    private int parseInt(String[] values, int index, int fallback) {
        if (values == null || index >= values.length || values[index] == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(values[index].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal parseDecimal(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(values[index].trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private List<String> validate(Invoice i) {
        List<String> errors = new ArrayList<>();

        if (i.getPatientId() <= 0) {
            errors.add("Please choose a patient.");
        }
        if (i.getItems().isEmpty()) {
            errors.add("Add at least one charge.");
        }
        if (i.getTaxPercent().signum() < 0 || i.getTaxPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            errors.add("Tax must be between 0 and 100 percent.");
        }
        if (i.getItems().stream().anyMatch(item -> item.getUnitPrice().signum() < 0)) {
            errors.add("A charge cannot have a negative price.");
        }
        return errors;
    }
}

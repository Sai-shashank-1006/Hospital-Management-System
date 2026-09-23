package com.hms.web;

import com.hms.dao.AppointmentDAO;
import com.hms.dao.DoctorDAO;
import com.hms.dao.PatientDAO;
import com.hms.dao.PrescriptionDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Prescription;
import com.hms.model.PrescriptionItem;
import com.hms.model.Role;
import com.hms.model.User;
import com.hms.security.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "PrescriptionServlet", urlPatterns = "/prescriptions/*")
public class PrescriptionServlet extends BaseServlet {

    private PrescriptionDAO prescriptionDAO;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() {
        prescriptionDAO = new PrescriptionDAO(DataSourceProvider.get());
        patientDAO = new PatientDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
        appointmentDAO = new AppointmentDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "prescriptions");
        User user = SessionUser.current(req);

        switch (action(req)) {
            case "/new" -> {
                Prescription prescription = new Prescription();
                prescription.setIssuedAt(LocalDateTime.now());
                // A doctor writing a prescription defaults to themselves.
                if (user.getRole() == Role.DOCTOR && user.getDoctorId() != null) {
                    prescription.setDoctorId(user.getDoctorId());
                }
                req.setAttribute("prescription", prescription);
                populateFormLists(req);
                render(req, resp, "prescription-form");
            }
            case "/edit" -> {
                Optional<Prescription> found =
                        prescriptionDAO.findById(intParam(req, "id", 0));
                if (found.isEmpty()) {
                    Flash.error(req, "That prescription no longer exists.");
                    redirect(req, resp, "/prescriptions/");
                    return;
                }
                req.setAttribute("prescription", found.get());
                populateFormLists(req);
                render(req, resp, "prescription-form");
            }
            case "/view" -> {
                Optional<Prescription> found =
                        prescriptionDAO.findById(intParam(req, "id", 0));
                if (found.isEmpty()) {
                    Flash.error(req, "That prescription no longer exists.");
                    redirect(req, resp, "/prescriptions/");
                    return;
                }
                req.setAttribute("prescription", found.get());
                patientDAO.findById(found.get().getPatientId())
                        .ifPresent(p -> req.setAttribute("patient", p));
                render(req, resp, "prescription-view");
            }
            default -> {
                // Doctors see only what they wrote; administrators see everything.
                List<Prescription> list =
                        (user.getRole() == Role.DOCTOR && user.getDoctorId() != null)
                                ? prescriptionDAO.findByDoctor(user.getDoctorId())
                                : prescriptionDAO.findAll();

                req.setAttribute("prescriptions", list);
                req.setAttribute("scopedToSelf", user.getRole() == Role.DOCTOR);
                render(req, resp, "prescriptions");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "prescriptions");

        if ("/delete".equals(action(req))) {
            if (prescriptionDAO.delete(intParam(req, "id", 0))) {
                Flash.success(req, "Prescription deleted.");
            } else {
                Flash.error(req, "Could not delete that prescription.");
            }
            redirect(req, resp, "/prescriptions/");
            return;
        }

        Prescription prescription = bindFrom(req);
        List<String> errors = validate(prescription);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("prescription", prescription);
            populateFormLists(req);
            render(req, resp, "prescription-form");
            return;
        }

        if (prescription.getId() > 0) {
            prescriptionDAO.update(prescription);
            Flash.success(req, "Prescription updated.");
        } else {
            prescriptionDAO.insert(prescription);
            Flash.success(req, "Prescription saved.");
        }
        redirect(req, resp, "/prescriptions/");
    }

    private void populateFormLists(HttpServletRequest req) {
        req.setAttribute("patients", patientDAO.findAll());
        req.setAttribute("doctors", doctorDAO.findAll());
        req.setAttribute("appointments", appointmentDAO.findAll());
    }

    private Prescription bindFrom(HttpServletRequest req) {
        Prescription p = new Prescription();
        p.setId(intParam(req, "id", 0));
        p.setPatientId(intParam(req, "patientId", 0));
        p.setDoctorId(intParam(req, "doctorId", 0));

        int appointmentId = intParam(req, "appointmentId", 0);
        p.setAppointmentId(appointmentId > 0 ? appointmentId : null);

        LocalDateTime issuedAt = dateTimeParam(req, "issuedAt");
        p.setIssuedAt(issuedAt == null ? LocalDateTime.now() : issuedAt);

        p.setDiagnosis(trimmed(req, "diagnosis"));
        p.setNotes(trimmed(req, "notes"));
        p.setItems(bindItems(req));
        return p;
    }

    /**
     * Reads the repeating medicine rows.
     *
     * <p>The form posts parallel arrays, one per column. A row with no medicine name
     * is treated as an empty row the user left behind and is dropped, so blank rows
     * never reach the database.
     */
    private List<PrescriptionItem> bindItems(HttpServletRequest req) {
        String[] medicines = req.getParameterValues("itemMedicine");
        if (medicines == null) {
            return List.of();
        }

        String[] dosages = req.getParameterValues("itemDosage");
        String[] frequencies = req.getParameterValues("itemFrequency");
        String[] durations = req.getParameterValues("itemDuration");
        String[] instructions = req.getParameterValues("itemInstructions");

        List<PrescriptionItem> items = new ArrayList<>();

        for (int i = 0; i < medicines.length; i++) {
            String medicine = medicines[i] == null ? "" : medicines[i].trim();
            if (medicine.isEmpty()) {
                continue;
            }

            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(medicine);
            item.setDosage(valueAt(dosages, i));
            item.setFrequency(valueAt(frequencies, i));
            item.setDuration(valueAt(durations, i));
            item.setInstructions(valueAt(instructions, i));
            items.add(item);
        }
        return items;
    }

    private String valueAt(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return null;
        }
        String value = values[index].trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> validate(Prescription p) {
        List<String> errors = new ArrayList<>();

        if (p.getPatientId() <= 0) {
            errors.add("Please choose a patient.");
        }
        if (p.getDoctorId() <= 0) {
            errors.add("Please choose the prescribing doctor.");
        }
        if (p.getDiagnosis() == null) {
            errors.add("A diagnosis is required.");
        }
        if (p.getItems().isEmpty()) {
            errors.add("Add at least one medicine.");
        }
        if (p.getIssuedAt() != null && p.getIssuedAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            errors.add("A prescription cannot be dated in the future.");
        }
        return errors;
    }
}

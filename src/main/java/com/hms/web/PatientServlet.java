package com.hms.web;

import com.hms.dao.PatientDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Patient;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "PatientServlet", urlPatterns = "/patients/*")
public class PatientServlet extends BaseServlet {

    private PatientDAO patientDAO;

    @Override
    public void init() {
        patientDAO = new PatientDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "patients");

        switch (action(req)) {
            case "/new" -> {
                req.setAttribute("patient", new Patient());
                render(req, resp, "patient-form");
            }
            case "/edit" -> {
                Optional<Patient> patient = patientDAO.findById(intParam(req, "id", 0));
                if (patient.isEmpty()) {
                    Flash.error(req, "That patient no longer exists.");
                    redirect(req, resp, "/patients/");
                    return;
                }
                req.setAttribute("patient", patient.get());
                render(req, resp, "patient-form");
            }
            default -> {
                String search = trimmed(req, "q");
                req.setAttribute("patients", patientDAO.search(search));
                req.setAttribute("search", search);
                render(req, resp, "patients");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "patients");

        if ("/delete".equals(action(req))) {
            int id = intParam(req, "id", 0);
            if (patientDAO.delete(id)) {
                Flash.success(req, "Patient deleted. Their appointments were removed as well.");
            } else {
                Flash.error(req, "Could not delete that patient.");
            }
            redirect(req, resp, "/patients/");
            return;
        }

        Patient patient = bindFrom(req);
        List<String> errors = validate(patient);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("patient", patient);
            render(req, resp, "patient-form");
            return;
        }

        if (patient.getId() > 0) {
            patientDAO.update(patient);
            Flash.success(req, "Patient \"" + patient.getFullName() + "\" updated.");
        } else {
            patientDAO.insert(patient);
            Flash.success(req, "Patient \"" + patient.getFullName() + "\" registered.");
        }
        redirect(req, resp, "/patients/");
    }

    private Patient bindFrom(HttpServletRequest req) {
        Patient p = new Patient();
        p.setId(intParam(req, "id", 0));
        p.setFullName(trimmed(req, "fullName"));
        p.setGender(trimmed(req, "gender"));
        p.setDateOfBirth(dateParam(req, "dateOfBirth"));
        p.setPhone(trimmed(req, "phone"));
        p.setEmail(trimmed(req, "email"));
        p.setAddress(trimmed(req, "address"));
        p.setBloodGroup(trimmed(req, "bloodGroup"));
        return p;
    }

    private List<String> validate(Patient p) {
        List<String> errors = new ArrayList<>();
        if (p.getFullName() == null) {
            errors.add("Full name is required.");
        }
        if (p.getGender() == null) {
            errors.add("Gender is required.");
        }
        if (p.getPhone() != null && !p.getPhone().matches("[0-9+\\-\\s()]{6,20}")) {
            errors.add("Phone number contains characters that are not valid.");
        }
        if (p.getEmail() != null && !p.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.add("Email address is not valid.");
        }
        if (p.getDateOfBirth() != null && p.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
            errors.add("Date of birth cannot be in the future.");
        }
        return errors;
    }
}

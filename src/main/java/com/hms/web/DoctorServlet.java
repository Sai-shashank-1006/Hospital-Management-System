package com.hms.web;

import com.hms.dao.DoctorDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Doctor;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "DoctorServlet", urlPatterns = "/doctors/*")
public class DoctorServlet extends BaseServlet {

    private DoctorDAO doctorDAO;

    @Override
    public void init() {
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "doctors");

        switch (action(req)) {
            case "/new" -> {
                req.setAttribute("doctor", new Doctor());
                render(req, resp, "doctor-form");
            }
            case "/edit" -> {
                Optional<Doctor> doctor = doctorDAO.findById(intParam(req, "id", 0));
                if (doctor.isEmpty()) {
                    Flash.error(req, "That doctor no longer exists.");
                    redirect(req, resp, "/doctors/");
                    return;
                }
                req.setAttribute("doctor", doctor.get());
                render(req, resp, "doctor-form");
            }
            default -> {
                req.setAttribute("doctors", doctorDAO.findAll());
                render(req, resp, "doctors");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "doctors");

        if ("/delete".equals(action(req))) {
            if (doctorDAO.delete(intParam(req, "id", 0))) {
                Flash.success(req, "Doctor removed from the roster.");
            } else {
                Flash.error(req, "Could not remove that doctor.");
            }
            redirect(req, resp, "/doctors/");
            return;
        }

        Doctor doctor = bindFrom(req);
        List<String> errors = validate(doctor);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("doctor", doctor);
            render(req, resp, "doctor-form");
            return;
        }

        if (doctor.getId() > 0) {
            doctorDAO.update(doctor);
            Flash.success(req, "Dr. " + doctor.getFullName() + " updated.");
        } else {
            doctorDAO.insert(doctor);
            Flash.success(req, "Dr. " + doctor.getFullName() + " added to the roster.");
        }
        redirect(req, resp, "/doctors/");
    }

    private Doctor bindFrom(HttpServletRequest req) {
        Doctor d = new Doctor();
        d.setId(intParam(req, "id", 0));
        d.setFullName(trimmed(req, "fullName"));
        d.setSpecialization(trimmed(req, "specialization"));
        d.setPhone(trimmed(req, "phone"));
        d.setEmail(trimmed(req, "email"));
        d.setConsultationFee(decimalParam(req, "consultationFee", BigDecimal.ZERO));
        d.setAvailable(req.getParameter("available") != null);
        return d;
    }

    private List<String> validate(Doctor d) {
        List<String> errors = new ArrayList<>();
        if (d.getFullName() == null) {
            errors.add("Full name is required.");
        }
        if (d.getSpecialization() == null) {
            errors.add("Specialization is required.");
        }
        if (d.getEmail() != null && !d.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.add("Email address is not valid.");
        }
        if (d.getConsultationFee() != null && d.getConsultationFee().signum() < 0) {
            errors.add("Consultation fee cannot be negative.");
        }
        return errors;
    }
}

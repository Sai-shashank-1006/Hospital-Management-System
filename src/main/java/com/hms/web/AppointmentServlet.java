package com.hms.web;

import com.hms.dao.AppointmentDAO;
import com.hms.dao.DoctorDAO;
import com.hms.dao.PatientDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Appointment;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "AppointmentServlet", urlPatterns = "/appointments/*")
public class AppointmentServlet extends BaseServlet {

    private AppointmentDAO appointmentDAO;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    @Override
    public void init() {
        appointmentDAO = new AppointmentDAO(DataSourceProvider.get());
        patientDAO = new PatientDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "appointments");

        switch (action(req)) {
            case "/new" -> {
                req.setAttribute("appointment", new Appointment());
                populateFormLists(req);
                render(req, resp, "appointment-form");
            }
            case "/edit" -> {
                Optional<Appointment> appointment =
                        appointmentDAO.findById(intParam(req, "id", 0));
                if (appointment.isEmpty()) {
                    Flash.error(req, "That appointment no longer exists.");
                    redirect(req, resp, "/appointments/");
                    return;
                }
                req.setAttribute("appointment", appointment.get());
                populateFormLists(req);
                render(req, resp, "appointment-form");
            }
            default -> {
                req.setAttribute("appointments", appointmentDAO.findAll());
                render(req, resp, "appointments");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "appointments");
        String action = action(req);

        if ("/delete".equals(action)) {
            if (appointmentDAO.delete(intParam(req, "id", 0))) {
                Flash.success(req, "Appointment deleted.");
            } else {
                Flash.error(req, "Could not delete that appointment.");
            }
            redirect(req, resp, "/appointments/");
            return;
        }

        if ("/status".equals(action)) {
            int id = intParam(req, "id", 0);
            String status = trimmed(req, "status");
            if (isKnownStatus(status) && appointmentDAO.updateStatus(id, status)) {
                Flash.success(req, "Appointment marked as " + status.toLowerCase() + ".");
            } else {
                Flash.error(req, "Could not update that appointment.");
            }
            redirect(req, resp, "/appointments/");
            return;
        }

        Appointment appointment = bindFrom(req);
        List<String> errors = validate(appointment);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("appointment", appointment);
            populateFormLists(req);
            render(req, resp, "appointment-form");
            return;
        }

        if (appointment.getId() > 0) {
            appointmentDAO.update(appointment);
            Flash.success(req, "Appointment updated.");
        } else {
            appointmentDAO.insert(appointment);
            Flash.success(req, "Appointment booked.");
        }
        redirect(req, resp, "/appointments/");
    }

    private void populateFormLists(HttpServletRequest req) {
        req.setAttribute("patients", patientDAO.findAll());
        req.setAttribute("doctors", doctorDAO.findAvailable());
    }

    private boolean isKnownStatus(String status) {
        return Appointment.STATUS_SCHEDULED.equals(status)
                || Appointment.STATUS_COMPLETED.equals(status)
                || Appointment.STATUS_CANCELLED.equals(status);
    }

    private Appointment bindFrom(HttpServletRequest req) {
        Appointment a = new Appointment();
        a.setId(intParam(req, "id", 0));
        a.setPatientId(intParam(req, "patientId", 0));
        a.setDoctorId(intParam(req, "doctorId", 0));
        a.setAppointmentTime(dateTimeParam(req, "appointmentTime"));
        a.setReason(trimmed(req, "reason"));

        String status = trimmed(req, "status");
        a.setStatus(isKnownStatus(status) ? status : Appointment.STATUS_SCHEDULED);
        return a;
    }

    private List<String> validate(Appointment a) {
        List<String> errors = new ArrayList<>();

        if (a.getPatientId() <= 0) {
            errors.add("Please choose a patient.");
        }
        if (a.getDoctorId() <= 0) {
            errors.add("Please choose a doctor.");
        }
        if (a.getAppointmentTime() == null) {
            errors.add("Please provide a valid appointment date and time.");
            return errors;
        }

        // Only new bookings must be in the future; an existing record may be edited
        // after the fact, for example to mark it completed.
        if (a.getId() == 0 && a.getAppointmentTime().isBefore(java.time.LocalDateTime.now())) {
            errors.add("Appointment time cannot be in the past.");
        }

        if (a.getDoctorId() > 0
                && appointmentDAO.hasConflict(a.getDoctorId(), a.getAppointmentTime(), a.getId())) {
            errors.add("That doctor already has an appointment at this time.");
        }
        return errors;
    }
}

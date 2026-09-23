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

@WebServlet(name = "DashboardServlet", urlPatterns = {"", "/dashboard"})
public class DashboardServlet extends BaseServlet {

    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;

    @Override
    public void init() {
        patientDAO = new PatientDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
        appointmentDAO = new AppointmentDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("patientCount", patientDAO.count());
        req.setAttribute("doctorCount", doctorDAO.count());
        req.setAttribute("appointmentCount", appointmentDAO.count());
        req.setAttribute("scheduledCount",
                appointmentDAO.countByStatus(Appointment.STATUS_SCHEDULED));
        req.setAttribute("upcoming", appointmentDAO.findUpcoming(5));

        req.setAttribute("activeNav", "dashboard");
        render(req, resp, "dashboard");
    }
}

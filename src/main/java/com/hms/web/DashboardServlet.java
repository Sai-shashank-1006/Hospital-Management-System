package com.hms.web;

import com.hms.dao.AppointmentDAO;
import com.hms.dao.DoctorDAO;
import com.hms.dao.InvoiceDAO;
import com.hms.dao.PatientDAO;
import com.hms.dao.PrescriptionDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Appointment;
import com.hms.model.Invoice;
import com.hms.model.Role;
import com.hms.model.User;
import com.hms.security.SessionUser;
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
    private InvoiceDAO invoiceDAO;
    private PrescriptionDAO prescriptionDAO;

    @Override
    public void init() {
        patientDAO = new PatientDAO(DataSourceProvider.get());
        doctorDAO = new DoctorDAO(DataSourceProvider.get());
        appointmentDAO = new AppointmentDAO(DataSourceProvider.get());
        invoiceDAO = new InvoiceDAO(DataSourceProvider.get());
        prescriptionDAO = new PrescriptionDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = SessionUser.current(req);

        req.setAttribute("patientCount", patientDAO.count());
        req.setAttribute("doctorCount", doctorDAO.count());
        req.setAttribute("appointmentCount", appointmentDAO.count());
        req.setAttribute("scheduledCount",
                appointmentDAO.countByStatus(Appointment.STATUS_SCHEDULED));
        req.setAttribute("upcoming", appointmentDAO.findUpcoming(5));

        // Billing figures are shown only to roles that may open the billing area,
        // so the dashboard never leaks a number the user cannot drill into.
        if (user.getRole().canAccess("/invoices")) {
            req.setAttribute("outstandingTotal",
                    invoiceDAO.sumTotalByStatus(Invoice.STATUS_UNPAID));
            req.setAttribute("unpaidCount",
                    invoiceDAO.countByStatus(Invoice.STATUS_UNPAID));
        }

        if (user.getRole().canAccess("/prescriptions")) {
            req.setAttribute("prescriptionCount", prescriptionDAO.count());
        }

        // Doctors get their own upcoming list rather than the whole hospital's.
        if (user.getRole() == Role.DOCTOR && user.getDoctorId() != null) {
            req.setAttribute("myUpcoming",
                    appointmentDAO.findUpcomingForDoctor(user.getDoctorId(), 5));
        }

        req.setAttribute("activeNav", "dashboard");
        render(req, resp, "dashboard");
    }
}

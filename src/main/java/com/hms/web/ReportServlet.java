package com.hms.web;

import com.hms.dao.InvoiceDAO;
import com.hms.dao.PatientDAO;
import com.hms.dao.PrescriptionDAO;
import com.hms.dao.ReportDAO;
import com.hms.db.DataSourceProvider;
import com.hms.model.Invoice;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "ReportServlet", urlPatterns = "/reports/*")
public class ReportServlet extends BaseServlet {

    private ReportDAO reportDAO;
    private InvoiceDAO invoiceDAO;
    private PatientDAO patientDAO;
    private PrescriptionDAO prescriptionDAO;

    @Override
    public void init() {
        reportDAO = new ReportDAO(DataSourceProvider.get());
        invoiceDAO = new InvoiceDAO(DataSourceProvider.get());
        patientDAO = new PatientDAO(DataSourceProvider.get());
        prescriptionDAO = new PrescriptionDAO(DataSourceProvider.get());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("activeNav", "reports");

        // Headline figures.
        req.setAttribute("patientCount", patientDAO.count());
        req.setAttribute("prescriptionCount", prescriptionDAO.count());
        req.setAttribute("collectedTotal", invoiceDAO.sumTotalByStatus(Invoice.STATUS_PAID));
        req.setAttribute("outstandingTotal", invoiceDAO.sumTotalByStatus(Invoice.STATUS_UNPAID));

        // Trends over the last six months. Appointments and revenue are separate
        // charts rather than one with two y-axes: different units on a shared axis
        // invite false comparisons between the two lines.
        req.setAttribute("appointmentsByMonth",
                ChartSeries.ofCounts(reportDAO.appointmentsByMonth(6)));
        req.setAttribute("revenueByMonth",
                ChartSeries.ofMoney(reportDAO.revenueByMonth(6)));

        // Breakdowns.
        req.setAttribute("appointmentsByStatus",
                ChartSeries.ofCounts(reportDAO.appointmentsByStatus()));
        req.setAttribute("appointmentsByDoctor",
                ChartSeries.ofCounts(reportDAO.appointmentsByDoctor(5)));
        req.setAttribute("appointmentsBySpecialization",
                ChartSeries.ofCounts(reportDAO.appointmentsBySpecialization()));
        req.setAttribute("patientsByGender",
                ChartSeries.ofCounts(reportDAO.patientsByGender()));
        req.setAttribute("patientsByBloodGroup",
                ChartSeries.ofCounts(reportDAO.patientsByBloodGroup()));
        req.setAttribute("patientsByInsurance",
                ChartSeries.ofCounts(reportDAO.patientsByInsurance()));
        req.setAttribute("invoicesByStatus",
                ChartSeries.ofCounts(reportDAO.invoicesByStatus()));
        req.setAttribute("topMedicines",
                ChartSeries.ofCounts(reportDAO.topMedicines(5)));

        render(req, resp, "reports");
    }
}

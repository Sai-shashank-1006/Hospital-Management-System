package com.hms.dao;

import com.hms.model.Appointment;
import com.hms.model.Doctor;
import com.hms.model.Patient;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentDAOTest {

    private AppointmentDAO dao;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    private int patientId;
    private int doctorId;

    @BeforeEach
    void setUp() {
        DataSource ds = TestDatabase.create();
        dao = new AppointmentDAO(ds);
        patientDAO = new PatientDAO(ds);
        doctorDAO = new DoctorDAO(ds);

        Patient p = new Patient();
        p.setFullName("Ananya Sharma");
        p.setGender("Female");
        patientId = patientDAO.insert(p).getId();

        Doctor d = new Doctor();
        d.setFullName("Priya Menon");
        d.setSpecialization("Cardiology");
        d.setConsultationFee(new BigDecimal("900.00"));
        d.setAvailable(true);
        doctorId = doctorDAO.insert(d).getId();
    }

    private Appointment booking(LocalDateTime when) {
        Appointment a = new Appointment();
        a.setPatientId(patientId);
        a.setDoctorId(doctorId);
        a.setAppointmentTime(when);
        a.setReason("Chest discomfort");
        a.setStatus(Appointment.STATUS_SCHEDULED);
        return a;
    }

    @Test
    @DisplayName("reads join the patient and doctor names for display")
    void readsIncludeJoinedNames() {
        Appointment saved = dao.insert(booking(LocalDateTime.now().plusDays(1)));

        Appointment found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("Ananya Sharma", found.getPatientName());
        assertEquals("Priya Menon", found.getDoctorName());
        assertEquals("Cardiology", found.getDoctorSpecialization());
    }

    @Test
    @DisplayName("a second booking for the same doctor at the same time is a conflict")
    void detectsDoubleBooking() {
        LocalDateTime slot = LocalDateTime.now().plusDays(1).withNano(0);
        dao.insert(booking(slot));

        assertTrue(dao.hasConflict(doctorId, slot, 0));
        assertFalse(dao.hasConflict(doctorId, slot.plusHours(1), 0),
                "a different time slot is free");
    }

    @Test
    @DisplayName("a cancelled appointment frees its slot")
    void cancelledAppointmentsDoNotConflict() {
        LocalDateTime slot = LocalDateTime.now().plusDays(1).withNano(0);
        Appointment saved = dao.insert(booking(slot));

        dao.updateStatus(saved.getId(), Appointment.STATUS_CANCELLED);

        assertFalse(dao.hasConflict(doctorId, slot, 0),
                "the slot should be bookable again once cancelled");
    }

    @Test
    @DisplayName("editing an appointment does not conflict with itself")
    void excludesItselfFromConflictCheck() {
        LocalDateTime slot = LocalDateTime.now().plusDays(1).withNano(0);
        Appointment saved = dao.insert(booking(slot));

        assertFalse(dao.hasConflict(doctorId, slot, saved.getId()),
                "the row being edited must be excluded from the conflict check");
    }

    @Test
    @DisplayName("upcoming lists only future scheduled appointments, soonest first")
    void upcomingOrdersByTime() {
        dao.insert(booking(LocalDateTime.now().plusDays(3)));
        dao.insert(booking(LocalDateTime.now().plusDays(1)));
        dao.insert(booking(LocalDateTime.now().minusDays(1)));

        Appointment cancelled = dao.insert(booking(LocalDateTime.now().plusDays(2)));
        dao.updateStatus(cancelled.getId(), Appointment.STATUS_CANCELLED);

        List<Appointment> upcoming = dao.findUpcoming(10);

        assertEquals(2, upcoming.size(), "past and cancelled appointments are excluded");
        assertTrue(upcoming.get(0).getAppointmentTime()
                        .isBefore(upcoming.get(1).getAppointmentTime()),
                "soonest appointment should come first");
    }

    @Test
    @DisplayName("upcoming honours the row limit")
    void upcomingRespectsLimit() {
        dao.insert(booking(LocalDateTime.now().plusDays(1)));
        dao.insert(booking(LocalDateTime.now().plusDays(2)));
        dao.insert(booking(LocalDateTime.now().plusDays(3)));

        assertEquals(2, dao.findUpcoming(2).size());
    }

    @Test
    @DisplayName("countByStatus counts each status separately")
    void countByStatus() {
        dao.insert(booking(LocalDateTime.now().plusDays(1)));
        Appointment second = dao.insert(booking(LocalDateTime.now().plusDays(2)));
        dao.updateStatus(second.getId(), Appointment.STATUS_COMPLETED);

        assertEquals(1, dao.countByStatus(Appointment.STATUS_SCHEDULED));
        assertEquals(1, dao.countByStatus(Appointment.STATUS_COMPLETED));
        assertEquals(0, dao.countByStatus(Appointment.STATUS_CANCELLED));
        assertEquals(2, dao.count());
    }

    @Test
    @DisplayName("deleting a patient cascades to their appointments")
    void deletingPatientCascades() {
        dao.insert(booking(LocalDateTime.now().plusDays(1)));
        assertEquals(1, dao.count());

        patientDAO.delete(patientId);

        assertEquals(0, dao.count(), "the appointment should have been removed with the patient");
    }
}

package com.hms.dao;

import com.hms.model.Doctor;
import com.hms.model.Patient;
import com.hms.model.Prescription;
import com.hms.model.PrescriptionItem;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrescriptionDAOTest {

    private PrescriptionDAO dao;
    private PatientDAO patientDAO;
    private DoctorDAO doctorDAO;

    private int patientId;
    private int doctorId;
    private int otherDoctorId;

    @BeforeEach
    void setUp() {
        DataSource ds = TestDatabase.create();
        dao = new PrescriptionDAO(ds);
        patientDAO = new PatientDAO(ds);
        doctorDAO = new DoctorDAO(ds);

        Patient p = new Patient();
        p.setFullName("Imran Khan");
        p.setGender("Male");
        patientId = patientDAO.insert(p).getId();

        doctorId = insertDoctor("Priya Menon", "Cardiology");
        otherDoctorId = insertDoctor("Sameer Gupta", "General Medicine");
    }

    private int insertDoctor(String name, String specialization) {
        Doctor d = new Doctor();
        d.setFullName(name);
        d.setSpecialization(specialization);
        return doctorDAO.insert(d).getId();
    }

    private Prescription prescription(int forDoctorId, String... medicines) {
        Prescription p = new Prescription();
        p.setPatientId(patientId);
        p.setDoctorId(forDoctorId);
        p.setIssuedAt(LocalDateTime.now());
        p.setDiagnosis("Stable angina");
        p.setNotes("Review in three months.");

        List<PrescriptionItem> items = new ArrayList<>();
        for (String medicine : medicines) {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(medicine);
            item.setDosage("20 mg");
            item.setFrequency("Once daily");
            item.setDuration("90 days");
            items.add(item);
        }
        p.setItems(items);
        return p;
    }

    @Test
    @DisplayName("a prescription and its medicines save and load together")
    void insertWithItems() {
        Prescription saved = dao.insert(prescription(doctorId, "Atorvastatin", "Aspirin"));

        assertTrue(saved.getId() > 0);

        Prescription found = dao.findById(saved.getId()).orElseThrow();
        assertEquals(2, found.getItems().size());
        assertEquals("Stable angina", found.getDiagnosis());
        assertEquals("Atorvastatin", found.getItems().get(0).getMedicine());
    }

    @Test
    @DisplayName("reads join the patient and doctor names for display")
    void readsIncludeJoinedNames() {
        Prescription saved = dao.insert(prescription(doctorId, "Atorvastatin"));

        Prescription found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("Imran Khan", found.getPatientName());
        assertEquals("Priya Menon", found.getDoctorName());
        assertEquals("Cardiology", found.getDoctorSpecialization());
    }

    @Test
    @DisplayName("a doctor's own list excludes prescriptions written by others")
    void findByDoctorIsScoped() {
        dao.insert(prescription(doctorId, "Atorvastatin"));
        dao.insert(prescription(doctorId, "Aspirin"));
        dao.insert(prescription(otherDoctorId, "Paracetamol"));

        assertEquals(2, dao.findByDoctor(doctorId).size());
        assertEquals(1, dao.findByDoctor(otherDoctorId).size());
        assertEquals(3, dao.findAll().size(), "an administrator still sees them all");
    }

    @Test
    @DisplayName("updating replaces the medicine rows wholesale")
    void updateReplacesItems() {
        Prescription saved = dao.insert(prescription(doctorId, "Atorvastatin", "Aspirin"));

        Prescription edited = dao.findById(saved.getId()).orElseThrow();
        edited.setDiagnosis("Reviewed diagnosis");
        edited.setItems(prescription(doctorId, "Clopidogrel").getItems());

        assertTrue(dao.update(edited));

        Prescription found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("Reviewed diagnosis", found.getDiagnosis());
        assertEquals(1, found.getItems().size());
        assertEquals("Clopidogrel", found.getItems().get(0).getMedicine());
    }

    @Test
    @DisplayName("updating a prescription that does not exist reports false")
    void updateMissingRow() {
        Prescription ghost = prescription(doctorId, "Atorvastatin");
        ghost.setId(4242);

        assertFalse(dao.update(ghost));
    }

    @Test
    @DisplayName("a prescription can be recorded without any linked appointment")
    void appointmentLinkIsOptional() {
        Prescription saved = dao.insert(prescription(doctorId, "Atorvastatin"));

        assertTrue(dao.findById(saved.getId()).orElseThrow().getAppointmentId() == null);
    }

    @Test
    @DisplayName("deleting a prescription removes its medicines too")
    void deleteCascadesToItems() {
        Prescription saved = dao.insert(prescription(doctorId, "Atorvastatin", "Aspirin"));

        assertTrue(dao.delete(saved.getId()));
        assertTrue(dao.findItems(saved.getId()).isEmpty());
        assertEquals(0, dao.count());
    }

    @Test
    @DisplayName("a patient's history lists their prescriptions")
    void findByPatient() {
        dao.insert(prescription(doctorId, "Atorvastatin"));
        dao.insert(prescription(otherDoctorId, "Paracetamol"));

        assertEquals(2, dao.findByPatient(patientId).size());
        assertEquals(0, dao.findByPatient(9999).size());
    }

    @Test
    @DisplayName("deleting a patient cascades to their prescriptions")
    void deletingPatientCascades() {
        dao.insert(prescription(doctorId, "Atorvastatin"));
        assertEquals(1, dao.count());

        patientDAO.delete(patientId);

        assertEquals(0, dao.count());
    }
}

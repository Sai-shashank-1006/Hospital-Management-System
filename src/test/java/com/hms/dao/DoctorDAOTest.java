package com.hms.dao;

import com.hms.model.Doctor;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorDAOTest {

    private DoctorDAO dao;

    @BeforeEach
    void setUp() {
        dao = new DoctorDAO(TestDatabase.create());
    }

    private Doctor sample(String name, String specialization, boolean available) {
        Doctor d = new Doctor();
        d.setFullName(name);
        d.setSpecialization(specialization);
        d.setPhone("9001122334");
        d.setEmail(name.toLowerCase().replace(' ', '.') + "@hospital.example");
        d.setConsultationFee(new BigDecimal("750.00"));
        d.setAvailable(available);
        return d;
    }

    @Test
    @DisplayName("insert stores every field, including the fee and availability flag")
    void insertAndFind() {
        Doctor saved = dao.insert(sample("Priya Menon", "Cardiology", true));

        assertTrue(saved.getId() > 0);

        Doctor found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("Priya Menon", found.getFullName());
        assertEquals("Cardiology", found.getSpecialization());
        assertEquals(0, new BigDecimal("750.00").compareTo(found.getConsultationFee()));
        assertTrue(found.isAvailable());
    }

    @Test
    @DisplayName("a null consultation fee is stored as zero rather than failing")
    void nullFeeDefaultsToZero() {
        Doctor d = sample("No Fee", "General Medicine", true);
        d.setConsultationFee(null);

        Doctor found = dao.findById(dao.insert(d).getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(found.getConsultationFee()));
    }

    @Test
    @DisplayName("findAvailable excludes doctors who are off duty")
    void findAvailableFiltersOffDuty() {
        dao.insert(sample("Priya Menon", "Cardiology", true));
        dao.insert(sample("Arjun Rao", "Orthopaedics", true));
        dao.insert(sample("Nisha Patel", "Dermatology", false));

        List<Doctor> available = dao.findAvailable();

        assertEquals(2, available.size());
        assertTrue(available.stream().allMatch(Doctor::isAvailable));
        assertEquals(3, dao.findAll().size(), "findAll still returns the whole roster");
    }

    @Test
    @DisplayName("findAll sorts the roster by name")
    void findAllSortsByName() {
        dao.insert(sample("Priya Menon", "Cardiology", true));
        dao.insert(sample("Arjun Rao", "Orthopaedics", true));

        assertEquals("Arjun Rao", dao.findAll().get(0).getFullName());
    }

    @Test
    @DisplayName("toggling availability persists")
    void updateAvailability() {
        Doctor saved = dao.insert(sample("Priya Menon", "Cardiology", true));

        saved.setAvailable(false);
        assertTrue(dao.update(saved));

        assertFalse(dao.findById(saved.getId()).orElseThrow().isAvailable());
        assertEquals(0, dao.findAvailable().size());
    }

    @Test
    @DisplayName("delete removes the doctor and count reflects it")
    void deleteAndCount() {
        Doctor saved = dao.insert(sample("Priya Menon", "Cardiology", true));
        assertEquals(1, dao.count());

        assertTrue(dao.delete(saved.getId()));
        assertEquals(0, dao.count());
        assertFalse(dao.delete(saved.getId()));
    }
}

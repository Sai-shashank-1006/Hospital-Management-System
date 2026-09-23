package com.hms.dao;

import com.hms.model.Patient;
import com.hms.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientDAOTest {

    private PatientDAO dao;

    @BeforeEach
    void setUp() {
        dao = new PatientDAO(TestDatabase.create());
    }

    private Patient sample(String name, String phone) {
        Patient p = new Patient();
        p.setFullName(name);
        p.setGender("Female");
        p.setDateOfBirth(LocalDate.of(1990, 5, 12));
        p.setPhone(phone);
        p.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        p.setAddress("1 Test Street");
        p.setBloodGroup("O+");
        return p;
    }

    @Test
    @DisplayName("insert assigns a generated id and the row can be read back")
    void insertAndFind() {
        Patient saved = dao.insert(sample("Ananya Sharma", "9876543210"));

        assertTrue(saved.getId() > 0, "insert should populate the generated id");

        Optional<Patient> found = dao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Ananya Sharma", found.get().getFullName());
        assertEquals("O+", found.get().getBloodGroup());
        assertEquals(LocalDate.of(1990, 5, 12), found.get().getDateOfBirth());
    }

    @Test
    @DisplayName("a patient with no date of birth round-trips as null rather than failing")
    void nullDateOfBirthIsAllowed() {
        Patient p = sample("No Birthday", "9000000000");
        p.setDateOfBirth(null);

        Patient saved = dao.insert(p);
        Patient found = dao.findById(saved.getId()).orElseThrow();

        assertNull(found.getDateOfBirth());
        assertNull(found.getAge(), "age should be absent when no date of birth is recorded");
    }

    @Test
    @DisplayName("update overwrites the stored values")
    void update() {
        Patient saved = dao.insert(sample("Old Name", "9111111111"));

        saved.setFullName("New Name");
        saved.setBloodGroup("AB-");
        assertTrue(dao.update(saved));

        Patient found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("New Name", found.getFullName());
        assertEquals("AB-", found.getBloodGroup());
    }

    @Test
    @DisplayName("update returns false when the id does not exist")
    void updateMissingRow() {
        Patient ghost = sample("Ghost", "9222222222");
        ghost.setId(4242);

        assertFalse(dao.update(ghost));
    }

    @Test
    @DisplayName("delete removes the row and reports whether anything was deleted")
    void delete() {
        Patient saved = dao.insert(sample("To Delete", "9333333333"));

        assertTrue(dao.delete(saved.getId()));
        assertTrue(dao.findById(saved.getId()).isEmpty());
        assertFalse(dao.delete(saved.getId()), "second delete should report no rows affected");
    }

    @Test
    @DisplayName("search matches name, phone and email case-insensitively")
    void search() {
        dao.insert(sample("Ananya Sharma", "9876543210"));
        dao.insert(sample("Rahul Verma", "9812345678"));

        assertEquals(1, dao.search("ananya").size(), "should match on partial lowercase name");
        assertEquals(1, dao.search("ANANYA").size(), "should ignore case");
        assertEquals(1, dao.search("98123").size(), "should match on a phone fragment");
        assertEquals(1, dao.search("rahul.verma@example.com").size(), "should match on email");
        assertEquals(0, dao.search("nobody").size());
    }

    @Test
    @DisplayName("a blank search term returns every patient")
    void blankSearchReturnsAll() {
        dao.insert(sample("Ananya Sharma", "9876543210"));
        dao.insert(sample("Rahul Verma", "9812345678"));

        assertEquals(2, dao.search("   ").size());
        assertEquals(2, dao.search(null).size());
    }

    @Test
    @DisplayName("count tracks inserts and deletes")
    void count() {
        assertEquals(0, dao.count());

        Patient first = dao.insert(sample("First Patient", "9444444444"));
        dao.insert(sample("Second Patient", "9555555555"));
        assertEquals(2, dao.count());

        dao.delete(first.getId());
        assertEquals(1, dao.count());
    }

    @Test
    @DisplayName("findAll lists the newest patient first")
    void findAllIsNewestFirst() {
        dao.insert(sample("Older Record", "9666666666"));
        dao.insert(sample("Newer Record", "9777777777"));

        List<Patient> all = dao.findAll();
        assertEquals("Newer Record", all.get(0).getFullName());
    }
}

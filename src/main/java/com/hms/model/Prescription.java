package com.hms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Prescription {

    private int id;
    private int patientId;
    private int doctorId;
    private Integer appointmentId;
    private LocalDateTime issuedAt;
    private String diagnosis;
    private String notes;

    /** Joined in by the DAO for display; not persisted on this table. */
    private String patientName;
    private String doctorName;
    private String doctorSpecialization;

    private List<PrescriptionItem> items = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorSpecialization() {
        return doctorSpecialization;
    }

    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    public int getItemCount() {
        return items.size();
    }

    public String getFormattedIssuedAt() {
        if (issuedAt == null) {
            return "";
        }
        return issuedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public String getInputValue() {
        if (issuedAt == null) {
            return "";
        }
        return issuedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}

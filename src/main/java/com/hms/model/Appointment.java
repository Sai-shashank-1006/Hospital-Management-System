package com.hms.model;

import java.time.LocalDateTime;

public class Appointment {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private int patientId;
    private int doctorId;
    private LocalDateTime appointmentTime;
    private String reason;
    private String status = STATUS_SCHEDULED;

    /** Joined in by the DAO for display; not persisted on the appointments table. */
    private String patientName;
    private String doctorName;
    private String doctorSpecialization;

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

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    /** Human-readable time for the list views, e.g. "25 Sep 2026, 10:30". */
    public String getFormattedTime() {
        if (appointmentTime == null) {
            return "";
        }
        return appointmentTime.format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    /** Value for an {@code <input type="datetime-local">}, which needs exactly "yyyy-MM-ddTHH:mm". */
    public String getInputValue() {
        if (appointmentTime == null) {
            return "";
        }
        return appointmentTime.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}

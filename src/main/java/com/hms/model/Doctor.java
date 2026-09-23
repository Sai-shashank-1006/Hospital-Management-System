package com.hms.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class Doctor {

    private int id;
    private String fullName;
    private String specialization;
    private String phone;
    private String email;
    private BigDecimal consultationFee = BigDecimal.ZERO;
    private boolean available = true;
    /** Comma-separated day codes, e.g. "MON,WED,FRI". */
    private String availableDays;
    private LocalTime availableFrom;
    private LocalTime availableTo;
    private String roomNumber;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public LocalTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalTime getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(LocalTime availableTo) {
        this.availableTo = availableTo;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    /** The scheduled days as a list, for ticking checkboxes on the form. */
    public List<String> getAvailableDayList() {
        if (availableDays == null || availableDays.isBlank()) {
            return List.of();
        }
        return Arrays.stream(availableDays.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .toList();
    }

    public boolean worksOn(String dayCode) {
        return getAvailableDayList().contains(dayCode);
    }

    /** The consulting window as "09:00 - 13:00", or empty when not set. */
    public String getFormattedHours() {
        if (availableFrom == null || availableTo == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return availableFrom.format(fmt) + " - " + availableTo.format(fmt);
    }

    /** Value for an {@code <input type="time">}, which needs "HH:mm". */
    public String getFromInputValue() {
        return availableFrom == null ? "" : availableFrom.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getToInputValue() {
        return availableTo == null ? "" : availableTo.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

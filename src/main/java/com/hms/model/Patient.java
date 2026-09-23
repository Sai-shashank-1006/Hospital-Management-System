package com.hms.model;

import java.time.LocalDate;

public class Patient {

    private int id;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;

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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    /** Age in whole years, or {@code null} when no date of birth is on record. */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** Human-readable date of birth for the list views, e.g. "12 May 1990". */
    public String getFormattedDob() {
        if (dateOfBirth == null) {
            return "";
        }
        return dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    /** Value for an {@code <input type="date">}, which needs ISO "yyyy-MM-dd". */
    public String getInputValue() {
        return dateOfBirth == null ? "" : dateOfBirth.toString();
    }
}

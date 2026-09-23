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
    private String insuranceProvider;
    private String insuranceNumber;
    private String allergies;
    private String medicalHistory;

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

    public String getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(String insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    /** True when the patient has insurance on file, used to badge the list view. */
    public boolean isInsured() {
        return insuranceProvider != null && !insuranceProvider.isBlank();
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

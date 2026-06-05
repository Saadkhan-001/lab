package com.lab.lms.models;

public class Patient {
    private String patientId;
    private String name;
    private int age;
    private int ageMonths;
    private int ageDays;
    private String gender;
    private String phone;
    private String whatsapp;
    private String address;
    private String referredDoctor;
    private String registrationDate;
    
    private int totalTests;
    private double totalPaid;
    private double totalPending;
    private double totalDiscount;

    private final String lowercaseName;
    private final String lowercaseId;
    private java.time.LocalDate parsedDate;

    public Patient(String patientId, String name, int age, int ageMonths, int ageDays, String gender, String phone, String whatsapp, String address,
            String referredDoctor, String registrationDate) {
        this(patientId, name, age, ageMonths, ageDays, gender, phone, whatsapp, address, referredDoctor, registrationDate, 0, 0.0, 0.0, 0.0);
    }

    public Patient(String patientId, String name, int age, int ageMonths, int ageDays, String gender, String phone, String whatsapp, String address,
            String referredDoctor, String registrationDate, int totalTests, double totalPaid, double totalPending, double totalDiscount) {
        this.patientId = patientId;
        this.name = name;
        this.lowercaseName = (name == null) ? "" : name.toLowerCase();
        this.lowercaseId = (patientId == null) ? "" : patientId.toLowerCase();
        this.age = age;
        this.ageMonths = ageMonths;
        this.ageDays = ageDays;
        this.gender = gender;
        this.phone = phone;
        this.whatsapp = whatsapp;
        this.address = address;
        this.referredDoctor = referredDoctor;
        this.registrationDate = registrationDate;
        
        this.totalTests = totalTests;
        this.totalPaid = totalPaid;
        this.totalPending = totalPending;
        this.totalDiscount = totalDiscount;
        
        if (registrationDate != null && registrationDate.length() >= 10) {
            try {
                this.parsedDate = java.time.LocalDate.parse(registrationDate.substring(0, 10));
            } catch (Exception e) {}
        }
    }

    public String getLowercaseName() { return lowercaseName; }
    public String getLowercaseId() { return lowercaseId; }
    public java.time.LocalDate getParsedDate() { return parsedDate; }

    // Getters
    public String getPatientId() {
        return patientId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
    
    public int getAgeMonths() {
        return ageMonths;
    }
    
    public int getAgeDays() {
        return ageDays;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getAddress() {
        return address;
    }

    public String getReferredDoctor() {
        return referredDoctor;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public double getTotalPending() {
        return totalPending;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    @Override
    public String toString() {
        if (name == null || name.trim().isEmpty()) {
            return patientId != null ? patientId : "";
        }
        return patientId + " - " + name;
    }
}

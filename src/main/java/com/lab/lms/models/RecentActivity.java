package com.lab.lms.models;

public class RecentActivity {
    private String patientId;
    private String name;
    private String status;
    private String phone;
    private String time;
    private String paid;
    private String discount;
    private String remaining;

    public RecentActivity(String patientId, String name, String status, String phone, String time, String paid, String discount, String remaining) {
        this.patientId = patientId;
        this.name = name;
        this.status = status;
        this.phone = phone;
        this.time = time;
        this.paid = paid;
        this.discount = discount;
        this.remaining = remaining;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getPhone() {
        return phone;
    }

    public String getTime() {
        return time;
    }

    public String getPaid() {
        return paid;
    }

    public String getDiscount() {
        return discount;
    }

    public String getRemaining() {
        return remaining;
    }
}

package com.lab.lms.models;

public class SummaryReportRow {
    private int id;
    private String date;
    private String mrNumber;
    private String patientName;
    private String tests;
    private String doctor;
    private double originalTotal;
    private double discount;
    private double amount; // Final Payable
    private double paid;
    private double due;
    private String status;
    private String performedBy;

    public SummaryReportRow(int id, String date, String mrNumber, String patientName, String tests, String doctor, 
                            double originalTotal, double discount, double amount, double paid, double due, String status, String performedBy) {
        this.id = id;
        this.date = date;
        this.mrNumber = mrNumber;
        this.patientName = patientName;
        this.tests = tests;
        this.doctor = doctor;
        this.originalTotal = originalTotal;
        this.discount = discount;
        this.amount = amount;
        this.paid = paid;
        this.due = due;
        this.status = status;
        this.performedBy = performedBy;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public String getMrNumber() { return mrNumber; }
    public String getPatientName() { return patientName; }
    public String getTests() { return tests; }
    public String getDoctor() { return doctor; }
    public double getOriginalTotal() { return originalTotal; }
    public double getDiscount() { return discount; }
    public double getAmount() { return amount; }
    public double getPaid() { return paid; }
    public double getDue() { return due; }
    public String getStatus() { return status; }
    public String getPerformedBy() { return performedBy; }
}

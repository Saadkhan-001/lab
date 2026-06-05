package com.lab.lms.models;

public class PendingApproval {
    private String sampleId;
    private String patientId;
    private String patientName;
    private int testId;
    private String testName;

    public PendingApproval(String sampleId, String patientId, String patientName, int testId, String testName) {
        this.sampleId = sampleId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.testId = testId;
        this.testName = testName;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public int getTestId() {
        return testId;
    }

    public String getTestName() {
        return testName;
    }
}

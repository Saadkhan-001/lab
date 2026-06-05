package com.lab.lms.models;

public class VisitHistory {
    private String date;
    private String sampleId;
    private String testName;
    private String status;
    private String pdfPath;
    private int testId;
    private String patientName;
    private String phone;
    private String sampleStatus;
    private int approvalStatus;
    private String patientId;
    private String resultComment;

    public String getPatientId() { return patientId; }
    public String getResultComment() { return resultComment == null ? "" : resultComment; }
    public void setResultComment(String comment) { this.resultComment = comment; }

    private boolean detailRow;
    private javafx.beans.property.BooleanProperty selected = new javafx.beans.property.SimpleBooleanProperty(this, "selected", false);
    private boolean expanded;

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
    public boolean isDetailRow() { return detailRow; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public VisitHistory(String date, String sampleId, String testName, String status, String pdfPath, int testId,
            String patientName, String phone, String sampleStatus, int approvalStatus, String patientId) {
        this.date = date;
        this.sampleId = sampleId;
        this.testName = testName;
        this.status = status;
        this.pdfPath = pdfPath;
        this.testId = testId;
        this.patientName = patientName;
        this.phone = phone;
        this.sampleStatus = sampleStatus;
        this.approvalStatus = approvalStatus;
        this.patientId = patientId;
        this.resultComment = "";
        this.detailRow = false;
        this.expanded = false;
    }

    public String getDate() { return date; }
    public String getSampleId() { return sampleId; }
    public String getTestName() { return testName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String p) { this.pdfPath = p; }
    public int getTestId() { return testId; }
    public String getPatientName() { return patientName; }
    public String getPhone() { return phone; }
    public String getSampleStatus() { return sampleStatus; }
    public int getApprovalStatus() { return approvalStatus; }
}

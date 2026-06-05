package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.VisitHistory;
import com.lab.lms.services.NavigationService;
import com.lab.lms.services.SessionContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import java.io.File;
import java.awt.Desktop;
import java.sql.*;
import java.util.*;

public class PatientProfileController {

    @FXML
    private Label lblHeaderName, lblHeaderId, lblAgeGender, lblPhone, lblWhatsapp, lblAddress, lblReferred;
    @FXML
    private Label lblTotalVisits, lblPendingReports, lblLastVisit;
    @FXML
    private TableView<VisitHistory> historyTable;
    @FXML
    private TableColumn<VisitHistory, String> colDate, colId, colTest, colStatus;
    @FXML
    private TableColumn<VisitHistory, Boolean> colSelect;
    @FXML
    private TableColumn<VisitHistory, Void> colAction;
    @FXML
    private Button btnPrintSelected;
    @FXML
    private Button btnWhatsAppSelected;
    @FXML
    private Button btnDeletePatient;

    private ObservableList<VisitHistory> historyData = FXCollections.observableArrayList();
    private String currentPid;

    @FXML
    public void initialize() {
        currentPid = SessionContext.getCurrentPatientId();
        if (currentPid == null)
            return;

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colId.setCellValueFactory(new PropertyValueFactory<>("sampleId"));
        colTest.setCellValueFactory(new PropertyValueFactory<>("testName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSelect.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colSelect.setCellFactory(column -> new TableCell<VisitHistory, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    VisitHistory v = getTableView().getItems().get(getIndex());
                    v.setSelected(checkBox.isSelected());
                    updatePrintButtonVisibility();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    VisitHistory v = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(v.isSelected());
                    setGraphic(checkBox);
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    VisitHistory v = getTableView().getItems().get(getIndex());
                    String displayStatus = "PENDING";
                    String bgColor = "#ECEFF1"; // Slate Light
                    String textColor = "#455A64"; // Slate Dark

                    if ("COLLECTED".equalsIgnoreCase(v.getSampleStatus())) {
                        if ("COMPLETED".equalsIgnoreCase(v.getStatus())) {
                            if (v.getApprovalStatus() == 1) {
                                displayStatus = "COMPLETED";
                                bgColor = "#FFEBEE"; 
                                textColor = "#961111"; 
                            } else {
                                displayStatus = "READY FOR REVIEW";
                                bgColor = "#F4F7F9"; 
                                textColor = "#455A64"; 
                            }
                        } else {
                            displayStatus = "IN PROCESSING";
                            bgColor = "#F4F7F9"; 
                            textColor = "#455A64";
                        }
                    } else if ("AWAITING COLLECTION".equalsIgnoreCase(v.getSampleStatus())) {
                        displayStatus = "WAITING FOR SAMPLE";
                        bgColor = "#F4F7F9"; 
                        textColor = "#455A64"; 
                    }

                    Label label = new Label(displayStatus.toUpperCase());
                    label.setStyle("-fx-background-color: " + bgColor + "; " +
                            "-fx-text-fill: " + textColor + "; " +
                            "-fx-padding: 4 10; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 10px; " +
                            "-fx-min-width: 130; " +
                            "-fx-alignment: center; " +
                            "-fx-font-family: 'Segoe UI', 'Inter', sans-serif; " +
                            "-fx-border-color: " + textColor + "; -fx-border-width: 0.5; -fx-border-radius: 2;");
                    setGraphic(label);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        setupActionColumn();
        loadPatientInfo();
        loadVisitHistory();
        historyTable.setItems(historyData);

        // Security Surveillance: Enforce Admin-Only Deletion Rights
        boolean canDelete = SessionContext.hasPermission("test_deletion");
        btnDeletePatient.setVisible(canDelete);
        btnDeletePatient.setManaged(canDelete);
    }

    private void loadPatientInfo() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM patients WHERE patient_id = ?");
            pstmt.setString(1, currentPid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                lblHeaderName.setText(name != null ? name.toUpperCase() : "UNKNOWN");
                lblHeaderId.setText("MR NO: " + currentPid);
                
                String gender = rs.getString("gender");
                String genderDisplay = (gender != null) ? gender.toUpperCase() : "N/A";
                
                int y = rs.getInt("age");
                int m = rs.getInt("age_months");
                int d = rs.getInt("age_days");
                String ageStr = y + "y";
                if (m > 0 || d > 0) ageStr += " " + m + "m " + d + "d";
                
                lblAgeGender.setText(ageStr + " / " + genderDisplay);
                
                lblPhone.setText(rs.getString("phone"));
                lblWhatsapp.setText(rs.getString("whatsapp"));
                lblAddress.setText(rs.getString("address"));
                lblReferred.setText(rs.getString("referred_doctor"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadVisitHistory() {
        historyData.clear();
        int pending = 0;
        String latest = "N/A";

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT s.collection_date, s.sample_id, t.name as test_name, r.status, r.pdf_path, t.id as test_id, p.name as p_name, "
                    + "COALESCE(p.whatsapp, p.phone) as contact_phone, s.status as sample_status, MIN(r.doctor_approval) as approval "
                    + "FROM samples s " +
                    "JOIN patients p ON s.patient_id = p.patient_id " +
                    "JOIN results r ON s.sample_id = r.sample_id " +
                    "JOIN tests t ON r.test_id = t.id " +
                    "WHERE s.patient_id = ? " +
                    "GROUP BY s.sample_id, t.id " +
                    "ORDER BY s.collection_date DESC";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, currentPid);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String status = rs.getString("status");
                String date = rs.getString("collection_date");
                if (latest.equals("N/A"))
                    latest = date != null ? date.split(" ")[0] : "N/A";
                if (!"COMPLETED".equals(status))
                    pending++;

                historyData.add(new VisitHistory(
                        date,
                        rs.getString("sample_id"),
                        rs.getString("test_name"),
                        status,
                        rs.getString("pdf_path"),
                        rs.getInt("test_id"),
                        rs.getString("p_name"),
                        rs.getString("contact_phone"),
                        rs.getString("sample_status"),
                        rs.getInt("approval"),
                        currentPid));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        lblTotalVisits.setText(String.valueOf(historyData.size()));
        lblPendingReports.setText(String.valueOf(pending));
        lblLastVisit.setText(latest);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<VisitHistory, Void> call(TableColumn<VisitHistory, Void> param) {
                return new TableCell<>() {
                    private final Button viewBtn = new Button("VIEW");
                    private final Button editBtn = new Button("EDIT");
                    private final Button whatsappBtn = new Button("WHATSAPP");
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, viewBtn,
                            editBtn, whatsappBtn);
                    {
                        viewBtn.getStyleClass().add("button-sm-teal");
                        editBtn.getStyleClass().add("button-sm-red");
                        whatsappBtn.getStyleClass().add("button-sm-red");
                        container.setAlignment(javafx.geometry.Pos.CENTER);

                        viewBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            openReport(history.getPdfPath());
                        });

                        editBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            com.lab.lms.services.SessionContext.setCurrentSampleId(history.getSampleId());
                            com.lab.lms.services.SessionContext.setCurrentTestId(history.getTestId());
                            com.lab.lms.services.NavigationService.switchView("/fxml/processing.fxml");
                        });

                        whatsappBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            com.lab.lms.services.WhatsAppService.sendReportWithRecovery(history.getPatientId(), history.getPatientName(),
                                    history.getPhone(), history.getPdfPath());
                        });

                        // Administrative Deletion Capability
                        Button deleteBtn = new Button("DELETE");
                        deleteBtn.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-cursor: hand;");
                        
                        boolean canDeleteVisit = SessionContext.hasPermission("test_deletion");
                        if (canDeleteVisit) {
                            container.getChildren().add(deleteBtn);
                        }

                        deleteBtn.setOnAction(event -> {
                            VisitHistory row = getTableView().getItems().get(getIndex());
                            handleDeleteVisit(row);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            VisitHistory row = getTableView().getItems().get(getIndex());
                            boolean hasPdf = row.getPdfPath() != null && !row.getPdfPath().isEmpty();
                            boolean isCompleted = "COMPLETED".equalsIgnoreCase(row.getStatus());

                            viewBtn.setDisable(!hasPdf);
                            whatsappBtn.setDisable(!hasPdf || !isCompleted);

                            setGraphic(container);
                        }
                    }
                };
            }
        });
    }

    private void openReport(String path) {
        if (path == null || path.isEmpty())
            return;
        try {
            File pdfFile = new File(path);
            if (pdfFile.exists()) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                new Alert(Alert.AlertType.ERROR, "Report file not found on disk.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeletePatient() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("CRITICAL: CLINICAL RECORD PURGE");
        alert.setHeaderText("Permanently delete patient: " + lblHeaderName.getText() + "?");
        alert.setContentText("This action will IRREVERSIBLY remove all clinical records, sample history, and results associated with this MR number.\n\nProceed with full system purge?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Phase 1: Wipe clinical data layers (Corrected for Schema Linkage)
                    // Results and Images are linked via sample_id, not directly to patient
                    String[] subqueryTables = {"results", "test_images"};
                    for (String table : subqueryTables) {
                        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + table + " WHERE sample_id IN (SELECT sample_id FROM samples WHERE patient_id = ?)")) {
                            pstmt.setString(1, currentPid);
                            pstmt.executeUpdate();
                        }
                    }

                    // Phase 2: Purge direct patient links
                    String[] directTables = {"samples", "invoices"};
                    for (String table : directTables) {
                        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + table + " WHERE patient_id = ?")) {
                            pstmt.setString(1, currentPid);
                            pstmt.executeUpdate();
                        }
                    }
                    
                    // Phase 3: Purge identity master
                    try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM patients WHERE patient_id = ?")) {
                        pstmt.setString(1, currentPid);
                        pstmt.executeUpdate();
                    }
                    
                    conn.commit();
                    NavigationService.switchView("/fxml/patients.fxml");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "System Purge Failed: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void handleBack() {
        NavigationService.switchView("/fxml/patients.fxml");
    }

    @FXML
    private void handleEditProfile() {
        SessionContext.setCurrentPatientId(currentPid);
        SessionContext.setEditProfileMode(true);
        NavigationService.switchView("/fxml/registration.fxml");
    }

    @FXML
    private void handleNewTest() {
        SessionContext.setCurrentPatientId(currentPid);
        SessionContext.setEditProfileMode(false);
        NavigationService.switchView("/fxml/registration.fxml");
    }

    @FXML
    private void handleDeleteVisit(VisitHistory row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("CONFIRM DELETION");
        alert.setHeaderText("Delete test visit: " + row.getTestName() + "?");
        alert.setContentText("Do you want to confirm delete this test? This will remove all associated results for sample " + row.getSampleId() + ".");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Phase 1: Remove results for this specific test in this specific sample
                    String delResSql = "DELETE FROM results WHERE sample_id = ? AND test_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(delResSql)) {
                        pstmt.setString(1, row.getSampleId());
                        pstmt.setInt(2, row.getTestId());
                        pstmt.executeUpdate();
                    }

                    // Phase 2: Remove test images
                    String delImgSql = "DELETE FROM test_images WHERE sample_id = ? AND test_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(delImgSql)) {
                        pstmt.setString(1, row.getSampleId());
                        pstmt.setInt(2, row.getTestId());
                        pstmt.executeUpdate();
                    }

                    // Phase 3: Check for sample orphans
                    String checkSql = "SELECT COUNT(*) FROM results WHERE sample_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                        pstmt.setString(1, row.getSampleId());
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next() && rs.getInt(1) == 0) {
                            // No more tests in this sample, delete sample record
                            try (PreparedStatement delSample = conn.prepareStatement("DELETE FROM samples WHERE sample_id = ?")) {
                                delSample.setString(1, row.getSampleId());
                                delSample.executeUpdate();
                            }
                        }
                    }

                    conn.commit();
                    loadVisitHistory(); // Refresh table
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Deletion Failed: " + e.getMessage()).show();
            }
        }
    }

    private void updatePrintButtonVisibility() {
        boolean anySelected = historyData.stream().anyMatch(VisitHistory::isSelected);
        btnPrintSelected.setVisible(anySelected);
        btnPrintSelected.setManaged(anySelected);
        btnWhatsAppSelected.setVisible(anySelected);
        btnWhatsAppSelected.setManaged(anySelected);
    }

    @FXML
    private void handlePrintSelectedTests() {
        java.util.List<VisitHistory> selected = historyData.stream()
                .filter(VisitHistory::isSelected)
                .collect(java.util.stream.Collectors.toList());

        if (selected.isEmpty()) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            java.util.List<com.lab.lms.services.ReportGenerator.TestData> allTestData = new java.util.ArrayList<>();
            
            for (VisitHistory v : selected) {
                String resSql = "SELECT tp.name, r.value, tp.unit, tp.min_range, tp.max_range, r.is_abnormal " +
                                "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                "WHERE r.sample_id = ? AND tp.test_id = ?";
                PreparedStatement rsPstmt = conn.prepareStatement(resSql);
                rsPstmt.setString(1, v.getSampleId());
                rsPstmt.setInt(2, v.getTestId());
                ResultSet resRs = rsPstmt.executeQuery();

                java.util.List<java.util.Map<String, String>> results = new java.util.ArrayList<>();
                while (resRs.next()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("name", resRs.getString("name"));
                    map.put("value", resRs.getString("value"));
                    map.put("unit", resRs.getString("unit"));
                    String min = resRs.getString("min_range");
                    String max = resRs.getString("max_range");
                    String range = (min == null || min.isEmpty()) ? (max == null ? "" : max) : (max == null || max.isEmpty() ? min : min + " - " + max);
                    map.put("range", range);
                    map.put("status", resRs.getInt("is_abnormal") == 1 ? "ABNORMAL" : "NORMAL");
                    results.add(map);
                }
                
                String noteSql = "SELECT notes, category, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings FROM tests WHERE id = ?";
                PreparedStatement ntPstmt = conn.prepareStatement(noteSql);
                ntPstmt.setInt(1, v.getTestId());
                ResultSet ntRs = ntPstmt.executeQuery();
                String notes = "", category = "ACTIVE", spec = "Blood", gStatus = "Positive", gFindings = "";
                int isSp = 0, isMic = 0, isCul = 0;
                if (ntRs.next()) {
                    notes = ntRs.getString("notes");
                    category = ntRs.getString("category");
                    isSp = ntRs.getInt("is_special");
                    isMic = ntRs.getInt("is_microscopic");
                    isCul = ntRs.getInt("is_culture");
                    spec = ntRs.getString("specimen");
                    gStatus = ntRs.getString("growth_status");
                    if (gStatus == null) gStatus = "Positive";
                    gFindings = ntRs.getString("growth_findings");
                }

                allTestData.add(new com.lab.lms.services.ReportGenerator.TestData(v.getTestName(), v.getTestId(), category, results, notes, isSp, isMic, isCul, spec, "", "", new ArrayList<>(), v.getSampleId(), gStatus));
                if (!allTestData.isEmpty()) {
                    allTestData.get(allTestData.size()-1).growthFindings = gFindings;
                }
            }

            // Fetch patient info for report header
            String pAge = "N/A", pGender = "N/A", pRef = "N/A", sDate = selected.get(0).getDate(), phone = "", address = "";
            if (sDate != null && sDate.contains(" ")) sDate = sDate.split(" ")[0];
            PreparedStatement metaPstmt = conn.prepareStatement("SELECT age, age_months, age_days, gender, address, referred_doctor, whatsapp, phone FROM patients WHERE patient_id = ?");
            metaPstmt.setString(1, currentPid);
            ResultSet mRs = metaPstmt.executeQuery();
            if (mRs.next()) {
                int y = mRs.getInt("age");
                int m = mRs.getInt("age_months");
                int d = mRs.getInt("age_days");
                pAge = y + "y";
                if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                pGender = mRs.getString("gender");
                address = mRs.getString("address");
                pRef = mRs.getString("referred_doctor");
                phone = mRs.getString("whatsapp");
                if (phone == null || phone.isEmpty()) phone = mRs.getString("phone");
            }

            boolean includeHeader = Boolean.parseBoolean(DatabaseManager.getSetting("print_header_footer", "true"));
            String pdfPath = com.lab.lms.services.ReportGenerator.generateMultiTestReport(
                    lblHeaderName.getText(), currentPid, pAge, pGender, pRef, sDate, phone, address, allTestData, includeHeader, true);

            if (pdfPath != null) {
                Desktop.getDesktop().open(new File(pdfPath));
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Bulk Print Failure: " + e.getMessage()).show();
        }
    }
    @FXML
    private void handleWhatsAppSelectedTests() {
        java.util.List<VisitHistory> selected = historyData.stream()
                .filter(VisitHistory::isSelected)
                .collect(java.util.stream.Collectors.toList());

        if (selected.isEmpty()) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            java.util.List<com.lab.lms.services.ReportGenerator.TestData> allTestData = new java.util.ArrayList<>();
            
            for (VisitHistory v : selected) {
                String resSql = "SELECT tp.name, r.value, tp.unit, tp.min_range, tp.max_range, r.is_abnormal " +
                                "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                "WHERE r.sample_id = ? AND tp.test_id = ?";
                PreparedStatement rsPstmt = conn.prepareStatement(resSql);
                rsPstmt.setString(1, v.getSampleId());
                rsPstmt.setInt(2, v.getTestId());
                ResultSet resRs = rsPstmt.executeQuery();

                java.util.List<java.util.Map<String, String>> results = new java.util.ArrayList<>();
                while (resRs.next()) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("name", resRs.getString("name"));
                    map.put("value", resRs.getString("value") == null ? "" : resRs.getString("value"));
                    map.put("unit", resRs.getString("unit"));
                    String min = resRs.getString("min_range");
                    String max = resRs.getString("max_range");
                    String range = (min == null || min.isEmpty()) ? (max == null ? "" : max) : (max == null || max.isEmpty() ? min : min + " - " + max);
                    map.put("range", range);
                    map.put("status", resRs.getInt("is_abnormal") == 1 ? "ABNORMAL" : "NORMAL");
                    results.add(map);
                }
                
                String noteSql = "SELECT notes, category, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings FROM tests WHERE id = ?";
                PreparedStatement ntPstmt = conn.prepareStatement(noteSql);
                ntPstmt.setInt(1, v.getTestId());
                ResultSet ntRs = ntPstmt.executeQuery();
                String notes = "", category = "ACTIVE", spec = "Blood", gStatus = "Positive", gFindings = "";
                int isSp = 0, isMic = 0, isCul = 0;
                if (ntRs.next()) {
                    notes = ntRs.getString("notes");
                    category = ntRs.getString("category");
                    isSp = ntRs.getInt("is_special");
                    isMic = ntRs.getInt("is_microscopic");
                    isCul = ntRs.getInt("is_culture");
                    spec = ntRs.getString("specimen");
                    gStatus = ntRs.getString("growth_status");
                    if (gStatus == null) gStatus = "Positive";
                    gFindings = ntRs.getString("growth_findings");
                }

                allTestData.add(new com.lab.lms.services.ReportGenerator.TestData(v.getTestName(), v.getTestId(), category, results, notes, isSp, isMic, isCul, spec, "", "", new ArrayList<>(), v.getSampleId(), gStatus));
                if (!allTestData.isEmpty()) {
                    allTestData.get(allTestData.size()-1).growthFindings = gFindings;
                }
            }

            // Fetch patient info for report header
            String pAge = "N/A", pGender = "N/A", pRef = "N/A", sDate = selected.get(0).getDate(), phone = "", address = "";
            if (sDate != null && sDate.contains(" ")) sDate = sDate.split(" ")[0];
            PreparedStatement metaPstmt = conn.prepareStatement("SELECT age, age_months, age_days, gender, address, referred_doctor, whatsapp, phone FROM patients WHERE patient_id = ?");
            metaPstmt.setString(1, currentPid);
            ResultSet mRs = metaPstmt.executeQuery();
            if (mRs.next()) {
                int y = mRs.getInt("age");
                int m = mRs.getInt("age_months");
                int d = mRs.getInt("age_days");
                pAge = y + "y";
                if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                pGender = mRs.getString("gender");
                address = mRs.getString("address");
                pRef = mRs.getString("referred_doctor");
                phone = mRs.getString("whatsapp");
                if (phone == null || phone.isEmpty()) phone = mRs.getString("phone");
            }

            // Generate WhatsApp PDF (Always include header)
            String pdfPath = com.lab.lms.services.ReportGenerator.generateMultiTestReport(
                    lblHeaderName.getText(), currentPid, pAge, pGender, pRef, sDate, phone, address, allTestData, true, false); // No watermark for digital

            if (pdfPath != null) {
                com.lab.lms.services.WhatsAppService.sendReportWithRecovery(currentPid, lblHeaderName.getText(), phone, pdfPath);

                String autoOpen = com.lab.lms.dao.DatabaseManager.getSetting("auto_print_whatsapp", "true");
                if (Boolean.parseBoolean(autoOpen)) {
                    java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "WhatsApp Failure: " + e.getMessage()).show();
        }
    }
}

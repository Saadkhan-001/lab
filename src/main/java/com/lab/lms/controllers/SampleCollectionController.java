package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.sql.*;

public class SampleCollectionController {

    @FXML
    private ComboBox<String> recentPatientCombo;
    @FXML
    private Label patientInfoLabel;
    @FXML
    private TableView<com.lab.lms.models.PendingSample> pendingSamplesTable;
    @FXML
    private TableColumn<com.lab.lms.models.PendingSample, Boolean> colSelect;
    @FXML
    private TableColumn<com.lab.lms.models.PendingSample, String> colTests;
    @FXML
    private TableColumn<com.lab.lms.models.PendingSample, String> colSpecimen;
    @FXML
    private TableColumn<com.lab.lms.models.PendingSample, String> colStatus;
    @FXML
    private TableColumn<com.lab.lms.models.PendingSample, String> colActions;

    @FXML
    private TextField barcodeScanField;
    @FXML
    private Label scanStatusLabel;

    private String currentSampleId;
    private ObservableList<com.lab.lms.models.PendingSample> queueList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadRecentPatients();
        pendingSamplesTable.setItems(queueList);
        
        // Auto-focus the scan field for efficiency
        javafx.application.Platform.runLater(() -> barcodeScanField.requestFocus());
        // Guided workflow: Auto-fill if we came from Billing or Registration
        String contextSid = com.lab.lms.services.SessionContext.getCurrentSampleId();
        String contextPid = com.lab.lms.services.SessionContext.getCurrentPatientId();

        if (contextSid != null && !contextSid.isEmpty()) {
            loadSampleData(contextSid);
            autoSelectPatientInCombo(contextSid);
        } else if (contextPid != null && !contextPid.isEmpty()) {
            loadSampleData(contextPid);
            autoSelectPatientInCombo(contextPid);
        }

        // Guided workflow: Auto-fill if we came from Billing or Registration
    }

    private void setupTable() {
        colSelect.setCellValueFactory(data -> data.getValue().selectedProperty());
        colSelect.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        pendingSamplesTable.setEditable(true);

        colTests.setCellValueFactory(data -> data.getValue().testNamesProperty());
        colSpecimen.setCellValueFactory(data -> data.getValue().specimenInfoProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        
        // Apply conditional coloring for statuses
        colStatus.setCellFactory(column -> new TableCell<com.lab.lms.models.PendingSample, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    String color = "#757575"; // Default grey
                    String bg = "#F5F5F5";
                    
                    if (item.contains("PENDING")) { color = "#FF8F00"; bg = "#FFF8E1"; }
                    else if (item.contains("RECEIVED") || item.contains("COLLECTED")) { color = "#2E7D32"; bg = "#E8F5E9"; }
                    
                    badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; " +
                                 "-fx-padding: 2 8; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 3;");
                    setGraphic(badge);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button printBtn = new Button("Print");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, viewBtn, printBtn);

            {
                viewBtn.getStyleClass().add("button-sm-teal");
                printBtn.getStyleClass().add("button-sm-red");
                viewBtn.setOnAction(e -> handleViewSample(getTableRow().getItem()));
                printBtn.setOnAction(e -> handlePrintSingleBarcode(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void handleViewSample(com.lab.lms.models.PendingSample sample) {
        if (sample == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sample Details - " + sample.getSampleId());
        alert.setHeaderText("Medical Protocols for " + sample.getSampleId());
        alert.setContentText("Tests: " + sample.getTestNames() + "\n\nRequirements: " + sample.getSpecimenInfo());
        alert.show();
    }

    private void handlePrintSingleBarcode(com.lab.lms.models.PendingSample sample) {
        if (sample == null) return;
        com.lab.lms.services.BarcodeService.generateAndOpenBarcode(sample.getSampleId());
    }

    private void autoSelectPatientInCombo(String id) {
        if (id == null)
            return;
        for (String item : recentPatientCombo.getItems()) {
            if (item.contains("(" + id + ")")) {
                recentPatientCombo.setValue(item);
                return;
            }
        }
        // If not found by ID (maybe it's a SID), we'd need a DB lookup to find PID
        // But usually we navigate with PID or the SID is recent enough.
    }

    private void loadRecentPatients() {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT patient_id, name FROM patients ORDER BY id DESC LIMIT 20";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            ObservableList<String> patients = FXCollections.observableArrayList();
            while (rs.next()) {
                patients.add(rs.getString("name") + " (" + rs.getString("patient_id") + ")");
            }
            recentPatientCombo.getItems().setAll(patients);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRecentSelect() {
        String selected = recentPatientCombo.getValue();
        if (selected != null && selected.contains("(") && selected.contains(")")) {
            String id = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            loadSampleData(id);
        }
    }

    private void loadSampleData(String query) {
        if (query == null || query.isEmpty())
            return;

        queueList.clear(); // Important: Clear once per full load
        try (Connection conn = DatabaseManager.getConnection()) {
            // Unify search: Find Samples by Patient ID or Specific Sample ID
            String sql = "SELECT s.sample_id, s.status, p.name, p.gender, p.age, p.age_months, p.age_days " +
                    "FROM samples s " +
                    "JOIN patients p ON s.patient_id = p.patient_id " +
                    "WHERE p.patient_id = ? OR s.sample_id = ? " +
                    "ORDER BY CASE WHEN s.status = 'AWAITING COLLECTION' THEN 0 ELSE 1 END, s.collection_date DESC";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                String sid = rs.getString("sample_id");
                
                int y = rs.getInt("age");
                int m = rs.getInt("age_months");
                int d = rs.getInt("age_days");
                String ageStr = y + "y";
                if (m > 0 || d > 0) ageStr += " " + m + "m " + d + "d";
                
                patientInfoLabel.setText("Patient: " + rs.getString("name") + " (" + rs.getString("gender") + ", "
                        + ageStr + ")\n" +
                        "Viewing Specimens for Selection | Active Patient: " + rs.getString("name"));
                patientInfoLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

                // Populate individual row for this specific sample ID
                appendSampleToQueue(sid, rs.getString("status"), conn);
            }

            if (!found) {
                patientInfoLabel.setText("No active specimens found for this patient.");
                patientInfoLabel.setStyle("-fx-text-fill: #961111;");
                pendingSamplesTable.getItems().clear();
            } else {
                currentSampleId = query.startsWith("SAM-") ? query : (queueList.isEmpty() ? null : queueList.get(0).getSampleId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void appendSampleToQueue(String sampleId, String dbStatus, Connection conn) throws SQLException {
        StringBuilder testNames = new StringBuilder();
        StringBuilder specInfo = new StringBuilder();

        String sql = "SELECT DISTINCT t.name, t.container, t.volume " +
                "FROM results r " +
                "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                "JOIN tests t ON tp.test_id = t.id " +
                "WHERE r.sample_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, sampleId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            if (testNames.length() > 0) {
                testNames.append(", ");
                specInfo.append("; ");
            }
            testNames.append(rs.getString("name"));
            String cont = rs.getString("container");
            String vol = rs.getString("volume");
            specInfo.append(cont != null ? cont : "Standard").append(vol != null && !vol.isEmpty() ? " (" + vol + ")" : "");
        }
        
        // Map status for display based on user requirements
        String displayStatus = mapStatus(dbStatus);
        
        if (testNames.length() > 0) {
            queueList.add(new com.lab.lms.models.PendingSample(sampleId, testNames.toString(), specInfo.toString(), displayStatus));
        }
        // queueList auto-reflects in UI since it's an ObservableList set in initialize
    }

    private String mapStatus(String dbStatus) {
        String trackingEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
        boolean isPhaseOn = Boolean.parseBoolean(trackingEnabled);

        if ("AWAITING COLLECTION".equals(dbStatus)) {
            return isPhaseOn ? "SPECIMEN PENDING" : "PENDING";
        } else if ("COLLECTED".equals(dbStatus)) {
            return isPhaseOn ? "SPECIMEN RECEIVED" : "PENDING";
        }
        return dbStatus;
    }

    @FXML
    private void handleBarcodeScan() {
        String bar = barcodeScanField.getText();
        if (bar == null || bar.trim().isEmpty()) return;

        barcodeScanField.clear();
        String sampleId = bar.trim();

        try (Connection conn = DatabaseManager.getConnection()) {
            // Verify sample exists
            PreparedStatement ck = conn.prepareStatement("SELECT status FROM samples WHERE sample_id = ?");
            ck.setString(1, sampleId);
            ResultSet rs = ck.executeQuery();
            
            if (rs.next()) {
                String currentStat = rs.getString("status");
                if ("COLLECTED".equals(currentStat)) {
                    scanStatusLabel.setText("System: Specimen " + sampleId + " already recorded.");
                    scanStatusLabel.setStyle("-fx-text-fill: #FFA000;");
                } else {
                    // Update to COLLECTED
                    PreparedStatement up = conn.prepareStatement("UPDATE samples SET status = 'COLLECTED', collection_date = CURRENT_TIMESTAMP WHERE sample_id = ?");
                    up.setString(1, sampleId);
                    up.executeUpdate();
                    
                    scanStatusLabel.setText("System: " + sampleId + " -> SPECIMEN RECEIVED");
                    scanStatusLabel.setStyle("-fx-text-fill: #2E7D32;");

                    // Refresh table if it contains this sample (or re-load for current patient)
                    loadSampleData(sampleId); 
                }
            } else {
                scanStatusLabel.setText("Error: Barcode " + sampleId + " not found in database.");
                scanStatusLabel.setStyle("-fx-text-fill: #D32F2F;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            scanStatusLabel.setText("Critical Database Link Error.");
        }
    }
    @FXML
    private void handleCollect() {
        if (currentSampleId == null)
            return;

        java.util.List<String> selectedIds = new java.util.ArrayList<>();
        for (com.lab.lms.models.PendingSample sample : queueList) {
            if (sample.isSelected()) {
                selectedIds.add(sample.getSampleId());
            }
        }
        
        if (selectedIds.isEmpty()) {
            selectedIds.add(currentSampleId);
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "UPDATE samples SET status = 'COLLECTED', collection_date = CURRENT_TIMESTAMP WHERE sample_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            int totalUpdated = 0;
            for (String sid : selectedIds) {
                pstmt.setString(1, sid);
                totalUpdated += pstmt.executeUpdate();
            }

            if (totalUpdated > 0) {
                // Generate and open the Barcode Label Preview (PDF)
                com.lab.lms.services.BarcodeService.generateAndOpenBarcodes(selectedIds);
                
                // Track for automatic loading in Processing module
                com.lab.lms.services.SessionContext.setCurrentSampleId(selectedIds.get(0));
                
                loadSampleData(selectedIds.get(0)); // Refresh UI
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNext() {
        if (currentSampleId == null) return;

        // Smart Selection Logic: Handle Collect Now vs Collect Later
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Collection Flow Decision");
        choice.setHeaderText("Patient Readiness Check: " + currentSampleId);
        choice.setContentText("Has the specimen been physically collected from the subject?");

        ButtonType btnNow = new ButtonType("COLLECT NOW");
        ButtonType btnLater = new ButtonType("COLLECT LATER");
        ButtonType btnCancel = new ButtonType("STAY HERE", ButtonBar.ButtonData.CANCEL_CLOSE);

        choice.getButtonTypes().setAll(btnNow, btnLater, btnCancel);

        choice.showAndWait().ifPresent(response -> {
            if (response == btnNow) {
                handleCollect(); // Marks as collected
                com.lab.lms.services.NavigationService.switchView("/fxml/processing.fxml");
            } else if (response == btnLater) {
                // Keep status as AWAITING COLLECTION (usually it already is)
                // Just move to the next logical step if needed, or stay
                com.lab.lms.services.NavigationService.switchView("/fxml/processing.fxml");
            }
        });
    }

    @FXML
    private void handlePrintSelected() {
        java.util.List<String> selectedIds = new java.util.ArrayList<>();
        for (com.lab.lms.models.PendingSample sample : queueList) {
            if (sample.isSelected()) {
                selectedIds.add(sample.getSampleId());
            }
        }
        
        if (selectedIds.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select at least one specimen from the queue to print.");
            alert.show();
        } else {
            com.lab.lms.services.BarcodeService.generateAndOpenBarcodes(selectedIds);
        }
    }
}

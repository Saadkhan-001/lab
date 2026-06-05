package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.PendingApproval;
import com.lab.lms.models.TestParameter;
import com.lab.lms.services.ReportGenerator;
import com.lab.lms.services.WhatsAppService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorReviewController {

    @FXML
    private TableView<PendingApproval> pendingTable;
    @FXML
    private ComboBox<String> recentPatientCombo;
    @FXML
    private TableColumn<PendingApproval, String> colPatient;
    @FXML
    private TableColumn<PendingApproval, String> colTest;

    @FXML
    private TableView<TestParameter> resultsPreviewTable;
    @FXML
    private TableColumn<TestParameter, String> colParamName;
    @FXML
    private TableColumn<TestParameter, String> colParamValue;
    @FXML
    private TableColumn<TestParameter, String> colParamUnit;
    @FXML
    private TableColumn<TestParameter, String> colParamRange;
    @FXML
    private TextArea testCommentsArea;
    @FXML
    private VBox commentBox;
    @FXML
    private Button btnAddComment;

    private ObservableList<PendingApproval> pendingList = FXCollections.observableArrayList();
    private ObservableList<TestParameter> currentResults = FXCollections.observableArrayList();
    private PendingApproval selectedApproval;
    private String currentGender;
    private String currentAgeStr;

    @FXML
    public void initialize() {
        loadRecentPatients();
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colTest.setCellValueFactory(new PropertyValueFactory<>("testName"));
        pendingTable.setItems(pendingList);

        colParamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colParamValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colParamUnit.setCellValueFactory(cell -> {
            if (selectedApproval != null) {
                // We need to check if the current test is microscopic. 
                // Since this is a cell factory, we'll use the flag set in loadResults or a custom approach.
                // For simplicity, let's look at the column header text which is already set in loadResults.
                if ("CATEGORY".equals(colParamUnit.getText())) {
                    return cell.getValue().categoryProperty();
                }
            }
            return cell.getValue().unitProperty();
        });
        colParamRange.setCellValueFactory(cell -> {
            return new javafx.beans.property.SimpleStringProperty(cell.getValue().getRange(currentGender, currentAgeStr));
        });

        colParamValue.setCellFactory(column -> new TextFieldTableCell<TestParameter, String>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                if (textField != null) {
                    textField.selectAll();
                    textField.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER || 
                            event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                            
                            commitEdit(textField.getText());
                            event.consume();

                            int currentRow = getTableRow().getIndex();
                            int nextRow = currentRow;
                            
                            if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
                                nextRow = currentRow + 1;
                            } else if (event.getCode() == KeyCode.UP) {
                                nextRow = currentRow - 1;
                            }

                            int target;
                            if (nextRow >= 0 && nextRow < getTableView().getItems().size()) {
                                target = nextRow;
                            } else {
                                target = currentRow;
                            }

                            javafx.application.Platform.runLater(() -> {
                                getTableView().getSelectionModel().select(target);
                                getTableView().getFocusModel().focus(target, colParamValue);
                                getTableView().scrollTo(Math.max(0, target - 4)); 
                                getTableView().edit(target, colParamValue);
                            });
                        }
                    });
                }
            }
        });
        colParamValue.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setValue(event.getNewValue());
        });

        resultsPreviewTable.setItems(currentResults);

        loadPending();

        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedApproval = newVal;
                loadResults(newVal.getSampleId(), newVal.getTestId());
            }
        });

        // Guided workflow: Auto-select if we came from Lab Processing
        String contextSid = com.lab.lms.services.SessionContext.getCurrentSampleId();
        String contextPid = com.lab.lms.services.SessionContext.getCurrentPatientId();

        if (contextSid != null && !contextSid.isEmpty()) {
            for (PendingApproval pa : pendingList) {
                if (pa.getSampleId().equals(contextSid)) {
                    pendingTable.getSelectionModel().select(pa);
                    break;
                }
            }
            autoSelectPatientInCombo(contextSid);
        } else if (contextPid != null && !contextPid.isEmpty()) {
            autoSelectPatientInCombo(contextPid);
        }
    }

    private void autoSelectPatientInCombo(String id) {
        if (id == null)
            return;
        for (String item : recentPatientCombo.getItems()) {
            if (item.contains("(" + id + ")")) {
                recentPatientCombo.setValue(item);
                break;
            }
        }
    }

    private void loadRecentPatients() {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT patient_id, name FROM patients ORDER BY id DESC LIMIT 20";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            ObservableList<String> patients = FXCollections.observableArrayList();
            while (rs.next()) {
                patients.add(rs.getString("name") + " (" + rs.getString("patient_id") + ")");
            }
            recentPatientCombo.setItems(patients);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRecentSelect() {
        String selected = recentPatientCombo.getValue();
        if (selected != null && selected.contains("(") && selected.contains(")")) {
            String id = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            // Find in pending list
            for (PendingApproval pa : pendingList) {
                if (pa.getPatientId().equals(id)) {
                    pendingTable.getSelectionModel().select(pa);
                    break;
                }
            }
        }
    }

    private void loadPending() {
        pendingList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            // Group by Sample and Test to show one entry for multiple parameters
            String sql = "SELECT DISTINCT s.sample_id, p.patient_id, p.name as patient_name, t.id as test_id, t.name as test_name "
                    +
                    "FROM results r " +
                    "JOIN samples s ON r.sample_id = s.sample_id " +
                    "JOIN patients p ON s.patient_id = p.patient_id " +
                    "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                    "JOIN tests t ON tp.test_id = t.id " +
                    "WHERE r.doctor_approval = 0";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                pendingList.add(new PendingApproval(
                        rs.getString("sample_id"),
                        rs.getString("patient_id"),
                        rs.getString("patient_name"),
                        rs.getInt("test_id"),
                        rs.getString("test_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadResults(String sampleId, int testId) {
        currentResults.clear();
        testCommentsArea.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean isMic = false;
            PreparedStatement mPstmt = conn.prepareStatement("SELECT is_microscopic FROM tests WHERE id = ?");
            mPstmt.setInt(1, testId);
            ResultSet mRs = mPstmt.executeQuery();
            if (mRs.next()) isMic = mRs.getInt("is_microscopic") == 1;

            // Fetch demographics for current context
            PreparedStatement pPstmt = conn.prepareStatement("SELECT gender, age, age_months, age_days FROM patients WHERE patient_id = (SELECT patient_id FROM samples WHERE sample_id = ?)");
            pPstmt.setString(1, sampleId);
            ResultSet pRs = pPstmt.executeQuery();
            if (pRs.next()) {
                currentGender = pRs.getString("gender");
                int y = pRs.getInt("age");
                int m = pRs.getInt("age_months");
                int d = pRs.getInt("age_days");
                currentAgeStr = y + "y";
                if (m > 0 || d > 0) currentAgeStr += " " + m + "m " + d + "d";
            } else {
                currentGender = "N/A";
                currentAgeStr = "N/A";
            }

            if (isMic) {
                colParamUnit.setText("CATEGORY");
                colParamRange.setVisible(false);
            } else {
                colParamUnit.setText("UNIT");
                colParamRange.setVisible(true);
            }

            String sql = "SELECT tp.*, r.value, r.comment " +
                    "FROM results r " +
                    "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                    "WHERE r.sample_id = ? AND tp.test_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sampleId);
            pstmt.setInt(2, testId);
            ResultSet rs = pstmt.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    String comment = rs.getString("comment");
                    if (comment != null) testCommentsArea.setText(comment);
                    first = false;
                }
                TestParameter tp = new TestParameter(
                        rs.getInt("id"),
                        testId,
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getString("min_range"),
                        rs.getString("max_range"),
                        rs.getString("min_range_male"),
                        rs.getString("max_range_male"),
                        rs.getString("min_range_female"),
                        rs.getString("max_range_female"),
                        rs.getString("min_range_kids"),
                        rs.getString("max_range_kids")
                );
                tp.setCategory(rs.getString("category"));
                tp.setValue(rs.getString("value"));
                currentResults.add(tp);
            }

            // Sync UI
            boolean hasComment = testCommentsArea.getText() != null && !testCommentsArea.getText().trim().isEmpty();
            commentBox.setVisible(hasComment);
            commentBox.setManaged(hasComment);
            btnAddComment.setVisible(!hasComment);
            btnAddComment.setManaged(!hasComment);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleApprove() {
        if (selectedApproval == null || currentResults.isEmpty())
            return;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Update values and mark as approved
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE results SET value = ?, doctor_approval = 1, comment = ? WHERE sample_id = ? AND parameter_id = ?");

                // Fetch Detailed Metadata for Report FIRST to use in range calculation
                String pAge = "N/A", pGender = "N/A", pRef = "N/A", sDate = "N/A", phone = "", address = "";
                PreparedStatement metaPstmt = conn.prepareStatement(
                        "SELECT p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, p.whatsapp, p.phone, s.collection_date "
                                +
                                "FROM patients p JOIN samples s ON p.patient_id = s.patient_id " +
                                "WHERE s.sample_id = ?");
                metaPstmt.setString(1, selectedApproval.getSampleId());
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
                    sDate = mRs.getString("collection_date");
                    phone = mRs.getString("whatsapp");
                    if (phone == null || phone.isEmpty())
                        phone = mRs.getString("phone");
                }

                String currentComment = testCommentsArea.getText();
                List<Map<String, String>> reportData = new ArrayList<>();
                for (TestParameter tp : currentResults) {
                    pstmt.setString(1, tp.getValue());
                    pstmt.setString(2, currentComment);
                    pstmt.setString(3, selectedApproval.getSampleId());
                    pstmt.setInt(4, tp.getId());
                    pstmt.addBatch();

                    Map<String, String> map = new HashMap<>();
                    map.put("name", tp.getName());
                    map.put("value", tp.getValue());
                    map.put("unit", tp.getUnit());
                    map.put("category", tp.getCategory());
                    map.put("range", tp.getRange(pGender, pAge));
                    map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                    reportData.add(map);
                }
                pstmt.executeBatch();

                // 3. Generate PDF Report with complete metadata
                boolean includeHeader = Boolean.parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("print_header_footer", "true"));
                String pdfPath = ReportGenerator.generateReport(
                        selectedApproval.getPatientName(),
                        selectedApproval.getPatientId(),
                        pAge, pGender, pRef, sDate,
                        selectedApproval.getTestName(),
                        phone, address,
                        reportData, includeHeader, true, currentComment, selectedApproval.getSampleId(), null);

                // 4. Update results with PDF path
                if (pdfPath != null) {
                    PreparedStatement pdfPstmt = conn.prepareStatement(
                            "UPDATE results SET pdf_path = ? WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?)");
                    pdfPstmt.setString(1, pdfPath);
                    pdfPstmt.setString(2, selectedApproval.getSampleId());
                    pdfPstmt.setInt(3, selectedApproval.getTestId());
                    pdfPstmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(true);

                if (pdfPath != null) {
                    com.lab.lms.models.Test t = new com.lab.lms.models.Test(
                            selectedApproval.getTestId(),
                            null, // numeric_code (unknown here)
                            null, // alpha_code (unknown here)
                            selectedApproval.getTestName(),
                            "Clinical",
                            0.0,
                            "N/A",
                            null,
                            0,
                            0,
                            0,
                            "Blood",
                            null);
                    showApprovalSuccessActions(pdfPath, phone, t);
                } else {
                    new Alert(Alert.AlertType.INFORMATION, "Report Approved successfully.").show();
                }

                loadPending();
                clearPreview();
                handleNext(); // Guided workflow: Auto-proceed

            } catch (Exception e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error approving results: " + e.getMessage()).show();
        }
    }

    private void showApprovalSuccessActions(String pdfPath, String phone, com.lab.lms.models.Test selectedTest) {
        Alert actions = new Alert(Alert.AlertType.CONFIRMATION);
        actions.setTitle("Medical Review Complete");
        actions.setHeaderText("Report Approved & Finalized");
        actions.setContentText("The clinical report is ready for distribution. Choose action:");

        ButtonType btnView = new ButtonType("VIEW REPORT");
        ButtonType btnPrint = new ButtonType("PRINT");
        ButtonType btnWhatsApp = new ButtonType("SEND WHATSAPP");
        ButtonType btnDone = new ButtonType("DONE", ButtonBar.ButtonData.CANCEL_CLOSE);

        actions.getButtonTypes().setAll(btnView, btnPrint, btnWhatsApp, btnDone);

        actions.showAndWait().ifPresent(result -> {
            if (result == btnView || result == btnPrint) {
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
                    showApprovalSuccessActions(pdfPath, phone, selectedTest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (result == btnWhatsApp) {
                if (phone != null && !phone.isEmpty()) {
                    // RE-GENERATE specifically for WhatsApp WITH header=true
                    try (Connection conn = DatabaseManager.getConnection()) {
                         // Fetch detailed metadata again for re-generation
                         String pAge = "N/A", pGender = "N/A", pRef = "N/A", sDate = "N/A", rAddress = "";
                         PreparedStatement metaP = conn.prepareStatement(
                                 "SELECT p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, s.collection_date "
                                         + "FROM patients p JOIN samples s ON p.patient_id = s.patient_id "
                                         + "WHERE s.sample_id = ?");
                         metaP.setString(1, selectedApproval.getSampleId());
                         ResultSet rsM = metaP.executeQuery();
                         if (rsM.next()) {
                             int y = rsM.getInt("age");
                             int m = rsM.getInt("age_months");
                             int d = rsM.getInt("age_days");
                             pAge = y + "y";
                             if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                             pGender = rsM.getString("gender");
                             rAddress = rsM.getString("address");
                             pRef = rsM.getString("referred_doctor");
                             sDate = rsM.getString("collection_date");
                         }

                         List<Map<String, String>> dataForPdf = new ArrayList<>();
                         for (TestParameter tp : currentResults) {
                             Map<String, String> m = new HashMap<>();
                             m.put("name", tp.getName());
                             m.put("value", tp.getValue());
                             m.put("unit", tp.getUnit());
                             m.put("category", tp.getCategory());
                             m.put("range", tp.getRange(pGender, pAge));
                             m.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                             dataForPdf.add(m);
                         }

                         String wPdf = ReportGenerator.generateReport(
                                 selectedApproval.getPatientName(),
                                 selectedApproval.getPatientId(), pAge, pGender, pRef, sDate,
                                 selectedApproval.getTestName(), phone, rAddress, dataForPdf, true, false, testCommentsArea.getText(), null); // No watermark for WhatsApp

                         if (wPdf != null) {
                             WhatsAppService.sendReportWithRecovery(selectedApproval.getPatientId(), selectedApproval.getPatientName(), phone, wPdf);
                         }
                    } catch (Exception e) {
                        e.printStackTrace();
                        WhatsAppService.sendReportWithRecovery(selectedApproval.getPatientId(), selectedApproval.getPatientName(), phone, pdfPath);
                    }
                }
                showApprovalSuccessActions(pdfPath, phone, selectedTest);
            }
        });
    }

    private void clearPreview() {
        selectedApproval = null;
        currentResults.clear();
    }

    @FXML
    private void handleNext() {
        com.lab.lms.services.NavigationService.switchView("/fxml/history.fxml");
    }

    @FXML
    private void handleAddComment() {
        commentBox.setVisible(true);
        commentBox.setManaged(true);
        btnAddComment.setVisible(false);
        btnAddComment.setManaged(false);
        testCommentsArea.requestFocus();
    }
}

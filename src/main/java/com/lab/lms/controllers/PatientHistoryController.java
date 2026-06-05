package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Patient;
import com.lab.lms.models.VisitHistory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import java.awt.Desktop;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.lab.lms.services.ReportGenerator;

public class PatientHistoryController {

    @FXML
    private TextField patientSearchField;
    @FXML
    private ComboBox<Patient> patientSelectCombo;
    @FXML
    private Label selectedPatientLabel;

    @FXML
    private TableView<VisitHistory> historyTable;
    @FXML
    private TableColumn<VisitHistory, String> colDate;
    @FXML
    private TableColumn<VisitHistory, String> colSampleId;
    @FXML
    private TableColumn<VisitHistory, String> colTestName;
    @FXML
    private TableColumn<VisitHistory, String> colStatus;
    @FXML
    private TableColumn<VisitHistory, Void> colAction;
    @FXML
    private TableColumn<VisitHistory, Boolean> colSelect;
    @FXML
    private Button btnPrintSelected;
    @FXML
    private Button btnWhatsAppSelected;

    // History filter controls
    @FXML
    private DatePicker histFilterFromDate;
    @FXML
    private DatePicker histFilterToDate;
    @FXML
    private ComboBox<String> histFilterTest;
    @FXML
    private ComboBox<String> histFilterStatus;

    private ObservableList<Patient> allPatients = FXCollections.observableArrayList();
    private ObservableList<VisitHistory> historyData = FXCollections.observableArrayList();
    private FilteredList<VisitHistory> filteredHistory;

    @FXML
    public void initialize() {
        // Setup Patients Selection
        loadPatients();
        FilteredList<Patient> filteredPatients = new FilteredList<>(allPatients, p -> true);

        // High-Fidelity Debounced Search for History Records
        // High-Performance Asynchronous Search for History Records
        javafx.animation.PauseTransition historySearchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
        
        patientSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            historySearchDebounce.setOnFinished(e -> {
                String query = (newVal == null) ? "" : newVal.trim().toLowerCase();
                
                // High-Performance Stream Filtering with strict LIMIT to prevent UI lag
                java.util.List<Patient> matches = allPatients.parallelStream()
                    .filter(p -> query.isEmpty() || p.getLowercaseName().contains(query) || p.getLowercaseId().contains(query))
                    .limit(15)
                    .collect(java.util.stream.Collectors.toList());

                patientSelectCombo.setItems(javafx.collections.FXCollections.observableArrayList(matches));
                
                if (!matches.isEmpty() && !query.isEmpty()) {
                    patientSelectCombo.show();
                }
            });
            historySearchDebounce.playFromStart();
        });

        patientSelectCombo.setItems(filteredPatients);
        patientSelectCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getPatientId() + ")");
                }
            }
        });

        // Button-like behavior for selection
        patientSelectCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                patientSearchField.setText(newVal.getName());
                selectedPatientLabel.setText("Showing Records for: " + newVal.getName());
                loadHistory(newVal.getPatientId());
            }
        });

        // Setup Table Selection
        historyTable.setEditable(true);
        colSelect.setEditable(true);
        
        colSelect.setCellFactory(column -> {
            javafx.scene.control.cell.CheckBoxTableCell<VisitHistory, Boolean> cell = new javafx.scene.control.cell.CheckBoxTableCell<>();
            cell.setEditable(true);
            return cell;
        });
        
        colSelect.setCellValueFactory(cellData -> {
            VisitHistory v = cellData.getValue();
            javafx.beans.property.BooleanProperty prop = v.selectedProperty();
            prop.addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    // Ensure same patient selection
                    String firstId = null;
                    for (VisitHistory item : historyData) {
                        if (item.isSelected() && !item.isDetailRow() && item != v) {
                            firstId = item.getPatientId();
                            break;
                        }
                    }
                    if (firstId != null && !firstId.equals(v.getPatientId())) {
                        javafx.application.Platform.runLater(() -> {
                            new Alert(Alert.AlertType.WARNING, "Selection restricted to tests of the same subject only.")
                                    .show();
                            prop.set(false);
                            v.setSelected(false); // Double ensure
                        });
                        return;
                    }
                }
                updatePrintButtonVisibility();
            });
            return prop;
        });

        // Setup History Table
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSampleId.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("testName"));
        colTestName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    VisitHistory v = getTableView().getItems().get(idx);
                    setText(v.getTestName());
                    setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-style: normal;");
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    VisitHistory v = getTableView().getItems().get(idx);
                    String displayStatus = "PENDING";
                    String bgColor = "#ECEFF1"; 
                    String textColor = "#455A64"; 

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
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        
        // Setup filtered history list
        filteredHistory = new FilteredList<>(historyData, h -> true);

        setupActionColumn();
        historyTable.setItems(filteredHistory);

        // Populate status filter
        histFilterStatus.getItems().addAll("All Status", "COMPLETED", "IN PROCESSING", "READY FOR REVIEW", "WAITING FOR SAMPLE", "PENDING");
        histFilterStatus.setValue("All Status");

        // Load all records by default
        loadHistory(null);

        // Date and ID blanking for detail rows
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    VisitHistory v = getTableView().getItems().get(getIndex());
                    setText(v.getDate());
                    setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-style: normal;");
                }
            }
        });
        colSampleId.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    VisitHistory v = getTableView().getItems().get(idx);
                    setText(v.getPatientName());
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-font-size: 11px;");
                }
            }
        });
    }

    private void loadPatients() {
        allPatients.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM patients ORDER BY name");
            while (rs.next()) {
                allPatients.add(new Patient(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getInt("age_months"),
                        rs.getInt("age_days"),
                        rs.getString("gender"),
                        rs.getString("phone"),
                        rs.getString("whatsapp"),
                        rs.getString("address"),
                        rs.getString("referred_doctor"),
                        rs.getString("registration_date")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadHistory(String pid) {
        historyData.clear();
        histFilterTest.getItems().clear();
        histFilterTest.getItems().add("All Tests");
        histFilterTest.setValue("All Tests");

        try (Connection conn = DatabaseManager.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT s.collection_date, s.sample_id, t.name as test_name, r.status, r.pdf_path, t.id as test_id, p.name as p_name, p.patient_id, "
                            + "COALESCE(p.whatsapp, p.phone) as contact_phone, s.status as sample_status, MIN(r.doctor_approval) as approval, MAX(r.comment) as result_comment "
                            + "FROM samples s " +
                            "JOIN patients p ON s.patient_id = p.patient_id " +
                            "JOIN results r ON s.sample_id = r.sample_id " +
                            "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                            "JOIN tests t ON tp.test_id = t.id ");

            if (pid != null && !pid.isEmpty()) {
                sql.append("WHERE s.patient_id = ? ");
            }

            sql.append("GROUP BY s.sample_id, t.id ORDER BY s.collection_date DESC");

            PreparedStatement hStmt = conn.prepareStatement(sql.toString());
            if (pid != null && !pid.isEmpty()) {
                hStmt.setString(1, pid);
            }
            ResultSet hRs = hStmt.executeQuery();
            while (hRs.next()) {
                VisitHistory v = new VisitHistory(
                        hRs.getString("collection_date"),
                        hRs.getString("sample_id"),
                        hRs.getString("test_name"),
                        hRs.getString("status"),
                        hRs.getString("pdf_path"),
                        hRs.getInt("test_id"),
                        hRs.getString("p_name"),
                        hRs.getString("contact_phone"),
                        hRs.getString("sample_status"),
                        hRs.getInt("approval"),
                        hRs.getString("patient_id"));
                v.setResultComment(hRs.getString("result_comment"));
                historyData.add(v);
                // Populate test name dropdown
                String tName = hRs.getString("test_name");
                if (!histFilterTest.getItems().contains(tName)) {
                    histFilterTest.getItems().add(tName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Reset filter
        histFilterFromDate.setValue(null);
        histFilterToDate.setValue(null);
        applyHistoryFilter();
    }


    private void applyHistoryFilter() {
        LocalDate from = histFilterFromDate.getValue();
        LocalDate to = histFilterToDate.getValue();
        String selectedTest = histFilterTest.getValue();
        String selectedStatus = histFilterStatus.getValue();

        filteredHistory.setPredicate(visit -> {

            // Test filter
            boolean matchesTest = (selectedTest == null || selectedTest.isEmpty() || selectedTest.equals("All Tests"))
                    || visit.getTestName().equals(selectedTest);

            // Date filter
            boolean matchesDate = true;
            String dateStr = visit.getDate();
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    LocalDate vDate = LocalDate.parse(dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr);
                    if (from != null && vDate.isBefore(from))
                        matchesDate = false;
                    if (to != null && vDate.isAfter(to))
                        matchesDate = false;
                } catch (Exception e) {
                    // ignore
                }
            }

            // Status filter
            boolean matchesStatus = true;
            if (selectedStatus != null && !selectedStatus.isEmpty() && !selectedStatus.equals("All Status")) {
                String displayStatus = "PENDING";
                if ("COLLECTED".equalsIgnoreCase(visit.getSampleStatus())) {
                    if ("COMPLETED".equalsIgnoreCase(visit.getStatus())) {
                        if (visit.getApprovalStatus() == 1) {
                            displayStatus = "COMPLETED";
                        } else {
                            displayStatus = "READY FOR REVIEW";
                        }
                    } else {
                        displayStatus = "IN PROCESSING";
                    }
                } else if ("AWAITING COLLECTION".equalsIgnoreCase(visit.getSampleStatus())) {
                    displayStatus = "WAITING FOR SAMPLE";
                }
                matchesStatus = displayStatus.equalsIgnoreCase(selectedStatus);
            }

            return matchesTest && matchesDate && matchesStatus;
        });
    }

    @FXML
    private void handleHistoryFilter() {
        collapseAll();
        applyHistoryFilter();
    }

    @FXML
    private void handleHistoryClearFilter() {
        collapseAll();
        histFilterFromDate.setValue(null);
        histFilterToDate.setValue(null);
        histFilterTest.setValue("All Tests");
        histFilterStatus.setValue("All Status");
        applyHistoryFilter();
    }

    private void collapseAll() {
        // No-op: Expansion removed
    }

    private void setupActionColumn() {
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<VisitHistory, Void> call(TableColumn<VisitHistory, Void> param) {
                return new TableCell<>() {
                    private final Button viewBtn = new Button("VIEW");
                    private final Button whatsappBtn = new Button("WHATSAPP");
                    private final Button enterResultBtn = new Button("ENTER RESULT");
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, enterResultBtn, viewBtn,
                            whatsappBtn);
                    {
                        viewBtn.setStyle(
                                "-fx-background-color: #455A64; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-cursor: hand;");
                        whatsappBtn.setStyle(
                                "-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-cursor: hand;");
                        
                        enterResultBtn.setStyle(
                                "-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-cursor: hand;");
                        
                        container.setAlignment(javafx.geometry.Pos.CENTER);

                        viewBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            openReport(history.getPdfPath());
                        });

                        enterResultBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            com.lab.lms.services.SessionContext.setCurrentPatientId(history.getPatientId());
                            com.lab.lms.services.SessionContext.setCurrentSampleId(history.getSampleId());
                            com.lab.lms.services.SessionContext.setCurrentTestId(history.getTestId());
                            com.lab.lms.services.NavigationService.switchView("/fxml/processing.fxml");
                        });

                        whatsappBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            // Re-generate for WhatsApp WITH header=true
                            try (Connection conn = DatabaseManager.getConnection()) {
                                // Fetch patient info and report data
                                String pName = "", pId = "", pAge = "", pGender = "", pRef = "", sDate = "", pAddress = "";
                                PreparedStatement pStmt = conn.prepareStatement(
                                    "SELECT p.patient_id, p.name, p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, s.collection_date " +
                                    "FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE s.sample_id = ?");
                                pStmt.setString(1, history.getSampleId());
                                ResultSet rsInf = pStmt.executeQuery();
                                if (rsInf.next()) {
                                    pId = rsInf.getString("patient_id");
                                    pName = rsInf.getString("name");
                                    
                                    int y = rsInf.getInt("age");
                                    int m = rsInf.getInt("age_months");
                                    int d = rsInf.getInt("age_days");
                                    pAge = y + "y";
                                    if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                                    
                                    pGender = rsInf.getString("gender");
                                    pAddress = rsInf.getString("address");
                                    pRef = rsInf.getString("referred_doctor");
                                    sDate = rsInf.getString("collection_date");
                                }

                                String commentFound = "";
                                PreparedStatement commPstmt = conn.prepareStatement("SELECT comment FROM results WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?) LIMIT 1");
                                commPstmt.setString(1, history.getSampleId());
                                commPstmt.setInt(2, history.getTestId());
                                ResultSet rsComm = commPstmt.executeQuery();
                                if (rsComm.next()) {
                                    String c = rsComm.getString("comment");
                                    if (c != null) commentFound = c;
                                }

                                java.util.List<java.util.Map<String, String>> reportData = new java.util.ArrayList<>();
                                PreparedStatement resPstmt = conn.prepareStatement(
                                    "SELECT * " +
                                    "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                    "WHERE r.sample_id = ? AND tp.test_id = ?");
                                resPstmt.setString(1, history.getSampleId());
                                resPstmt.setInt(2, history.getTestId());
                                ResultSet rsRes = resPstmt.executeQuery();
                                while (rsRes.next()) {
                                    com.lab.lms.models.TestParameter tp = new com.lab.lms.models.TestParameter(
                                        0, 0,
                                        rsRes.getString("name"),
                                        rsRes.getString("unit"),
                                        rsRes.getString("min_range"),
                                        rsRes.getString("max_range"),
                                        rsRes.getString("min_range_male"),
                                        rsRes.getString("max_range_male"),
                                        rsRes.getString("min_range_female"),
                                        rsRes.getString("max_range_female"),
                                        rsRes.getString("min_range_kids"),
                                        rsRes.getString("max_range_kids")
                                    );
                                    tp.setValue(rsRes.getString("value") == null ? "" : rsRes.getString("value"));
                                    
                                    java.util.Map<String, String> map = new java.util.HashMap<>();
                                    map.put("name", tp.getName());
                                    map.put("value", tp.getValue());
                                    map.put("unit", tp.getUnit());
                                    map.put("range", tp.getRange(pGender, pAge));
                                    map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                                    reportData.add(map);
                                }

                                // Forced header generation
                                String wPdf = com.lab.lms.services.ReportGenerator.generateReport(pName, pId, pAge, pGender, pRef, sDate, history.getTestName(), history.getPhone(), pAddress, reportData, true, false, commentFound, null); // No watermark for digital
                                if (wPdf != null) {
                                    com.lab.lms.services.WhatsAppService.sendReportWithRecovery(history.getPatientId(), history.getPatientName(),
                                            history.getPhone(), wPdf);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Fallback to existing path if re-generation fails
                                com.lab.lms.services.WhatsAppService.sendReportWithRecovery(history.getPatientId(), history.getPatientName(),
                                        history.getPhone(), history.getPdfPath());
                            }
                        });

                        viewBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            openReport(history.getPdfPath());
                        });

                        whatsappBtn.setOnAction(event -> {
                            VisitHistory history = getTableView().getItems().get(getIndex());
                            handleWhatsApp(history);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            VisitHistory row = getTableView().getItems().get(getIndex());
                            boolean isCompleted = "COMPLETED".equalsIgnoreCase(row.getStatus());
                            
                            enterResultBtn.setVisible(!isCompleted);
                            enterResultBtn.setManaged(!isCompleted);
                            
                            viewBtn.setVisible(isCompleted);
                            viewBtn.setManaged(isCompleted);
                            
                            whatsappBtn.setVisible(isCompleted);
                            whatsappBtn.setManaged(isCompleted);
                            
                            if (isCompleted) {
                                boolean hasPdf = row.getPdfPath() != null && !row.getPdfPath().isEmpty();
                                viewBtn.setDisable(!hasPdf);
                                whatsappBtn.setDisable(!hasPdf);
                            }
                            setGraphic(container);
                        }
                    }
                };
            }
        });
    }

    @FXML
    private void handleNewRegistration() {
        com.lab.lms.services.SessionContext.setCurrentPatientId(null);
        com.lab.lms.services.SessionContext.setCurrentSampleId(null);
        com.lab.lms.services.NavigationService.switchView("/fxml/registration.fxml");
    }

    private void handleWhatsApp(VisitHistory history) {
        if (history.getPdfPath() == null || history.getPdfPath().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No clinical report (PDF) found for this test.").show();
            return;
        }
        com.lab.lms.services.WhatsAppService.sendReportWithRecovery(history.getPatientId(), history.getPatientName(),
                history.getPhone(), history.getPdfPath());
    }

    @FXML
    private void handleWhatsAppSelectedTests() {
        java.util.List<VisitHistory> selected = new java.util.ArrayList<>();
        for (VisitHistory v : historyData) {
            if (v.isSelected() && !v.isDetailRow())
                selected.add(v);
        }

        if (selected.isEmpty()) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Fetch Patient Info
            String pid = selected.get(0).getPatientId();
            String pName = "", pId = "", pAge = "", pGender = "", pRef = "", rAddress = "", pPhone = "";
            PreparedStatement pStmt = conn.prepareStatement("SELECT * FROM patients WHERE patient_id = ?");
            pStmt.setString(1, pid);
            ResultSet rsInf = pStmt.executeQuery();
            if (rsInf.next()) {
                pId = rsInf.getString("patient_id");
                pName = rsInf.getString("name");
                
                int y = rsInf.getInt("age");
                int m = rsInf.getInt("age_months");
                int d = rsInf.getInt("age_days");
                pAge = y + "y";
                if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                
                pGender = rsInf.getString("gender");
                rAddress = rsInf.getString("address");
                pRef = rsInf.getString("referred_doctor");
                pPhone = rsInf.getString("whatsapp");
                if (pPhone == null || pPhone.isEmpty()) pPhone = rsInf.getString("phone");
            }

            // 2. Aggregate all test data
            List<ReportGenerator.TestData> allTestData = new ArrayList<>();
            for (VisitHistory v : selected) {
                String notes = "", category = "ACTIVE", spec = "Blood", gStatus = "Positive", gFindings = "";
                int isSp = 0, isMic = 0, isCul = 0;
                PreparedStatement metaStmt = conn.prepareStatement("SELECT notes, category, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings FROM tests WHERE id = ?");
                metaStmt.setInt(1, v.getTestId());
                ResultSet rsMeta = metaStmt.executeQuery();
                if (rsMeta.next()) {
                    notes = rsMeta.getString("notes");
                    category = rsMeta.getString("category");
                    isSp = rsMeta.getInt("is_special");
                    isMic = rsMeta.getInt("is_microscopic");
                    isCul = rsMeta.getInt("is_culture");
                    spec = rsMeta.getString("specimen");
                    gStatus = rsMeta.getString("growth_status");
                    if (gStatus == null) gStatus = "Positive";
                    gFindings = rsMeta.getString("growth_findings");
                }

                List<Map<String, String>> reportData = new ArrayList<>();
                String resSql = "SELECT * " +
                                "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                "WHERE r.sample_id = ? AND tp.test_id = ? " +
                                "ORDER BY CASE WHEN tp.print_order = 0 THEN 999999 ELSE tp.print_order END ASC, tp.id ASC";
                PreparedStatement rsPstmt = conn.prepareStatement(resSql);
                rsPstmt.setString(1, v.getSampleId());
                rsPstmt.setInt(2, v.getTestId());
                ResultSet resRs = rsPstmt.executeQuery();
                while (resRs.next()) {
                    com.lab.lms.models.TestParameter tp = new com.lab.lms.models.TestParameter(
                        0, 0,
                        resRs.getString("name"),
                        resRs.getString("unit"),
                        resRs.getString("min_range"),
                        resRs.getString("max_range"),
                        resRs.getString("min_range_male"),
                        resRs.getString("max_range_male"),
                        resRs.getString("min_range_female"),
                        resRs.getString("max_range_female"),
                        resRs.getString("min_range_kids"),
                        resRs.getString("max_range_kids")
                    );
                    tp.setValue(resRs.getString("value") == null ? "" : resRs.getString("value"));
                    
                    Map<String, String> map = new HashMap<>();
                    map.put("name", tp.getName());
                    map.put("value", tp.getValue());
                    map.put("unit", tp.getUnit());
                    map.put("category", tp.getCategory());
                    map.put("range", tp.getRange(pGender, pAge));
                    map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                    reportData.add(map);
                }

                String commentFound = "";
                PreparedStatement commPstmt = conn.prepareStatement("SELECT comment FROM results WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?) LIMIT 1");
                commPstmt.setString(1, v.getSampleId());
                commPstmt.setInt(2, v.getTestId());
                ResultSet rsComm = commPstmt.executeQuery();
                if (rsComm.next()) {
                    String c = rsComm.getString("comment");
                    if (c != null) commentFound = c;
                }

                ReportGenerator.TestData td = new ReportGenerator.TestData(v.getTestName(), v.getTestId(), category, reportData, notes, isSp, isMic, isCul, spec, commentFound, "", new ArrayList<>(), v.getSampleId(), gStatus);
                td.growthFindings = gFindings;
                allTestData.add(td);
            }

            // Generate WhatsApp PDF (Always include header)
            String finalPath = ReportGenerator.generateMultiTestReport(pName, pId, pAge, pGender, pRef, 
                                                                     selected.get(0).getDate(), 
                                                                     pPhone, rAddress, allTestData, true, false); // No watermark for WhatsApp
            if (finalPath != null) {
                com.lab.lms.services.WhatsAppService.sendReportWithRecovery(pId, pName, pPhone, finalPath);

                String autoOpen = DatabaseManager.getSetting("auto_print_whatsapp", "true");
                if (Boolean.parseBoolean(autoOpen)) {
                    java.awt.Desktop.getDesktop().open(new java.io.File(finalPath));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Generation error: " + e.getMessage()).show();
        }
    }
    @FXML
    private void handlePrintSelectedTests() {
        java.util.List<VisitHistory> selected = new java.util.ArrayList<>();
        for (VisitHistory v : historyData) {
            if (v.isSelected() && !v.isDetailRow())
                selected.add(v);
        }

        if (selected.isEmpty()) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Fetch Patient Info (from the first selected item, they are guaranteed same set)
            String pid = selected.get(0).getPatientId();
            String pName = "", pId = "", pAge = "", pGender = "", pRef = "", rAddress = "", pPhone = "";
            PreparedStatement pStmt = conn.prepareStatement("SELECT * FROM patients WHERE patient_id = ?");
            pStmt.setString(1, pid);
            ResultSet rsInf = pStmt.executeQuery();
            if (rsInf.next()) {
                pId = rsInf.getString("patient_id");
                pName = rsInf.getString("name");
                
                int y = rsInf.getInt("age");
                int m = rsInf.getInt("age_months");
                int d = rsInf.getInt("age_days");
                pAge = y + "y";
                if (m > 0 || d > 0) pAge += " " + m + "m " + d + "d";
                
                pGender = rsInf.getString("gender");
                rAddress = rsInf.getString("address");
                pRef = rsInf.getString("referred_doctor");
                pPhone = rsInf.getString("whatsapp");
                if (pPhone == null || pPhone.isEmpty()) pPhone = rsInf.getString("phone");
            }

            // 2. Aggregate all test data
            List<ReportGenerator.TestData> allTestData = new ArrayList<>();
            for (VisitHistory v : selected) {
                // Fetch test metadata
                String notes = "", category = "ACTIVE", spec = "Blood", gStatus = "Positive", gFindings = "";
                int isSp = 0, isMic = 0, isCul = 0;
                PreparedStatement metaStmt = conn.prepareStatement("SELECT notes, category, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings FROM tests WHERE id = ?");
                metaStmt.setInt(1, v.getTestId());
                ResultSet rsMeta = metaStmt.executeQuery();
                if (rsMeta.next()) {
                    notes = rsMeta.getString("notes");
                    category = rsMeta.getString("category");
                    isSp = rsMeta.getInt("is_special");
                    isMic = rsMeta.getInt("is_microscopic");
                    isCul = rsMeta.getInt("is_culture");
                    spec = rsMeta.getString("specimen");
                    gStatus = rsMeta.getString("growth_status");
                    if (gStatus == null) gStatus = "Positive";
                    gFindings = rsMeta.getString("growth_findings");
                }

                List<Map<String, String>> reportData = new ArrayList<>();
                String resSql = "SELECT * " +
                                "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                "WHERE r.sample_id = ? AND tp.test_id = ? " +
                                "ORDER BY CASE WHEN tp.print_order = 0 THEN 999999 ELSE tp.print_order END ASC, tp.id ASC";
                PreparedStatement rsPstmt = conn.prepareStatement(resSql);
                rsPstmt.setString(1, v.getSampleId());
                rsPstmt.setInt(2, v.getTestId());
                ResultSet resRs = rsPstmt.executeQuery();
                while (resRs.next()) {
                    com.lab.lms.models.TestParameter tp = new com.lab.lms.models.TestParameter(
                        0, 0,
                        resRs.getString("name"),
                        resRs.getString("unit"),
                        resRs.getString("min_range"),
                        resRs.getString("max_range"),
                        resRs.getString("min_range_male"),
                        resRs.getString("max_range_male"),
                        resRs.getString("min_range_female"),
                        resRs.getString("max_range_female"),
                        resRs.getString("min_range_kids"),
                        resRs.getString("max_range_kids")
                    );
                    tp.setValue(resRs.getString("value") == null ? "" : resRs.getString("value"));

                    Map<String, String> map = new HashMap<>();
                    map.put("name", tp.getName());
                    map.put("value", tp.getValue());
                    map.put("unit", tp.getUnit());
                    map.put("category", tp.getCategory());
                    map.put("range", tp.getRange(pGender, pAge));
                    map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                    reportData.add(map);
                }
                String commentFound = "";
                PreparedStatement commPstmt = conn.prepareStatement("SELECT comment FROM results WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?) LIMIT 1");
                commPstmt.setString(1, v.getSampleId());
                commPstmt.setInt(2, v.getTestId());
                ResultSet rsComm = commPstmt.executeQuery();
                if (rsComm.next()) {
                    String c = rsComm.getString("comment");
                    if (c != null) commentFound = c;
                }

                ReportGenerator.TestData td = new ReportGenerator.TestData(v.getTestName(), v.getTestId(), category, reportData, notes, isSp, isMic, isCul, spec, commentFound, "", new ArrayList<>(), v.getSampleId(), gStatus);
                td.growthFindings = gFindings;
                allTestData.add(td);
            }

            boolean includeHeader = Boolean.parseBoolean(DatabaseManager.getSetting("print_header_footer", "true"));
            String finalPath = ReportGenerator.generateMultiTestReport(pName, pId, pAge, pGender, pRef, 
                                                                     selected.get(0).getDate(), // Use date from first selection
                                                                     pPhone, rAddress, allTestData, includeHeader, true);
            if (finalPath != null) {
                openReport(finalPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Generation error: " + e.getMessage()).show();
        }
    }




    private void updatePrintButtonVisibility() {
        boolean anySelected = historyData.stream().anyMatch(v -> v.isSelected() && !v.isDetailRow());
        btnPrintSelected.setVisible(anySelected);
        btnPrintSelected.setManaged(anySelected);
        btnWhatsAppSelected.setVisible(anySelected);
        btnWhatsAppSelected.setManaged(anySelected);
    }

    private void openReport(String path) {
        if (path == null || path.isEmpty())
            return;
        try {
            File pdfFile = new File(path);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Report file not found on disk.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open report.").show();
        }
    }
}

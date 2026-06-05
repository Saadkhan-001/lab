package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Test;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.awt.Desktop;
import com.lab.lms.services.InventoryService;

public class BillingController {

    @FXML
    private ComboBox<String> recentPatientCombo;
    @FXML
    private Label patientNameLabel;
    @FXML
    private TextField patientSearchField;
    @FXML
    private ComboBox<Test> testCombo;
    @FXML
    private TableView<Test> selectedTestsTable;
    @FXML
    private TableColumn<Test, String> colTestName;
    @FXML
    private TableColumn<Test, String> colNumericCode;
    @FXML
    private TableColumn<Test, String> colAlphaCode;
    @FXML
    private TableColumn<Test, Double> colPrice;
    @FXML
    private TableColumn<Test, String> colTime;
    @FXML
    private TableColumn<Test, Void> colAction;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private TextField discountField;
    @FXML
    private Label finalPayableLabel;
    @FXML
    private Label discountTypeLabel;
    @FXML
    private ToggleButton btnPercent;
    @FXML
    private ToggleButton btnCash;
    @FXML
    private Button btnPayAndProceed;

    @FXML
    private TextField paidAmountField;
    @FXML
    private Label dueBalanceLabel;
    @FXML
    private Button billingSettingsBtn;
    @FXML
    private VBox billingSettingsPanel;
    @FXML
    private TextField defaultDiscountField;
    @FXML
    private ComboBox<String> defaultDiscountTypeCombo;
    @FXML
    private CheckBox autoPayZeroCheck;

    private String selectedPatientId;
    private ObservableList<Test> selectedTests = FXCollections.observableArrayList();
    private boolean isUpdating = false;
    private ToggleGroup discountToggleGroup = new ToggleGroup();
    private boolean isCashDiscount = false;
    private double previousDue = 0.0;
    
    // Custom Popup Engine Fields
    private ContextMenu searchPopup = new ContextMenu();
    private ListView<Test> searchListView = new ListView<>();

    @FXML
    public void initialize() {
        colNumericCode.setCellValueFactory(new PropertyValueFactory<>("numericCode"));
        colAlphaCode.setCellValueFactory(new PropertyValueFactory<>("alphaCode"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("resultTime"));

        // Custom display for testCombo dropdown
        testCombo.setCellFactory(lv -> new ListCell<Test>() {
            @Override
            protected void updateItem(Test t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                } else {
                    setText(t.toString());
                }
            }
        });
        testCombo.setConverter(new javafx.util.StringConverter<Test>() {
            @Override
            public String toString(Test t) {
                return t == null ? "" : t.toString();
            }
            @Override
            public Test fromString(String string) {
                return BillingController.this.resolveTest(string);
            }
        });
        
        // Setup Custom Popup Engine
        searchListView.setPrefHeight(250);
        searchListView.setPrefWidth(450);
        searchPopup.getItems().add(new CustomMenuItem(searchListView, false));
        
        searchListView.setOnMouseClicked(e -> {
            Test selected = searchListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                testCombo.getEditor().setText(selected.getName());
                handleAddTest();
                searchPopup.hide();
            }
        });
        
        testCombo.setOnShowing(e -> {
            if (testCombo.getEditor().isFocused()) e.consume();
        });

        // Custom cell for delete button
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("×");

            {
                btnDelete.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 1 8; -fx-cursor: hand; -fx-background-radius: 0; -fx-font-size: 14;");
                btnDelete.setOnAction(event -> {
                    Test test = getTableView().getItems().get(getIndex());
                    selectedTests.remove(test);
                    calculateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        selectedTestsTable.setItems(selectedTests);

        loadTests();
        loadRecentPatients();

        btnPercent.setText("%");
        
        // Setup Discount Toggle
        btnPercent.setToggleGroup(discountToggleGroup);
        btnCash.setToggleGroup(discountToggleGroup);
        discountToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Maintain selection
                return;
            }
            isCashDiscount = (newVal == btnCash);
            discountTypeLabel.setText(isCashDiscount ? "DISCOUNT (PKR)" : "DISCOUNT (%)");
            calculateTotal();
        });

        discountField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotal());
        paidAmountField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotal());

        // Guided workflow: Auto-fill if patient was just registered
        String contextPid = com.lab.lms.services.SessionContext.getCurrentPatientId();
        if (contextPid != null && !contextPid.isEmpty()) {
            loadPatientData(contextPid);
            
            // Intelligence Layer: Ensure the context patient is in the combo, even if not in 'top 20'
            boolean found = false;
            for (String item : recentPatientCombo.getItems()) {
                if (item.contains("(" + contextPid + ")")) {
                    recentPatientCombo.setValue(item);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                // Fetch name specifically if not in top 20 to populate combo correctly
                try (Connection conn = DatabaseManager.getConnection()) {
                    PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM patients WHERE patient_id = ?");
                    pstmt.setString(1, contextPid);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String newItem = rs.getString("name") + " (" + contextPid + ")";
                        recentPatientCombo.getItems().add(0, newItem);
                        recentPatientCombo.setValue(newItem);
                    }
                } catch (SQLException e) {}
            }
        }

        // Load tests from Registration context
        java.util.List<com.lab.lms.models.Test> registrationTests = com.lab.lms.services.SessionContext
                .getSelectedTests();
        if (registrationTests != null && !registrationTests.isEmpty()) {
            selectedTests.addAll(registrationTests);
            calculateTotal();
            // Clear context after loading
            com.lab.lms.services.SessionContext.setSelectedTests(new java.util.ArrayList<>());
        }


        testCombo.setEditable(true);
        testCombo.setConverter(new javafx.util.StringConverter<com.lab.lms.models.Test>() {
            @Override
            public String toString(com.lab.lms.models.Test t) {
                return (t == null) ? "" : t.getName();
            }
            @Override
            public com.lab.lms.models.Test fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                com.lab.lms.models.Test resolved = BillingController.this.resolveTest(string);
                if (resolved == null) {
                    // FIX: Return placeholder to keep editor text alive during typing
                    return new com.lab.lms.models.Test(-1, "", "", string, "", 0, "", "", 0, 0, 0, "", "", "", "", "", "");
                }
                return resolved;
            }
        });
        testCombo.setItems(FXCollections.observableArrayList(masterTests));

        // High-Fidelity Pulse Search: Low-latency filtering with custom popup enforcement

        // High-Fidelity Pulse Search: Low-latency filtering with visibility enforcement
        javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(30));

        testCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;

            // Loop-breaker: don't re-filter if name matches selected item
            Object rawSelect = testCombo.getSelectionModel().getSelectedItem();
            if (rawSelect instanceof Test) {
                if (((Test) rawSelect).toString().equalsIgnoreCase(newVal)) return;
            }

            searchDebounce.setOnFinished(e -> {
                String queryText = testCombo.getEditor().getText();
                if (queryText == null) return;
                String query = queryText.toLowerCase().trim();
                
                isUpdating = true;
                try {
                    // High-Performance Stream Filtering with strict LIMIT to prevent UI lag
                    java.util.List<Test> results = masterTests.parallelStream()
                        .filter(t -> {
                            // Search Filter
                            if (query.isEmpty()) return true;
                            String queryClean = query.trim();
                            return t.getLowercaseName().contains(queryClean);
                        })
                        .limit(25)
                        .collect(java.util.stream.Collectors.toList());

                    javafx.application.Platform.runLater(() -> {
                        searchListView.getItems().setAll(results);
                        if (!results.isEmpty() && testCombo.getEditor().isFocused()) {
                            searchListView.getSelectionModel().select(0); // Auto-select first item
                            if (!searchPopup.isShowing()) {
                                searchPopup.show(testCombo, javafx.geometry.Side.BOTTOM, 0, 0);
                            }
                        } else {
                            searchPopup.hide();
                        }
                    });
                } finally {
                    isUpdating = false;
                }
            });
            searchDebounce.playFromStart();
        });

        // Add Test on Enter (High-Velocity Keyboard Intake)
        testCombo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (searchPopup.isShowing()) {
                if (e.getCode() == KeyCode.DOWN) {
                    int index = searchListView.getSelectionModel().getSelectedIndex();
                    searchListView.getSelectionModel().select(index + 1);
                    searchListView.scrollTo(index + 1);
                    e.consume();
                } else if (e.getCode() == KeyCode.UP) {
                    int index = searchListView.getSelectionModel().getSelectedIndex();
                    if (index > 0) {
                        searchListView.getSelectionModel().select(index - 1);
                        searchListView.scrollTo(index - 1);
                    }
                    e.consume();
                } else if (e.getCode() == KeyCode.ENTER) {
                    Test selected = searchListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        testCombo.getEditor().setText(selected.getName());
                        handleAddTest();
                        searchPopup.hide();
                        e.consume();
                    }
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                handleAddTest();
                e.consume();
            }
        });

        // Empty Action Filter to prevent closure

        // Empty Action Filter to prevent closure

        // Unified Action Label
        String specimenEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
        if (!Boolean.parseBoolean(specimenEnabled)) {
            btnPayAndProceed.setText("CONFIRM PAYMENT & START ANALYSIS");
        }

        // Admin Check for Billing Settings
        boolean isAdmin = com.lab.lms.services.SessionContext.getUserRole() != null && 
                          com.lab.lms.services.SessionContext.getUserRole().equals("ADMIN");
        billingSettingsBtn.setVisible(isAdmin);
        billingSettingsBtn.setManaged(isAdmin);
        
        defaultDiscountTypeCombo.setItems(FXCollections.observableArrayList("%", "PKR"));
        loadBillingSettings();

        loadBillingSettings();
    }

    private void loadBillingSettings() {
        String defaultVal = DatabaseManager.getSetting("billing_default_discount_value", "0");
        String defaultType = DatabaseManager.getSetting("billing_default_discount_type", "%");
        boolean autoPayZero = DatabaseManager.getSetting("billing_auto_pay_zero", "false").equals("true");
        
        defaultDiscountField.setText(defaultVal);
        defaultDiscountTypeCombo.setValue(defaultType);
        autoPayZeroCheck.setSelected(autoPayZero);
        
        // Initial application
        applyDefaultDiscount();
    }

    private void applyDefaultDiscount() {
        String defaultVal = DatabaseManager.getSetting("billing_default_discount_value", "0");
        String defaultType = DatabaseManager.getSetting("billing_default_discount_type", "%");
        
        discountField.setText(defaultVal);
        if ("PKR".equals(defaultType)) {
            btnCash.setSelected(true);
            isCashDiscount = true;
            discountTypeLabel.setText("DISCOUNT (PKR)");
        } else {
            btnPercent.setSelected(true);
            isCashDiscount = false;
            discountTypeLabel.setText("DISCOUNT (%)");
        }
        calculateTotal();
    }

    @FXML
    private void handleToggleSettings() {
        boolean isVisible = !billingSettingsPanel.isVisible();
        billingSettingsPanel.setVisible(isVisible);
        billingSettingsPanel.setManaged(isVisible);
    }

    @FXML
    private void handleSaveSettings() {
        String val = defaultDiscountField.getText();
        String type = defaultDiscountTypeCombo.getValue();
        boolean autoPayZero = autoPayZeroCheck.isSelected();
        
        DatabaseManager.saveSetting("billing_default_discount_value", val);
        DatabaseManager.saveSetting("billing_default_discount_type", type);
        DatabaseManager.saveSetting("billing_auto_pay_zero", String.valueOf(autoPayZero));
        
        new Alert(Alert.AlertType.INFORMATION, "Billing settings saved successfully.").show();
        handleToggleSettings();
        
        // Update current form if empty/fresh
        if (selectedTests.isEmpty() && discountField.getText().equals("0")) {
            applyDefaultDiscount();
        }
    }

    private ObservableList<Test> masterTests = FXCollections.observableArrayList();

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

    private void loadTests() {
        try (Connection conn = DatabaseManager.getConnection()) {
            masterTests.clear();
            String sql = "SELECT t.*, p.id as p_id, p.name as param_name, p.unit, p.min_range, p.max_range FROM tests t " +
                         "LEFT JOIN test_parameters p ON t.id = p.test_id " +
                         "WHERE p.is_global = 1 OR p.is_global IS NULL " + // Allow NULL for tests with no params yet
                         "ORDER BY t.name ASC";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                java.util.Map<Integer, com.lab.lms.models.Test> protocols = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    com.lab.lms.models.Test proto = protocols.get(id);
                    if (proto == null) {
                        proto = new com.lab.lms.models.Test(
                                id,
                                rs.getString("numeric_code") != null ? rs.getString("numeric_code").trim() : "",
                                rs.getString("alpha_code") != null ? rs.getString("alpha_code").trim() : "",
                                rs.getString("name"),
                                rs.getString("category"),
                                rs.getDouble("price"),
                                rs.getString("result_time"),
                                rs.getString("notes"),
                                rs.getInt("is_special"),
                                rs.getInt("is_microscopic"),
                                rs.getInt("is_culture"),
                                rs.getString("specimen"),
                                rs.getString("image_path"),
                                rs.getString("protocol_class"),
                                rs.getString("container"),
                                rs.getString("volume"),
                                rs.getString("fasting"));
                        protocols.put(id, proto);
                    }
                    String pName = rs.getString("param_name");
                    if (pName != null) {
                        proto.getParameters().add(new com.lab.lms.models.TestParameter(rs.getInt("p_id"), id, pName, 
                                                                                    rs.getString("unit"), rs.getString("min_range"), rs.getString("max_range")));
                    }
                }


                for (com.lab.lms.models.Test p : protocols.values()) {
                    masterTests.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRecentSelect() {
        String selected = recentPatientCombo.getValue();
        if (selected != null && selected.contains("(") && selected.contains(")")) {
            String id = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            loadPatientData(id);
        }
    }

    @FXML
    private void handlePatientSearch() {
        String query = patientSearchField.getText();
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT patient_id, name FROM patients WHERE name LIKE ? OR patient_id LIKE ? OR phone LIKE ? LIMIT 50";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            pstmt.setString(3, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            ObservableList<String> results = FXCollections.observableArrayList();
            while (rs.next()) {
                results.add(rs.getString("name") + " (" + rs.getString("patient_id") + ")");
            }

            if (results.size() == 1) {
                String selected = results.get(0);
                String id = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                recentPatientCombo.setValue(selected);
                loadPatientData(id);
            } else if (results.size() > 1) {
                recentPatientCombo.getItems().setAll(results);
                recentPatientCombo.show();
                new Alert(Alert.AlertType.INFORMATION, "Multiple patients found. Please select from the dropdown below.").show();
            } else {
                new Alert(Alert.AlertType.WARNING, "No patients found for: " + query).show();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadPatientData(String id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT name, age, age_months, age_days, gender FROM patients WHERE patient_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                selectedPatientId = id;
                String gender = rs.getString("gender");
                if (gender == null || gender.trim().isEmpty() || gender.equalsIgnoreCase("null")) {
                    gender = "N/A";
                }
                
                int y = rs.getInt("age");
                int m = rs.getInt("age_months");
                int d = rs.getInt("age_days");
                
                String ageStr = y + "y";
                if (m > 0 || d > 0) ageStr += " " + m + "m " + d + "d";
                
                patientNameLabel.setText("MR Identity: " + rs.getString("name") + " (" + gender + ", " + ageStr + ")");
                
                // Fetch Historical Balance
                try (PreparedStatement balPstmt = conn.prepareStatement("SELECT SUM(due_amount) FROM invoices WHERE patient_id = ?")) {
                    balPstmt.setString(1, id);
                    ResultSet balRs = balPstmt.executeQuery();
                    if (balRs.next()) {
                        previousDue = balRs.getDouble(1);
                        if (previousDue > 0) {
                            patientNameLabel.setText(patientNameLabel.getText() + " | PREVIOUS DUE: PKR " + String.format("%.2f", previousDue));
                        }
                    } else {
                        previousDue = 0.0;
                    }
                }
                
                patientNameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
            } else {
                selectedPatientId = null;
                patientNameLabel.setText("Patient not found.");
                patientNameLabel.setStyle("-fx-text-fill: #961111;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTest() {
        Object selected = testCombo.getSelectionModel().getSelectedItem();
        Test t = null;
        if (selected instanceof Test) {
            t = (Test) selected;
        } else {
            String input = testCombo.getEditor().getText();
            if (t == null && input != null && !input.trim().isEmpty()) {
                t = resolveTest(input);
                if (t == null) {
                    loadTests();
                    t = resolveTest(input);
                }
            }
        }

        if (t != null && t.getId() != -1 && !selectedTests.contains(t)) {
            selectedTests.add(t);
            calculateTotal();
            
            // Full UI Reset
            isUpdating = true;
            try {
                testCombo.getSelectionModel().clearSelection();
                testCombo.getEditor().clear();
                testCombo.getItems().setAll(masterTests);
            } finally {
                isUpdating = false;
            }
            javafx.application.Platform.runLater(() -> testCombo.getEditor().requestFocus());
        }
    }

    private Test resolveTest(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        // Strip trailing space for resolution while typing
        String query = input.toLowerCase().trim();
        for (Test test : masterTests) {
            String nCode = test.getNumericCode() != null ? test.getNumericCode().toLowerCase().trim() : "";
            String aCode = test.getAlphaCode() != null ? test.getAlphaCode().toLowerCase().trim() : "";
            String name = test.getName().toLowerCase();
            String fullStr = test.toString().toLowerCase();

            if (fullStr.equalsIgnoreCase(query) || name.equalsIgnoreCase(query) || 
                aCode.equalsIgnoreCase(query) || nCode.equalsIgnoreCase(query)) return test;
            
            if (name.contains(query) || fullStr.contains(query)) return test;
        }
        return null;
    }
    private void calculateTotal() {
        double total = 0;
        for (Test t : selectedTests) {
            total += t.getPrice();
        }
        totalAmountLabel.setText(String.format("PKR %.2f", total));

        double discountValue = 0;
        try {
            discountValue = Double.parseDouble(discountField.getText());
        } catch (NumberFormatException e) {
            // ignore
        }

        double finalAmount;
        if (isCashDiscount) {
            finalAmount = total - discountValue;
        } else {
            finalAmount = total - (total * (discountValue / 100));
        }
        
        if (finalAmount < 0) finalAmount = 0;
        finalPayableLabel.setText(String.format("PKR %.2f", finalAmount));
        
        // Paid Amount & Due Balance Logic
        double paidValue = 0;
        try {
            paidValue = Double.parseDouble(paidAmountField.getText());
        } catch (NumberFormatException e) {}
        
        double dueValue = (finalAmount - paidValue) + previousDue;
        if (dueValue < 0) dueValue = 0;
        dueBalanceLabel.setText(String.format("PKR %.2f", dueValue));
        
        // Visual feedback: Maintain Professional Medical Red for all financial states
        dueBalanceLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #961111;");
    }
    @FXML
    private void handleGenerateInvoice() {
        if (selectedPatientId == null || selectedTests.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a patient and at least one test.");
            alert.show();
            return;
        }

        double total = Double.parseDouble(totalAmountLabel.getText().replace("PKR ", "").trim());
        double finalAmount = Double.parseDouble(finalPayableLabel.getText().replace("PKR ", "").trim());
        double absoluteDiscount = total - finalAmount;
        
        double paidAmount = 0;
        try {
            paidAmount = Double.parseDouble(paidAmountField.getText());
        } catch (NumberFormatException e) {}

        // Handle Auto-Pay on Zero setting
        boolean autoPayIfZero = DatabaseManager.getSetting("billing_auto_pay_zero", "false").equals("true");
        double dueAmount = finalAmount - paidAmount;
        if (autoPayIfZero && paidAmount == 0) {
            paidAmount = finalAmount;
            dueAmount = 0;
        }
        
        if (dueAmount < 0) dueAmount = 0;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Clinical Info Declarations
                String patName = "N/A", ageVal = "N/A", genderVal = "N/A", phoneVal = "N/A", refVal = "Self";
                String collectionTime = "N/A", reportingTime = "N/A", receiptPath = null;

                // Calculate Actual Reporting Time BEFORE generation
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");
                java.util.Date now = new java.util.Date();
                collectionTime = sdf.format(now);
                
                long maxOffsetMillis = 3600000;
                for (Test t : selectedTests) {
                    String rt = t.getResultTime();
                    if (rt != null) {
                        rt = rt.toLowerCase();
                        try {
                            if (rt.contains("day")) {
                                int days = Integer.parseInt(rt.replaceAll("[^0-9]", ""));
                                maxOffsetMillis = Math.max(maxOffsetMillis, days * 86400000L);
                            } else if (rt.contains("hour")) {
                                int hours = Integer.parseInt(rt.replaceAll("[^0-9]", ""));
                                maxOffsetMillis = Math.max(maxOffsetMillis, hours * 3600000L);
                            } else if (rt.contains("min")) {
                                int mins = Integer.parseInt(rt.replaceAll("[^0-9]", ""));
                                maxOffsetMillis = Math.max(maxOffsetMillis, mins * 60000L);
                            }
                        } catch (Exception e) {}
                    }
                }
                java.util.Date reportingDateObj = new java.util.Date(now.getTime() + maxOffsetMillis + (5 * 60000));
                reportingTime = sdf.format(reportingDateObj);

                // Fetch Patient Info for Receipt
                try (PreparedStatement pnPstmt = conn.prepareStatement("SELECT name, age, age_months, age_days, gender, COALESCE(whatsapp, phone) as contact, referred_doctor FROM patients WHERE patient_id = ?")) {
                    pnPstmt.setString(1, selectedPatientId);
                    try (ResultSet pnRs = pnPstmt.executeQuery()) {
                        if (pnRs.next()) {
                            patName = pnRs.getString("name");
                            ageVal = pnRs.getString("age") + "y";
                            genderVal = pnRs.getString("gender");
                            phoneVal = pnRs.getString("contact");
                            refVal = pnRs.getString("referred_doctor");
                        }
                    }
                }

                // 1.5 Pre-fetch Branding while we have a connection and lock
                com.lab.lms.models.LabBranding branding = new com.lab.lms.models.LabBranding(
                        DatabaseManager.getSetting("lab_name", "Laboratory Information System"),
                        DatabaseManager.getSetting("lab_address", ""),
                        DatabaseManager.getSetting("lab_contact", ""),
                        DatabaseManager.getSetting("lab_email", ""),
                        DatabaseManager.getSetting("lab_website", ""),
                        DatabaseManager.getSetting("lab_tagline", ""),
                        DatabaseManager.getSetting("lab_logo", ""),
                        DatabaseManager.getSetting("receipt_policies", "1. Standard Receipt")
                );

                String status = (dueAmount <= 0) ? "PAID" : "PARTIALLY PAID";
                String sql = "INSERT INTO invoices (patient_id, total_amount, discount, final_amount, paid_amount, due_amount, status, staff_id, receipt_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, selectedPatientId);
                pstmt.setDouble(2, total);
                pstmt.setDouble(3, absoluteDiscount);
                pstmt.setDouble(4, finalAmount);
                pstmt.setDouble(5, paidAmount);
                pstmt.setDouble(6, dueAmount);
                pstmt.setString(7, status);
                pstmt.setString(8, com.lab.lms.services.SessionContext.getStaffId());
                
                // Pre-calculate path but generate later
                String receiptDir = System.getProperty("user.home") + java.io.File.separator + ".lablms" + java.io.File.separator + "receipts";
                new java.io.File(receiptDir).mkdirs();
                String finalReceiptPath = new java.io.File(receiptDir, "INV_" + selectedPatientId + "_" + System.currentTimeMillis() + ".pdf").getAbsolutePath();
                pstmt.setString(9, finalReceiptPath);
                
                pstmt.executeUpdate();

                // 3. Specimen Generation & Results Setup
                String specimenEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
                String sampleStatus = Boolean.parseBoolean(specimenEnabled) ? "AWAITING COLLECTION" : "COLLECTED";
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                
                String sampleSql = "INSERT INTO samples (sample_id, patient_id, status, collection_date, staff_id) VALUES (?, ?, ?, ?, ?)";
                String resSql = "INSERT INTO results (sample_id, test_id, parameter_id, value, status, is_abnormal) VALUES (?, ?, ?, '', 'PENDING', 0)";
                
                PreparedStatement sPstmt = conn.prepareStatement(sampleSql);
                PreparedStatement rPstmt = conn.prepareStatement(resSql);
                
                int testIndex = 1;
                for (Test test : selectedTests) {
                    String sampleId = "SAM-" + timestamp + "-" + testIndex;
                    testIndex++;
                    sPstmt.setString(1, sampleId);
                    sPstmt.setString(2, selectedPatientId);
                    sPstmt.setString(3, sampleStatus);
                    if ("COLLECTED".equals(sampleStatus)) {
                        sPstmt.setString(4, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                    } else {
                        sPstmt.setNull(4, java.sql.Types.VARCHAR);
                    }
                    sPstmt.setString(5, com.lab.lms.services.SessionContext.getStaffId());
                    sPstmt.addBatch();
                    for (com.lab.lms.models.TestParameter tp : test.getParameters()) {
                        rPstmt.setString(1, sampleId);
                        rPstmt.setInt(2, test.getId());
                        rPstmt.setInt(3, tp.getId());
                        rPstmt.addBatch();
                    }
                    // Insert anchor row (-1) for status synchronization and clinical notes persistence
                    rPstmt.setString(1, sampleId);
                    rPstmt.setInt(2, test.getId());
                    rPstmt.setInt(3, -1);
                    rPstmt.addBatch();

                    com.lab.lms.services.SessionContext.setCurrentSampleId(sampleId);
                }
                sPstmt.executeBatch();
                rPstmt.executeBatch();

                conn.commit();
                System.out.println("[BILLING] Transaction committed. Starting PDF generation...");

                // Capture data for background thread
                final String fPatName = patName;
                final String fPatId = selectedPatientId;
                final String fAge = ageVal;
                final String fGender = genderVal;
                final String fPhone = phoneVal;
                final String fRef = refVal;
                final String fColl = collectionTime;
                final String fRep = reportingTime;
                final List<Test> fTests = new ArrayList<>(selectedTests);
                final double fTotal = total;
                final double fDiscount = absoluteDiscount;
                final double fFinal = finalAmount;
                final double fPaid = paidAmount;
                final double fDue = dueAmount;
                final String fReceiptPath = finalReceiptPath;
                final String fLastSampleId = com.lab.lms.services.SessionContext.getCurrentSampleId();

                new Thread(() -> {
                    try {
                        // Generate using pre-defined path and branding
                        System.out.println("[TRACE] Starting async PDF generation...");
                        String generatedPath = com.lab.lms.services.ReportGenerator.generateReceipt(
                            fPatName, fPatId, fAge, fGender, fPhone, fRef, 
                            fColl, fRep, fTests, fTotal, fDiscount, fFinal, fPaid, fDue, branding
                        );

                        javafx.application.Platform.runLater(() -> {
                            if (generatedPath != null) {
                                showBillingSuccessActions(generatedPath, fLastSampleId);
                                clearForm();
                                handleNext();
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Failed to generate receipt PDF.").show();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "System Error: " + e.getMessage()).show();
        }
    }

    private void showBillingSuccessActions(String pdfPath, String sampleId) {
        Alert actions = new Alert(Alert.AlertType.CONFIRMATION);
        actions.setTitle("Billing Complete");
        actions.setHeaderText("Invoice Generated Successfully");
        actions.setContentText("The clinical receipt is ready. All selected tests have been queued for specimen collection.");

        ButtonType btnView = new ButtonType("VIEW RECEIPT");
        ButtonType btnPrint = new ButtonType("PRINT");
        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);

        actions.getButtonTypes().setAll(btnView, btnPrint, btnOk);

        actions.showAndWait().ifPresent(response -> {
            if (response == btnView || response == btnPrint) {
                try {
                    Desktop.getDesktop().open(new File(pdfPath));
                    showBillingSuccessActions(pdfPath, sampleId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void clearForm() {
        selectedPatientId = null;
        patientNameLabel.setText("NO PATIENT SELECTED");
        patientNameLabel.setStyle("-fx-text-fill: #961111;");
        selectedTests.clear();
        discountField.setText("0");
        paidAmountField.setText("0");
        applyDefaultDiscount();
        calculateTotal();
    }

    @FXML
    private void handleNext() {
        String specimenEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
        if (Boolean.parseBoolean(specimenEnabled)) {
            com.lab.lms.services.NavigationService.switchView("/fxml/samples.fxml");
        } else {
            com.lab.lms.services.NavigationService.switchView("/fxml/processing.fxml");
        }
    }
}


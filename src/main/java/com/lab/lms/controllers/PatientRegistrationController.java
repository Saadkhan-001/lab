package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PatientRegistrationController {

    @FXML
    private ComboBox<com.lab.lms.models.Patient> patientIdCombo;
    @FXML
    private ComboBox<String> titleCombo;
    @FXML
    private TextField nameField;
    @FXML
    private TextField ageField;
    @FXML
    private TextField ageMonthField;
    @FXML
    private TextField ageDayField;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField whatsappField;
    @FXML
    private TextField addressArea;
    @FXML
    private TextField referredDoctorField;
    @FXML
    private DatePicker regDatePicker;
    @FXML
    private TextField regTimeField;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField existingPatientSearch;
    @FXML
    private Label searchResultLabel;

    @FXML
    private ComboBox<com.lab.lms.models.Test> testCombo;
    @FXML
    private TextField selectedTestsField;

    private javafx.collections.ObservableList<com.lab.lms.models.Test> selectedTests = FXCollections
            .observableArrayList();
    private javafx.collections.ObservableList<com.lab.lms.models.Test> masterTests = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<com.lab.lms.models.Test> filteredTests;
    private boolean isUpdating = false;
    private String lastGeneratedId;
    
    // Custom Popup Engine Fields
    private javafx.scene.control.ContextMenu searchPopup = new javafx.scene.control.ContextMenu();
    private javafx.scene.control.ListView<com.lab.lms.models.Test> searchListView = new javafx.scene.control.ListView<>();

    @FXML private javafx.scene.layout.HBox actionButtonsBox;
    @FXML private Button btnEmptyTemplate;
    @FXML private Button btnResetFields;
    @FXML private Button btnCommitRegistration;
    @FXML private Button btnProceedTests;
    @FXML private Button btnConfirmEdit;

    @FXML
    public void initialize() {
        titleCombo.setItems(FXCollections.observableArrayList("Mr.", "Mrs.", "Miss", "Dr.", "Prof.", "Other"));
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));

        // Default to current date and time with live sync
        regDatePicker.setValue(java.time.LocalDate.now());
        startLiveClock();
        
        // Setup Custom Popup Engine
        searchListView.setPrefHeight(250);
        searchListView.setPrefWidth(450);
        searchListView.getStyleClass().add("medical-list-view");
        javafx.scene.control.CustomMenuItem customItem = new javafx.scene.control.CustomMenuItem(searchListView, false);
        searchPopup.getItems().add(customItem);
        
        searchListView.setOnMouseClicked(e -> {
            com.lab.lms.models.Test selected = searchListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                testCombo.getEditor().setText(selected.getName());
                handleAddTest();
                searchPopup.hide();
            }
        });
        
        testCombo.setOnShowing(e -> {
            if (testCombo.getEditor().isFocused()) e.consume();
        });

        loadTests();
        loadRecentPatients();

        // ── Existing Patient Search Bar with ContextMenu Popup ──────────────────
        ContextMenu patientSearchPopup = new ContextMenu();
        javafx.animation.PauseTransition existingSearchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));

        existingPatientSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                patientSearchPopup.hide();
                searchResultLabel.setText("");
                return;
            }

            existingSearchDebounce.setOnFinished(ev -> {
                String q = newVal.trim();
                try (Connection conn = DatabaseManager.getConnection()) {
                    String sql = "SELECT * FROM patients WHERE patient_id LIKE ? OR name LIKE ? OR phone LIKE ? OR whatsapp LIKE ? ORDER BY id DESC LIMIT 10";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, "%" + q + "%");
                    pstmt.setString(2, "%" + q + "%");
                    pstmt.setString(3, "%" + q + "%");
                    pstmt.setString(4, "%" + q + "%");
                    ResultSet rs = pstmt.executeQuery();

                    java.util.List<MenuItem> items = new java.util.ArrayList<>();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        String pid = rs.getString("patient_id");
                        String pName = rs.getString("name");
                        String pPhone = rs.getString("phone");
                        String pGender = rs.getString("gender");
                        int pAge = rs.getInt("age");

                        String display = pid + "  ·  " + (pName != null ? pName.toUpperCase() : "N/A")
                                + "  ·  " + (pPhone != null && !pPhone.isEmpty() ? pPhone : "No Phone")
                                + "  ·  " + pAge + "Y / " + (pGender != null ? pGender : "N/A");

                        MenuItem mi = new MenuItem(display);
                        mi.setStyle("-fx-font-size: 11; -fx-font-family: 'Segoe UI', 'Inter', sans-serif;");
                        final String selectedPid = pid;
                        mi.setOnAction(e -> {
                            loadPatientData(selectedPid);
                            existingPatientSearch.setText("");
                            patientSearchPopup.hide();
                            searchResultLabel.setText("✅ Loaded: " + pName);
                        });
                        items.add(mi);
                    }

                    javafx.application.Platform.runLater(() -> {
                        patientSearchPopup.getItems().setAll(items);
                        if (!items.isEmpty() && existingPatientSearch.isFocused()) {
                            if (!patientSearchPopup.isShowing()) {
                                patientSearchPopup.show(existingPatientSearch, javafx.geometry.Side.BOTTOM, 0, 0);
                            }
                            searchResultLabel.setText(items.size() + " patient(s) found");
                        } else {
                            patientSearchPopup.hide();
                            searchResultLabel.setText("No patients found");
                        }
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
            existingSearchDebounce.playFromStart();
        });

        existingPatientSearch.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) patientSearchPopup.hide();
        });

        // Default Age Strategy: Standardize all clinical intake at 0
        ageField.setText("0");
        ageMonthField.setText("0");
        ageDayField.setText("0");

        // Focus Handlers for rapid high-fidelity data entry
        for (TextField tf : new TextField[]{ageField, ageMonthField, ageDayField}) {
            tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) javafx.application.Platform.runLater(tf::selectAll);
            });
        }

        // Auto-Tab Logic: Intelligent transition between granular age segments
        ageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() >= 2) ageMonthField.requestFocus();
        });
        ageMonthField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() >= 2) ageDayField.requestFocus();
        });

        selectedTests
                .addListener((javafx.collections.ListChangeListener.Change<? extends com.lab.lms.models.Test> c) -> {
                    String testNames = selectedTests.stream()
                            .map(com.lab.lms.models.Test::toString)
                            .collect(java.util.stream.Collectors.joining(", "));
                    selectedTestsField.setText(testNames);
                });

        // Search-as-you-type logic for Test Selection
        testCombo.setEditable(true);
        testCombo.setConverter(new javafx.util.StringConverter<com.lab.lms.models.Test>() {
            @Override
            public String toString(com.lab.lms.models.Test t) {
                return (t == null) ? "" : t.getName();
            }
            @Override
            public com.lab.lms.models.Test fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                com.lab.lms.models.Test resolved = PatientRegistrationController.this.resolveTest(string);
                if (resolved == null) {
                    // FIX: Return placeholder to keep editor text alive during typing
                    return new com.lab.lms.models.Test(-1, "", "", string, "", 0, "", "", 0, 0, 0, "", "", "", "", "", "");
                }
                return resolved;
            }
        });
        loadTests();
        filteredTests = new javafx.collections.transformation.FilteredList<>(masterTests, t -> true);
        testCombo.setItems(filteredTests);
        
        // High-Fidelity Pulse Search: Low-latency filtering with custom popup enforcement
        javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(30));
        
        testCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;
            
            // Loop-Breaker: Don't re-filter if the text change is just the system showing the selected item name.
            Object rawSelect = testCombo.getSelectionModel().getSelectedItem();
            if (rawSelect instanceof com.lab.lms.models.Test) {
                if (((com.lab.lms.models.Test) rawSelect).getName().equalsIgnoreCase(newVal)) return;
            }

            searchDebounce.setOnFinished(e -> {
                String queryText = testCombo.getEditor().getText();
                if (queryText == null) return;
                String query = queryText.toLowerCase().trim();
                
                isUpdating = true;
                try {
                    filteredTests.setPredicate(t -> {
                        if (query.isEmpty()) return true;
                        
                        String queryClean = query.trim();
                        String nCode = t.getNumericCode() != null ? t.getNumericCode().toLowerCase() : "";
                        String aCode = t.getAlphaCode() != null ? t.getAlphaCode().toLowerCase() : "";
                        String nName = t.getName().toLowerCase();
                        return nName.contains(queryClean) || nCode.contains(queryClean) || aCode.contains(queryClean);
                    });

                    javafx.application.Platform.runLater(() -> {
                        searchListView.getItems().setAll(filteredTests);
                        if (!filteredTests.isEmpty() && testCombo.getEditor().isFocused()) {
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
                    com.lab.lms.models.Test selected = searchListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        testCombo.getEditor().setText(selected.getName());
                        handleAddTest();
                        searchPopup.hide();
                        e.consume();
                    }
                }
            } else if (e.getCode() == KeyCode.ENTER) {
                // Ensure selection is committed from dropdown if visible
                if (testCombo.isShowing()) {
                    Object selected = testCombo.getSelectionModel().getSelectedItem();
                    if (selected == null && !testCombo.getItems().isEmpty()) {
                        testCombo.getSelectionModel().select(0);
                    }
                }
                handleAddTest();
                e.consume();
            }
        });

        // Global Keyboard Shortcuts for High-Velocity Workflow
        javafx.application.Platform.runLater(() -> {
            if (nameField.getScene() != null) {
                nameField.getScene().getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown()) {
                        if (event.getCode() == KeyCode.N) {
                            handleClear();
                            event.consume();
                        }
                    }
                });
            }
        });

        // NEW: Debounced Patient Search to prevent intensive DB hits on every keystroke
        javafx.animation.PauseTransition patientSearchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
        
        // FIX: Strong-typed StringConverter to handle editable ComboBox logic safely
        patientIdCombo.setConverter(new javafx.util.StringConverter<com.lab.lms.models.Patient>() {
            @Override
            public String toString(com.lab.lms.models.Patient p) {
                return (p == null) ? "" : p.toString();
            }
            @Override
            public com.lab.lms.models.Patient fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                // If it's already a full display string, extract ID
                String id = string.contains(" - ") ? string.split(" - ")[0].trim() : string.trim();
                return new com.lab.lms.models.Patient(id, "", 0, 0, 0, "", "", "", "", "", "");
            }
        });

        patientIdCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty() || newVal.equals(lastGeneratedId) || newVal.contains(" - ")) {
                return;
            }
            if (newVal.length() >= 2) {
                // Don't search if we just selected a patient
                Object selected = patientIdCombo.getSelectionModel().getSelectedItem();
                if (selected != null && selected.toString().equals(newVal)) {
                    return;
                }
                
                patientSearchDebounce.setOnFinished(e -> searchPatients(newVal));
                patientSearchDebounce.playFromStart();
            }
        });

        // FIX: Use raw ChangeListener to bypass the ClassCastException in JVM bridge methods
        patientIdCombo.getSelectionModel().selectedItemProperty().addListener(new javafx.beans.value.ChangeListener<Object>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Object> obs, Object oldVal, Object newVal) {
                if (newVal == null) return;
                
                // FIX: Stop any pending search to prevent late item-refresh from clearing this selection
                patientSearchDebounce.stop();
                
                if (newVal instanceof com.lab.lms.models.Patient) {
                    com.lab.lms.models.Patient p = (com.lab.lms.models.Patient) newVal;
                    // If it's a skeleton from converter, only load if it has an ID
                    if (p.getPatientId() != null && !p.getPatientId().isEmpty()) {
                        loadPatientData(p.getPatientId());
                    }
                } else if (newVal instanceof String) {
                    String potentialId = (String) newVal;
                    if (potentialId.contains(" - ")) {
                        potentialId = potentialId.split(" - ")[0].trim();
                    }
                    if (!potentialId.isEmpty()) {
                        loadPatientData(potentialId);
                    }
                }
            }
        });

        // Check for existing session patient (e.g. from Profile screen)
        String pid = com.lab.lms.services.SessionContext.getCurrentPatientId();
        if (com.lab.lms.services.SessionContext.isEditProfileMode()) {
            btnEmptyTemplate.setVisible(false); btnEmptyTemplate.setManaged(false);
            btnResetFields.setVisible(false); btnResetFields.setManaged(false);
            btnCommitRegistration.setVisible(false); btnCommitRegistration.setManaged(false);
            btnProceedTests.setVisible(false); btnProceedTests.setManaged(false);
            
            btnConfirmEdit.setVisible(true); btnConfirmEdit.setManaged(true);
            statusLabel.setText("EDITING PATIENT PROFILE");
        }

        if (pid != null && !pid.isEmpty()) {
            loadPatientData(pid);
        } else {
            generatePatientId();
        }

        // --- NEW: Custom ContextMenu Autocomplete for Referring Physician ---
        loadRecentDoctors();
        ContextMenu doctorPopup = new ContextMenu();
        
        referredDoctorField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;
            
            if (newVal == null || newVal.trim().isEmpty()) {
                doctorPopup.hide();
                return;
            }

            String query = newVal.trim().toLowerCase();
            java.util.List<String> matches = doctorNames.stream()
                .filter(d -> d.toLowerCase().contains(query))
                .limit(8)
                .collect(java.util.stream.Collectors.toList());

            if (matches.isEmpty() || !referredDoctorField.isFocused()) {
                doctorPopup.hide();
            } else {
                doctorPopup.getItems().clear();
                for (String match : matches) {
                    MenuItem item = new MenuItem(match);
                    item.setOnAction(e -> {
                        isUpdating = true;
                        referredDoctorField.setText(match);
                        isUpdating = false;
                        referredDoctorField.requestFocus();
                        referredDoctorField.positionCaret(match.length());
                    });
                    doctorPopup.getItems().add(item);
                }
                
                if (!doctorPopup.isShowing()) {
                    doctorPopup.show(referredDoctorField, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            }
        });

        referredDoctorField.focusedProperty().addListener((obs, old, newVal) -> {
            if (!newVal) doctorPopup.hide();
        });

        referredDoctorField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (doctorPopup.isShowing()) {
                    doctorPopup.hide();
                }
            }
        });

        // --- NEW: Custom ContextMenu Autocomplete for Residential Address ---
        loadRecentAddresses();
        ContextMenu addressPopup = new ContextMenu();
        
        addressArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;
            
            if (newVal == null || newVal.trim().isEmpty()) {
                addressPopup.hide();
                return;
            }

            String query = newVal.trim().toLowerCase();
            java.util.List<String> matches = recentAddresses.stream()
                .filter(a -> a.toLowerCase().contains(query))
                .limit(8)
                .collect(java.util.stream.Collectors.toList());

            if (matches.isEmpty() || !addressArea.isFocused()) {
                addressPopup.hide();
            } else {
                addressPopup.getItems().clear();
                for (String match : matches) {
                    MenuItem item = new MenuItem(match);
                    item.setOnAction(e -> {
                        isUpdating = true;
                        addressArea.setText(match);
                        isUpdating = false;
                        addressArea.requestFocus();
                        addressArea.positionCaret(match.length());
                    });
                    addressPopup.getItems().add(item);
                }
                
                if (!addressPopup.isShowing()) {
                    addressPopup.show(addressArea, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            }
        });

        addressArea.focusedProperty().addListener((obs, old, newVal) -> {
            if (!newVal) addressPopup.hide();
        });

        addressArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (addressPopup.isShowing()) {
                    addressPopup.hide();
                }
            }
        });
    }

    private void startLiveClock() {
        javafx.animation.Timeline clock = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, e -> {
            if (!regTimeField.isFocused() && !isUpdating) {
                regTimeField.setText(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            }
        }), new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1)));
        clock.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        clock.play();
    }

    private javafx.collections.ObservableList<String> doctorNames = FXCollections.observableArrayList();
    private void loadRecentDoctors() {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT DISTINCT referred_doctor FROM patients WHERE referred_doctor IS NOT NULL AND referred_doctor != '' ORDER BY referred_doctor ASC LIMIT 50";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            doctorNames.clear();
            while (rs.next()) {
                doctorNames.add(rs.getString("referred_doctor"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private javafx.collections.ObservableList<String> recentAddresses = FXCollections.observableArrayList();
    private void loadRecentAddresses() {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT DISTINCT address FROM patients WHERE address IS NOT NULL AND address != '' ORDER BY address ASC LIMIT 50";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            recentAddresses.clear();
            while (rs.next()) {
                recentAddresses.add(rs.getString("address"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrintEmptyTemplate() {
        try {
            String pdfPath = com.lab.lms.services.ReportGenerator.generateEmptyTemplate();
            if (pdfPath != null) {
                java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentPatients() {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT * FROM patients ORDER BY id DESC LIMIT 15";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            javafx.collections.ObservableList<com.lab.lms.models.Patient> list = FXCollections.observableArrayList();
            while (rs.next()) {
                list.add(new com.lab.lms.models.Patient(
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
            javafx.application.Platform.runLater(() -> patientIdCombo.getItems().setAll(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void searchPatients(String query) {
        if (query == null || query.trim().isEmpty() || query.contains(" - ")) {
            loadRecentPatients();
            return;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT * FROM patients WHERE patient_id LIKE ? OR name LIKE ? OR phone LIKE ? OR whatsapp LIKE ? LIMIT 15";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            pstmt.setString(3, "%" + query + "%");
            pstmt.setString(4, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();
            javafx.collections.ObservableList<com.lab.lms.models.Patient> matches = FXCollections.observableArrayList();
            while (rs.next()) {
                matches.add(new com.lab.lms.models.Patient(
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
            javafx.application.Platform.runLater(() -> {
                patientIdCombo.getItems().setAll(matches);
                if (!matches.isEmpty()) {
                    patientIdCombo.show();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPatientData(String pid) {
        try (Connection conn = DatabaseManager.getConnection()) {
            
            // FIX: Ensure the editor text actually shows the ID being loaded, preventing "empty" appearance
            javafx.application.Platform.runLater(() -> {
                if (!patientIdCombo.getEditor().getText().startsWith(pid)) {
                    patientIdCombo.getEditor().setText(pid);
                }
            });

            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM patients WHERE patient_id = ?");
            pstmt.setString(1, pid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                ageField.setText(String.valueOf(rs.getInt("age")));
                ageMonthField.setText(String.valueOf(rs.getInt("age_months")));
                ageDayField.setText(String.valueOf(rs.getInt("age_days")));
                String gender = rs.getString("gender");
                genderCombo.setValue(gender != null ? gender : "N/A");
                String title = rs.getString("title");
                titleCombo.setValue(title != null ? title : "");
                phoneField.setText(rs.getString("phone"));
                whatsappField.setText(rs.getString("whatsapp"));
                addressArea.setText(rs.getString("address"));
                
                isUpdating = true;
                try {
                    String doc = rs.getString("referred_doctor");
                    if (com.lab.lms.services.SessionContext.isEditProfileMode()) {
                        referredDoctorField.setText(doc != null ? doc : "");
                    } else {
                        referredDoctorField.setText("");
                    }
                } finally {
                    isUpdating = false;
                }
                
                statusLabel.setText("Active MR Identity: " + rs.getString("name"));
                statusLabel.setStyle("-fx-text-fill: #1B5E20;");

                com.lab.lms.services.SessionContext.setCurrentPatientId(pid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTests() {
        try (Connection conn = DatabaseManager.getConnection()) {
            masterTests.clear();
            
            // 1. Load All Protocols and Their Parameters in a single efficient pass
            String sql = "SELECT t.*, p.id as p_id, p.name as param_name FROM tests t " +
                         "LEFT JOIN test_parameters p ON t.id = p.test_id " +
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
                        proto.getParameters().add(new com.lab.lms.models.TestParameter(rs.getInt("p_id"), id, pName, "", "", ""));
                    }
                }
                
                // 2. Intelligence Layer: Flatten the catalogue for Precision Selection
                for (com.lab.lms.models.Test p : protocols.values()) {
                    // Add the Full Protocol (High-Velocity option)
                    masterTests.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTest() {
        com.lab.lms.models.Test t = testCombo.getValue();
        String input = testCombo.getEditor().getText();
        
        if (t == null && input != null && !input.trim().isEmpty()) {
            t = resolveTest(input);
            if (t == null) {
                loadTests();
                t = resolveTest(input);
            }
        }

        if (t != null && t.getId() != -1) {
            final com.lab.lms.models.Test finalTest = t;
            boolean exists = selectedTests.stream()
                .anyMatch(st -> st.getName().equalsIgnoreCase(finalTest.getName()));
            
            if (!exists) {
                selectedTests.add(t);
                statusLabel.setText("✅ Added: " + t.getName());
                statusLabel.setStyle("-fx-text-fill: #1A0A0A; -fx-font-weight: bold;");
                javafx.application.Platform.runLater(() -> {
                    isUpdating = true;
                    try {
                        testCombo.setValue(null);
                        testCombo.getEditor().clear();
                        filteredTests.setPredicate(p -> true);
                    } finally {
                        isUpdating = false;
                    }
                    testCombo.getEditor().requestFocus();
                });
            } else {
                statusLabel.setText("ℹ️ Already in list: " + t.getName());
                statusLabel.setStyle("-fx-text-fill: #455A64;");
                javafx.application.Platform.runLater(() -> testCombo.getEditor().requestFocus());
            }
        } else {
            statusLabel.setText("⚠️ Please select a valid diagnostic protocol.");
            statusLabel.setStyle("-fx-text-fill: #961111; -fx-font-weight: bold;");
        }
    }

    private com.lab.lms.models.Test resolveTest(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        // Strip trailing space for resolution while typing
        String query = input.toLowerCase().trim();
        for (com.lab.lms.models.Test test : masterTests) {
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

    @FXML
    private void handleRemoveTest() {
        selectedTests.clear();
    }

    private void generatePatientId() {
        String yearMonth = new SimpleDateFormat("yyyyMM").format(new Date());
        String prefix = "PAT" + yearMonth;
        try (Connection conn = DatabaseManager.getConnection()) {
            // Robust Sequence Recovery: Find the highest MR number in the current month
            String sql = "SELECT patient_id FROM patients WHERE patient_id LIKE ? ORDER BY patient_id DESC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, prefix + "%");
            ResultSet rs = pstmt.executeQuery();

            int nextNumber = 1;
            if (rs.next()) {
                String lastId = rs.getString(1);
                if (lastId != null && lastId.length() > prefix.length()) {
                    try {
                        String numStr = lastId.substring(prefix.length());
                        // Extract only numeric part in case of any suffixes
                        numStr = numStr.replaceAll("[^0-9]", "");
                        if (!numStr.isEmpty()) {
                            nextNumber = Integer.parseInt(numStr) + 1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // If no patient for this month, check if there's any patient at all for a fallback
                String fallbackSql = "SELECT patient_id FROM patients ORDER BY registration_date DESC LIMIT 1";
                ResultSet fallbackRs = conn.createStatement().executeQuery(fallbackSql);
                if (fallbackRs.next()) {
                    String lastGlobalId = fallbackRs.getString(1);
                    // If it follows the PATyyyyMMnnnn format, we can try to extract a global sequence
                }
            }
            lastGeneratedId = prefix + String.format("%04d", nextNumber);
            
            // Sync UI State
            javafx.application.Platform.runLater(() -> {
                patientIdCombo.getEditor().setText(lastGeneratedId);
                lastGeneratedId = lastGeneratedId; // Ensure visibility
                statusLabel.setText("System Ready: New MR Assigned (" + lastGeneratedId + ")");
                statusLabel.setStyle("-fx-text-fill: #1B5E20;");
            });
        } catch (SQLException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> patientIdCombo.getEditor().setText("SEQ_ERR"));
        }
    }

    @FXML
    private void handleSave() {
        if (savePatientData()) {
            statusLabel.setText("MR Identity Synchronized.");
            statusLabel.setStyle("-fx-text-fill: #1B5E20;");
        }
    }

    private boolean savePatientData() {
        if (nameField.getText().isEmpty() || ageField.getText().isEmpty() || ageMonthField.getText().isEmpty() || ageDayField.getText().isEmpty()) {
            statusLabel.setText("Please fill required fields (Name, Age).");
            statusLabel.setStyle("-fx-text-fill: red;");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String rawInput = patientIdCombo.getEditor().getText().trim();
                String pId = rawInput;
                if (rawInput.contains(" - ")) {
                    pId = rawInput.split(" - ")[0].trim();
                }

                // Check if patient already exists by entered ID
                String checkSql = "SELECT patient_id FROM patients WHERE patient_id = ?";
                PreparedStatement cpstmt = conn.prepareStatement(checkSql);
                cpstmt.setString(1, pId);
                ResultSet rs = cpstmt.executeQuery();

                String gender = genderCombo.getValue() != null ? genderCombo.getValue() : "N/A";

                if (rs.next()) {
                    // Update existing record with latest demographic info
                    String updateSql = "UPDATE patients SET name=?, age=?, age_months=?, age_days=?, gender=?, phone=?, whatsapp=?, address=?, referred_doctor=?, title=?, staff_id=? WHERE patient_id=?";
                    PreparedStatement upstmt = conn.prepareStatement(updateSql);
                    upstmt.setString(1, nameField.getText());
                    upstmt.setInt(2, Integer.parseInt(ageField.getText()));
                    upstmt.setInt(3, Integer.parseInt(ageMonthField.getText()));
                    upstmt.setInt(4, Integer.parseInt(ageDayField.getText()));
                    upstmt.setString(5, gender);
                    upstmt.setString(6, phoneField.getText());
                    upstmt.setString(7, whatsappField.getText());
                    upstmt.setString(8, addressArea.getText());
                    upstmt.setString(9, referredDoctorField.getText());
                    upstmt.setString(10, titleCombo.getValue());
                    upstmt.setString(11, com.lab.lms.services.SessionContext.getStaffId());
                    upstmt.setString(12, pId);
                    upstmt.executeUpdate();
                } else {
                    String customTimestamp = regDatePicker.getValue().toString() + " " + regTimeField.getText();
                    String sql = "INSERT INTO patients (patient_id, name, age, age_months, age_days, gender, phone, whatsapp, address, referred_doctor, registration_date, title, staff_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, pId);
                    pstmt.setString(2, nameField.getText());
                    pstmt.setInt(3, Integer.parseInt(ageField.getText()));
                    pstmt.setInt(4, Integer.parseInt(ageMonthField.getText()));
                    pstmt.setInt(5, Integer.parseInt(ageDayField.getText()));
                    pstmt.setString(6, gender);
                    pstmt.setString(7, phoneField.getText());
                    pstmt.setString(8, whatsappField.getText());
                    pstmt.setString(9, addressArea.getText());
                    pstmt.setString(10, referredDoctorField.getText());
                    pstmt.setString(11, customTimestamp);
                    pstmt.setString(12, titleCombo.getValue());
                    pstmt.setString(13, com.lab.lms.services.SessionContext.getStaffId());
                    pstmt.executeUpdate();
                }

                conn.commit();
                
                // Update local doctor cache for autocomplete
                String currentDoc = referredDoctorField.getText().trim();
                if (!currentDoc.isEmpty() && !doctorNames.contains(currentDoc)) {
                    doctorNames.add(currentDoc);
                    FXCollections.sort(doctorNames);
                }

                com.lab.lms.services.SessionContext.setCurrentPatientId(pId);
                com.lab.lms.services.SessionContext.setCurrentSampleId(null);
                com.lab.lms.services.SessionContext.setSelectedTests(new java.util.ArrayList<>(selectedTests));
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    public void handleClear() {
        com.lab.lms.services.SessionContext.setCurrentPatientId(null);
        com.lab.lms.services.SessionContext.setCurrentSampleId(null);
        nameField.clear();
        titleCombo.getSelectionModel().clearSelection();
        ageField.setText("0");
        ageMonthField.setText("0");
        ageDayField.setText("0");
        genderCombo.getSelectionModel().clearSelection();
        phoneField.clear();
        whatsappField.clear();
        addressArea.clear();
        referredDoctorField.clear();
        selectedTests.clear();
        regDatePicker.setValue(java.time.LocalDate.now());
        regTimeField.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));

        generatePatientId();
    }

    @FXML
    private void handleConfirmEdit() {
        if (!nameField.getText().trim().isEmpty()) {
            if (savePatientData()) {
                com.lab.lms.services.SessionContext.setEditProfileMode(false);
                com.lab.lms.services.NavigationService.switchView("/fxml/profile.fxml");
            }
        } else {
            statusLabel.setText("Patient name is required to confirm edits.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleNext() {
        // Ensure tests are passed to context
        com.lab.lms.services.SessionContext.setSelectedTests(new java.util.ArrayList<>(selectedTests));

        if (!nameField.getText().trim().isEmpty()) {
            if (savePatientData()) {
                com.lab.lms.services.NavigationService.switchView("/fxml/billing.fxml");
            }
        } else {
            com.lab.lms.services.NavigationService.switchView("/fxml/billing.fxml");
        }
    }
}

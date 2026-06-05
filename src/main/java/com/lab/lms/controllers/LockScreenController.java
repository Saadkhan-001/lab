package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.services.ReportGenerator;
import com.lab.lms.services.SubscriptionService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.File;
import java.awt.Desktop;
import java.sql.*;
import java.util.*;

public class LockScreenController {

    @FXML
    private Label systemIdLabel;
    @FXML
    private TextField renewalKeyField;
    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        systemIdLabel.setText(DatabaseManager.getSetting("system_id", "NOT_FOUND"));
    }

    @FXML
    private void handleRenew() {
        String key = renewalKeyField.getText().trim();
        if (key.isEmpty()) {
            errorLabel.setText("Please enter a valid renewal key.");
            return;
        }

        if (SubscriptionService.validateAndRenew(key)) {
            Alert success = new Alert(Alert.AlertType.INFORMATION,
                    "System Reactivated. Clinical operations resumed. System will now restart to verify license status.");
            success.show();

            // Programmatic Restart: Reload the surveillance cycle via centralized restart
            javafx.application.Platform.runLater(() -> {
                com.lab.lms.Main.restart();
            });
        } else {
            errorLabel.setText("Activation Failed: Key mismatch or invalid protocol.");
        }
    }

    @FXML
    private void handlePrintBlank() {
        try {
            String path = ReportGenerator.generateEmptyTemplate();
            if (path != null) {
                Desktop.getDesktop().open(new File(path));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrintExisting() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Advanced Report Retrieval");
        dialog.setHeaderText("Identify Patient/Sample for Emergency Retrieval");

        ButtonType selectButtonType = new ButtonType("RETRIEVE & PRINT", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        VBox container = new VBox(10);
        container.setPrefWidth(400);
        container.setStyle("-fx-padding: 10;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name, MR Number, or Sample ID...");
        searchField.setStyle("-fx-height: 40; -fx-font-size: 13;");

        ListView<String> resultsList = new ListView<>();
        resultsList.setPrefHeight(250);

        // Load recent 20 patients initially
        loadRetrievalData(resultsList, "");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadRetrievalData(resultsList, newVal);
        });

        container.getChildren().addAll(new Label("SEARCH REGISTRY:"), searchField,
                new Label("RECENT CLINICAL RECORDS:"), resultsList);
        dialog.getDialogPane().setContent(container);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                String selection = resultsList.getSelectionModel().getSelectedItem();
                if (selection != null && selection.contains("|")) {
                    String[] parts = selection.split("\\|");
                    if (parts.length >= 2) {
                        String[] idPair = parts[1].split(":");
                        if (idPair.length >= 2) {
                            return idPair[1].trim(); 
                        }
                    }
                }
            }
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::fetchAndPrintReport);
    }

    private void loadRetrievalData(ListView<String> list, String query) {
        List<String> results = new ArrayList<>();
        String sql;
        if (query.isEmpty()) {
            sql = "SELECT p.name, p.patient_id, s.sample_id FROM patients p JOIN samples s ON p.patient_id = s.patient_id GROUP BY p.patient_id ORDER BY s.collection_date DESC LIMIT 20";
        } else {
            sql = "SELECT p.name, p.patient_id, s.sample_id FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE p.name LIKE ? OR p.patient_id LIKE ? OR s.sample_id LIKE ? GROUP BY p.patient_id ORDER BY s.collection_date DESC LIMIT 50";
        }

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!query.isEmpty()) {
                String wild = "%" + query + "%";
                pstmt.setString(1, wild);
                pstmt.setString(2, wild);
                pstmt.setString(3, wild);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name") + " | ID: " + rs.getString("patient_id") + " | Sample: "
                        + rs.getString("sample_id"));
            }

            if (results.isEmpty()) {
                results.add("No matching clinical records found.");
            }
            list.getItems().setAll(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fetchAndPrintReport(String id) {
        try (Connection conn = DatabaseManager.getConnection()) {
                // Find patient/sample info even when locked
                String sql = "SELECT p.name, p.patient_id, p.age, p.age_months, p.age_days, p.gender, p.referred_doctor, s.collection_date, p.whatsapp, p.address, s.sample_id "
                        +
                        "FROM patients p JOIN samples s ON p.patient_id = s.patient_id " +
                        "WHERE p.patient_id = ? OR s.sample_id = ? ORDER BY s.collection_date DESC LIMIT 1";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, id);
                pstmt.setString(2, id);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String sid = rs.getString("sample_id");
                    int y = rs.getInt("age");
                    int m = rs.getInt("age_months");
                    int d = rs.getInt("age_days");
                    String ageStr = y + "y";
                    if (m > 0 || d > 0) ageStr += " " + m + "m " + d + "d";
                    // Fetch results
                    String resSql = "SELECT tp.name, r.value, tp.unit, tp.min_range, tp.max_range, r.is_abnormal, r.comment, t.name as test_name, t.id as test_id, t.notes, t.is_special, t.is_microscopic, t.is_culture, t.specimen "
                            +
                            "FROM results r JOIN test_parameters tp ON r.parameter_id = tp.id " +
                            "JOIN tests t ON tp.test_id = t.id " +
                            "WHERE r.sample_id = ?";
                    PreparedStatement rsPstmt = conn.prepareStatement(resSql);
                    rsPstmt.setString(1, sid);
                    ResultSet resRs = rsPstmt.executeQuery();

                    List<ReportGenerator.TestData> allTestData = new ArrayList<>();
                    Map<String, List<Map<String, String>>> testGroups = new LinkedHashMap<>();
                    Map<String, Integer> testIdMap = new HashMap<>(); // Store test IDs
                    Map<String, String> testNotesMap = new HashMap<>();
                    Map<String, String> testCommentMap = new HashMap<>();
                    Map<String, Integer> testSpecialMap = new HashMap<>();
                    Map<String, Integer> testMicroscopicMap = new HashMap<>();
                    Map<String, Integer> testCultureMap = new HashMap<>();
                    Map<String, String> testSpecimenMap = new HashMap<>();

                    while (resRs.next()) {
                        String testName = resRs.getString("test_name");
                        int tIdValue = resRs.getInt("test_id");
                        Map<String, String> map = new HashMap<>();
                        map.put("name", resRs.getString("name"));
                        map.put("value", resRs.getString("value"));
                        map.put("unit", resRs.getString("unit"));
                        String min = resRs.getString("min_range");
                        String max = resRs.getString("max_range");
                        String range = (min == null || min.isEmpty()) ? (max == null ? "" : max) : (max == null || max.isEmpty() ? min : min + " - " + max);
                        map.put("range", range);
                        map.put("status", resRs.getInt("is_abnormal") == 1 ? "ABNORMAL" : "NORMAL");

                        testGroups.computeIfAbsent(testName, k -> new ArrayList<>()).add(map);
                        testNotesMap.put(testName, resRs.getString("notes"));
                        testIdMap.put(testName, tIdValue);
                        
                        // Store other meta
                        if (!testCommentMap.containsKey(testName)) {
                            testCommentMap.put(testName, resRs.getString("comment"));
                            testSpecialMap.put(testName, resRs.getInt("is_special"));
                            testMicroscopicMap.put(testName, resRs.getInt("is_microscopic"));
                            testCultureMap.put(testName, resRs.getInt("is_culture"));
                            testSpecimenMap.put(testName, resRs.getString("specimen"));
                        }
                    }
                    
                    for (String tName : testGroups.keySet()) {
                        String catFetch = "GENERAL";
                        try (Connection catConn = DatabaseManager.getConnection()) {
                            PreparedStatement catPstmt = catConn.prepareStatement("SELECT category FROM tests WHERE name = ?");
                            catPstmt.setString(1, tName);
                            ResultSet catRs = catPstmt.executeQuery();
                            if (catRs.next()) catFetch = catRs.getString("category");
                        } catch (Exception e) {}

                        allTestData.add(new ReportGenerator.TestData(tName, testIdMap.getOrDefault(tName, 0), catFetch, testGroups.get(tName), 
                            testNotesMap.get(tName), testSpecialMap.getOrDefault(tName, 0), testMicroscopicMap.getOrDefault(tName, 0), testCultureMap.getOrDefault(tName, 0), testSpecimenMap.get(tName), testCommentMap.get(tName), "", new ArrayList<>(), sid));
                    }

                if (!allTestData.isEmpty()) {
                    boolean includeHeader = Boolean.parseBoolean(DatabaseManager.getSetting("print_header_footer", "true"));
                    String path = ReportGenerator.generateMultiTestReport(
                            rs.getString("name"), rs.getString("patient_id"),
                            ageStr, rs.getString("gender"),
                            rs.getString("referred_doctor"), rs.getString("collection_date"),
                            rs.getString("whatsapp"), rs.getString("address"), allTestData, includeHeader, true);

                    if (path != null) {
                        Desktop.getDesktop().open(new File(path));
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "No clinical data found for this identifier.").show();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "No record found with provided Identity.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Retrieval Failure: " + e.getMessage()).show();
        }
    }
}

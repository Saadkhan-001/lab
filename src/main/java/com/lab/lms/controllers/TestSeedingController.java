package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class TestSeedingController {

    @FXML private ListView<String> syncLogList;
    @FXML private ProgressBar syncProgressBar;
    @FXML private Label statusLabel;
    @FXML private Label percentageLabel;
    @FXML private Label filePathLabel;
    @FXML private Button btnStartSync;

    private Task<Void> syncTask;
    private File selectedSeedFile;

    @FXML
    public void initialize() {
        syncProgressBar.setProgress(0.0);
        statusLabel.setText("Ready to synchronize...");
        percentageLabel.setText("0%");
    }

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Clinical Seed File (JSON)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(btnStartSync.getScene().getWindow());
        if (file != null) {
            selectedSeedFile = file;
            filePathLabel.setText("Selected: " + file.getName());
            filePathLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleStartSync() {
        btnStartSync.setDisable(true);
        syncLogList.getItems().clear();
        
        syncTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Reading seed data...");
                updateProgress(0, 1);

                InputStream is = null;
                if (selectedSeedFile != null && selectedSeedFile.exists()) {
                    is = new FileInputStream(selectedSeedFile);
                } else {
                    is = getClass().getResourceAsStream("/tests_seed.json");
                }

                if (is == null) {
                    throw new Exception("Seed file not found.");
                }

                String json;
                try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    json = s.useDelimiter("\\A").next();
                }

                JSONArray tests = new JSONArray(json);
                int total = tests.length();
                updateMessage("Synchronizing " + total + " clinical protocols...");

                try (Connection conn = DatabaseManager.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        for (int i = 0; i < total; i++) {
                            if (isCancelled()) break;

                            JSONObject testObj = tests.getJSONObject(i);
                            String name = testObj.getString("name");
                            String code = testObj.optString("code", "");
                            
                            updateProgress(i + 1, total);
                            final int currentIdx = i + 1;
                            Platform.runLater(() -> {
                                syncLogList.getItems().add(0, "[SYNC] Processed: " + name + " (" + code + ")");
                                statusLabel.setText("Processing " + currentIdx + " of " + total + "...");
                                percentageLabel.setText((int)((double)currentIdx / total * 100) + "%");
                            });

                            // Synchronize Test
                            syncTest(conn, testObj);

                            // Micro-sleep to prevent UI hang and allow visual feedback
                            if (i % 10 == 0) conn.commit();
                        }
                        conn.commit();
                    } catch (Exception e) {
                        conn.rollback();
                        throw e;
                    }
                }
                return null;
            }
        };

        syncTask.setOnSucceeded(e -> {
            statusLabel.setText("Synchronization Successful!");
            percentageLabel.setText("100%");
            btnStartSync.setText("CLOSE");
            btnStartSync.setDisable(false);
            btnStartSync.setOnAction(event -> ((Stage) btnStartSync.getScene().getWindow()).close());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sync Complete");
            alert.setHeaderText("Global Clinical Engine Synchronized");
            alert.setContentText("The laboratory repository has been successfully updated with the latest clinical protocols.");
            alert.show();
        });

        syncTask.setOnFailed(e -> {
            Throwable ex = syncTask.getException();
            ex.printStackTrace();
            statusLabel.setText("Sync Failed: " + ex.getMessage());
            btnStartSync.setDisable(false);
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Sync Error");
            alert.setHeaderText("Critical Synchronization Failure");
            alert.setContentText(ex.getMessage());
            alert.show();
        });

        syncProgressBar.progressProperty().bind(syncTask.progressProperty());
        new Thread(syncTask).start();
    }

    private void syncTest(Connection conn, JSONObject testObj) throws Exception {
        String code = testObj.optString("code", "");
        String name = testObj.getString("name");
        String category = testObj.optString("category", "General");
        
        int testId = -1;
        // Check if test exists
        String checkSql = "SELECT id FROM tests WHERE numeric_code = ? OR name = ?";
        try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
            checkPstmt.setString(1, code);
            checkPstmt.setString(2, name);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next()) {
                testId = rs.getInt("id");
            }
        }

        if (testId == -1) {
            // INSERT
            String insertSql = "INSERT INTO tests (name, numeric_code, alpha_code, category, price, result_time, specimen, is_special, is_microscopic, is_culture, protocol_class) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement inPstmt = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                inPstmt.setString(1, name);
                inPstmt.setString(2, code);
                inPstmt.setString(3, testObj.optString("alpha_code", ""));
                inPstmt.setString(4, category);
                inPstmt.setDouble(5, testObj.optDouble("price", 0.0));
                inPstmt.setString(6, testObj.optString("result_time", ""));
                inPstmt.setString(7, testObj.optString("specimen", "Blood"));
                inPstmt.setInt(8, testObj.optInt("is_special", 0));
                inPstmt.setInt(9, testObj.optInt("is_microscopic", 0));
                inPstmt.setInt(10, testObj.optInt("is_culture", 0));
                inPstmt.setString(11, testObj.optString("protocol_class", "ROUTINE"));
                inPstmt.executeUpdate();
                try (ResultSet grs = inPstmt.getGeneratedKeys()) {
                    if (grs.next()) testId = grs.getInt(1);
                }
            }
        } else {
            // UPDATE
            String updateSql = "UPDATE tests SET name = ?, numeric_code = ?, alpha_code = ?, category = ?, price = ?, is_special = ?, is_microscopic = ?, is_culture = ?, specimen = ?, protocol_class = ? WHERE id = ?";
            try (PreparedStatement upPstmt = conn.prepareStatement(updateSql)) {
                upPstmt.setString(1, name);
                upPstmt.setString(2, code);
                upPstmt.setString(3, testObj.optString("alpha_code", ""));
                upPstmt.setString(4, category);
                upPstmt.setDouble(5, testObj.optDouble("price", 0.0));
                upPstmt.setInt(6, testObj.optInt("is_special", 0));
                upPstmt.setInt(7, testObj.optInt("is_microscopic", 0));
                upPstmt.setInt(8, testObj.optInt("is_culture", 0));
                upPstmt.setString(9, testObj.optString("specimen", "Blood"));
                upPstmt.setString(10, testObj.optString("protocol_class", "ROUTINE"));
                upPstmt.setInt(11, testId);
                upPstmt.executeUpdate();
            }
        }

        if (testId != -1) {
            syncParameters(conn, testId, testObj.getJSONArray("parameters"));
        }
    }

    private void syncParameters(Connection conn, int testId, JSONArray params) throws Exception {
        // High-Integrity Upsert Strategy: Match by Name within TestId to preserve existing IDs
        // This prevents orphaning results that were linked to these parameters.
        
        String checkParamSql = "SELECT id FROM test_parameters WHERE test_id = ? AND name = ?";
        String updateParamSql = "UPDATE test_parameters SET unit = ?, category = ?, min_range = ?, max_range = ?, min_range_male = ?, max_range_male = ?, min_range_female = ?, max_range_female = ?, min_range_kids = ?, max_range_kids = ?, print_order = ? WHERE id = ?";
        String insertParamSql = "INSERT INTO test_parameters (test_id, name, unit, category, min_range, max_range, min_range_male, max_range_male, min_range_female, max_range_female, min_range_kids, max_range_kids, print_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement checkPstmt = conn.prepareStatement(checkParamSql);
             PreparedStatement upPstmt = conn.prepareStatement(updateParamSql);
             PreparedStatement insPstmt = conn.prepareStatement(insertParamSql)) {
            
            for (int i = 0; i < params.length(); i++) {
                JSONObject p = params.getJSONObject(i);
                String pName = p.getString("name");
                
                int existingParamId = -1;
                checkPstmt.setInt(1, testId);
                checkPstmt.setString(2, pName);
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next()) {
                        existingParamId = rs.getInt("id");
                    }
                }

                if (existingParamId != -1) {
                    // UPDATE
                    upPstmt.setString(1, p.optString("unit", "-"));
                    upPstmt.setString(2, p.optString("category", ""));
                    upPstmt.setString(3, p.optString("min_range", ""));
                    upPstmt.setString(4, p.optString("max_range", ""));
                    upPstmt.setString(5, p.optString("min_range_male", ""));
                    upPstmt.setString(6, p.optString("max_range_male", ""));
                    upPstmt.setString(7, p.optString("min_range_female", ""));
                    upPstmt.setString(8, p.optString("max_range_female", ""));
                    upPstmt.setString(9, p.optString("min_range_kids", ""));
                    upPstmt.setString(10, p.optString("max_range_kids", ""));
                    upPstmt.setInt(11, p.optInt("print_order", i + 1));
                    upPstmt.setInt(12, existingParamId);
                    upPstmt.executeUpdate();
                } else {
                    // INSERT
                    insPstmt.setInt(1, testId);
                    insPstmt.setString(2, pName);
                    insPstmt.setString(3, p.optString("unit", "-"));
                    insPstmt.setString(4, p.optString("category", ""));
                    insPstmt.setString(5, p.optString("min_range", ""));
                    insPstmt.setString(6, p.optString("max_range", ""));
                    insPstmt.setString(7, p.optString("min_range_male", ""));
                    insPstmt.setString(8, p.optString("max_range_male", ""));
                    insPstmt.setString(9, p.optString("min_range_female", ""));
                    insPstmt.setString(10, p.optString("max_range_female", ""));
                    insPstmt.setString(11, p.optString("min_range_kids", ""));
                    insPstmt.setString(12, p.optString("max_range_kids", ""));
                    insPstmt.setInt(13, p.optInt("print_order", i + 1));
                    insPstmt.executeUpdate();
                }
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (syncTask != null && syncTask.isRunning()) {
            syncTask.cancel();
        }
        ((Stage) btnStartSync.getScene().getWindow()).close();
    }
}

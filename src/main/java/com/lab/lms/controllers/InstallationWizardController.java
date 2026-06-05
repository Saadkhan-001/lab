package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.UUID;
import org.json.JSONObject;

public class InstallationWizardController {

    @FXML
    private StackPane contentStack;
    @FXML
    private VBox step1, step2, step3;
    @FXML
    private Button btnBack, btnNext;
    @FXML
    private Label errorLabel;

    // Step 1
    @FXML
    private TextField systemIdField;
    @FXML
    private TextArea licenseKeyArea;

    // Step 2
    @FXML
    private TextField labNameField, labAddressField, labContactField;
    @FXML
    private ImageView logoPreview;
    private String selectedLogoPath = "";

    // Step 3
    @FXML
    private TextField adminUserField;
    @FXML
    private PasswordField adminPassField, confirmPassField;

    private int currentStep = 1;
    private String generatedSid = "";

    @FXML
    public void initialize() {
        // Generate or Fetch System ID
        generatedSid = DatabaseManager.getSetting("system_id", "");
        if (generatedSid.isEmpty()) {
            generatedSid = "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            DatabaseManager.saveSetting("system_id", generatedSid);
        }
        systemIdField.setText(generatedSid);

        showStep(1);
    }

    private void showStep(int step) {
        step1.setVisible(step == 1);
        step2.setVisible(step == 2);
        step3.setVisible(step == 3);

        btnBack.setVisible(step > 1);
        btnNext.setText(step == 3 ? "FINALIZE DEPLOYMENT" : "PROCEED & VALIDATE");
        currentStep = step;
        errorLabel.setText("");
    }

    @FXML
    private void handleNext() {
        if (validateStep(currentStep)) {
            if (currentStep < 3) {
                showStep(currentStep + 1);
            } else {
                finalizeSetup();
            }
        }
    }

    @FXML
    private void handleBack() {
        showStep(currentStep - 1);
    }

    private boolean validateStep(int step) {
        switch (step) {
            case 1:
                String key = licenseKeyArea.getText().trim();
                if (key.isEmpty()) {
                    errorLabel.setText("Activation key is required.");
                    return false;
                }
                try {
                    // Normalize the key (remove any whitespace/newlines from copy-paste)
                    String cleanKey = key.replaceAll("\\s", "");
                    String decoded = new String(Base64.getDecoder().decode(cleanKey));

                    System.out.println("Validating License Key... Format Detected: "
                            + (decoded.startsWith("{") ? "JSON" : "LEGACY"));

                    if (decoded.startsWith("{")) {
                        // High-Fidelity JSON Protocol
                        JSONObject json = new JSONObject(decoded);
                        String sid = json.optString("sid", "PENDING");

                        if (!sid.equals(generatedSid) && !sid.equals("PENDING")) {
                            errorLabel.setText("License mismatch: Identity does not match this system.");
                            return false;
                        }
                        return true;
                    } else {
                        // Fallback Legacy Protocol (SID|EXPIRY)
                        String[] parts = decoded.split("\\|");
                        if (parts.length >= 2) {
                            String sid = parts[0];
                            if (!sid.equals(generatedSid) && !sid.equals("PENDING")) {
                                errorLabel.setText("Legacy License mismatch: System ID mismatch.");
                                return false;
                            }
                            return true;
                        }
                    }

                    errorLabel.setText("Operational Error: Unrecognized key signature.");
                    return false;
                } catch (Exception e) {
                    System.err.println("Installation Wizard Error: " + e.getMessage());
                    e.printStackTrace();
                    errorLabel.setText("Operational Error: Invalid or corrupted activation key.");
                    return false;
                }
            case 2:
                if (labNameField.getText().isEmpty() || labAddressField.getText().isEmpty()
                        || labContactField.getText().isEmpty()) {
                    errorLabel.setText("All branding fields marked with * are mandatory.");
                    return false;
                }
                return true;
            case 3:
                if (adminUserField.getText().isEmpty() || adminPassField.getText().isEmpty()) {
                    errorLabel.setText("Administrative credentials cannot be empty.");
                    return false;
                }
                if (!adminPassField.getText().equals(confirmPassField.getText())) {
                    errorLabel.setText("Credential Error: Passwords do not match.");
                    return false;
                }
                return true;
        }
        return false;
    }

    @FXML
    private void handleUploadLogo() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(contentStack.getScene().getWindow());
        if (file != null) {
            selectedLogoPath = file.getAbsolutePath();
            logoPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    private void finalizeSetup() {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Clinical Data Preservation (Moved from destructive purge)
                // We no longer delete patient/test data during re-deployment to prevent
                // accidental loss
                // Only update branding and credentials as needed

                // 2. Save Core Branding & Identity Settings within THIS transaction
                PreparedStatement setPstmt = conn
                        .prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)");

                String[][] settings = {
                        { "lab_name", labNameField.getText() },
                        { "lab_address", labAddressField.getText() },
                        { "lab_contact", labContactField.getText() },
                        { "lab_logo", selectedLogoPath },
                        { "is_installed", "true" }
                };

                for (String[] s : settings) {
                    setPstmt.setString(1, s[0]);
                    setPstmt.setString(2, s[1]);
                    setPstmt.executeUpdate();
                }

                // Handle License & Expiry
                String key = licenseKeyArea.getText().trim().replaceAll("\\s", "");
                String decoded = new String(Base64.getDecoder().decode(key));
                String expiry = "2099-12-31";
                if (decoded.startsWith("{")) {
                    expiry = new JSONObject(decoded).optString("expiry", "2099-12-31");
                } else {
                    String[] parts = decoded.split("\\|");
                    if (parts.length >= 2)
                        expiry = parts[1];
                }

                setPstmt.setString(1, "license_key");
                setPstmt.setString(2, key);
                setPstmt.executeUpdate();

                setPstmt.setString(1, "expiry_date");
                setPstmt.setString(2, expiry);
                setPstmt.executeUpdate();

                // 3. Seat New Admin Credentials
                PreparedStatement userPstmt = conn
                        .prepareStatement("INSERT OR REPLACE INTO users (username, password, role) VALUES (?, ?, 'ADMIN')");
                userPstmt.setString(1, adminUserField.getText());
                userPstmt.setString(2, adminPassField.getText());
                userPstmt.executeUpdate();
                
                // 4. Clinical Repository Injection: Ensure all 700+ tests are installed on new hardware
                System.out.println("Installation Wizard: Syncing medical master data...");
                DatabaseManager.forceSeedDatabase(conn);

                conn.commit();

                Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "Clinical deployment successful. System will now restart to finalize security.");
                success.setTitle("Clinical Deployment Complete");
                success.show();

                // Programmatic Restart: Reload the surveillance cycle via centralized restart
                javafx.application.Platform.runLater(() -> {
                    com.lab.lms.Main.restart();
                });

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Deployment Failure: " + e.getMessage());
        }
    }
}

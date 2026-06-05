package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.services.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import java.io.File;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private Button eyeBtn;
    @FXML
    private Label errorLabel;
    @FXML
    private VBox ipContainer;
    @FXML
    private TextField ipField;
    @FXML
    private Label localIpLabel;
    @FXML
    private Hyperlink networkSettingsLink;
    @FXML
    private Button loginBtn;

    @FXML
    private void handleLogin(ActionEvent event) {
        // Refresh Environment before authentication attempt (prevents stale network links)
        DatabaseManager.refreshUrl();
        
        String usernameInput = usernameField.getText();
        String passwordInput = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

        if (usernameInput == null || usernameInput.trim().isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        String username = usernameInput.trim();
        String password = passwordInput;

        try (Connection conn = DatabaseManager.getConnection()) {
            // Match against BOTH username (Full Name) OR staff_id (e.g. STF-562)
            String sql = "SELECT role, staff_id FROM users WHERE (lower(username) = lower(?) OR lower(staff_id) = lower(?)) AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                String staffId = rs.getString("staff_id");
                
                // Initialize Session
                SessionContext.setUserRole(role);
                SessionContext.setUsername(username);
                SessionContext.setStaffId(staffId);
                
                if ("ADMIN".equals(role)) {
                    SessionContext.setPermissions("dashboard,registration,processing,billing,inventory,authorization,configuration");
                } else if (staffId != null) {
                    // Fetch permissions from staff table via staffId
                    String staffSql = "SELECT permissions FROM staff WHERE staff_id = ?";
                    try (PreparedStatement sPstmt = conn.prepareStatement(staffSql)) {
                        sPstmt.setString(1, staffId);
                        ResultSet sRs = sPstmt.executeQuery();
                        if (sRs.next()) {
                            SessionContext.setPermissions(sRs.getString("permissions"));
                        }
                    }
                } else {
                    // Technician with no staff_id link (legacy or special case)
                    // We can either block or give default technician perms
                    SessionContext.setPermissions("dashboard,registration,processing");
                }

                // Load Dashboard
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                    Parent root = loader.load();

                    // Load Dashboard with Adaptive Sizing
                    javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                    double width = Math.min(1250, screenBounds.getWidth());
                    double height = Math.min(660, screenBounds.getHeight() - 50);

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.setScene(new Scene(root, width, height));
                    stage.centerOnScreen();
                    stage.setResizable(true);
                    stage.setMaximized(true); // Ensure perfect look by filling screen
                    stage.setTitle("Laboratory Management System - " + role);
                } catch (Throwable t) {
                    Throwable cause = t;
                    while (cause.getCause() != null) cause = cause.getCause();
                    errorLabel.setText("System Fault: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                    t.printStackTrace();
                }
            } else {
                errorLabel.setText("Security Access Denied: Invalid credentials.");
            }
        } catch (SQLException e) {
            errorLabel.setText("Database Fault: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    public void initialize() {
        System.out.println("[TRACE] LoginController: Initializing...");
        // Mode-Driven Navigation Tools
        try {
            boolean isMultiDevice = "true".equalsIgnoreCase(DatabaseManager.getSetting("multi_device_mode", "false"));
            System.out.println("[TRACE] LoginController: multi_device_mode=" + isMultiDevice);
            networkSettingsLink.setVisible(isMultiDevice);
            networkSettingsLink.setManaged(isMultiDevice);

            // Quick Authentication (Enter Key Access)
            javafx.event.EventHandler<javafx.scene.input.KeyEvent> enterHandler = event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleLogin(null);
                }
            };
            usernameField.setOnKeyPressed(enterHandler);
            passwordField.setOnKeyPressed(enterHandler);
            passwordTextField.setOnKeyPressed(enterHandler);

            if (isMultiDevice) {
                String savedIp = DatabaseManager.getSetting("server_ip", "");
                ipField.setText(savedIp);
                System.out.println("[TRACE] LoginController: server_ip=" + savedIp);
                
                if (!savedIp.isEmpty()) {
                    handleConnect(null);
                } else {
                    loginBtn.setDisable(false);
                    loginBtn.setText("AUTHENTICATE (LOCAL)");
                }
            }
            System.out.println("[TRACE] LoginController: Initialization Complete.");
        } catch (Throwable t) {
            System.err.println("[TRACE] LoginController: CRASH during initialize");
            t.printStackTrace();
        }
    }

    @FXML
    private void handleToggleNetworkUI(ActionEvent event) {
        boolean isNowVisible = !ipContainer.isVisible();
        ipContainer.setVisible(isNowVisible);
        ipContainer.setManaged(isNowVisible);
        
        if (isNowVisible) {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                localIpLabel.setText("WORKSTATION IP: " + ip);
            } catch (java.net.UnknownHostException e) {
                localIpLabel.setText("WORKSTATION IP: UNKNOWN");
            }
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Transition to Visible Password
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText().length());
        } else {
            // Return to Masked Password
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        String ip = ipField.getText();
        if (ip == null || ip.trim().isEmpty()) {
            DatabaseManager.saveSetting("server_ip", "");
            DatabaseManager.refreshUrl();
            errorLabel.setText("System Workspace: Standard Local Mode Enabled.");
            errorLabel.setStyle("-fx-text-fill: #961111;");
            loginBtn.setDisable(false);
            loginBtn.setText("AUTHENTICATE (LOCAL)");
            return;
        }

        errorLabel.setText("Linking to Network Repository...");
        errorLabel.setStyle("-fx-text-fill: #961111;");
        
        // Save proposed configuration to trigger URL refresh
        DatabaseManager.saveSetting("server_ip", ip.trim());
        DatabaseManager.refreshUrl();

        // Validate Connection Integrity
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ping clinical repo
            try (Statement stmt = conn.createStatement()) {
                stmt.executeQuery("SELECT 1").close();
            }
            errorLabel.setText("Synchronized with Network Repository.");
            errorLabel.setStyle("-fx-text-fill: #961111;");
            loginBtn.setDisable(false);
            loginBtn.setText("AUTHENTICATE (NETWORK MODE)");
        } catch (SQLException e) {
            errorLabel.setText("Clinical Link Fault: Remote server not responding.");
            errorLabel.setStyle("-fx-text-fill: #B71C1C;");
            loginBtn.setDisable(true);
            loginBtn.setText("SYNC FAULT");
        }
    }
}

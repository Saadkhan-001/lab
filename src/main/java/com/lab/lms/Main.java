package com.lab.lms;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.services.SubscriptionService;
import com.lab.lms.services.TrialService;
import com.lab.lms.util.UIHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;

public class Main extends Application {
    private static Main instance;
    private static Stage primaryStage;
    public static final String APP_VERSION = "7.0.2";
    public static javafx.application.HostServices hostServicesInstance;

    @Override
    public void init() {
        if (getParameters().getRaw().contains("--demo-mode")) {
            TrialService.setDemo(true);
        }
        if (getParameters().getRaw().contains("--recover-mode")) {
            TrialService.setRecover(true);
        }
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        primaryStage = stage;
        hostServicesInstance = getHostServices();
        try {
            DatabaseManager.initializeDatabase();
            SubscriptionService.Status status = SubscriptionService.getSystemStatus();
            
            if (status == SubscriptionService.Status.UNINSTALLED && !TrialService.isDemo()) {
                loadScene(stage, "/fxml/installation_wizard.fxml", "System Deployment Wizard", 800, 600);
                return;
            } else if (status == SubscriptionService.Status.EXPIRED && !TrialService.isDemo()) {
                loadScene(stage, "/fxml/lock_screen.fxml", "System Locked - License Expired", 800, 600);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            stage.setTitle("MSF Digital Solutions - Laboratory Platform");
            UIHelper.setAppIcon(stage);
            stage.setScene(new Scene(root, 500, 380));
            applyGlobalShortcuts(stage.getScene(), stage);
            stage.setResizable(false);
            stage.centerOnScreen();
            
            stage.show();

            if (com.lab.lms.services.TrialService.isRecover()) {
                stage.setOnCloseRequest(e -> triggerSelfDestruct());
            }

            startBackgroundSurveillance();
        } catch (Throwable e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("The application failed to start.");
            alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage() + "\nCheck console for details.");
            alert.showAndWait();
        }
    }

    private void startBackgroundSurveillance() {
        // 1. Periodic Backup Service (10-second initial delay, then background)
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                com.lab.lms.services.BackupService.performBackup();
            } catch (Exception e) {}
        }).start();

        // 2. Continuous License Monitor (Checks every hour for "Automatic Locking")
        Thread licenseMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000); // 1 Hour check interval
                    System.out.println("[MONITOR] Heartbeat: Checking clinical license status...");
                    if (SubscriptionService.getSystemStatus() == SubscriptionService.Status.EXPIRED) {
                        System.err.println("[MONITOR] Critical: License expired. Triggering lock protocol.");
                        javafx.application.Platform.runLater(() -> {
                            loadScene(primaryStage, "/fxml/lock_screen.fxml", "System Locked - License Expired", 800, 600);
                        });
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("[MONITOR] Error during license surveillance: " + e.getMessage());
                }
            }
        });
        licenseMonitor.setDaemon(true);
        licenseMonitor.start();
    }

    public void loadScene(Stage stage, String fxml, String title, int width, int height) {
        try {
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double finalWidth = Math.min(width, screenBounds.getWidth());
            double finalHeight = Math.min(height, screenBounds.getHeight() - 40);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            stage.setTitle(title);
            UIHelper.setAppIcon(stage);
            stage.setScene(new Scene(root, finalWidth, finalHeight));
            applyGlobalShortcuts(stage.getScene(), stage);
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyGlobalShortcuts(Scene scene, Stage stage) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.I) {
                event.consume();
                Platform.runLater(() -> handleGlobalDatabaseImport(stage));
            }
        });
    }

    private void handleGlobalDatabaseImport(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Database Backup to Import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db", "*.sqlite"));
        File file = chooser.showOpenDialog(stage);
        
        if (file != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to import this database?\nAll current tests, patients, and organization settings will be replaced by the imported data.",
                ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Import Database Confirmation");
            
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                try {
                    String appDataDir = System.getProperty("user.home") + File.separator + ".lablms";
                    File targetDb = new File(appDataDir, "laboratory.db");
                    
                    // Physical file replacement
                    Files.copy(file.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Database imported successfully! The application will now restart.");
                    success.showAndWait();
                    
                    restart();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to import database: " + e.getMessage());
                    error.showAndWait();
                }
            }
        }
    }

    public static void restart() {
        if (instance != null && primaryStage != null) {
            instance.start(primaryStage);
        }
    }

    private void triggerSelfDestruct() {
        try {
            // Locate the application root directory (where unins000.exe lives)
            File jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File appDir = jarPath.getParentFile().getParentFile(); // Assuming structure: {app}/app/jar
            
            if (!new File(appDir, "unins000.exe").exists()) {
                // Secondary check for other directory layouts
                appDir = jarPath.getParentFile();
            }

            File destructBat = new File(System.getProperty("java.io.tmpdir"), "destruct_lms.bat");
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(destructBat)) {
                writer.println("@echo off");
                writer.println("title LMS Recovery Cleanup");
                writer.println("echo Waiting for application to close...");
                writer.println("timeout /t 4 /nobreak > nul");
                writer.println("cd /d \"" + appDir.getAbsolutePath() + "\"");
                writer.println("if exist \"unins000.exe\" (");
                writer.println("    echo Triggering silent uninstallation...");
                writer.println("    start \"\" \"unins000.exe\" /VERYSILENT /SUPPRESSMSGBOXES");
                writer.println(") else (");
                writer.println("    echo Manual cleanup mode...");
                writer.println("    taskkill /F /IM LaboratoryManagementSystem.exe /T 2>nul");
                writer.println("    timeout /t 2 /nobreak > nul");
                writer.println("    del /S /Q *.* > nul 2>&1");
                writer.println(")");
                writer.println("echo Cleanup complete.");
                writer.println("del \"%~f0\" & exit");
            }
            
            // Launch as a separate detached process
            Runtime.getRuntime().exec("cmd /c start /min \"\" \"" + destructBat.getAbsolutePath() + "\"");
        } catch (Exception e) {
            System.err.println("[RECOVERY] Self-destruct sequence failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

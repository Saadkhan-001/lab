package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.services.ReportGenerator;
import com.lab.lms.services.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.awt.Desktop;
import java.io.IOException;
import java.io.File;
import java.sql.*;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javafx.scene.control.ButtonBar.ButtonData;

public class DashboardController {

    private static Stage rateListStage;

    @FXML
    private StackPane contentArea;
    @FXML
    private VBox homeView;
    @FXML
    private VBox vboxNav;
    @FXML
    private VBox navHome;
    @FXML
    private VBox navRegistration;
    @FXML
    private VBox navDatabase;
    @FXML
    private VBox navSpecimen;
    @FXML
    private VBox navProcessing;
    @FXML
    private VBox navBilling;
    @FXML
    private VBox navHistory;
    @FXML
    private VBox navSettings;
    @FXML
    private javafx.scene.layout.StackPane headerInventory;
    @FXML
    private javafx.scene.layout.StackPane headerLicense;
    @FXML
    private VBox navDoctor;
    @FXML
    private Label lblTodayRegistered;
    @FXML
    private Label lblPendingTests;
    @FXML
    private Label lblRevenue;

    @FXML
    private javafx.scene.image.ImageView labLogoView;
    @FXML
    private Label headerLabName;
    @FXML
    private Label headerLabInfo;
    @FXML
    private Label lblClock;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblLicenseStatus;
    @FXML
    private Label lblTrialStatus;
    @FXML
    private javafx.scene.control.TextField searchPatientField;
    @FXML
    private javafx.scene.control.DatePicker filterStartDate;
    @FXML
    private javafx.scene.control.DatePicker filterEndDate;
    @FXML
    private javafx.scene.control.ComboBox<String> filterGender;
    @FXML
    private javafx.scene.control.ComboBox<String> filterDateRange;
    @FXML
    private javafx.scene.layout.HBox customDateBox;

    // Pagination variables for Recent Activity
    private int currentActivityPage = 0;
    private static final int ACTIVITY_PAGE_SIZE = 15;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnRegistration;
    @FXML
    private Button btnBilling;
    @FXML
    private Button btnSamples;
    @FXML
    private Button btnProcessing;
    @FXML
    private Button btnReview;
    @FXML
    private Button btnHistory;
    @FXML
    private Button btnSettings;

    @FXML
    private TableView<com.lab.lms.models.RecentActivity> recentTable;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentId;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentName;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentStatus;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentTime;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentPaid;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentPhone;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentDiscount;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, String> colRecentRemaining;
    @FXML
    private javafx.scene.control.TableColumn<com.lab.lms.models.RecentActivity, Void> colRecentAction;

    @FXML
    private Button toggleVisibilityBtn;
    private boolean isDataHidden = false;

    @FXML
    private void handleToggleVisibility() {
        isDataHidden = !isDataHidden;
        toggleVisibilityBtn.setText(isDataHidden ? "SHOW DATA" : "HIDE DATA");
        updateStats();
    }

    @FXML
    private void handlePrevActivity() {
        if (currentActivityPage > 0) {
            currentActivityPage--;
            updateStats();
        }
    }

    @FXML
    private void handleNextActivity() {
        currentActivityPage++;
        updateStats();
    }

    @FXML
    private void handleDateRangeChange() {
        if (filterDateRange == null)
            return;
        String selection = filterDateRange.getSelectionModel().getSelectedItem();
        boolean isCustom = "Date to Date".equals(selection);
        if (customDateBox != null) {
            customDateBox.setVisible(isCustom);
            customDateBox.setManaged(isCustom);
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        if ("Today".equals(selection)) {
            filterStartDate.setValue(today);
            filterEndDate.setValue(today);
            handleApplyFilters();
        } else if ("Yesterday".equals(selection)) {
            filterStartDate.setValue(today.minusDays(1));
            filterEndDate.setValue(today.minusDays(1));
            handleApplyFilters();
        } else if ("Last 7 Days".equals(selection)) {
            filterStartDate.setValue(today.minusDays(7));
            filterEndDate.setValue(today);
            handleApplyFilters();
        }
    }

    @FXML
    private void handleApplyFilters() {
        currentActivityPage = 0;
        updateStats();
    }

    private static DashboardController instance;

    public static DashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        
        // Wrap initialization in Platform.runLater to handle potential "FXML injection delays" 
        // reported on certain 32-bit environments. This ensures the loader finishes 
        // field injection before the logic attempts to access them.
        javafx.application.Platform.runLater(() -> {
            try {
                if (contentArea != null) {
                    com.lab.lms.services.NavigationService.setContentArea(contentArea);
                }

                if (colRecentId != null) colRecentId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("patientId"));
                if (colRecentName != null) colRecentName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
                if (colRecentStatus != null) colRecentStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
                if (colRecentTime != null) colRecentTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
                if (colRecentPaid != null) colRecentPaid.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("paid"));
                if (colRecentPhone != null) colRecentPhone.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));
                if (colRecentDiscount != null) colRecentDiscount.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("discount"));
                if (colRecentRemaining != null) colRecentRemaining.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("remaining"));
                
                // Status Jump
                if (colRecentStatus != null) {
                    colRecentStatus.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                setText(item);
                                setStyle("-fx-text-fill: #961111; -fx-font-weight: bold;");
                                setOnMouseClicked(e -> {
                                    if (e.getClickCount() == 1) {
                                        handleRecentRowDoubleClicked(getTableView().getItems().get(getIndex()));
                                    }
                                });
                            }
                        }
                    });
                }

                // Paid Inline Update
                if (colRecentPaid != null) {
                    colRecentPaid.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(item);
                                setOnMouseClicked(e -> {
                                    if (e.getClickCount() == 2) {
                                        handlePaidFieldUpdate(getTableView().getItems().get(getIndex()));
                                    }
                                });
                            }
                        }
                    });
                }

                // Quick Actions (Buttons)
                if (colRecentAction != null) {
                    colRecentAction.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
                        private final javafx.scene.control.Button btnReceipt = new javafx.scene.control.Button("RECEIPT");
                        private final javafx.scene.control.Button btnReport = new javafx.scene.control.Button("REPORT");
                        private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, btnReceipt, btnReport);
                        {
                            box.setPadding(new javafx.geometry.Insets(2, 0, 2, 0));
                            box.setAlignment(javafx.geometry.Pos.CENTER);
                            btnReceipt.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 3 8; -fx-cursor: hand;");
                            btnReport.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 3 8; -fx-cursor: hand;");
                            btnReceipt.setOnAction(e -> handleViewReceipt(getTableView().getItems().get(getIndex())));
                            btnReport.setOnAction(e -> handleViewReport(getTableView().getItems().get(getIndex())));
                        }
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) setGraphic(null);
                            else setGraphic(box);
                        }
                    });
                }

                if (filterGender != null) {
                    filterGender.setItems(
                            javafx.collections.FXCollections.observableArrayList("All Genders", "Male", "Female", "Other"));
                    filterGender.setValue("All Genders");
                }

                if (filterDateRange != null) {
                    filterDateRange.setItems(javafx.collections.FXCollections.observableArrayList("Today", "Yesterday",
                            "Last 7 Days", "Date to Date"));
                    filterDateRange.getSelectionModel().select("Today");
                }

                if (searchPatientField != null) {
                    searchPatientField.textProperty().addListener((obs, oldVal, newVal) -> handleApplyFilters());
                }

                updateStats();
                refreshGlobalUI();

                // License & Expiry System
                updateLicenseDisplay();

                // Start Clock
                startClock();

                // SCROLL AUTOMATION: Enable next/prev page on scroll reach
                if (recentTable != null) {
                    recentTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                        if (newSkin != null) {
                            javafx.scene.control.ScrollBar vBar = (javafx.scene.control.ScrollBar) recentTable
                                    .lookup(".scroll-bar:vertical");
                            if (vBar != null) {
                                vBar.valueProperty().addListener((vObs, vOld, vNew) -> {
                                    if (vNew.doubleValue() == vBar.getMax() && vBar.getMax() > 0) {
                                        // User reached bottom - go next
                                        handleNextActivity();
                                    } else if (vNew.doubleValue() == vBar.getMin() && vBar.getMin() == 0
                                            && currentActivityPage > 0) {
                                        // User reached top - go prev (if not on page 0)
                                        if (vOld.doubleValue() > 0.05) {
                                            handlePrevActivity();
                                        }
                                    }
                                });
                            }
                        }
                    });

                    recentTable.setRowFactory(tv -> {
                        javafx.scene.control.TableRow<com.lab.lms.models.RecentActivity> row = new javafx.scene.control.TableRow<>();
                        row.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2 && (!row.isEmpty())) {
                                com.lab.lms.models.RecentActivity rowData = row.getItem();
                                handleRecentRowDoubleClicked(rowData);
                            }
                        });
                        return row;
                    });
                }

                // Set Default View to Patient Registration & Add Shortcut
                VBox regIcon = (vboxNav != null && vboxNav.getChildren().size() > 1) 
                               ? (VBox) vboxNav.getChildren().get(1) : null;
                performSwitch("Patient Desk", regIcon);

                // BACKGROUND PRE-LOADING: Pre-load heavy views after dashboard is ready
                javafx.animation.PauseTransition preLoadDelay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                preLoadDelay.setOnFinished(e -> NavigationService.preLoad("/fxml/admin.fxml"));
                preLoadDelay.play();

                if (contentArea != null) {
                    if (contentArea.getScene() != null) {
                        addGlobalShortcuts(contentArea.getScene(), regIcon);
                    } else {
                        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
                            if (newScene != null) {
                                addGlobalShortcuts(newScene, regIcon);
                            }
                        });
                    }
                }

                // Demo Integrity Sync: Check Trial Status & Display 
                if (com.lab.lms.services.TrialService.isDemo()) {
                    if (lblTrialStatus != null) {
                        lblTrialStatus.setVisible(true);
                        lblTrialStatus.setManaged(true);
                        
                        if (com.lab.lms.services.TrialService.isExpired()) {
                            lblTrialStatus.setText("TRIAL EXPIRED - SYSTEM LOCKED");
                            showTrialLockout();
                        } else {
                            lblTrialStatus.setText("DEMO VERSION (" + com.lab.lms.services.TrialService.getRemainingDays() + " DAYS REMAINING)");
                        }
                    }
                }
            } catch (Throwable t) {
                System.err.println("[CRITICAL] Dashboard Background Initialization Error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private void addGlobalShortcuts(javafx.scene.Scene scene, VBox regIcon) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, new javafx.event.EventHandler<KeyEvent>() {
                    private StringBuilder barcodeBuffer = new StringBuilder();
                    private long lastTime = 0;

                    @Override
                    public void handle(KeyEvent event) {
                        if (event.isControlDown() && event.getCode() == KeyCode.N) {
                            com.lab.lms.services.SessionContext.setCurrentPatientId(null);
                            com.lab.lms.services.SessionContext.setCurrentSampleId(null);
                            com.lab.lms.services.SessionContext.setSelectedTests(new java.util.ArrayList<>());
                            performSwitch("Patient Desk", regIcon);
                            
                            // Trigger reset on registration controller
                            Object controller = com.lab.lms.services.NavigationService.getController("/fxml/registration.fxml");
                            if (controller instanceof PatientRegistrationController) {
                                ((PatientRegistrationController) controller).handleClear();
                            }
                            
                            event.consume();
                            return;
                        }

                        // Navigation Shortcuts (Alt key combinations)
                        if (event.isAltDown()) {
                            switch (event.getCode()) {
                                case H: performSwitch("Home", navHome); event.consume(); break;
                                case D: performSwitch("Database", navDatabase); event.consume(); break;
                                case P: performSwitch("Billing", navBilling); event.consume(); break;
                                case R: performSwitch("Processing", navProcessing); event.consume(); break;
                                case A: performSwitch("History", navHistory); event.consume(); break;
                                case S: performSwitch("Settings", navSettings); event.consume(); break;
                            }
                        }

                        // Barcode Scan Logic (High-speed sequence detection)
                        long now = System.currentTimeMillis();
                        if (now - lastTime > 60) {
                            barcodeBuffer.setLength(0); // Treat as slow typing if > 60ms
                        }
                        
                        if (event.getCode() == KeyCode.ENTER) {
                            String code = barcodeBuffer.toString().trim();
                            if ((code.startsWith("SAM-") || code.startsWith("PAT")) && code.length() >= 5) {
                                processGlobalBarcode(code);
                                event.consume();
                            }
                            barcodeBuffer.setLength(0);
                        } else if (event.getText() != null && !event.getText().isEmpty()) {
                            barcodeBuffer.append(event.getText());
                            lastTime = now;
                        }
                    }
                });
    }
    
    private void showTrialLockout() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("TRIAL EXPIRED");
        alert.setHeaderText("Clinical Software Evaluation Period Ended");
        alert.setContentText("Your 10-day evaluation period for this system has expired. Please contact MSF Digital Solutions (+923165794442) for activation.");
        alert.showAndWait();
        System.exit(0);
    }

    public void refreshGlobalUI() {
        // Load Lab Branding
        loadLabBranding();

        // Check Permissions & Persistent Settings
        boolean hasDashboard = com.lab.lms.services.SessionContext.hasPermission("dashboard");
        boolean hasRegistration = com.lab.lms.services.SessionContext.hasPermission("registration");
        boolean hasProcessing = com.lab.lms.services.SessionContext.hasPermission("processing");
        boolean hasBilling = com.lab.lms.services.SessionContext.hasPermission("billing");
        boolean hasAuthorization = com.lab.lms.services.SessionContext.hasPermission("authorization");
        boolean hasConfiguration = com.lab.lms.services.SessionContext.hasPermission("configuration");

        // UI Enforcement with Null Safety for 32-bit Resiliency
        if (navHome != null) {
            navHome.setVisible(hasDashboard);
            navHome.setManaged(hasDashboard);
        }
        
        if (navRegistration != null) {
            navRegistration.setVisible(hasRegistration);
            navRegistration.setManaged(hasRegistration);
        }
        
        if (navDatabase != null) {
            navDatabase.setVisible(hasRegistration);
            navDatabase.setManaged(hasRegistration);
        }

        String specimenEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
        boolean isSpecimen = Boolean.parseBoolean(specimenEnabled) && hasProcessing;
        if (navSpecimen != null) {
            navSpecimen.setVisible(isSpecimen);
            navSpecimen.setManaged(isSpecimen);
        }

        if (navBilling != null) {
            navBilling.setVisible(hasBilling);
            navBilling.setManaged(hasBilling);
        }

        if (navProcessing != null) {
            navProcessing.setVisible(hasProcessing);
            navProcessing.setManaged(hasProcessing);
        }

        String doctorEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_doctor_review", "true");
        boolean isDoctor = Boolean.parseBoolean(doctorEnabled) && hasAuthorization;
        if (navDoctor != null) {
            navDoctor.setVisible(isDoctor);
            navDoctor.setManaged(isDoctor);
        }

        if (navHistory != null) {
            navHistory.setVisible(hasRegistration || hasProcessing || hasAuthorization);
            navHistory.setManaged(hasRegistration || hasProcessing || hasAuthorization);
        }

        if (navSettings != null) {
            navSettings.setVisible(hasConfiguration);
            navSettings.setManaged(hasConfiguration);
        }

        if (headerInventory != null) {
            boolean hasInv = com.lab.lms.services.SessionContext.hasPermission("inventory");
            headerInventory.setVisible(hasInv);
            headerInventory.setManaged(hasInv);
        }

        // Auto-update license display on refresh
        updateLicenseDisplay();

        // Hide dashboard stats if no dashboard perm
        if (!hasDashboard && homeView != null) {
            // If they can't see dashboard, force them to registration or something else
            if (hasRegistration) {
                performSwitch("Patient Desk", navRegistration);
            } else if (hasProcessing) {
                performSwitch("Clinical Lab", navProcessing);
            }
        }

        // Rate List Initialization (Synchronized with Admin Toggle)
        checkRateListWindow();
    }

    public void checkRateListWindow() {
        javafx.application.Platform.runLater(() -> {
            try {
                String enabled = DatabaseManager.getSetting("enable_rate_list", "false");
                System.out.println("[CRITICAL DEBUG] Rate List State Sync: " + enabled);

                if ("true".equalsIgnoreCase(enabled)) {
                    if (rateListStage == null) {
                        System.out.println("[CRITICAL DEBUG] Creating Persistent Rate List Accessory...");
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rate_list.fxml"));
                        Parent root = loader.load();
                        
                        rateListStage = new Stage();
                        rateListStage.setTitle("LMS - Clinical Rate List");
                        rateListStage.setScene(new Scene(root));
                        rateListStage.setAlwaysOnTop(true); 
                        rateListStage.setResizable(true);
                        
                        try {
                            java.io.InputStream iconStream = getClass().getResourceAsStream("/images/icon.png");
                            if (iconStream != null) rateListStage.getIcons().add(new javafx.scene.image.Image(iconStream));
                        } catch (Exception ex) {}

                        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                        rateListStage.setX(screen.getVisualBounds().getWidth() - 470); 
                        rateListStage.setY(120);
                    }

                    if (!rateListStage.isShowing()) {
                        rateListStage.show();
                    } else {
                        rateListStage.toFront();
                    }
                } else {
                    if (rateListStage != null && rateListStage.isShowing()) {
                        rateListStage.close();
                    }
                }
            } catch (Throwable t) {
                System.err.println("[RESILIENCY] UI Refresh Error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private void loadLabBranding() {
        String name = com.lab.lms.dao.DatabaseManager.getSetting("lab_name", "MSF DIGITAL SOLUTIONS (SMC-PRIVATE) LIMITED");
        String contact = com.lab.lms.dao.DatabaseManager.getSetting("lab_contact", "03165794442");
        String logoPath = com.lab.lms.dao.DatabaseManager.getSetting("lab_logo", "");

        if (headerLabName != null) headerLabName.setText(name.toUpperCase());
        if (headerLabInfo != null && !contact.isEmpty()) {
            headerLabInfo.setText("Contact: " + contact + " | Registered Excellence");
        }

        if (labLogoView != null && logoPath != null && !logoPath.isEmpty()) {
            java.io.File file = new java.io.File(logoPath);
            if (file.exists()) {
                labLogoView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            }
        }
    }

    private void updateLicenseDisplay() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (lblLicenseStatus == null) return;

                if (com.lab.lms.services.TrialService.isDemo()) {
                    long remaining = com.lab.lms.services.TrialService.getRemainingDays();
                    lblLicenseStatus.setText("LICENSE EXPIRES IN: " + remaining + " DAYS");
                    lblLicenseStatus.setStyle("-fx-text-fill: #FFD54F; -fx-font-weight: bold;");
                } else {
                    String expiryStr = com.lab.lms.dao.DatabaseManager.getSetting("expiry_date", "2025-01-01");
                    if ("2099-12-31".equals(expiryStr)) {
                        lblLicenseStatus.setText("ACTIVATED AS A LIFETIME");
                        lblLicenseStatus.setStyle("-fx-text-fill: #81C784; -fx-font-weight: bold;");
                    } else {
                        try {
                            java.time.LocalDate now = java.time.LocalDate.now();
                            java.time.LocalDate expiry = java.time.LocalDate.parse(expiryStr);
                            long days = java.time.temporal.ChronoUnit.DAYS.between(now, expiry);
                            
                            if (days < 0) {
                                lblLicenseStatus.setText("LICENSE EXPIRED");
                                lblLicenseStatus.setStyle("-fx-text-fill: #EF5350; -fx-font-weight: bold;");
                            } else {
                                lblLicenseStatus.setText("LICENSE EXPIRES IN: " + days + " DAYS");
                                if (days <= 7) {
                                    lblLicenseStatus.setStyle("-fx-text-fill: #FFB74D; -fx-font-weight: bold;"); // Warning orange
                                } else {
                                    lblLicenseStatus.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                                }
                            }
                        } catch (Exception e) {
                            lblLicenseStatus.setText("LICENSE ERROR");
                            lblLicenseStatus.setStyle("-fx-text-fill: #EF5350; -fx-font-weight: bold;");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[RESILIENCY] License Display Error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleCheckForUpdate() {
        if (!com.lab.lms.services.UpdateService.isConnected()) {
            javafx.scene.control.Alert connAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            connAlert.setTitle("CONNECTION REQUIRED");
            connAlert.setHeaderText("No Internet Connection Detected");
            connAlert.setContentText("Please connect to the internet for this updatation.");
            connAlert.show();
            return;
        }

        javafx.scene.control.Alert progress = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        progress.setTitle("SYSTEM UPDATE CHECK");
        progress.setHeaderText(null);
        progress.setContentText("Checking for Clinical Runtime Updates...");
        progress.show();

        new Thread(() -> {
            try {
                String latest = com.lab.lms.services.UpdateService.fetchLatestVersion();
                String current = com.lab.lms.Main.APP_VERSION;

                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    if (!latest.equals(current)) {
                        javafx.scene.control.Alert updateAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, 
                            "A new clinical update [v" + latest + "] is available. \nWould you like to install it now?", 
                            javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                        updateAlert.setTitle("SYSTEM UPDATE AVAILABLE");
                        updateAlert.setHeaderText("New Features & Security Patches Detected");
                        
                        if (updateAlert.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                            try {
                                com.lab.lms.services.UpdateService.applyUpdate();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                javafx.scene.control.Alert deployError = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                deployError.setTitle("DEPLOYMENT ERROR");
                                deployError.setHeaderText("Update Installation Failed");
                                deployError.setContentText("The system encountered a problem while installing the update. Please try again or contact support if the issue persists.");
                                deployError.show();
                            }
                        }
                    } else {
                        javafx.scene.control.Alert latestAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, 
                            "You are currently running the latest version of Standard Medical Laboratory System (v" + current + ").");
                        latestAlert.setTitle("SYSTEM UP TO DATE");
                        latestAlert.setHeaderText("No New Updates Found");
                        latestAlert.show();
                    }
                });
            } catch (Exception e) {
                System.err.println("[RESILIENCY] Update Check failed gracefully: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errorAlert.setTitle("SYNCHRONIZATION ERROR");
                    errorAlert.setHeaderText("Unable to reach the Clinical Update Server");
                    errorAlert.setContentText("The system could not check for new updates at this time. Please ensure your internet connection is stable or contact technical support for assistance.");
                    errorAlert.show();
                });
            }
        }).start();
    }

    @FXML
    private void handleGuideTutorial(javafx.scene.input.MouseEvent event) {
        ContextMenu menu = new ContextMenu();

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setPrefWidth(350);

        Label title = new Label("VIDEO TUTORIALS");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #961111;");
        
        VBox list = new VBox(8);
        list.getChildren().addAll(
            createVideoItem("1. Inventory Management in LMS", "https://youtu.be/PwH-RBlGJUM?si=yljyexn5HNyMDBOK"),
            createVideoItem("2. How to Add New Tests (Settings)", "https://youtu.be/tvH_GTfmNSE?si=kn2pZHJ8YWOIG7m6"),
            createVideoItem("3. Add Test for Existing Patient", "https://youtu.be/Q2roGAdrWGA?si=bKDLMCa7wvc35TVA"),
            createVideoItem("4. How to Edit Test Reports", "https://youtu.be/zuze7eIX-rg?si=X86CfUd1GdqRN7Pz"),
            createVideoItem("5. Complete Workflow (Reg->Print)", "https://youtu.be/WewW1rMXKAk?si=IPjwAGW_hQOigTQO"),
            createVideoItem("6. Patient Registry & Past Records", "https://youtu.be/Op_kJF-B-Z8?si=PvpLgsBUZxC8N_ei"),
            createVideoItem("7. Lab Branding & Organization", "https://youtu.be/YaqX8nAGrEw?si=feLZl4sZqA4dbPJA"),
            createVideoItem("8. Add Laboratory Staff/Users", "https://youtu.be/I3tFC43NIVg?si=1LbWjJf7UzR5F-_N"),
            createVideoItem("9. Pay Remaining Balance Amount", "https://www.youtube.com/watch?v=94jCnGByi_E")
        );

        container.getChildren().addAll(title, new Separator(), list);
        
        CustomMenuItem customItem = new CustomMenuItem(container);
        customItem.setHideOnClick(false);
        menu.getItems().add(customItem);

        Node source = (Node) event.getSource();
        menu.show(source, Side.BOTTOM, 0, 5);
    }

    private HBox createVideoItem(String title, String url) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #37474F;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(220);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btn = new Button("WATCH");
        btn.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 15; -fx-cursor: hand;");
        btn.setOnAction(e -> openUrl(url));

        HBox row = new HBox(10, lbl, spacer, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setStyle("-fx-background-color: #F5F7F8; -fx-background-radius: 6;");
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 6;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #F5F7F8; -fx-background-radius: 6;"));

        return row;
    }

    @FXML
    private void handleUserManual(javafx.scene.input.MouseEvent event) {
        ContextMenu menu = new ContextMenu();

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setPrefWidth(320);

        Label title = new Label("USER MANUAL & GUIDANCE");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #1976D2;");
        
        VBox list = new VBox(5);
        list.getChildren().addAll(
            createManualItem("ALT + H  ->  Home Dashboard"),
            createManualItem("ALT + D  ->  Patient Registry"),
            createManualItem("ALT + P  ->  Billing & Payments"),
            createManualItem("ALT + R  ->  Result Entry (Lab)"),
            createManualItem("ALT + A  ->  Patient History"),
            createManualItem("ALT + S  ->  System Settings"),
            createManualItem("CTRL + N ->  New Patient Entry")
        );

        container.getChildren().addAll(title, new Separator(), list);
        
        CustomMenuItem customItem = new CustomMenuItem(container);
        customItem.setHideOnClick(true);
        menu.getItems().add(customItem);

        Node source = (Node) event.getSource();
        menu.show(source, Side.BOTTOM, 0, 5);
    }

    private Label createManualItem(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #455A64; -fx-padding: 5 8; -fx-cursor: hand;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #1976D2; -fx-background-color: #E3F2FD; -fx-background-radius: 4; -fx-padding: 5 8; -fx-cursor: hand;"));
        lbl.setOnMouseExited(e -> lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #455A64; -fx-padding: 5 8; -fx-cursor: hand;"));
        return lbl;
    }

    @FXML
    private void handleCustomerSupport(javafx.scene.input.MouseEvent event) {
        ContextMenu menu = new ContextMenu();

        VBox container = new VBox(12);
        container.setPadding(new Insets(15));
        container.setPrefWidth(300);

        Label title = new Label("CUSTOMER SUPPORT");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #00796B;");

        String labName = com.lab.lms.dao.DatabaseManager.getSetting("lab_name", "Laboratory");
        String systemId = com.lab.lms.dao.DatabaseManager.getSetting("system_id", "ST-UNK");

        VBox info = new VBox(5);
        Label phoneLbl = new Label("Technical Help: +92 316 5794442");
        Label emailLbl = new Label("Email: msf.support@digital.com");
        phoneLbl.setStyle("-fx-font-weight: bold;");
        info.getChildren().addAll(phoneLbl, emailLbl);
        info.setStyle("-fx-font-size: 11; -fx-text-fill: #455A64;");

        Button whatsappBtn = new Button("CONTACT ON WHATSAPP");
        whatsappBtn.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11;");
        whatsappBtn.setMaxWidth(Double.MAX_VALUE);
        
        whatsappBtn.setOnAction(e -> {
            String msg = "*LMS SUPPORT REQUEST*\n" +
                         "----------------------------\n" +
                         "*Lab:* " + labName + "\n" +
                         "*System ID:* " + systemId + "\n" +
                         "----------------------------\n" +
                         "I need assistance regarding:";
            try {
                String encodedMsg = java.net.URLEncoder.encode(msg, "UTF-8");
                openUrl("https://wa.me/923165794442?text=" + encodedMsg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Label footer = new Label("System ID: " + systemId);
        footer.setStyle("-fx-font-size: 9; -fx-text-fill: #90A4AE;");

        container.getChildren().addAll(title, new Separator(), info, whatsappBtn, footer);
        
        CustomMenuItem customItem = new CustomMenuItem(container);
        customItem.setHideOnClick(false);
        menu.getItems().add(customItem);

        Node source = (Node) event.getSource();
        menu.show(source, Side.BOTTOM, 0, 5);
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startClock() {
        if (lblDate != null) {
            lblDate.setText(
                    new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy").format(new java.util.Date()).toUpperCase());
        }
        Thread clockThread = new Thread(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm:ss a");
            while (true) {
                String time = sdf.format(new java.util.Date());
                javafx.application.Platform.runLater(() -> {
                    if (lblClock != null) lblClock.setText(time);
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();
    }

    private void updateStats() {
        if (filterStartDate == null || filterEndDate == null) return;
        
        String start = filterStartDate.getValue() != null ? filterStartDate.getValue().toString() : "1970-01-01";
        String end = filterEndDate.getValue() != null ? filterEndDate.getValue().toString() : "2099-12-31";

        String role = com.lab.lms.services.SessionContext.getUserRole();
        String currentStaffId = com.lab.lms.services.SessionContext.getStaffId();

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Registered Patients in Range
            String patSql = "SELECT COUNT(DISTINCT p.patient_id) FROM patients p WHERE (" +
                            "date(registration_date) BETWEEN ? AND ? OR " +
                            "patient_id IN (SELECT patient_id FROM samples WHERE date(collection_date, 'localtime') BETWEEN ? AND ?) OR " +
                            "patient_id IN (SELECT patient_id FROM invoices WHERE date(date, 'localtime') BETWEEN ? AND ?)) " +
                            "AND (? = 'ADMIN' OR staff_id = ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(patSql)) {
                pstmt.setString(1, start);
                pstmt.setString(2, end);
                pstmt.setString(3, start);
                pstmt.setString(4, end);
                pstmt.setString(5, start);
                pstmt.setString(6, end);
                pstmt.setString(7, role);
                pstmt.setString(8, currentStaffId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (lblTodayRegistered != null && rs.next()) {
                        String val = String.valueOf(rs.getInt(1));
                        lblTodayRegistered.setText(isDataHidden ? "****" : val);
                    }
                }
            } catch (SQLException e) {
                if (lblTodayRegistered != null) lblTodayRegistered.setText("0");
            }

            // 2. Pending Tests
            String qSql = "SELECT COUNT(DISTINCT r.sample_id) FROM results r " +
                          "JOIN samples s ON r.sample_id = s.sample_id " +
                          "JOIN patients p ON s.patient_id = p.patient_id " +
                          "WHERE r.doctor_approval = 0 AND (" +
                          "  (s.collection_date IS NOT NULL AND date(s.collection_date, 'localtime') BETWEEN ? AND ?) OR " +
                          "  (s.collection_date IS NULL AND date(p.registration_date) BETWEEN ? AND ?)" +
                          ") AND (? = 'ADMIN' OR s.staff_id = ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(qSql)) {
                pstmt.setString(1, start);
                pstmt.setString(2, end);
                pstmt.setString(3, start);
                pstmt.setString(4, end);
                pstmt.setString(5, role);
                pstmt.setString(6, currentStaffId);
                ResultSet rs = pstmt.executeQuery();
                if (lblPendingTests != null && rs.next()) {
                    String val = String.valueOf(rs.getInt(1));
                    lblPendingTests.setText(isDataHidden ? "****" : val);
                }
            } catch (SQLException e) {
                if (lblPendingTests != null) lblPendingTests.setText("0");
            }

            // 3. Total Revenue
            String revSql = "SELECT SUM(final_amount) FROM invoices WHERE date(date, 'localtime') BETWEEN ? AND ? " +
                           "AND (? = 'ADMIN' OR staff_id = ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(revSql)) {
                pstmt.setString(1, start);
                pstmt.setString(2, end);
                pstmt.setString(3, role);
                pstmt.setString(4, currentStaffId);
                ResultSet rs = pstmt.executeQuery();
                if (lblRevenue != null && rs.next()) {
                    String val = String.format("%.2f", rs.getDouble(1));
                    lblRevenue.setText(isDataHidden ? "********" : val);
                }
            } catch (SQLException e) {
                if (lblRevenue != null) lblRevenue.setText("0.00");
            }

            // Recent Patients
            try {
                javafx.collections.ObservableList<com.lab.lms.models.RecentActivity> recent = javafx.collections.FXCollections.observableArrayList();

                StringBuilder sql = new StringBuilder(
                        "SELECT p.patient_id, p.name, p.phone, p.whatsapp, p.registration_date, " +
                                "(SELECT status FROM invoices WHERE patient_id = p.patient_id ORDER BY id DESC LIMIT 1) as inv_status, " +
                                "(SELECT SUM(total_amount) FROM invoices WHERE patient_id = p.patient_id) as inv_total, " +
                                "(SELECT SUM(discount) FROM invoices WHERE patient_id = p.patient_id) as inv_disc, " +
                                "(SELECT SUM(final_amount) FROM invoices WHERE patient_id = p.patient_id) as inv_final, " +
                                "(SELECT SUM(paid_amount) FROM invoices WHERE patient_id = p.patient_id) as inv_paid, " +
                                "(SELECT SUM(due_amount) FROM invoices WHERE patient_id = p.patient_id) as inv_due, " +
                                "(SELECT COUNT(*) FROM invoices WHERE patient_id = p.patient_id) as inv_count, " +
                                "(SELECT status FROM samples WHERE patient_id = p.patient_id ORDER BY id DESC LIMIT 1) as smp_status, " +
                                "(SELECT MIN(doctor_approval) FROM results r JOIN samples s ON r.sample_id = s.sample_id WHERE s.patient_id = p.patient_id) as doc_app_min " +
                                "FROM patients p WHERE 1=1 AND (? = 'ADMIN' OR p.staff_id = ?) ");

                if (filterStartDate.getValue() != null && filterEndDate.getValue() != null) {
                    String s = filterStartDate.getValue().toString();
                    String e = filterEndDate.getValue().toString();
                    sql.append(" AND (p.patient_id IN (")
                       .append("SELECT patient_id FROM patients WHERE date(registration_date) BETWEEN '").append(s).append("' AND '").append(e).append("' ")
                       .append("UNION SELECT patient_id FROM samples WHERE date(collection_date, 'localtime') BETWEEN '").append(s).append("' AND '").append(e).append("' ")
                       .append("UNION SELECT patient_id FROM invoices WHERE date(date, 'localtime') BETWEEN '").append(s).append("' AND '").append(e).append("' ")
                       .append(")) ");
                }

                if (searchPatientField != null && !searchPatientField.getText().isEmpty()) {
                    sql.append(" AND (p.name LIKE '%").append(searchPatientField.getText()).append("%' OR p.patient_id LIKE '%").append(searchPatientField.getText()).append("%') ");
                }

                if (filterGender != null && filterGender.getValue() != null && !"All Genders".equals(filterGender.getValue())) {
                    sql.append(" AND p.gender = '").append(filterGender.getValue()).append("' ");
                }

                sql.append(" ORDER BY p.id DESC LIMIT ? OFFSET ?");

                try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                    pstmt.setString(1, role);
                    pstmt.setString(2, currentStaffId);
                    pstmt.setInt(3, ACTIVITY_PAGE_SIZE);
                    pstmt.setInt(4, currentActivityPage * ACTIVITY_PAGE_SIZE);
                    
                    try (ResultSet rs4 = pstmt.executeQuery()) {
                        while (rs4.next()) {
                            String pId = rs4.getString("patient_id");
                            String name = rs4.getString("name");
                            String rawDate = rs4.getString("registration_date");
                            String formattedTime = rawDate != null ? rawDate : "Unknown";

                            String status = "Registered";
                            int invCount = rs4.getInt("inv_count");
                            String invStatus = rs4.getString("inv_status");
                            String smpStatus = rs4.getString("smp_status");
                            int docAppMin = rs4.getInt("doc_app_min");
                            boolean hasResults = !rs4.wasNull();

                            if (invCount == 0) {
                                status = "Awaiting Test Selection";
                            } else if ("UNPAID".equals(invStatus) || "PARTIAL".equals(invStatus)) {
                                status = "Payment Pending";
                            } else if (smpStatus == null) {
                                status = "Awaiting Specimen";
                            } else if ("PROCESSING".equals(smpStatus)) {
                                status = "In Lab Analysis";
                            } else if (hasResults && docAppMin == 0) {
                                status = "Report Confirmation";
                            } else if (hasResults && docAppMin == 1) {
                                status = "Report Ready";
                            }

                            String pPhone = rs4.getString("phone");
                            if (pPhone == null || pPhone.isEmpty()) pPhone = rs4.getString("whatsapp");
                            if (pPhone == null) pPhone = "";

                            String paidStr = String.format("%.2f", rs4.getDouble("inv_paid"));
                            String dueStr = String.format("%.2f", rs4.getDouble("inv_due"));
                            String discStr = String.format("%.2f", rs4.getDouble("inv_disc"));

                            if (invCount == 0) {
                                paidStr = "0.00";
                                dueStr = "0.00";
                                discStr = "0.00";
                            }

                            recent.add(new com.lab.lms.models.RecentActivity(pId, name, status, 
                                isDataHidden ? "****" : pPhone, 
                                formattedTime, 
                                isDataHidden ? "****" : paidStr, 
                                isDataHidden ? "****" : discStr, 
                                isDataHidden ? "****" : dueStr));
                        }
                    }
                }
                javafx.application.Platform.runLater(() -> {
                    if (recentTable != null) recentTable.getItems().setAll(recent);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        updateStats();
    }

    @FXML
    private void switchViewMouse(javafx.scene.input.MouseEvent event) {
        Object source = event.getSource();
        if (source instanceof VBox) {
            VBox clickedBox = (VBox) source;
            String text = "";
            for (Node child : clickedBox.getChildren()) {
                if (child instanceof Label && ((Label) child).getText() != null
                        && ((Label) child).getText().length() > 2) {
                    text = ((Label) child).getText();
                }
            }
            performSwitch(text, clickedBox);
        }
    }

    @FXML
    private void switchView(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        performSwitch(clickedBtn.getText(), null);
    }

    private void performSwitch(String text, VBox clickedIcon) {
        updateStats(); // Refresh stats when navigating

        if (clickedIcon != null && vboxNav != null) {
            for (Node node : vboxNav.getChildren()) {
                if (node instanceof VBox) {
                    node.getStyleClass().remove("nav-icon-btn-active");
                }
            }
            clickedIcon.getStyleClass().add("nav-icon-btn-active");
        }

        try {
            Node node = null;
            if (text.contains("Database") || text.contains("Registry")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("registration")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/patients.fxml");
            } else if (text.contains("Profile")) {
                node = com.lab.lms.services.NavigationService.getView("/fxml/profile.fxml");
            } else if (text.contains("Patient") || text.contains("Registration") || text.contains("New Entry")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("registration")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/registration.fxml");
            } else if (text.contains("Test") || text.contains("Billing")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("billing")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/billing.fxml");
            } else if (text.contains("Specimen") || text.contains("Sample") || text.contains("Collection")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("processing")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/samples.fxml");
            } else if (text.contains("Processing") || text.contains("Clinical")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("processing")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/processing.fxml");
            } else if (text.contains("Review") || text.contains("Doctor")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("authorization")) return;
                node = com.lab.lms.services.NavigationService.getView("/fxml/review.fxml");
            } else if (text.contains("History") || text.contains("Records") || text.contains("Case History")) {
                node = com.lab.lms.services.NavigationService.getView("/fxml/history.fxml");
            } else if (text.contains("Settings") || text.contains("Admin") || text.contains("Inventory")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("configuration")) return;
                
                if (text.contains("Inventory")) {
                    com.lab.lms.services.SessionContext.setTargetTab("inventory");
                }
                
                node = com.lab.lms.services.NavigationService.getView("/fxml/admin.fxml");
            } else if (text.contains("Dashboard") || text.contains("Reports") || text.contains("Home")) {
                if (!com.lab.lms.services.SessionContext.hasPermission("dashboard")) return;
                node = homeView;
            }

            if (node != null && contentArea != null) {
                contentArea.getChildren().setAll(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Critical Interface Fault: " + e.toString() + "\n\nRefer to system developer logs for precise clinical diagnostic.");
            alert.show();
        }
    }

    @FXML
    private void handleOpenReport() {
        try {
            com.lab.lms.services.NavigationService.switchView("/fxml/summary_report.fxml");
            if (contentArea != null) {
                // Update sidebar selection visually
                if (vboxNav != null) {
                    for (Node n : vboxNav.getChildren()) {
                        if (n instanceof VBox) {
                            n.getStyleClass().remove("nav-icon-btn-active");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogoClick() {
        performSwitch("Home", null);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        doLogout();
    }

    @FXML
    private void handleLogoutMouse(javafx.scene.input.MouseEvent event) {
        doLogout();
    }

    private void doLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();

            // Reset to Login Size - Re-normalized for focused clinical identity check
            stage.setScene(new Scene(login, 500, 530));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.setTitle("Laboratory Management System - Clinical Network Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRecentRowDoubleClicked(com.lab.lms.models.RecentActivity rowData) {
        String status = rowData.getStatus();
        String pid = rowData.getPatientId();
        com.lab.lms.services.SessionContext.setCurrentPatientId(pid);

        String targetText = "Dashboard";
        String specimenEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");

        if (status.contains("Test Selection") || status.contains("Payment") || status.equals("Registered")) {
            targetText = "Test Billing";
        } else if (status.contains("Specimen")) {
            if (Boolean.parseBoolean(specimenEnabled)) {
                targetText = "Specimen";
            } else {
                targetText = "Clinical Lab";
            }
        } else if (status.contains("Lab Analysis")) {
            targetText = "Clinical Lab";
        } else if (status.contains("Confirmation")) {
            if (Boolean.parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("enable_doctor_review", "true"))) {
                targetText = "Medical Review";
            } else {
                targetText = "Case History";
            }
        } else if (status.contains("Ready") || status.equals("Completed")) {
            // Dynamic behavior: Open report if ready
            try (Connection conn = DatabaseManager.getConnection()) {
                String pdfSql = "SELECT r.pdf_path, p.name, p.whatsapp, p.phone FROM results r " +
                        "JOIN samples s ON r.sample_id = s.sample_id " +
                        "JOIN patients p ON s.patient_id = p.patient_id " +
                        "WHERE s.patient_id = ? AND r.pdf_path IS NOT NULL ORDER BY r.id DESC LIMIT 1";
                PreparedStatement pstmt = conn.prepareStatement(pdfSql);
                pstmt.setString(1, pid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String pdfPath = rs.getString("pdf_path");
                    String pName = rs.getString("name");
                    String pWhatsApp = rs.getString("whatsapp");
                    String pPhone = rs.getString("phone");
                    String contact = (pWhatsApp != null && !pWhatsApp.isEmpty()) ? pWhatsApp : pPhone;

                    if (pdfPath != null && !pdfPath.isEmpty()) {
                        File pdfFile = new File(pdfPath);
                        if (pdfFile.exists()) {
                            // Decision Gateway: View vs WhatsApp
                            Alert decision = new Alert(AlertType.CONFIRMATION);
                            decision.setTitle("Report Action - " + pName);
                            decision.setHeaderText("Choose Service for " + pName);
                            decision.setContentText("Would you like to view the report locally or send it via WhatsApp?");

                            ButtonType btnView = new ButtonType("VIEW REPORT");
                            ButtonType btnWhatsApp = new ButtonType("SEND WHATSAPP");
                            ButtonType btnCancel = new ButtonType("CLOSE", ButtonData.CANCEL_CLOSE);

                            decision.getButtonTypes().setAll(btnView, btnWhatsApp, btnCancel);

                            Optional<ButtonType> result = decision.showAndWait();
                            if (result.isPresent()) {
                                if (result.get() == btnView) {
                                    java.awt.Desktop.getDesktop().open(pdfFile);
                                } else if (result.get() == btnWhatsApp) {
                                    com.lab.lms.services.WhatsAppService.sendReportWithRecovery(pid, pName, contact, pdfPath);
                                }
                            }
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            targetText = "Case History";
        }

        // Find sidebar icon for consistent UX
        VBox targetIcon = null;
        if (vboxNav != null) {
            for (Node node : vboxNav.getChildren()) {
                if (node instanceof VBox) {
                    VBox iconBox = (VBox) node;
                    for (Node child : iconBox.getChildren()) {
                        if (child instanceof Label && ((Label) child).getText() != null) {
                            String labelText = ((Label) child).getText();
                            // Check hidden managed labels which have the text
                            if (!child.isManaged()) {
                                if (labelText.contains(targetText)
                                        || (targetText.equals("Doctor") && labelText.contains("Review"))) {
                                    targetIcon = iconBox;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (targetIcon != null)
                    break;
            }
        }

        performSwitch(targetText, targetIcon);
    }

    @FXML
    private void handlePrintEmptyTemplate() {
        try {
            String pdfPath = com.lab.lms.services.ReportGenerator.generateEmptyTemplate();
            if (pdfPath != null) {
                java.io.File file = new java.io.File(pdfPath);
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            }
        } catch (Throwable t) {
            System.err.println("[RESILIENCY] Desktop open failed: " + t.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        if (searchPatientField != null)
            searchPatientField.clear();
        if (filterStartDate != null)
            filterStartDate.setValue(null);
        if (filterEndDate != null)
            filterEndDate.setValue(null);
        if (filterGender != null)
            filterGender.setValue("All Genders");
        if (filterDateRange != null)
            filterDateRange.getSelectionModel().select("Today");
        currentActivityPage = 0;
        updateStats();
    }

    private void processGlobalBarcode(String code) {
        System.out.println("[SCANNER] GLOBAL SCAN DETECTED: " + code);
        try (Connection conn = DatabaseManager.getConnection()) {
            // Check for Patient ID - Quick Navigation
            if (code.startsWith("PAT")) {
                com.lab.lms.services.SessionContext.setCurrentPatientId(code);
                javafx.application.Platform.runLater(() -> {
                    performSwitch("Patient Desk", null);
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Clinic Navigation");
                    alert.setHeaderText("Patient Identified: " + code);
                    alert.setContentText("Redirected to Patient Registration module for subject: " + code);
                    alert.show();
                });
                return;
            }

            // Check for Sample ID - Logic for Collection / Receipt
            String q = "SELECT status, patient_id FROM samples WHERE sample_id = ?";
            PreparedStatement ps = conn.prepareStatement(q);
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String dbStatus = rs.getString("status");
                String trackingEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_specimen_tracking", "true");
                boolean isPhaseOn = Boolean.parseBoolean(trackingEnabled);

                if ("AWAITING COLLECTION".equals(dbStatus)) {
                    PreparedStatement up = conn.prepareStatement(
                        "UPDATE samples SET status = 'COLLECTED', collection_date = CURRENT_TIMESTAMP WHERE sample_id = ?");
                    up.setString(1, code);
                    up.executeUpdate();
                    
                    String displayStatus = isPhaseOn ? "SPECIMEN RECEIVED" : "PENDING";
                    
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Clinical Specimen Check-in");
                        alert.setHeaderText("Specimen " + code + ": " + displayStatus);
                        alert.setContentText("The specimen has been successfully checked in and timestamped.\n" +
                                           "Current Phase: " + (isPhaseOn ? "SPECIMEN RECEIVED (Waiting for Analysis)" : "PENDING (Ready for Evaluation)"));
                        alert.show();
                        
                        // Smart Redirect: Go to Specimens or Analysis based on flow
                        com.lab.lms.services.SessionContext.setCurrentSampleId(code);
                        if (isPhaseOn) {
                            performSwitch("Specimen", null);
                        } else {
                            performSwitch("Clinical Lab", null);
                        }
                        updateStats(); // Refresh dashboard stats
                    });
                } else if ("COLLECTED".equals(dbStatus)) {
                    javafx.application.Platform.runLater(() -> {
                        com.lab.lms.services.SessionContext.setCurrentSampleId(code);
                        performSwitch("Clinical Lab", null);
                    });
                }
            } else {
                System.out.println("[SCANNER] No matching clinical record for: " + code);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePaidFieldUpdate(com.lab.lms.models.RecentActivity rowData) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(rowData.getPaid());
        dialog.setTitle("Update Patient Payment");
        dialog.setHeaderText("Finance Override: Update Paid Amount for Lab Work");
        dialog.setContentText("Enter New Paid Amount (MR: " + rowData.getPatientId() + "):");

        dialog.showAndWait().ifPresent(newVal -> {
            try {
                double newPaid = Double.parseDouble(newVal);
                try (Connection conn = DatabaseManager.getConnection()) {
                    // Update latest invoice
                    String updateSql = "UPDATE invoices SET paid_amount = ?, due_amount = final_amount - ?, " +
                                       "status = CASE WHEN final_amount - ? <= 0 THEN 'PAID' ELSE 'PARTIAL' END " +
                                       "WHERE id = (SELECT id FROM invoices WHERE patient_id = ? ORDER BY id DESC LIMIT 1)";
                    PreparedStatement pstmt = conn.prepareStatement(updateSql);
                    pstmt.setDouble(1, newPaid);
                    pstmt.setDouble(2, newPaid);
                    pstmt.setDouble(3, newPaid);
                    pstmt.setString(4, rowData.getPatientId());
                    int affected = pstmt.executeUpdate();
                    if (affected > 0) {
                        String path = ReportGenerator.regenerateLatestReceipt(rowData.getPatientId());
                        updateStats(); // Refresh
                        
                        // Action popup following professional billing pattern
                        if (path != null) {
                            showPaymentSuccessActions(path, rowData.getPatientId());
                        }
                    }
                }
            } catch (Exception e) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Invalid Amount: " + e.getMessage());
                alert.show();
            }
        });
    }

    private void showPaymentSuccessActions(String pdfPath, String patientId) {
        javafx.scene.control.Alert actions = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        actions.setTitle("Payment Synchronized");
        actions.setHeaderText("Financial Protocol Updated Successfully");
        actions.setContentText("The client payment has been recorded and the clinical receipt is synchronized.");

        javafx.scene.control.ButtonType btnView = new javafx.scene.control.ButtonType("VIEW RECEIPT");
        javafx.scene.control.ButtonType btnPrint = new javafx.scene.control.ButtonType("PRINT");
        javafx.scene.control.ButtonType btnOk = new javafx.scene.control.ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);

        actions.getButtonTypes().setAll(btnView, btnPrint, btnOk);

        actions.showAndWait().ifPresent(response -> {
            if (response == btnView || response == btnPrint) {
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
                    // Re-show after view/print to allow other action or exit via OK
                    showPaymentSuccessActions(pdfPath, patientId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // NO REDIRECTION: Clicking OK simply closes the dialog and stays on Dashboard.
        });
    }

    @FXML
    private void handleEnterLicenseKey() {
        String systemId = DatabaseManager.getSetting("system_id", "UNKNOWN");
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("System Activation");
        dialog.setHeaderText("Activate Clinical License\nSystem ID: " + systemId);
        dialog.setContentText("Enter Activation Key:");
        
        // Use lab logo as dialog icon if available
        try {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(key -> {
            if (key.trim().isEmpty()) return;
            
            boolean success = com.lab.lms.services.SubscriptionService.validateAndRenew(key);
            if (success) {
                // De-escalate from Demo if active
                com.lab.lms.services.TrialService.setDemo(false);
                
                updateLicenseDisplay();
                
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Activation Successful");
                alert.setHeaderText("System Activated Successfully");
                alert.setContentText("The clinical license has been renewed. Please restart the system if some features remain locked.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Activation Failed");
                alert.setHeaderText("Invalid Activation Key");
                alert.setContentText("The provided key is invalid, expired, or does not match this System ID (" + systemId + ").");
                alert.show();
            }
        });
    }

    private void handleViewReceipt(com.lab.lms.models.RecentActivity rowData) {
        try {
            String path = ReportGenerator.regenerateLatestReceipt(rowData.getPatientId());
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) java.awt.Desktop.getDesktop().open(file);
                else throw new Exception("Receipt file not found on disk: " + path);
            } else {
                throw new Exception("No receipt record exists for this patient.");
            }
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Receipt Access Error: " + e.getMessage());
            alert.show();
        }
    }

    private void handleViewReport(com.lab.lms.models.RecentActivity rowData) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT pdf_path FROM results r JOIN samples s ON r.sample_id = s.sample_id WHERE s.patient_id = ? AND pdf_path IS NOT NULL ORDER BY r.id DESC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, rowData.getPatientId());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String path = rs.getString("pdf_path");
                if (path != null && !path.isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) java.awt.Desktop.getDesktop().open(file);
                    else throw new Exception("Clinical Report file not found on disk: " + path);
                }
            } else {
                throw new Exception("Laboratory Analysis not yet completed or reported.");
            }
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Clinical Report Error: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void handleExportDatabase(javafx.scene.input.MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Backup Database (SQL Export)");
        fileChooser.setInitialFileName("lab_backup_" + java.time.LocalDate.now() + ".db");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files (*.db)", "*.db"));
        
        File file = fileChooser.showSaveDialog(contentArea.getScene().getWindow());
        if (file != null) {
            try {
                Path source = Paths.get(DatabaseManager.getDbPath());
                Path target = file.toPath();
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                
                Alert success = new Alert(AlertType.INFORMATION, "Database backup successful!\nFile saved to: " + file.getAbsolutePath());
                success.setTitle("Backup Complete");
                success.show();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void handleImportDatabase(javafx.scene.input.MouseEvent event) {
        Alert confirm = new Alert(AlertType.WARNING, "Are you sure you want to import this database?\n\nWARNING: THIS WILL OVERWRITE ALL CURRENT DATA AND RESTART THE APPLICATION.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Restore (SQL Import)");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Database to Restore");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files (*.db)", "*.db"));
            
            File file = fileChooser.showOpenDialog(contentArea.getScene().getWindow());
            if (file != null) {
                try {
                    Path source = file.toPath();
                    Path target = Paths.get(DatabaseManager.getDbPath());
                    
                    // Simple file copy to overwrite the current database
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    
                    Alert restartAlert = new Alert(AlertType.INFORMATION, "Import Successful! The application will now restart to finalize the restoration.");
                    restartAlert.setTitle("Import Complete");
                    restartAlert.showAndWait();
                    
                    com.lab.lms.Main.restart();
                } catch (Exception e) {
                    e.printStackTrace();
                    new Alert(AlertType.ERROR, "Import Failed: " + e.getMessage()).show();
                }
            }
        }
    }

    @FXML
    private void handleNavInventory() {
        com.lab.lms.services.SessionContext.setTargetTab("inventory");
        performSwitch("Inventory Management", null);
    }
}

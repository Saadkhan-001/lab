package com.lab.lms.controllers;

import javafx.scene.control.ChoiceBox;
import javafx.beans.property.SimpleStringProperty;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.*;
import com.lab.lms.services.BackupService;
import com.lab.lms.services.ReportGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.web.HTMLEditor;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.collections.transformation.FilteredList;
import java.io.File;
import java.util.List;
import java.sql.*;
import java.util.ArrayList;
import javafx.stage.Stage;

public class AdminController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField numericCodeField;
    @FXML
    private TextField alphaCodeField;
    @FXML
    private ComboBox<String> categoryField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField resultTimeField;
    @FXML
    private ComboBox<String> tatUnitBox;
    @FXML
    private HTMLEditor testNotesArea;
    @FXML
    private TextField specimenField;
    @FXML
    private ComboBox<String> testClassBox;
    @FXML
    private CheckBox isSpecialCheck;
    @FXML
    private CheckBox isMicroscopicCheck;
    @FXML
    private CheckBox isCultureCheck;
    @FXML
    private Label testImagePathLabel;

    @FXML
    private TextField adminUsernameField;
    @FXML
    private PasswordField adminPasswordField;
    @FXML
    private PasswordField adminConfirmPasswordField;

    @FXML
    private TextField paramNameField;
    @FXML
    private TextField paramUnitField;
    @FXML
    private TextField paramMinField;
    @FXML
    private TextField paramMaxField;
    @FXML
    private TextField paramMinKidsField;
    @FXML
    private TextField paramMaxKidsField;
    @FXML
    private TextField paramMinMaleField;
    @FXML
    private TextField paramMaxMaleField;
    @FXML
    private TextField paramMinFemaleField;
    @FXML
    private TextField paramMaxFemaleField;
    @FXML
    private VBox unitInputBox, minInputBox, maxInputBox, categoryInputBox;
    @FXML
    private VBox kidsRangeBox, maleRangeBox, femaleRangeBox;
    @FXML
    private VBox containerGeneralRange, containerKidsRange, containerMaleRange, containerFemaleRange;
    
    private java.util.List<TextField> extraMinFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMaxFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMinMaleFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMaxMaleFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMinFemaleFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMaxFemaleFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMinKidsFields = new java.util.ArrayList<>();
    private java.util.List<TextField> extraMaxKidsFields = new java.util.ArrayList<>();
    @FXML
    private Label minLabel, paramNameLabel, paramCategoryLabel;
    @FXML
    private VBox referenceValueBox;
    @FXML
    private ComboBox<String> paramCategoryField, cultureTypeField;
    @FXML
    private VBox cultureTypeInputBox;
    @FXML
    private TextArea secondaryRangesArea;

    @FXML
    private TableView<TestParameter> paramTable;
    @FXML
    private Button btnAddParameter;
    @FXML
    private TableColumn<TestParameter, Boolean> colParamSelect;
    @FXML
    private TableColumn<TestParameter, String> colParamName;
    @FXML
    private TableColumn<TestParameter, String> colParamUnit;
    @FXML
    private TableColumn<TestParameter, String> colParamCategory;
    @FXML
    private TableColumn<TestParameter, String> colParamRange;

    private TestParameter selectedParameter;

    @FXML
    private TableView<Test> testTable;
    @FXML
    private TableColumn<Test, String> colNumericCode;
    @FXML
    private TableColumn<Test, String> colAlphaCode;
    @FXML
    private TableColumn<Test, String> colName;
    @FXML
    private TableColumn<Test, Double> colPrice;
    @FXML
    private TableColumn<Test, String> colTAT;
    @FXML
    private TableColumn<Test, Void> colAction;

    @FXML
    private Label backupPathLabel;
    @FXML
    private Label templatePathLabel;
    @FXML
    private Label signaturePathLabel;
    @FXML
    private Label logoPathLabel;
    @FXML
    private Label headerPathLabel;
    @FXML
    private Label watermarkPathLabel;
    @FXML
    private Label footerPathLabel;

    @FXML
    private TextArea pathologistDetailsArea;
    @FXML
    private TextArea doctorsFooterArea;
    @FXML
    private TextArea reportNoteArea;

    @FXML
    private TextField labNameField;
    @FXML
    private TextField labContactField;
    @FXML
    private TextArea labAddressArea;
    @FXML
    private TextArea receiptPoliciesArea;
    @FXML
    private TextField labEmailField, labWebsiteField, labTaglineField;

    @FXML
    private TextField staffNameField;
    @FXML
    private TextArea staffAddressField;
    @FXML
    private TextField staffPhoneField;
    @FXML
    private ComboBox<String> staffGenderBox;
    @FXML
    private DatePicker staffDobPicker;
    @FXML
    private TextField staffQualField;
    @FXML
    private PasswordField staffPassField;
    @FXML
    private TextField staffDesignationField;
    @FXML
    private javafx.scene.image.ImageView staffImageView;
    @FXML
    private Label staffImagePathLabel;

    @FXML
    private TableView<Staff> staffTable;
    @FXML
    private TableColumn<Staff, String> colStaffId;
    @FXML
    private TableColumn<Staff, String> colStaffName;
    @FXML
    private TableColumn<Staff, String> colStaffDesignation;
    @FXML
    private TableColumn<Staff, String> colStaffGender;
    @FXML
    private TableColumn<Staff, String> colStaffAddress;
    @FXML
    private TableColumn<Staff, String> colStaffQual;
    @FXML
    private TableColumn<Staff, String> colStaffPhone;
    @FXML
    private TableColumn<Staff, String> colStaffDob;
    @FXML
    private TableColumn<Staff, String> colStaffPass;
    @FXML
    private TableColumn<Staff, Void> colStaffAction;

    @FXML
    private CheckBox permDashboard, permRegistration, permProcessing, permBilling, permInventory, permAuthorization, permConfiguration, permTestDeletion;
    @FXML
    private CheckBox toggleMultiDeviceMode, toggleInventoryManagement;
    
    // Inventory System
    @FXML private TextField inventoryItemNameField, inventoryQtyField, inventoryUnitField, inventoryMinLevelField, inventorySearchField;
    @FXML private ComboBox<String> inventoryCategoryField;
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, String> colInvName, colInvUnit, colInvStatus, colInvCategory;
    @FXML private TableColumn<InventoryItem, Double> colInvStock, colInvMinLevel;
    
    // Test-Inventory Linking
    @FXML private ComboBox<InventoryItem> testInventoryItemBox;
    @FXML private TextField testInventoryQtyField;
    @FXML private TableView<TestInventoryLink> linkedInventoryTable;
    @FXML private TableColumn<TestInventoryLink, String> colLinkedInvName, colLinkedInvAction;
    @FXML private TableColumn<TestInventoryLink, Double> colLinkedInvQty;

    @FXML
    private TabPane adminTabPane;
    @FXML
    private Tab tabTests, tabStaff, tabOrganization, tabInventory, tabIntegrity;
    @FXML
    private TextField testSearchField;

    private boolean isStaffLoaded = false;
    private boolean isInventorySystemLoaded = false;
    private boolean isOrganizationLoaded = false;
    private boolean isIntegrityLoaded = false;
    private boolean isMicrobiologyLoaded = false;

    private ObservableList<TestParameter> tempParameters = FXCollections.observableArrayList();
    private ObservableList<Staff> staffList = FXCollections.observableArrayList();
    private ObservableList<Test> masterTestList = FXCollections.observableArrayList();
    private ObservableList<InventoryItem> inventoryItems = FXCollections.observableArrayList();
    private ObservableList<TestInventoryLink> linkedInventoryLinks = FXCollections.observableArrayList();
    private Test selectedTest;
    private InventoryItem selectedInventoryItem;
    private Staff selectedStaff;
    private FilteredList<Test> filteredTests;
    @FXML
    private ChoiceBox<String> templateSelector;
    @FXML
    private ChoiceBox<String> cashbookPrinterSelector;
    @FXML
    private Button btnAddInventory;
    private int nextSelectionOrder = 1;
    private String currentStaffPicPath = "";
    private ObservableList<String> standardOrganisms = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ensureInventoryCategoryColumnExists();
        nameField.textProperty().addListener((obs, old, val) -> updateMicroscopicCultureUI());
        // Test Table Init
        filteredTests = new FilteredList<>(masterTestList, p -> true);
        colParamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colParamUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colParamCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colParamRange.setCellValueFactory(new PropertyValueFactory<>("disclosure"));

        // Initialize Protocol Class Dropdown
        testClassBox.setItems(FXCollections.observableArrayList("ROUTINE", "PREMIUM"));
        testClassBox.setValue("ROUTINE");


        colParamSelect.setCellValueFactory(f -> f.getValue().selectedProperty());
        
        colParamSelect.setCellFactory(column -> new TableCell<TestParameter, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(5, checkBox, label);
            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
                checkBox.setOnAction(e -> {
                    TestParameter param = getTableView().getItems().get(getIndex());
                    if (checkBox.isSelected()) {
                        param.setSelected(true);
                        param.setSelectionOrder(nextSelectionOrder++);
                    } else {
                        param.setSelected(false);
                        int oldOrder = param.getSelectionOrder();
                        param.setSelectionOrder(0);
                        // Shift others to fill the gap
                        for (TestParameter other : getTableView().getItems()) {
                            if (other.getSelectionOrder() > oldOrder) {
                                other.setSelectionOrder(other.getSelectionOrder() - 1);
                            }
                        }
                        nextSelectionOrder--;
                    }
                    getTableView().refresh();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    TestParameter p = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(p.isSelected());
                    label.setText(p.getSelectionOrder() > 0 ? String.valueOf(p.getSelectionOrder()) : "");
                    setGraphic(container);
                }
            }
        });

        paramTable.setItems(tempParameters);

        // Auto-select tab if signaled via SessionContext
        javafx.application.Platform.runLater(() -> {
            String target = com.lab.lms.services.SessionContext.getTargetTab();
            if ("inventory".equals(target) && adminTabPane != null && tabInventory != null) {
                adminTabPane.getSelectionModel().select(tabInventory);
                com.lab.lms.services.SessionContext.setTargetTab(null);
            }
        });

        paramTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedParameter = newVal;
                paramNameField.setText(newVal.getName());
                if (isMicroscopicCheck.isSelected() || isCultureCheck.isSelected()) {
                    paramCategoryField.setValue(newVal.getCategory());
                    paramUnitField.setText(newVal.getUnit());
                    if (isCultureCheck.isSelected()) {
                        cultureTypeField.setValue(newVal.getMinRange());
                    }
                } else {
                    paramUnitField.setText(newVal.getUnit());
                }
                paramMinField.setText(newVal.getMinRange());
                paramMaxField.setText(newVal.getMaxRange());
                paramMinKidsField.setText(newVal.getMinRangeKids());
                paramMaxKidsField.setText(newVal.getMaxRangeKids());
                paramMinMaleField.setText(newVal.getMinRangeMale());
                paramMinFemaleField.setText(newVal.getMinRangeFemale());
                paramMaxFemaleField.setText(newVal.getMaxRangeFemale());
                repopulateRanges(containerGeneralRange, extraMinFields, extraMaxFields, newVal.getMinRanges(), newVal.getMaxRanges());
                repopulateRanges(containerKidsRange, extraMinKidsFields, extraMaxKidsFields, newVal.getMinRangesKids(), newVal.getMaxRangesKids());
                repopulateRanges(containerMaleRange, extraMinMaleFields, extraMaxMaleFields, newVal.getMinRangesMale(), newVal.getMaxRangesMale());
                repopulateRanges(containerFemaleRange, extraMinFemaleFields, extraMaxFemaleFields, newVal.getMinRangesFemale(), newVal.getMaxRangesFemale());
                btnAddParameter.setText("UPDATE");
            } else {
                btnAddParameter.setText("ADD");
                selectedParameter = null;
            }
        });

        colNumericCode.setCellValueFactory(new PropertyValueFactory<>("numericCode"));
        colAlphaCode.setCellValueFactory(new PropertyValueFactory<>("alphaCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(column -> new TableCell<Test, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;"); }
            }
        });
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTAT.setCellValueFactory(new PropertyValueFactory<>("resultTime"));

        colAction.setCellFactory(param -> new TableCell<Test, Void>() {
            private final Button deleteBtn = new Button("DELETE");
            private final Button toggleBtn = new Button("MARK PREMIUM");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8, toggleBtn, deleteBtn);
            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
                deleteBtn.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> { Test test = getTableView().getItems().get(getIndex()); selectedTest = test; handleDelete(); });
                toggleBtn.setStyle("-fx-background-color: #455A64; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                toggleBtn.setOnAction(event -> {
                    Test test = getTableView().getItems().get(getIndex());
                    // 0 = Routine (ACTIVE), 1 = Premium (INACTIVE)
                    int nextSpecial = (test.getIsSpecial() == 1) ? 0 : 1;
                    String nextClass = (nextSpecial == 1) ? "INACTIVE" : "ACTIVE";
                    
                    try (Connection conn = DatabaseManager.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("UPDATE tests SET is_special = ?, protocol_class = ? WHERE id = ?")) {
                        pstmt.setInt(1, nextSpecial);
                        pstmt.setString(2, nextClass);
                        pstmt.setInt(3, test.getId());
                        pstmt.executeUpdate();
                        
                        test.setIsSpecial(nextSpecial);
                        test.setProtocolClass(nextClass);
                        getTableView().refresh();
                    } catch (SQLException e) { e.printStackTrace(); }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) { setGraphic(null); }
                else {
                    Test test = getTableView().getItems().get(getIndex());
                    if (test.getIsSpecial() == 0) { // Routine
                        toggleBtn.setText("MARK PREMIUM");
                        toggleBtn.setStyle("-fx-background-color: #455A64; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                    } else { // Premium
                        toggleBtn.setText("MARK ROUTINE");
                        toggleBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                    }
                    setGraphic(container);
                }
            }
        });

        // Test Table Selection Listener
        testTable.setItems(filteredTests);
        testTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) { selectedTest = newVal; populateTestFields(newVal); loadTestInventoryLinks(newVal.getId()); }
        });

        // HIDDEN DIAGNOSTIC: Master Sync Shortcut (CTRL+1) - Using EventFilter for Tab-Wide Reach
        javafx.application.Platform.runLater(() -> {
            if (adminTabPane != null) {
                adminTabPane.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT1) {
                        if (tabTests != null && tabTests.isSelected()) {
                            handleMasterSync();
                            event.consume();
                        }
                    }
                });
            }
        });

        // Test Search
        testSearchField.textProperty().addListener((obs, old, val) -> {
            filteredTests.setPredicate(test -> {
                if (val == null || val.isEmpty()) return true;
                String lower = val.toLowerCase();
                return test.getName().toLowerCase().contains(lower) || test.getCategory().toLowerCase().contains(lower)
                    || (test.getNumericCode() != null && test.getNumericCode().toLowerCase().contains(lower))
                    || (test.getAlphaCode() != null && test.getAlphaCode().toLowerCase().contains(lower));
            });
        });

        // Diagnostic Filter Buttons
        if (btnRoutineFilter != null && btnPremiumFilter != null) {
            btnRoutineFilter.setToggleGroup(diagnosticFilterGroup);
            btnPremiumFilter.setToggleGroup(diagnosticFilterGroup);
            diagnosticFilterGroup.selectedToggleProperty().addListener((obs, old, val) -> {
                if (val == btnRoutineFilter) filteredTests.setPredicate(t -> t.getIsSpecial() == 0);
                else if (val == btnPremiumFilter) filteredTests.setPredicate(t -> t.getIsSpecial() == 1);
                else filteredTests.setPredicate(p -> true);
            });
        }

        // Setup HTMLEditor ribbon
        javafx.application.Platform.runLater(() -> setupHTMLEditorRibbon(testNotesArea, () -> handleInsertMSWordTable()));

        // Load Tests Data
        loadTests();
        loadMicrobiologyKnowledgeBase();

        // Setup Lazy Loading for secondary tabs
        setupLazyLoading();

        cultureTypeField.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("--- ADD CUSTOM ---".equals(newVal)) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Custom Culture Item");
                dialog.setHeaderText("Create New Protocol Type/Specimen");
                dialog.setContentText("Enter Name:");
                dialog.showAndWait().ifPresent(name -> {
                    if (name != null && !name.trim().isEmpty()) {
                        String cleanName = name.trim().toUpperCase();
                        if (!cultureTypeField.getItems().contains(cleanName)) {
                            cultureTypeField.getItems().add(cultureTypeField.getItems().size() - 1, cleanName);
                        }
                        cultureTypeField.getSelectionModel().select(cleanName);
                    } else {
                        cultureTypeField.getSelectionModel().clearSelection();
                    }
                });
            }
        });

        paramCategoryField.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("--- ADD CUSTOM ---".equals(newVal)) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Custom Microorganism/Category");
                dialog.setHeaderText("Create New Diagnostic Heading");
                dialog.setContentText("Enter Name:");
                dialog.showAndWait().ifPresent(name -> {
                    if (name != null && !name.trim().isEmpty()) {
                        String cleanName = name.trim().toUpperCase();
                        if (!paramCategoryField.getItems().contains(cleanName)) {
                            paramCategoryField.getItems().add(paramCategoryField.getItems().size() - 1, cleanName);
                        }
                        paramCategoryField.getSelectionModel().select(cleanName);
                    } else {
                        paramCategoryField.getSelectionModel().clearSelection();
                    }
                });
            }
        });

        isMicroscopicCheck.selectedProperty().addListener((obs, old, val) -> {
            if (val) isCultureCheck.setSelected(false);
            updateMicroscopicCultureUI();
        });
        isCultureCheck.selectedProperty().addListener((obs, old, val) -> {
            if (val) isMicroscopicCheck.setSelected(false);
            updateMicroscopicCultureUI();
        });
        
        updateMicroscopicCultureUI();

        // Load Report Template Path & Initialize Selector
        String currentTemplate = DatabaseManager.getSetting("report_template_path", "DEFAULT (INTERNAL ENGINE)");
        if (templateSelector != null) {
            templateSelector.setConverter(new javafx.util.StringConverter<String>() {
                @Override
                public String toString(String object) {
                    if (object == null) return "";
                    if (object.endsWith(".html") || object.contains(":") || object.contains("\\")) {
                        java.io.File f = new java.io.File(object);
                        if (f.getParentFile() != null && !f.getParentFile().getName().equals("templates")) {
                            return "CUSTOM : " + f.getParentFile().getName().toUpperCase();
                        }
                        return "CUSTOM : " + f.getName().toUpperCase();
                    }
                    return object;
                }
                @Override
                public String fromString(String string) {
                    return null;
                }
            });

            templateSelector.getItems().addAll("DEFAULT (INTERNAL ENGINE)", "MODERN PRECISION (CLINICAL)", "SMART CLINICAL TEMPLATE (ULTRA)", "SHIFA INTERNATIONAL (FIDELITY)");
            if (!currentTemplate.equals("DEFAULT (INTERNAL ENGINE)") && 
                !currentTemplate.equals("MODERN PRECISION (CLINICAL)") && 
                !currentTemplate.equals("SMART CLINICAL TEMPLATE (ULTRA)") &&
                !currentTemplate.equals("SHIFA INTERNATIONAL (FIDELITY)")) {
                templateSelector.getItems().add(currentTemplate);
            }
            templateSelector.setValue(currentTemplate);
            
            templateSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    DatabaseManager.saveSetting("report_template_path", val);
                }
            });
        }

        if (cashbookPrinterSelector != null) {
            cashbookPrinterSelector.getItems().addAll("A4 / LEGAL (FULL)", "THERMAL (RECEIPT)");
            String currentType = DatabaseManager.getSetting("cashbook_printer_type", "A4 / LEGAL (FULL)");
            cashbookPrinterSelector.setValue(currentType);
            cashbookPrinterSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    DatabaseManager.saveSetting("cashbook_printer_type", val);
                }
            });
        }

        headerHeightField.setText(DatabaseManager.getSetting("header_height", "124"));
        footerHeightField.setText(DatabaseManager.getSetting("footer_height", "80"));

        // Lab Brand Settings
        labNameField.setText(DatabaseManager.getSetting("lab_name", "Laboratory Management System"));
        labContactField.setText(DatabaseManager.getSetting("lab_contact", ""));
        labEmailField.setText(DatabaseManager.getSetting("lab_email", ""));
        labWebsiteField.setText(DatabaseManager.getSetting("lab_website", ""));
        labTaglineField.setText(DatabaseManager.getSetting("lab_tagline", "Accurate | Caring | Instant"));
        labAddressArea.setText(DatabaseManager.getSetting("lab_address", ""));
        receiptPoliciesArea.setText(DatabaseManager.getSetting("receipt_policies", ""));
        logoPathLabel.setText(DatabaseManager.getSetting("lab_logo", "No logo uploaded"));
        headerPathLabel.setText(DatabaseManager.getSetting("lab_header", "No header uploaded"));
        footerPathLabel.setText(DatabaseManager.getSetting("lab_footer", "No footer uploaded"));

        // Admin Credentials Init
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT username FROM users WHERE role = 'ADMIN'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                adminUsernameField.setText(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // finalizeInventorySetup(); // MOVED TO LAZY LOADING
    }

    private void ensureInventoryCategoryColumnExists() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            // Check if column exists
            ResultSet rs = conn.getMetaData().getColumns(null, null, "inventory", "category");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE inventory ADD COLUMN category TEXT;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMicrobiologyKnowledgeBase() {
        try (Connection conn = DatabaseManager.getConnection()) {
            java.util.Set<String> specimens = new java.util.LinkedHashSet<>();
            java.util.Set<String> organisms = new java.util.LinkedHashSet<>();
            
            // Strictly database-driven discovery
            String sSql = "SELECT DISTINCT min_range FROM test_parameters tp JOIN tests t ON tp.test_id = t.id WHERE t.is_culture = 1 AND min_range IS NOT NULL AND min_range != ''";
            try (ResultSet rsS = conn.createStatement().executeQuery(sSql)) {
                while (rsS.next()) specimens.add(rsS.getString(1).trim().toUpperCase());
            }
            
            String oSql = "SELECT DISTINCT unit FROM test_parameters tp JOIN tests t ON tp.test_id = t.id WHERE t.is_culture = 1 AND unit IS NOT NULL AND unit != ''";
            try (ResultSet rsO = conn.createStatement().executeQuery(oSql)) {
                while (rsO.next()) organisms.add(rsO.getString(1).trim().toUpperCase());
            }

            ObservableList<String> sList = FXCollections.observableArrayList(specimens);
            sList.add("--- ADD CUSTOM ---");
            cultureTypeField.setItems(sList);

            ObservableList<String> oList = FXCollections.observableArrayList(organisms);
            oList.add("--- ADD CUSTOM ---");
            standardOrganisms.setAll(oList);
            paramCategoryField.setItems(oList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void finalizeInventorySetup() {
        // Inventory Main Table
        colInvName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colInvStock.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colInvUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colInvCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colInvMinLevel.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));
        colInvStatus.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getStatus()));
        inventoryTable.setItems(inventoryItems);
        loadInventory();

        // Populate Categories
        ObservableList<String> invCats = FXCollections.observableArrayList(
            "HEMATOLOGY", "BIOCHEMISTRY", "SEROLOGY", "GENERAL CONSUMABLES", "MICROBIOLOGY", "URINE/DIPSTICKS", "HISTOPATHOLOGY", "--- ADD CUSTOM ---"
        );
        inventoryCategoryField.setItems(invCats);
        inventoryCategoryField.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if ("--- ADD CUSTOM ---".equals(val)) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Custom Category");
                dialog.setHeaderText("Create New Inventory Category");
                dialog.setContentText("Enter Name:");
                dialog.showAndWait().ifPresent(name -> {
                    if (name != null && !name.trim().isEmpty()) {
                        String cleanName = name.trim().toUpperCase();
                        if (!inventoryCategoryField.getItems().contains(cleanName)) {
                            inventoryCategoryField.getItems().add(inventoryCategoryField.getItems().size() - 1, cleanName);
                        }
                        inventoryCategoryField.getSelectionModel().select(cleanName);
                    }
                });
            }
        });

        // Linked Inventory Table (Test Repository)
        colLinkedInvName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colLinkedInvQty.setCellValueFactory(new PropertyValueFactory<>("usageQuantity"));
        colLinkedInvAction.setCellFactory(param -> new TableCell<TestInventoryLink, String>() {
            private final Button btn = new Button("REMOVE");
            {
                btn.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                btn.setOnAction(e -> handleUnlinkInventory(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        linkedInventoryTable.setItems(linkedInventoryLinks);
        testInventoryItemBox.setItems(inventoryItems);

        inventorySearchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                inventoryTable.setItems(inventoryItems);
            } else {
                inventoryTable.setItems(inventoryItems.filtered(i -> i.getItemName().toLowerCase().contains(val.toLowerCase())));
            }
        });

        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedInventoryItem = newVal;
                inventoryItemNameField.setText(newVal.getItemName());
                inventoryQtyField.setText(String.valueOf(newVal.getQuantity()));
                inventoryUnitField.setText(newVal.getUnit());
                inventoryMinLevelField.setText(String.valueOf(newVal.getMinStockLevel()));
                inventoryCategoryField.setValue(newVal.getCategory());
                if (btnAddInventory != null) btnAddInventory.setText("UPDATE STOCK");
            } else {
                selectedInventoryItem = null;
                if (btnAddInventory != null) btnAddInventory.setText("ADD TO STOCK");
            }
        });
    }

    @FXML
    private void handleUploadLogo() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(labNameField.getScene().getWindow());
        if (file != null) {
            logoPathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleUploadTestImage() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(nameField.getScene().getWindow());
        if (file != null) {
            testImagePathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleRemoveTestImage() {
        testImagePathLabel.setText("No image attached");
    }


    @FXML
    private void handleInsertMSWordTable() {
        com.lab.lms.util.UIHelper.showInteractiveTableBuilder(testNotesArea.getScene().getWindow(), tableText -> {
            String old = testNotesArea.getHtmlText();
            testNotesArea.setHtmlText((old == null ? "" : old) + tableText);
            testNotesArea.requestFocus();
        });
    }

    @FXML
    private void handleUploadHeader() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(labNameField.getScene().getWindow());
        if (file != null) {
            headerPathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleUploadFooter() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(labNameField.getScene().getWindow());
        if (file != null) {
            footerPathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleRemoveLogo() { logoPathLabel.setText("No logo uploaded"); }
    @FXML
    private void handleRemoveHeader() { headerPathLabel.setText("No header uploaded"); }
    @FXML
    private void handleRemoveFooter() { footerPathLabel.setText("No footer uploaded"); }
    @FXML
    private void handleRemoveWatermark() { watermarkPathLabel.setText("No watermark uploaded"); }
    @FXML
    private void handleRemoveSignature() { signaturePathLabel.setText("No signature uploaded"); }

    @FXML
    private void handleSaveLabSettings() {
        // Branding
        DatabaseManager.saveSetting("lab_name", labNameField.getText());
        DatabaseManager.saveSetting("lab_contact", labContactField.getText());
        DatabaseManager.saveSetting("lab_email", labEmailField.getText());
        DatabaseManager.saveSetting("lab_website", labWebsiteField.getText());
        DatabaseManager.saveSetting("lab_tagline", labTaglineField.getText());
        DatabaseManager.saveSetting("lab_address", labAddressArea.getText());
        DatabaseManager.saveSetting("receipt_policies", receiptPoliciesArea.getText());
        DatabaseManager.saveSetting("lab_logo", logoPathLabel.getText());
        DatabaseManager.saveSetting("lab_header", headerPathLabel.getText());
        DatabaseManager.saveSetting("lab_footer", footerPathLabel.getText());

        // Report Customization
        DatabaseManager.saveSetting("report_watermark", watermarkPathLabel.getText());
        DatabaseManager.saveSetting("report_pathologist", pathologistDetailsArea.getText());
        DatabaseManager.saveSetting("report_doctors_footer", doctorsFooterArea.getText());
        DatabaseManager.saveSetting("report_note_footer", reportNoteArea.getText());
        DatabaseManager.saveSetting("doctor_signature", signaturePathLabel.getText());

        if (DashboardController.getInstance() != null) DashboardController.getInstance().refreshGlobalUI();

        new Alert(Alert.AlertType.INFORMATION, "Laboratory organizational and profile details updated successfully.").show();
    }

    @FXML
    private void handleUploadWatermark() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(labNameField.getScene().getWindow());
        if (file != null) {
            watermarkPathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSaveReportCustomization() {
        DatabaseManager.saveSetting("report_watermark", watermarkPathLabel.getText());
        DatabaseManager.saveSetting("report_pathologist", pathologistDetailsArea.getText());
        DatabaseManager.saveSetting("report_doctors_footer", doctorsFooterArea.getText());
        DatabaseManager.saveSetting("report_note_footer", reportNoteArea.getText());
        new Alert(Alert.AlertType.INFORMATION, "Report customization details updated successfully.").show();
    }


    @FXML
    private void handleSaveSystemIntegrity() {
        // Save Dimensions
        DatabaseManager.saveSetting("header_height", headerHeightField.getText());
        DatabaseManager.saveSetting("footer_height", footerHeightField.getText());
        
        // Save All Workflow Checkboxes too (Consolidated Save)
        DatabaseManager.saveSetting("enable_specimen_tracking", String.valueOf(toggleSpecimenTracking.isSelected()));
        DatabaseManager.saveSetting("enable_doctor_review", String.valueOf(toggleDoctorReview.isSelected()));
        DatabaseManager.saveSetting("print_header_footer", String.valueOf(togglePrintHeaderFooter.isSelected()));
        DatabaseManager.saveSetting("enable_electronic_verify", String.valueOf(toggleElectronicVerify.isSelected()));
        DatabaseManager.saveSetting("enable_footer", String.valueOf(toggleFooter.isSelected()));
        DatabaseManager.saveSetting("auto_print_billing", String.valueOf(autoPrintBilling.isSelected()));
        DatabaseManager.saveSetting("auto_print_whatsapp", String.valueOf(autoPrintWhatsApp.isSelected()));
        DatabaseManager.saveSetting("enable_premium_tests", String.valueOf(togglePremiumTests.isSelected()));

        DatabaseManager.saveSetting("multi_device_mode", String.valueOf(toggleMultiDeviceMode.isSelected()));
        DatabaseManager.saveSetting("enable_inventory_management", String.valueOf(toggleInventoryManagement.isSelected()));
        
        new Alert(Alert.AlertType.INFORMATION, "System Configuration: Clinical Integrity Suite updated successfully.").show();
    }

    @FXML
    private void handleBrowseBackupLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Backup Folder");
        File selectedDirectory = directoryChooser.showDialog(backupPathLabel.getScene().getWindow());

        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath();
            BackupService.setBackupLocation(path);
            backupPathLabel.setText(path);
            new Alert(Alert.AlertType.INFORMATION, "Backup location set successfully!").show();
        }
    }

    @FXML
    private void handleManualBackup() {
        String path = BackupService.getBackupLocation();
        if (path.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a backup location first.").show();
            return;
        }
        BackupService.performBackup();
        new Alert(Alert.AlertType.INFORMATION, "Manual backup completed successfully.").show();
    }


    @FXML
    private void handleUploadSignature() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Doctor's Signature Image");
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(signaturePathLabel.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Ensure directory exists
                File dir = new File("config/signatures");
                if (!dir.exists())
                    dir.mkdirs();

                // Copy file to local config directory
                File destFile = new File(dir,
                        "doctor_signature_" + System.currentTimeMillis() + "_" + selectedFile.getName());
                java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                String path = destFile.getAbsolutePath();

                DatabaseManager.saveSetting("doctor_signature", path);

                signaturePathLabel.setText(path);
                new Alert(Alert.AlertType.INFORMATION, "Signature uploaded and saved successfully!").show();

            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error uploading signature: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private CheckBox toggleSpecimenTracking;
    @FXML
    private CheckBox toggleDoctorReview;
    @FXML
    private CheckBox togglePrintHeaderFooter;
    @FXML
    private TextField headerHeightField;
    @FXML
    private TextField footerHeightField;
    @FXML
    private CheckBox toggleElectronicVerify;
    @FXML
    private CheckBox toggleFooter;
    @FXML
    private CheckBox autoPrintBilling;
    @FXML
    private CheckBox autoPrintWhatsApp;
    @FXML
    private CheckBox togglePremiumTests;
    @FXML
    private ToggleButton btnRoutineFilter, btnPremiumFilter;
    private ToggleGroup diagnosticFilterGroup = new ToggleGroup();

    @FXML
    private void loadTests() {
        masterTestList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM tests ORDER BY id DESC");
            while (rs.next()) {
                Test test = new Test(
                        rs.getInt("id"),
                        rs.getString("numeric_code"),
                        rs.getString("alpha_code"),
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
                        rs.getString("fasting"),
                        rs.getString("growth_status"),
                        rs.getString("growth_findings"));
                // Load parameters for each test
                PreparedStatement pstmtParams = conn.prepareStatement("SELECT * FROM test_parameters WHERE test_id = ? AND is_global = 1");
                pstmtParams.setInt(1, test.getId());
                ResultSet rsParams = pstmtParams.executeQuery();
                while (rsParams.next()) {
                    TestParameter tp = new TestParameter(
                            rsParams.getInt("id"),
                            rsParams.getInt("test_id"),
                            rsParams.getString("name"),
                            rsParams.getString("unit"),
                            rsParams.getString("min_range"),
                            rsParams.getString("max_range"),
                            rsParams.getString("min_range_male"),
                            rsParams.getString("max_range_male"),
                            rsParams.getString("min_range_female"),
                            rsParams.getString("max_range_female"),
                            rsParams.getString("min_range_kids"),
                            rsParams.getString("max_range_kids"),
                            rsParams.getString("secondary_ranges")
                    );
                    tp.setCategory(rsParams.getString("category"));
                    test.addParameter(tp);
                }
                masterTestList.add(test);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyFilters();
    }

    private void setupLazyLoading() {
        adminTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabStaff) loadStaffTabData();
            else if (newTab == tabInventory) loadInventoryTabData();
            else if (newTab == tabOrganization) loadOrganizationTabData();
            else if (newTab == tabIntegrity) loadIntegrityTabData();
        });
    }

    private void populateTestFields(Test test) {
        if (test == null) return;
        nameField.setText(test.getName());
        numericCodeField.setText(test.getNumericCode());
        alphaCodeField.setText(test.getAlphaCode());
        categoryField.setValue(test.getCategory());
        priceField.setText(String.valueOf(test.getPrice()));
        resultTimeField.setText(test.getResultTime());
        // Map Protocol Class to UI Display
        String uiClass = "INACTIVE".equalsIgnoreCase(test.getProtocolClass()) ? "PREMIUM" : "ROUTINE";
        testClassBox.setValue(uiClass);
        specimenField.setText(test.getSpecimen());
        isSpecialCheck.setSelected(test.getIsSpecial() == 1);
        isMicroscopicCheck.setSelected(test.getIsMicroscopic() == 1);
        isCultureCheck.setSelected(test.getIsCulture() == 1);
        testNotesArea.setHtmlText(test.getNotes() != null ? test.getNotes() : "");
        testImagePathLabel.setText(test.getImagePath() != null ? test.getImagePath() : "No image attached");
        loadTestParameters(test.getId());
        updateMicroscopicCultureUI();
    }

    private void loadStaffTabData() {
        if (isStaffLoaded) return;
        colStaffId.setCellValueFactory(new PropertyValueFactory<>("staffId"));
        colStaffName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStaffDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colStaffQual.setCellValueFactory(new PropertyValueFactory<>("qualification"));
        colStaffPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStaffDob.setCellValueFactory(new PropertyValueFactory<>("dob"));
        colStaffGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colStaffAction.setCellFactory(param -> new TableCell<Staff, Void>() {
            private final Button printBtn = new Button("PRINT CARD");
            {
                printBtn.setStyle("-fx-background-color: #37474F; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 3; -fx-cursor: hand;");
                printBtn.setOnAction(event -> handlePrintStaffCard(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else { setGraphic(printBtn); setAlignment(javafx.geometry.Pos.CENTER); }
            }
        });
        loadStaff();
        staffTable.setItems(staffList);
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) { selectedStaff = newVal; handleEditStaff(); }
        });
        isStaffLoaded = true;
    }

    private void loadInventoryTabData() {
        if (isInventorySystemLoaded) return;
        finalizeInventorySetup();
        isInventorySystemLoaded = true;
    }

    private void loadOrganizationTabData() {
        if (isOrganizationLoaded) return;
        isOrganizationLoaded = true;
    }

    private void loadIntegrityTabData() {
        if (isIntegrityLoaded) return;
        isIntegrityLoaded = true;
    }


    private void loadTestParameters(int testId) {
        tempParameters.clear();
        nextSelectionOrder = 1;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM test_parameters WHERE test_id = ? AND is_global = 1 ORDER BY print_order ASC");
            pstmt.setInt(1, testId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                TestParameter tp = new TestParameter(
                        rs.getInt("id"),
                        rs.getInt("test_id"),
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getString("min_range"),
                        rs.getString("max_range"),
                        rs.getString("min_range_male"),
                        rs.getString("max_range_male"),
                        rs.getString("min_range_female"),
                        rs.getString("max_range_female"),
                        rs.getString("min_range_kids"),
                        rs.getString("max_range_kids"),
                        rs.getString("secondary_ranges")
                );
                tp.setCategory(rs.getString("category"));
                
                int order = rs.getInt("print_order");
                if (order > 0) {
                    tp.setSelected(true);
                    tp.setSelectionOrder(order);
                    if (order >= nextSelectionOrder) nextSelectionOrder = order + 1;
                }
                tempParameters.add(tp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddParameter() {
        if (paramNameField.getText().isEmpty())
            return;
        
        String unitValue = paramUnitField.getText();
        String categoryValue = (isMicroscopicCheck.isSelected() || isCultureCheck.isSelected()) ? paramCategoryField.getEditor().getText() : "";
        String culturalValue = cultureTypeField.getEditor().getText();

        if (isMicroscopicCheck.isSelected() || isCultureCheck.isSelected()) {
            String typed = paramCategoryField.getEditor().getText();
            if (typed != null && !typed.trim().isEmpty() && 
                !typed.equals("--- ADD CUSTOM ---") &&
                !paramCategoryField.getItems().contains(typed)) {
                paramCategoryField.getItems().add(paramCategoryField.getItems().size() - 1, typed);
            }
        }

        if (selectedParameter != null) {
            selectedParameter.setName(paramNameField.getText());
            selectedParameter.setUnit(unitValue);
            selectedParameter.setCategory(categoryValue);
            selectedParameter.setMinRange(isCultureCheck.isSelected() ? culturalValue : paramMinField.getText());
            selectedParameter.setMaxRange(paramMaxField.getText());
            selectedParameter.setMinRangeKids(paramMinKidsField.getText());
            selectedParameter.setMaxRangeKids(paramMaxKidsField.getText());
            selectedParameter.setMinRangeMale(paramMinMaleField.getText());
            
            selectedParameter.getMinRanges().clear(); selectedParameter.getMaxRanges().clear();
            selectedParameter.getMinRanges().add(isCultureCheck.isSelected() ? culturalValue : paramMinField.getText());
            selectedParameter.getMaxRanges().add(paramMaxField.getText());
            collectExtraRanges(extraMinFields, extraMaxFields, selectedParameter.getMinRanges(), selectedParameter.getMaxRanges());

            selectedParameter.getMinRangesKids().clear(); selectedParameter.getMaxRangesKids().clear();
            selectedParameter.getMinRangesKids().add(paramMinKidsField.getText());
            selectedParameter.getMaxRangesKids().add(paramMaxKidsField.getText());
            collectExtraRanges(extraMinKidsFields, extraMaxKidsFields, selectedParameter.getMinRangesKids(), selectedParameter.getMaxRangesKids());

            selectedParameter.getMinRangesMale().clear(); selectedParameter.getMaxRangesMale().clear();
            selectedParameter.getMinRangesMale().add(paramMinMaleField.getText());
            selectedParameter.getMaxRangesMale().add(paramMaxMaleField.getText());
            collectExtraRanges(extraMinMaleFields, extraMaxMaleFields, selectedParameter.getMinRangesMale(), selectedParameter.getMaxRangesMale());

            selectedParameter.getMinRangesFemale().clear(); selectedParameter.getMaxRangesFemale().clear();
            selectedParameter.getMinRangesFemale().add(paramMinFemaleField.getText());
            selectedParameter.getMaxRangesFemale().add(paramMaxFemaleField.getText());
            collectExtraRanges(extraMinFemaleFields, extraMaxFemaleFields, selectedParameter.getMinRangesFemale(), selectedParameter.getMaxRangesFemale());

            paramTable.refresh();
            selectedParameter = null;
            btnAddParameter.setText("ADD");
        } else {
            // Adding new parameter
            String name = paramNameField.getText().trim();
            String unit = unitValue.trim();
            
            TestParameter param = new TestParameter(0, 0, name, unit);
            param.setCategory(categoryValue);
            param.getMinRanges().add(isCultureCheck.isSelected() ? culturalValue : paramMinField.getText());
            param.getMaxRanges().add(paramMaxField.getText());
            collectExtraRanges(extraMinFields, extraMaxFields, param.getMinRanges(), param.getMaxRanges());

            param.getMinRangesKids().add(paramMinKidsField.getText());
            param.getMaxRangesKids().add(paramMaxKidsField.getText());
            collectExtraRanges(extraMinKidsFields, extraMaxKidsFields, param.getMinRangesKids(), param.getMaxRangesKids());

            param.getMinRangesMale().add(paramMinMaleField.getText());
            param.getMaxRangesMale().add(paramMaxMaleField.getText());
            collectExtraRanges(extraMinMaleFields, extraMaxMaleFields, param.getMinRangesMale(), param.getMaxRangesMale());

            param.getMinRangesFemale().add(paramMinFemaleField.getText());
            param.getMaxRangesFemale().add(paramMaxFemaleField.getText());
            collectExtraRanges(extraMinFemaleFields, extraMaxFemaleFields, param.getMinRangesFemale(), param.getMaxRangesFemale());

            tempParameters.add(param);
        }
        
        // Reset and clear containers
        resetParamForm();
    }

    private void collectExtraRanges(List<TextField> mins, List<TextField> maxs, List<String> targetMins, List<String> targetMaxs) {
        for (int i = 0; i < mins.size(); i++) {
            String mn = mins.get(i).getText().trim();
            String mx = maxs.get(i).getText().trim();
            if (!mn.isEmpty() || !mx.isEmpty()) {
                targetMins.add(mn);
                targetMaxs.add(mx);
            }
        }
    }

    private void resetParamForm() {
        paramNameField.clear();
        if (!isCultureCheck.isSelected() && !isMicroscopicCheck.isSelected()) {
            paramUnitField.clear();
            cultureTypeField.getEditor().clear();
            paramCategoryField.getEditor().clear();
        } else {
             // For culture/microscopic tests, keep specimen/bacteria active for the next drug/parameter addition
        }
        paramMinField.clear();
        paramMaxField.clear();
        paramMinKidsField.clear();
        paramMaxKidsField.clear();
        paramMinMaleField.clear();
        paramMaxMaleField.clear();
        paramMinFemaleField.clear();
        paramMaxFemaleField.clear();
        
        clearExtraContainer(containerGeneralRange, extraMinFields, extraMaxFields);
        clearExtraContainer(containerKidsRange, extraMinKidsFields, extraMaxKidsFields);
        clearExtraContainer(containerMaleRange, extraMinMaleFields, extraMaxMaleFields);
        clearExtraContainer(containerFemaleRange, extraMinFemaleFields, extraMaxFemaleFields);
        
        paramNameField.requestFocus();
    }

    private void clearExtraContainer(VBox container, List<TextField> minList, List<TextField> maxList) {
        while (container.getChildren().size() > 1) {
            container.getChildren().remove(1);
        }
        minList.clear();
        maxList.clear();
    }

    private void repopulateRanges(VBox container, List<TextField> minList, List<TextField> maxList, List<String> dataMins, List<String> dataMaxs) {
        clearExtraContainer(container, minList, maxList);
        for (int i = 1; i < dataMins.size(); i++) {
            appendRangeField(container, minList, maxList);
            minList.get(i-1).setText(dataMins.get(i));
            if (i < dataMaxs.size()) maxList.get(i-1).setText(dataMaxs.get(i));
        }
    }

    @FXML
    private void handleRemoveParameter() {
        TestParameter selected = paramTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tempParameters.remove(selected);
        }
    }

    @FXML
    private void handlePreviewTest() {
        String name = nameField.getText();
        if (name == null || name.strip().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Preview Protocol", "Please enter a Diagnostic Name to view layout.");
            return;
        }

        java.util.List<java.util.Map<String, String>> params = new java.util.ArrayList<>();
        for (TestParameter p : tempParameters) {
            java.util.Map<String, String> row = new java.util.HashMap<>();
            row.put("name", p.getName());
            row.put("unit", p.getUnit());
            row.put("category", p.getCategory());
            row.put("range", p.getRange());
            row.put("value", "RESULT"); // Static placeholder for layout visualization
            params.add(row);
        }

        int isSp = isSpecialCheck.isSelected() ? 1 : 0;
        int isMic = isMicroscopicCheck.isSelected() ? 1 : 0;
        int isCul = isCultureCheck.isSelected() ? 1 : 0;
        String spec = specimenField.getText();
        String notes = testNotesArea.getHtmlText();

        String pdfPath = com.lab.lms.services.ReportGenerator.generatePreview(name, isSp, isMic, isCul, spec, notes, params);
        if (pdfPath != null) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                try {
                    java.awt.Desktop.getDesktop().open(pdfFile);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "System Error", "Failed to launch PDF viewer: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText();
        if (name == null || name.isEmpty())
            return;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int testId = -1;
                if (selectedTest == null) {
                    // Insert Test Protocol
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO tests (name, category, price, result_time, notes, is_special, is_microscopic, is_culture, specimen, image_path, protocol_class, numeric_code, alpha_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    pstmt.setString(1, name);
                    pstmt.setString(2, categoryField.getValue() != null ? categoryField.getValue() : "");
                    pstmt.setDouble(3, Double.parseDouble(priceField.getText()));
                    String finalTat = resultTimeField.getText().trim() + " " + tatUnitBox.getValue();
                    pstmt.setString(4, finalTat);
                    pstmt.setString(5, testNotesArea.getHtmlText());
                    pstmt.setInt(7, isMicroscopicCheck.isSelected() ? 1 : 0);
                    pstmt.setInt(8, isCultureCheck.isSelected() ? 1 : 0);
                    pstmt.setString(9, specimenField.getText() == null || specimenField.getText().isEmpty() ? "Blood" : specimenField.getText());
                    String imgPath = testImagePathLabel.getText();
                    pstmt.setString(10, "No image attached".equals(imgPath) ? "" : imgPath);
                    String selectedClass = testClassBox.getValue() != null ? testClassBox.getValue() : "ROUTINE";
                    int finalIsSpecial = "PREMIUM".equalsIgnoreCase(selectedClass) ? 1 : 0;
                    String finalProtocolClass = (finalIsSpecial == 1) ? "INACTIVE" : "ACTIVE";

                    pstmt.setInt(6, finalIsSpecial); // is_special
                    pstmt.setString(11, finalProtocolClass); // protocol_class
                    pstmt.setString(12, numericCodeField.getText());
                    pstmt.setString(13, alphaCodeField.getText());
                    pstmt.executeUpdate();

                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next())
                        testId = rs.getInt(1);
                    else
                        throw new SQLException("Protocol ID generation failed");
                } else {
                    // Update Test Protocol
                    testId = selectedTest.getId();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE tests SET name = ?, category = ?, price = ?, result_time = ?, notes = ?, is_special = ?, is_microscopic = ?, is_culture = ?, specimen = ?, image_path = ?, protocol_class = ?, numeric_code = ?, alpha_code = ? WHERE id = ?");
                    pstmt.setString(1, name);
                    pstmt.setString(2, categoryField.getValue() != null ? categoryField.getValue() : "");
                    pstmt.setDouble(3, Double.parseDouble(priceField.getText()));
                    String finalTat = resultTimeField.getText().trim() + " " + tatUnitBox.getValue();
                    pstmt.setString(4, finalTat);
                    pstmt.setString(5, testNotesArea.getHtmlText());
                    pstmt.setString(9, specimenField.getText() == null || specimenField.getText().isEmpty() ? "Blood" : specimenField.getText());
                    String imgPath = testImagePathLabel.getText();
                    pstmt.setString(10, "No image attached".equals(imgPath) ? "" : imgPath);
                    String selectedClass = testClassBox.getValue() != null ? testClassBox.getValue() : "ROUTINE";
                    int finalIsSpecial = "PREMIUM".equalsIgnoreCase(selectedClass) ? 1 : 0;
                    String finalProtocolClass = (finalIsSpecial == 1) ? "INACTIVE" : "ACTIVE";

                    pstmt.setInt(6, finalIsSpecial); // is_special
                    pstmt.setString(11, finalProtocolClass); // protocol_class
                    pstmt.setString(12, numericCodeField.getText());
                    pstmt.setString(13, alphaCodeField.getText());
                    pstmt.setInt(14, testId);
                    pstmt.executeUpdate();
                }

                // [CONSISTENT PERSISTENCE SYNC]
                List<TestParameter> currentInDbList = new ArrayList<>();
                try (PreparedStatement checkPs = conn.prepareStatement("SELECT id, name FROM test_parameters WHERE test_id = ? AND is_global = 1")) {
                    checkPs.setInt(1, testId);
                    try (ResultSet rsCheck = checkPs.executeQuery()) {
                        while (rsCheck.next()) {
                            currentInDbList.add(new TestParameter(rsCheck.getInt("id"), testId, rsCheck.getString("name"), "-"));
                        }
                    }
                }

                List<Integer> idsToKeepStore = new ArrayList<>();
                for (TestParameter uiParam : tempParameters) {
                    for (TestParameter dbParam : currentInDbList) {
                        if ((uiParam.getId() > 0 && dbParam.getId() == uiParam.getId()) || 
                            (uiParam.getId() <= 0 && dbParam.getName().equalsIgnoreCase(uiParam.getName()))) {
                            idsToKeepStore.add(dbParam.getId());
                            break;
                        }
                    }
                }

                StringBuilder purgeSql = new StringBuilder("DELETE FROM test_parameters WHERE test_id = ? AND is_global = 1");
                if (!idsToKeepStore.isEmpty()) {
                    purgeSql.append(" AND id NOT IN (");
                    for (int i = 0; i < idsToKeepStore.size(); i++) {
                        purgeSql.append(idsToKeepStore.get(i));
                        if (i < idsToKeepStore.size() - 1) purgeSql.append(",");
                    }
                    purgeSql.append(")");
                }
                try (PreparedStatement delPs = conn.prepareStatement(purgeSql.toString())) {
                    delPs.setInt(1, testId);
                    delPs.executeUpdate();
                }

                String paramInSql = "INSERT INTO test_parameters (test_id, name, unit, min_range, max_range, min_range_male, max_range_male, min_range_female, max_range_female, min_range_kids, max_range_kids, print_order, is_global, secondary_ranges, category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";
                String paramUpSql = "UPDATE test_parameters SET name = ?, unit = ?, min_range = ?, max_range = ?, min_range_male = ?, max_range_male = ?, min_range_female = ?, max_range_female = ?, min_range_kids = ?, max_range_kids = ?, print_order = ?, secondary_ranges = ?, category = ? WHERE id = ?";
                
                try (PreparedStatement upPs = conn.prepareStatement(paramUpSql);
                     PreparedStatement inPs = conn.prepareStatement(paramInSql)) {
                    
                    for (int k = 0; k < tempParameters.size(); k++) {
                        TestParameter p = tempParameters.get(k);
                        TestParameter existingMatch = null;
                        for (TestParameter dbP : currentInDbList) {
                            if ((p.getId() > 0 && dbP.getId() == p.getId()) || 
                                (p.getId() <= 0 && dbP.getName().equalsIgnoreCase(p.getName()))) {
                                existingMatch = dbP;
                                break;
                            }
                        }

                        if (existingMatch != null) {
                            upPs.setString(1, p.getName());
                            upPs.setString(2, p.getUnit());
                            upPs.setString(3, String.join("\n", p.getMinRanges()));
                            upPs.setString(4, String.join("\n", p.getMaxRanges()));
                            upPs.setString(5, String.join("\n", p.getMinRangesMale()));
                            upPs.setString(6, String.join("\n", p.getMaxRangesMale()));
                            upPs.setString(7, String.join("\n", p.getMinRangesFemale()));
                            upPs.setString(8, String.join("\n", p.getMaxRangesFemale()));
                            upPs.setString(9, String.join("\n", p.getMinRangesKids()));
                            upPs.setString(10, String.join("\n", p.getMaxRangesKids()));
                            upPs.setInt(11, k + 1); 
                            upPs.setString(12, p.getSecondaryRanges());
                            upPs.setString(13, p.getCategory());
                            upPs.setInt(14, existingMatch.getId());
                            upPs.addBatch();
                        } else {
                            inPs.setInt(1, testId);
                            inPs.setString(2, p.getName());
                            inPs.setString(3, p.getUnit());
                            inPs.setString(4, String.join("\n", p.getMinRanges()));
                            inPs.setString(5, String.join("\n", p.getMaxRanges()));
                            inPs.setString(6, String.join("\n", p.getMinRangesMale()));
                            inPs.setString(7, String.join("\n", p.getMaxRangesMale()));
                            inPs.setString(8, String.join("\n", p.getMinRangesFemale()));
                            inPs.setString(9, String.join("\n", p.getMaxRangesFemale()));
                            inPs.setString(10, String.join("\n", p.getMinRangesKids()));
                            inPs.setString(11, String.join("\n", p.getMaxRangesKids()));
                            inPs.setInt(12, k + 1);
                            inPs.setString(13, p.getSecondaryRanges());
                            inPs.setString(14, p.getCategory());
                            inPs.addBatch();
                        }
                    }
                    upPs.executeBatch();
                    inPs.executeBatch();
                }

                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Protocol configuration saved successfully.").show();
                clearForm();
                loadTests();
            } catch (Exception exInternal) {
                conn.rollback();
                throw exInternal;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception exGlobal) {
            new Alert(Alert.AlertType.ERROR, "Critical Persistence Error: " + exGlobal.getMessage()).show();
            exGlobal.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedTest == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
            "Are you sure you want to PERMANENTLY DELETE " + selectedTest.getName() + "?\n\nThis cannot be undone and will remove all associated parameters.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("CONFIRM DIAGNOSTIC DELETION");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseManager.getConnection()) {
                    PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tests WHERE id = ?");
                    pstmt.setInt(1, selectedTest.getId());
                    pstmt.executeUpdate();

                    clearForm();
                    loadTests();
                } catch (SQLException exDel) {
                    new Alert(Alert.AlertType.ERROR, "Deletion failed: " + exDel.getMessage()).show();
                    exDel.printStackTrace();
                }
            }
        });
    }

    private void loadStaff() {
        staffList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM staff");
            while (rs.next()) {
                staffList.add(new Staff(
                        rs.getString("staff_id"),
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("address"),
                        rs.getString("qualification"),
                        rs.getString("phone"),
                        rs.getString("dob"),
                        rs.getString("password"),
                        rs.getString("permissions"),
                        rs.getString("designation"),
                        rs.getString("profile_picture")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditStaff() {
        if (selectedStaff == null)
            return;

        staffNameField.setText(selectedStaff.getName());
        staffAddressField.setText(selectedStaff.getAddress());
        staffPhoneField.setText(selectedStaff.getPhone());
        staffGenderBox.setValue(selectedStaff.getGender());
        staffQualField.setText(selectedStaff.getQualification());
        staffPassField.setText(selectedStaff.getPassword());
        staffDesignationField.setText(selectedStaff.getDesignation());
        
        // Handle profile picture preview
        String picPath = selectedStaff.getProfilePicture();
        currentStaffPicPath = picPath != null ? picPath : "";
        staffImagePathLabel.setText(currentStaffPicPath.isEmpty() ? "No image attached" : new java.io.File(currentStaffPicPath).getName());
        if (!currentStaffPicPath.isEmpty()) {
            java.io.File file = new java.io.File(currentStaffPicPath);
            if (file.exists()) {
                staffImageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            } else {
                staffImageView.setImage(null);
            }
        } else {
            staffImageView.setImage(null);
        }
        if (selectedStaff.getDob() != null && !selectedStaff.getDob().isEmpty()) {
            staffDobPicker.setValue(java.time.LocalDate.parse(selectedStaff.getDob()));
        }

        // Set permissions checkboxes
        String perms = selectedStaff.getPermissions();
        if (perms == null) perms = "";
        permDashboard.setSelected(perms.contains("dashboard"));
        permRegistration.setSelected(perms.contains("registration"));
        permProcessing.setSelected(perms.contains("processing"));
        permBilling.setSelected(perms.contains("billing"));
        permInventory.setSelected(perms.contains("inventory"));
        permAuthorization.setSelected(perms.contains("authorization"));
        permConfiguration.setSelected(perms.contains("configuration"));
        permTestDeletion.setSelected(perms.contains("test_deletion"));
    }
    @FXML
    private void handleSaveStaff() {
        String name = staffNameField.getText();
        if (name.isEmpty()) return;

        // Collect permissions
        StringBuilder sb = new StringBuilder();
        if (permDashboard.isSelected()) sb.append("dashboard,");
        if (permRegistration.isSelected()) sb.append("registration,");
        if (permProcessing.isSelected()) sb.append("processing,");
        if (permBilling.isSelected()) sb.append("billing,");
        if (permInventory.isSelected()) sb.append("inventory,");
        if (permAuthorization.isSelected()) sb.append("authorization,");
        if (permConfiguration.isSelected()) sb.append("configuration,");
        if (permTestDeletion.isSelected()) sb.append("test_deletion,");
        String perms = sb.toString();

        try (Connection conn = DatabaseManager.getConnection()) {
            if (selectedStaff == null) {
                // Insert New Staff
                String staffId = "STF-" + System.currentTimeMillis() % 10000;
                String sql = "INSERT INTO staff (staff_id, name, gender, address, qualification, phone, dob, password, permissions, designation, profile_picture) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, staffId);
                pstmt.setString(2, name);
                pstmt.setString(3, staffGenderBox.getValue());
                pstmt.setString(4, staffAddressField.getText());
                pstmt.setString(5, staffQualField.getText());
                pstmt.setString(6, staffPhoneField.getText());
                pstmt.setString(7, staffDobPicker.getValue() != null ? staffDobPicker.getValue().toString() : "");
                pstmt.setString(8, staffPassField.getText());
                pstmt.setString(9, perms);
                pstmt.setString(10, staffDesignationField.getText());
                pstmt.setString(11, currentStaffPicPath);
                pstmt.executeUpdate();

                // Also add to users table for login - Use Full Name as Username
                String userSql = "INSERT INTO users (username, password, role, staff_id) VALUES (?, ?, ?, ?)";
                PreparedStatement userPstmt = conn.prepareStatement(userSql);
                userPstmt.setString(1, name.trim());
                userPstmt.setString(2, staffPassField.getText());
                userPstmt.setString(3, "TECHNICIAN");
                userPstmt.setString(4, staffId);
                userPstmt.executeUpdate();
            } else {
                // Update Existing Staff
                String sql = "UPDATE staff SET name=?, gender=?, address=?, qualification=?, phone=?, dob=?, password=?, permissions=?, designation=?, profile_picture=? WHERE staff_id=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, staffGenderBox.getValue());
                pstmt.setString(3, staffAddressField.getText());
                pstmt.setString(4, staffQualField.getText());
                pstmt.setString(5, staffPhoneField.getText());
                pstmt.setString(6, staffDobPicker.getValue() != null ? staffDobPicker.getValue().toString() : "");
                pstmt.setString(7, staffPassField.getText());
                pstmt.setString(8, perms);
                pstmt.setString(9, staffDesignationField.getText());
                pstmt.setString(10, currentStaffPicPath);
                pstmt.setString(11, selectedStaff.getStaffId());
                pstmt.executeUpdate();

                // Update Users table too
                String userSql = "UPDATE users SET username=?, password=? WHERE staff_id=?";
                PreparedStatement userPstmt = conn.prepareStatement(userSql);
                userPstmt.setString(1, name.trim());
                userPstmt.setString(2, staffPassField.getText());
                userPstmt.setString(3, selectedStaff.getStaffId());
                userPstmt.executeUpdate();
            }

            clearStaffForm();
            loadStaff();
            new Alert(Alert.AlertType.INFORMATION, "Staff saved successfully!").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleClearStaff() {
        clearStaffForm();
    }

    @FXML
    private void handleDeleteStaff() {
        if (selectedStaff == null)
            return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM staff WHERE staff_id = ?");
            pstmt.setString(1, selectedStaff.getStaffId());
            pstmt.executeUpdate();

            // Delete from users too
            PreparedStatement userPstmt = conn.prepareStatement("DELETE FROM users WHERE staff_id = ?");
            userPstmt.setString(1, selectedStaff.getStaffId());
            userPstmt.executeUpdate();

            clearStaffForm();
            loadStaff();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadStaffPic() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose Staff Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(staffNameField.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Ensure directory exists
                java.io.File dir = new java.io.File(".lablms/staff_pics");
                if (!dir.exists()) dir.mkdirs();

                // Create a unique filename
                String fileName = "staff_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                java.io.File destFile = new java.io.File(dir, fileName);
                
                // Copy file
                java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                currentStaffPicPath = destFile.getAbsolutePath();
                staffImagePathLabel.setText(destFile.getName());
                staffImageView.setImage(new javafx.scene.image.Image(destFile.toURI().toString()));
            } catch (java.io.IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Upload Failed", "Could not copy image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handlePrintStaffCard() {
        if (selectedStaff == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a staff member from the directory first.");
            return;
        }
        handlePrintStaffCard(selectedStaff);
    }

    private void handlePrintStaffCard(Staff staff) {
        if (staff == null) return;
        
        try {
            String pdfPath = ReportGenerator.generateEmployeeCard(staff);
            if (pdfPath != null) {
                java.io.File file = new java.io.File(pdfPath);
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(file.toURI());
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Card Generated", "Employee card saved at: " + pdfPath);
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Generation Failed", "Could not generate the employee card PDF.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "An error occurred while printing the card: " + e.getMessage());
        }
    }

    private void clearStaffForm() {
        selectedStaff = null;
        staffNameField.clear();
        staffAddressField.clear();
        staffPhoneField.clear();
        staffGenderBox.getSelectionModel().clearSelection();
        staffDobPicker.setValue(null);
        staffQualField.clear();
        staffPassField.clear();
        staffDesignationField.clear();
        staffImageView.setImage(null);
        staffImagePathLabel.setText("No image attached");
        currentStaffPicPath = "";
        staffTable.getSelectionModel().clearSelection();

        permDashboard.setSelected(true);
        permRegistration.setSelected(true);
        permProcessing.setSelected(true);
        permBilling.setSelected(true);
        permInventory.setSelected(false);
        permAuthorization.setSelected(false);
        permConfiguration.setSelected(false);
        permTestDeletion.setSelected(false);
    }

    @FXML
    private void handleUpdateAdminCredentials() {
        String newUsername = adminUsernameField.getText().trim();
        String newPass = adminPasswordField.getText();
        String confirmPass = adminConfirmPasswordField.getText();

        if (newUsername.isEmpty() || newPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Security Validation", "Username and Password cannot be empty.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Security Validation",
                    "Passwords do not match. Please verify credentials.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "UPDATE users SET username = ?, password = ? WHERE role = 'ADMIN'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newUsername);
            pstmt.setString(2, newPass);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Security Update Complete",
                        "Administrative root credentials have been successfully rotated.");
                adminPasswordField.clear();
                adminConfirmPasswordField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "System Error", "Failed to update administrator credentials.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Fault", "Secure update protocol failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMasterSync() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/test_seeding.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Global Clinical Engine Synchronization");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Refresh dashboard data after synchronization
            loadTests();
            if (isStaffLoaded) loadStaff();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Synchronization Critical Failure", 
                "An error occurred during global synchronization: " + e.getMessage());
        }
    }

    @FXML
    private void handleForceSeed() {
        handleMasterSync(); // Map existing button to the new comprehensive sync
    }

    @FXML
    private void handleImportTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Clinical Report Template");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Template Package (ZIP)", "*.zip"),
            new FileChooser.ExtensionFilter("Template Files", "*.xml", "*.jrxml", "*.pdf", "*.html"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(labNameField.getScene().getWindow());
        if (selectedFile != null) {
            try {
                if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                    extractZipTemplate(selectedFile);
                } else {
                    File templateDir = new File("config/templates");
                    templateDir.mkdirs();
                    File dest = new File(templateDir, selectedFile.getName());
                    java.nio.file.Files.copy(selectedFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    String path = dest.getAbsolutePath();
                    DatabaseManager.saveSetting("report_template_path", path);
                    if (!templateSelector.getItems().contains(path)) {
                        templateSelector.getItems().add(path);
                    }
                    templateSelector.setValue(path);
                    
                    showAlert(Alert.AlertType.INFORMATION, "Template Imported", "Custom template imported and activated.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Import Error", "Failed to import template: " + e.getMessage());
            }
        }
    }

    private void extractZipTemplate(File zipFile) throws Exception {
        String baseName = zipFile.getName().substring(0, zipFile.getName().lastIndexOf('.'));
        File extractDir = new File("config/templates/" + baseName);
        if (!extractDir.exists()) extractDir.mkdirs();

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(extractDir, zipEntry.getName());
                if (!newFile.getCanonicalPath().startsWith(extractDir.getCanonicalPath() + File.separator)) {
                    throw new java.io.IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new java.io.IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new java.io.IOException("Failed to create directory " + parent);
                    }
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        File metadataFile = new File(extractDir, "metadata.json");
        if (metadataFile.exists()) {
            String content = new String(java.nio.file.Files.readAllBytes(metadataFile.toPath()));
            org.json.JSONObject metadata = new org.json.JSONObject(content);
            
            if (metadata.has("headerHeight")) {
                DatabaseManager.saveSetting("header_height", metadata.getString("headerHeight"));
                headerHeightField.setText(metadata.getString("headerHeight"));
            }
            if (metadata.has("footerHeight")) {
                DatabaseManager.saveSetting("footer_height", metadata.getString("footerHeight"));
                footerHeightField.setText(metadata.getString("footerHeight"));
            }
            if (metadata.has("reportNotes")) {
                DatabaseManager.saveSetting("report_note_footer", metadata.getString("reportNotes"));
                reportNoteArea.setText(metadata.getString("reportNotes"));
            }
            if (metadata.has("pathologistDetails")) {
                DatabaseManager.saveSetting("report_pathologist", metadata.getString("pathologistDetails"));
                pathologistDetailsArea.setText(metadata.getString("pathologistDetails"));
            }
            if (metadata.has("doctorsFooter")) {
                DatabaseManager.saveSetting("report_doctors_footer", metadata.getString("doctorsFooter"));
                doctorsFooterArea.setText(metadata.getString("doctorsFooter"));
            }
            if (metadata.has("qrEnabled")) {
                DatabaseManager.saveSetting("report_qr_enabled", String.valueOf(metadata.getBoolean("qrEnabled")));
            }
            if (metadata.has("qrDataType")) {
                DatabaseManager.saveSetting("report_qr_type", metadata.getString("qrDataType"));
            }
            if (metadata.has("qrCustomData")) {
                DatabaseManager.saveSetting("report_qr_custom", metadata.getString("qrCustomData"));
            }
            if (metadata.has("qrPosition")) {
                DatabaseManager.saveSetting("report_qr_position", metadata.getString("qrPosition"));
            }
            if (metadata.has("baseFontSize")) {
                DatabaseManager.saveSetting("report_font_size", metadata.getString("baseFontSize"));
            }
            if (metadata.has("pagePadding")) {
                DatabaseManager.saveSetting("report_page_padding", metadata.getString("pagePadding"));
            }

            if (metadata.has("assets")) {
                org.json.JSONObject assets = metadata.getJSONObject("assets");
                if (assets.has("header")) {
                    File img = new File(extractDir, "assets/" + assets.getString("header"));
                    DatabaseManager.saveSetting("lab_header", img.getAbsolutePath());
                    headerPathLabel.setText(img.getAbsolutePath());
                }
                if (assets.has("footer")) {
                    File img = new File(extractDir, "assets/" + assets.getString("footer"));
                    DatabaseManager.saveSetting("lab_footer", img.getAbsolutePath());
                    footerPathLabel.setText(img.getAbsolutePath());
                }
                if (assets.has("watermark")) {
                    File img = new File(extractDir, "assets/" + assets.getString("watermark"));
                    DatabaseManager.saveSetting("report_watermark", img.getAbsolutePath());
                    watermarkPathLabel.setText(img.getAbsolutePath());
                }
                if (assets.has("signature")) {
                    File img = new File(extractDir, "assets/" + assets.getString("signature"));
                    DatabaseManager.saveSetting("doctor_signature", img.getAbsolutePath());
                    signaturePathLabel.setText(img.getAbsolutePath());
                }
            }
        }
        
        File templateHtml = new File(extractDir, "template.html");
        if (templateHtml.exists()) {
            String path = templateHtml.getAbsolutePath();
            DatabaseManager.saveSetting("report_template_path", path);
            if (!templateSelector.getItems().contains(path)) {
                templateSelector.getItems().add(path);
            }
            templateSelector.setValue(path);
            showAlert(Alert.AlertType.INFORMATION, "Template Extracted", "Template ZIP loaded correctly and active.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Warning", "ZIP extracted successfully but no template.html was found.");
        }
    }

    @FXML
    private void handleResetToDefaultTemplate() {
        templateSelector.setValue("DEFAULT (INTERNAL ENGINE)");
        showAlert(Alert.AlertType.INFORMATION, "Template Reset", "System has reverted to the internal standard report engine.");
    }

    @FXML
    private void handlePrintBrandingOnly() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Footer Branding Color");
        dialog.setHeaderText("Choose a color for your footer template (Composing Shop)");
        
        ButtonType printButtonType = new ButtonType("Generate PDF", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(printButtonType, ButtonType.CANCEL);
        
        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        VBox box = new VBox(10);
        box.getChildren().addAll(new Label("Select Color:"), colorPicker);
        box.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(box);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == printButtonType) {
                Color c = colorPicker.getValue();
                return String.format("#%02X%02X%02X", 
                    (int)(c.getRed() * 255), 
                    (int)(c.getGreen() * 255), 
                    (int)(c.getBlue() * 255));
            }
            return null;
        });

        dialog.showAndWait().ifPresent(hexColor -> {
            try {
                String path = com.lab.lms.services.ReportGenerator.generateBrandingOnly(hexColor);
                if (path != null) {
                    java.awt.Desktop.getDesktop().open(new File(path));
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "System Error", "Failed to generate branding template: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handlePrintRateList() {
        java.util.List<String> choices = java.util.Arrays.asList("ROUTINE TESTS", "PREMIUM TESTS", "ALL TESTS");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("ROUTINE TESTS", choices);
        dialog.setTitle("Rate List Generation");
        dialog.setHeaderText("Select Test Categories to Include");
        dialog.setContentText("Show:");

        dialog.showAndWait().ifPresent(choice -> {
            try (Connection conn = DatabaseManager.getConnection()) {
                String sql = "SELECT * FROM tests";
                if ("ROUTINE TESTS".equals(choice)) {
                    sql += " WHERE protocol_class = 'ACTIVE'";
                } else if ("PREMIUM TESTS".equals(choice)) {
                    sql += " WHERE protocol_class = 'INACTIVE'";
                }
                
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();
                java.util.List<Test> tests = new java.util.ArrayList<>();
                while (rs.next()) {
                    Test t = new Test(
                        rs.getInt("id"),
                        rs.getString("numeric_code"),
                        rs.getString("alpha_code"),
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
                        rs.getString("protocol_class")
                    );
                    tests.add(t);
                }
                
                String pdfPath = com.lab.lms.services.ReportGenerator.generateRateList(tests, choice);
                if (pdfPath != null) {
                    java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Process Failed: " + e.getMessage()).show();
            }
        });
    }

    @FXML
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearForm() {
        selectedTest = null;
        nameField.clear();
        categoryField.getSelectionModel().clearSelection();
        categoryField.setValue(null);
        priceField.clear();
        resultTimeField.clear();
        testNotesArea.setHtmlText("");
        specimenField.clear();
        numericCodeField.clear();
        alphaCodeField.clear();
        testClassBox.setValue("ROUTINE");
        isMicroscopicCheck.setSelected(false);
        isCultureCheck.setSelected(false);
        tempParameters.clear();
        paramCategoryField.getEditor().clear();
        paramMinKidsField.clear();
        paramMaxKidsField.clear();
        paramMinMaleField.clear();
        paramMaxMaleField.clear();
        paramMinFemaleField.clear();
        paramMaxFemaleField.clear();
        testImagePathLabel.setText("No image attached");
        testTable.getSelectionModel().clearSelection();
    }

    private void applyFilters() {
        if (filteredTests == null) return;
        
        String query = testSearchField.getText() == null ? "" : testSearchField.getText().toLowerCase().trim();
        boolean routineOnly = btnRoutineFilter.isSelected();
        boolean premiumOnly = btnPremiumFilter.isSelected();
        
        // Intelligence Layer: Cache UI settings outside the high-frequency predicate loop

        filteredTests.setPredicate(test -> {
            if (routineOnly) {
                if (test.getIsSpecial() != 0) return false;
            } else if (premiumOnly) {
                if (test.getIsSpecial() != 1) return false;
            }

            if (query.isEmpty()) return true;
            return test.getLowercaseName().contains(query) || 
                   (test.getNumericCode() != null && test.getNumericCode().contains(query)) ||
                   (test.getAlphaCode() != null && test.getAlphaCode().toLowerCase().contains(query)) ||
                   (test.getCategory() != null && test.getCategory().toLowerCase().contains(query));
        });
    }

    private void updateMicroscopicCultureUI() {
        javafx.application.Platform.runLater(() -> {
            boolean isMic = isMicroscopicCheck.isSelected();
            boolean isCul = isCultureCheck.isSelected();
            boolean isEither = isMic || isCul;

            unitInputBox.setVisible(!isEither);
            unitInputBox.setManaged(!isEither);

            categoryInputBox.setVisible(isEither);
            categoryInputBox.setManaged(isEither);
            
            // Hide demographic ranges for microscopic/culture tests
            kidsRangeBox.setVisible(!isEither);
            kidsRangeBox.setManaged(!isEither);
            maleRangeBox.setVisible(!isEither);
            maleRangeBox.setManaged(!isEither);
            femaleRangeBox.setVisible(!isEither);
            femaleRangeBox.setManaged(!isEither);

            if (isEither) {
                // Shared Mic/Cul Config
                maxInputBox.setVisible(false);
                maxInputBox.setManaged(false);

                String tName = (nameField.getText() != null) ? nameField.getText().trim().toUpperCase() : "";
                boolean isMicro = isMic || tName.contains("MICRO");

                if (isCul) {
                    paramNameLabel.setText("DRUG NAME");
                    paramNameField.setPromptText("e.g. Amoxicillin");
                    paramCategoryLabel.setText("BACTERIA NAME");
                    paramCategoryField.setPromptText("Select/Type Bacteria");
                    paramCategoryField.setItems(standardOrganisms);
                    
                    resultTimeField.setPromptText("Incubation Days (e.g. 5)");
                    
                    referenceValueBox.setVisible(false);
                    referenceValueBox.setManaged(false);

                    // Table Column Updates for Culture
                    colParamName.setText("DRUG NAME");
                    colParamUnit.setText("BACTERIA NAME");
                    colParamRange.setText("CULTURE TYPE (SPECIMEN)");
                    colParamRange.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMinRange()));
                    colParamRange.setVisible(true);

                    cultureTypeInputBox.setVisible(true);
                    cultureTypeInputBox.setManaged(true);
                } else {
                    cultureTypeInputBox.setVisible(false);
                    cultureTypeInputBox.setManaged(false);

                    paramNameLabel.setText("PARAMETER NAME");
                    referenceValueBox.setVisible(true);
                    referenceValueBox.setManaged(true);
                    
                    kidsRangeBox.setVisible(true); kidsRangeBox.setManaged(true);
                    maleRangeBox.setVisible(true); maleRangeBox.setManaged(true);
                    femaleRangeBox.setVisible(true); femaleRangeBox.setManaged(true);
                    secondaryRangesArea.setVisible(true); secondaryRangesArea.setManaged(true);

                    minLabel.setText("REGULAR RANGES");
                    colParamUnit.setText("UNIT");
                    colParamCategory.setVisible(true);
                    colParamRange.setText("REFERENCE DISCLOSURE");

                    if (isMicro) {
                        paramCategoryLabel.setText("EXAMINATION CATEGORY");
                        paramCategoryField.setPromptText("Choose category");
                        paramCategoryField.setItems(FXCollections.observableArrayList(
                            "Physical Examination", 
                            "Chemical Examination", 
                            "General Examination", 
                            "Microscopic Examination", 
                            "--- ADD CUSTOM ---"
                        ));
                    } else {
                        paramCategoryLabel.setText("EXAMINATION CATEGORY");
                        paramCategoryField.setPromptText("Select Category");
                        paramCategoryField.setItems(standardOrganisms);
                    }
                }
                colParamRange.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getMinRange()));
                colParamRange.setVisible(true);

                if (paramCategoryField.getValue() == null) {
                    paramCategoryField.setValue(isCul ? "Culture & Sensitivity" : "Physical Examination");
                }
                if (specimenField.getText() == null || specimenField.getText().trim().isEmpty() || specimenField.getText().equalsIgnoreCase("Blood")) {
                    specimenField.setText(isCul ? "Urine/Pus/Blood" : "Urine");
                }
            } else {
                paramNameLabel.setText("PARAMETER NAME");
                paramNameField.setPromptText("e.g. Hemoglobin");
                minLabel.setText("REGULAR MIN");
                paramMinField.setPromptText("Min Value");
                
                maxInputBox.setVisible(true);
                maxInputBox.setManaged(true);
                referenceValueBox.setVisible(true);
                referenceValueBox.setManaged(true);

                // Default Table Column Labels
                colParamName.setText("PARAMETER");
                colParamUnit.setText("UNIT");
                colParamCategory.setVisible(false);
                colParamRange.setText("REFERENCE DISCLOSURE");
                colParamRange.setCellValueFactory(new PropertyValueFactory<>("disclosure"));
                colParamRange.setVisible(true);
            }
        });
    }

    private void setupRichTextRibbon(HTMLEditor editor, Runnable smartTableAction) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node node = editor.lookup(".top-toolbar");
            if (node instanceof ToolBar) {
                ToolBar topBar = (ToolBar) node;
                
                Button btnSmart = new Button();
                javafx.scene.shape.SVGPath tableIcon = new javafx.scene.shape.SVGPath();
                tableIcon.setContent("M4 4h16v16H4V4zm2 2v4h4V6H6zm6 0v4h4V6h-4zm6 0v4h2V6h-2zM6 12v4h4v-4H6zm6 0v4h4v-4h-4zm6 0v4h2v-4h-2zM6 18v2h4v-2H6zm6 0v2h4v-2h-4zm6 0v2h2v-2h-2z");
                tableIcon.setFill(javafx.scene.paint.Color.web("#961111"));
                tableIcon.setScaleX(0.8);
                tableIcon.setScaleY(0.8);
                
                btnSmart.setGraphic(tableIcon);
                btnSmart.setTooltip(new Tooltip("Clinical Smart Table"));
                btnSmart.getStyleClass().add("html-editor-button");
                btnSmart.setStyle("-fx-cursor: hand; -fx-background-color: transparent; -fx-padding: 2 6;");
                btnSmart.setOnAction(e -> smartTableAction.run());
                
                topBar.getItems().addAll(new Separator(), btnSmart);
            }

            // Inject pro-document styling (Arial/Segoe UI feel)
            javafx.scene.web.WebView webView = (javafx.scene.web.WebView) editor.lookup("WebView");
            if (webView != null) {
                webView.getEngine().setJavaScriptEnabled(true);
                webView.getEngine().documentProperty().addListener((obs, old, doc) -> {
                    if (doc != null) {
                        try {
                            webView.getEngine().executeScript(
                                "if (!document.getElementById('proStyle')) {" +
                                "  var style = document.createElement('style');" +
                                "  style.id = 'proStyle';" +
                                "  style.innerHTML = 'body { font-family: \"Segoe UI\", Arial, sans-serif !important; font-size: 14px !important; line-height: 1.6 !important; color: #1a237e !important; margin: 15px !important; } " +
                                "                     table { border-collapse: collapse !important; table-layout: fixed !important; width: 100% !important; border: 1px solid #CFD8DC !important; margin: 12px 0 !important; } " +
                                "                     th, td { border: 1px solid #CFD8DC !important; padding: 12px !important; min-width: 50px; vertical-align: top; } " +
                                "                     th { background-color: #F8F9FA !important; font-weight: bold; }';" +
                                "  document.head.appendChild(style);" +
                                "}"
                            );
                        } catch (Exception e) {}
                    }
                });
            }
        });
    }

    private void loadInventory() {
        inventoryItems.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM inventory ORDER BY item_name ASC");
            while (rs.next()) {
                inventoryItems.add(new InventoryItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getDouble("quantity"),
                    rs.getString("unit"),
                    rs.getDouble("min_stock_level"),
                    rs.getString("category")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveInventory() {
        String name = inventoryItemNameField.getText();
        String qtyStr = inventoryQtyField.getText();
        String unit = inventoryUnitField.getText();
        String minLevelStr = inventoryMinLevelField.getText();

        if (name == null || name.isEmpty() || qtyStr == null || qtyStr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Item name and quantity are required.").show();
            return;
        }

        try {
            double qty = Double.parseDouble(qtyStr);
            double minLevel = minLevelStr != null && !minLevelStr.isEmpty() ? Double.parseDouble(minLevelStr) : 0;
            String category = inventoryCategoryField.getValue();

            try (Connection conn = DatabaseManager.getConnection()) {
                if (selectedInventoryItem != null) {
                    // Explicit Mode: Update existing item by ID
                    PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE inventory SET item_name = ?, quantity = ?, unit = ?, min_stock_level = ?, category = ? WHERE id = ?"
                    );
                    pstmt.setString(1, name);
                    pstmt.setDouble(2, qty);
                    pstmt.setString(3, unit);
                    pstmt.setDouble(4, minLevel);
                    pstmt.setString(5, category);
                    pstmt.setInt(6, selectedInventoryItem.getId());
                    pstmt.executeUpdate();
                    new Alert(Alert.AlertType.INFORMATION, "Inventory item updated successfully.").show();
                } else {
                    // Incremental Mode: Add or increment existing by name
                    PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO inventory (item_name, quantity, unit, min_stock_level, category) VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(item_name) DO UPDATE SET quantity = quantity + EXCLUDED.quantity, unit = EXCLUDED.unit, min_stock_level = EXCLUDED.min_stock_level, category = EXCLUDED.category"
                    );
                    pstmt.setString(1, name);
                    pstmt.setDouble(2, qty);
                    pstmt.setString(3, unit);
                    pstmt.setDouble(4, minLevel);
                    pstmt.setString(5, category);
                    pstmt.executeUpdate();
                    new Alert(Alert.AlertType.INFORMATION, "Inventory item saved successfully.").show();
                }
                
                loadInventory();
                handleClearInventory();
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid numeric format for quantity or min level.").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleClearInventory() {
        inventoryItemNameField.clear();
        inventoryQtyField.clear();
        inventoryUnitField.clear();
        inventoryMinLevelField.clear();
        inventoryCategoryField.getSelectionModel().clearSelection();
        selectedInventoryItem = null;
        inventoryTable.getSelectionModel().clearSelection();
        if (btnAddInventory != null) btnAddInventory.setText("ADD TO STOCK");
    }

    @FXML
    private void handleLinkInventory() {
        if (selectedTest == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a test protocol first.").show();
            return;
        }
        InventoryItem item = testInventoryItemBox.getValue();
        String usageStr = testInventoryQtyField.getText();

        if (item == null || usageStr == null || usageStr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select an inventory item and enter usage quantity.").show();
            return;
        }

        try {
            double usage = Double.parseDouble(usageStr);
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO test_inventory (test_id, inventory_id, usage_quantity) VALUES (?, ?, ?)"
                );
                pstmt.setInt(1, selectedTest.getId());
                pstmt.setInt(2, item.getId());
                pstmt.setDouble(3, usage);
                pstmt.executeUpdate();
                
                loadTestInventoryLinks(selectedTest.getId());
                testInventoryQtyField.clear();
                testInventoryItemBox.getSelectionModel().clearSelection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUnlinkInventory(TestInventoryLink link) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM test_inventory WHERE id = ?");
            pstmt.setInt(1, link.getId());
            pstmt.executeUpdate();
            loadTestInventoryLinks(selectedTest.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTestInventoryLinks(int testId) {
        linkedInventoryLinks.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT ti.*, i.item_name, i.unit FROM test_inventory ti " +
                "JOIN inventory i ON ti.inventory_id = i.id " +
                "WHERE ti.test_id = ?"
            );
            pstmt.setInt(1, testId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                linkedInventoryLinks.add(new TestInventoryLink(
                    rs.getInt("id"),
                    rs.getInt("test_id"),
                    rs.getInt("inventory_id"),
                    rs.getString("item_name"),
                    rs.getDouble("usage_quantity"),
                    rs.getString("unit")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewRangeGeneral() { appendRangeField(containerGeneralRange, extraMinFields, extraMaxFields); }
    @FXML
    private void handleNewRangeKids() { appendRangeField(containerKidsRange, extraMinKidsFields, extraMaxKidsFields); }
    @FXML
    private void handleNewRangeMale() { appendRangeField(containerMaleRange, extraMinMaleFields, extraMaxMaleFields); }
    @FXML
    private void handleNewRangeFemale() { appendRangeField(containerFemaleRange, extraMinFemaleFields, extraMaxFemaleFields); }

    private void appendRangeField(VBox container, java.util.List<TextField> minList, java.util.List<TextField> maxList) {
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(6);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        TextField min = new TextField(); min.setPromptText("Min"); min.setPrefWidth(150); min.setMaxWidth(150);
        TextField max = new TextField(); max.setPromptText("Max"); max.setPrefWidth(150); max.setMaxWidth(150);
        
        Button removeBtn = new Button("\u00d7");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E53935; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4;");
        removeBtn.setTooltip(new Tooltip("Remove this range"));
        removeBtn.setOnAction(e -> {
            container.getChildren().remove(hbox);
            minList.remove(min);
            maxList.remove(max);
        });

        hbox.getChildren().addAll(min, max, removeBtn);
        container.getChildren().add(hbox);
        minList.add(min);
        maxList.add(max);
    }

    private void setupHTMLEditorRibbon(HTMLEditor editor, Runnable onInsertTable) {
        javafx.application.Platform.runLater(() -> {
            Node node = editor.lookup(".top-toolbar");
            if (node instanceof ToolBar) {
                ToolBar bar = (ToolBar) node;
                Button btnTable = new Button("INSERT MS WORD TABLE");
                btnTable.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                btnTable.setOnAction(e -> onInsertTable.run());
                bar.getItems().add(btnTable);
            }
        });
    }

}

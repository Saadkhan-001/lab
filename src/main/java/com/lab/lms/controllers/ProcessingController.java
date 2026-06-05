package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Test;
import com.lab.lms.models.TestParameter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.lab.lms.util.ClinicalAgeCalculator;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.awt.Desktop;
import com.lab.lms.services.ReportGenerator;
import com.lab.lms.services.WhatsAppService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ProcessingController {

    @FXML
    private ComboBox<String> recentPatientCombo;
    @FXML
    private Label patientDetailsLabel;
    @FXML
    private TextField patientSearchField;

    @FXML
    private TableView<Test> testsToProcessTable;
    @FXML
    private TableColumn<Test, Boolean> colSelect;
    @FXML
    private TableColumn<Test, String> colTestName;
    @FXML
    private TableColumn<Test, String> colStatus;

    @FXML
    private Label selectedTestName;
    @FXML
    private TableView<TestParameter> resultsEntryTable;
    @FXML
    private TableColumn<TestParameter, String> colParamName;
    @FXML
    private TableColumn<TestParameter, String> colParamUnit;
    @FXML
    private TableColumn<TestParameter, String> colParamRange;
    @FXML
    private TableColumn<TestParameter, String> colParamValue;

    @FXML
    private Button btnCommit, btnNext, btnAddOpinion, btnAddLabComment, btnAddCultureResult, btnAddParam, btnEditTest;
    @FXML
    private TextArea testCommentsArea, testLabCommentsArea;
    @FXML
    private Label testCommentsLabel, testLabCommentsLabel;

    @FXML
    private VBox opinionBox, labCommentBox, cultureOptionsBox;
    @FXML
    private TextField bacteriaSelectCombo, cultureTypeSelectCombo;
    @FXML
    private ComboBox<String> growthStatusCombo;
    @FXML
    private TextField cultureSpecimenField, cultureGrowthDaysField;
    @FXML
    private TextArea cultureGrowthField;
    @FXML
    private VBox organismSelectBox;
    @FXML
    private Label cultureGrowthLabel, sensitivityTableLabel;
    @FXML
    private FlowPane resultImageGallery;

    private ObservableList<Test> testQueue = FXCollections.observableArrayList();
    private ObservableList<TestParameter> currentParameters = FXCollections.observableArrayList();
    private ObservableList<TestParameter> allCultureParameters = FXCollections.observableArrayList();
    private boolean isLoadingData = false;
    private ObservableList<String> bacteriaComboList = FXCollections.observableArrayList();
    private String currentSampleId;
    private String currentPatientId;
    private String currentGender;
    private String currentAgeStr;
    private Map<Test, String> testToSampleMap = new HashMap<>();
    private Set<String> popularSuggestions = new TreeSet<>();

    @FXML
    public void initialize() {
        loadRecentPatients();
        if (growthStatusCombo != null) {
            growthStatusCombo.setItems(FXCollections.observableArrayList("Positive", "Negative"));
            growthStatusCombo.getSelectionModel().select("Positive");
        }

        // Setup table selection with checkbox
        colSelect.setCellFactory(column -> new javafx.scene.control.cell.CheckBoxTableCell<>());
        colSelect.setCellValueFactory(cellData -> {
            Test t = cellData.getValue();
            javafx.beans.property.BooleanProperty prop = new javafx.beans.property.SimpleBooleanProperty(
                    t.isSelected());
            prop.addListener((obs, oldVal, newVal) -> t.setSelected(newVal));
            return prop;
        });

        colTestName.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().toString()));
        colTestName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // Dynamic status with theme-compliant badges
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Test t = getTableView().getItems().get(getIndex());
                    String status = t.getStatus() == null ? "PENDING" : t.getStatus();
                    boolean isComp = "COMPLETED".equalsIgnoreCase(status);

                    Label badge = new Label(status);
                    String color = "#455A64";
                    String bg = "#F4F7F9";

                    if (isComp) {
                        color = "#961111";
                        bg = "#FFEBEE";
                    } else if ("REFUNDED".equalsIgnoreCase(status)) {
                        color = "#961111";
                        bg = "#F9F2F2";
                    } else if ("PENDING".equalsIgnoreCase(status)) {
                        color = "#455A64";
                        bg = "#F4F7F9";
                    }

                    badge.setStyle("-fx-background-color: " + bg + "; " +
                            "-fx-text-fill: " + color + "; " +
                            "-fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold; " +
                            "-fx-font-family: 'Segoe UI', sans-serif; -fx-background-radius: 3;");
                    setGraphic(badge);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        testsToProcessTable.setItems(testQueue);

        // Queue Shortcuts: Ctrl+A (Select All), Ctrl+P (Print Selected), 1-9 (Select
        // Test)
        testsToProcessTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.A) {
                    testQueue.forEach(t -> t.setSelected(true));
                    testsToProcessTable.refresh();
                    event.consume();
                } else if (event.getCode() == KeyCode.P) {
                    handlePrintSelected();
                    event.consume();
                }
            } else if (event.getCode().isDigitKey() && resultsEntryTable.getEditingCell() == null) {
                handleNumericTestSelect(event);
            }
        });

        colParamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colParamName.setCellFactory(column -> {
            javafx.scene.control.TableCell<TestParameter, String> cell = javafx.scene.control.cell.TextFieldTableCell.<TestParameter>forTableColumn().call(column);
            cell.itemProperty().addListener((obs, old, val) -> {
                cell.setEditable(com.lab.lms.services.SessionContext.hasPermission("ADMIN"));
            });
            return cell;
        });
        colParamName.setOnEditCommit(event -> handleParameterEdit(event, "NAME"));

        colParamUnit.setCellValueFactory(cell -> {
            Test selected = testsToProcessTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getIsMicroscopic() == 1) {
                return cell.getValue().categoryProperty();
            }
            return (cell.getValue().unitOverrideProperty().get() != null) ? cell.getValue().unitOverrideProperty() : cell.getValue().unitProperty();
        });
        colParamUnit.setCellFactory(column -> {
            javafx.scene.control.TableCell<TestParameter, String> cell = javafx.scene.control.cell.TextFieldTableCell.<TestParameter>forTableColumn().call(column);
            cell.itemProperty().addListener((obs, old, val) -> {
                cell.setEditable(com.lab.lms.services.SessionContext.hasPermission("ADMIN"));
            });
            return cell;
        });
        colParamUnit.setOnEditCommit(event -> handleParameterEdit(event, "UNIT"));

        colParamRange.setCellValueFactory(cell -> {
            return new SimpleStringProperty(cell.getValue().getRange(currentGender, currentAgeStr));
        });
        colParamRange.setCellFactory(column -> {
            javafx.scene.control.TableCell<TestParameter, String> cell = javafx.scene.control.cell.TextFieldTableCell.<TestParameter>forTableColumn().call(column);
            cell.itemProperty().addListener((obs, old, val) -> {
                cell.setEditable(com.lab.lms.services.SessionContext.hasPermission("ADMIN"));
            });
            return cell;
        });
        colParamRange.setOnEditCommit(event -> handleParameterEdit(event, "RANGE"));
        colParamValue.setCellValueFactory(cell -> cell.getValue().valueProperty());

        setupAutocompleteColumn();

        colParamValue.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setValue(event.getNewValue());
        });

        loadPopularSuggestions();

        resultsEntryTable.setItems(currentParameters);

        // Global Shortcuts (Save, Select All, Print, 1-9 Select Test)
        // Strict Focus Trapping and Navigation (Tab, Enter, Arrows)
        resultsEntryTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.S) {
                    handleSaveResult();
                    event.consume();
                } else if (event.getCode() == KeyCode.A) {
                    testQueue.forEach(t -> t.setSelected(true));
                    testsToProcessTable.refresh();
                    event.consume();
                } else if (event.getCode() == KeyCode.P) {
                    handlePrintSelected();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER
                    || event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                // Strictly trap focus navigation within the resultsEntryTable boundaries
                // Handle navigation here ONLY if not currently editing (let the TextField
                // handle it during edit)
                if (resultsEntryTable.getEditingCell() == null && resultsEntryTable.isFocused()) {
                    int delta = 0;
                    if (event.getCode() == KeyCode.TAB) {
                        delta = event.isShiftDown() ? -1 : 1;
                    } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
                        delta = 1;
                    } else if (event.getCode() == KeyCode.UP) {
                        delta = -1;
                    }

                    if (delta != 0) {
                        moveResultsTableFocus(delta);
                        event.consume();
                    }
                }
            } else if (event.getCode().isDigitKey() && resultsEntryTable.getEditingCell() == null) {
                handleNumericTestSelect(event);
            }
        });

        testsToProcessTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // CRITICAL: Sync clinical comments back to the old object before switching
            if (oldVal != null) {
                oldVal.setNotes(testCommentsArea.getText());
                // Use a custom property or shared field if we want to store lab notes in the
                // Test object
                // For now, let's assume notes is generic or we combine them.
                // Or better: store BOTH in the Test object if it supports it.
            }

            if (newVal != null) {
                if (btnEditTest != null) {
                    boolean isAdmin = com.lab.lms.services.SessionContext.hasPermission("ADMIN");
                    btnEditTest.setVisible(isAdmin);
                    btnEditTest.setManaged(isAdmin);
                }

                currentSampleId = testToSampleMap.get(newVal);
                selectedTestName.setText(
                        newVal.getName() + ("REFUNDED".equalsIgnoreCase(newVal.getStatus()) ? " (REFUNDED)" : ""));
                loadTestParameters(newVal.getId());

                // Load clinical comments (interpretations)
                String fetchedNotes = newVal.getNotes();
                testCommentsArea.setText(fetchedNotes == null ? ""
                        : com.lab.lms.services.ReportGenerator.extractPlainText(fetchedNotes));
                // Clear Lab comments for new selection (unless we fetch from DB)
                testLabCommentsArea.setText("");

                // Show Lab Comment button only for culture tests
                boolean isCul = (newVal.getIsCulture() == 1);
                btnAddLabComment.setVisible(isCul);
                btnAddLabComment.setManaged(isCul);

                // Disable entry for refunded tests to maintain ledger integrity
                boolean isRefunded = "REFUNDED".equalsIgnoreCase(newVal.getStatus());
                resultsEntryTable.setEditable(!isRefunded);
                btnCommit.setDisable(isRefunded);
            }
        });

        // Culture Auto-update for results
        bacteriaSelectCombo.textProperty().addListener((obs, old, val) -> updateCultureResultSentence());
        cultureTypeSelectCombo.textProperty().addListener((obs, old, val) -> {
            updateCultureResultSentence();
        });
        cultureGrowthDaysField.textProperty().addListener((obs, oldVal, newVal) -> updateCultureResultSentence());

        // Guided workflow: Auto-fill if we came from Sample Collection or Patient History
        String contextSid = com.lab.lms.services.SessionContext.getCurrentSampleId();
        if (contextSid != null && !contextSid.isEmpty()) {
            loadScanData(contextSid);
            
            // Auto-select patient in combo box
            for (String item : recentPatientCombo.getItems()) {
                if (item.contains("(" + contextSid + ")")) {
                    recentPatientCombo.setValue(item);
                    break;
                }
            }
            
            // Auto-select specific test if provided (e.g. from History -> Enter Result)
            Integer contextTid = com.lab.lms.services.SessionContext.getCurrentTestId();
            if (contextTid != null) {
                for (com.lab.lms.models.Test t : testQueue) {
                    if (t.getId() == contextTid) {
                        testsToProcessTable.getSelectionModel().select(t);
                        testsToProcessTable.scrollTo(t);
                        break;
                    }
                }
                com.lab.lms.services.SessionContext.setCurrentTestId(null); // One-time selection
            }
        }

        // Dynamic labels based on workflow

        // Dynamic labels based on workflow
        String doctorEnabled = com.lab.lms.dao.DatabaseManager.getSetting("enable_doctor_review", "true");
        if (!Boolean.parseBoolean(doctorEnabled)) {
            btnCommit.setText("SAVE & FINALIZE (PROCEED TO RECORD)");
            btnNext.setVisible(false);
            btnNext.setManaged(false);
        }
    }

    private void loadPopularSuggestions() {
        popularSuggestions.addAll(Arrays.asList(
                "Positive", "Negative", "Reactive", "Non-Reactive", "Trace", "Nill",
                "Yellow", "Pale Yellow", "Cloudy", "Clear", "Normal", "Abnormal",
                "Present", "Absent", "1.010", "1.020", "1.030", "5.0", "6.0", "7.0"));

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT value, COUNT(*) as c FROM results WHERE value IS NOT NULL AND value != '' GROUP BY value HAVING c > 1 ORDER BY c DESC LIMIT 100";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                popularSuggestions.add(rs.getString("value"));
            }
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleBacteriaSelect() {
        // No filtration by default anymore as requested: "Make sure all the drugs
        // created in that test show."
        // We will show ALL culture parameters, but the user can use the text fields for
        // the organism name and specimen
        currentParameters.setAll(allCultureParameters);
        updateCultureResultSentence();
        resultsEntryTable.refresh();
    }

    private void updateCultureResultSentence() {
        if (isLoadingData)
            return;

        String status = growthStatusCombo.getSelectionModel().getSelectedItem();
        if (status == null)
            return;

        String daysContent = cultureGrowthDaysField.getText();
        if (daysContent == null || daysContent.isEmpty())
            daysContent = "5";

        // Smart unit normalization: only add 'days' if no unit is present
        String normalizedDays = (daysContent.toLowerCase().contains("day") || daysContent.toLowerCase().contains("hr"))
                ? daysContent
                : (daysContent + " days");

        String bac = bacteriaSelectCombo.getText();

        if ("Negative".equals(status)) {
            // Main Culture Finding as requested: "No growth after [X days] of incubation."
            String defaultGrowth = "No growth after " + normalizedDays + " of incubation.";
            String currentGrowth = cultureGrowthField.getText();
            // ONLY OVERWRITE IF BLANK OR CONTAINS DEFAULT TEXT TEMPLATE
            if (currentGrowth == null || currentGrowth.trim().isEmpty()
                    || currentGrowth.trim().equalsIgnoreCase("No growth observed.") ||
                    (currentGrowth.toLowerCase().contains("no growth")
                            && currentGrowth.toLowerCase().contains("of incubation"))) {
                cultureGrowthField.setText(defaultGrowth);
            }

            // Automated Laboratory Comment disclaimer
            String autoTxt = "Discontinue antibiotics 24-72 hours before collecting blood, urine, or other culture samples to enhance accuracy and to avoid false negative results. Kindly rule out any contamination.";
            String currentLabNote = testLabCommentsArea.getText();
            if (currentLabNote == null || currentLabNote.trim().isEmpty() || currentLabNote.trim().equals(autoTxt)) {
                testLabCommentsArea.setText(autoTxt);
            }

            // If the lab comment box was hidden, show it automatically so the user can
            // verify/edit
            if (labCommentBox != null && !labCommentBox.isVisible()) {
                labCommentBox.setVisible(true);
                labCommentBox.setManaged(true);
            }
        } else {
            // Positive Case
            if (bac != null && !bac.isEmpty()) {
                cultureGrowthField.setText("The culture yielded growth of " + bac.toUpperCase()
                        + " after incubation for " + normalizedDays + ".");
            } else {
                cultureGrowthField.setText("Positive growth observed after incubation for " + normalizedDays + ".");
            }

            // Should we clear the automated comment when switching back to Positive?
            // Only clear it if it exactly matches our automated template to avoid losing
            // user-typed notes
            String autoTxt = "Discontinue antibiotics 24-72 hours before collecting blood, urine, or other culture samples to enhance accuracy and to avoid false negative results. Kindly rule out any contamination.";
            if (testLabCommentsArea.getText() != null && testLabCommentsArea.getText().trim().equals(autoTxt)) {
                testLabCommentsArea.setText("");
            }
        }
    }

    private void setupAutocompleteColumn() {
        colParamValue.setCellFactory(column -> new TableCell<TestParameter, String>() {
            private TextField textField;
            private ContextMenu suggestionsPopup = new ContextMenu();

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    createTextField();
                }
                setGraphic(textField);
                setText(null);
                textField.setText(getItem());
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
                suggestionsPopup.hide();
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(item);
                        }
                        setGraphic(textField);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }

            private void createTextField() {
                textField = new TextField(getItem());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal == null || newVal.trim().isEmpty()) {
                        suggestionsPopup.hide();
                        return;
                    }

                    String query = newVal.trim().toLowerCase();
                    List<String> matches = new ArrayList<>();
                    for (String s : popularSuggestions) {
                        if (s.toLowerCase().startsWith(query)) {
                            matches.add(s);
                        }
                    }

                    if (matches.isEmpty()) {
                        suggestionsPopup.hide();
                    } else {
                        suggestionsPopup.getItems().clear();
                        for (String match : matches) {
                            MenuItem mi = new MenuItem(match);
                            mi.setOnAction(e -> {
                                textField.setText(match);
                                commitEdit(match);
                                suggestionsPopup.hide();
                                moveResultsTableFocus(1);
                            });
                            suggestionsPopup.getItems().add(mi);
                        }
                        if (!suggestionsPopup.isShowing()) {
                            suggestionsPopup.show(textField, javafx.geometry.Side.BOTTOM, 0, 0);
                        }
                    }
                });

                textField.focusedProperty().addListener((obs, old, newVal) -> {
                    if (!newVal) {
                        commitEdit(textField.getText());
                        suggestionsPopup.hide();
                    }
                });

                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                        commitEdit(textField.getText());
                        suggestionsPopup.hide();
                        moveResultsTableFocus(event.isShiftDown() ? -1 : 1);
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                        if (!suggestionsPopup.isShowing()) {
                            commitEdit(textField.getText());
                            moveResultsTableFocus(event.getCode() == KeyCode.UP ? -1 : 1);
                            event.consume();
                        }
                    }
                });
            }

            // Replaced with centralized moveResultsTableFocus in parent class
        });
    }

    private void moveResultsTableFocus(int delta) {
        int currentRow = resultsEntryTable.getSelectionModel().getSelectedIndex();
        int size = resultsEntryTable.getItems().size();
        if (currentRow == -1 && size > 0)
            currentRow = 0;

        int target = currentRow + delta;
        if (target >= 0 && target < size) {
            final int t = target;
            Platform.runLater(() -> {
                resultsEntryTable.requestFocus(); // Force focus capture first
                resultsEntryTable.getSelectionModel().clearAndSelect(t);
                resultsEntryTable.getFocusModel().focus(t, colParamValue);
                resultsEntryTable.scrollTo(t);
                resultsEntryTable.edit(t, colParamValue);
            });
        } else {
            // Strictly capture focus at the boundaries to prevent it from escaping to other
            // UI areas (e.g., patient switching)
            final int row = currentRow;
            Platform.runLater(() -> {
                resultsEntryTable.getSelectionModel().select(row);
                resultsEntryTable.scrollTo(row);
                resultsEntryTable.requestFocus();
            });
        }
    }


    private void handleNumericTestSelect(KeyEvent event) {
        try {
            int index = Integer.parseInt(event.getText()) - 1;
            if (index >= 0 && index < testQueue.size()) {
                Test t = testQueue.get(index);
                testsToProcessTable.getSelectionModel().select(t);
                Platform.runLater(() -> {
                    if (!currentParameters.isEmpty()) {
                        resultsEntryTable.getSelectionModel().select(0);
                        resultsEntryTable.getFocusModel().focus(0, colParamValue);
                        resultsEntryTable.edit(0, colParamValue);
                    }
                });
                event.consume();
            }
        } catch (Exception e) {
        }
    }

    private void loadRecentPatients() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Fetch UNIQUE PATIENTS that have at least one COLLECTED sample with a PENDING
            // result
            // OR a result COMPLETED today (so they stay in the queue for the same day as
            // requested)
            String sql = "SELECT p.patient_id, p.name, MAX(COALESCE(s.collection_date, s.sample_id)) as last_coll FROM patients p "
                    +
                    "JOIN samples s ON p.patient_id = s.patient_id " +
                    "JOIN results r ON s.sample_id = r.sample_id " +
                    "WHERE s.status IN ('COLLECTED', 'AWAITING COLLECTION') AND (r.status = 'PENDING' OR (r.status = 'COMPLETED' AND date(r.completed_at, 'localtime') = date('now', 'localtime'))) "
                    +
                    "GROUP BY p.patient_id, p.name " +
                    "ORDER BY last_coll DESC LIMIT 50";

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

    @FXML
    private void handleRecentSelectPat() {
        String selected = recentPatientCombo.getValue();
        if (selected != null && selected.contains("(") && selected.contains(")")) {
            String id = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
            loadScanData(id);
        }
    }

    @FXML
    private void handlePatientSearch() {
        String query = patientSearchField.getText();
        if (query == null || query.trim().isEmpty())
            return;
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
                loadScanData(id);
            } else if (results.size() > 1) {
                recentPatientCombo.getItems().setAll(results);
                recentPatientCombo.show();
                new Alert(Alert.AlertType.INFORMATION,
                        "Multiple patients found. Please select from the dropdown below.").show();
            } else {
                new Alert(Alert.AlertType.WARNING, "No patients found for: " + query).show();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScanData(String sid) {
        if (sid == null || sid.trim().isEmpty())
            return;

        testQueue.clear();
        currentParameters.clear();
        testToSampleMap.clear();

        try (Connection conn = DatabaseManager.getConnection()) {
            String resolveSql = "SELECT p.patient_id, p.name, p.gender, p.age, p.age_months, p.age_days FROM patients p "
                    +
                    "LEFT JOIN samples s ON p.patient_id = s.patient_id " +
                    "WHERE p.patient_id = ? OR s.sample_id = ? LIMIT 1";
            PreparedStatement rsPstmt = conn.prepareStatement(resolveSql);
            rsPstmt.setString(1, sid);
            rsPstmt.setString(2, sid);
            ResultSet rsCheck = rsPstmt.executeQuery();
            if (rsCheck.next()) {
                currentPatientId = rsCheck.getString("patient_id");
                currentGender = rsCheck.getString("gender");

                int y = rsCheck.getInt("age");
                int m = rsCheck.getInt("age_months");
                int d = rsCheck.getInt("age_days");
                String ageStr = y + "y";
                if (m > 0 || d > 0)
                    ageStr += " " + m + "m " + d + "d";
                currentAgeStr = ageStr;

                patientDetailsLabel.setText("Patient: " + rsCheck.getString("name") + " (" + currentGender + ", "
                        + ageStr + ") | ID: " + currentPatientId);
                patientDetailsLabel.setStyle("-fx-text-fill: #1A0A0A; -fx-font-weight: bold;");
            } else {
                patientDetailsLabel.setText("Patient details could not be resolved.");
                patientDetailsLabel.setStyle("-fx-text-fill: #961111;");
                return;
            }

            // SELF-HEALING: Ensure test_id links are populated for this patient before
            // loading
            try {
                String repairSql = "UPDATE results SET test_id = (SELECT test_id FROM test_parameters WHERE id = results.parameter_id) "
                        +
                        "WHERE test_id IS NULL AND sample_id IN (SELECT sample_id FROM samples WHERE patient_id = ?)";
                PreparedStatement psRepair = conn.prepareStatement(repairSql);
                psRepair.setString(1, currentPatientId);
                psRepair.executeUpdate();
            } catch (Exception e) {
            }

            String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            String tSql = "SELECT t.id, t.numeric_code, t.alpha_code, t.name, t.category, t.price, t.result_time, t.notes, t.is_special, t.is_microscopic, t.is_culture, t.specimen, t.image_path, t.container, t.volume, t.fasting, s.sample_id, "
                    +
                    "MAX(CASE WHEN UPPER(r.status) = 'PENDING' THEN 'PENDING' WHEN UPPER(r.status) = 'REFUNDED' THEN 'REFUNDED' ELSE 'COMPLETED' END) as status "
                    +
                    "FROM results r " +
                    "JOIN tests t ON (r.test_id = t.id) " +
                    "JOIN samples s ON r.sample_id = s.sample_id " +
                    "WHERE s.patient_id = ? AND s.status IN ('COLLECTED', 'AWAITING COLLECTION') " +
                    "AND (UPPER(r.status) = 'PENDING' OR UPPER(r.status) = 'REFUNDED' OR (UPPER(r.status) = 'COMPLETED' AND date(r.completed_at, 'localtime') = ?)) "
                    +
                    "GROUP BY s.sample_id, t.id " +
                    "ORDER BY CASE WHEN UPPER(r.status) = 'PENDING' THEN 0 WHEN UPPER(r.status) = 'REFUNDED' THEN 1 ELSE 2 END, s.collection_date DESC";

            PreparedStatement tPstmt = conn.prepareStatement(tSql);
            tPstmt.setString(1, currentPatientId);
            tPstmt.setString(2, todayStr);
            ResultSet tRs = tPstmt.executeQuery();
            boolean testsFound = false;
            while (tRs.next()) {
                testsFound = true;
                Test testObj = new Test(
                        tRs.getInt("id"),
                        tRs.getString("numeric_code"),
                        tRs.getString("alpha_code"),
                        tRs.getString("name"),
                        tRs.getString("category"),
                        tRs.getDouble("price"),
                        tRs.getString("result_time"),
                        tRs.getString("notes"),
                        tRs.getInt("is_special"),
                        tRs.getInt("is_microscopic"),
                        tRs.getInt("is_culture"),
                        tRs.getString("specimen"),
                        tRs.getString("image_path"),
                        tRs.getString("container"),
                        tRs.getString("volume"),
                        tRs.getString("fasting"));

                String status = tRs.getString("status");
                testObj.setStatus(status);

                String sampleId = tRs.getString("sample_id");
                testQueue.add(testObj);
                testToSampleMap.put(testObj, sampleId);

                // CRITICAL: Always update currentSampleId to the active SID when loading
                // patient data
                currentSampleId = sampleId;
            }

            if (!testsFound) {
                patientDetailsLabel
                        .setText(patientDetailsLabel.getText() + " | NO ACTIVE TESTS FOUND FOR THIS PATIENT");
                patientDetailsLabel.setStyle("-fx-text-fill: #961111;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTestParameters(int testId) {
        currentParameters.clear();
        allCultureParameters.clear();
        testCommentsArea.setText("");
        testLabCommentsArea.setText("");
        bacteriaSelectCombo.setText("");
        cultureTypeSelectCombo.setText("");
        if (growthStatusCombo != null)
            growthStatusCombo.getSelectionModel().select("Positive");
        cultureSpecimenField.setText("");
        cultureGrowthField.setText("");
        cultureGrowthDaysField.setText("");
        cultureOptionsBox.setVisible(false);
        cultureOptionsBox.setManaged(false);

        try (Connection conn = DatabaseManager.getConnection()) {
            // [CLINICAL SELF-HEALING] Synchronize results table with current test
            // parameters
            try {
                String syncSql = "INSERT OR IGNORE INTO results (sample_id, test_id, parameter_id, status) " +
                        "SELECT ?, ?, id, 'PENDING' FROM test_parameters WHERE test_id = ? AND is_global = 1";
                try (PreparedStatement psSync = conn.prepareStatement(syncSql)) {
                    psSync.setString(1, currentSampleId);
                    psSync.setInt(2, testId);
                    psSync.setInt(3, testId);
                    psSync.executeUpdate();
                }

                // [CULTURE ANCHOR] Ensure a unique persistence anchor exists (using -1 as a
                // reserved parameter ID to trigger UNIQUE constraint)
                String anchorSql = "INSERT OR IGNORE INTO results (sample_id, test_id, parameter_id, status) VALUES (?, ?, -1, 'PENDING')";
                try (PreparedStatement psAnchor = conn.prepareStatement(anchorSql)) {
                    psAnchor.setString(1, currentSampleId);
                    psAnchor.setInt(2, testId);
                    psAnchor.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("[SYNC FIX] Parameter Integrity Update: " + e.getMessage());
            }

            boolean isMic = false;
            boolean isCul = false;
            PreparedStatement mPstmt = conn.prepareStatement(
                    "SELECT is_microscopic, is_culture, specimen, growth_status, growth_findings FROM tests WHERE id = ?");
            mPstmt.setInt(1, testId);
            ResultSet mRs = mPstmt.executeQuery();
            if (mRs.next()) {
                try {
                    isMic = mRs.getInt("is_microscopic") == 1;
                } catch (Exception e) {
                }
                try {
                    isCul = mRs.getInt("is_culture") == 1;
                } catch (Exception e) {
                }
                try {
                    cultureSpecimenField.setText(mRs.getString("specimen"));
                } catch (Exception e) {
                }
                try {
                    String gs = mRs.getString("growth_status");
                    if (gs == null)
                        gs = "Positive";
                    if (growthStatusCombo != null) {
                        growthStatusCombo.getSelectionModel().select(gs);
                    }
                } catch (Exception e) {
                }

                try {
                    String gf = mRs.getString("growth_findings");
                    if (isCul && gf != null && !gf.isEmpty()) {
                        cultureGrowthField.setText(gf);
                    } else if (isCul) {
                        cultureGrowthField.setText("");
                    }
                } catch (Exception e) {
                    if (isCul)
                        cultureGrowthField.setText("");
                }
            }

            if (isCul) {
                // For Ease: Show master options by default for culture tests
                cultureOptionsBox.setVisible(true);
                cultureOptionsBox.setManaged(true);
                // Also default to opinion being open for culture as they usually need it
                opinionBox.setVisible(true);
                opinionBox.setManaged(true);
                labCommentBox.setVisible(false);
                labCommentBox.setManaged(false);

                btnAddOpinion.setText("ADD OPINION");
                btnAddOpinion.setManaged(true);
                btnAddOpinion.setVisible(true);

                btnAddLabComment.setText("ADD LAB COMMENT");
                btnAddLabComment.setVisible(true);
                btnAddLabComment.setManaged(true);

                btnAddCultureResult.setText("CULTURE GROWTH OBSERVATION");
                btnAddCultureResult.setVisible(true);
                btnAddCultureResult.setManaged(true);

                btnAddParam.setText("ADD PARAMETER");
                btnAddParam.setVisible(true);
                btnAddParam.setManaged(true);

                colParamUnit.setText("ORGANISM");
                colParamRange.setVisible(false);

                testCommentsLabel.setText("CLINICAL OPINION / INTERPRETATION");
                testCommentsArea.setPromptText("Enter clinical opinion...");
                testLabCommentsLabel.setVisible(true);
                testLabCommentsLabel.setManaged(true);
                testLabCommentsArea.setVisible(true);
                testLabCommentsArea.setManaged(true);
            } else {
                cultureOptionsBox.setVisible(false);
                cultureOptionsBox.setManaged(false);
                opinionBox.setVisible(false);
                opinionBox.setManaged(false);
                labCommentBox.setVisible(false);
                labCommentBox.setManaged(false);

                btnAddOpinion.setText("+ ADD NOTE");
                btnAddOpinion.setVisible(true);
                btnAddOpinion.setManaged(true);

                btnAddLabComment.setVisible(false);
                btnAddLabComment.setManaged(false);

                btnAddCultureResult.setVisible(false);
                btnAddCultureResult.setManaged(false);

                btnAddParam.setText("+ ADD PARAMETER");
                btnAddParam.setVisible(true);
                btnAddParam.setManaged(true);

                testCommentsLabel.setText("INTERPRETATION / NOTES");
                testCommentsArea.setPromptText("Enter clinical findings...");
                testLabCommentsLabel.setVisible(false);
                testLabCommentsLabel.setManaged(false);
                testLabCommentsArea.setVisible(false);
                testLabCommentsArea.setManaged(false);

                if (isCul) {
                    colParamName.setText("DRUG NAME");
                    colParamUnit.setText("BACTERIA / ORGANISM");
                    colParamRange.setVisible(false);
                } else if (isMic) {
                    colParamName.setText("PARAMETER");
                    colParamUnit.setText("CATEGORY");
                    colParamRange.setVisible(false);
                } else {
                    colParamName.setText("PARAMETER");
                    colParamUnit.setText("UNIT");
                    colParamRange.setVisible(true);
                }
            }

            // Load specific parameter placeholders and culture metadata.
            // Priority: Load from the anchor row (-1) or any row that already has clinical
            // findings.
            String commentsSql = "SELECT comment, lab_notes, identified_organism, culture_type, growth_status, growth_findings, duration "
                    +
                    "FROM results WHERE sample_id = ? AND test_id = ? " +
                    "ORDER BY (CASE WHEN identified_organism IS NOT NULL THEN 1 ELSE 0 END) DESC, parameter_id ASC LIMIT 1";
            try (PreparedStatement csPstmt = conn.prepareStatement(commentsSql)) {
                csPstmt.setString(1, currentSampleId);
                csPstmt.setInt(2, testId);
                ResultSet csRs = csPstmt.executeQuery();
                if (csRs.next()) {
                    isLoadingData = true; // Block UI listeners early during metadata load
                    String rawComment = csRs.getString("comment");
                    String labNote = csRs.getString("lab_notes");

                    // Defensive reading: Try dedicated columns one by one
                    String loadedOrg = null;
                    try {
                        loadedOrg = csRs.getString("identified_organism");
                    } catch (Exception e) {
                    }
                    String loadedCult = null;
                    try {
                        loadedCult = csRs.getString("culture_type");
                    } catch (Exception e) {
                    }
                    String loadedGS = null;
                    try {
                        loadedGS = csRs.getString("growth_status");
                    } catch (Exception e) {
                    }
                    String loadedGF = null;
                    try {
                        loadedGF = csRs.getString("growth_findings");
                    } catch (Exception e) {
                    }
                    String loadedDur = null;
                    try {
                        loadedDur = csRs.getString("duration");
                    } catch (Exception e) {
                    }

                    // Priority 2: Structured Fallback in comments (crucial if dedicated columns are
                    // missing)
                    if ((loadedOrg == null || loadedOrg.isEmpty()) && rawComment != null
                            && rawComment.contains("[ORG]:")) {
                        int orgIdx = rawComment.indexOf("[ORG]:");
                        int endIdx = rawComment.indexOf(" ", orgIdx);
                        if (endIdx == -1)
                            endIdx = rawComment.length();
                        loadedOrg = rawComment.substring(orgIdx + 6, endIdx).trim();
                    }
                    if ((loadedCult == null || loadedCult.isEmpty()) && rawComment != null
                            && rawComment.contains("[SPEC]:")) {
                        int specIdx = rawComment.indexOf("[SPEC]:");
                        int endIdx = rawComment.indexOf(" ", specIdx);
                        if (endIdx == -1)
                            endIdx = rawComment.length();
                        loadedCult = rawComment.substring(specIdx + 7, endIdx).trim();
                    }

                    if (isCul) {
                        if (loadedOrg != null && !loadedOrg.isEmpty())
                            bacteriaSelectCombo.setText(loadedOrg);
                        if (loadedCult != null && !loadedCult.isEmpty()) {
                            cultureTypeSelectCombo.setText(loadedCult);
                            cultureSpecimenField.setText(loadedCult);
                        }
                        if (loadedGS != null && growthStatusCombo != null)
                            growthStatusCombo.getSelectionModel().select(loadedGS);
                        if (loadedGF != null && !loadedGF.isEmpty())
                            cultureGrowthField.setText(loadedGF);
                        if (loadedDur != null && !loadedDur.isEmpty())
                            cultureGrowthDaysField.setText(loadedDur);
                    }

                    if (rawComment != null && rawComment.contains("[GROWTH]:")) {
                        int gIdx = rawComment.indexOf("[GROWTH]:");
                        int oIdx = rawComment.indexOf("[OPINION]:");
                        if (gIdx != -1) {
                            if (oIdx != -1) {
                                if (isCul && (cultureGrowthField.getText() == null
                                        || cultureGrowthField.getText().isEmpty()))
                                    cultureGrowthField.setText(rawComment.substring(gIdx + 8, oIdx).trim());
                                testCommentsArea.setText(rawComment.substring(oIdx + 10).trim());
                            } else {
                                if (isCul && (cultureGrowthField.getText() == null
                                        || cultureGrowthField.getText().isEmpty()))
                                    cultureGrowthField.setText(rawComment.substring(gIdx + 8).trim());
                            }
                        }
                    } else if (rawComment != null) {
                        testCommentsArea.setText(ReportGenerator.extractPlainText(rawComment));
                    }

                    if (labNote != null && !labNote.isEmpty()) {
                        testLabCommentsArea.setText(ReportGenerator.extractPlainText(labNote));
                    } else if (rawComment != null && rawComment.contains("[LAB]:")) {
                        int lIdx = rawComment.indexOf("[LAB]:");
                        testLabCommentsArea
                                .setText(ReportGenerator.extractPlainText(rawComment.substring(lIdx + 6).trim()));
                    }
                }
            } catch (Exception ex) {
                System.err.println("[LOAD ERROR] Culture Metadata Retrieval: " + ex.getMessage());
            }

            String sql = "SELECT tp.id, tp.name, tp.unit, tp.category, tp.min_range, tp.max_range, " +
                    "tp.min_range_male, tp.max_range_male, tp.min_range_female, tp.max_range_female, tp.min_range_kids, tp.max_range_kids, "
                    +
                    "r.value, r.name_override, r.unit_override, r.range_override " +
                    "FROM results r " +
                    "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                    "WHERE r.sample_id = ? AND tp.test_id = ? " +
                    "ORDER BY CASE WHEN tp.print_order = 0 THEN 999999 ELSE tp.print_order END ASC, tp.id ASC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            // isLoadingData is already true if csRs.next() was hit
            pstmt.setString(1, currentSampleId);
            pstmt.setInt(2, testId);
            ResultSet rs = pstmt.executeQuery();
            Set<String> bacteriaList = new LinkedHashSet<>();
            Set<String> cultTypes = new LinkedHashSet<>();
            try {
                while (rs.next()) {
                    // Parameters loaded here...
                    TestParameter tp = new TestParameter(
                            rs.getInt("id"),
                            testId,
                            rs.getString("name"),
                            rs.getString("unit"),
                            rs.getString("min_range"),
                            rs.getString("max_range"),
                            rs.getString("min_range_male"),
                            rs.getString("max_range_male"),
                            rs.getString("min_range_female"),
                            rs.getString("max_range_female"),
                            rs.getString("min_range_kids"),
                            rs.getString("max_range_kids"));
                    tp.setCategory(rs.getString("category"));
                    tp.setValue(rs.getString("value"));
                    tp.setNameOverride(rs.getString("name_override"));
                    tp.setUnitOverride(rs.getString("unit_override"));
                    tp.setRangeOverride(rs.getString("range_override"));

                    if (isCul) {
                        allCultureParameters.add(tp);
                        String u = tp.getUnit();
                        if (u != null && !u.trim().isEmpty()) {
                            String cleanU = u.trim().toUpperCase();
                            String lowerU = cleanU.toLowerCase();
                            // EXCLUDE units that are clearly not bacteria organisms
                            if (!lowerU.contains("/") && !lowerU.equals("%") && !lowerU.equals("mg/dl") &&
                                    !lowerU.equals("iu/ml") && !lowerU.equals("miu/ml") && !lowerU.equals("u/l")) {
                                bacteriaList.add(cleanU);
                            }
                        }

                        String c = tp.getMinRange();
                        if (c != null && !c.trim().isEmpty()) {
                            cultTypes.add(c.trim().toUpperCase());
                        }
                    } else {
                        currentParameters.add(tp);
                    }
                }

                if (isCul) {
                    if (growthStatusCombo != null) {
                        handleGrowthStatusChange();
                    }

                    // NEW: Load default culture duration from test repository (result_time field)
                    try (PreparedStatement drPs = conn.prepareStatement("SELECT result_time FROM tests WHERE id = ?")) {
                        drPs.setInt(1, testId);
                        ResultSet drRs = drPs.executeQuery();
                        if (drRs.next()) {
                            String defaultDays = drRs.getString("result_time");
                            if (defaultDays != null && !defaultDays.isEmpty()
                                    && cultureGrowthDaysField.getText().equals("5")) {
                                cultureGrowthDaysField.setText(defaultDays);
                            }
                        }
                    } catch (Exception e) {
                    }

                    for (TestParameter tp : allCultureParameters) {
                        String unit = tp.getUnit();
                        if (unit != null && !unit.trim().isEmpty() && !bacteriaList.contains(unit)) {
                            String lowerU = unit.toLowerCase().trim();
                            // RIGID FILTER: Exclude units and common non-bacteria categories
                            if (!lowerU.contains("/") && !lowerU.equals("%") && !lowerU.equals("mg/dl") &&
                                    !lowerU.equals("iu/ml") && !lowerU.equals("miu/ml") && !lowerU.equals("u/l") &&
                                    !lowerU.contains("examination") && !lowerU.contains("status")
                                    && !lowerU.contains("result") &&
                                    !lowerU.contains("findings") && !lowerU.contains("count")) {
                                bacteriaList.add(unit.trim().toUpperCase());
                            }
                        }
                    }
                    // No hardcoded standards - strictly database-driven discovery

                    // Global specimens scan
                    try (PreparedStatement csPs = conn.prepareStatement(
                            "SELECT DISTINCT min_range FROM test_parameters tp JOIN tests t ON tp.test_id = t.id WHERE t.is_culture = 1 AND min_range IS NOT NULL AND min_range != ''")) {
                        ResultSet csRs = csPs.executeQuery();
                        while (csRs.next()) {
                            cultTypes.add(csRs.getString(1).trim().toUpperCase());
                        }
                    } catch (Exception e) {
                    }

                    // Global bacteria scan
                    try (PreparedStatement gPs = conn.prepareStatement(
                            "SELECT DISTINCT unit FROM test_parameters WHERE test_id IN (SELECT id FROM tests WHERE is_culture = 1) AND unit IS NOT NULL AND unit != ''")) {
                        ResultSet gRs = gPs.executeQuery();
                        while (gRs.next()) {
                            String u = gRs.getString("unit").trim().toUpperCase();
                            String lowerU = u.toLowerCase();
                            if (!lowerU.contains("/") && !lowerU.contains("%") && !lowerU.contains("mg/dl") &&
                                    !lowerU.contains("iu/ml") && !lowerU.contains("miu/ml") && !lowerU.contains("u/l")
                                    &&
                                    !lowerU.contains("examination") && !lowerU.contains("status")
                                    && !lowerU.contains("result") &&
                                    !lowerU.contains("findings") && !lowerU.contains("count")) {
                                bacteriaList.add(u);
                            }
                        }
                    } catch (Exception e) {
                        java.sql.Statement stmt = conn.createStatement();
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN lab_notes TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN identified_organism TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN culture_type TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN growth_status TEXT DEFAULT 'Positive'");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN growth_findings TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE results ADD COLUMN duration TEXT");
                        } catch (SQLException ex) {
                        }

                        // [FORCE REPAIR] Ensure columns exist in common test tables too
                        try {
                            stmt.execute("ALTER TABLE tests ADD COLUMN specimen TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE tests ADD COLUMN growth_status TEXT");
                        } catch (SQLException ex) {
                        }
                        try {
                            stmt.execute("ALTER TABLE tests ADD COLUMN growth_findings TEXT");
                        } catch (SQLException ex) {
                        }

                        conn.commit();
                    }

                    btnAddOpinion.setStyle(
                            "-fx-background-color: #ECEFF1; -fx-text-fill: #455A64; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand;");
                    btnAddLabComment.setStyle(
                            "-fx-background-color: #ECEFF1; -fx-text-fill: #455A64; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand;");
                    btnAddCultureResult.setStyle(
                            "-fx-background-color: #ECEFF1; -fx-text-fill: #455A64; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand;");
                    btnAddParam.setStyle(
                            "-fx-background-color: #ECEFF1; -fx-text-fill: #455A64; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand;");

                    // Discovery: only auto-populate if the fields are currently empty (to avoid
                    // overwriting saved data)
                    // Discovery: do NOT auto-populate text fields, just ensure parameters are
                    // loaded
                    // This prevents 'dot and dash' values from appearing when the user wants a
                    // clean form.
                    currentParameters.setAll(allCultureParameters);
                }

                // By default, hide comment box unless user clicks '+ ADD COMMENT'
                opinionBox.setVisible(false);
                opinionBox.setManaged(false);
                labCommentBox.setVisible(false);
                labCommentBox.setManaged(false);

                btnAddOpinion.setVisible(true);
                btnAddOpinion.setManaged(true);

                // Load Attached Images
                refreshImageGallery(testId);

                // Auto-focus first parameter for rapid entry
                Platform.runLater(() -> {
                    if (!currentParameters.isEmpty()) {
                        resultsEntryTable.getSelectionModel().select(0);
                        resultsEntryTable.edit(0, colParamValue);
                    }
                });
            } finally {
                isLoadingData = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshImageGallery(int testId) {
        resultImageGallery.getChildren().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn
                    .prepareStatement("SELECT id, image_path FROM test_images WHERE sample_id = ? AND test_id = ?");
            ps.setString(1, currentSampleId);
            ps.setInt(2, testId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int imgId = rs.getInt("id");
                String path = rs.getString("image_path");
                addImageThumbnail(imgId, path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addImageThumbnail(int imgId, String path) {
        File file = new File(path);
        if (!file.exists())
            return;

        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: white; -fx-padding: 2; -fx-border-color: #CFD8DC; -fx-border-radius: 5;");

        ImageView iv = new ImageView(new Image(file.toURI().toString()));
        iv.setFitHeight(80);
        iv.setFitWidth(80);
        iv.setPreserveRatio(true);

        Button btnDel = new Button("×");
        btnDel.setStyle(
                "-fx-background-color: #961111; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6; -fx-font-size: 10; -fx-background-radius: 10;");
        btnDel.setOnAction(e -> handleRemoveResultImage(imgId));
        StackPane.setAlignment(btnDel, javafx.geometry.Pos.TOP_RIGHT);

        pane.getChildren().addAll(iv, btnDel);
        resultImageGallery.getChildren().add(pane);

        pane.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                }
            }
        });
    }

    @FXML
    private void handleUploadResultImage() {
        Test selected = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selected == null || currentSampleId == null) {
            new Alert(Alert.AlertType.WARNING, "Select a test protocol first.").show();
            return;
        }

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(resultImageGallery.getScene().getWindow());

        if (file != null) {
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn
                        .prepareStatement("INSERT INTO test_images (sample_id, test_id, image_path) VALUES (?, ?, ?)");
                ps.setString(1, currentSampleId);
                ps.setInt(2, selected.getId());
                ps.setString(3, file.getAbsolutePath());
                ps.executeUpdate();
                refreshImageGallery(selected.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRemoveResultImage(int imgId) {
        Test selected = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM test_images WHERE id = ?");
            ps.setInt(1, imgId);
            ps.executeUpdate();
            refreshImageGallery(selected.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditPatientInfo() {
        if (currentPatientId == null || currentPatientId.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No patient selected to edit.").show();
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT name, referred_doctor, gender, COALESCE(whatsapp, phone) as phone FROM patients WHERE patient_id = ?");
            ps.setString(1, currentPatientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String currName = rs.getString("name");
                String currDoc = rs.getString("referred_doctor");
                String currGender = rs.getString("gender");
                String currPhone = rs.getString("phone");

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Edit Patient Info");
                dialog.setHeaderText("Modify Patient Details for Active Test");

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                TextField nameField = new TextField(currName != null ? currName : "");
                TextField docField = new TextField(currDoc != null ? currDoc : "");
                javafx.scene.control.ComboBox<String> genderCombo = new javafx.scene.control.ComboBox<>(javafx.collections.FXCollections.observableArrayList("Male", "Female", "Other"));
                genderCombo.setValue(currGender != null ? currGender : "Male");
                TextField phoneField = new TextField(currPhone != null ? currPhone : "");

                grid.add(new Label("Patient Name:"), 0, 0);
                grid.add(nameField, 1, 0);
                grid.add(new Label("Referred Doctor:"), 0, 1);
                grid.add(docField, 1, 1);
                grid.add(new Label("Gender:"), 0, 2);
                grid.add(genderCombo, 1, 2);
                grid.add(new Label("Phone / WhatsApp:"), 0, 3);
                grid.add(phoneField, 1, 3);

                dialog.getDialogPane().setContent(grid);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                Optional<ButtonType> result = dialog.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    PreparedStatement update = conn.prepareStatement("UPDATE patients SET name = ?, referred_doctor = ?, gender = ?, phone = ?, whatsapp = ? WHERE patient_id = ?");
                    update.setString(1, nameField.getText().trim());
                    update.setString(2, docField.getText().trim());
                    update.setString(3, genderCombo.getValue());
                    update.setString(4, phoneField.getText().trim());
                    update.setString(5, phoneField.getText().trim());
                    update.setString(6, currentPatientId);
                    update.executeUpdate();

                    new Alert(Alert.AlertType.INFORMATION, "Patient details updated successfully for the selected tests.").show();
                    loadScanData(currentSampleId); // Refresh patient header immediately
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleSaveResult() {
        Test selTest = testsToProcessTable.getSelectionModel().getSelectedItem();
        boolean isCult = selTest != null && selTest.getIsCulture() == 1;
        List<TestParameter> paramList = isCult ? allCultureParameters : currentParameters;

        if (currentSampleId == null || (paramList.isEmpty() && !isCult))
            return;

        try (Connection conn = DatabaseManager.getConnection()) {
            String doctorEnabled = DatabaseManager.getSetting("enable_doctor_review", "true");
            int approvalStatus = Boolean.parseBoolean(doctorEnabled) ? 0 : 1;

            String pName = "", pId = "", pAge = "30y", pGender = "Male", pRef = "Self", sDate = "-", pAddress = "",
                    pPhone = null;

            // 1. Fetch Patient Demographics with Registration Date for Precise Age
            // Calculation
            PreparedStatement pInfPstmt = conn.prepareStatement(
                    "SELECT p.patient_id, p.name, p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, p.whatsapp, p.phone, p.registration_date, s.collection_date "
                            + "FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE s.sample_id = ?");
            pInfPstmt.setString(1, currentSampleId);
            ResultSet rsInf = pInfPstmt.executeQuery();
            if (rsInf.next()) {
                pId = rsInf.getString("patient_id");
                pName = rsInf.getString("name");

                // Precise Age Resolution
                int regY = rsInf.getInt("age");
                int regM = rsInf.getInt("age_months");
                int regD = rsInf.getInt("age_days");
                String regDate = rsInf.getString("registration_date");

                ClinicalAgeCalculator.AgeResult currentAge = ClinicalAgeCalculator.calculateCurrentAge(regY, regM, regD,
                        regDate);

                pAge = currentAge.toString();

                pGender = rsInf.getString("gender");
                pAddress = rsInf.getString("address");
                pRef = rsInf.getString("referred_doctor");
                sDate = rsInf.getString("collection_date");
                pPhone = rsInf.getString("whatsapp");
                if (pPhone == null || pPhone.isEmpty())
                    pPhone = rsInf.getString("phone");
            }

            // 2. Update existing result rows, including completion timestamp and NEW
            // culture fields
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE results SET value = ?, is_abnormal = ?, status = 'COMPLETED', doctor_approval = ?, comment = ?, completed_at = CURRENT_TIMESTAMP, "
                            +
                            "identified_organism = ?, culture_type = ?, growth_status = ?, growth_findings = ?, duration = ? "
                            +
                            "WHERE sample_id = ? AND parameter_id = ?");

            String currentComment = testCommentsArea.getText();
            if (currentComment == null)
                currentComment = "";
            String labNotes = testLabCommentsArea.getText();
            if (labNotes == null)
                labNotes = "";

            Test selectedTest = testsToProcessTable.getSelectionModel().getSelectedItem();
            boolean isCul = selectedTest != null && selectedTest.getIsCulture() == 1;

            // If Culture Mode: Sync Growth Findings from TextArea back to the hidden
            // parameter
            if (isCul) {
                String growth = cultureGrowthField.getText();
                for (TestParameter p : allCultureParameters) {
                    if (p.getName().toLowerCase().contains("growth") || p.getName().toLowerCase().contains("result")) {
                        p.setValue(growth);
                    }
                }
            }

            // Save ALL parameters (including hidden ones in culture mode)
            List<TestParameter> paramsToSave = isCul ? allCultureParameters : currentParameters;
            String organism = isCul ? bacteriaSelectCombo.getText() : "";
            String cultType = isCul ? cultureTypeSelectCombo.getText() : "";
            String gStatus = isCul ? (growthStatusCombo.getSelectionModel().getSelectedItem() != null
                    ? growthStatusCombo.getSelectionModel().getSelectedItem()
                    : "Positive") : "";
            String gFindings = isCul ? cultureGrowthField.getText() : "";
            String gDays = isCul ? cultureGrowthDaysField.getText() : "";

            for (TestParameter p : paramsToSave) {
                pstmt.setString(1, (p.getValue() == null) ? "" : p.getValue());
                pstmt.setInt(2, p.isAbnormal(pGender, pAge) ? 1 : 0);
                pstmt.setInt(3, approvalStatus);

                // Store BOTH Growth findings and Clinical Opinion in the comment field for
                // legacy compatibility
                String resComment = (currentComment == null) ? "" : currentComment;
                if (isCul && !gFindings.isEmpty()) {
                    resComment = "[GROWTH]:" + gFindings + "[OPINION]:" + resComment;
                }
                pstmt.setString(4, resComment);

                // New Dedicated Columns
                pstmt.setString(5, organism);
                pstmt.setString(6, cultType);
                pstmt.setString(7, gStatus);
                pstmt.setString(8, gFindings);
                pstmt.setString(9, gDays);

                pstmt.setString(10, currentSampleId);
                pstmt.setInt(11, p.getId());
                pstmt.addBatch();
            }

            pstmt.executeBatch();

            // Finalize all rows for this test (including the hidden anchor row) to ensure status sync
            String statusSyncSql = "UPDATE results SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP " +
                                 "WHERE sample_id = ? AND (test_id = ? OR parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?))";
            try (PreparedStatement asPstmt = conn.prepareStatement(statusSyncSql)) {
                asPstmt.setString(1, currentSampleId);
                asPstmt.setInt(2, selectedTest.getId());
                asPstmt.setInt(3, selectedTest.getId());
                asPstmt.executeUpdate();
            }

            // Sync clinical comments to ALL rows for this test
            try (PreparedStatement commPstmt = conn.prepareStatement("UPDATE results SET comment = ? WHERE sample_id = ? AND test_id = ?")) {
                commPstmt.setString(1, currentComment);
                commPstmt.setString(2, currentSampleId);
                commPstmt.setInt(3, selectedTest.getId());
                commPstmt.executeUpdate();
            }

            // ROBUSTNESS FOR CULTURE: Ensure ALL associated result rows are updated to
            // COMPLETED
            // even if no specific TestParameters are loaded (common for culture tests with
            // zero drug sensitivities)
            if (isCul) {
                String resComment = (currentComment == null) ? "" : currentComment;
                if (!gFindings.isEmpty()) {
                    resComment = "[GROWTH]:" + gFindings + "[OPINION]:" + resComment;
                }

                // FORCE SYNC: Ensure ALL rows for this sample/test (including drugs and the
                // anchor row) have identical metadata
                String fallbackSql = "UPDATE results SET status = 'COMPLETED', comment = ?, lab_notes = ?, completed_at = CURRENT_TIMESTAMP, "
                        +
                        "identified_organism = ?, culture_type = ?, growth_status = ?, growth_findings = ?, duration = ? "
                        +
                        "WHERE sample_id = ? AND test_id = ?";
                try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                    fallbackPstmt.setString(1, resComment);
                    fallbackPstmt.setString(2, labNotes);
                    fallbackPstmt.setString(3, organism);
                    fallbackPstmt.setString(4, cultType);
                    fallbackPstmt.setString(5, gStatus);
                    fallbackPstmt.setString(6, gFindings);
                    fallbackPstmt.setString(7, gDays);
                    fallbackPstmt.setString(8, currentSampleId);
                    fallbackPstmt.setInt(9, selectedTest.getId());
                    fallbackPstmt.executeUpdate();
                } catch (SQLException ex) {
                    // SMART LEGACY FALLBACK: Preserve metadata in structured markers within the
                    // comment field
                    try (PreparedStatement legacyPstmt = conn.prepareStatement(
                            "UPDATE results SET status = 'COMPLETED', comment = ?, completed_at = CURRENT_TIMESTAMP WHERE sample_id = ? AND test_id = ?")) {
                        String legacyComment = resComment + " | [ORG]:" + organism + " [SPEC]:" + cultType + " [LAB]:"
                                + labNotes;
                        legacyPstmt.setString(1, legacyComment);
                        legacyPstmt.setString(2, currentSampleId);
                        legacyPstmt.setInt(3, selectedTest.getId());
                        legacyPstmt.executeUpdate();
                    }
                }
            }

            // Optional: Update specimen in test definition if changed
            if (isCul && selectedTest != null) {
                String newSpec = cultureSpecimenField.getText();
                String newGrowth = growthStatusCombo.getSelectionModel().getSelectedItem();
                String findings = cultureGrowthField.getText();
                if (newSpec != null || newGrowth != null || findings != null) {
                    PreparedStatement specPstmt = conn.prepareStatement(
                            "UPDATE tests SET specimen = ?, growth_status = ?, growth_findings = ? WHERE id = ?");
                    specPstmt.setString(1, newSpec);
                    specPstmt.setString(2, newGrowth == null ? "Positive" : newGrowth);
                    specPstmt.setString(3, findings);
                    specPstmt.setInt(4, selectedTest.getId());
                    specPstmt.executeUpdate();
                }
            }

            String pdfPath = null;
            ReportGenerator.TestData singleTD = null;

            // 1. If Doctor Review is disabled, generate report immediately
            if (approvalStatus == 1) {
                if (selectedTest != null) {
                    // Prepare Report Data
                    List<Map<String, String>> reportData = new ArrayList<>();
                    for (TestParameter tp : paramsToSave) {
                        String val = tp.getValue();
                        // For culture/microscopic, only include parameters that actually have a result
                        // entered
                        if (isCul && (val == null || val.trim().isEmpty()))
                            continue;

                        Map<String, String> map = new HashMap<>();
                        map.put("name", tp.getName());
                        map.put("value", (val == null) ? "" : val);
                        map.put("unit", tp.getUnit());
                        map.put("category", tp.getCategory());
                        map.put("range", tp.getRange(pGender, pAge));
                        map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                        reportData.add(map);
                    }

                    // Generate PDF Report with complete metadata including Lab Notes
                    boolean includeHeader = Boolean
                            .parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("print_header_footer", "true"));
                    String selectedOrg = null;
                    if (selectedTest.getIsCulture() == 1) {
                        selectedOrg = bacteriaSelectCombo.getText();
                    }

                    String growthFindingsVal = (selectedTest.getIsCulture() == 1) ? cultureGrowthField.getText() : null;
                    pdfPath = ReportGenerator.generateReport(pName, pId, pAge, pGender, pRef, sDate,
                            selectedTest.getName(), pPhone, pAddress, reportData, includeHeader, true, currentComment,
                            labNotes, currentSampleId, selectedOrg, growthFindingsVal);

                    // PASSING COMMENT IN MULTI-REPORT DATA (For success dialog/WhatsApp)
                    int isSp = selectedTest.getIsSpecial();
                    int isMic = selectedTest.getIsMicroscopic();
                    String spec = selectedTest.getSpecimen();

                    // Fetch Images for Report
                    String refImg = "";
                    List<String> resImgs = new ArrayList<>();
                    PreparedStatement imgPstmt = conn.prepareStatement("SELECT image_path FROM tests WHERE id = ?");
                    imgPstmt.setInt(1, selectedTest.getId());
                    ResultSet rsImg = imgPstmt.executeQuery();
                    if (rsImg.next())
                        refImg = rsImg.getString("image_path");

                    PreparedStatement resImgPstmt = conn
                            .prepareStatement("SELECT image_path FROM test_images WHERE sample_id = ? AND test_id = ?");
                    resImgPstmt.setString(1, currentSampleId);
                    resImgPstmt.setInt(2, selectedTest.getId());
                    ResultSet rsResImg = resImgPstmt.executeQuery();
                    while (rsResImg.next())
                        resImgs.add(rsResImg.getString("image_path"));

                    singleTD = new ReportGenerator.TestData(selectedTest.getName(), selectedTest.getId(),
                            selectedTest.getCategory(), reportData, labNotes, isSp, isMic, selectedTest.getIsCulture(),
                            spec, currentComment, refImg, resImgs, currentSampleId);
                    if (selectedTest.getIsCulture() == 1) {
                        singleTD.growthFindings = cultureGrowthField.getText();
                    }
                    if (selectedOrg != null)
                        singleTD.selectedOrganism = selectedOrg;

                    if (pdfPath != null) {
                        // Update PDF path in DB
                        PreparedStatement pdfPstmt = conn.prepareStatement(
                                "UPDATE results SET pdf_path = ? WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?)");
                        pdfPstmt.setString(1, pdfPath);
                        pdfPstmt.setString(2, currentSampleId);
                        pdfPstmt.setInt(3, selectedTest.getId());
                        pdfPstmt.executeUpdate();

                        // Update status to COMPLETED and set completed_at timestamp
                        PreparedStatement statusPstmt = conn.prepareStatement(
                                "UPDATE results SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE sample_id = ? AND parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?)");
                        statusPstmt.setString(1, currentSampleId);
                        statusPstmt.setInt(2, selectedTest.getId());
                        statusPstmt.executeUpdate();
                        
                        // Execute Automated Inventory Deduction
                        deductInventory(conn, selectedTest.getId());
                    }
                }
            }

            // Show Success Actions Dialog (Consolidated)
            if (approvalStatus == 1 && pdfPath != null) {
                List<ReportGenerator.TestData> singleTestDataList = new ArrayList<>();
                singleTestDataList.add(singleTD);
                showSuccessActions(pdfPath, pPhone, singleTestDataList, pName, pId, pAge, pGender, pRef, sDate, pAddress);
            } else if (approvalStatus == 1 && pdfPath == null) {
                new Alert(Alert.AlertType.ERROR,
                        "Results saved, but the clinical report (PDF) could not be generated. Please verify parameters.")
                        .show();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Results saved and entry confirmed.").show();
            }

            loadScanData(currentSampleId); // Refresh active test list (now includes completed today)
            loadRecentPatients(); // Refresh the sidebar queue (now retains completed for today as requested)
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "System Error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    private void deductInventory(Connection conn, int testId) {
        boolean lowStockAlert = false;
        StringBuilder alertMessage = new StringBuilder("Low Stock Warning! The following items are below minimum levels:\n\n");

        try {
            String linkSql = "SELECT ti.inventory_id, ti.usage_quantity, i.item_name FROM test_inventory ti JOIN inventory i ON ti.inventory_id = i.id WHERE ti.test_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(linkSql)) {
                ps.setInt(1, testId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int invId = rs.getInt("inventory_id");
                        double usage = rs.getDouble("usage_quantity");
                        String itemName = rs.getString("item_name");

                        String deductSql = "UPDATE inventory SET quantity = quantity - ? WHERE id = ?";
                        try (PreparedStatement upPs = conn.prepareStatement(deductSql)) {
                            upPs.setDouble(1, usage);
                            upPs.setInt(2, invId);
                            upPs.executeUpdate();
                        }

                        // Check low stock
                        String checkSql = "SELECT quantity, min_stock_level FROM inventory WHERE id = ?";
                        try (PreparedStatement chkPs = conn.prepareStatement(checkSql)) {
                            chkPs.setInt(1, invId);
                            try (ResultSet rsCheck = chkPs.executeQuery()) {
                                if (rsCheck.next()) {
                                    double qty = rsCheck.getDouble("quantity");
                                    double minLvl = rsCheck.getDouble("min_stock_level");
                                    if (qty <= minLvl) {
                                        lowStockAlert = true;
                                        alertMessage.append("- ").append(itemName).append(" (Remaining: ").append(String.format("%.2f", qty)).append(")\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (lowStockAlert) {
                final String finalMsg = alertMessage.toString();
                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.WARNING, finalMsg).show();
                });
            }
        } catch (SQLException e) {
            System.err.println("Inventory Deduction Error: " + e.getMessage());
        }
    }

    private void showSuccessActions(String pdfPath, String phone, List<ReportGenerator.TestData> allTestData,
            String pName, String pId, String pAge, String pGender, String pRef, String sDate, String pAddress) {
        Alert actions = new Alert(Alert.AlertType.CONFIRMATION);
        actions.setTitle("Diagnostic Processing Complete");
        actions.setHeaderText("Results Saved & Document Generated");
        actions.setContentText("The clinical report is ready. You can view, print, or share it via WhatsApp.");

        ButtonType btnView = new ButtonType("VIEW REPORT");
        ButtonType btnPrint = new ButtonType("PRINT");
        ButtonType btnWhatsApp = new ButtonType("SEND WHATSAPP");
        ButtonType btnDone = new ButtonType("DONE", ButtonBar.ButtonData.CANCEL_CLOSE);

        actions.getButtonTypes().setAll(btnView, btnPrint, btnWhatsApp, btnDone);

        Optional<ButtonType> result = actions.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnView) {
                try {
                    Desktop.getDesktop().open(new File(pdfPath));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                showSuccessActions(pdfPath, phone, allTestData, pName, pId, pAge, pGender, pRef, sDate, pAddress);
            } else if (result.get() == btnPrint) {
                // Secondary selection for Header/Footer
                Alert printOptions = new Alert(Alert.AlertType.CONFIRMATION);
                printOptions.setTitle("Print Options");
                printOptions.setHeaderText("Select Report Layout");
                printOptions.setContentText("Choose whether to include the laboratory header and footer.");

                ButtonType btnWithHeader = new ButtonType("Report With Header/Footer");
                ButtonType btnWithoutHeader = new ButtonType("Report Without Header/Footer");
                ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                printOptions.getButtonTypes().setAll(btnWithHeader, btnWithoutHeader, btnCancel);

                Optional<ButtonType> printResult = printOptions.showAndWait();
                if (printResult.isPresent() && printResult.get() != btnCancel) {
                    boolean includeHeader = (printResult.get() == btnWithHeader);
                    String finalPdfPath = pdfPath;

                    // If user wants no header, we must re-generate a temp PDF
                    if (!includeHeader) {
                        try {
                            finalPdfPath = ReportGenerator.generateMultiTestReport(pName, pId, pAge, pGender, pRef,
                                    sDate, phone, pAddress, allTestData, false, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        // Open in browser for high-fidelity printing context
                        if (finalPdfPath != null) {
                            Desktop.getDesktop().browse(new File(finalPdfPath).toURI());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Fallback to default open
                        try {
                            Desktop.getDesktop().open(new File(finalPdfPath));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                showSuccessActions(pdfPath, phone, allTestData, pName, pId, pAge, pGender, pRef, sDate, pAddress);
            } else if (result.get() == btnWhatsApp) {
                // RE-GENERATE specifically for WhatsApp WITH header=true
                try (Connection conn = DatabaseManager.getConnection()) {
                    // 1. Fetch CLEAN Patient Info from DB
                    String wpAge = "", wpGender = "", wpRef = "", wpDate = "", wrAddress = "";
                    PreparedStatement pInfPstmt = conn.prepareStatement(
                            "SELECT p.patient_id, p.name, p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, s.collection_date "
                                    +
                                    "FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE s.sample_id = ?");
                    pInfPstmt.setString(1, currentSampleId);
                    ResultSet rsInf = pInfPstmt.executeQuery();
                    
                    String wpId = pId;
                    String wpName = pName;

                    if (rsInf.next()) {
                        wpId = rsInf.getString("patient_id");
                        wpName = rsInf.getString("name");

                        int y = rsInf.getInt("age");
                        int m = rsInf.getInt("age_months");
                        int d = rsInf.getInt("age_days");
                        wpAge = y + "y";
                        if (m > 0 || d > 0)
                            wpAge += " " + m + "m " + d + "d";

                        wpGender = rsInf.getString("gender");
                        wrAddress = rsInf.getString("address");
                        wpRef = rsInf.getString("referred_doctor");
                        wpDate = rsInf.getString("collection_date");
                    }

                    // 2. WhatsApp Generation (Use provided allTestData)
                    String whatsappPdf = ReportGenerator.generateMultiTestReport(wpName, wpId, wpAge, wpGender, wpRef, wpDate,
                            phone, wrAddress, allTestData, true, false); // No watermark for WhatsApp

                    if (whatsappPdf != null) {
                        WhatsAppService.sendReportWithRecovery(wpId, wpName, phone, whatsappPdf);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    WhatsAppService.sendReportWithRecovery(pId, pName, phone, pdfPath); // Fallback to existing
                }
                showSuccessActions(pdfPath, phone, allTestData, pName, pId, pAge, pGender, pRef, sDate, pAddress);
            }
        }
    }

    @FXML
    private void handlePrintSelected() {
        if (currentSampleId == null)
            return;

        List<Test> selected = new ArrayList<>();
        for (Test t : testQueue) {
            if (t.isSelected())
                selected.add(t);
        }

        if (selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select at least one test to print.").show();
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Fetch Patient Info
            String pName = "", pId = "", pAge = "", pGender = "", pRef = "", sDate = "", phone = "", address = "";
            PreparedStatement pInfPstmt = conn.prepareStatement(
                    "SELECT p.patient_id, p.name, p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, p.whatsapp, p.phone, s.collection_date "
                            +
                            "FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE s.sample_id = ?");
            pInfPstmt.setString(1, currentSampleId);
            ResultSet rsInf = pInfPstmt.executeQuery();
            if (rsInf.next()) {
                pId = rsInf.getString("patient_id");
                pName = rsInf.getString("name");
                pAge = String.valueOf(rsInf.getInt("age"));
                pGender = rsInf.getString("gender");
                address = rsInf.getString("address");
                pRef = rsInf.getString("referred_doctor");
                sDate = rsInf.getString("collection_date");
                phone = rsInf.getString("whatsapp");
                if (phone == null || phone.isEmpty())
                    phone = rsInf.getString("phone");
            }

            // 2. Aggregate Results for Selected Tests
            List<ReportGenerator.TestData> allTestData = new ArrayList<>();
            for (Test t : selected) {
                String testSid = testToSampleMap.getOrDefault(t, currentSampleId);
                List<Map<String, String>> testResults = new ArrayList<>();
                String commentFound = "";

                // Fetch full parameter info to use model logic
                String resSql = "SELECT tp.id, tp.name, tp.unit, tp.category, tp.min_range, tp.max_range, tp.min_range_male, tp.max_range_male, "
                        +
                        "tp.min_range_female, tp.max_range_female, tp.min_range_kids, tp.max_range_kids, r.value, r.is_abnormal, r.status, r.comment "
                        +
                        "FROM test_parameters tp " +
                        "LEFT JOIN results r ON r.parameter_id = tp.id AND r.sample_id = ? " +
                        "WHERE tp.test_id = ?";
                try (PreparedStatement rsPstmt = conn.prepareStatement(resSql)) {
                    rsPstmt.setString(1, testSid);
                    rsPstmt.setInt(2, t.getId());
                    try (ResultSet resRs = rsPstmt.executeQuery()) {
                        while (resRs.next()) {
                            if (commentFound.isEmpty()) {
                                String c = resRs.getString("comment");
                                if (c != null)
                                    commentFound = c;
                            }

                            // Use Model for consistent range logic
                            TestParameter tp = new TestParameter(resRs.getInt("id"), t.getId(), resRs.getString("name"),
                                    resRs.getString("unit"), resRs.getString("min_range"), resRs.getString("max_range"),
                                    resRs.getString("min_range_male"), resRs.getString("max_range_male"),
                                    resRs.getString("min_range_female"), resRs.getString("max_range_female"),
                                    resRs.getString("min_range_kids"), resRs.getString("max_range_kids"));

                            tp.setCategory(resRs.getString("category"));
                            tp.setValue(resRs.getString("value"));

                            Map<String, String> map = new HashMap<>();
                            map.put("name", tp.getName());
                            map.put("value",
                                    (tp.getValue() == null || tp.getValue().trim().isEmpty()) ? " " : tp.getValue());
                            map.put("unit", tp.getUnit());
                            map.put("category", tp.getCategory());
                            map.put("range", tp.getRange(pGender, pAge));
                            map.put("status", tp.isAbnormal(pGender, pAge) ? "ABNORMAL" : "NORMAL");
                            map.put("_raw_comment", resRs.getString("comment"));
                            testResults.add(map);
                        }
                    }
                }
                // Fetch Images for Report
                String refImg = "";
                List<String> resImgs = new ArrayList<>();
                PreparedStatement imgPstmt = conn.prepareStatement("SELECT image_path FROM tests WHERE id = ?");
                imgPstmt.setInt(1, t.getId());
                ResultSet rsImg = imgPstmt.executeQuery();
                if (rsImg.next())
                    refImg = rsImg.getString("image_path");

                PreparedStatement resImgPstmt = conn
                        .prepareStatement("SELECT image_path FROM test_images WHERE sample_id = ? AND test_id = ?");
                resImgPstmt.setString(1, testSid);
                resImgPstmt.setInt(2, t.getId());
                ResultSet rsResImg = resImgPstmt.executeQuery();
                while (rsResImg.next())
                    resImgs.add(rsResImg.getString("image_path"));

                ReportGenerator.TestData td = new ReportGenerator.TestData(t.getName(), t.getId(), t.getCategory(),
                        testResults, t.getNotes(), t.getIsSpecial(), t.getIsMicroscopic(), t.getIsCulture(),
                        t.getSpecimen(), commentFound, refImg, resImgs, testSid);
                if (t.getIsCulture() == 1) {
                    // Try to identify the selected organism and restore growth findings
                    for (Map<String, String> rRow : testResults) {
                        String v = rRow.get("value");
                        String rawC = rRow.get("_raw_comment");
                        if (rawC != null && rawC.startsWith("[GROWTH]:")) {
                            td.growthFindings = rawC.split("\\[OPINION\\]:")[0].replace("[GROWTH]:", "");
                        }
                        if (v != null
                                && (v.equalsIgnoreCase("S") || v.equalsIgnoreCase("R") || v.equalsIgnoreCase("I"))) {
                            td.selectedOrganism = rRow.get("unit");
                        }
                    }
                }
                allTestData.add(td);
            }

            // 3. Generate Consolidated Report
            boolean includeHeader = Boolean
                    .parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("print_header_footer", "true"));
            String pdfPath = ReportGenerator.generateMultiTestReport(pName, pId, pAge, pGender, pRef, sDate, phone,
                    address, allTestData, includeHeader, true);
            if (pdfPath != null) {
                showSuccessActions(pdfPath, phone, allTestData, pName, pId, pAge, pGender, pRef, sDate, address);
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to generate report.").show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error printing reports: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handlePrintBlankReport() {
        if (currentSampleId == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a patient first.").show();
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            String pName = "", pId = "", pAge = "", pGender = "", pRef = "", sDate = "", phone = "", address = "";
            PreparedStatement pInfPstmt = conn.prepareStatement(
                    "SELECT p.patient_id, p.name, p.age, p.age_months, p.age_days, p.gender, p.address, p.referred_doctor, p.whatsapp, p.phone, s.collection_date "
                            +
                            "FROM patients p JOIN samples s ON p.patient_id = s.patient_id WHERE s.sample_id = ?");
            pInfPstmt.setString(1, currentSampleId);
            ResultSet rsInf = pInfPstmt.executeQuery();
            if (rsInf.next()) {
                pId = rsInf.getString("patient_id");
                pName = rsInf.getString("name");
                pAge = String.valueOf(rsInf.getInt("age"));
                pGender = rsInf.getString("gender");
                address = rsInf.getString("address");
                pRef = rsInf.getString("referred_doctor");
                sDate = rsInf.getString("collection_date");
                phone = rsInf.getString("whatsapp");
                if (phone == null || phone.isEmpty())
                    phone = rsInf.getString("phone");
            }

            // Only include SELECTED tests in the blank report
            List<Integer> testIds = new ArrayList<>();
            for (Test t : testQueue) {
                if (t.isSelected()) {
                    testIds.add(t.getId());
                }
            }
            // If nothing selected, fall back to all tests in queue
            if (testIds.isEmpty()) {
                for (Test t : testQueue)
                    testIds.add(t.getId());
            }

            boolean includeHeader = Boolean
                    .parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("print_header_footer", "true"));
            String pdfPath = ReportGenerator.generateBlankReport(pName, pId, pAge, pGender, pRef, sDate, phone, address,
                    testIds, includeHeader, true);
            if (pdfPath != null) {
                showSuccessActions(pdfPath, phone, null, pName, pId, pAge, pGender, pRef, sDate, address);
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error printing blank report: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleNext() {
        String doctorEnabled = DatabaseManager.getSetting("enable_doctor_review", "true");
        if (Boolean.parseBoolean(doctorEnabled)) {
            com.lab.lms.services.NavigationService.switchView("/fxml/review.fxml");
        } else {
            com.lab.lms.services.NavigationService.switchView("/fxml/history.fxml");
        }
    }

    @FXML
    private void handleEditTestGlobal() {
        Test selectedTest = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a test protocol from the queue first.").show();
            return;
        }

        Stage editStage = new Stage();
        editStage.setTitle("Enhanced Edit Test Template: " + selectedTest.getName());
        
        // Notes Editor
        Label notesLabel = new Label("Global Test Notes / Clinical Opinion Template:");
        notesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #961111;");
        javafx.scene.web.HTMLEditor testNotesEditor = new javafx.scene.web.HTMLEditor();
        testNotesEditor.setPrefHeight(150);
        testNotesEditor.setHtmlText(selectedTest.getNotes() != null ? selectedTest.getNotes() : "");
        Button btnSaveNotes = new Button("SAVE NOTES GLOBALLY");
        btnSaveNotes.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSaveNotes.setOnAction(e -> {
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE tests SET notes = ? WHERE id = ?");
                ps.setString(1, testNotesEditor.getHtmlText());
                ps.setInt(2, selectedTest.getId());
                ps.executeUpdate();
                selectedTest.setNotes(testNotesEditor.getHtmlText());
                new Alert(Alert.AlertType.INFORMATION, "Global notes updated successfully!").show();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        VBox topBox = new VBox(5, notesLabel, testNotesEditor, btnSaveNotes);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-border-color: #CFD8DC; -fx-border-width: 0 0 1 0;");

        // Middle Table
        TableView<TestParameter> paramTable = new TableView<>();
        TableColumn<TestParameter, String> colName = new TableColumn<>("Parameter Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<TestParameter, String> colUnit = new TableColumn<>("Unit");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        paramTable.getColumns().addAll(colName, colUnit);
        paramTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paramTable.setPrefHeight(200);

        // Detail Form
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(10, 0, 10, 0));
        
        TextField nameField = new TextField();
        TextField unitField = new TextField();
        TextField minGeneral = new TextField(); TextField maxGeneral = new TextField();
        TextField minMale = new TextField(); TextField maxMale = new TextField();
        TextField minFemale = new TextField(); TextField maxFemale = new TextField();
        TextField minKids = new TextField(); TextField maxKids = new TextField();
        
        form.add(new Label("Name:"), 0, 0); form.add(nameField, 1, 0, 3, 1);
        form.add(new Label("Unit:"), 0, 1); form.add(unitField, 1, 1, 3, 1);
        
        form.add(new Label("General Min:"), 0, 2); form.add(minGeneral, 1, 2);
        form.add(new Label("General Max:"), 2, 2); form.add(maxGeneral, 3, 2);
        
        form.add(new Label("Male Min:"), 0, 3); form.add(minMale, 1, 3);
        form.add(new Label("Male Max:"), 2, 3); form.add(maxMale, 3, 3);

        form.add(new Label("Female Min:"), 0, 4); form.add(minFemale, 1, 4);
        form.add(new Label("Female Max:"), 2, 4); form.add(maxFemale, 3, 4);
        
        form.add(new Label("Kids Min:"), 0, 5); form.add(minKids, 1, 5);
        form.add(new Label("Kids Max:"), 2, 5); form.add(maxKids, 3, 5);
        
        Button btnSaveGlobal = new Button("SAVE TEMPLATE GLOBALLY");
        btnSaveGlobal.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-weight: bold;");
        Button btnSavePatient = new Button("SAVE TO CURRENT PATIENT");
        btnSavePatient.setStyle("-fx-background-color: #455A64; -fx-text-fill: white; -fx-font-weight: bold;");
        Button btnAddNew = new Button("+ ADD NEW TO GLOBALLY");
        btnAddNew.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox actionBox = new HBox(10, btnSaveGlobal, btnSavePatient, btnAddNew);

        ObservableList<TestParameter> globalParams = FXCollections.observableArrayList();
        Runnable loadParams = () -> {
            globalParams.clear();
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM test_parameters WHERE test_id = ?");
                ps.setInt(1, selectedTest.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    TestParameter p = new TestParameter(rs.getInt("id"), selectedTest.getId(), rs.getString("name"), rs.getString("unit"),
                            rs.getString("min_range"), rs.getString("max_range"),
                            rs.getString("min_range_male"), rs.getString("max_range_male"),
                            rs.getString("min_range_female"), rs.getString("max_range_female"),
                            rs.getString("min_range_kids"), rs.getString("max_range_kids"));
                    globalParams.add(p);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            paramTable.refresh();
        };
        loadParams.run();
        paramTable.setItems(globalParams);

        paramTable.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            if(p != null) {
                nameField.setText(p.getName()); unitField.setText(p.getUnit());
                minGeneral.setText(p.getMinRange()); maxGeneral.setText(p.getMaxRange());
                minMale.setText(p.getMinRangeMale()); maxMale.setText(p.getMaxRangeMale());
                minFemale.setText(p.getMinRangeFemale()); maxFemale.setText(p.getMaxRangeFemale());
                minKids.setText(p.getMinRangeKids()); maxKids.setText(p.getMaxRangeKids());
            }
        });

        btnSaveGlobal.setOnAction(e -> {
            TestParameter p = paramTable.getSelectionModel().getSelectedItem();
            if(p == null) return;
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE test_parameters SET name=?, unit=?, min_range=?, max_range=?, min_range_male=?, max_range_male=?, min_range_female=?, max_range_female=?, min_range_kids=?, max_range_kids=? WHERE id=?");
                ps.setString(1, nameField.getText()); ps.setString(2, unitField.getText());
                ps.setString(3, minGeneral.getText()); ps.setString(4, maxGeneral.getText());
                ps.setString(5, minMale.getText()); ps.setString(6, maxMale.getText());
                ps.setString(7, minFemale.getText()); ps.setString(8, maxFemale.getText());
                ps.setString(9, minKids.getText()); ps.setString(10, maxKids.getText());
                ps.setInt(11, p.getId());
                ps.executeUpdate();
                loadParams.run();
                new Alert(Alert.AlertType.INFORMATION, "Global parameter updated!").show();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        btnSavePatient.setOnAction(e -> {
            TestParameter p = paramTable.getSelectionModel().getSelectedItem();
            if(p == null || currentSampleId == null) {
                new Alert(Alert.AlertType.WARNING, "No parameter selected or no active patient sample.").show();
                return;
            }
            TestParameter tempP = new TestParameter(p.getId(), p.getTestId(), nameField.getText(), unitField.getText(), 
                    minGeneral.getText(), maxGeneral.getText(), minMale.getText(), maxMale.getText(), 
                    minFemale.getText(), maxFemale.getText(), minKids.getText(), maxKids.getText());
            String effectiveRange = tempP.getRange(currentGender, currentAgeStr);

            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE results SET name_override=?, unit_override=?, range_override=? WHERE sample_id=? AND test_name=? AND parameter_name=?");
                ps.setString(1, nameField.getText()); ps.setString(2, unitField.getText()); ps.setString(3, effectiveRange);
                ps.setString(4, currentSampleId); ps.setString(5, selectedTest.getName()); ps.setString(6, p.getName());
                int mod = ps.executeUpdate();
                if(mod > 0) new Alert(Alert.AlertType.INFORMATION, "Saved locally for this patient's specific demographics (" + effectiveRange + ").").show();
                else new Alert(Alert.AlertType.WARNING, "Parameter not found in this patient's active result queue.").show();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        btnAddNew.setOnAction(e -> {
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name required for new parameter.").show();
                return;
            }
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO test_parameters (test_id, name, unit, min_range, max_range, min_range_male, max_range_male, min_range_female, max_range_female, min_range_kids, max_range_kids) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, selectedTest.getId()); ps.setString(2, nameField.getText()); ps.setString(3, unitField.getText());
                ps.setString(4, minGeneral.getText()); ps.setString(5, maxGeneral.getText());
                ps.setString(6, minMale.getText()); ps.setString(7, maxMale.getText());
                ps.setString(8, minFemale.getText()); ps.setString(9, maxFemale.getText());
                ps.setString(10, minKids.getText()); ps.setString(11, maxKids.getText());
                ps.executeUpdate();
                loadParams.run();
                new Alert(Alert.AlertType.INFORMATION, "New parameter added globally!").show();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        VBox bottomBox = new VBox(10, new Label("Select parameter to edit, or Add New"), form, actionBox);
        bottomBox.setPadding(new Insets(10));

        VBox layout = new VBox(topBox, paramTable, bottomBox);
        editStage.setScene(new javafx.scene.Scene(layout, 700, 750));
        editStage.setOnHidden(e -> loadTestParameters(selectedTest.getId()));
        editStage.show();
    }

    @FXML
    private void handleAddCustomParameter() {
        Test selectedTest = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a test protocol from the queue first.").show();
            return;
        }

        Dialog<TestParameter> dialog = new Dialog<>();
        dialog.setTitle("Add Custom Parameter");
        dialog.setHeaderText("Add a new parameter to: " + selectedTest.getName());

        ButtonType addButtonType = new ButtonType("ADD", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        boolean isMic = selectedTest.getIsMicroscopic() == 1;

        TextField nameField = new TextField();
        nameField.setPromptText("Parameter Name (e.g. Iron)");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        if (isMic) {
            // Fetch existing categories for this test to populate the dropdown
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT DISTINCT unit FROM test_parameters WHERE test_id = ? AND unit IS NOT NULL AND unit != ''");
                ps.setInt(1, selectedTest.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    categoryCombo.getItems().add(rs.getString("unit"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (categoryCombo.getItems().isEmpty()) {
                categoryCombo.getItems().addAll("Physical Examination", "Chemical Examination",
                        "Microscopic Examination");
            }
            categoryCombo.setPromptText("Select or Type Category");
        }

        TextField unitField = new TextField();
        unitField.setPromptText("Unit (e.g. mg/dL)");

        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.setItems(bacteriaComboList);
        unitCombo.setEditable(true);
        unitCombo.setPromptText("Select Organism");

        TextField rangeField = new TextField();
        rangeField.setPromptText("Range (e.g. 10 - 20)");
        TextField valueField = new TextField();
        valueField.setPromptText("Result Value");
        boolean isCul = selectedTest.getIsCulture() == 1;

        if (isCul) {
            grid.add(new Label("Drug Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Sensitivity (Result):"), 0, 1);
            grid.add(valueField, 1, 1);
        } else if (isMic) {
            grid.add(new Label("Parameter Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Category:"), 0, 1);
            grid.add(categoryCombo, 1, 1);
            grid.add(new Label("Initial Result:"), 0, 2);
            grid.add(valueField, 1, 2);
        } else {
            grid.add(new Label("Parameter Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Unit:"), 0, 1);
            grid.add(unitField, 1, 1);
            grid.add(new Label("Ref. Range:"), 0, 2);
            grid.add(rangeField, 1, 2);
            grid.add(new Label("Initial Result:"), 0, 3);
            grid.add(valueField, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                // For culture, use the main screen's identified organism as the 'unit'
                // (category) for the new drug
                String unitVal = isCul ? bacteriaSelectCombo.getText()
                        : (isMic ? categoryCombo.getEditor().getText() : unitField.getText());
                String range = isMic ? "" : rangeField.getText();
                String min = range, max = "";
                if (!isMic && !isCul && range.contains("-")) {
                    String[] split = range.split("-", 2);
                    min = split[0].trim();
                    max = split[1].trim();
                }
                TestParameter tp = new TestParameter(0, selectedTest.getId(), nameField.getText(),
                        unitVal, min, max);
                if (isMic)
                    tp.setCategory(unitVal);
                tp.setValue(valueField.getText());
                return tp;
            }
            return null;
        });

        Optional<TestParameter> result = dialog.showAndWait();
        result.ifPresent(tp -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Save Configuration");
            confirm.setHeaderText("New Clinical Parameter Detected");
            confirm.setContentText("Do you want to save this parameter globally in the Test Repository for all future "
                    + selectedTest.getName() + " tests?");

            ButtonType btnYes = new ButtonType("YES (SAVE GLOBALLY)");
            ButtonType btnNo = new ButtonType("NO (THIS PATIENT ONLY)");
            confirm.getButtonTypes().setAll(btnYes, btnNo, ButtonType.CANCEL);

            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isPresent() && choice.get() != ButtonType.CANCEL) {
                boolean saveGlobally = (choice.get() == btnYes);
                saveCustomParameter(tp, selectedTest.getId(), saveGlobally);
            }
        });
    }

    private void saveCustomParameter(TestParameter tp, int testId, boolean saveGlobally) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Create the parameter record
            String pSql = "INSERT INTO test_parameters (test_id, name, unit, min_range, max_range, is_global, category) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pPstmt = conn.prepareStatement(pSql, Statement.RETURN_GENERATED_KEYS);
            pPstmt.setInt(1, testId);
            pPstmt.setString(2, tp.getName());
            pPstmt.setString(3, tp.getUnit());
            pPstmt.setString(4, tp.getMinRange());
            pPstmt.setString(5, tp.getMaxRange());
            pPstmt.setInt(6, saveGlobally ? 1 : 0);
            pPstmt.setString(7, tp.getCategory());
            pPstmt.executeUpdate();

            int newParamId = -1;
            ResultSet grs = pPstmt.getGeneratedKeys();
            if (grs.next())
                newParamId = grs.getInt(1);

            if (newParamId != -1) {
                // 2. Create the result entry for current sample
                String rSql = "INSERT INTO results (sample_id, test_id, parameter_id, value, is_abnormal, status) VALUES (?, ?, ?, ?, ?, 'COMPLETED')";
                PreparedStatement rPstmt = conn.prepareStatement(rSql);
                rPstmt.setString(1, currentSampleId);
                rPstmt.setInt(2, testId);
                rPstmt.setInt(3, newParamId);
                rPstmt.setString(4, tp.getValue());
                rPstmt.setInt(5, tp.isAbnormal() ? 1 : 0);
                rPstmt.executeUpdate();

                // 3. Update UI
                loadTestParameters(testId);
                new Alert(Alert.AlertType.INFORMATION, "Parameter added successfully.").show();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleAddOpinion() {
        boolean v = !opinionBox.isVisible();
        opinionBox.setVisible(v);
        opinionBox.setManaged(v);

        // Show Lab Comment Box implicitly if it's NOT a Culture, matching previous
        // behavior
        Test selectedTest = testsToProcessTable.getSelectionModel().getSelectedItem();
        boolean isCulture = (selectedTest != null && selectedTest.getIsCulture() == 1);

        if (!isCulture) {
            labCommentBox.setVisible(v);
            labCommentBox.setManaged(v);
        }

        if (v)
            testCommentsArea.requestFocus();
    }

    @FXML
    private void handleAddLabComment() {
        boolean v = !labCommentBox.isVisible();
        labCommentBox.setVisible(v);
        labCommentBox.setManaged(v);
        if (v)
            testLabCommentsArea.requestFocus();
    }

    @FXML
    private void handleToggleCultureResult() {
        boolean v = !cultureOptionsBox.isVisible();
        cultureOptionsBox.setVisible(v);
        cultureOptionsBox.setManaged(v);
        if (v) {
            if ("Negative".equals(growthStatusCombo.getSelectionModel().getSelectedItem())) {
                cultureGrowthDaysField.requestFocus();
            } else {
                bacteriaSelectCombo.requestFocus();
            }
        }
    }

    @FXML
    private void handleGrowthStatusChange() {
        String status = growthStatusCombo.getSelectionModel().getSelectedItem();
        boolean isNeg = "Negative".equalsIgnoreCase(status);

        if (isNeg) {
            updateCultureResultSentence();
            // Hide sensitivity table if strictly negative, but keep bacteria selection
            // enabled
            resultsEntryTable.setVisible(false);
            resultsEntryTable.setManaged(false);
            sensitivityTableLabel.setVisible(false);
            sensitivityTableLabel.setManaged(false);

            cultureGrowthDaysField.setDisable(false);
            bacteriaSelectCombo.setDisable(false); // ALWAYS ENABLED AS PER USER REQUEST
            cultureGrowthField.setDisable(false);
        } else {
            updateCultureResultSentence();
            resultsEntryTable.setVisible(true);
            resultsEntryTable.setManaged(true);
            sensitivityTableLabel.setVisible(true);
            sensitivityTableLabel.setManaged(true);

            cultureGrowthDaysField.setDisable(false);
            bacteriaSelectCombo.setDisable(false);
            cultureGrowthField.setDisable(false);

            // Show all possible antibiotics/drugs when Positive is selected to simplify
            // flow
            currentParameters.setAll(allCultureParameters);
        }
    }

    @FXML
    private void handleSaveOrganismToQueue() {
        Test sel = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getIsCulture() == 0 || currentSampleId == null)
            return;

        String organism = bacteriaSelectCombo.getText();
        String specimenType = cultureTypeSelectCombo.getText();
        String status = (growthStatusCombo.getSelectionModel().getSelectedItem() != null)
                ? growthStatusCombo.getSelectionModel().getSelectedItem()
                : "Positive";
        String findings = cultureGrowthField.getText();
        String duration = cultureGrowthDaysField.getText();

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Update Test Definition (Staging Template)
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE tests SET growth_status = ?, growth_findings = ?, specimen = ? WHERE id = ?");
            pstmt.setString(1, status);
            pstmt.setString(2, findings);
            pstmt.setString(3, specimenType);
            pstmt.setInt(4, sel.getId());
            pstmt.executeUpdate();

            // 2. Update Patient-Specific Results (Live Persistence)
            String resSql = "UPDATE results SET identified_organism = ?, culture_type = ?, growth_status = ?, growth_findings = ?, duration = ? "
                    +
                    "WHERE sample_id = ? AND test_id = ?";
            try (PreparedStatement resPstmt = conn.prepareStatement(resSql)) {
                resPstmt.setString(1, organism);
                resPstmt.setString(2, specimenType);
                resPstmt.setString(3, status);
                resPstmt.setString(4, findings);
                resPstmt.setString(5, duration);
                resPstmt.setString(6, currentSampleId);
                resPstmt.setInt(7, sel.getId());
                resPstmt.executeUpdate();
            }

            new Alert(Alert.AlertType.INFORMATION,
                    "Clinical findings for " + sel.getName() + " have been saved successfully.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Saving Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleInsertMSWordTable() {
        com.lab.lms.util.UIHelper.showInteractiveTableBuilder(testCommentsArea.getScene().getWindow(), tableHtml -> {
            String old = testCommentsArea.getText();
            testCommentsArea.setText((old == null ? "" : old) + tableHtml);
            testCommentsArea.requestFocus();
        });
    }

    @FXML
    private void handlePrintEmptyTemplate() {
        handlePreviewReport();
    }

    @FXML
    private void handlePreviewReport() {
        Test selected = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a test protocol first.").show();
            return;
        }

        List<Map<String, String>> reportData = new ArrayList<>();
        for (TestParameter tp : currentParameters) {
            Map<String, String> map = new HashMap<>();
            map.put("name", tp.getName());
            map.put("value", tp.getValue());
            map.put("category", tp.getCategory());
            map.put("range", tp.getRange("Male", "30y")); // Placeholder for preview
            reportData.add(map);
        }

        String comment = testCommentsArea.getText();
        if (comment == null)
            comment = "";

        ReportGenerator.TestData previewTD = new ReportGenerator.TestData(selected.getName(), selected.getId(),
                selected.getCategory(), reportData, selected.getNotes(), selected.getIsSpecial(),
                selected.getIsMicroscopic(),
                selected.getIsCulture(), selected.getSpecimen(), comment, null, null, currentSampleId);

        if (selected.getIsCulture() == 1) {
            previewTD.growthFindings = cultureGrowthField.getText();
            previewTD.growthStatus = growthStatusCombo.getSelectionModel().getSelectedItem();
            previewTD.selectedOrganism = bacteriaSelectCombo.getText();
        }

        String previewPath = ReportGenerator.generatePreviewFromTD(previewTD);

        if (previewPath != null) {
            try {
                Desktop.getDesktop().open(new File(previewPath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewHistory() {
        Test selected = testsToProcessTable.getSelectionModel().getSelectedItem();
        if (selected == null || currentPatientId == null) {
            new Alert(Alert.AlertType.WARNING, "Select a test and patient identity first.").show();
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT s.collection_date, r.value, tp.name as param_name, r.completed_at FROM results r " +
                    "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                    "JOIN samples s ON r.sample_id = s.sample_id " +
                    "WHERE s.patient_id = ? AND tp.test_id = ? AND r.status = 'COMPLETED' AND r.sample_id != ? " +
                    "ORDER BY r.completed_at DESC";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, currentPatientId);
            pstmt.setInt(2, selected.getId());
            pstmt.setString(3, currentSampleId);

            ResultSet rs = pstmt.executeQuery();

            // Group by completed_at/session
            Map<String, List<String>> history = new LinkedHashMap<>();
            while (rs.next()) {
                String date = rs.getString("completed_at");
                String param = rs.getString("param_name");
                String value = rs.getString("value");
                history.computeIfAbsent(date, k -> new ArrayList<>()).add(param + ": " + value);
            }

            if (history.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No previous clinical history found for this test.").show();
                return;
            }

            // Create a simple history viewer window
            Stage stage = new Stage();
            stage.setTitle("Clinical History: " + selected.getName());

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: #F9F2F2;");

            Label title = new Label("HISTORY FOR " + selected.getName().toUpperCase());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #961111;");
            root.getChildren().add(title);

            int count = 0;
            for (Map.Entry<String, List<String>> entry : history.entrySet()) {
                if (count >= 3)
                    break;

                VBox card = new VBox(5);
                card.setStyle(
                        "-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #CFD8DC;");

                Label lblDate = new Label("COMPLETED AT: " + entry.getKey());
                lblDate.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                card.getChildren().add(lblDate);

                for (String result : entry.getValue()) {
                    card.getChildren().add(new Label("• " + result));
                }

                root.getChildren().add(card);
                count++;
            }

            ScrollPane sp = new ScrollPane(root);
            sp.setFitToWidth(true);

            stage.setScene(new javafx.scene.Scene(sp, 450, 500));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleParameterEdit(TableColumn.CellEditEvent<TestParameter, String> event, String type) {
        TestParameter tp = event.getRowValue();
        String newValue = event.getNewValue();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Parameter");
        alert.setHeaderText("Choose Update Scope for " + type);
        alert.setContentText("Do you want to apply this change globally or only for this patient?");

        ButtonType btnGlobal = new ButtonType("Save Globally");
        ButtonType btnPatient = new ButtonType("Save for This Patient");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnGlobal, btnPatient, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnGlobal) {
                updateParameterGlobally(tp, type, newValue);
            } else if (result.get() == btnPatient) {
                updateParameterForPatient(tp, type, newValue);
            } else {
                resultsEntryTable.refresh();
            }
        } else {
            resultsEntryTable.refresh();
        }
    }

    private void updateParameterGlobally(TestParameter tp, String type, String newValue) {
        String sql = "";
        if ("NAME".equals(type)) {
            sql = "UPDATE test_parameters SET name = ? WHERE id = ?";
            tp.setName(newValue);
            tp.setNameOverride(null);
        } else if ("UNIT".equals(type)) {
            sql = "UPDATE test_parameters SET unit = ? WHERE id = ?";
            tp.setUnit(newValue);
            tp.setUnitOverride(null);
        } else if ("RANGE".equals(type)) {
             String min = newValue, max = "";
             if (newValue.contains("-")) {
                 String[] parts = newValue.split("-");
                 min = parts[0].trim();
                 max = (parts.length > 1) ? parts[1].trim() : "";
             }
             
             String colMin = "min_range", colMax = "max_range";
             try {
                double age = Double.parseDouble(currentAgeStr.replaceAll("[^0-9.]", ""));
                if (age < 10 && age > 0.001) {
                    colMin = "min_range_kids"; colMax = "max_range_kids";
                } else if (currentGender != null) {
                    if (currentGender.toUpperCase().contains("MALE") && !currentGender.toUpperCase().contains("FEMALE")) {
                        colMin = "min_range_male"; colMax = "max_range_male";
                    } else if (currentGender.toUpperCase().contains("FEMALE")) {
                        colMin = "min_range_female"; colMax = "max_range_female";
                    }
                }
             } catch(Exception e) {}
             
             sql = "UPDATE test_parameters SET " + colMin + " = ?, " + colMax + " = ? WHERE id = ?";
             try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                 pstmt.setString(1, min);
                 pstmt.setString(2, max);
                 pstmt.setInt(3, tp.getId());
                 pstmt.executeUpdate();
             } catch(SQLException e) { e.printStackTrace(); }
             
             if (colMin.equals("min_range")) { tp.setMinRange(min); tp.setMaxRange(max); }
             else if (colMin.contains("kids")) { tp.setMinRangeKids(min); tp.setMaxRangeKids(max); }
             else if (colMin.contains("male")) { tp.setMinRangeMale(min); tp.setMaxRangeMale(max); }
             else if (colMin.contains("female")) { tp.setMinRangeFemale(min); tp.setMaxRangeFemale(max); }
             
             tp.setRangeOverride(null);
             resultsEntryTable.refresh();
             return;
        }

        if (!sql.isEmpty()) {
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newValue);
                pstmt.setInt(2, tp.getId());
                pstmt.executeUpdate();
            } catch(SQLException e) { e.printStackTrace(); }
        }
        resultsEntryTable.refresh();
    }

    private void updateParameterForPatient(TestParameter tp, String type, String newValue) {
        if ("NAME".equals(type)) tp.setNameOverride(newValue);
        else if ("UNIT".equals(type)) tp.setUnitOverride(newValue);
        else if ("RANGE".equals(type)) tp.setRangeOverride(newValue);
        
        String col = ("NAME".equals(type)) ? "name_override" : ("UNIT".equals(type) ? "unit_override" : "range_override");
        String sql = "UPDATE results SET " + col + " = ? WHERE sample_id = ? AND parameter_id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newValue);
            pstmt.setString(2, currentSampleId);
            pstmt.setInt(3, tp.getId());
            pstmt.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
        resultsEntryTable.refresh();
    }
}

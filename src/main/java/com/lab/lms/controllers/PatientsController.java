package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.time.LocalDate;

public class PatientsController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<Patient> patientsTable;
    @FXML
    private TableColumn<Patient, String> colId;
    @FXML
    private TableColumn<Patient, String> colName;
    @FXML
    private TableColumn<Patient, Integer> colAge;
    @FXML
    private TableColumn<Patient, String> colGender;
    @FXML
    private TableColumn<Patient, String> colPhone;
    @FXML
    private TableColumn<Patient, Integer> colTests;
    @FXML
    private TableColumn<Patient, Double> colPaid;
    @FXML
    private TableColumn<Patient, Double> colPending;
    @FXML
    private TableColumn<Patient, Double> colDiscount;
    @FXML
    private TableColumn<Patient, String> colDate;
    @FXML
    private TableColumn<Patient, Void> colAction;

    // Filter controls
    @FXML
    private DatePicker filterFromDate;
    @FXML
    private DatePicker filterToDate;
    @FXML
    private ComboBox<String> filterGender;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();
    private FilteredList<Patient> filteredData;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colTests.setCellValueFactory(new PropertyValueFactory<>("totalTests"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("totalPaid"));
        colPending.setCellValueFactory(new PropertyValueFactory<>("totalPending"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("totalDiscount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("registrationDate"));

        // Paid Inline Update
        colPaid.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            handlePaidFieldUpdate(getTableView().getItems().get(getIndex()));
                        }
                    });
                }
            }
        });

        // Quick Action: Print History
        colAction.setCellFactory(tc -> new TableCell<>() {
            private final Button btnHistory = new Button("PRINT HISTORY");
            {
                btnHistory.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
                btnHistory.setOnAction(e -> handlePrintVisitHistory(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnHistory);
            }
        });

        // Populate gender filter
        filterGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));

        loadPatients();

        // High-Fidelity Debounced Search for Patient Registry
        javafx.animation.PauseTransition patientSearchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
        patientSearchDebounce.setOnFinished(e -> applyFilters());
        
        filteredData = new FilteredList<>(patientList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> patientSearchDebounce.playFromStart());
        patientsTable.setItems(filteredData);

        // Double Click Action
        patientsTable.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Patient rowData = row.getItem();
                    openPatientHistory(rowData.getPatientId());
                }
            });
            return row;
        });
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        LocalDate from = filterFromDate.getValue();
        LocalDate to = filterToDate.getValue();
        String gender = filterGender.getValue();

        filteredData.setPredicate(patient -> {
            // High-Performance Indexed Text Search
            boolean matchesSearch = search.isEmpty()
                    || patient.getLowercaseName().contains(search)
                    || patient.getLowercaseId().contains(search)
                    || patient.getPhone().contains(search);

            // Optimized Gender Matching
            boolean matchesGender = (gender == null || gender.isEmpty())
                    || patient.getGender().equalsIgnoreCase(gender);

            // Sub-Millisecond Date Range Filtering
            boolean matchesDate = true;
            LocalDate pDate = patient.getParsedDate();
            if (pDate != null) {
                if (from != null && pDate.isBefore(from)) matchesDate = false;
                if (to != null && pDate.isAfter(to)) matchesDate = false;
            } else if (from != null || to != null) {
                matchesDate = false; // Filter out entries without valid dates if filter is active
            }

            return matchesSearch && matchesGender && matchesDate;
        });
    }

    @FXML
    private void handleApplyFilter() {
        applyFilters();
    }

    @FXML
    private void handleClearFilter() {
        filterFromDate.setValue(null);
        filterToDate.setValue(null);
        filterGender.setValue(null);
        searchField.clear();
        applyFilters();
    }

    private void loadPatients() {
        patientList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT p.*, " +
                         "(SELECT COUNT(*) FROM results r JOIN samples s ON r.sample_id = s.sample_id WHERE s.patient_id = p.patient_id) as total_tests, " +
                         "(SELECT SUM(paid_amount) FROM invoices WHERE patient_id = p.patient_id) as total_paid, " +
                         "(SELECT SUM(due_amount) FROM invoices WHERE patient_id = p.patient_id) as total_pending, " +
                         "(SELECT SUM(discount) FROM invoices WHERE patient_id = p.patient_id) as total_discount " +
                         "FROM patients p ORDER BY p.id DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                patientList.add(new Patient(
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
                        rs.getString("registration_date"),
                        rs.getInt("total_tests"),
                        rs.getDouble("total_paid"),
                        rs.getDouble("total_pending"),
                        rs.getDouble("total_discount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handlePaidFieldUpdate(Patient rowData) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(rowData.getTotalPaid()));
        dialog.setTitle("Update Patient Payment");
        dialog.setHeaderText("Finance Override: Update Paid Amount for Registry ID " + rowData.getPatientId());
        dialog.setContentText("Enter New Cumulative Paid Amount:");

        dialog.showAndWait().ifPresent(newVal -> {
            try {
                double newPaidTotal = Double.parseDouble(newVal);
                try (Connection conn = DatabaseManager.getConnection()) {
                    // Updating the latest invoice for this patient as the primary override target
                    String updateSql = "UPDATE invoices SET paid_amount = ?, due_amount = final_amount - ?, " +
                                       "status = CASE WHEN final_amount - ? <= 0 THEN 'PAID' ELSE 'PARTIAL' END " +
                                       "WHERE id = (SELECT id FROM invoices WHERE patient_id = ? ORDER BY id DESC LIMIT 1)";
                    PreparedStatement pstmt = conn.prepareStatement(updateSql);
                    pstmt.setDouble(1, newPaidTotal);
                    pstmt.setDouble(2, newPaidTotal);
                    pstmt.setDouble(3, newPaidTotal);
                    pstmt.setString(4, rowData.getPatientId());
                    int affected = pstmt.executeUpdate();
                    if (affected > 0) {
                        loadPatients(); // Refresh registry
                    }
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Amount: " + e.getMessage());
                alert.show();
            }
        });
    }

    private void handlePrintVisitHistory(Patient rowData) {
        try {
            // High-Performance PDF Generation for Subject History
            String pdfPath = com.lab.lms.services.ReportGenerator.generatePatientHistoryReport(rowData.getPatientId());
            if (pdfPath != null) {
                java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
            } else {
                new Alert(Alert.AlertType.WARNING, "No clinical history found for MR: " + rowData.getPatientId()).show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Thermal Printing Error: " + e.getMessage()).show();
        }
    }

    private void openPatientHistory(String pid) {
        com.lab.lms.services.SessionContext.setCurrentPatientId(pid);
        com.lab.lms.services.NavigationService.switchView("/fxml/profile.fxml");
    }
}

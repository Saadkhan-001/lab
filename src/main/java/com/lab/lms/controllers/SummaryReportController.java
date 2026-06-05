package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.SummaryReportRow;
import com.lab.lms.services.ReportGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.HBox;

public class SummaryReportController {

    @FXML private ComboBox<String> filterPeriod;
    @FXML private VBox customDateBox;
    @FXML private VBox customDateBoxEnd;
    @FXML private DatePicker filterFromDate;
    @FXML private DatePicker filterToDate;
    @FXML private ComboBox<String> filterDoctor;
    @FXML private ComboBox<String> filterTest;
    @FXML private TableView<SummaryReportRow> summaryTable;
    @FXML private TableColumn<SummaryReportRow, String> colDate;
    @FXML private TableColumn<SummaryReportRow, String> colMR;
    @FXML private TableColumn<SummaryReportRow, String> colPatient;
    @FXML private TableColumn<SummaryReportRow, String> colTests;
    @FXML private TableColumn<SummaryReportRow, String> colDoctor;
    @FXML private TableColumn<SummaryReportRow, Double> colTotal;
    @FXML private TableColumn<SummaryReportRow, Double> colDiscount;
    @FXML private TableColumn<SummaryReportRow, Double> colAmount; // Styled as NET
    @FXML private TableColumn<SummaryReportRow, Double> colPaid;
    @FXML private TableColumn<SummaryReportRow, Double> colDue;
    @FXML private TableColumn<SummaryReportRow, String> colStatus;
    @FXML private Button btnRefund;
    
    @FXML private Label lblTotalRecords;
    @FXML private Label lblTotalDiscount;
    @FXML private Label lblTotalAmount; // NET Revenue
    @FXML private Label lblTotalPaid;
    @FXML private Label lblTotalDue;
    @FXML private Label lblTotalExpense;
    @FXML private Label lblProfitLoss;

    private ObservableList<SummaryReportRow> reportData = FXCollections.observableArrayList();
    private double currentDisc = 0, currentExp = 0, currentPL = 0;

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colMR.setCellValueFactory(new PropertyValueFactory<>("mrNumber"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colTests.setCellValueFactory(new PropertyValueFactory<>("tests"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("originalTotal"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paid"));
        colDue.setCellValueFactory(new PropertyValueFactory<>("due"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(com.lab.lms.services.SessionContext.getUserRole());
        btnRefund.setVisible(isAdmin);
        btnRefund.setManaged(isAdmin);

        summaryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && summaryTable.getSelectionModel().getSelectedItem() != null) {
                handleRowDoubleClick(summaryTable.getSelectionModel().getSelectedItem());
            }
        });

        filterPeriod.setItems(FXCollections.observableArrayList("Today", "Yesterday", "Last 7 Days", "This Month", "Date to Date"));
        filterPeriod.getSelectionModel().select("Today");
        
        // Add listeners for dynamic test list updates
        filterDoctor.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> updateTestFilter());
        filterFromDate.valueProperty().addListener((obs, old, newVal) -> updateTestFilter());
        filterToDate.valueProperty().addListener((obs, old, newVal) -> updateTestFilter());

        loadDoctors();
        updateTestFilter(); 
        handleFetch();
    }

    private void handleRowDoubleClick(SummaryReportRow row) {
        if (row.getDue() <= 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Account already settled for this patient.");
            alert.show();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Payment Settlement");
        dialog.setHeaderText("Patient: " + row.getPatientName() + " (Pending: " + row.getDue() + " PKR)");
        dialog.setContentText("Enter Amount to Pay:");

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) return;
                if (amount > row.getDue()) {
                    new Alert(Alert.AlertType.ERROR, "Payment cannot exceed pending due!").show();
                    return;
                }
                processPayment(row, amount);
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Invalid amount entered.").show();
            }
        });
    }

    private void processPayment(SummaryReportRow row, double amount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE invoices SET paid_amount = paid_amount + ?, due_amount = due_amount - ? WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setDouble(1, amount);
                pstmt.setDouble(2, amount);
                pstmt.setInt(3, row.getId());
                pstmt.executeUpdate();

                String statusSql = "UPDATE invoices SET status = CASE WHEN due_amount <= 0 THEN 'PAID' ELSE 'PENDING' END WHERE id = ?";
                PreparedStatement spstmt = conn.prepareStatement(statusSql);
                spstmt.setInt(1, row.getId());
                spstmt.executeUpdate();

                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Payment of " + amount + " PKR processed successfully.").show();
                handleFetch();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleRefund() {
        if (reportData.isEmpty()) return;
        List<String> reportDescriptions = new ArrayList<>();
        for (SummaryReportRow r : reportData) {
            reportDescriptions.add(String.format("MR: %s - %s (%s) - Due: %.2f (ID: %d)",
                    r.getMrNumber(), r.getPatientName(), r.getDate(), r.getDue(), r.getId()));
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(reportDescriptions.get(0), reportDescriptions);
        dialog.setTitle("Refund Management");
        dialog.setHeaderText("Select Patient Entry for Refund");

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String selectedDescription = result.get();
            int startIndex = selectedDescription.lastIndexOf("(ID: ") + 5;
            int endIndex = selectedDescription.lastIndexOf(")");
            if (startIndex != -1 && endIndex != -1) {
                try {
                    int selectedId = Integer.parseInt(selectedDescription.substring(startIndex, endIndex));
                    SummaryReportRow row = reportData.stream().filter(r -> r.getId() == selectedId).findFirst().orElse(null);
                    if (row != null) performRefundLookup(row);
                } catch (Exception e) {}
            }
        }
    }

    private void performRefundLookup(SummaryReportRow row) {
        List<String> patientTests = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT t.name, t.price FROM samples s " +
                         "JOIN results r ON s.sample_id = r.sample_id " +
                         "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                         "JOIN tests t ON tp.test_id = t.id " +
                         "WHERE s.patient_id = ? AND s.collection_date LIKE ? " +
                         "GROUP BY t.id";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, row.getMrNumber());
            pstmt.setString(2, row.getDate().substring(0, 10) + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) patientTests.add(rs.getString(1) + " (Price: " + rs.getDouble(2) + " PKR)");
        } catch (SQLException e) { e.printStackTrace(); }

        if (patientTests.isEmpty()) return;

        ChoiceDialog<String> testDialog = new ChoiceDialog<>(patientTests.get(0), patientTests);
        testDialog.setTitle("Test Refund");
        testDialog.setHeaderText("Identify test to refund for MR: " + row.getMrNumber());
        java.util.Optional<String> selectedTest = testDialog.showAndWait();
        if (selectedTest.isPresent()) processRefund(row, selectedTest.get());
    }

    private void processRefund(SummaryReportRow row, String testInfo) {
        String testName = testInfo.split(" \\(")[0];
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Refund '" + testName + "'?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().get() != ButtonType.YES) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double price = 0;
                int testId = -1;
                PreparedStatement pPrice = conn.prepareStatement("SELECT price, id FROM tests WHERE name = ?");
                pPrice.setString(1, testName);
                ResultSet rsP = pPrice.executeQuery();
                if (rsP.next()) { price = rsP.getDouble(1); testId = rsP.getInt(2); }

                // Update results to REFUNDED instead of deleting, to satisfy clinical tracking requirements
                String upResSql = "UPDATE results SET status = 'REFUNDED', value = 'REFUNDED' WHERE parameter_id IN (SELECT id FROM test_parameters WHERE test_id = ?) " +
                                  "AND sample_id IN (SELECT sample_id FROM samples WHERE patient_id = ? AND collection_date LIKE ?)";
                PreparedStatement psUpRes = conn.prepareStatement(upResSql);
                psUpRes.setInt(1, testId);
                psUpRes.setString(2, row.getMrNumber());
                psUpRes.setString(3, row.getDate().substring(0, 10) + "%");
                psUpRes.executeUpdate();

                // Count active results remaining (excluding REFUNDED)
                String countSql = "SELECT COUNT(*) FROM results r JOIN samples s ON r.sample_id = s.sample_id " +
                                 "WHERE s.patient_id = ? AND s.collection_date LIKE ? AND r.status != 'REFUNDED'";
                PreparedStatement psCount = conn.prepareStatement(countSql);
                psCount.setString(1, row.getMrNumber());
                psCount.setString(2, row.getDate().substring(0, 10) + "%");
                ResultSet rsCount = psCount.executeQuery();
                int remaining = rsCount.next() ? rsCount.getInt(1) : 0;
                String newStatus = (remaining == 0) ? "REFUNDED" : "PARTIAL REFUND";

                String upSql = "UPDATE invoices SET total_amount = total_amount - ?, final_amount = final_amount - ?, " +
                               "due_amount = CASE WHEN due_amount > ? THEN due_amount - ? ELSE 0 END, status = ? WHERE id = ?";
                PreparedStatement psUp = conn.prepareStatement(upSql);
                psUp.setDouble(1, price);
                psUp.setDouble(2, price);
                psUp.setDouble(3, price);
                psUp.setDouble(4, price);
                psUp.setString(5, newStatus);
                psUp.setInt(6, row.getId());
                psUp.executeUpdate();

                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Refund Successful. Financial ledgers adjusted.").show();
                handleFetch();
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateTestFilter() {
        LocalDate[] dates = calculateDates();
        LocalDate start = dates[0], end = dates[1];
        if (start == null || end == null) return;

        String selectedDr = filterDoctor.getValue();
        String currentSelection = filterTest.getValue();

        try (Connection conn = DatabaseManager.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT t.name FROM tests t " +
                "JOIN test_parameters tp ON t.id = tp.test_id " +
                "JOIN results r ON tp.id = r.parameter_id " +
                "JOIN samples s ON r.sample_id = s.sample_id " +
                "JOIN patients p ON s.patient_id = p.patient_id " +
                "WHERE date(s.collection_date, 'localtime') BETWEEN ? AND ?");
            
            if (selectedDr != null && !"All Doctors".equals(selectedDr)) sql.append(" AND p.referred_doctor = ?");
            sql.append(" ORDER BY t.name ASC");

            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            if (selectedDr != null && !"All Doctors".equals(selectedDr)) pstmt.setString(3, selectedDr);

            ResultSet rs = pstmt.executeQuery();
            List<String> tests = new ArrayList<>();
            tests.add("All Tests");
            while (rs.next()) tests.add(rs.getString(1));
            
            filterTest.setItems(FXCollections.observableArrayList(tests));
            
            if (currentSelection != null && tests.contains(currentSelection)) {
                filterTest.getSelectionModel().select(currentSelection);
            } else {
                filterTest.getSelectionModel().select("All Tests");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private LocalDate[] calculateDates() {
        String periodText = filterPeriod.getValue();
        LocalDate start = LocalDate.now(), end = LocalDate.now();

        if ("Yesterday".equals(periodText)) { start = end = LocalDate.now().minusDays(1); }
        else if ("Last 7 Days".equals(periodText)) { start = LocalDate.now().minusDays(7); }
        else if ("This Month".equals(periodText)) { start = LocalDate.now().withDayOfMonth(1); }
        else if ("Date to Date".equals(periodText)) { 
            start = filterFromDate.getValue(); 
            end = filterToDate.getValue(); 
        }
        return new LocalDate[]{start, end};
    }

    private void loadDoctors() {
        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT DISTINCT referred_doctor FROM patients WHERE referred_doctor IS NOT NULL AND referred_doctor != ''");
            List<String> doctors = new ArrayList<>();
            doctors.add("All Doctors");
            while (rs.next()) doctors.add(rs.getString(1));
            filterDoctor.setItems(FXCollections.observableArrayList(doctors));
            filterDoctor.getSelectionModel().select("All Doctors");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handlePeriodChange() {
        String selection = filterPeriod.getValue();
        boolean isCustom = "Date to Date".equals(selection);
        customDateBox.setVisible(isCustom);
        customDateBox.setManaged(isCustom);
        customDateBoxEnd.setVisible(isCustom);
        customDateBoxEnd.setManaged(isCustom);
        
        updateTestFilter(); // Dynamic update when period changes
        if (!isCustom) handleFetch();
    }

    @FXML
    private void handleFetch() {
        reportData.clear();
        LocalDate[] dates = calculateDates();
        LocalDate start = dates[0], end = dates[1];
        if (start == null || end == null) return;

        double totalNet = 0, totalDisc = 0, totalPaid = 0, totalDue = 0;
        try (Connection conn = DatabaseManager.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT i.date, i.patient_id, p.name, i.total_amount, i.discount, i.final_amount, i.paid_amount, i.due_amount, i.status, p.referred_doctor, i.id, i.staff_id " +
                "FROM invoices i " +
                "JOIN patients p ON i.patient_id = p.patient_id " +
                "WHERE date(i.date, 'localtime') BETWEEN ? AND ? ");
            
            String selectedDr = filterDoctor.getValue();
            if (selectedDr != null && !"All Doctors".equals(selectedDr)) sql.append(" AND p.referred_doctor = ?");

            String selectedTest = filterTest.getValue();
            if (selectedTest != null && !"All Tests".equals(selectedTest)) {
                sql.append(" AND EXISTS (SELECT 1 FROM samples s " +
                           "JOIN results r ON s.sample_id = r.sample_id " +
                           "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                           "JOIN tests t ON tp.test_id = t.id " +
                           "WHERE s.patient_id = i.patient_id AND s.collection_date LIKE SUBSTR(i.date, 1, 10) || '%' " +
                           "AND t.name = ?)");
            }

            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            
            int paramIdx = 3;
            if (selectedDr != null && !"All Doctors".equals(selectedDr)) pstmt.setString(paramIdx++, selectedDr);
            if (selectedTest != null && !"All Tests".equals(selectedTest)) pstmt.setString(paramIdx++, selectedTest);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String dateStr = rs.getString(1);
                String pid = rs.getString(2);
                String name = rs.getString(3);
                double total = rs.getDouble(4);
                double disc = rs.getDouble(5);
                double net = rs.getDouble(6);
                double paid = rs.getDouble(7);
                double due = rs.getDouble(8);
                String dbStatus = rs.getString(9);
                String dr = rs.getString(10);
                int invId = rs.getInt(11);
                String staff = rs.getString(12);
                
                String status = dbStatus;
                if (!"REFUNDED".equals(dbStatus) && !"PARTIAL REFUND".equals(dbStatus)) {
                    status = (due > 0) ? "PENDING" : dbStatus;
                }
                
                String testsList = "";
                double testsPriceSum = 0;
                String testSql = "SELECT t.name, AVG(t.price) as p FROM samples s " +
                                 "JOIN results r ON s.sample_id = r.sample_id " +
                                 "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                 "JOIN tests t ON tp.test_id = t.id " +
                                 "WHERE s.patient_id = ? AND s.collection_date LIKE ? ";
                
                if (selectedTest != null && !"All Tests".equals(selectedTest)) {
                    testSql += " AND t.name = ?";
                }
                testSql += " GROUP BY t.id"; // Aggregate params into tests

                StringBuilder testsAgg = new StringBuilder();
                try (PreparedStatement tpstmt = conn.prepareStatement(testSql)) {
                    tpstmt.setString(1, pid);
                    tpstmt.setString(2, dateStr.substring(0, 10) + "%");
                    if (selectedTest != null && !"All Tests".equals(selectedTest)) {
                        tpstmt.setString(3, selectedTest);
                    }
                    
                    ResultSet trs = tpstmt.executeQuery();
                    while (trs.next()) {
                        if (testsAgg.length() > 0) testsAgg.append(", ");
                        testsAgg.append(trs.getString(1));
                        testsPriceSum += trs.getDouble(2);
                    }
                    testsList = testsAgg.toString();
                }

                // Granular Intelligence: Calculate Proportional Financials if filtering by test
                double rowTotal = total;
                double rowDisc = disc;
                double rowNet = net;
                double rowPaid = paid;
                double rowDue = due;

                if (selectedTest != null && !"All Tests".equals(selectedTest) && total > 0) {
                    double ratio = testsPriceSum / total;
                    rowTotal = testsPriceSum;
                    rowDisc = disc * ratio;
                    rowNet = net * ratio;
                    rowPaid = paid * ratio;
                    rowDue = due * ratio;
                }

                reportData.add(new SummaryReportRow(invId, dateStr, pid, name, (testsList == null ? "" : testsList), dr, rowTotal, rowDisc, rowNet, rowPaid, rowDue, status, staff));
                totalNet += rowNet;
                totalDisc += rowDisc;
                totalPaid += rowPaid;
                totalDue += rowDue;
            }

            // Fetch Expenses
            double totalExp = 0;
            String expSql = "SELECT SUM(amount) FROM expenses WHERE date(date, 'localtime') BETWEEN ? AND ?";
            PreparedStatement epstmt = conn.prepareStatement(expSql);
            epstmt.setString(1, start.toString());
            epstmt.setString(2, end.toString());
            ResultSet rsExp = epstmt.executeQuery();
            if (rsExp.next()) totalExp = rsExp.getDouble(1);

            updateTotals(reportData.size(), totalDisc, totalNet, totalPaid, totalDue, totalExp);
            
            this.currentDisc = totalDisc;
            this.currentExp = totalExp;
            this.currentPL = totalNet - totalExp;
            
        } catch (SQLException e) { e.printStackTrace(); }

        summaryTable.setItems(reportData);
    }

    private void updateTotals(int records, double disc, double net, double paid, double due, double exp) {
        lblTotalRecords.setText(String.valueOf(records));
        lblTotalDiscount.setText(String.format("%.2f", disc));
        lblTotalAmount.setText(String.format("%.2f", net));
        lblTotalPaid.setText(String.format("%.2f", paid));
        lblTotalDue.setText(String.format("%.2f", due));
        lblTotalExpense.setText(String.format("%.2f PKR", exp));
        
        double pl = net - exp;
        lblProfitLoss.setText(String.format("%.2f PKR", pl));
        lblProfitLoss.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + (pl >= 0 ? "#1A0A0A" : "#961111") + ";");
    }

    @FXML
    private void handleAddExpense() {
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Clinical Expense Entry");
        dialog.setHeaderText("Log new operational expense");

        ButtonType saveButtonType = new ButtonType("SAVE", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField descField = new TextField(); descField.setPromptText("Description (e.g. Chemicals)");
        TextField amountField = new TextField(); amountField.setPromptText("Amount (PKR)");

        grid.add(new Label("Description:"), 0, 0); grid.add(descField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1); grid.add(amountField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) return new javafx.util.Pair<>(descField.getText(), amountField.getText());
            return null;
        });

        java.util.Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            try {
                double amount = Double.parseDouble(pair.getValue());
                saveExpense(pair.getKey(), amount);
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Invalid Data Entry.").show();
            }
        });
    }

    private void saveExpense(String desc, double amount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "INSERT INTO expenses (description, amount, staff_id) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, desc);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, com.lab.lms.services.SessionContext.getUsername());
            pstmt.executeUpdate();
            new Alert(Alert.AlertType.INFORMATION, "Expense logged successfully.").show();
            handleFetch();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handlePrintSummary() {
        if (reportData.isEmpty()) return;
        
        // Resolve a high-fidelity period string for the report header
        String periodText = filterPeriod.getValue();
        LocalDate startD = LocalDate.now(), endD = LocalDate.now();
        
        if ("Yesterday".equalsIgnoreCase(periodText)) { startD = endD = LocalDate.now().minusDays(1); }
        else if ("Last 7 Days".equalsIgnoreCase(periodText)) { startD = LocalDate.now().minusDays(7); }
        else if ("This Month".equalsIgnoreCase(periodText)) { startD = LocalDate.now().withDayOfMonth(1); }
        else if ("Date to Date".equalsIgnoreCase(periodText)) { 
            startD = filterFromDate.getValue() != null ? filterFromDate.getValue() : LocalDate.now(); 
            endD = filterToDate.getValue() != null ? filterToDate.getValue() : LocalDate.now(); 
        }
        
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String finalPeriod = startD.isEqual(endD) ? startD.format(dtf) : startD.format(dtf) + " to " + endD.format(dtf);
        
        List<String> options = new ArrayList<>();
        options.add("Sales Performance Log (Detailed)");
        options.add("Doctor Revenue Summary (Aggregated)");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Report Format Selection");
        dialog.setHeaderText("Choose Summary Format for Print");

        dialog.showAndWait().ifPresent(selection -> {
            String pdfPath = null;
            if (selection.contains("Performance")) {
                pdfPath = ReportGenerator.generateSummaryReportThermal(
                    new ArrayList<>(reportData), finalPeriod, filterDoctor.getValue(), currentDisc, currentExp, currentPL);
            } else {
                pdfPath = ReportGenerator.generateDoctorsSummaryReportThermal(
                    new ArrayList<>(reportData), finalPeriod, currentDisc, currentExp, currentPL);
            }
            if (pdfPath != null) {
                try { java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath)); } catch (Exception e) {}
            }
        });
    }

    @FXML
    private void handleBack() {
        if (DashboardController.getInstance() != null) DashboardController.getInstance().handleLogoClick();
    }

    // ============================================
    // DOCTOR COMMISSION PORTAL INTEGRATION
    // ============================================

    public static class DoctorRow {
        private final javafx.beans.property.SimpleIntegerProperty id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleDoubleProperty commission;

        public DoctorRow(int id, String name, double commission) {
            this.id = new javafx.beans.property.SimpleIntegerProperty(id);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.commission = new javafx.beans.property.SimpleDoubleProperty(commission);
        }

        public int getId() { return id.get(); }
        public String getName() { return name.get(); }
        public double getCommission() { return commission.get(); }
        public void setCommission(double val) { commission.set(val); }
    }

    @FXML
    private void handleOpenDoctorPortal() {
        Dialog<Void> portalDialog = new Dialog<>();
        portalDialog.setTitle("Doctor Commission Management");
        portalDialog.setHeaderText("Manage Registered Doctors and Commission Framework");
        portalDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox rootBox = new VBox(15);
        rootBox.setPadding(new javafx.geometry.Insets(15));
        rootBox.setPrefWidth(600);
        rootBox.setPrefHeight(500);

        // --- SECTION 1: Add New Doctor ---
        HBox addBox = new HBox(10);
        addBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField newDocNameField = new TextField();
        newDocNameField.setPromptText("Enter New Doctor Name");
        newDocNameField.setPrefWidth(300);
        Button btnAddDoctor = new Button("➕ ADD DOCTOR");
        btnAddDoctor.setStyle("-fx-background-color: #455A64; -fx-text-fill: white; -fx-font-weight: bold;");

        addBox.getChildren().addAll(new Label("New Doctor:"), newDocNameField, btnAddDoctor);

        // --- SECTION 2: Doctor Table ---
        TableView<DoctorRow> docTable = new TableView<>();
        docTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        docTable.setPrefHeight(300);

        TableColumn<DoctorRow, String> colName = new TableColumn<>("DOCTOR NAME");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<DoctorRow, Double> colComm = new TableColumn<>("COMMISSION (%)");
        colComm.setCellValueFactory(new PropertyValueFactory<>("commission"));
        colComm.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        docTable.getColumns().addAll(colName, colComm);

        ObservableList<DoctorRow> doctorsList = FXCollections.observableArrayList();

        java.lang.Runnable refreshDoctors = () -> {
            doctorsList.clear();
            try (Connection conn = DatabaseManager.getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT id, name, commission_percentage FROM doctors ORDER BY name ASC");
                while (rs.next()) {
                    doctorsList.add(new DoctorRow(rs.getInt(1), rs.getString(2), rs.getDouble(3)));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            docTable.setItems(doctorsList);
        };
        refreshDoctors.run();

        // Add action
        btnAddDoctor.setOnAction(e -> {
            String name = newDocNameField.getText().trim();
            if (name.isEmpty()) return;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO doctors (name, commission_percentage) VALUES (?, 0.0)")) {
                pstmt.setString(1, name);
                pstmt.executeUpdate();
                newDocNameField.clear();
                refreshDoctors.run();
                loadDoctors(); // Refresh main UI dropdown
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, "Error adding doctor. Perhaps name already exists?").show();
            }
        });

        // Edit on double click
        docTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && docTable.getSelectionModel().getSelectedItem() != null) {
                DoctorRow selected = docTable.getSelectionModel().getSelectedItem();
                TextInputDialog pctDialog = new TextInputDialog(String.valueOf(selected.getCommission()));
                pctDialog.setTitle("Update Commission");
                pctDialog.setHeaderText("Set Commission Percentage for " + selected.getName());
                pctDialog.setContentText("Percentage (e.g. 15.5):");
                pctDialog.showAndWait().ifPresent(val -> {
                    try {
                        double pct = Double.parseDouble(val);
                        if (pct < 0 || pct > 100) throw new NumberFormatException("Invalid bounds");
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement("UPDATE doctors SET commission_percentage = ? WHERE id = ?")) {
                            pstmt.setDouble(1, pct);
                            pstmt.setInt(2, selected.getId());
                            pstmt.executeUpdate();
                            refreshDoctors.run();
                        }
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Invalid percentage value.").show();
                    }
                });
            }
        });

        // --- SECTION 3: Commission Printing ---
        VBox printBox = new VBox(10);
        printBox.setStyle("-fx-padding: 15; -fx-border-color: #CFD8DC; -fx-border-radius: 4; -fx-background-color: white;");
        Label lblPrintTitle = new Label("🖨️ PRINT DOCTOR COMMISSION REPORT");
        lblPrintTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #961111;");

        HBox printControls = new HBox(15);
        printControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ComboBox<String> cbPrintPeriod = new ComboBox<>(FXCollections.observableArrayList(
                "Today", "Yesterday", "Last 7 Days", "This Month"
        ));
        cbPrintPeriod.setValue("Today");

        ComboBox<String> cbPrintDoctor = new ComboBox<>();
        // Sync the print doctor box with the table selection simply implicitly, or just load all doctors
        Runnable updatePrintDoctors = () -> {
            cbPrintDoctor.getItems().clear();
            for (DoctorRow d : doctorsList) cbPrintDoctor.getItems().add(d.getName());
            if (!cbPrintDoctor.getItems().isEmpty()) cbPrintDoctor.getSelectionModel().selectFirst();
        };
        updatePrintDoctors.run();
        docTable.getItems().addListener((javafx.collections.ListChangeListener<DoctorRow>) c -> updatePrintDoctors.run());
        
        docTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) cbPrintDoctor.setValue(val.getName());
        });

        Button btnPrintComm = new Button("PRINT COMMISSION TESTS");
        btnPrintComm.setStyle("-fx-background-color: #961111; -fx-text-fill: white; -fx-font-weight: bold;");
        
        btnPrintComm.setOnAction(e -> {
            String dr = cbPrintDoctor.getValue();
            String period = cbPrintPeriod.getValue();
            if (dr == null) return;
            printCommissionReportOffline(dr, period, doctorsList);
        });

        printControls.getChildren().addAll(new Label("Period:"), cbPrintPeriod, new Label("Doctor:"), cbPrintDoctor, btnPrintComm);
        printBox.getChildren().addAll(lblPrintTitle, printControls);

        rootBox.getChildren().addAll(addBox, docTable, printBox);
        portalDialog.getDialogPane().setContent(rootBox);
        portalDialog.showAndWait();
    }

    private void printCommissionReportOffline(String doctorName, String period, ObservableList<DoctorRow> doctorsList) {
        LocalDate start = LocalDate.now(), end = LocalDate.now();
        if ("Yesterday".equals(period)) { start = end = LocalDate.now().minusDays(1); }
        else if ("Last 7 Days".equals(period)) { start = LocalDate.now().minusDays(7); }
        else if ("This Month".equals(period)) { start = LocalDate.now().withDayOfMonth(1); }

        double commissionRate = 0.0;
        for (DoctorRow d : doctorsList) {
            if (d.getName().equals(doctorName)) { commissionRate = d.getCommission(); break; }
        }

        try {
            String pdfPath = ReportGenerator.generateDoctorCommissionReportThermal(doctorName, start, end, commissionRate, period);
            if (pdfPath != null) {
                java.awt.Desktop.getDesktop().open(new java.io.File(pdfPath));
            } else {
                new Alert(Alert.AlertType.INFORMATION, "No tests found for this doctor in the specified period.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error generating report: " + e.getMessage()).show();
        }
    }
}

package com.lab.lms.controllers;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Test;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RateListController {

    @FXML
    private TableView<Test> rateTable;
    @FXML
    private TableColumn<Test, String> colNumericCode;
    @FXML
    private TableColumn<Test, String> colAlphaCode;
    @FXML
    private TableColumn<Test, String> colTestName;
    @FXML
    private TableColumn<Test, Double> colPrice;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterClassCombo;
    @FXML
    private Label lblCount;

    private ObservableList<Test> masterList = FXCollections.observableArrayList();
    private FilteredList<Test> filteredData;

    @FXML
    public void initialize() {
        // Setup table columns
        colNumericCode.setCellValueFactory(new PropertyValueFactory<>("numericCode"));
        colAlphaCode.setCellValueFactory(new PropertyValueFactory<>("alphaCode"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Setup filter options
        filterClassCombo.setItems(FXCollections.observableArrayList("BOTH PROTOCOLS", "ACTIVE ONLY", "INACTIVE ONLY"));
        filterClassCombo.setValue("BOTH PROTOCOLS");

        // Add custom cell factory for price to format it nicely
        colPrice.setCellFactory(column -> new javafx.scene.control.TableCell<Test, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.2f", price));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1A0A0A;");
                }
            }
        });

        // Search and Filter logic
        filteredData = new FilteredList<>(masterList, p -> true);
        
        javafx.beans.value.ChangeListener<Object> filterListener = (obs, old, val) -> {
            String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String filter = filterClassCombo.getValue();
            
            filteredData.setPredicate(test -> {
                // Class Filter
                if (filter != null) {
                    if ("ACTIVE ONLY".equals(filter) && !"ACTIVE".equalsIgnoreCase(test.getProtocolClass())) return false;
                    if ("INACTIVE ONLY".equals(filter) && !"INACTIVE".equalsIgnoreCase(test.getProtocolClass())) return false;
                }
                
                // Search Filter
                if (query.isEmpty()) return true;
                return test.getName().toLowerCase().contains(query) || 
                       (test.getCategory() != null && test.getCategory().toLowerCase().contains(query));
            });
            updateCountLabel();
        };

        searchField.textProperty().addListener(filterListener);
        filterClassCombo.valueProperty().addListener(filterListener);

        rateTable.setItems(filteredData);
        loadRates();
    }

    private void loadRates() {
        masterList.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            // Fetch only active/all tests depending on lab policy, usually all in the catalog
            String sql = "SELECT * FROM tests ORDER BY name ASC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                masterList.add(new Test(
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
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateCountLabel();
    }

    private void updateCountLabel() {
        lblCount.setText("Total Protocols: " + filteredData.size());
    }

    @FXML
    private void handleRefresh() {
        loadRates();
    }

    @FXML
    private void handlePrint() {
        try {
            java.util.List<Test> testsToPrint = new java.util.ArrayList<>(filteredData);
            if (testsToPrint.isEmpty()) return;
            
            String path = com.lab.lms.services.ReportGenerator.generateRateList(testsToPrint, filterClassCombo.getValue());
            if (path != null) {
                java.awt.Desktop.getDesktop().open(new java.io.File(path));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

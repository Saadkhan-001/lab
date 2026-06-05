package com.lab.lms.services;

import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Test;
import java.sql.*;
import java.util.List;

public class InventoryService {

    /**
     * Deducts inventory items associated with the given list of tests.
     * @param tests The list of tests whose consumables should be deducted.
     * @param conn An active database connection (to participate in a transaction).
     */
    public static void deductTestsInventory(List<Test> tests, Connection conn) {
        String enabled = DatabaseManager.getSetting("enable_inventory_management", "false");
        if (!"true".equalsIgnoreCase(enabled)) return;

        try {
            // Find all inventory items linked to these tests
            String sql = "SELECT inventory_id, usage_quantity FROM test_inventory WHERE test_id = ?";
            String updateSql = "UPDATE inventory SET quantity = quantity - ? WHERE id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 PreparedStatement upstmt = conn.prepareStatement(updateSql)) {
                
                for (Test test : tests) {
                    pstmt.setInt(1, test.getId());
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        int invId = rs.getInt("inventory_id");
                        double usage = rs.getDouble("usage_quantity");
                        
                        upstmt.setDouble(1, usage);
                        upstmt.setInt(2, invId);
                        upstmt.addBatch();
                    }
                }
                upstmt.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Inventory Deduction Error: " + e.getMessage());
            e.printStackTrace();
            // We don't throw here to avoid failing the entire registration/billing, 
            // but in a production lab system, you might want to rollback.
        }
    }
}

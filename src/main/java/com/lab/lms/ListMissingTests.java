package com.lab.lms;

import com.lab.lms.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ListMissingTests {
    public static void main(String[] args) {
        try {
            System.out.println("Checking Clinical Tests Missing Codes (v7.0.2)...");
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            
            String sql = "SELECT name, numeric_code, alpha_code FROM tests WHERE numeric_code IS NULL OR numeric_code = ''";
            ResultSet rs = stmt.executeQuery(sql);
            
            int count = 0;
            while (rs.next()) {
                System.out.println("- Missing: " + rs.getString("name"));
                count++;
            }
            
            System.out.println("\nTotal Tests Missing Codes: " + count);
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

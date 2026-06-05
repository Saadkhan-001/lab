package com.lab.lms;

import java.sql.*;

public class FixSchema {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:C:\\Users\\tohioba\\.lablms\\laboratory.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("Checking 'tests' table for missing columns...");
            
            try {
                stmt.execute("ALTER TABLE tests ADD COLUMN growth_status TEXT DEFAULT 'Positive'");
                System.out.println("Added 'growth_status' to 'tests' table.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    System.out.println("'growth_status' already exists.");
                } else {
                    System.out.println("Error adding 'growth_status': " + e.getMessage());
                }
            }
            
            try {
                stmt.execute("ALTER TABLE tests ADD COLUMN growth_findings TEXT");
                System.out.println("Added 'growth_findings' to 'tests' table.");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column name")) {
                    System.out.println("'growth_findings' already exists.");
                } else {
                    System.out.println("Error adding 'growth_findings': " + e.getMessage());
                }
            }
            
            // Also check patients table for registration_date and title if they are missing
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN registration_date DATETIME DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException e) {}
            
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN age_months INTEGER DEFAULT 0");
            } catch (SQLException e) {}
            
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN age_days INTEGER DEFAULT 0");
            } catch (SQLException e) {}

            System.out.println("Schema check complete.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

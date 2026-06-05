package com.lab.lms;

import com.lab.lms.dao.DatabaseManager;
import java.sql.*;

public class ForceGenerateMissingCodes {
    public static void main(String[] args) {
        try {
            System.out.println("Force Generating Missing Clinical Codes (v7.0.2)...");
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Find the highest existing code
            int nextCode = 901;
            try (Statement s = conn.createStatement(); 
                 ResultSet rs = s.executeQuery("SELECT MAX(CAST(numeric_code AS INTEGER)) FROM tests")) {
                if (rs.next()) {
                    int max = rs.getInt(1);
                    if (max > 0) nextCode = max + 1;
                }
            }
            
            // 2. Select tests missing codes
            String sql = "SELECT id, name FROM tests WHERE numeric_code IS NULL OR numeric_code = ''";
            int updated = 0;
            
            try (Statement s = conn.createStatement(); 
                 ResultSet rs = s.executeQuery(sql);
                 PreparedStatement up = conn.prepareStatement("UPDATE tests SET numeric_code = ?, alpha_code = ? WHERE id = ?")) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    
                    String code = String.valueOf(nextCode++);
                    String alpha = generateAlphaCode(name);
                    
                    up.setString(1, code);
                    up.setString(2, alpha);
                    up.setInt(3, id);
                    up.executeUpdate();
                    
                    System.out.println(" - " + name + " -> CODE: " + code + ", ALPHA: " + alpha);
                    updated++;
                }
            }
            
            conn.commit();
            conn.close();
            System.out.println("\nSuccessfully generated " + updated + " missing clinical codes.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String generateAlphaCode(String name) {
        if (name == null || name.isEmpty()) return "UNK";
        
        // Take first letters of each word
        String[] words = name.split("[\\s\\-\\(\\)]+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        
        String alpha = sb.toString();
        if (alpha.length() > 5) alpha = alpha.substring(0, 5);
        if (alpha.isEmpty()) alpha = "TEST";
        
        return alpha;
    }
}

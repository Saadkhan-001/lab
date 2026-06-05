package com.lab.lms;

import com.lab.lms.dao.DatabaseManager;
import java.io.File;

public class AuditSettings {
    public static void main(String[] args) {
        System.out.println("[AUDIT] Checking Database Settings...");
        try {
            String logoPath = DatabaseManager.getSetting("lab_logo", "NULL");
            System.out.println("[AUDIT] lab_logo: " + logoPath);
            if (!logoPath.equals("NULL") && !logoPath.isEmpty()) {
                File f = new File(logoPath);
                System.out.println("[AUDIT] Logo exists: " + f.exists());
                if (f.exists()) {
                    System.out.println("[AUDIT] Logo size: " + f.length() + " bytes");
                }
            }
            
            String labName = DatabaseManager.getSetting("lab_name", "NULL");
            System.out.println("[AUDIT] lab_name: " + labName);
            
            String reportsDir = System.getProperty("user.home") + File.separator + ".lablms" + File.separator + "reports";
            File rDir = new File(reportsDir);
            System.out.println("[AUDIT] Reports dir: " + reportsDir + " (Exists: " + rDir.exists() + ")");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[AUDIT] Done.");
    }
}

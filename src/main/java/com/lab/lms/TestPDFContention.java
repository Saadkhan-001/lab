package com.lab.lms;

import com.lab.lms.services.ReportGenerator;
import com.lab.lms.dao.DatabaseManager;
import com.lab.lms.models.Test;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TestPDFContention {
    public static void main(String[] args) {
        System.out.println("[TEST] Starting Transactional Contention Test...");
        
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("[TEST] Opening Parent Transaction (AutoCommit=false)...");
            conn.setAutoCommit(false);
            
            List<Test> tests = new ArrayList<>();
            tests.add(new Test(1, "101", "T1", "Glucose", "Category", 500.0, "1 Hour", "Notes", 0, 0, 0, "Blood", "", "ACTIVE", "", "", ""));
            
            System.out.println("[TEST] Calling generateReceipt while transaction is OPEN...");
            String path = ReportGenerator.generateReceipt(
                "TEST PATIENT", "P-100", "30", "Male", 
                "03001234567", "Self", "22-May-2026 10:00 AM", "22-May-2026 11:00 AM",
                tests, 500.0, 0.0, 500.0, 500.0, 0.0
            );
            
            if (path != null) {
                System.out.println("[TEST] SUCCESS! PDF generated at: " + path);
            } else {
                System.out.println("[TEST] FAILED: generateReceipt returned null.");
            }
            
            System.out.println("[TEST] Committing Parent Transaction...");
            conn.commit();
            
        } catch (Throwable t) {
            System.out.println("[TEST] CRASH DETECTED:");
            t.printStackTrace();
        }
        
        System.out.println("[TEST] Finished.");
    }
}

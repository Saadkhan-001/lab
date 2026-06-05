package com.lab.lms;

import com.lab.lms.services.ReportGenerator;
import com.lab.lms.models.Test;
import java.util.ArrayList;
import java.util.List;

public class TestPDF {
    public static void main(String[] args) {
        System.out.println("[TEST] Starting Standalone PDF Generation Test...");
        
        try {
            List<Test> tests = new ArrayList<>();
            tests.add(new Test(1, "101", "T1", "Glucose", "Category", 500.0, "1 Hour", "Notes", 0, 0, 0, "Blood", "", "ACTIVE", "", "", ""));
            
            System.out.println("[TEST] Calling generateReceipt...");
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
            
        } catch (Throwable t) {
            System.out.println("[TEST] CRASH DETECTED:");
            t.printStackTrace();
        }
        
        System.out.println("[TEST] Finished.");
    }
}

package com.lab.lms.services;

import com.lab.lms.dao.DatabaseManager;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TrialService {
    private static final int TRIAL_DAYS = 10;
    private static boolean recoverMode = false;

    public static void setRecover(boolean recover) {
        recoverMode = recover;
    }

    public static boolean isRecover() {
        return recoverMode;
    }

    public static void setDemo(boolean demo) {
        DatabaseManager.saveSetting("is_demo", String.valueOf(demo));
        DatabaseManager.refreshUrl();
    }

    public static boolean isDemo() {
        return "true".equalsIgnoreCase(DatabaseManager.getSetting("is_demo", "false"));
    }

    public static boolean isExpired() {
        if (!isDemo()) return false;
        
        String startDateStr = DatabaseManager.getSetting("trial_start_date", null);
        if (startDateStr == null || startDateStr.equals("null") || startDateStr.isEmpty()) {
            startDateStr = LocalDate.now().toString();
            DatabaseManager.saveSetting("trial_start_date", startDateStr);
            System.out.println("[TRIAL] Baseline set to: " + startDateStr);
            return false;
        }
        
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            long daysPassed = ChronoUnit.DAYS.between(startDate, LocalDate.now());
            boolean expired = daysPassed >= TRIAL_DAYS;
            if (expired) System.err.println("[TRIAL] System EXPIRED. Days passed: " + daysPassed);
            return expired;
        } catch (Exception e) {
            System.err.println("[TRIAL] Date corruption detected. Defaulting to EXPIRED for security.");
            return true;
        }
    }

    public static long getRemainingDays() {
        if (!isDemo()) return -1;
        
        String startDateStr = DatabaseManager.getSetting("trial_start_date", null);
        if (startDateStr == null || startDateStr.equals("null") || startDateStr.isEmpty()) return TRIAL_DAYS;
        
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            long daysPassed = ChronoUnit.DAYS.between(startDate, LocalDate.now());
            return Math.max(0, TRIAL_DAYS - daysPassed);
        } catch (Exception e) {
            return 0;
        }
    }
}

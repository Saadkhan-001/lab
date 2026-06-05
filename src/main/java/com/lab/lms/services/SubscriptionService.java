package com.lab.lms.services;

import com.lab.lms.dao.DatabaseManager;
import java.time.LocalDate;
import java.util.Base64;
import org.json.JSONObject;

public class SubscriptionService {

    public enum Status {
        UNINSTALLED,
        EXPIRED,
        ACTIVE
    }

    public static Status getSystemStatus() {
        if (com.lab.lms.services.TrialService.isDemo()) {
            if (com.lab.lms.services.TrialService.isExpired()) {
                System.out.println("[DIAGNOSTIC] Evaluation Period Expired.");
                return Status.EXPIRED;
            }
            return Status.ACTIVE; // Trial still valid
        }
        
        String isInstalled = DatabaseManager.getSetting("is_installed", "false");
        if (!Boolean.parseBoolean(isInstalled)) {
            return Status.UNINSTALLED;
        }

        String expiryStr = DatabaseManager.getSetting("expiry_date", "");
        if (expiryStr.isEmpty())
            return Status.EXPIRED;

        try {
            LocalDate now = LocalDate.now();
            LocalDate expiry = LocalDate.parse(expiryStr);
            System.out.println("[DIAGNOSTIC] System Clock: " + now);
            System.out.println("[DIAGNOSTIC] License Expiry: " + expiry);

            if (expiry.isBefore(now)) {
                System.out.println("[DIAGNOSTIC] Status: EXPIRED (Locking System)");
                return Status.EXPIRED;
            }
            System.out.println("[DIAGNOSTIC] Status: ACTIVE");
            return Status.ACTIVE;
        } catch (Exception e) {
            System.err.println("[DIAGNOSTIC] Date Parsing Error: " + e.getMessage());
            return Status.EXPIRED;
        }
    }

    public static boolean validateAndRenew(String key) {
        try {
            String cleanKey = key.trim().replaceAll("\\s", "");
            String decoded = new String(Base64.getDecoder().decode(cleanKey));

            String currentSid = DatabaseManager.getSetting("system_id", "");
            String expiry = "";

            if (decoded.startsWith("{")) {
                JSONObject json = new JSONObject(decoded);
                String sid = json.optString("sid", "PENDING");
                expiry = json.optString("expiry", "2099-12-31");

                if (!sid.equals(currentSid) && !sid.equals("PENDING")) {
                    return false; // Hardware mismatch
                }
            } else {
                String[] parts = decoded.split("\\|");
                if (parts.length >= 2) {
                    String sid = parts[0];
                    expiry = parts[1];
                    if (!sid.equals(currentSid) && !sid.equals("PENDING")) {
                        return false; // Hardware mismatch
                    }
                } else {
                    return false; // Invalid format
                }
            }

            // Perform real-time validation check
            LocalDate newExpiry = LocalDate.parse(expiry);
            if (newExpiry.isBefore(LocalDate.now())) {
                System.err.println("[DIAGNOSTIC] Activation Failed: Provided key has already expired on " + expiry);
                return false;
            }

            // Update license in database
            DatabaseManager.saveSetting("license_key", cleanKey);
            DatabaseManager.saveSetting("expiry_date", expiry);

            System.out.println("[DIAGNOSTIC] Activation Successful: System ID " + currentSid + " renewed until " + expiry);
            return true;
        } catch (Exception e) {
            System.err.println("[DIAGNOSTIC] Activation Error: " + e.getMessage());
            return false;
        }
    }

    // Retained for backward compatibility if needed, but updated logic
    public static boolean isSubscriptionActive() {
        return getSystemStatus() == Status.ACTIVE;
    }
}

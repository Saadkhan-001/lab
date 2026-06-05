package com.lab.lms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ClinicalAgeCalculator {

    public static class AgeResult {
        public int years;
        public int months;
        public int days;

        public AgeResult(int y, int m, int d) {
            this.years = y;
            this.months = m;
            this.days = d;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(years).append("y");
            if (months > 0) sb.append(" ").append(months).append("m");
            if (days > 0) sb.append(" ").append(days).append("d");
            return sb.toString();
        }
    }

    /**
     * Calculates the current age of a patient based on the values at registration 
     * and the time elapsed since then.
     */
    public static AgeResult calculateCurrentAge(int regY, int regM, int regD, String regDateStr) {
        if (regDateStr == null || regDateStr.isEmpty()) return new AgeResult(regY, regM, regD);

        try {
            // Laboratory registers often store date or datetime
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime regDateTime;
            try {
                regDateTime = LocalDateTime.parse(regDateStr, dtf);
            } catch (Exception e) {
                // Fallback for simple date format
                regDateTime = LocalDate.parse(regDateStr.split(" ")[0]).atStartOfDay();
            }

            LocalDate regDate = regDateTime.toLocalDate();
            LocalDate today = LocalDate.now();

            long daysSinceReg = ChronoUnit.DAYS.between(regDate, today);
            
            if (daysSinceReg <= 0) return new AgeResult(regY, regM, regD);

            // Add the delta to the registration age
            int totalDays = regD + (int) daysSinceReg;
            int totalMonths = regM + (totalDays / 30);
            int finalDays = totalDays % 30;
            
            int finalYears = regY + (totalMonths / 12);
            int finalMonths = totalMonths % 12;

            return new AgeResult(finalYears, finalMonths, finalDays);
        } catch (Exception e) {
            System.err.println("[AGE] Calculation error for " + regDateStr + ": " + e.getMessage());
            return new AgeResult(regY, regM, regD);
        }
    }
}

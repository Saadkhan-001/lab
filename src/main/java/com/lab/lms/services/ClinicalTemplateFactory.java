package com.lab.lms.services;

import com.lab.lms.dao.DatabaseManager;

public class ClinicalTemplateFactory {

    private static final String TEMPLATE_SETTING = "report_template_path";

    public static ClinicalReportTemplate getTemplate() {
        String templateName = DatabaseManager.getSetting(TEMPLATE_SETTING, "SMART");
        return getTemplate(templateName);
    }
    
    public static ClinicalReportTemplate getTemplate(String name) {
        if (name == null) return new SmartClinicalUltraTemplate();
        
        if (name.toLowerCase().endsWith(".html")) {
            return new HtmlClinicalTemplate(name);
        }

        String nameUpper = name.toUpperCase();

        if (nameUpper.contains("DEFAULT") || nameUpper.contains("BASIC")) {
            return new DefaultClinicalTemplate();
        } else if (nameUpper.contains("FIDELITY") || nameUpper.contains("SPECIAL") || nameUpper.contains("SHIFA")) {
            return new ShifaFidelityTemplate();
        } else if (nameUpper.contains("PRECISION") || nameUpper.contains("MODERN")) {
            return new ModernPrecisionTemplate();
        } else {
            return new SmartClinicalUltraTemplate();
        }
    }
}

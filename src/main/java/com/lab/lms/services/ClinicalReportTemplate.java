package com.lab.lms.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import java.util.List;

public interface ClinicalReportTemplate {
    void apply(Document document, PdfDocument pdf, String patientName,
               String patientId, String age, String gender, String refDr, 
               String collDate, String title, String phone, String address, 
               List<ReportGenerator.TestData> allTestData, boolean isBlank, 
               boolean includeHeader, boolean includeWatermark, String regDate) throws Exception;
}

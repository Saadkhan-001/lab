package com.lab.lms.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.util.List;
import java.io.File;
import java.nio.file.Files;

public class HtmlClinicalTemplate implements ClinicalReportTemplate {

    private String htmlFilePath;

    public HtmlClinicalTemplate(String htmlFilePath) {
        this.htmlFilePath = htmlFilePath;
    }

    @Override
    public void apply(Document document, PdfDocument pdf, String patientName,
               String patientId, String age, String gender, String refDr, 
               String collDate, String title, String phone, String address, 
               List<ReportGenerator.TestData> allTestData, boolean isBlank, 
               boolean includeHeader, boolean includeWatermark, String regDate) throws Exception {
        
        File file = new File(htmlFilePath);
        if (!file.exists()) {
            document.add(new Paragraph("Error: Template HTML file not found at " + htmlFilePath));
            return;
        }

        try {
            float hdrH = Float.parseFloat(com.lab.lms.dao.DatabaseManager.getSetting("header_height", "155"));
            float ftrH = Float.parseFloat(com.lab.lms.dao.DatabaseManager.getSetting("footer_height", "60"));
            float sidePadding = Float.parseFloat(com.lab.lms.dao.DatabaseManager.getSetting("report_page_padding", "1.0")) * 37.8f; // cm to px
            
            // Push content down and up based on header/footer size + Side Padding
            document.setMargins(hdrH + 5, sidePadding, ftrH + 5, sidePadding);

            final String headerPath = com.lab.lms.dao.DatabaseManager.getSetting("lab_header", "");
            final String footerPath = com.lab.lms.dao.DatabaseManager.getSetting("lab_footer", "");
            final boolean showFooter = Boolean.parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("enable_footer", "true"));
            
            final boolean qrEnabled = Boolean.parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("report_qr_enabled", "false"));
            final String qrType = com.lab.lms.dao.DatabaseManager.getSetting("report_qr_type", "patient_details");
            final String qrCustom = com.lab.lms.dao.DatabaseManager.getSetting("report_qr_custom", "");
            final String qrPos = com.lab.lms.dao.DatabaseManager.getSetting("report_qr_position", "bottom_right");
            final String labName = com.lab.lms.dao.DatabaseManager.getSetting("lab_name", "Laboratory");

            if (includeHeader || qrEnabled) {
                pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, new com.itextpdf.kernel.events.IEventHandler() {
                    @Override
                    public void handleEvent(com.itextpdf.kernel.events.Event event) {
                        com.itextpdf.kernel.events.PdfDocumentEvent docEvt = (com.itextpdf.kernel.events.PdfDocumentEvent) event;
                        com.itextpdf.kernel.pdf.PdfPage page = docEvt.getPage();
                        com.itextpdf.kernel.pdf.canvas.PdfCanvas canvas = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(page.newContentStreamBefore(), page.getResources(), docEvt.getDocument());
                        try (com.itextpdf.layout.Canvas headFootCanvas = new com.itextpdf.layout.Canvas(canvas, page.getPageSize())) {
                            
                            if (includeHeader) {
                                if (headerPath != null && !headerPath.isEmpty() && new File(headerPath).exists()) {
                                    try {
                                        com.itextpdf.layout.element.Image img = new com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(headerPath));
                                        img.scaleToFit(page.getPageSize().getWidth() - 40, hdrH);
                                        img.setFixedPosition(20, page.getPageSize().getHeight() - hdrH - 10);
                                        headFootCanvas.add(img);
                                    } catch (Exception e) {}
                                }

                                if (showFooter && footerPath != null && !footerPath.isEmpty() && new File(footerPath).exists()) {
                                    try {
                                        com.itextpdf.layout.element.Image img = new com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(footerPath));
                                        img.scaleToFit(page.getPageSize().getWidth() - 40, ftrH);
                                        img.setFixedPosition(20, 10);
                                        headFootCanvas.add(img);
                                    } catch (Exception e) {}
                                }
                            }
                            
                            if (qrEnabled) {
                                String qrDataStr = "VERIFY";
                                if ("patient_details".equals(qrType)) {
                                    qrDataStr = ReportGenerator.generateQRString(labName, patientName, patientId, age, gender, regDate, allTestData);
                                } else if ("lab_location".equals(qrType) || "map_link".equals(qrType) || "admin_contact".equals(qrType)) {
                                    qrDataStr = (qrCustom != null && !qrCustom.trim().isEmpty()) ? qrCustom : "SCAN ME FOR DETAILS";
                                }
                                
                                try {
                                    com.itextpdf.barcodes.BarcodeQRCode qrCode = new com.itextpdf.barcodes.BarcodeQRCode(qrDataStr);
                                    com.itextpdf.kernel.pdf.xobject.PdfFormXObject qrObject = qrCode.createFormXObject(com.itextpdf.kernel.colors.ColorConstants.BLACK, docEvt.getDocument());
                                    com.itextpdf.layout.element.Image qrImage = new com.itextpdf.layout.element.Image(qrObject);
                                    qrImage.scale(1.3f, 1.3f);
                                    
                                    float x = page.getPageSize().getWidth() - 65;
                                    float y = 12;

                                    if ("top_right".equals(qrPos)) {
                                        y = page.getPageSize().getHeight() - 65;
                                    } else if ("top_left".equals(qrPos)) {
                                        x = 20;
                                        y = page.getPageSize().getHeight() - 65;
                                    } else if ("bottom_left".equals(qrPos)) {
                                        x = 20;
                                        y = 12;
                                    }
                                    
                                    qrImage.setFixedPosition(x, y);
                                    headFootCanvas.add(qrImage);
                                } catch (Exception e) {}
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String htmlContent = new String(Files.readAllBytes(file.toPath()));
        String baseFontSize = com.lab.lms.dao.DatabaseManager.getSetting("report_font_size", "11");
        htmlContent = "<style>body, table, p, div { font-size: " + baseFontSize + "px !important; }</style>" + htmlContent;

        // Perform text replacements for template engine
        htmlContent = htmlContent.replace("{{PATIENT_NAME}}", patientName != null ? patientName : "");
        htmlContent = htmlContent.replace("{{PATIENT_ID}}", patientId != null ? patientId : "");
        htmlContent = htmlContent.replace("{{AGE}}", age != null ? age : "");
        htmlContent = htmlContent.replace("{{GENDER}}", gender != null ? gender : "");
        htmlContent = htmlContent.replace("{{REF_DR}}", refDr != null ? refDr : "");
        htmlContent = htmlContent.replace("{{DATE}}", collDate != null ? collDate : "");
        htmlContent = htmlContent.replace("{{REG_DATE}}", regDate != null ? regDate : "");
        htmlContent = htmlContent.replace("{{REPORT_TITLE}}", title != null ? title : "");
        htmlContent = htmlContent.replace("{{PHONE}}", phone != null ? phone : "");
        htmlContent = htmlContent.replace("{{ADDRESS}}", address != null ? address : "");
        htmlContent = htmlContent.replace("{{REPORT_DATE}}", new java.text.SimpleDateFormat("hh:mm a dd MMM, yyyy").format(new java.util.Date()));
        
        StringBuilder testTableHtml = new StringBuilder();
        if (allTestData != null && !allTestData.isEmpty() && !isBlank) {
            testTableHtml.append("<table><tr><th>Test Parameter</th><th>Result</th><th>Unit</th><th>Reference Ranges</th></tr>");
            for (ReportGenerator.TestData data : allTestData) {
                if (data.results != null) {
                    for (java.util.Map<String, String> rowMap : data.results) {
                        String rName = rowMap.get("name") != null ? rowMap.get("name") : "-";
                        String rVal = rowMap.get("value") != null ? rowMap.get("value") : "";
                        String rUnit = rowMap.get("unit") != null ? rowMap.get("unit") : "";
                        String rRange = rowMap.get("range") != null ? rowMap.get("range") : "";

                        // In current LMS logic, sometimes a category or header is indicated by lacking value/unit/range
                        boolean isCat = rVal.trim().isEmpty() && rUnit.trim().isEmpty() && rRange.trim().isEmpty();

                        if (isCat && !rName.trim().isEmpty() && !rName.equals("-")) {
                            testTableHtml.append("<tr><td colspan=\"4\"><b><u>").append(rName).append("</u></b></td></tr>");
                        } else {
                            testTableHtml.append("<tr>");
                            testTableHtml.append("<td>").append(rName).append("</td>");
                            testTableHtml.append("<td><b>").append(rVal).append("</b></td>");
                            testTableHtml.append("<td>").append(rUnit).append("</td>");
                            testTableHtml.append("<td>").append(rRange).append("</td>");
                            testTableHtml.append("</tr>");
                        }
                    }
                }
            }
            testTableHtml.append("</table>");
        }
        
        htmlContent = htmlContent.replace("{{TEST_TABLE}}", testTableHtml.toString());

        String pathologist = com.lab.lms.dao.DatabaseManager.getSetting("pathologist_details", "");
        htmlContent = htmlContent.replace("{{PATHOLOGIST_DETAILS}}", pathologist);
        
        String notes = com.lab.lms.dao.DatabaseManager.getSetting("report_notes", "");
        htmlContent = htmlContent.replace("{{REPORT_NOTES}}", notes);

        String footer = com.lab.lms.dao.DatabaseManager.getSetting("doctors_footer", "");
        htmlContent = htmlContent.replace("{{DOCTORS_FOOTER}}", footer);

        boolean enableVerify = Boolean.parseBoolean(com.lab.lms.dao.DatabaseManager.getSetting("enable_electronic_verify", "false"));
        if (enableVerify) {
            htmlContent = htmlContent.replace("{{ELECTRONIC_VERIFY_NOTE}}", "<p style=\"text-align: center; font-size: 9px; font-weight: bold; margin-top: 5px;\">*** THIS IS AN ELECTRONICALLY VERIFIED REPORT. NO PHYSICAL SIGNATURE IS REQUIRED. ***</p>");
        } else {
            htmlContent = htmlContent.replace("{{ELECTRONIC_VERIFY_NOTE}}", "");
        }

        // Delegate HTML rendering to the existing robust clinical parser in ReportGenerator
        ReportGenerator.renderClinicalNotes(document, htmlContent);
    }
}

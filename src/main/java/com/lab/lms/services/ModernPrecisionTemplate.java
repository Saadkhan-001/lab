package com.lab.lms.services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import com.lab.lms.dao.DatabaseManager;
import java.io.File;
import java.util.*;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.lab.lms.services.SessionContext;
import java.text.SimpleDateFormat;

public class ModernPrecisionTemplate implements ClinicalReportTemplate {

    @Override
    public void apply(Document document, PdfDocument pdf, String patientName,
                      String patientId, String age, String gender, String refDr, 
                      String collDate, String title, String phone, String address, 
                      java.util.List<ReportGenerator.TestData> allTestData, boolean isBlank, 
                      boolean includeHeader, boolean includeWatermark, String regDate) throws Exception {

        // 1. Branding & Settings
        String labName = DatabaseManager.getSetting("lab_name", "MODERN CLINICAL LABORATORY");
        String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
        String labContact = DatabaseManager.getSetting("lab_contact", "");
        String logoPath = DatabaseManager.getSetting("lab_logo", "");
        String headerPath = DatabaseManager.getSetting("lab_header", "");
        String labEmail = DatabaseManager.getSetting("lab_email", "");
        String labWebsite = DatabaseManager.getSetting("lab_website", "");
        String labTagline = DatabaseManager.getSetting("lab_tagline", "");
        String watermarkPath = DatabaseManager.getSetting("report_watermark", "");
        String pathologistsFooter = ReportGenerator.getReportPathologist();
        String footerPath = DatabaseManager.getSetting("lab_footer", "");
        String doctorsFooter = DatabaseManager.getSetting("report_doctors_footer", "");
        
        // Synchronized Header/Footer Settings from Default Engine
        float headerHeight = Float.parseFloat(DatabaseManager.getSetting("header_height", "150"));
        float footerHeightSetting = Float.parseFloat(DatabaseManager.getSetting("footer_height", "150"));
        float totalHeaderZone = headerHeight + 130;
        document.setMargins(totalHeaderZone + 2, 36, Math.max(160, footerHeightSetting + 15), 36);

        // Sanitation
        patientName = (patientName == null || patientName.isEmpty()) ? "Anonymous" : patientName.trim().toUpperCase();
        patientId = (patientId == null || patientId.isEmpty()) ? "-" : patientId.trim();
        age = (age == null) ? "0" : age.trim();

        // Clinical Data for QR
        String outFormat = "dd-MMM-yyyy hh:mm:ss a";
        java.text.SimpleDateFormat outSdf = new java.text.SimpleDateFormat(outFormat);
        String regD = "-";
        if (regDate != null && !regDate.isEmpty()) {
            try {
                java.text.SimpleDateFormat pSdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                regD = outSdf.format(pSdf.parse(regDate)).toUpperCase();
            } catch (Exception e) { regD = regDate; }
        }
        
        String qrData = ReportGenerator.generateQRString(labName, patientName, patientId, age, gender, regD, allTestData);

        // 4. Clinical Payload Evaluation (Auto-Scale Logic)
        int totalRows = 0;
        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                if (td.results != null) totalRows += td.results.size();
                totalRows += 5; 
            }
        }
        
        float scale = totalRows > 40 ? 0.70f : (totalRows > 32 ? 0.78f : (totalRows > 24 ? 0.85f : (totalRows > 16 ? 0.92f : 1.0f)));
        float dynPadding = totalRows > 24 ? 2.0f : 4.0f;
        float dynMargin  = totalRows > 24 ? 5.0f : 12.0f;

        // Add Header/Footer Event Handler (Same as Default Settings)
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new ModernHeaderFooterHandler(
            labName, labAddress, labContact, labEmail, labWebsite, labTagline, logoPath, headerPath, footerPath, watermarkPath, title, patientName, patientId, age, gender,
            refDr, phone, address, regD, collDate, pathologistsFooter, doctorsFooter, includeHeader, includeWatermark,
            scale, qrData, headerHeight, allTestData
        ));

        // 8. Test Results Rendering
        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                java.util.List<Map<String, String>> rows = td.results;
                if (!isBlank && td.results != null) {
                    java.util.List<Map<String, String>> filteredRows = new java.util.ArrayList<>();
                    for (Map<String, String> row : td.results) {
                        String val = row.get("value");
                        if (val == null || val.trim().isEmpty() || val.equals("-") || val.equalsIgnoreCase("NILL")) continue;
                        filteredRows.add(row);
                    }
                    rows = filteredRows;
                }

                if (td.isCulture == 1) {
                    ReportGenerator.renderCultureSection(document, td, 9 * scale);
                    document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginBottom(dynMargin).setFontColor(ColorConstants.LIGHT_GRAY));
                    continue;
                }

                if (rows == null || rows.isEmpty()) continue;

                // Refined Header: [DEPT] (Large Initial/Highlighted) | Center: [TEST NAME]
                String dept = (td.category != null && !td.category.equalsIgnoreCase("ACTIVE")) ? td.category.toUpperCase() : "CLINICAL";
                if (dept.isEmpty()) dept = "SERVICE";

                String fL = dept.substring(0, 1);
                String rest = dept.substring(1);

                Table headTab = new Table(UnitValue.createPercentArray(new float[]{30, 40, 30})).useAllAvailableWidth().setMarginBottom(dynMargin).setMarginTop(0);
                
                // Dept Cell (Badge with large initial)
                Paragraph pBadge = new Paragraph();
                pBadge.add(new Text(" " + fL).setBold().setFontSize(14 * scale));
                pBadge.add(new Text(rest + " ").setBold().setFontSize(10 * scale));
                pBadge.setBackgroundColor(new DeviceRgb(45, 62, 80)).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE).setPadding(2);
                
                headTab.addCell(new Cell().add(pBadge).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
                
                // Test Name Cell (Centered on Page) - Added null safety
                String safeTestName = (td.testName != null) ? td.testName.toUpperCase() : "TEST";
                headTab.addCell(new Cell().add(new Paragraph(safeTestName)
                        .setBold().setFontSize(11 * scale).setFontColor(new DeviceRgb(45, 62, 80))
                        .setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
                
                headTab.addCell(new Cell().setBorder(Border.NO_BORDER));
                
                document.add(headTab);
                
                if (td.isMicroscopic == 1) {
                    Map<String, java.util.List<Map<String, String>>> grouped = new LinkedHashMap<>();
                    for (Map<String, String> row : rows) {
                        String name = row.getOrDefault("name", "").toLowerCase();
                        String cat = row.getOrDefault("category", "").trim();
                        
                        // Robust fallback for legacy data or missing categories
                        if (cat.isEmpty() || cat.equals("-") || cat.equalsIgnoreCase("Examination")) {
                            String unitFallback = row.getOrDefault("unit", "").trim();
                            if (unitFallback.isEmpty() || unitFallback.equals("-") || unitFallback.equalsIgnoreCase("Examination")) {
                                if (name.contains("color") || name.contains("appearance") || name.contains("gravity") || name.contains("physical")) cat = "Physical Examination";
                                else if (name.contains("glucose") || name.contains("sugar") || name.contains("protein") || name.contains("albumin") || 
                                         name.contains("ketone") || name.contains("bilirubin") || name.contains("urobilinogen") || name.contains("nitrite") || 
                                         name.contains("blood") || name.contains("leucocyte") || name.contains("chemical") || name.contains("ph")) cat = "Chemical Examination";
                                else if (name.contains("wbc") || name.contains("rbc") || name.contains("pus") || name.contains("epithelial") || 
                                         name.contains("cast") || name.contains("urate") || name.contains("mucus") || name.contains("microscopic")) cat = "Microscopic Examination";
                                else if (name.contains("crystal")) cat = "Crystal";
                                else cat = "General Examination";
                            } else {
                                if (unitFallback.contains("Examination") || unitFallback.contains("Physical") || unitFallback.contains("Chemical") || unitFallback.contains("Microscopic")) {
                                    cat = unitFallback;
                                } else {
                                    cat = "General Examination";
                                }
                            }
                        }
                        grouped.computeIfAbsent(cat, k -> new java.util.ArrayList<>()).add(row);
                    }
                    
                    for (Map.Entry<String, java.util.List<Map<String, String>>> entry : grouped.entrySet()) {
                        Table resTable = new Table(UnitValue.createPercentArray(new float[] { 45, 25, 30 })).useAllAvailableWidth();
                        
                        // Section Header
                        resTable.addHeaderCell(new Cell(1, 3).add(new Paragraph(entry.getKey().toUpperCase())
                                .setBold().setFontSize(8.5f * scale).setFontColor(new DeviceRgb(26, 35, 126)))
                                .setBorder(Border.NO_BORDER).setPaddingBottom(5).setPaddingTop(10));

                        for (Map<String, String> row : entry.getValue()) {
                            String mName = row.get("name");
                            resTable.addCell(new Cell().add(new Paragraph(mName == null ? "" : mName))
                                    .setFontSize(11 * scale).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
                            
                            String vs = row.get("value");
                            Paragraph vp = new Paragraph(vs == null ? "NILL" : vs).setBold().setFontSize(12 * scale);
                            if ("ABNORMAL".equals(row.get("status"))) vp.setFontColor(new DeviceRgb(200, 0, 0));
                            resTable.addCell(new Cell().add(vp).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setTextAlignment(TextAlignment.CENTER));
                            
                            String rng = row.get("range");
                            resTable.addCell(new Cell().add(new Paragraph(rng == null ? "-" : rng)).setFontSize(10 * scale).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setTextAlignment(TextAlignment.CENTER));
                        }
                        document.add(resTable.setMarginBottom(dynMargin));
                    }
                } else {
                    java.util.List<String> histHeaders = td.historyHeaders;
                    int histCount = (histHeaders != null) ? histHeaders.size() : 0;
                    float[] tWidths;
                    if (histCount == 0) {
                        tWidths = new float[] { 35, 20, 15, 30 };
                    } else {
                        tWidths = new float[3 + 1 + histCount];
                        tWidths[0] = 25; 
                        float resColW = 50f / (1 + histCount);
                        for (int i = 1; i <= (1 + histCount); i++) tWidths[i] = resColW;
                        tWidths[tWidths.length - 2] = 10; 
                        tWidths[tWidths.length - 1] = 15;
                    }

                    Table t = new Table(UnitValue.createPercentArray(tWidths)).useAllAvailableWidth().setMarginBottom(dynMargin);
                    
                    // Styled Header Row
                    Cell hTest = new Cell().add(new Paragraph("INVESTIGATION").setBold().setFontSize(9 * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setPadding(dynPadding);
                    t.addHeaderCell(hTest);
                    
                    if (histCount > 0) {
                        t.addHeaderCell(new Cell().add(new Paragraph("CURRENT").setBold().setFontSize(8 * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
                        for (String hh : histHeaders) {
                            t.addHeaderCell(new Cell().add(new Paragraph(hh.replace("\n", " ")).setBold().setFontSize(6.5f * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
                        }
                    } else {
                        t.addHeaderCell(new Cell().add(new Paragraph("RESULT").setBold().setFontSize(11 * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
                    }
                    t.addHeaderCell(new Cell().add(new Paragraph("UNIT").setBold().setFontSize(11 * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
                    t.addHeaderCell(new Cell().add(new Paragraph("NORMAL RANGE").setBold().setFontSize(11 * scale).setFontColor(ColorConstants.WHITE)).setBackgroundColor(new DeviceRgb(45, 62, 80)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                    for (Map<String, String> r : rows) {
                        String name = r.get("name");
                        t.addCell(new Cell().add(new Paragraph(name == null ? "-" : name)).setFontSize(11.5f * scale).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
                        
                        String val = r.get("value");
                        Paragraph vp = new Paragraph(val == null ? "-" : val).setBold().setFontSize(12.5f * scale).setTextAlignment(TextAlignment.CENTER);
                        if ("ABNORMAL".equals(r.get("status"))) vp.setFontColor(new DeviceRgb(180, 0, 0));
                        t.addCell(new Cell().add(vp).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setBackgroundColor(new DeviceRgb(252, 252, 252)));
                        
                        if (histCount > 0) {
                            for (int i = 0; i < histCount; i++) {
                                String hVal = r.get("history_" + i);
                                Paragraph pHVal = new Paragraph(hVal == null ? "-" : hVal).setFontSize(10.5f * scale).setTextAlignment(TextAlignment.CENTER);
                                if ("ABNORMAL".equals(r.get("history_status_" + i))) pHVal.setFontColor(new DeviceRgb(180, 0, 0)).setBold();
                                t.addCell(new Cell().add(pHVal).setPadding(dynPadding).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
                            }
                        }
                        
                        String unit = r.get("unit");
                        t.addCell(new Cell().add(new Paragraph(unit == null ? "-" : unit)).setFontSize(11 * scale).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
                        String range = r.get("range");
                        t.addCell(new Cell().add(new Paragraph(range == null ? "-" : range)).setFontSize(10.5f * scale).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
                    }
                    document.add(t);
                }

                if (td.resultComment != null && !td.resultComment.trim().isEmpty()) {
                    document.add(new Paragraph("Laboratory Findings:").setBold().setFontSize(8 * scale).setFontColor(new DeviceRgb(45, 62, 80)));
                    ReportGenerator.renderClinicalNotes(document, td.resultComment.trim());
                }

                if (td.notes != null && !td.notes.trim().isEmpty()) {
                    document.add(new Paragraph("CLINICAL NOTES:").setBold().setFontSize(8 * scale).setFontColor(new DeviceRgb(45, 62, 80)));
                    ReportGenerator.renderClinicalNotes(document, td.notes.trim());
                }
                if (td != allTestData.get(allTestData.size() - 1)) {
                    document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.DashedLine(0.5f)).setMarginBottom(dynMargin));
                }
                ReportGenerator.addClinicalImages(document, td);
            }
        }

    }

    protected class ModernHeaderFooterHandler implements IEventHandler {
        private String labName, labAddress, labContact, labEmail, labWebsite, labTagline, logoPath, headerPath, footerPath, watermarkPath, title, patientName, patientId, age, gender, refDr, phone, address, regD, collDate;
        private String pathologistsFooter, doctorsFooter, qrData;
        private boolean includeHeader, includeWatermark;
        private float scale, headerHeight;
        private java.util.List<ReportGenerator.TestData> allTestData;

        public ModernHeaderFooterHandler(String labName, String labAddress, String labContact, String labEmail, String labWebsite, String labTagline, String logoPath, String headerPath, String footerPath, String watermarkPath, String title, String patientName, String patientId, String age, String gender,
                                      String refDr, String phone, String address, String regD, String collDate, String pathologistsFooter, String doctorsFooter, boolean includeHeader, boolean includeWatermark,
                                      float scale, String qrData, float headerHeight, java.util.List<ReportGenerator.TestData> allTestData) {
            this.labName = labName; this.labAddress = labAddress; this.labContact = labContact; this.labEmail = labEmail; this.labWebsite = labWebsite; this.labTagline = labTagline; this.logoPath = logoPath; this.headerPath = headerPath; this.footerPath = footerPath; this.watermarkPath = watermarkPath; this.title = title; this.patientName = patientName; this.patientId = patientId; this.age = age; this.gender = gender;
            this.refDr = refDr; this.phone = phone; this.address = address; this.regD = regD; this.collDate = collDate; this.pathologistsFooter = pathologistsFooter; this.doctorsFooter = doctorsFooter; this.includeHeader = includeHeader; this.includeWatermark = includeWatermark;
            this.scale = scale; this.qrData = qrData; this.headerHeight = headerHeight; this.allTestData = allTestData;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            float pageW = pageSize.getWidth();
            float pageH = pageSize.getHeight();
            int pageNum = pdf.getPageNumber(page);

            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);

            // Watermark Logic
            if (includeWatermark && watermarkPath != null && !watermarkPath.trim().isEmpty() && new File(watermarkPath).isFile()) {
                try {
                    pdfCanvas.saveState();
                    pdfCanvas.setExtGState(new com.itextpdf.kernel.pdf.extgstate.PdfExtGState().setFillOpacity(0.08f));
                    pdfCanvas.addImageFittedIntoRectangle(ImageDataFactory.create(watermarkPath), new Rectangle((pageW - 400) / 2, (pageH - 400) / 2, 400, 400), false);
                    pdfCanvas.restoreState();
                } catch (Exception ignored) {}
            }

            float totalHdrZone = headerHeight + 130;
            Rectangle headerRect = new Rectangle(36, pageH - totalHdrZone - 10, pageW - 72, totalHdrZone);
            try (Canvas canvas = new Canvas(pdfCanvas, headerRect)) {
                if (includeHeader && headerPath != null && !headerPath.trim().isEmpty() && new File(headerPath).isFile() && !headerPath.toLowerCase().contains("no header")) {
                    try {
                        canvas.add(new Image(ImageDataFactory.create(headerPath)).setWidth(UnitValue.createPercentValue(100)).setHeight(headerHeight));
                    } catch (Exception e) { renderDefaultHeader(canvas, pdf); }
                } else if (includeHeader) {
                    renderDefaultHeader(canvas, pdf);
                } else {
                    canvas.add(new Paragraph("\u00a0").setHeight(headerHeight).setMargin(0).setPadding(0).setMultipliedLeading(0));
                }
                
                // Patient Grid Redesign: 5-Column Layout with Big QR Code
                Table patGrid = new Table(UnitValue.createPercentArray(new float[] { 13, 27, 16, 27, 17 })).useAllAvailableWidth().setFontSize(9 * scale);
                patGrid.setBorder(new SolidBorder(new DeviceRgb(224, 224, 224), 0.5f));

                StringBuilder ts = new StringBuilder();
                if (allTestData != null) {
                    for (int i=0; i<allTestData.size(); i++) {
                        String tn = allTestData.get(i).testName;
                        if (tn != null) ts.append(tn.toUpperCase());
                        else ts.append("-");
                        if (i < allTestData.size() - 1) ts.append(", ");
                    }
                }

                // Row 1: MR NO | TEST REQUESTED | QR
                patGrid.addCell(labelCell("MR NO", scale).setPaddingLeft(5));
                patGrid.addCell(valueCell(": " + patientId, scale));
                patGrid.addCell(labelCell("TEST REQUESTED", scale));
                patGrid.addCell(valueCell(": " + ts.toString(), scale));
                
                // Add QR Code at the end of the first row with rowspan
                try {
                    com.itextpdf.barcodes.BarcodeQRCode pQr = new com.itextpdf.barcodes.BarcodeQRCode(qrData != null ? qrData : "LIS");
                    Image qrImg = new Image(pQr.createFormXObject(pdf)).setWidth(UnitValue.createPercentValue(100));
                    patGrid.addCell(new Cell(5, 1).add(qrImg).setBorder(Border.NO_BORDER).setPadding(5).setVerticalAlignment(VerticalAlignment.MIDDLE));
                } catch(Exception ignored) {
                    patGrid.addCell(new Cell(5, 1).setBorder(Border.NO_BORDER));
                }

                // Row 2: NAME | REFERRED BY
                patGrid.addCell(labelCell("NAME", scale).setPaddingLeft(5));
                patGrid.addCell(valueCell(": " + (patientName == null ? "ANONYMOUS" : patientName.toUpperCase()), scale));
                patGrid.addCell(labelCell("REFERRED BY", scale));
                patGrid.addCell(valueCell(": " + (refDr == null || refDr.isEmpty() ? "SELF" : refDr.toUpperCase()), scale));

                // Row 3: AGE/SEX | REQUESTED AT
                patGrid.addCell(labelCell("AGE/SEX", scale).setPaddingLeft(5));
                patGrid.addCell(valueCell(": " + age + "Y / " + gender, scale));
                patGrid.addCell(labelCell("REQUESTED AT", scale));
                patGrid.addCell(valueCell(": " + regD, scale));

                // Row 4: PHONE | SPECIMEN AT
                patGrid.addCell(labelCell("PHONE", scale).setPaddingLeft(5));
                patGrid.addCell(valueCell(": " + (phone == null || phone.isEmpty() ? "-" : phone), scale));
                patGrid.addCell(labelCell("SPECIMEN AT", scale));
                patGrid.addCell(valueCell(": " + ReportGenerator.getClinicalOffsetTime(regD, 2), scale));

                // Row 5: ADDRESS | REPORTED BY
                String pathName = (pathologistsFooter != null && !pathologistsFooter.isBlank()) ? pathologistsFooter.split("\n")[0].trim() : "ADMIN";
                patGrid.addCell(labelCell("ADDRESS", scale).setPaddingLeft(5));
                patGrid.addCell(valueCell(": " + (address == null || address.isEmpty() ? "-" : address.toUpperCase()), scale));
                patGrid.addCell(labelCell("REPORTED BY", scale));
                patGrid.addCell(valueCell(": " + pathName, scale));

                canvas.add(patGrid.setMarginTop(5));

                // pBar (Pagination Bar) Removed to save space
            }

            // Footer Section
            if (includeHeader) {
                float fH = Float.parseFloat(DatabaseManager.getSetting("footer_height", "150"));
                Rectangle footerRect = new Rectangle(36, 25, pageW - 72, fH);
                try (Canvas canvas = new Canvas(pdfCanvas, footerRect)) {
                    if (footerPath != null && !footerPath.trim().isEmpty() && new File(footerPath).isFile()) {
                        try { canvas.add(new Image(ImageDataFactory.create(footerPath)).setWidth(UnitValue.createPercentValue(100)).setHeight(fH)); } 
                        catch (Exception e) { renderDefaultFooter(canvas, pdf, pageNum); }
                    } else { renderDefaultFooter(canvas, pdf, pageNum); }
                }

                // Medico-Legal Disclaimer
                pdfCanvas.saveState();
                pdfCanvas.setFillColor(new DeviceRgb(45, 62, 80));
                pdfCanvas.rectangle(0, 0, pageW, 20);
                pdfCanvas.fill();
                pdfCanvas.restoreState();

                try (Canvas canvas = new Canvas(pdfCanvas, new Rectangle(0, 0, pageW, 20))) {
                    canvas.add(new Paragraph("PROVISIONAL REPORT - NOT VALID FOR MEDICO LEGAL PURPOSE")
                            .setFontColor(ColorConstants.WHITE).setBold().setFontSize(8 * scale).setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
                }
            }
        }

        private void renderDefaultHeader(Canvas canvas, PdfDocument pdf) {
            Table h = new Table(UnitValue.createPercentArray(new float[] { 20, 60, 20 })).useAllAvailableWidth();
            
            Cell left = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            try { 
                com.itextpdf.barcodes.BarcodeQRCode qrCode = new com.itextpdf.barcodes.BarcodeQRCode(qrData != null && !qrData.isEmpty() ? qrData : "LIS");
                left.add(new Image(qrCode.createFormXObject(pdf)).setWidth(65 * scale)); 
            } catch(Exception ignored) {}
            h.addCell(left);

            Cell center = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            center.add(new Paragraph(labName).setBold().setFontSize(18).setMarginBottom(0));
            if (labTagline != null) center.add(new Paragraph(labTagline.toUpperCase()).setFontSize(8).setMarginTop(0));
            center.add(new Paragraph(labAddress).setFontSize(8.5f));
            h.addCell(center);

            Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
            if (logoPath != null && new File(logoPath).isFile()) {
                try { right.add(new Image(ImageDataFactory.create(logoPath)).setWidth(65 * scale)); } catch(Exception ignored) {}
            }
            h.addCell(right);

            canvas.add(h.setMarginBottom(10));
        }

        private void renderDefaultFooter(Canvas canvas, PdfDocument pdf, int pageNum) {
            String loginUser = SessionContext.getUsername();
            if (loginUser == null) loginUser = "SYSTEM";

            Table sigs = new Table(2).useAllAvailableWidth().setMarginTop(10);
            sigs.addCell(new Cell().add(new Paragraph(loginUser.toUpperCase()).setBold().setFontSize(9 * scale)).add(new Paragraph("PREPARED BY").setFontSize(7 * scale)).setBorder(Border.NO_BORDER));
            
            String pathRef = (pathologistsFooter != null && !pathologistsFooter.isEmpty()) ? pathologistsFooter : "AUTHORIZED SIGNATORY";
            String[] pLines = pathRef.split("\n");
            Cell pCell = new Cell().setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER);
            for (int i=0; i<pLines.length; i++) {
                Paragraph p = new Paragraph(pLines[i].trim()).setFontSize(i==0 ? 9*scale : 7.5f*scale);
                if (i==0) p.setBold();
                pCell.add(p);
            }
            sigs.addCell(pCell);
            canvas.add(sigs);

            Table vBar = new Table(1).useAllAvailableWidth().setBackgroundColor(new DeviceRgb(240, 240, 240)).setMarginTop(5);
            vBar.addCell(new Cell().add(new Paragraph("Electronically verified report, no signature(s) required.").setBold().setFontSize(8 * scale).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER).setPadding(3));
            canvas.add(vBar);

            if (doctorsFooter != null && !doctorsFooter.isEmpty()) {
                String[] drs = doctorsFooter.split("\\|\\|");
                float[] ws = new float[Math.min(drs.length, 5)]; java.util.Arrays.fill(ws, 100f/ws.length);
                Table drGrid = new Table(UnitValue.createPercentArray(ws)).useAllAvailableWidth().setMarginTop(5);
                for(int i=0; i<ws.length; i++) {
                    String[] lines = drs[i].trim().split("\n");
                    Cell c = new Cell().setBorder(Border.NO_BORDER);
                    for(int j=0; j<lines.length; j++) {
                        Paragraph p = new Paragraph(lines[j].trim()).setFontSize(j==0 ? 7.5f*scale : 6*scale);
                        if(j==0) p.setBold();
                        c.add(p);
                    }
                    drGrid.addCell(c);
                }
                canvas.add(drGrid);
            }
        }

        private Cell labelCell(String text, float scale) {
            return new Cell().add(new Paragraph(text).setBold().setFontSize(8.5f * scale)).setBorder(Border.NO_BORDER).setPadding(2);
        }

        private Cell valueCell(String text, float scale) {
            return new Cell().add(new Paragraph(text)).setBorder(Border.NO_BORDER).setPadding(2);
        }

        private void addGridRow(Table table, String label1, String value1, String label2, String value2, float scale) {
            table.addCell(labelCell(label1, scale).setPaddingLeft(10));
            table.addCell(valueCell(value1, scale));
            table.addCell(labelCell(label2, scale).setPaddingLeft(10));
            table.addCell(valueCell(value2, scale));
        }
    }
}

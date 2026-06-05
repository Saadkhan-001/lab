package com.lab.lms.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.lab.lms.dao.DatabaseManager;
import java.io.File;
import java.util.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.Rectangle;

public class ShifaFidelityTemplate implements ClinicalReportTemplate {

    @Override
    public void apply(Document document, PdfDocument pdf, String patientName,
                      String patientId, String age, String gender, String refDr,
                      String collDate, String title, String phone, String address,
                      java.util.List<ReportGenerator.TestData> allTestData, boolean isBlank,
                      boolean includeHeader, boolean includeWatermark, String regDate) throws Exception {

        String labName = DatabaseManager.getSetting("lab_name", "Shifa International Hospitals Ltd.");
        String labAddress = DatabaseManager.getSetting("lab_address", "");
        String labContact = DatabaseManager.getSetting("lab_contact", "");
        String labEmail = DatabaseManager.getSetting("lab_email", "");
        String labWebsite = DatabaseManager.getSetting("lab_website", "");
        String labTagline = DatabaseManager.getSetting("lab_tagline", "");
        String footerPath = DatabaseManager.getSetting("lab_footer", "");
        String doctorsFooter = DatabaseManager.getSetting("report_doctors_footer", "");

        // Default doctors for Shifa design (4 columns)
        if (doctorsFooter == null || doctorsFooter.trim().isEmpty()) {
            doctorsFooter = "DR. MUHAMMAD USMAN\nMBBS, FCPS\nExt: 4283||DR. SANIA RAZA\nMBBS, FCPS\nExt: 4285||DR. MADEEHA FATIMA\nMBBS, FCPS\nExt: 4287||DR. SALMAN RIAZ\nMBBS, FCPS\nExt: 4289";
        }

        patientName = (patientName == null || patientName.equalsIgnoreCase("null")) ? "" : patientName.trim();
        patientId = (patientId == null || patientId.equalsIgnoreCase("null")) ? "" : patientId.trim();
        age = (age == null || age.equalsIgnoreCase("null")) ? "0" : age.trim();
        gender = (gender == null || gender.equalsIgnoreCase("null") || gender.isEmpty()) ? "-" : gender.trim();
        refDr = (refDr == null || refDr.equalsIgnoreCase("null") || refDr.isEmpty()) ? "" : refDr.trim();

        String datePattern = "dd-MMM-yyyy HH:mm:ss";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(datePattern);
        String viewTime = sdf.format(new java.util.Date());

        float hdrH = Float.parseFloat(DatabaseManager.getSetting("header_height", "130"));
        float totalHeaderZone = hdrH + 160;
        document.setMargins(totalHeaderZone + 20, 48, 160, 48);

        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new ShifaHeaderFooterHandler(
            labName, labAddress, labContact, labEmail, labWebsite, labTagline, footerPath, patientName, patientId, age, gender,
            refDr, viewTime, regDate, doctorsFooter, includeHeader
        ));

        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                // Section: Final Report Title
                String reportSub = (title != null && !title.isEmpty()) ? title : "Consolidated Diagnostic";
                document.add(new Paragraph(reportSub + " [ Final Report ]")
                    .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

                // Table headers for Shifa grid style
                Table resTable = new Table(UnitValue.createPercentArray(new float[] { 40, 25, 20, 15 })).useAllAvailableWidth();
                resTable.addHeaderCell(sCell("TEST(s)", TextAlignment.LEFT));
                resTable.addHeaderCell(sCell("RESULT(s)", TextAlignment.CENTER));
                resTable.addHeaderCell(sCell("NORMAL RANGE", TextAlignment.CENTER));
                resTable.addHeaderCell(sCell("UNITS", TextAlignment.CENTER));

                for (Map<String, String> row : td.results) {
                    resTable.addCell(new Cell().add(new Paragraph(row.getOrDefault("name", ""))).setFontSize(10).setPadding(4).setBorder(new SolidBorder(0.3f)));
                    
                    String val = row.getOrDefault("value", "-");
                    Paragraph pVal = new Paragraph(val).setBold().setFontSize(10.5f);
                    if ("ABNORMAL".equals(row.get("status"))) pVal.setFontColor(ColorConstants.RED);
                    resTable.addCell(new Cell().add(pVal).setTextAlignment(TextAlignment.CENTER).setPadding(4).setBorder(new SolidBorder(0.3f)));
                    
                    resTable.addCell(new Cell().add(new Paragraph(row.getOrDefault("range", "-"))).setFontSize(9).setTextAlignment(TextAlignment.CENTER).setPadding(4).setBorder(new SolidBorder(0.3f)));
                    resTable.addCell(new Cell().add(new Paragraph(row.getOrDefault("unit", ""))).setFontSize(9).setTextAlignment(TextAlignment.CENTER).setPadding(4).setBorder(new SolidBorder(0.3f)));
                }
                document.add(resTable.setMarginBottom(20));

                if (td.notes != null && !td.notes.trim().isEmpty()) {
                    document.add(new Paragraph("TECHNICAL NOTES").setBold().setFontSize(10).setUnderline().setMarginBottom(5));
                    ReportGenerator.renderClinicalNotes(document, td.notes.trim());
                }
                ReportGenerator.addClinicalImages(document, td);
            }
        }
    }

    private Cell sCell(String txt, TextAlignment align) {
        return new Cell().add(new Paragraph(txt).setBold().setFontSize(10.5f))
            .setBackgroundColor(new DeviceRgb(240, 240, 240))
            .setTextAlignment(align).setPadding(4).setBorder(new SolidBorder(0.5f));
    }

    protected class ShifaHeaderFooterHandler implements IEventHandler {
        private String labName, labAddress, labContact, labEmail, labWebsite, labTagline, footerPath, patientName, patientId, age, gender, refDr, viewTime, regDate, doctorsFooter;
        private boolean includeHeader;

        public ShifaHeaderFooterHandler(String labName, String labAddress, String labContact, String labEmail, String labWebsite, String labTagline, String footerPath, String patientName, String patientId, String age, String gender,
                                      String refDr, String viewTime, String regDate, String doctorsFooter, boolean includeHeader) {
            this.labName = labName; this.labAddress = labAddress; this.labContact = labContact; this.labEmail = labEmail; this.labWebsite = labWebsite; this.labTagline = labTagline; this.footerPath = footerPath; this.patientName = patientName; this.patientId = patientId; this.age = age; this.gender = gender;
            this.refDr = refDr; this.viewTime = viewTime; this.regDate = regDate; 
            this.doctorsFooter = doctorsFooter; this.includeHeader = includeHeader;
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

            // Shifa Vertical Signature Stamp (Right Margin)
            pdfCanvas.saveState();
            try {
                pdfCanvas.beginText().setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(), 7)
                    .setColor(ColorConstants.GRAY, true)
                    .setTextMatrix(0, 1, -1, 0, pageW - 15, pageH / 2 - 50)
                    .showText("This is a Computer GENERATED Report. It DOES NOT require any SIGNATURE or STAMP.")
                    .endText();
            } catch (Exception e) {
                // Fallback or ignore
            }
            pdfCanvas.restoreState();

            // Header Section — dynamic height based on header_height setting
            float totalHdrZone = Float.parseFloat(DatabaseManager.getSetting("header_height", "130")) + 160;
            Rectangle headerRect = new Rectangle(36, pageH - totalHdrZone - 10, pageW - 72, totalHdrZone);
            try (Canvas canvas = new Canvas(pdfCanvas, headerRect)) {
                if (includeHeader) {
                    Table hTab = new Table(UnitValue.createPercentArray(new float[] { 20, 60, 20 })).useAllAvailableWidth();
                    hTab.addCell(new Cell().add(new Paragraph("ISO 9001\nCert No. 2555").setFontSize(7.5f)).setBorder(Border.NO_BORDER));
                    
                    Cell mid = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
                    mid.add(new Paragraph(labName).setBold().setFontSize(22).setMarginBottom(0));
                    if (labTagline != null && !labTagline.trim().isEmpty()) {
                        mid.add(new Paragraph(labTagline.toUpperCase()).setFontSize(8.5f).setItalic().setMarginBottom(1));
                    }
                    if (labAddress != null && !labAddress.trim().isEmpty()) {
                        mid.add(new Paragraph(labAddress).setFontSize(9));
                    } else {
                        mid.add(new Paragraph("Islamic Republic of Pakistan").setFontSize(9).setItalic());
                    }
                    
                    // Contact Meta Bar (Email & Web)
                    StringBuilder meta = new StringBuilder();
                    if (labEmail != null && !labEmail.trim().isEmpty()) meta.append(labEmail.toLowerCase());
                    if (labWebsite != null && !labWebsite.trim().isEmpty()) {
                        if (meta.length() > 0) meta.append(" | ");
                        meta.append(labWebsite.toLowerCase());
                    }
                    if (meta.length() > 0) {
                        mid.add(new Paragraph(meta.toString()).setFontSize(7).setMarginTop(0));
                    }
                    hTab.addCell(mid);
                    
                    hTab.addCell(new Cell().add(new Paragraph("PATHOLOGY").setBold().setFontSize(14)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                    canvas.add(hTab.setMarginBottom(10));
                } else {
                    // Leave blank space at the top for pre-printed letterhead header
                    float spacerH = Float.parseFloat(DatabaseManager.getSetting("header_height", "130"));
                    canvas.add(new Paragraph("\u00a0").setHeight(spacerH)
                            .setMargin(0).setPadding(0).setMultipliedLeading(0));
                }

                // Patient Info Grid with Labels and Dots
                Table patGrid = new Table(UnitValue.createPercentArray(new float[] { 5, 15, 30, 20, 30 })).useAllAvailableWidth().setFontSize(9.5f);
                
                // Vertical "OUT-PATIENT" indicator
                Cell outCell = new Cell(4, 1).add(new Paragraph("O\nU\nT\n-\nP\nA\nT\nI\nE\nN\nT").setBold().setFontSize(7).setMarginTop(5)).setBorder(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.CENTER);
                patGrid.addCell(outCell);

                addShifaRow(patGrid, "MR No", ": " + patientId, "Ordered By", ": " + (refDr.isEmpty() ? "SELF" : refDr.toUpperCase()));
                addShifaRow(patGrid, "Patient", ": " + patientName.toUpperCase(), "Ordered On", ": " + regDate);
                addShifaRow(patGrid, "Age/Gender", ": " + age + " Y / " + gender, "Specimen No", ": " + (patientId.length() > 4 ? "S" + patientId.substring(4) : "S0091"));
                addShifaRow(patGrid, "Contact", ": " + labContact, "Sample Type", ": BLOOD");

                canvas.add(patGrid.setMarginBottom(10).setBorder(new SolidBorder(0.5f)));

                // Verified Bar (Dark Gray)
                Table vBar = new Table(1).useAllAvailableWidth().setBackgroundColor(new DeviceRgb(60, 60, 60));
                vBar.addCell(new Cell().add(new Paragraph("Verified On: " + viewTime).setFontColor(ColorConstants.WHITE).setBold().setFontSize(9.5f).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER).setPadding(3));
                canvas.add(vBar.setMarginBottom(10));
            }

            // Footer — only render content when includeHeader is true
            // When !includeHeader, leave blank space for pre-printed letterhead paper
            if (includeHeader) {
                float fH = Float.parseFloat(DatabaseManager.getSetting("footer_height", "120"));
                Rectangle footerRect = new Rectangle(36, 10, pageW - 72, fH);
                try (Canvas canvas = new Canvas(pdfCanvas, footerRect)) {
                    if (footerPath != null && !footerPath.trim().isEmpty() && new File(footerPath).isFile()) {
                        try {
                            Image customFooter = new Image(ImageDataFactory.create(footerPath))
                                    .setWidth(UnitValue.createPercentValue(100)).setHeight(fH);
                            canvas.add(customFooter);
                        } catch (Exception e) {
                            renderDefaultFooter(canvas, pageNum);
                        }
                    } else {
                        renderDefaultFooter(canvas, pageNum);
                    }
                }
            }
        }

        private void renderDefaultFooter(Canvas canvas, int pageNum) {
            if (doctorsFooter != null && !doctorsFooter.isEmpty()) {
                String[] df = doctorsFooter.split("\\|\\|");
                Table dG = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 })).useAllAvailableWidth();
                for (String doc : df) {
                    String[] lines = doc.trim().split("\n");
                    Cell c = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT);
                    for (int j=0; j<lines.length; j++) {
                        Paragraph lp = new Paragraph(lines[j].trim()).setFontSize(6.5f);
                        if (j == 0) lp.setBold().setFontSize(7.5f);
                        c.add(lp);
                    }
                    dG.addCell(c);
                }
                canvas.add(dG.setMarginTop(10));
            }

            Paragraph pInfo = new Paragraph("Printed On: " + viewTime + " | Verified By: System | Page: " + pageNum)
                .setFontSize(7).setTextAlignment(TextAlignment.CENTER).setMarginTop(10);
            canvas.add(pInfo);
        }

        private void addShifaRow(Table t, String labelL, String valL, String labelR, String valR) {
            t.addCell(new Cell().add(new Paragraph(labelL + ".......")).setBorder(Border.NO_BORDER).setPaddingLeft(5));
            t.addCell(new Cell().add(new Paragraph(valL).setBold()).setBorder(Border.NO_BORDER));
            t.addCell(new Cell().add(new Paragraph(labelR + ".......")).setBorder(Border.NO_BORDER).setPaddingLeft(10));
            t.addCell(new Cell().add(new Paragraph(valR).setBold()).setBorder(Border.NO_BORDER));
        }
    }
}

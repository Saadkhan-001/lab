package com.lab.lms.services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.barcodes.Barcode128;
import com.lab.lms.dao.DatabaseManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.Rectangle;

/**
 * SMART CLINICAL ULTRA TEMPLATE (v6.0 — Drlogy Reference-Perfect)
 *
 * Pixel-perfect recreation of the Drlogy Pathology Lab Blood Group Report:
 *  1. Dark navy (#0F4C75) header — Logo+Name+Address left | Contact right
 *  2. Diagonal stripe ornament band
 *  3. Thin dark-navy strip with right-aligned website
 *  4. 3-col patient card: Info | Barcode+Dates | QR
 *  5. Gray horizontal separator
 *  6. Centered bold report title
 *  7. Results table: navy header row (white text) + alternating light-blue rows
 *     Columns: INVESTIGATION | RESULT | UNIT | REFERENCE VALUE
 *     Abnormal values in bold red
 *  8. Notes/Comment sections (blue label)
 *  9. Footer: Prepared By (left) | Pathologist (right)
 *     Electronically verified gray bar + Doctors grid
 * 10. "NOT VALID FOR MEDICO LEGAL/COURT PURPOSE" navy bottom banner
 */
public class SmartClinicalUltraTemplate implements ClinicalReportTemplate {

    // ── Drlogy Palette ──────────────────────────────────────────────────────
    private static final DeviceRgb NAVY          = new DeviceRgb(15,  76, 117);    // Primary header navy
    private static final DeviceRgb NAVY_DARK     = new DeviceRgb(10,  50,  80);    // Secondary strip / table header
    private static final DeviceRgb STRIPE_BLUE   = new DeviceRgb(52, 152, 219);    // Diagonal stripe bright blue
    private static final DeviceRgb ROW_ALT       = new DeviceRgb(235, 245, 255);   // Alternating table row
    private static final DeviceRgb TBL_HEADER_BG = new DeviceRgb(13,  60,  97);    // Results table header row bg
    private static final DeviceRgb RED_ABNORMAL  = new DeviceRgb(192,  0,   0);    // Abnormal result text
    private static final DeviceRgb GRAY_BORDER   = new DeviceRgb(200, 200, 200);
    private static final DeviceRgb GRAY_TEXT     = new DeviceRgb(90,  90,  90);
    private static final DeviceRgb TEXT_BLACK    = new DeviceRgb(30,  30,  30);
    private static final DeviceRgb NOTE_BLUE     = new DeviceRgb(26,  82, 118);

    @Override
    public void apply(Document document, PdfDocument pdf,
                      String patientName, String patientId,
                      String age,         String gender,
                      String refDr,       String collDate,
                      String title,       String phone,
                      String address,
                      java.util.List<ReportGenerator.TestData> allTestData,
                      boolean isBlank,    boolean includeHeader,
                      boolean includeWatermark, String regDate) throws Exception {

        // ── Settings ────────────────────────────────────────────────────────
        String labName  = DatabaseManager.getSetting("lab_name",    "DRLOGY PATHOLOGY LAB");
        String labAddr  = DatabaseManager.getSetting("lab_address",  "105-108, Smart Vision Complex, Healthcare Road, Mumbai - 689578");
        String labPhone = DatabaseManager.getSetting("lab_contact",  "");
        String labWeb   = DatabaseManager.getSetting("lab_website",  "");
        String labEmail = DatabaseManager.getSetting("lab_email",    "");
        String tagline  = DatabaseManager.getSetting("lab_tagline",  "Accurate | Caring | Instant");
        String logoPath = DatabaseManager.getSetting("lab_logo",     "");
        String hdrPath  = DatabaseManager.getSetting("lab_header",  "");
        String ftrPath  = DatabaseManager.getSetting("lab_footer",  "");
        String wmPath   = DatabaseManager.getSetting("report_watermark", "");
        String pathFtr  = ReportGenerator.getReportPathologist();
        String docFtr   = DatabaseManager.getSetting("report_doctors_footer", "");
        String noteFtr  = DatabaseManager.getSetting("report_note_footer",    "");
        boolean footerOn = Boolean.parseBoolean(DatabaseManager.getSetting("enable_footer", "true"));
        float hdrH = Float.parseFloat(DatabaseManager.getSetting("header_height", "130"));
        float ftrH = Float.parseFloat(DatabaseManager.getSetting("footer_height", "80"));

        String regD        = (regDate  == null || regDate.isEmpty())  ? new SimpleDateFormat("hh:mm a dd MMM, yy").format(new Date()) : regDate;
        String collDisplay = (collDate == null || collDate.isEmpty()) ? new SimpleDateFormat("hh:mm a dd MMM, yy").format(new Date()) : collDate;
        String repDisplay  = new SimpleDateFormat("hh:mm a dd MMM, yy").format(new Date());

        // ── Auto-scale for report density ───────────────────────────────────
        int totalRows = 0;
        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                if (td.results != null) totalRows += td.results.size();
                totalRows += 4;
            }
        }
        float scale      = totalRows > 24 ? 0.80f : (totalRows > 18 ? 0.90f : 1.0f);
        float dynPadding = totalRows > 24 ? 1.5f  : (totalRows > 18 ? 2.5f  : 3.5f);
        float dynMargin  = totalRows > 24 ? 2.0f  : (totalRows > 18 ? 5.0f  : 8.0f);

        // ── Watermark fallback ──────────────────────────────────────────────
        String wmFinal = (wmPath != null && !wmPath.isBlank() && new File(wmPath).isFile()) ? wmPath : logoPath;
        String qrData  = ReportGenerator.generateQRString(labName, patientName, patientId, age, gender, regD, allTestData);

        // Dynamic top margin: header zone + patient card (~110px) + padding
        float topMargin = hdrH + 110;
        document.setMargins(topMargin, 20, 220, 20);

        // Register page event handler (draws header + footer on every page)
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new DrlogyHeaderFooterHandler(
                labName, labAddr, labPhone, labWeb, labEmail, tagline, logoPath, hdrPath, ftrPath, wmFinal,
                patientName, patientId, age, gender, refDr, qrData, regD, collDisplay, repDisplay,
                pathFtr, docFtr, noteFtr, includeHeader, footerOn, includeWatermark, scale, hdrH, ftrH
        ));

        // ── Report Title ────────────────────────────────────────────────────
        String reportTitle = (title != null && !title.isBlank()) ? title.toUpperCase() : "DIAGNOSTIC REPORT";
        document.add(new Paragraph(reportTitle)
                .setBold().setFontSize(11 * scale)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(TEXT_BLACK)
                .setMarginTop(4 * scale)
                .setMarginBottom(dynMargin));

        // ── Test Result Sections ────────────────────────────────────────────
        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                buildTestSection(document, td, isBlank, scale, dynPadding, dynMargin);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAGE EVENT HANDLER — Header + Footer
    // ═══════════════════════════════════════════════════════════════════════════
    protected class DrlogyHeaderFooterHandler implements IEventHandler {
        private final String labName, labAddr, labPhone, labWeb, labEmail, tagline;
        private final String logoPath, hdrPath, ftrPath, wmFinal, patientName, patientId, age, gender, refDr;
        private final String qrData, regD, collD, repD, pathFtr, docFtr, noteFtr;
        private final boolean includeHeader, footerOn, includeWatermark;
        private final float scale, hdrH, ftrH;

        public DrlogyHeaderFooterHandler(
                String labName, String labAddr, String labPhone,
                String labWeb, String labEmail, String tagline, String logoPath, String hdrPath, String ftrPath, String wmFinal,
                String patientName, String patientId, String age, String gender, String refDr,
                String qrData, String regD, String collD, String repD,
                String pathFtr, String docFtr, String noteFtr,
                boolean includeHeader, boolean footerOn, boolean includeWatermark,
                float scale, float hdrH, float ftrH) {
            this.labName = labName; this.labAddr = labAddr; this.labPhone = labPhone;
            this.labWeb = labWeb; this.labEmail = labEmail; this.tagline = tagline;
            this.logoPath = logoPath; this.hdrPath = hdrPath; this.ftrPath = ftrPath; this.wmFinal = wmFinal;
            this.patientName = patientName; this.patientId = patientId;
            this.age = age; this.gender = gender; this.refDr = refDr;
            this.qrData = qrData; this.regD = regD; this.collD = collD; this.repD = repD;
            this.pathFtr = pathFtr; this.docFtr = docFtr; this.noteFtr = noteFtr;
            this.includeHeader = includeHeader; this.footerOn = footerOn;
            this.includeWatermark = includeWatermark;
            this.scale = scale; this.hdrH = hdrH; this.ftrH = ftrH;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf   = docEvent.getDocument();
            PdfPage     page  = docEvent.getPage();
            Rectangle   ps    = page.getPageSize();
            float       pW    = ps.getWidth();
            float       pH    = ps.getHeight();
            int         pgNum = pdf.getPageNumber(page);

            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);

            // ── 1. Watermark ──────────────────────────────────────────────────
            if (includeWatermark && wmFinal != null && !wmFinal.isBlank()) {
                try {
                    pdfCanvas.saveState();
                    com.itextpdf.kernel.pdf.extgstate.PdfExtGState gs = new com.itextpdf.kernel.pdf.extgstate.PdfExtGState().setFillOpacity(0.08f);
                    pdfCanvas.setExtGState(gs);
                    try (Canvas c = new Canvas(pdfCanvas, ps)) {
                        Image wmImg = new Image(ImageDataFactory.create(wmFinal)).setWidth(380);
                        wmImg.setFixedPosition((pW - 380) / 2f, (pH - 380) / 2f);
                        c.add(wmImg);
                    }
                    pdfCanvas.restoreState();
                } catch (Exception ignored) {}
            }

            // ── 2. Unified Header & Patient Identity Block ────────────────────
            if (includeHeader) {
                boolean useCustom = hdrPath != null && !hdrPath.isBlank() && new File(hdrPath).isFile();
                
                if (useCustom) {
                    try {
                        Rectangle hr = new Rectangle(0, pH - hdrH, pW, hdrH);
                        try (Canvas hc = new Canvas(pdfCanvas, hr)) {
                            hc.add(new Image(ImageDataFactory.create(hdrPath)).setWidth(pW).setHeight(hdrH));
                        }
                    } catch (Exception ignored) {}
                } else {
                    float navyH = 90 * scale;
                    float stripeH = 12 * scale;
                    float stripH = 18 * scale;
                    float cardH = 90 * scale;
                    
                    // ── BACKGROUNDS ─────
                    // Main Navy
                    pdfCanvas.saveState().setFillColor(NAVY).rectangle(0, pH - navyH, pW, navyH).fill().restoreState();
                    // Stripe Band
                    float sY = pH - navyH - stripeH;
                    pdfCanvas.saveState().setFillColor(new DeviceRgb(240, 248, 255)).rectangle(0, sY, pW, stripeH).fill().restoreState();
                    // Draw diagonal stripes
                    pdfCanvas.saveState().setFillColor(STRIPE_BLUE);
                    float sww = 16 * scale, gap = 12 * scale;
                    for (float x = -sww; x < pW + sww; x += (sww + gap)) {
                        pdfCanvas.moveTo(x, sY).lineTo(x + sww, sY).lineTo(x + sww - 6, sY + stripeH).lineTo(x - 6, sY + stripeH).closePath().fill();
                    }
                    pdfCanvas.restoreState();
                    // Thin Strip
                    float stY = sY - stripH;
                    pdfCanvas.saveState().setFillColor(NAVY_DARK).rectangle(0, stY, pW, stripH).fill().restoreState();

                    // ── CONTENT ─────
                    // Header Content
                    try (Canvas c = new Canvas(pdfCanvas, new Rectangle(0, pH - navyH, pW, navyH))) {
                        Table ht = new Table(UnitValue.createPercentArray(new float[]{60, 40})).setWidth(pW).setHeight(navyH);
                        Cell lc = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE).setPaddingLeft(20);
                        Table li = new Table(UnitValue.createPercentArray(new float[]{15, 85})).useAllAvailableWidth();
                        if (logoPath != null && !logoPath.isBlank() && new File(logoPath).isFile()) {
                            try { li.addCell(new Cell().add(new Image(ImageDataFactory.create(logoPath)).setWidth(45 * scale)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)); }
                            catch (Exception ignored) { li.addCell(new Cell().setBorder(Border.NO_BORDER)); }
                        } else { li.addCell(new Cell().setBorder(Border.NO_BORDER)); }
                        Cell ncc = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
                        ncc.add(new Paragraph(labName.toUpperCase()).setBold().setFontSize(22 * scale).setFontColor(ColorConstants.WHITE).setMarginBottom(1));
                        ncc.add(new Paragraph(tagline).setBold().setFontSize(10.5f * scale).setFontColor(new DeviceRgb(200, 225, 250)).setMarginBottom(2));
                        ncc.add(new Paragraph(labAddr).setFontSize(8.5f * scale).setFontColor(new DeviceRgb(180, 210, 240)));
                        li.addCell(ncc);
                        lc.add(li);
                        ht.addCell(lc);
                        Cell rc = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE).setPaddingRight(20);
                        rc.add(new Paragraph("\u260E  " + labPhone).setBold().setFontSize(10.5f * scale).setFontColor(ColorConstants.WHITE).setMarginBottom(3));
                        
                        StringBuilder webMail = new StringBuilder();
                        if (labEmail != null && !labEmail.isEmpty()) webMail.append(labEmail.toLowerCase());
                        if (labWeb != null && !labWeb.isEmpty()) {
                            if (webMail.length() > 0) webMail.append(" | ");
                            webMail.append(labWeb.toLowerCase());
                        }
                        if (webMail.length() > 0) {
                            rc.add(new Paragraph(webMail.toString()).setFontSize(9.5f * scale).setFontColor(new DeviceRgb(200, 225, 250)));
                        }
                        ht.addCell(rc);
                        c.add(ht);
                    }
                    
                    // Patient Card
                    float cardY = stY - cardH - 5;
                    try (Canvas cc = new Canvas(pdfCanvas, new Rectangle(0, cardY, pW, cardH))) {
                        Table patT = new Table(UnitValue.createPercentArray(new float[]{42, 42, 16})).setWidth(pW - 40).setMarginLeft(20);
                        patT.setBorderTop(new SolidBorder(GRAY_BORDER, 0.6f)).setBorderBottom(new SolidBorder(GRAY_BORDER, 0.6f)).setPadding(4);
                        
                        // C1: Info
                        Cell c1 = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
                        c1.add(new Paragraph("Patient: " + (patientName == null ? "N/A" : patientName.toUpperCase())).setBold().setFontSize(9 * scale));
                        c1.add(new Paragraph("Age/Sex: " + age + " / " + gender).setFontSize(8 * scale));
                        c1.add(new Paragraph("ID: " + patientId).setFontSize(8 * scale));
                        c1.add(new Paragraph("Ref Dr: " + (refDr == null ? "Self" : refDr)).setFontSize(8 * scale));
                        patT.addCell(c1);
                        
                        // C2: Barcode + Dates
                        Cell c2 = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
                        try {
                            Barcode128 bc = new Barcode128(pdf); bc.setCode(patientId == null ? "0000" : patientId);
                            c2.add(new Image(bc.createFormXObject(pdf)).setWidth(85 * scale).setHeight(15 * scale).setMarginBottom(4));
                        } catch (Exception ignored) {}
                        c2.add(new Paragraph("Registered: " + regD).setFontSize(7.5f * scale));
                        c2.add(new Paragraph("Collected: " + collD).setFontSize(7.5f * scale));
                        c2.add(new Paragraph("Reported: " + repD).setFontSize(7.5f * scale));
                        patT.addCell(c2);
                        
                        // C3: QR
                        Cell c3 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
                        try {
                           BarcodeQRCode qr = new BarcodeQRCode(qrData == null ? "VERIFY" : qrData);
                           c3.add(new Image(qr.createFormXObject(pdf)).setWidth(48 * scale));
                        } catch (Exception ignored) {}
                        patT.addCell(c3);
                        cc.add(patT);
                    }
                }
            } else {
                // When !includeHeader: leave blank space at top for pre-printed header,
                // but still render the patient card below it
                float cardH = 90 * scale;
                float cardY = pH - hdrH - cardH - 5;
                try (Canvas cc = new Canvas(pdfCanvas, new Rectangle(0, cardY, pW, cardH))) {
                    Table patT = new Table(UnitValue.createPercentArray(new float[]{42, 42, 16})).setWidth(pW - 40).setMarginLeft(20);
                    patT.setBorderTop(new SolidBorder(GRAY_BORDER, 0.6f)).setBorderBottom(new SolidBorder(GRAY_BORDER, 0.6f)).setPadding(4);
                    
                    // C1: Info
                    Cell c1 = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
                    c1.add(new Paragraph("Patient: " + (patientName == null ? "N/A" : patientName.toUpperCase())).setBold().setFontSize(9 * scale));
                    c1.add(new Paragraph("Age/Sex: " + age + " / " + gender).setFontSize(8 * scale));
                    c1.add(new Paragraph("ID: " + patientId).setFontSize(8 * scale));
                    c1.add(new Paragraph("Ref Dr: " + (refDr == null ? "Self" : refDr)).setFontSize(8 * scale));
                    patT.addCell(c1);
                    
                    // C2: Barcode + Dates
                    Cell c2 = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
                    try {
                        Barcode128 bc = new Barcode128(pdf); bc.setCode(patientId == null ? "0000" : patientId);
                        c2.add(new Image(bc.createFormXObject(pdf)).setWidth(85 * scale).setHeight(15 * scale).setMarginBottom(4));
                    } catch (Exception ignored) {}
                    c2.add(new Paragraph("Registered: " + regD).setFontSize(7.5f * scale));
                    c2.add(new Paragraph("Collected: " + collD).setFontSize(7.5f * scale));
                    c2.add(new Paragraph("Reported: " + repD).setFontSize(7.5f * scale));
                    patT.addCell(c2);
                    
                    // C3: QR
                    Cell c3 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
                    try {
                       BarcodeQRCode qr = new BarcodeQRCode(qrData == null ? "VERIFY" : qrData);
                       c3.add(new Image(qr.createFormXObject(pdf)).setWidth(48 * scale));
                    } catch (Exception ignored) {}
                    patT.addCell(c3);
                    cc.add(patT);
                }
            }

            // ── 3. Footer Zone ──────────────────────────────────────────────
            if (includeHeader) {
                // Render full footer content only when header/footer is enabled
                boolean useCustomFooter = ftrPath != null && !ftrPath.isBlank() && new File(ftrPath).isFile();
                
                if (useCustomFooter) {
                    try {
                        Rectangle fr = new Rectangle(0, 0, pW, ftrH);
                        try (Canvas fc = new Canvas(pdfCanvas, fr)) {
                            fc.add(new Image(ImageDataFactory.create(ftrPath)).setWidth(pW).setHeight(ftrH));
                        }
                    } catch (Exception ignored) {}
                } else {
                    Rectangle fRect = new Rectangle(0, 0, pW, 215 * scale);
                    try (Canvas canvas = new Canvas(pdfCanvas, fRect)) {
                        canvas.add(buildSignatureRow(pgNum, pW));
                        if (footerOn) {
                            buildFooterBanner(canvas, pdfCanvas, pW);
                        }
                    }
                }
            }
            // When !includeHeader, footer zone is left blank (margins already reserve the space)
        }

        // ── Signature Row ────────────────────────────────────────────────────
        private Table buildSignatureRow(int pageNum, float pW) {
            Table wrap = new Table(1).setWidth(pW - 40).setMarginLeft(20).setMarginRight(20).setMarginTop(0);

            // Clinical notes footer
            if (noteFtr != null && !noteFtr.isBlank()) {
                wrap.addCell(new Cell()
                        .add(new Paragraph("Notes:").setBold().setFontSize(8 * scale).setFontColor(NOTE_BLUE).setMarginBottom(1 * scale))
                        .add(new Paragraph(noteFtr).setFontSize(7.5f * scale).setFontColor(GRAY_TEXT).setMarginBottom(4 * scale))
                        .setBorder(Border.NO_BORDER));
            }

            // Prepared By (left) | Verified By / Pathologist (right)
            Table sigRow = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginBottom(4 * scale);

            String loginUser = SessionContext.getUsername();
            if (loginUser == null || loginUser.trim().isEmpty()) loginUser = "System Administrator";

            Cell lSig = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT);
            lSig.add(new Paragraph(" ").setMarginBottom(8 * scale));
            lSig.add(new Paragraph(loginUser).setBold().setFontSize(9 * scale).setFontColor(TEXT_BLACK).setMarginBottom(0));
            lSig.add(new Paragraph("Prepared By").setFontSize(7.5f * scale).setFontColor(GRAY_TEXT));
            sigRow.addCell(lSig);

            String pathName = (pathFtr != null && !pathFtr.isBlank()) ? pathFtr.split("\n")[0].trim() : "Chief Pathologist";
            String pathQual = (pathFtr != null && pathFtr.contains("\n")) ? pathFtr.substring(pathFtr.indexOf("\n")).trim() : "MBBS, MD (Pathology)";

            Cell rSig = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            rSig.add(new Paragraph(" ").setMarginBottom(8 * scale));
            rSig.add(new Paragraph(pathName).setBold().setFontSize(9 * scale).setFontColor(TEXT_BLACK).setMarginBottom(0));
            rSig.add(new Paragraph(pathQual).setFontSize(7.5f * scale).setFontColor(GRAY_TEXT));
            sigRow.addCell(rSig);

            wrap.addCell(new Cell().add(sigRow).setBorder(Border.NO_BORDER).setPadding(0));

            // Electronically verified bar
            Table evBar = new Table(1).useAllAvailableWidth().setBackgroundColor(new DeviceRgb(232, 232, 232)).setMarginBottom(2 * scale);
            evBar.addCell(new Cell()
                    .add(new Paragraph("** Electronically verified report, no physical signature required. **")
                            .setBold().setFontSize(7.5f * scale).setFontColor(TEXT_BLACK).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(new SolidBorder(GRAY_BORDER, 0.5f)).setPadding(4 * scale));
            wrap.addCell(new Cell().add(evBar).setBorder(Border.NO_BORDER).setPadding(0));

            // Doctors signature grid
            if (docFtr != null && !docFtr.trim().isEmpty()) {
                String[] docs = docFtr.split("\\|\\|");
                java.util.List<String[]> sigs = new ArrayList<>();
                for (String d : docs) {
                    if (!d.trim().isEmpty()) sigs.add(d.trim().split("\n"));
                }
                if (!sigs.isEmpty()) {
                    int nCols = Math.min(sigs.size(), 5);
                    float[] ws = new float[nCols];
                    Arrays.fill(ws, 100f / nCols);
                    Table dG = new Table(UnitValue.createPercentArray(ws)).useAllAvailableWidth()
                            .setBackgroundColor(new DeviceRgb(252, 252, 252)).setMarginTop(0);
                    for (int i = 0; i < nCols; i++) {
                        Cell c = new Cell().setBorder(new SolidBorder(GRAY_BORDER, 0.5f))
                                .setPadding(4 * scale).setPaddingLeft(8 * scale)
                                .setTextAlignment(TextAlignment.LEFT);
                        String[] lines = sigs.get(i);
                        for (int j = 0; j < lines.length; j++) {
                            Paragraph p = new Paragraph(lines[j].trim());
                            if (j == 0) p.setBold().setFontSize(7.5f * scale).setFontColor(TEXT_BLACK);
                            else        p.setFontSize(6.5f * scale).setFontColor(GRAY_TEXT).setMarginTop(0).setMarginBottom(0);
                            c.add(p);
                        }
                        dG.addCell(c);
                    }
                    wrap.addCell(new Cell().add(dG).setBorder(Border.NO_BORDER).setPadding(0));
                }
            }

            // Page number
            wrap.addCell(new Cell()
                    .add(new Paragraph("Page " + pageNum).setFontSize(7 * scale).setFontColor(GRAY_TEXT).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(Border.NO_BORDER).setPaddingTop(4 * scale).setPaddingBottom(32 * scale));

            return wrap;
        }

        // ── Footer Banner: "NOT VALID FOR MEDICO LEGAL…" ────────────────────
        private void buildFooterBanner(Canvas canvas, PdfCanvas pdfCanvas, float pW) {
            // Hint text above banner
            Table hn = new Table(1).setWidth(pW - 40).setMarginLeft(20).setMarginTop(2 * scale);
            hn.addCell(new Cell()
                    .add(new Paragraph("Maintain a healthy lifestyle by checking your health regularly.")
                            .setFontSize(6 * scale).setFontColor(GRAY_TEXT).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(Border.NO_BORDER));
            canvas.add(hn);

            // Navy bottom banner
            float bannerH = 24 * scale;
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(NAVY_DARK);
            pdfCanvas.rectangle(0, 0, pW, bannerH);
            pdfCanvas.fill();
            pdfCanvas.restoreState();

            Rectangle bannerRect = new Rectangle(0, 0, pW, bannerH);
            try (Canvas bc = new Canvas(pdfCanvas, bannerRect)) {
                bc.add(new Paragraph("NOT VALID FOR MEDICO LEGAL / COURT PURPOSE")
                        .setBold().setFontSize(9 * scale).setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setMarginTop(5 * scale));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TEST SECTION BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    private void buildTestSection(Document doc, ReportGenerator.TestData td,
                                   boolean isBlank, float scale,
                                   float dynPadding, float dynMargin) {
        if (td == null) return;

        // Culture reports use dedicated renderer
        if (td.isCulture == 1) {
            ReportGenerator.renderCultureSection(doc, td, 9 * scale);
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.DashedLine(0.5f)).setMarginBottom(dynMargin));
            return;
        }

        // ── Results Table ────────────────────────────────────────────────────
        // 4 columns: INVESTIGATION | RESULT | UNIT | REFERENCE VALUE
        Table t = new Table(UnitValue.createPercentArray(new float[]{38, 16, 14, 32}))
                .useAllAvailableWidth().setMarginBottom(6 * scale);

        // Header row — navy background, white bold text
        String[] headers = {"INVESTIGATION", "RESULT", "UNIT", "REFERENCE VALUE"};
        TextAlignment[] hAlign = {TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.CENTER, TextAlignment.CENTER};
        for (int i = 0; i < headers.length; i++) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(headers[i]).setBold().setFontSize(9 * scale)
                            .setFontColor(ColorConstants.WHITE).setTextAlignment(hAlign[i]))
                    .setBackgroundColor(TBL_HEADER_BG)
                    .setBorder(Border.NO_BORDER)
                    .setBorderLeft(i == 0 ? new SolidBorder(NAVY_DARK, 1f) : Border.NO_BORDER)
                    .setBorderRight(i == headers.length - 1 ? new SolidBorder(NAVY_DARK, 1f) : Border.NO_BORDER)
                    .setPadding(dynPadding + 1).setPaddingLeft(i == 0 ? 8 * scale : dynPadding));
        }

        // Data rows with alternating color
        boolean isMic = td.isMicroscopic == 1;
        if (!isBlank && td.results != null) {
            if (isMic) {
                Map<String, java.util.List<Map<String, String>>> grouped = new java.util.LinkedHashMap<>();
                for (Map<String, String> row : td.results) {
                    String name = row.getOrDefault("name", "").toLowerCase();
                    String cat = row.getOrDefault("category", "").trim();
                    
                    // Robust fallback for legacy data or missing categories
                    if (cat.isEmpty() || cat.equals("-")) {
                        String unitFallback = row.getOrDefault("unit", "").trim();
                        if (unitFallback.isEmpty() || unitFallback.equals("-")) {
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

                int rowIdx = 0;
                for (Map.Entry<String, java.util.List<Map<String, String>>> entry : grouped.entrySet()) {
                    // Category Header Row
                    t.addCell(new Cell(1, 4).add(new Paragraph(entry.getKey().toUpperCase())
                            .setBold().setFontSize(9 * scale).setFontColor(NAVY_DARK))
                            .setBackgroundColor(new DeviceRgb(245, 245, 245))
                            .setPadding(dynPadding + 1).setPaddingLeft(10 * scale)
                            .setBorderLeft(new SolidBorder(NAVY_DARK, 0.5f))
                            .setBorderRight(new SolidBorder(NAVY_DARK, 0.5f)));
                    
                    for (Map<String, String> r : entry.getValue()) {
                        String val = r.getOrDefault("value", "");
                        DeviceRgb rowBg = (rowIdx % 2 == 1) ? ROW_ALT : null;
                        boolean abnormal = "ABNORMAL".equalsIgnoreCase(r.get("status"));

                        Cell nameCell = new Cell().add(new Paragraph(r.getOrDefault("name", "-")).setFontSize(9 * scale).setPaddingLeft(15 * scale))
                                .setBorder(Border.NO_BORDER).setBorderLeft(new SolidBorder(NAVY_DARK, 0.5f)).setPadding(dynPadding);
                        if (rowBg != null) nameCell.setBackgroundColor(rowBg);
                        t.addCell(nameCell);

                        String flag = abnormal ? (val.contains(">") || val.contains("+") ? " \u25B2" : " \u25BC") : "";
                        Paragraph valP = new Paragraph(val.isBlank() ? "-" : val + flag).setFontSize(9 * scale).setTextAlignment(TextAlignment.CENTER);
                        if (abnormal) valP.setBold().setFontColor(RED_ABNORMAL);
                        Cell valCell = new Cell().add(valP).setBorder(Border.NO_BORDER).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                        if (rowBg != null) valCell.setBackgroundColor(rowBg);
                        t.addCell(valCell);

                        Cell unitCell = new Cell().add(new Paragraph(r.getOrDefault("unit", "-")).setFontSize(8 * scale))
                                .setBorder(Border.NO_BORDER).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                        if (rowBg != null) unitCell.setBackgroundColor(rowBg);
                        t.addCell(unitCell);

                        Cell refCell = new Cell().add(new Paragraph(r.getOrDefault("range", "-")).setFontSize(8 * scale))
                                .setBorder(Border.NO_BORDER).setBorderRight(new SolidBorder(NAVY_DARK, 0.5f)).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                        if (rowBg != null) refCell.setBackgroundColor(rowBg);
                        t.addCell(refCell);
                        rowIdx++;
                    }
                }
            } else {
                int rowIdx = 0;
                for (Map<String, String> r : td.results) {
                    String val = r.getOrDefault("value", "");
                    if (val.isBlank()) continue;

                    DeviceRgb rowBg = (rowIdx % 2 == 1) ? ROW_ALT : null;
                    boolean abnormal = "ABNORMAL".equalsIgnoreCase(r.get("status"));

                    Cell nameCell = new Cell().add(new Paragraph(r.getOrDefault("name", "-")).setFontSize(9 * scale).setPaddingLeft(5 * scale))
                            .setBorder(Border.NO_BORDER).setBorderLeft(new SolidBorder(NAVY_DARK, 0.5f)).setPadding(dynPadding);
                    if (rowBg != null) nameCell.setBackgroundColor(rowBg);
                    t.addCell(nameCell);

                    String flag = abnormal ? (val.contains(">") || val.contains("+") ? " \u25B2" : " \u25BC") : "";
                    Paragraph valP = new Paragraph(val.isBlank() ? "-" : val + flag).setFontSize(9 * scale).setTextAlignment(TextAlignment.CENTER);
                    if (abnormal) valP.setBold().setFontColor(RED_ABNORMAL);
                    Cell valCell = new Cell().add(valP).setBorder(Border.NO_BORDER).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                    if (rowBg != null) valCell.setBackgroundColor(rowBg);
                    t.addCell(valCell);

                    Cell unitCell = new Cell().add(new Paragraph(r.getOrDefault("unit", "-")).setFontSize(8 * scale))
                            .setBorder(Border.NO_BORDER).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                    if (rowBg != null) unitCell.setBackgroundColor(rowBg);
                    t.addCell(unitCell);

                    Cell refCell = new Cell().add(new Paragraph(r.getOrDefault("range", "-")).setFontSize(8 * scale))
                            .setBorder(Border.NO_BORDER).setBorderRight(new SolidBorder(NAVY_DARK, 0.5f)).setPadding(dynPadding).setTextAlignment(TextAlignment.CENTER);
                    if (rowBg != null) refCell.setBackgroundColor(rowBg);
                    t.addCell(refCell);
                    rowIdx++;
                }
            }
        }

        // Bottom border line spanning all columns
        t.addCell(new Cell(1, 4).setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(NAVY_DARK, 0.8f))
                .setBorderLeft(new SolidBorder(NAVY_DARK, 0.5f))
                .setBorderRight(new SolidBorder(NAVY_DARK, 0.5f)));

        doc.add(t);

        // ── Notes ────────────────────────────────────────────────────────────

        // ── Comments ─────────────────────────────────────────────────────────
        if (td.resultComment != null && !td.resultComment.isBlank()) {
            doc.add(new Paragraph("Comments :").setBold().setFontSize(9 * scale)
                    .setFontColor(NOTE_BLUE).setMarginTop(8 * scale).setMarginBottom(2 * scale));
            ReportGenerator.renderClinicalNotes(doc, td.resultComment.trim());
        }

        // ── Attached Images ──────────────────────────────────────────────────
        ReportGenerator.addClinicalImages(doc, td);

        doc.add(new Paragraph("").setMarginBottom(dynMargin));
    }
}

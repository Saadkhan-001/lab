package com.lab.lms.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageDataFactory;
import com.lab.lms.dao.DatabaseManager;
import java.io.File;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.properties.VerticalAlignment;

public class DefaultClinicalTemplate implements ClinicalReportTemplate {

    @Override
    public void apply(Document document, PdfDocument pdf, String patientName,
            String patientId, String age, String gender, String refDr,
            String collDate, String title, String phone, String address,
            java.util.List<ReportGenerator.TestData> allTestData, boolean isBlank,
            boolean includeHeader, boolean includeWatermark, String regDate) throws Exception {

        String labName = DatabaseManager.getSetting("lab_name", "MSF DIGITAL SOLUTIONS");
        String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
        String labContact = DatabaseManager.getSetting("lab_contact", "");
        String labEmail = DatabaseManager.getSetting("lab_email", "");
        String labWebsite = DatabaseManager.getSetting("lab_website", "");
        String labTagline = DatabaseManager.getSetting("lab_tagline", "");
        String logoPath = DatabaseManager.getSetting("lab_logo", "");
        String headerPath = DatabaseManager.getSetting("lab_header", "");
        String footerPath = DatabaseManager.getSetting("lab_footer", "");
        String doctorsFooter = DatabaseManager.getSetting("report_doctors_footer", "");

        // Default doctors
        if (doctorsFooter == null || doctorsFooter.trim().isEmpty()) {
            doctorsFooter = "SHAHIDA TASNEEM\nMBBS, M. Phil, Ph.D.\nHistopathology||ALI TALHA KHALIL\nPhD (Biotechnology)\nMolecular Biology||ARSHAD ISLAM\nM. Phil, PhD Biochemistry||MOHAMMAD TAHIR\nMBBS, DCP, FCPS\nHistopathology||KHIZAR ABDULLAH KHAN\nMBBS, DCP,FCPS\nHematology||M. RIAZUDDIN GHORI\nMBBS, MPhil Hematology";
        }

        patientName = (patientName == null || patientName.equalsIgnoreCase("null")) ? "" : patientName.trim();
        patientId = (patientId == null || patientId.equalsIgnoreCase("null")) ? "" : patientId.trim();
        age = (age == null || age.equalsIgnoreCase("null")) ? "0" : age.trim();
        gender = (gender == null || gender.equalsIgnoreCase("null") || gender.isEmpty()) ? "-" : gender.trim();
        refDr = (refDr == null || refDr.equalsIgnoreCase("null") || refDr.isEmpty()) ? "" : refDr.trim();
        phone = (phone == null || phone.equalsIgnoreCase("null")) ? "" : phone.trim();
        address = (address == null || address.equalsIgnoreCase("null")) ? "" : address.trim();

        // Fetch full age (Y M D) from database for clinical precision
        String fullAge = age + " Year(s)";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn
                    .prepareStatement("SELECT age, age_months, age_days FROM patients WHERE patient_id = ?");
            pstmt.setString(1, patientId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int y = rs.getInt("age");
                int m = rs.getInt("age_months");
                int d = rs.getInt("age_days");
                fullAge = y + "Y " + m + "M " + d + "D";
            }
        } catch (Exception e) {
        }

        // Determine Department Title based on Category
        String deptTitle = "CLINICAL";
        if (allTestData != null && !allTestData.isEmpty()) {
            String cat = allTestData.get(0).category;
            if (cat != null && !cat.isEmpty() && !cat.equalsIgnoreCase("ACTIVE")) {
                deptTitle = cat.toUpperCase();
            }
        }
        String finalTitle = deptTitle + " REPORT";

        String datePattern = "dd-MMM-yyyy HH:mm:ss";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(datePattern);
        String viewTime = sdf.format(new java.util.Date());

        String formattedRegDate = "-";
        if (regDate != null && !regDate.isEmpty()) {
            try {
                java.text.SimpleDateFormat pSdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                formattedRegDate = sdf.format(pSdf.parse(regDate)).toUpperCase();
            } catch (Exception e1) {
                formattedRegDate = regDate;
            }
        }

        float headerHeight = Float.parseFloat(DatabaseManager.getSetting("header_height", "150"));
        // Dynamic top margin: header image + title bar + patient grid (~160px) + padding
        float totalHeaderZone = headerHeight + 160;
        document.setMargins(totalHeaderZone + 20, 36, 180, 36);
        String qrData = ReportGenerator.generateQRString(labName, patientName, patientId, fullAge, gender, viewTime,
                allTestData);

        String pathologist = ReportGenerator.getReportPathologist();
        boolean enableVerification = Boolean
                .parseBoolean(DatabaseManager.getSetting("enable_electronic_verification", "true"));
        boolean enableFooter = Boolean.parseBoolean(DatabaseManager.getSetting("enable_footer", "true"));
        float footHeight = Float.parseFloat(DatabaseManager.getSetting("footer_height", "160"));

        // ── Auto-scale for report density ───────────────────────────────────
        int totalRows = 0;
        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                if (td.results != null)
                    totalRows += td.results.size();
                totalRows += 6; // overhead for headers/margins
            }
        }
        float scale = totalRows > 32 ? 0.70f : (totalRows > 24 ? 0.76f : (totalRows > 16 ? 0.86f : 1.0f));
        float dynPadding = totalRows > 32 ? 0.4f : (totalRows > 24 ? 0.6f : (totalRows > 16 ? 1.0f : 2.0f));
        float dynMargin = totalRows > 32 ? 0.6f : (totalRows > 24 ? 1.0f : (totalRows > 16 ? 2.0f : 4.0f));
        float dynTopM = totalRows > 32 ? 1.0f : (totalRows > 24 ? 1.5f : (totalRows > 16 ? 3.0f : 5.0f));

        PdfFormXObject placeholder = new PdfFormXObject(new Rectangle(0, 0, 20, 10));

        // Generate combined test names for the "TEST REQUESTED" field
        StringBuilder testNamesBuilder = new StringBuilder();
        if (allTestData != null) {
            for (int i = 0; i < allTestData.size(); i++) {
                testNamesBuilder.append(allTestData.get(i).testName.toUpperCase());
                if (i < allTestData.size() - 1)
                    testNamesBuilder.append(", ");
            }
        }
        String testsRequested = testNamesBuilder.toString();

        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new DefaultHeaderFooterHandler(
                labName, labAddress, labContact, labEmail, labWebsite, labTagline, logoPath, headerPath, footerPath, finalTitle, patientName, patientId, fullAge, gender,
                refDr, phone, address, viewTime, formattedRegDate, doctorsFooter, includeHeader, includeWatermark,
                qrData, headerHeight, footHeight, pathologist, enableVerification, enableFooter, placeholder,
                testsRequested));

        // Fill placeholder with total page count when document is finished
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument p = docEvent.getDocument();
            try (Canvas c = new Canvas(placeholder, p)) {
                c.add(new Paragraph(String.valueOf(p.getNumberOfPages())).setFontSize(9 * scale).setMargin(0)
                        .setPadding(0).setMultipliedLeading(1.0f));
            }
        });

        if (allTestData != null) {
            for (ReportGenerator.TestData td : allTestData) {
                if (td.isCulture == 1) {
                    // Refined Microbiology Culture & Sensitivity Layout (Professional Boxed Look)
                    Table cultureBox = new Table(1).useAllAvailableWidth().setMarginTop(10).setMarginBottom(10).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(10);
                    Cell boxContent = new Cell().setBorder(Border.NO_BORDER).setPadding(10);

                    boxContent.add(new Paragraph("MICROBIOLOGY | CULTURE & SENSITIVITY")
                            .setBold().setFontSize(12 * scale).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

                    // Specimen resolve logic (Look in results for specific mapping)
                    String resolvedSpec = td.specimen;
                    for (Map<String, String> r : td.results) {
                        String cType = r.get("range");
                        if (cType != null && !cType.trim().isEmpty() && !cType.equals("-") && !cType.contains("ref")) {
                            resolvedSpec = cType.trim();
                            break;
                        }
                    }

                    Paragraph pSpecCul = new Paragraph().setFontSize(11 * scale).setMarginBottom(4);
                    pSpecCul.add(new Text("SPECIMEN TYPE : ").setBold());
                    pSpecCul.add(new Text(td.cultureType != null && !td.cultureType.isEmpty() ? td.cultureType.trim().toUpperCase() : (resolvedSpec != null ? resolvedSpec.trim().toUpperCase() : "WHOLE BLOOD")));
                    boxContent.add(pSpecCul);

                    if (td.selectedOrganism != null && !td.selectedOrganism.trim().isEmpty() && !td.selectedOrganism.equalsIgnoreCase("-")) {
                        Paragraph pOrgCul = new Paragraph().setFontSize(11 * scale).setMarginBottom(4);
                        pOrgCul.add(new Text("IDENTIFIED BACTERIA : ").setBold());
                        pOrgCul.add(new Text(td.selectedOrganism.trim().toUpperCase()));
                        boxContent.add(pOrgCul);
                    }

                    if (td.incubationDuration != null && !td.incubationDuration.trim().isEmpty()) {
                        Paragraph pDurCul = new Paragraph().setFontSize(11 * scale).setMarginBottom(8);
                        pDurCul.add(new Text("INCUBATION DURATION : ").setBold());
                        pDurCul.add(new Text(td.incubationDuration.trim().toUpperCase()));
                        boxContent.add(pDurCul);
                    }

                    Paragraph pResCul = new Paragraph().setFontSize(11 * scale).setMarginBottom(8);
                    pResCul.add(new Text("RESULT : ").setBold());
                    String gFindings = td.growthFindings;
                    boolean isNeg = "Negative".equalsIgnoreCase(td.growthStatus);
                    if (gFindings == null || gFindings.trim().isEmpty() || gFindings.equalsIgnoreCase("N/A")) {
                        String duration = (td.incubationDuration != null && !td.incubationDuration.trim().isEmpty()) ? td.incubationDuration.trim() : "5 days";
                        gFindings = isNeg ? "No growth observed after " + duration + " of incubation at 37°C." : "Positive growth observed.";
                    }
                    pResCul.add(new Text(ReportGenerator.extractPlainText(gFindings)));
                    boxContent.add(pResCul);

                    // Antibiotic Sensitivity Table Logic (Only for Positive Cultures)
                    java.util.List<Map<String, String>> drugList = new java.util.ArrayList<>();
                    if (!isNeg) {
                        for (Map<String, String> r : td.results) {
                            String name = r.get("name");
                            String unit = r.get("unit");
                            String val = r.getOrDefault("value", "").trim();
                            if (name == null || unit == null || unit.trim().isEmpty() || name.equalsIgnoreCase(unit)) continue;
                            if (val.isEmpty() || val.equals("-") || val.equalsIgnoreCase("NILL")) continue;
                            if (name.toLowerCase().contains("result") || name.toLowerCase().contains("growth") || name.toLowerCase().contains("finding")) continue;
                            drugList.add(r);
                        }
                    }

                    if (!isNeg && !drugList.isEmpty()) {
                        Paragraph pSensTitle = new Paragraph("ANTIMICROBIAL SENSITIVITY :").setBold().setFontSize(10.5f * scale).setMarginTop(8).setMarginBottom(5);
                        boxContent.add(pSensTitle);

                        Table sensTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
                        sensTable.addHeaderCell(new Cell().add(new Paragraph("DRUGS").setBold().setFontSize(10 * scale)).setBackgroundColor(new DeviceRgb(240, 240, 240)).setPadding(5).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                        sensTable.addHeaderCell(new Cell().add(new Paragraph("SENSITIVITY").setBold().setFontSize(10 * scale)).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(new DeviceRgb(240, 240, 240)).setPadding(5).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));

                        for (Map<String, String> dr : drugList) {
                            sensTable.addCell(new Cell().add(new Paragraph(dr.getOrDefault("name", "-")).setFontSize(9.5f * scale)).setPadding(4).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                            String dVal = dr.getOrDefault("value", "");
                            String uVal = dVal.toUpperCase();
                            String displayVal = dVal;
                            if (uVal.equals("SENSITIVE") || uVal.equals("S")) displayVal = "S";
                            else if (uVal.equals("RESISTANCE") || uVal.equals("R")) displayVal = "R";
                            else if (uVal.equals("INTERMEDIATE") || uVal.equals("I")) displayVal = "I";
                            sensTable.addCell(new Cell().add(new Paragraph(displayVal).setFontSize(9.5f * scale)).setTextAlignment(TextAlignment.CENTER).setPadding(4).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                        }
                        boxContent.add(sensTable.setMarginBottom(5));
                        boxContent.add(new Paragraph("I = Intermediate , R = Resistance , S = Sensitive").setFontSize(8 * scale).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(5));
                    }

                    // Comments / Opinion for Culture Section
                    if (td.notes != null && !td.notes.trim().isEmpty() && !td.notes.equalsIgnoreCase("N/A")) {
                        boxContent.add(new Paragraph("Comments :").setBold().setFontSize(10 * scale).setMarginTop(5));
                        boxContent.add(new Paragraph(ReportGenerator.extractPlainText(td.notes.trim())).setFontSize(9f * scale));
                    }
                    if (td.resultComment != null && !td.resultComment.trim().isEmpty() && !td.resultComment.equalsIgnoreCase("N/A")) {
                        boxContent.add(new Paragraph("Opinion :").setBold().setFontSize(10 * scale).setMarginTop(3));
                        boxContent.add(new Paragraph(ReportGenerator.extractPlainText(td.resultComment.trim())).setFontSize(9f * scale));
                    }

                    cultureBox.addCell(boxContent);
                    document.add(cultureBox);
                } else {
                    // Specimen
                    Paragraph pSpec = new Paragraph().setFontSize(11 * scale).setMarginBottom(dynMargin)
                            .setMarginTop(dynTopM);
                    pSpec.add(new Text("SPECIMEN   : ").setBold());
                    pSpec.add(new Text(td.specimen != null && !td.specimen.isEmpty() ? td.specimen.trim().toUpperCase()
                            : "WHOLE BLOOD"));
                    document.add(pSpec);

                    // Methodology
                    String method = DatabaseManager.getSetting("report_methodology", "");
                    if (method != null && !method.trim().isEmpty()) {
                        Paragraph mp = new Paragraph().setFontSize(9 * scale).setMarginBottom(dynMargin / 2f);
                        mp.add(new Text("METHODOLOGY: ").setBold());
                        mp.add(new Text(method.trim()));
                        document.add(mp);
                    }

                    if (td.isMicroscopic == 1) {
                        // Microscopic Section (Previous implementation)
                        float[] micWidths = new float[] { 45, 25, 30 };
                        Table mTable = new Table(UnitValue.createPercentArray(micWidths)).useAllAvailableWidth();
                        mTable.addHeaderCell(new Cell().add(new Paragraph("TEST(s)").setBold().setFontSize(9.5f * scale))
                                .setBorder(Border.NO_BORDER).setPadding(dynPadding));
                        mTable.addHeaderCell(new Cell().add(new Paragraph("RESULT(s)").setBold().setFontSize(9.5f * scale))
                                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(dynPadding));
                        mTable.addHeaderCell(new Cell().add(new Paragraph("NORMAL RANGE").setBold().setFontSize(9.5f * scale))
                                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(dynPadding));

                        Map<String, java.util.List<Map<String, String>>> grouped = new LinkedHashMap<>();
                        for (Map<String, String> row : td.results) {
                            String name = row.getOrDefault("name", "").toLowerCase();
                            String cat = row.getOrDefault("category", "").trim();
                            
                            // Intelligent research-based categorization fallback
                            if (cat.isEmpty() || cat.equals("-") || cat.equalsIgnoreCase("ACTIVE") || cat.equalsIgnoreCase("Examination")) {
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
                            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(row);
                        }

                        for (Map.Entry<String, java.util.List<Map<String, String>>> entry : grouped.entrySet()) {
                            String catName = entry.getKey();
                            mTable.addCell(new Cell(1, 3).add(new Paragraph(catName.toUpperCase() + " :")
                                    .setBold().setFontSize(10.5f * scale).setFontColor(new DeviceRgb(20, 20, 20)))
                                    .setBorder(Border.NO_BORDER).setPaddingTop(6).setPaddingBottom(4).setPaddingLeft(5));

                            for (Map<String, String> row : entry.getValue()) {
                                String val = row.getOrDefault("value", "");
                                if (val == null || val.trim().isEmpty() || val.equals("-") || val.equalsIgnoreCase("NILL")) continue;

                                mTable.addCell(new Cell().add(new Paragraph("• " + row.getOrDefault("name", "-")).setPaddingLeft(10))
                                        .setFontSize(9.5f * scale).setBorder(Border.NO_BORDER).setPadding(dynPadding / 2.0f));
                                
                                if (val == null || val.isEmpty()) val = "-";
                                Paragraph pVal = new Paragraph(val).setBold().setFontSize(10.5f * scale);
                                if ("ABNORMAL".equals(row.get("status")))
                                    pVal.setFontColor(new DeviceRgb(180, 0, 0));
                                mTable.addCell(new Cell().add(pVal).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(dynPadding / 2.0f));

                                String rng = row.getOrDefault("range", "-");
                                String unt = row.getOrDefault("unit", "");
                                if (rng == null || rng.isEmpty()) rng = "-";
                                if (unt != null && !unt.isEmpty() && !unt.equals("-") && !unt.equalsIgnoreCase(catName)) {
                                    if (!rng.toLowerCase().contains(unt.toLowerCase())) rng += " " + unt;
                                }
                                mTable.addCell(new Cell().add(new Paragraph(rng)).setFontSize(9.5f * scale).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(dynPadding / 2.0f));
                            }
                        }
                        document.add(mTable.setMarginBottom(dynMargin));
                    } else {
                    // Precise 7-Column Layout for Clinical History tracking
                    int histCount = (td.historyHeaders == null) ? 0 : Math.min(td.historyHeaders.size(), 3);
                    float[] colArr = new float[4 + histCount];
                    colArr[0] = 30; // TEST(s)
                    colArr[1] = 12; // RESULT(s) - Current
                    for (int i = 0; i < histCount; i++)
                        colArr[2 + i] = 10; // History 1-3
                    colArr[2 + histCount] = 13; // UNITS
                    colArr[3 + histCount] = 25; // REFERENCE RANGE

                    Table tableHeader = new Table(UnitValue.createPercentArray(colArr)).useAllAvailableWidth();

                    tableHeader.addCell(new Cell().add(new Paragraph("TEST(s)").setBold().setFontSize(10f * scale))
                            .setBorder(Border.NO_BORDER).setPadding(dynPadding));
                    tableHeader.addCell(new Cell().add(new Paragraph("RESULT(s)").setBold().setFontSize(10f * scale))
                            .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                    // History Columns (Dates)
                    for (int i = 0; i < histCount; i++) {
                        tableHeader.addCell(
                                new Cell().add(new Paragraph(td.historyHeaders.get(i)).setBold().setFontSize(6.5f * scale))
                                        .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(1));
                    }

                    tableHeader.addCell(new Cell().add(new Paragraph("UNITS").setBold().setFontSize(10f * scale))
                            .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
                    tableHeader.addCell(new Cell().add(new Paragraph("NORMAL RANGE").setBold().setFontSize(10f * scale))
                            .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                    document.add(tableHeader.setMarginBottom(dynPadding + 1).setMarginTop(dynPadding));

                    // Results Rows
                    for (Map<String, String> row : td.results) {
                        String valStr = row.getOrDefault("value", "");
                        if (valStr == null || valStr.trim().isEmpty() || valStr.equals("-") || valStr.equalsIgnoreCase("NILL")) continue;

                        Table resRow = new Table(UnitValue.createPercentArray(colArr)).useAllAvailableWidth();

                        // Investigation Name
                        resRow.addCell(new Cell().add(new Paragraph(row.getOrDefault("name", "-")))
                                .setFontSize(9.5f * scale).setBorder(Border.NO_BORDER).setPadding(dynPadding));

                        // Current Result
                        if (valStr == null || valStr.isEmpty()) valStr = "-";
                        Paragraph pVal = new Paragraph(valStr).setBold().setFontSize(11f * scale);
                        if ("ABNORMAL".equals(row.get("status")))
                            pVal.setFontColor(new DeviceRgb(180, 0, 0));
                        resRow.addCell(
                                new Cell().add(pVal).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                        // History Values
                        for (int i = 0; i < histCount; i++) {
                            String hVal = row.getOrDefault("history_" + i, "-");
                            Paragraph phVal = new Paragraph(hVal).setFontSize(8.5f * scale);
                            if ("ABNORMAL".equals(row.get("history_status_" + i)))
                                phVal.setBold().setFontColor(new DeviceRgb(180, 0, 0));
                            resRow.addCell(new Cell().add(phVal).setBorder(Border.NO_BORDER)
                                    .setTextAlignment(TextAlignment.CENTER));
                        }

                        // Units & Range
                        resRow.addCell(new Cell().add(new Paragraph(row.getOrDefault("unit", "-"))).setFontSize(9f * scale)
                                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                        String urange = row.getOrDefault("range", "-");
                        resRow.addCell(new Cell().add(new Paragraph(urange)).setFontSize(9f * scale)
                                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                        document.add(resRow.setMarginBottom(dynMargin));
                    }
                }
            }

                document.add(new LineSeparator(new SolidLine(1.0f)).setMarginTop(5).setMarginBottom(10));


                if (td.resultComment != null && !td.resultComment.trim().isEmpty()) {
                    String comment = td.resultComment.trim();
                    if (comment.toLowerCase().startsWith("method:")) {
                        document.add(new Paragraph("Method:").setBold().setFontSize(10).setMarginBottom(2));
                        document.add(new Paragraph(comment.substring(7).trim()).setFontSize(9f).setMarginBottom(8));
                    } else if (comment.contains("|")) {
                        document.add(new Paragraph("INTERPRETATION").setBold().setFontSize(10).setMarginTop(8)
                                .setMarginBottom(4));
                        String[] lines = comment.split("\n");
                        Table interTable = new Table(UnitValue.createPercentArray(new float[] { 25, 20, 55 }))
                                .useAllAvailableWidth();
                        for (String line : lines) {
                            if (line.contains("|")) {
                                String[] cols = line.split("\\|");
                                for (String c : cols) {
                                    interTable.addCell(new Cell().add(new Paragraph(c.trim()).setFontSize(8.5f))
                                            .setPadding(3).setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f)));
                                }
                            }
                        }
                        document.add(interTable);
                    } else {
                        document.add(new Paragraph(comment).setFontSize(9f).setMarginTop(5));
                    }
                }
                ReportGenerator.addClinicalImages(document, td);
            }
        }
    }

    protected class DefaultHeaderFooterHandler implements IEventHandler {
        private String labName, labAddress, labContact, labEmail, labWebsite, labTagline, logoPath, headerPath, footerPath, title, patientName, patientId, age, gender, refDr,
                phone, address, viewTime, regDate, doctorsFooter, qrData, pathologist, testsRequested;
        private boolean includeHeader, includeWatermark, enableVerification, enableFooter;
        private float headerHeight, footerHeight;
        private PdfFormXObject placeholder;

        public DefaultHeaderFooterHandler(String labName, String labAddress, String labContact, String labEmail, String labWebsite, String labTagline, String logoPath, String headerPath, String footerPath,
                String title, String patientName, String patientId, String age, String gender,
                String refDr, String phone, String address, String viewTime, String regDate, String doctorsFooter,
                boolean includeHeader, boolean includeWatermark,
                String qrData, float headerHeight, float footerHeight, String pathologist, boolean enableVerification,
                boolean enableFooter, PdfFormXObject placeholder, String testsRequested) {
            this.labName = labName;
            this.labAddress = labAddress;
            this.labContact = labContact;
            this.labEmail = labEmail;
            this.labWebsite = labWebsite;
            this.labTagline = labTagline;
            this.logoPath = logoPath;
            this.headerPath = headerPath;
            this.footerPath = footerPath;
            this.title = title;
            this.patientName = patientName;
            this.patientId = patientId;
            this.age = age;
            this.gender = gender;
            this.refDr = refDr;
            this.phone = phone;
            this.address = address;
            this.viewTime = viewTime;
            this.regDate = regDate;
            this.doctorsFooter = doctorsFooter;
            this.includeHeader = includeHeader;
            this.includeWatermark = includeWatermark;
            this.qrData = qrData;
            this.headerHeight = headerHeight;
            this.footerHeight = footerHeight;
            this.pathologist = pathologist;
            this.enableVerification = enableVerification;
            this.enableFooter = enableFooter;
            this.placeholder = placeholder;
            this.testsRequested = testsRequested;
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

            // Watermark
            // Watermark logic: Use Logo as background watermark
            if (includeWatermark && logoPath != null && !logoPath.trim().isEmpty() && new File(logoPath).isFile()) {
                try {
                    Rectangle wmRect = new Rectangle(0, 0, pageW, pageH);
                    try (Canvas canvas = new Canvas(pdfCanvas, wmRect)) {
                        canvas.add(new Image(ImageDataFactory.create(logoPath)).setWidth(360)
                                .setFixedPosition((pageW - 360) / 2, (pageH - 360) / 2).setOpacity(0.12f));
                    }
                } catch (Exception ignored) {
                }
            }

            // Header Section — dynamic height based on headerHeight setting
            float totalHdrZone = headerHeight + 160;
            Rectangle headerRect = new Rectangle(36, pageH - totalHdrZone - 10, pageW - 72, totalHdrZone);
            try (Canvas canvas = new Canvas(pdfCanvas, headerRect)) {
                if (includeHeader && headerPath != null && !headerPath.trim().isEmpty() && new File(headerPath).isFile()
                        && !headerPath.toLowerCase().contains("no header")) {
                    try {
                        Image customHeader = new Image(ImageDataFactory.create(headerPath))
                                .setWidth(UnitValue.createPercentValue(100)).setHeight(headerHeight);
                        canvas.add(customHeader);
                    } catch (Exception e) {
                        renderLabHeader(canvas, pdf);
                    }
                } else if (includeHeader) {
                    renderLabHeader(canvas, pdf);
                } else {
                    // Leave blank space at the top for pre-printed letterhead header
                    // Using non-breaking space with zero margin/padding/leading for exact sizing
                    canvas.add(new Paragraph("\u00a0").setHeight(headerHeight)
                            .setMargin(0).setPadding(0).setMultipliedLeading(0));
                }

                // Title Bar - Optimized for Single Line Department Identity
                canvas.add(new LineSeparator(new SolidLine(1.5f)).setMarginTop(4));
                Table titleBar = new Table(UnitValue.createPercentArray(new float[] { 28, 44, 28 }))
                        .useAllAvailableWidth().setBackgroundColor(new DeviceRgb(240, 240, 240));
                titleBar.addCell(new Cell().add(new Paragraph("VIEW: " + viewTime).setFontSize(8.5f))
                        .setBorder(Border.NO_BORDER).setPadding(4).setVerticalAlignment(VerticalAlignment.MIDDLE));
                titleBar.addCell(
                        new Cell()
                                .add(new Paragraph(title != null && !title.isEmpty() ? title : "CLINICAL REPORT")
                                        .setBold().setFontSize(14))
                                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

                Paragraph pgP = new Paragraph("Page " + pageNum + " of ")
                        .add(new Image(placeholder).setPaddingTop(2.2f)).setFontSize(9).setMultipliedLeading(1.0f);
                titleBar.addCell(new Cell().add(pgP).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4).setVerticalAlignment(VerticalAlignment.MIDDLE));

                canvas.add(titleBar);
                canvas.add(new LineSeparator(new SolidLine(1.5f)).setMarginBottom(8));

                // Patient Info Grid - Reorganized for Laboratory Protocol
                Table patGrid = new Table(UnitValue.createPercentArray(new float[] { 15, 35, 20, 30 }))
                        .useAllAvailableWidth().setFontSize(9.5f);
                patGrid.setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));

                String orderVal = (refDr == null || refDr.isEmpty() || refDr.equalsIgnoreCase("Self")) ? "-"
                        : refDr.toUpperCase();
                String dispAddress = (address == null || address.trim().isEmpty()) ? "-" : address.trim().toUpperCase();
                String dispPhone = (phone == null || phone.trim().isEmpty()) ? "-" : phone.trim().toUpperCase();
                String dispRegDate = (regDate == null || regDate.trim().isEmpty()) ? "-" : regDate.trim();

                // Row 1: MRNO & Test Requested
                addBioRowWithCondition(patGrid, "MRNO", ": " + patientId, "Test Requested", ": " + testsRequested, 9.5f,
                        7.5f);

                // Row 2: Name & Referred By
                addBioRow(patGrid, "Name", ": " + patientName.toUpperCase(), "Referred By", ": " + orderVal);

                // Row 3: Age/Sex & Requested
                addBioRow(patGrid, "Age/Sex", ": " + age + "Y / " + gender, "Requested", ": " + dispRegDate);

                // Row 4: Phone & Specimen Received
                String specTime = ReportGenerator.getClinicalOffsetTime(viewTime, 3);
                addBioRow(patGrid, "Phone", ": " + dispPhone, "Specimen Received", ": " + specTime);

                // Row 5: Address & Reported By
                String pathName = (pathologist != null && !pathologist.isBlank()) ? pathologist.split("\n")[0].trim() : "ADMIN";
                addBioRow(patGrid, "Address", ": " + dispAddress, "Reported By", ": " + pathName);

                canvas.add(patGrid.setMarginBottom(8).setPadding(2));
                canvas.add(new LineSeparator(new SolidLine(1.0f)).setMarginBottom(5));
            }

            // Footer Section — only render content when includeHeader is true
            // When !includeHeader, leave blank space for pre-printed letterhead paper
            if (enableFooter && includeHeader) {
                Rectangle footerRect = new Rectangle(36, 10, pageW - 72, footerHeight);
                try (Canvas canvas = new Canvas(pdfCanvas, footerRect)) {
                    if (footerPath != null && !footerPath.trim().isEmpty() && new File(footerPath).isFile()) {
                        try {
                            Image customFooter = new Image(ImageDataFactory.create(footerPath))
                                    .setWidth(UnitValue.createPercentValue(100)).setHeight(footerHeight);
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
            // Signature
            String[] psLines = (pathologist != null && !pathologist.isEmpty()) ? pathologist.split("\n")
                    : new String[] { "Ahmad Ullah", "Sr. Medical Technologist" };
            Table sigTable = new Table(1).useAllAvailableWidth();
            for (int i = 0; i < psLines.length; i++) {
                Paragraph p = new Paragraph(psLines[i].trim()).setTextAlignment(TextAlignment.RIGHT);
                if (i == 0)
                    p.setBold().setFontSize(10.5f);
                else
                    p.setFontSize(9.5f);
                sigTable.addCell(new Cell().add(p).setBorder(Border.NO_BORDER));
            }
            canvas.add(sigTable.setMarginTop(5).setMarginBottom(6));

            // Verified Bar
            if (enableVerification) {
                Table vBar = new Table(1).useAllAvailableWidth()
                        .setBackgroundColor(new DeviceRgb(230, 230, 230));
                vBar.addCell(new Cell()
                        .add(new Paragraph("Electronically verified report, no signature(s) required.")
                                .setBold().setFontSize(9.5f).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f)).setPadding(4));
                canvas.add(vBar.setMarginBottom(4));
            }

            // Separator Line and Dynamic Doctors Grid
            canvas.add(new LineSeparator(new SolidLine(1.0f)).setMarginBottom(8).setMarginTop(4));
            if (doctorsFooter != null && !doctorsFooter.isEmpty()) {
                String[] df = doctorsFooter.split("\\|\\|");
                float[] ws = new float[Math.min(df.length, 5)];
                java.util.Arrays.fill(ws, 100f / ws.length);
                Table dG = new Table(UnitValue.createPercentArray(ws)).useAllAvailableWidth();
                for (int i = 0; i < ws.length; i++) {
                    String[] lines = df[i].trim().split("\n");
                    Cell c = new Cell().setBorder(Border.NO_BORDER);
                    for (int j = 0; j < lines.length; j++) {
                        Paragraph lp = new Paragraph(lines[j].trim()).setFontSize(6.5f);
                        if (j == 0)
                            lp.setBold().setFontSize(7.5f);
                        c.add(lp);
                    }
                    dG.addCell(c);
                }
                canvas.add(dG);
            }
        }

        private void renderLabHeader(Canvas canvas, PdfDocument pdf) {
            Table header = new Table(UnitValue.createPercentArray(new float[] { 20, 60, 20 })).useAllAvailableWidth();

            // Left Column: QR Code Implementation
            Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            try {
                com.itextpdf.barcodes.BarcodeQRCode qrCode = new com.itextpdf.barcodes.BarcodeQRCode(
                        qrData != null && !qrData.isEmpty() ? qrData : "LIS");
                qrCell.add(new Image(qrCode.createFormXObject(pdf)).setWidth(70));
            } catch (Exception e) {
                qrCell.add(new Paragraph(
                        "QR ID:" + (qrData != null && qrData.length() > 5 ? qrData.substring(0, 5) : "LIS"))
                        .setFontSize(6));
            }
            header.addCell(qrCell);

            // Center Column: Lab Identity (Comprehensive Branding)
            Cell center = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            
            // 1. Lab Name
            center.add(new Paragraph(labName).setBold().setFontSize(19).setMarginBottom(0));
            
            // 2. Tagline (If exists)
            if (labTagline != null && !labTagline.trim().isEmpty()) {
                center.add(new Paragraph(labTagline.toUpperCase()).setFontSize(9f).setMarginBottom(1).setMarginTop(0));
            }
            
            // 3. Address
            if (labAddress != null && !labAddress.trim().isEmpty()) {
                center.add(new Paragraph(labAddress).setFontSize(9f).setMarginTop(0).setMarginBottom(1));
            }
            
            // 4. Contact Metadata Line (Phone | Email | Web)
            StringBuilder contactLine = new StringBuilder();
            if (labContact != null && !labContact.trim().isEmpty()) {
                contactLine.append("Contact: ").append(labContact);
            }
            if (labEmail != null && !labEmail.trim().isEmpty()) {
                if (contactLine.length() > 0) contactLine.append(" | ");
                contactLine.append("Email: ").append(labEmail.toLowerCase());
            }
            if (labWebsite != null && !labWebsite.trim().isEmpty()) {
                if (contactLine.length() > 0) contactLine.append(" | ");
                contactLine.append("Web: ").append(labWebsite.toLowerCase());
            }
            
            if (contactLine.length() > 0) {
                center.add(new Paragraph(contactLine.toString()).setFontSize(8.5f).setMarginTop(1));
            }
            
            header.addCell(center);

            // Right Column: Professional Logo
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            if (logoPath != null && !logoPath.trim().isEmpty() && new File(logoPath).isFile()) {
                try {
                    logoCell.add(new Image(ImageDataFactory.create(logoPath)).setWidth(65));
                } catch (Exception e) {
                }
            }
            header.addCell(logoCell);

            canvas.add(header.setMarginBottom(6));
        }

        private void addBioRowWithCondition(Table table, String lL, String vL, String lR, String vR, float defaultFs,
                float shrinkFs) {
            table.addCell(new Cell().add(new Paragraph(lL).setBold()).setBorder(Border.NO_BORDER).setPadding(1)
                    .setPaddingLeft(5));
            table.addCell(new Cell().add(new Paragraph(vL)).setBorder(Border.NO_BORDER).setPadding(1));
            table.addCell(new Cell().add(new Paragraph(lR).setBold()).setBorder(Border.NO_BORDER).setPadding(1)
                    .setPaddingLeft(5));

            float targetFs = (vR != null && vR.length() > 35) ? shrinkFs : defaultFs;
            table.addCell(
                    new Cell().add(new Paragraph(vR).setFontSize(targetFs)).setBorder(Border.NO_BORDER).setPadding(1));
        }

        private void addBioRow(Table table, String lL, String vL, String lR, String vR) {
            table.addCell(new Cell().add(new Paragraph(lL).setBold()).setBorder(Border.NO_BORDER).setPadding(1)
                    .setPaddingLeft(5));
            table.addCell(new Cell().add(new Paragraph(vL)).setBorder(Border.NO_BORDER).setPadding(1));
            table.addCell(new Cell().add(new Paragraph(lR).setBold()).setBorder(Border.NO_BORDER).setPadding(1)
                    .setPaddingLeft(5));
            table.addCell(new Cell().add(new Paragraph(vR)).setBorder(Border.NO_BORDER).setPadding(1));
        }
    }
}

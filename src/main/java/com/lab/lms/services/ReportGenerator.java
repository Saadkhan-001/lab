package com.lab.lms.services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import java.sql.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.lab.lms.dao.DatabaseManager;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.lab.lms.models.Test;
import com.lab.lms.models.TestParameter;
import com.lab.lms.models.Staff;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.geom.PageSize;
import java.util.Date;
import java.util.TimeZone;

public class ReportGenerator {

        public static class TestData {
                public String testName;
                public int testId;
                public String category;
                public java.util.List<java.util.Map<String, String>> results;
                public String notes;
                public int isSpecial;
                public int isMicroscopic;
                public int isCulture;
                public String specimen;
                public String resultComment;
                public String referenceImage;
                public java.util.List<String> resultImages;
                public String sampleId;
                public String growthStatus;
                public String selectedOrganism;
                public java.util.List<String> historyHeaders = new java.util.ArrayList<>();
                public java.util.Map<String, java.util.List<String>> historyMap = new java.util.LinkedHashMap<>();

                public TestData(String testName, int testId, java.util.List<java.util.Map<String, String>> results, String notes, int isS, int isM, int isC, String spec, String comment, String sid) {
                        this(testName, testId, "ACTIVE", results, notes, isS, isM, isC, spec, comment, null, null, sid, "Positive");
                }

                public TestData(String testName, int testId, java.util.List<java.util.Map<String, String>> results, String notes, int isS, int isM, int isC, String spec, String comment, String refI, String sid) {
                        this(testName, testId, "ACTIVE", results, notes, isS, isM, isC, spec, comment, refI, null, sid, "Positive");
                }

                public TestData(String testName, int testId, java.util.List<java.util.Map<String, String>> results, String notes, int isS, int isM, int isC, String spec, String comment, String refI, java.util.List<String> resIs, String sid) {
                        this(testName, testId, "ACTIVE", results, notes, isS, isM, isC, spec, comment, refI, resIs, sid, "Positive");
                }

                public TestData(String testName, int testId, String category, java.util.List<java.util.Map<String, String>> results, String notes, int isS, int isM, int isC, String spec, String comment, String refI, java.util.List<String> resIs, String sid) {
                        this(testName, testId, category, results, notes, isS, isM, isC, spec, comment, refI, resIs, sid, "Positive");
                }

                public TestData(String testName, int testId, String category, java.util.List<java.util.Map<String, String>> results, String notes, int isS, int isM, int isC, String spec, String comment, String refI, java.util.List<String> resIs, String sid, String growth) {
                        this.testName = testName;
                        this.testId = testId;
                        this.category = (category == null || category.trim().isEmpty()) ? "ACTIVE" : category;
                        this.results = results;
                        this.notes = notes;
                        this.isSpecial = isS;
                        this.isMicroscopic = isM;
                        this.isCulture = isC;
                        this.specimen = spec;
                        this.resultComment = comment;
                        this.referenceImage = refI;
                        this.resultImages = resIs;
                        this.sampleId = sid;
                        this.growthStatus = (growth == null) ? "Positive" : growth;
                        this.selectedOrganism = null;
                        this.growthFindings = null;
                }
                public String growthFindings;
                public String cultureType;
                public String incubationDuration;
        }

    public static String generateRateList(List<Test> tests, String filterLabel) {
        String dir = getReceiptDir();
        File file = new File(dir, "RATE_LIST_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(20, 36, 40, 36);

            String labName = DatabaseManager.getSetting("lab_name", "MSF DIGITAL SOLUTIONS (SMC-PRIVATE) LIMITED");
            String labAddress = DatabaseManager.getSetting("lab_address", "");
            String labContact = DatabaseManager.getSetting("lab_contact", "");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");
            String headerPath = DatabaseManager.getSetting("lab_header", "");

            if (headerPath != null && !headerPath.trim().isEmpty() && new File(headerPath).isFile() && !headerPath.toLowerCase().contains("no header")) {
                Image customHeader = new Image(ImageDataFactory.create(headerPath)).setWidth(UnitValue.createPercentValue(100));
                document.add(customHeader);
            } else {
                addDefaultHeader(document, pdf, labName, labAddress, labContact, logoPath, "LMS-CATALOG");
            }

            document.add(new Paragraph("DIAGNOSTIC TEST CATALOGUE (" + filterLabel + ")").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(150, 17, 17)));
            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(10));

            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 15, 55, 15})).useAllAvailableWidth();
            
            table.addHeaderCell(new Cell().add(new Paragraph("CODE")).setBold().setFontSize(8).setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE));
            table.addHeaderCell(new Cell().add(new Paragraph("ALPHA")).setBold().setFontSize(8).setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE));
            table.addHeaderCell(new Cell().add(new Paragraph("TEST DESCRIPTION")).setBold().setFontSize(8).setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE));
            table.addHeaderCell(new Cell().add(new Paragraph("PRICE (PKR)")).setBold().setFontSize(8).setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER));

            for (Test t : tests) {
                table.addCell(new Cell().add(new Paragraph(t.getNumericCode() != null ? t.getNumericCode() : "-")).setFontSize(8).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(t.getAlphaCode() != null ? t.getAlphaCode() : "-")).setFontSize(8).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(t.getName().toUpperCase())).setFontSize(8).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f", t.getPrice()))).setFontSize(8).setBold().setPadding(2).setTextAlignment(TextAlignment.RIGHT));
            }

            document.add(table);
            document.add(new LineSeparator(new SolidLine(1f)).setMarginTop(20));
            document.add(new Paragraph("Printed on: " + new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new java.util.Date()))
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void logError(Exception e) {
        try {
            String labDir = System.getProperty("user.home") + File.separator + ".lablms";
            File logFile = new File(labDir, "error_log.txt");
            java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
            java.io.PrintWriter pw = new java.io.PrintWriter(fw);
            pw.println("--- ERROR: " + new java.util.Date().toString() + " ---");
            e.printStackTrace(pw);
            pw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String getReportDir() {
        String dir = System.getProperty("user.home") + File.separator + ".lablms" + File.separator + "reports";
        new File(dir).mkdirs();
        return dir;
    }

    private static float getFooterMargin() {
        try {
            float h = Float.parseFloat(DatabaseManager.getSetting("footer_height", "80"));
            return Math.max(36, h + 20);
        } catch (Exception e) {
            return 80f;
        }
    }

    private static String getReceiptDir() {
        String dir = System.getProperty("user.home") + File.separator + ".lablms" + File.separator + "receipts";
        new File(dir).mkdirs();
        return dir;
    }

        public static String generateReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String testName, String phone, String address,
                        List<Map<String, String>> results, boolean includeHeader, boolean includeWatermark, String resultComment, String growthFindings) {
            return generateReport(patientName, patientId, age, gender, refDr, collDate, testName, phone, address, results, includeHeader, includeWatermark, resultComment, null, null, null, growthFindings);
        }

        public static String generateReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String testName, String phone, String address,
                        List<Map<String, String>> results, boolean includeHeader, boolean includeWatermark, String resultComment, String sampleId, String growthFindings) {
            return generateReport(patientName, patientId, age, gender, refDr, collDate, testName, phone, address, results, includeHeader, includeWatermark, resultComment, null, sampleId, null, growthFindings);
        }

        public static String generateReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String testName, String phone, String address,
                        List<Map<String, String>> results, boolean includeHeader, boolean includeWatermark, String resultComment, String labNotes, String sampleId, String growthFindings) {
                return generateReport(patientName, patientId, age, gender, refDr, collDate, testName, phone, address, results, includeHeader, includeWatermark, resultComment, labNotes, sampleId, null, growthFindings);
        }

        public static String generateReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String testName, String phone, String address,
                        List<Map<String, String>> results, boolean includeHeader, boolean includeWatermark, String resultComment, String labNotes, String sampleId, String selectedOrganism, String growthFindings) {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, patientId + "_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        String notes = "";
                        int isSp = 0;
                        int isMic = 0;
                        int isCul = 0;
                        String spec = "Blood";
                        String refImg = "";
                        List<String> resImgs = new ArrayList<>();
                        int tId = 0;
                        String duration = "5 Days";
                        String cType = spec;
                        String dbGrowthFindings = null;
                        String regDate = collDate; 
                        String gStatusFinal = "Positive";
                        String dbCategory = "ACTIVE";

                        try (Connection conn = DatabaseManager.getConnection()) {
                            PreparedStatement pstmt = conn.prepareStatement("SELECT id, category, notes, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings, image_path FROM tests WHERE LOWER(name) = LOWER(?)");
                            pstmt.setString(1, testName);
                            ResultSet rs = pstmt.executeQuery();
                            // dbCategory moved outside
                            if (rs.next()) {
                                tId = rs.getInt("id");
                                dbCategory = rs.getString("category");
                                notes = (labNotes != null && !labNotes.trim().isEmpty()) ? labNotes : rs.getString("notes");
                                try { isSp = rs.getInt("is_special"); } catch (SQLException e) {}
                                try { isMic = rs.getInt("is_microscopic"); } catch (SQLException e) {}
                                try { isCul = rs.getInt("is_culture"); } catch (SQLException e) {}
                                try { spec = rs.getString("specimen"); } catch (SQLException e) {}
                                try {
                                    gStatusFinal = rs.getString("growth_status");
                                    if (gStatusFinal == null) gStatusFinal = "Positive";
                                } catch (SQLException e) { gStatusFinal = "Positive"; }
                                try { dbGrowthFindings = rs.getString("growth_findings"); } catch (SQLException e) {}
                                try { refImg = rs.getString("image_path"); } catch (SQLException e) {}
                            }    

                            // Fetch Patient-Specific Culture Metadata from results table
                            if (sampleId != null && isCul == 1) {
                                String metaSql = "SELECT identified_organism, culture_type, growth_status, growth_findings, duration " +
                                                 "FROM results WHERE sample_id = ? AND test_id = ? LIMIT 1";
                                try (PreparedStatement mPstmt = conn.prepareStatement(metaSql)) {
                                    mPstmt.setString(1, sampleId);
                                    mPstmt.setInt(2, tId);
                                    ResultSet mRs = mPstmt.executeQuery();
                                    if (mRs.next()) {
                                        String dbOrg = mRs.getString("identified_organism");
                                        String dbCType = mRs.getString("culture_type");
                                        String dbStat = mRs.getString("growth_status");
                                        String dbGFin = mRs.getString("growth_findings");
                                        String dbDur = mRs.getString("duration");

                                        if (dbOrg != null && !dbOrg.isEmpty()) selectedOrganism = dbOrg;
                                        if (dbCType != null && !dbCType.isEmpty()) cType = dbCType;
                                        if (dbStat != null && !dbStat.isEmpty()) gStatusFinal = dbStat;
                                        if (dbGFin != null && !dbGFin.isEmpty()) growthFindings = dbGFin;
                                        if (dbDur != null && !dbDur.isEmpty()) duration = dbDur;
                                    }
                                }
                            }

                            if (sampleId != null) {
                                PreparedStatement resImgPstmt = conn.prepareStatement("SELECT image_path FROM test_images WHERE sample_id = ? AND test_id = ?");
                                resImgPstmt.setString(1, sampleId);
                                resImgPstmt.setInt(2, tId);
                                ResultSet rsResImg = resImgPstmt.executeQuery();
                                while (rsResImg.next()) resImgs.add(rsResImg.getString("image_path"));
                            }
                            
                            PreparedStatement patPstmt = conn.prepareStatement("SELECT registration_date FROM patients WHERE patient_id = ?");
                            patPstmt.setString(1, patientId);
                            ResultSet rsPat = patPstmt.executeQuery();
                            if (rsPat.next()) {
                                String dbReg = rsPat.getString("registration_date");
                                if (dbReg != null && !dbReg.isEmpty()) regDate = dbReg;
                            }
                        } catch (Exception e) {}
                        
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);
                        document.setMargins(155f, 36, 60f, 36);

                        String finalReportTitle = testName;
                        if (results != null) {
                            List<Map<String, String>> validResults = new ArrayList<>();
                            for (Map<String, String> r : results) {
                                String val = r.get("value");
                                if (val != null && !val.trim().isEmpty() && !val.equalsIgnoreCase("NILL")) {
                                    validResults.add(r);
                                }
                            }
                            if (validResults.size() == 1) {
                                finalReportTitle = validResults.get(0).getOrDefault("name", testName);
                            }
                        }
                        TestData td = new TestData(finalReportTitle, tId, dbCategory, results, notes, isSp, isMic, isCul, spec, resultComment, refImg, resImgs, sampleId, gStatusFinal);
                        td.selectedOrganism = selectedOrganism;
                        td.growthFindings = (growthFindings != null && !growthFindings.trim().isEmpty()) ? growthFindings : dbGrowthFindings;
                        td.cultureType = cType;
                        td.incubationDuration = duration;
                        try (Connection conn = DatabaseManager.getConnection()) {
                            populateHistory(conn, patientId, sampleId, tId, td);
                        } catch (Exception e) {}

                        ClinicalTemplateFactory.getTemplate().apply(document, pdf, patientName, patientId, age, gender, refDr, collDate,
                                    finalReportTitle, phone, address, java.util.Collections.singletonList(td), false, includeHeader, includeWatermark, regDate);
                        document.close();
                        return dest;
                } catch (Exception e) {
                        logError(e);
                        return null;
                }
        }

        public static String generateMultiTestReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String phone, String address,
                        List<TestData> allTestData, boolean includeHeader, boolean includeWatermark) {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, patientId + "_COMB_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        String regDate = collDate; 
                        try (Connection conn = DatabaseManager.getConnection()) {
                             PreparedStatement patPstmt = conn.prepareStatement("SELECT registration_date FROM patients WHERE patient_id = ?");
                             patPstmt.setString(1, patientId);
                             ResultSet rsPat = patPstmt.executeQuery();
                             if (rsPat.next()) {
                                 String dbReg = rsPat.getString("registration_date");
                                 if (dbReg != null && !dbReg.isEmpty()) regDate = dbReg;
                             }
                        } catch (Exception e) {}

                        Document document = new Document(pdf);
                        document.setMargins(155f, 36, 60f, 36);
                        
                        try (Connection conn = DatabaseManager.getConnection()) {
                            for (TestData td : allTestData) {
                                populateHistory(conn, patientId, td.sampleId, td.testId, td);
                            }
                        } catch (Exception e) {}

                        for (TestData td : allTestData) {
                            if (td.results != null) {
                                List<Map<String, String>> active = new ArrayList<>();
                                for (Map<String, String> r : td.results) {
                                    String val = r.get("value");
                                    if (val != null && !val.trim().isEmpty() && !val.equalsIgnoreCase("NILL")) {
                                        active.add(r);
                                    }
                                }
                                if (active.size() == 1) {
                                    td.testName = active.get(0).getOrDefault("name", td.testName);
                                }
                            }
                        }

                        String globalTitle = "Consolidated Diagnostic";
                        if (allTestData.size() == 1) {
                            globalTitle = allTestData.get(0).testName;
                        }

                        ClinicalTemplateFactory.getTemplate().apply(document, pdf, patientName, patientId, age, gender, refDr, collDate,
                                    globalTitle, phone, address, allTestData, false, includeHeader, includeWatermark, regDate);

                        document.close();
                        return dest;
                } catch (Exception e) {
                        logError(e);
                        return null;
                }
        }

        public static String generateBlankReport(String patientName, String patientId, String age, String gender,
                        String refDr, String collDate, String phone, String address, List<Integer> testIds, boolean includeHeader, boolean includeWatermark) {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, (patientId == null || patientId.isEmpty() ? "BLANK" : patientId) + "_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);
                        document.setMargins(155f, 36, 60f, 36);
                        List<TestData> allTestData = new ArrayList<>();
                        try (Connection conn = DatabaseManager.getConnection()) {
                                for (Integer tId : testIds) {
                                        String tName = "Unknown Test";
                                        String dbCategory = "ACTIVE";
                                        String tNotes = "";
                                        int tSpecial = 0;
                                        int tMicro = 0;
                                        int tCulture = 0;
                                        String tSpecimen = "Blood";
                                        String tRefImg = "";
                                        String tGrowth = "Positive";
                                        try (PreparedStatement testPstmt = conn.prepareStatement("SELECT name, category, notes, is_special, is_microscopic, is_culture, specimen, growth_status, growth_findings, image_path FROM tests WHERE id = ?")) {
                                                testPstmt.setInt(1, tId);
                                                try (ResultSet trs = testPstmt.executeQuery()) {
                                                        if (trs.next()) {
                                                                tName = trs.getString("name");
                                                                dbCategory = trs.getString("category");
                                                                tNotes = trs.getString("notes");
                                                                tSpecial = trs.getInt("is_special");
                                                                tMicro = trs.getInt("is_microscopic");
                                                                tCulture = trs.getInt("is_culture");
                                                                tSpecimen = trs.getString("specimen");
                                                                tGrowth = trs.getString("growth_status");
                                                                if (tGrowth == null) tGrowth = "Positive";
                                                                tRefImg = trs.getString("image_path");
                                                        }
                                                }
                                        }

                                        java.util.List<Map<String, String>> paramList = new java.util.ArrayList<>();
                                        String sql = "SELECT name, unit, category, min_range, max_range, min_range_male, max_range_male, min_range_female, max_range_female, min_range_kids, max_range_kids " +
                                                     "FROM test_parameters WHERE test_id = ? ORDER BY CASE WHEN print_order = 0 THEN 999999 ELSE print_order END ASC, id ASC";

                                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                                pstmt.setInt(1, tId);
                                                try (ResultSet rs = pstmt.executeQuery()) {
                                                        while (rs.next()) {
                                                                java.util.Map<String, String> row = new java.util.HashMap<>();
                                                                row.put("name", rs.getString("name"));
                                                                row.put("unit", rs.getString("unit"));
                                                                row.put("category", rs.getString("category"));
                                                                TestParameter tp = new TestParameter(0, tId, rs.getString("name"),
                                                                        rs.getString("unit"), rs.getString("min_range"), rs.getString("max_range"),
                                                                        rs.getString("min_range_male"), rs.getString("max_range_male"),
                                                                        rs.getString("min_range_female"), rs.getString("max_range_female"),
                                                                        rs.getString("min_range_kids"), rs.getString("max_range_kids"));
                                                                row.put("range", tp.getRange(gender, age));
                                                                row.put("value", " ");
                                                                paramList.add(row);
                                                        }
                                                }
                                        }
                                        allTestData.add(new TestData(tName, tId, dbCategory, paramList, tNotes, tSpecial, tMicro, tCulture, tSpecimen, "", tRefImg, new ArrayList<>(), "", tGrowth));
                                }
                        }

                        String regDate = collDate;
                        try (Connection conn = DatabaseManager.getConnection()) {
                            if (patientId != null && !patientId.isEmpty()) {
                                PreparedStatement patPstmt = conn.prepareStatement("SELECT registration_date FROM patients WHERE patient_id = ?");
                                patPstmt.setString(1, patientId);
                                ResultSet rsPat = patPstmt.executeQuery();
                                if (rsPat.next()) {
                                    String dbReg = rsPat.getString("registration_date");
                                    if (dbReg != null && !dbReg.isEmpty()) regDate = dbReg;
                                }
                            }
                        } catch (Exception e) {}

                        ClinicalTemplateFactory.getTemplate().apply(document, pdf, patientName, patientId, age, gender, refDr, collDate,
                                    (allTestData.size() == 1 ? allTestData.get(0).testName : "Consolidated Diagnostic"), phone, address, allTestData, false, includeHeader, includeWatermark, regDate);

                        document.close();
                        return dest;
                } catch (Exception e) {
                        logError(e);
                        return null;
                }
        }

        public static String generateBrandingOnly(String hexColor) {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, "BRANDING_TEMPLATE_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);
                        document.setMargins(20, 36, getFooterMargin(), 36);

                        String labName = DatabaseManager.getSetting("lab_name", "Laboratory Information System");
                        String labAddress = DatabaseManager.getSetting("lab_address", "");
                        String labContact = DatabaseManager.getSetting("lab_contact", "");
                        String logoPath = DatabaseManager.getSetting("lab_logo", "");
                        String headerPath = DatabaseManager.getSetting("lab_header", "");
                        String doctorsFooter = DatabaseManager.getSetting("report_doctors_footer", "");

                        int r = Integer.valueOf(hexColor.substring(1, 3), 16);
                        int g = Integer.valueOf(hexColor.substring(3, 5), 16);
                        int b = Integer.valueOf(hexColor.substring(5, 7), 16);
                        DeviceRgb footerColor = new DeviceRgb(r, g, b);

                        if (headerPath != null && !headerPath.trim().isEmpty() && new File(headerPath).isFile() && !headerPath.contains("No header uploaded")) {
                                Image customHeader = new Image(ImageDataFactory.create(headerPath)).setWidth(UnitValue.createPercentValue(100));
                                document.add(customHeader);
                        } else {
                                addDefaultHeader(document, pdf, labName, labAddress, labContact, logoPath, "LMS-TEMPLATE");
                        }

                        float footerY = 40;
                        if (doctorsFooter == null || doctorsFooter.trim().isEmpty()) {
                                doctorsFooter = "DR KHALID KHAN\nProfessor Hematology\nMBBS,FCPS\n|| DR ZAHID ULLAH KHAN\nAssociate Professor Microbiology\nMBBS,DA MPHIL,CHPE\n|| DR SHAGUFTA NASIR\nAssociate Professor Histopathology\nMBBS,DCP, FCPS\n|| DR HUMA RIAZ\nAssociate Professor Hematology\nMBBS,FCPS";
                        }

                        String[] docs = doctorsFooter.split("\\|\\|");
                        Table docGrid = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 })).useAllAvailableWidth();
                        for (String d : docs) {
                                String[] lines = d.trim().split("\n");
                                Cell cell = new Cell().setBorder(Border.NO_BORDER).setFontColor(footerColor);
                                for (int k = 0; k < lines.length; k++) {
                                        Paragraph p = new Paragraph(lines[k].trim()).setFontSize(7).setMultipliedLeading(1.0f);
                                        if (k == 0) p.setBold();
                                        cell.add(p);
                                }
                                docGrid.addCell(cell);
                        }
                        docGrid.setFixedPosition(36, footerY, pdf.getDefaultPageSize().getWidth() - 72);
                        document.add(docGrid);

                        document.close();
                        return dest;
                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        private static void populateHistory(Connection conn, String patientId, String currentSampleId, int testId, TestData td) {
            try {
                if (patientId == null || testId <= 0) return;
                
                List<String> prevSamples = new ArrayList<>();
                List<String> prevDates = new ArrayList<>();
                
                String sql = "SELECT DISTINCT s.sample_id, s.collection_date FROM samples s " +
                             "JOIN results r ON s.sample_id = r.sample_id " +
                             "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                             "WHERE s.patient_id = ? AND tp.test_id = ? AND r.doctor_approval = 1 ";
                if (currentSampleId != null) sql += "AND s.sample_id != ? ";
                sql += "ORDER BY s.collection_date DESC LIMIT 3";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, patientId);
                    pstmt.setInt(2, testId);
                    if (currentSampleId != null) pstmt.setString(3, currentSampleId);
                    
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        prevSamples.add(rs.getString("sample_id"));
                        String dateStr = rs.getString("collection_date");
                        try {
                            java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
                            prevDates.add(new java.text.SimpleDateFormat("dd-MMM-yyyy\nhh:mm:ss a").format(d).toUpperCase());
                        } catch (Exception e) {
                            prevDates.add(dateStr);
                        }
                    }
                }
                
                td.historyHeaders.addAll(prevDates);
                if (prevSamples.isEmpty()) return;
                
                for (Map<String, String> row : td.results) {
                    String paramName = row.get("name");
                    for (int i = 0; i < prevSamples.size(); i++) {
                        String sid = prevSamples.get(i);
                        String vSql = "SELECT r.value, r.is_abnormal FROM results r " +
                                      "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                      "WHERE r.sample_id = ? AND tp.name = ? AND tp.test_id = ? LIMIT 1";
                        try (PreparedStatement vPstmt = conn.prepareStatement(vSql)) {
                            vPstmt.setString(1, sid);
                            vPstmt.setString(2, paramName);
                            vPstmt.setInt(3, testId);
                            ResultSet vRs = vPstmt.executeQuery();
                            if (vRs.next()) {
                                row.put("history_" + i, vRs.getString("value"));
                                row.put("history_status_" + i, vRs.getInt("is_abnormal") == 1 ? "ABNORMAL" : "NORMAL");
                            } else {
                                row.put("history_" + i, "-");
                                row.put("history_status_" + i, "NORMAL");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public static void addDefaultHeader(Document document, PdfDocument pdf, String labName, String labAddress, String labContact, String logoPath, String qrData) throws Exception {
        addDefaultHeaderBlock(document, pdf, labName, labAddress, labContact, logoPath, qrData, "", "", "");
    }

    public static void addDefaultHeader(com.itextpdf.layout.Canvas canvas, PdfDocument pdf, String labName, String labAddress, String labContact, String logoPath, String qrData) throws Exception {
        addDefaultHeaderBlock(canvas, pdf, labName, labAddress, labContact, logoPath, qrData, "", "", "");
    }

    public static void addDefaultHeader(com.itextpdf.layout.Canvas canvas, PdfDocument pdf, String labName, String labAddress, String labContact, String logoPath, String qrData, String labEmail, String labWebsite) throws Exception {
        addDefaultHeaderBlock(canvas, pdf, labName, labAddress, labContact, logoPath, qrData, labEmail, labWebsite, "");
    }

    public static void addDefaultHeader(com.itextpdf.layout.Canvas canvas, PdfDocument pdf, String labName, String labAddress, String labContact, String logoPath, String qrData, String labEmail, String labWebsite, String tagline) throws Exception {
        addDefaultHeaderBlock(canvas, pdf, labName, labAddress, labContact, logoPath, qrData, labEmail, labWebsite, tagline);
    }

    private static void addDefaultHeaderBlock(Object document, PdfDocument pdf, String labName, String labAddress, String labContact, String logoPath, String qrData, String labEmail, String labWebsite, String tagline) throws Exception {
        Table header = new Table(UnitValue.createPercentArray(new float[] { 20, 60, 20 })).useAllAvailableWidth();
        
        Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            BarcodeQRCode qrCode = new BarcodeQRCode(qrData != null && !qrData.isEmpty() ? qrData : "LIS");
            qrCell.add(new Image(qrCode.createFormXObject(pdf)).setWidth(70));
        } catch (Exception e) {
            qrCell.add(new Paragraph("QR ID:" + (qrData != null && qrData.length() > 5 ? qrData.substring(0, 5) : "LIS")).setFontSize(6));
        }
        header.addCell(qrCell);
        
        Cell center = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        
        // 1. Lab Name
        center.add(new Paragraph(labName).setBold().setFontSize(18).setMarginBottom(0));
        
        // 2. Tagline (If exists)
        if (tagline != null && !tagline.trim().isEmpty()) {
            center.add(new Paragraph(tagline.toUpperCase()).setFontSize(9f).setMarginBottom(1).setMarginTop(0));
        }
        
        // 3. Address
        if (labAddress != null && !labAddress.trim().isEmpty()) {
            center.add(new Paragraph(labAddress).setFontSize(9).setMarginTop(0).setMarginBottom(0));
        }
        
        // 4. Contact line
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
            center.add(new Paragraph(contactLine.toString()).setBold().setFontSize(8.5f).setMarginTop(0));
        }
        
        header.addCell(center);
        
        if (logoPath != null && !logoPath.trim().isEmpty() && new File(logoPath).isFile()) {
                try {
                        header.addCell(new Cell().add(new Image(ImageDataFactory.create(logoPath)).setWidth(80))
                                         .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                                         .setVerticalAlignment(VerticalAlignment.MIDDLE));
                } catch (Exception e) {
                        header.addCell(new Cell().setBorder(Border.NO_BORDER));
                }
        } else {
                header.addCell(new Cell().setBorder(Border.NO_BORDER));
        }
        
        if (document instanceof Document) {
            ((Document) document).add(header.setMarginBottom(10));
        } else if (document instanceof com.itextpdf.layout.Canvas) {
            ((com.itextpdf.layout.Canvas) document).add(header.setMarginBottom(10));
        }
    }

    public static void addClinicalImages(Document document, TestData td) {
        if (td == null) return;
        
        if (td.referenceImage != null && !td.referenceImage.trim().isEmpty() && new File(td.referenceImage).isFile()) {
            try {
                document.add(new Paragraph("REFERENCE PROTOCOL IMAGE:").setBold().setFontSize(7).setFontColor(ColorConstants.GRAY));
                Image refImg = new Image(ImageDataFactory.create(td.referenceImage)).setMaxWidth(150).setMarginBottom(10);
                document.add(refImg);
            } catch (Exception e) {}
        }

        if (td.resultImages != null && !td.resultImages.isEmpty()) {
            document.add(new Paragraph("CLINICAL IMAGING ATTACHMENTS:").setBold().setFontSize(8).setFontColor(new DeviceRgb(150, 17, 17)));
            
            Table imgTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            for (String imgObj : td.resultImages) {
                String imgPath = imgObj;
                if (imgPath != null && !imgPath.trim().isEmpty() && new File(imgPath).isFile()) {
                    try {
                        Image img = new Image(ImageDataFactory.create(imgPath)).setWidth(UnitValue.createPercentValue(100));
                        Cell cell = new Cell().add(img).setBorder(new SolidBorder(0.5f)).setPadding(5);
                        imgTable.addCell(cell);
                    } catch (Exception e) {}
                }
            }
            if (td.resultImages != null && td.resultImages.size() % 2 != 0) {
                imgTable.addCell(new Cell().setBorder(Border.NO_BORDER));
            }
            document.add(imgTable.setMarginBottom(15));
        }
    }

    public static String generatePreviewFromTD(TestData td) {
        String dir = getReportDir();
        File file = new File(dir, "PREVIEW_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(20, 36, 40, 36);

            // Fetch Lab Settings for Header
            String labName = DatabaseManager.getSetting("lab_name", "Public Health Reference Laboratory");
            String labPhone = DatabaseManager.getSetting("lab_contact", "03165794442");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");

            // Top Header: Visual Identity
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{15, 85})).useAllAvailableWidth().setMarginBottom(10);
            if (logoPath != null && new File(logoPath).exists()) {
                try {
                Image logo = new Image(ImageDataFactory.create(logoPath)).setWidth(50);
                headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
                } catch(Exception e) {}
            } else {
                headerTable.addCell(new Cell().add(new Paragraph("PHRL").setBold().setFontSize(12)).setBorder(Border.NO_BORDER));
            }

            Paragraph lName = new Paragraph(labName.toUpperCase()).setBold().setFontSize(14).setFontColor(new DeviceRgb(26, 10, 10));
            Paragraph lContact = new Paragraph("Contact: " + labPhone + " | Registered Excellence").setFontSize(8).setFontColor(new DeviceRgb(69, 90, 100));
            headerTable.addCell(new Cell().add(lName).add(lContact).setBorder(Border.NO_BORDER).setPaddingLeft(10).setVerticalAlignment(VerticalAlignment.MIDDLE));
            document.add(headerTable);

            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)).setMarginBottom(10));
            document.add(new Paragraph("CLINICAL PREVIEW REPORT").setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(150, 17, 17)));

            // Patient Identity Grid
            Table patientTable = new Table(UnitValue.createPercentArray(new float[]{15, 35, 15, 35})).useAllAvailableWidth().setMarginBottom(15);
            addInfoRow(patientTable, "ID", "PA-PREVIEW", "Age/Sex", "30y/Male");
            addInfoRow(patientTable, "Name", "PREVIEW PATIENT", "Date", new java.text.SimpleDateFormat("dd-MMM-yyyy").format(new java.util.Date()));
            document.add(patientTable);

            // Render Test Content
            float dynFontSize = 8.5f;
            if (td.isCulture == 1) {
                renderCultureSection(document, td, dynFontSize);
            } else if (td.isSpecial == 1) {
                renderSpecialSection(document, td, dynFontSize);
            } else if (td.isMicroscopic == 1) {
                renderMicroscopicSection(document, td, dynFontSize);
            } else {
                renderRoutineSection(document, td, dynFontSize);
            }

            document.add(new Paragraph("\n--- PREVIEW ONLY ---").setFontSize(7).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(189, 189, 189)));
            
            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void addInfoRow(Table table, String l1, String v1, String l2, String v2) {
        table.addCell(new Cell().add(new Paragraph(l1).setBold().setFontSize(8.5f)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(v1).setFontSize(8.5f)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(l2).setBold().setFontSize(8.5f)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(v2).setFontSize(8.5f)).setBorder(Border.NO_BORDER));
    }

    public static void renderSpecialSection(Document doc, TestData td, float fs) {
        doc.add(new Paragraph(td.testName.toUpperCase()).setBold().setFontSize(fs + 1.5f).setMarginBottom(10));
        addResultsTable(doc, td.results, "Result");
    }

    public static void renderMicroscopicSection(Document doc, TestData td, float fs) {
        doc.add(new Paragraph(td.testName.toUpperCase()).setBold().setFontSize(fs + 1.5f).setMarginBottom(10));
        addResultsTable(doc, td.results, "Result");
    }

    public static void renderRoutineSection(Document doc, TestData td, float fs) {
        doc.add(new Paragraph(td.testName.toUpperCase()).setBold().setFontSize(fs + 1.5f).setMarginBottom(10));
        addResultsTable(doc, td.results, "Result");
    }

    public static void renderCultureSection(Document document, TestData td, float dynFontSize) {
        if (td == null || td.results == null) return;

        Table outerContainer = new Table(1).useAllAvailableWidth().setMarginTop(5).setMarginBottom(10);
        Cell outerCell = new Cell().setBorder(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.BLACK, 0.5f)).setPadding(15);

        outerCell.add(new Paragraph("Provisional Report").setFontSize(dynFontSize).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        
        String headerName = td.testName;
        if (!headerName.toLowerCase().contains("microbiology")) {
            headerName = "MICROBIOLOGY | " + headerName.toUpperCase();
        } else {
            headerName = headerName.toUpperCase();
        }
        outerCell.add(new Paragraph(headerName)
                .setBold().setFontSize(dynFontSize + 1)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        Map<String, String> growthResult = null;
        Map<String, String> organismNames = new java.util.LinkedHashMap<>();
        Map<String, Map<String, String>> sensitivityResults = new java.util.LinkedHashMap<>();
        java.util.List<String> organisms = new java.util.ArrayList<>();
        java.util.List<Map<String, String>> nonDrugParams = new java.util.ArrayList<>();

        for (Map<String, String> r : td.results) {
            String name = r.get("name");
            String val = r.get("value");
            String unit = r.get("unit");
            
            if (name == null) name = "-";
            if (val == null) val = "";
            if (unit == null) unit = "";
            
            String trimVal = val.trim();
            // SKIP EMPTY OR PLACEHOLDER PARAMETERS
            if (trimVal.isEmpty() || trimVal.equals("-") || trimVal.equalsIgnoreCase("NILL") || trimVal.equalsIgnoreCase("nil")) continue;

            String upperVal = trimVal.toUpperCase();
            
            // Allow more comprehensive sensitivity and qualitative values

            if (td.selectedOrganism != null && !td.selectedOrganism.trim().isEmpty()) {
                if (!unit.equalsIgnoreCase(td.selectedOrganism.trim())) {
                    // Strictly hide results for OTHER organisms for this organism-specific view
                    continue;
                }
            }

            // Treat any parameter with an associated unit (organism) as a candidate for the sensitivity table
            if (unit != null && !unit.trim().isEmpty() && !name.trim().equalsIgnoreCase(unit.trim())) {
                String cleanUnit = unit.trim().toUpperCase();
                if (!organisms.contains(cleanUnit)) organisms.add(cleanUnit);
                
                String displayVal = trimVal;
                if (upperVal.equals("SENSITIVE") || upperVal.equals("S")) displayVal = "S";
                else if (upperVal.equals("RESISTANCE") || upperVal.equals("R")) displayVal = "R";
                else if (upperVal.equals("INTERMEDIATE") || upperVal.equals("I")) displayVal = "I";
                
                // Allow multiple values (merged) for same drug/organism
                sensitivityResults.computeIfAbsent(name, k -> new java.util.LinkedHashMap<>())
                                  .merge(cleanUnit, displayVal, (old, n) -> old.equals(n) ? old : old + ", " + n);
            } else if (unit != null && !unit.trim().isEmpty() && (name.trim().equalsIgnoreCase(unit.trim()))) {
                organismNames.put(unit.trim().toUpperCase(), val);
            } else if (growthResult == null && (name.toLowerCase().contains("result") || name.toLowerCase().contains("growth") || unit.isEmpty())) {
                // Prioritize td.growthFindings if it exists; otherwise use this parameter as the growth finding
                if (td.growthFindings == null || td.growthFindings.isEmpty() || td.growthFindings.equalsIgnoreCase("N/A")) {
                    growthResult = r;
                }
            } else {
                boolean isCurrentOrg = (td.selectedOrganism != null && unit.equalsIgnoreCase(td.selectedOrganism.trim()));
                if (td.selectedOrganism == null || isCurrentOrg || unit.isEmpty()) {
                    // Filter out redundant 'Result'/'Growth' parameters if we already have a primary growth finding
                    String cleanName = name.toLowerCase();
                    if (!(cleanName.contains("result") || cleanName.contains("finding") || cleanName.contains("growth"))) {
                        nonDrugParams.add(r);
                    } else if (td.growthFindings == null || td.growthFindings.isEmpty() || td.growthFindings.equalsIgnoreCase("N/A")) {
                        // Keep it only if no primary findings exist
                        nonDrugParams.add(r);
                    }
                }
            }
        }

        String resolvedSpecimen = td.specimen;
        // Search results for a more specific Culture Type (stored in 'range' field)
        for (Map<String, String> r : td.results) {
            String cType = r.get("range");
            if (cType != null && !cType.trim().isEmpty() && !cType.equals("-") && !cType.contains("ref")) {
                resolvedSpecimen = cType.trim();
                break;
            }
        }

        Paragraph pSpec = new Paragraph().setFontSize(dynFontSize).setMarginBottom(4);
        pSpec.add(new Text("SPECIMEN TYPE : ").setBold());
        pSpec.add(new Text(td.cultureType != null && !td.cultureType.isEmpty() ? td.cultureType.toUpperCase() : (resolvedSpecimen != null ? resolvedSpecimen.toUpperCase() : "NOT SPECIFIED")));
        outerCell.add(pSpec);

        if (td.selectedOrganism != null && !td.selectedOrganism.trim().isEmpty() && !td.selectedOrganism.equalsIgnoreCase("-")) {
            Paragraph pOrg = new Paragraph().setFontSize(dynFontSize).setMarginBottom(4);
            pOrg.add(new Text("IDENTIFIED BACTERIA : ").setBold());
            pOrg.add(new Text(td.selectedOrganism.toUpperCase()));
            outerCell.add(pOrg);
        }

        if (td.incubationDuration != null && !td.incubationDuration.trim().isEmpty()) {
            Paragraph pDur = new Paragraph().setFontSize(dynFontSize).setMarginBottom(8);
            pDur.add(new Text("INCUBATION DURATION : ").setBold());
            pDur.add(new Text(td.incubationDuration.toUpperCase()));
            outerCell.add(pDur);
        }

        boolean isNegative = "Negative".equalsIgnoreCase(td.growthStatus);
        
        if (td.growthFindings != null && !td.growthFindings.trim().isEmpty() && !td.growthFindings.trim().equalsIgnoreCase("N/A")) {
            Paragraph pGrowth = new Paragraph().setFontSize(dynFontSize).setMarginBottom(8);
            pGrowth.add(new Text("Result : ").setBold());
            pGrowth.add(new Text(extractPlainText(td.growthFindings)));
            outerCell.add(pGrowth);
        } else if (isNegative) {
            Paragraph pGrowth = new Paragraph().setFontSize(dynFontSize).setMarginBottom(8);
            pGrowth.add(new Text("Result : ").setBold());
            // Safe fallback if for some reason growthFindings was not populated
            String negText = "No growth after 5 days of incubation.";
            pGrowth.add(new Text(negText));
            outerCell.add(pGrowth);
        } else if (growthResult != null || !organisms.isEmpty()) {
            Paragraph pGrowth = new Paragraph().setFontSize(dynFontSize).setMarginBottom(8);
            pGrowth.add(new Text("Result : ").setBold());
            String cleanVal = extractPlainText(growthResult != null ? growthResult.getOrDefault("value", "") : "");
            
            // Backup Search: If td.growthFindings is missing, look for a specific 'Growth findings' parameter in the list
            if (cleanVal.isEmpty() && td.growthFindings == null) {
                for (Map<String, String> r : td.results) {
                    String rName = r.getOrDefault("name", "").toLowerCase();
                    if (rName.contains("growth") && rName.contains("finding")) {
                        cleanVal = extractPlainText(r.getOrDefault("value", ""));
                        break;
                    }
                }
            }

            if (cleanVal.isEmpty() && !organisms.isEmpty()) {
                String orgToUse = (td.selectedOrganism != null && !td.selectedOrganism.isEmpty()) ? td.selectedOrganism.trim() : String.join(" and ", organisms);
                String rawNotes = (td.notes != null && !td.notes.trim().isEmpty()) ? td.notes : "5 days";
                String cleanNotes = extractPlainText(rawNotes);
                cleanVal = "The culture yielded growth of " + orgToUse + " after incubation for " + cleanNotes + " under standard conditions.";
            } else if (cleanVal.isEmpty()) {
                cleanVal = "Positive growth observed.";
            }
            pGrowth.add(new Text(cleanVal));
            outerCell.add(pGrowth);
        }

        if (!nonDrugParams.isEmpty()) {
            boolean hasValidParams = false;
            Table paramTable = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth().setMarginBottom(10);
            for (Map<String, String> row : nonDrugParams) {
                String pName = extractPlainText(row.getOrDefault("name", ""));
                String pUnit = extractPlainText(row.getOrDefault("unit", ""));
                String pVal = extractPlainText(row.getOrDefault("value", "")).trim();
                
                if (pVal.isEmpty() || pVal.equalsIgnoreCase("NILL") || pVal.equalsIgnoreCase("-")) continue;
                
                hasValidParams = true;
                paramTable.addCell(new Cell().add(new Paragraph(pName)).setBorder(Border.NO_BORDER).setFontSize(dynFontSize));
                paramTable.addCell(new Cell().add(new Paragraph(pVal + (pUnit.isEmpty() ? "" : " " + pUnit))).setBorder(Border.NO_BORDER).setFontSize(dynFontSize));
            }
            if (hasValidParams) outerCell.add(paramTable);
        }

        if (!isNegative) {
            // Identify drug results and organisms
            java.util.List<Map<String, String>> drugResults = new java.util.ArrayList<>();
            for (Map<String, String> r : td.results) {
                String name = r.get("name");
                String unit = r.get("unit");
                if (name == null || unit == null || unit.trim().isEmpty() || name.trim().equalsIgnoreCase(unit.trim())) continue;
                
                // Exclude growth results
                if (name.toLowerCase().contains("result") || name.toLowerCase().contains("growth") || name.toLowerCase().contains("finding")) continue;

                // Add to drug list if it belongs to selected organism (if any)
                if (td.selectedOrganism == null || td.selectedOrganism.trim().isEmpty() || unit.equalsIgnoreCase(td.selectedOrganism.trim())) {
                    drugResults.add(r);
                    String cleanUnit = unit.trim().toUpperCase();
                    if (!organisms.contains(cleanUnit)) organisms.add(cleanUnit);
                }
            }

            if (!drugResults.isEmpty()) {
                Table sensTitleTable = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth().setMarginBottom(5).setMarginTop(8);
                sensTitleTable.addCell(new Cell().add(new Paragraph("ANTIMICROBIAL SENSITIVITY:").setBold().setFontSize(dynFontSize + 0.5f)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.BOTTOM));
                
                StringBuilder orgsTitle = new StringBuilder();
                for (int i=0; i<organisms.size(); i++) {
                    String unit = organisms.get(i);
                    String oName = organismNames.getOrDefault(unit, "");
                    orgsTitle.append(unit).append(": ").append(oName);
                    if (i < organisms.size() - 1) orgsTitle.append(" | ");
                }
                sensTitleTable.addCell(new Cell().add(new Paragraph(orgsTitle.toString()).setItalic().setFontSize(dynFontSize)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.BOTTOM));
                outerCell.add(sensTitleTable);

                Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
                DeviceRgb borderColor = new DeviceRgb(207, 216, 220);
                DeviceRgb headerBgColor = new DeviceRgb(248, 249, 250);
                
                table.addHeaderCell(new Cell().add(new Paragraph("Drugs")).setBold().setFontSize(dynFontSize + 0.5f).setBackgroundColor(headerBgColor).setPadding(6).setBorder(new com.itextpdf.layout.borders.SolidBorder(borderColor, 1f)));
                table.addHeaderCell(new Cell().add(new Paragraph("Sensitivity")).setBold().setFontSize(dynFontSize + 0.5f).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(headerBgColor).setPadding(6).setBorder(new com.itextpdf.layout.borders.SolidBorder(borderColor, 1f)));

                for (Map<String, String> dr : drugResults) {
                    String dName = dr.getOrDefault("name", "-");
                    String dVal = dr.getOrDefault("value", "");
                    String upperVal = dVal.toUpperCase();
                    
                    String displayVal = dVal;
                    if (upperVal.equals("SENSITIVE") || upperVal.equals("S")) displayVal = "S";
                    else if (upperVal.equals("RESISTANCE") || upperVal.equals("R")) displayVal = "R";
                    else if (upperVal.equals("INTERMEDIATE") || upperVal.equals("I")) displayVal = "I";

                    table.addCell(new Cell().add(new Paragraph(dName)).setFontSize(dynFontSize).setPadding(6).setPaddingTop(4).setPaddingBottom(4).setBorder(new com.itextpdf.layout.borders.SolidBorder(borderColor, 1f)));
                    table.addCell(new Cell().add(new Paragraph(displayVal)).setTextAlignment(TextAlignment.CENTER).setFontSize(dynFontSize).setPadding(6).setPaddingTop(4).setPaddingBottom(4).setBorder(new com.itextpdf.layout.borders.SolidBorder(borderColor, 1f)));
                }
                outerCell.add(table.setMarginBottom(4));
                outerCell.add(new Paragraph("I = Intermediate , R = Resistance , S = Sensitivity").setFontSize(dynFontSize - 1.5f).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(10));
            }
        }

        if (td.resultComment != null && !td.resultComment.trim().isEmpty()) {
            String cleanOpinion = extractPlainText(td.resultComment.trim());
            if (!cleanOpinion.isEmpty()) {
                outerCell.add(new Paragraph("Opinion :").setBold().setFontSize(dynFontSize).setMarginBottom(2));
                outerCell.add(new Paragraph(cleanOpinion).setFontSize(dynFontSize).setMarginBottom(6));
            }
        }

        if (td.notes != null && !td.notes.trim().isEmpty()) {
            String cleanComments = extractPlainText(td.notes.trim());
            if (!cleanComments.isEmpty()) {
                outerCell.add(new Paragraph("Comments :").setBold().setFontSize(dynFontSize).setMarginBottom(2).setMarginTop(8));
                outerCell.add(new Paragraph(cleanComments).setFontSize(dynFontSize).setMarginBottom(6));
            }
        }

        outerCell.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(10).setMarginBottom(5));
        
        String bookingTime = "N/A";
        String resultTime = "N/A";
        if (td.sampleId != null && !td.sampleId.isEmpty()) {
            try (java.sql.Connection conn = com.lab.lms.dao.DatabaseManager.getConnection()) {
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT collection_date FROM samples WHERE sample_id = ? LIMIT 1")) {
                    pstmt.setString(1, td.sampleId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getString(1) != null) bookingTime = rs.getString(1);
                    }
                }
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT MAX(completed_at) FROM results WHERE sample_id = ?")) {
                    pstmt.setString(1, td.sampleId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getString(1) != null) resultTime = rs.getString(1);
                    }
                }
            } catch (Exception e) {}
        }
        
        java.text.SimpleDateFormat dbFmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dbFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        java.text.SimpleDateFormat outFmt = new java.text.SimpleDateFormat("MMM dd, hh:mm a");
        outFmt.setTimeZone(TimeZone.getDefault());
        
        Date now = new Date();
        try { if (!"N/A".equals(bookingTime)) bookingTime = outFmt.format(dbFmt.parse(bookingTime)); } catch(Exception e){}
        try { 
            if (!"N/A".equals(resultTime)) {
                resultTime = outFmt.format(dbFmt.parse(resultTime));
            } else {
                resultTime = outFmt.format(now);
            }
        } catch(Exception e){
            resultTime = outFmt.format(now);
        }

        Paragraph pFooter = new Paragraph()
            .add(new Text("Booking: ").setBold())
            .add(new Text(bookingTime + "   "))
            .add(new Text("Result Processed: ").setBold())
            .add(new Text(resultTime))
            .setFontSize(dynFontSize - 1);
        outerCell.add(pFooter);

        outerContainer.addCell(outerCell);
        document.add(outerContainer);
    }



        public static void addResultsTable(Document document, List<Map<String, String>> rows, String caseHeader) {
                Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 25, 15, 30 }))
                                .useAllAvailableWidth();
                table.addHeaderCell(new Cell().add(new Paragraph("Test")).setBold().setFontSize(9)
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5));
                table.addHeaderCell(new Cell().add(new Paragraph("Normal Value")).setBold().setFontSize(9)
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));
                table.addHeaderCell(new Cell().add(new Paragraph("Unit")).setBold().setFontSize(9)
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));
                table.addHeaderCell(new Cell().add(new Paragraph(caseHeader)).setBold().setFontSize(7)
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));

                for (Map<String, String> row : rows) {
                        String name = row.get("name");
                        table.addCell(new Cell().add(new Paragraph(name == null ? "" : name)).setFontSize(9)
                                        .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5));
                        
                        String range = row.get("range");
                        table.addCell(new Cell().add(new Paragraph(range != null && !range.equals("null") ? range : "-")).setFontSize(9)
                                        .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));
                        
                        String unit = row.get("unit");
                        table.addCell(new Cell().add(new Paragraph(unit != null && !unit.equals("null") ? unit : "-")).setFontSize(9)
                                        .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));
                        
                        String val = row.get("value");
                        val = (val == null) ? "" : val;
                        
                        Paragraph pVal = new Paragraph(val).setBold().setFontSize(9);
                        if ("ABNORMAL".equals(row.get("status"))) {
                                pVal.setFontColor(new DeviceRgb(255, 0, 0));
                        }
                        
                        table.addCell(new Cell().add(pVal).setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f)).setPadding(5).setTextAlignment(TextAlignment.CENTER));
                }
                document.add(table.setMarginBottom(10));
        }

    public static String regenerateLatestReceipt(String patientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT p.*, i.* FROM patients p JOIN invoices i ON p.patient_id = i.patient_id " +
                         "WHERE p.patient_id = ? ORDER BY i.id DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, patientId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String patName = rs.getString("name");
                        String ageVal = (rs.getString("age") == null ? "0" : rs.getString("age"));
                        String genderVal = rs.getString("gender");
                        String phoneVal = (rs.getString("phone") != null ? rs.getString("phone") : rs.getString("whatsapp"));
                        String refVal = rs.getString("referred_doctor");
                        double total = rs.getDouble("total_amount");
                        double discount = rs.getDouble("discount");
                        double finalAmount = rs.getDouble("final_amount");
                        double paid = rs.getDouble("paid_amount");
                        double due = rs.getDouble("due_amount");
                        String collectionTime = rs.getString("date");

                        // Identify tests for the current session (within 2 hours of invoice timestamp)
                        String testSql = "SELECT DISTINCT t.* FROM tests t " +
                                         "JOIN test_parameters tp ON t.id = tp.test_id " +
                                         "JOIN results r ON tp.id = r.parameter_id " +
                                         "JOIN samples s ON r.sample_id = s.sample_id " +
                                         "WHERE s.patient_id = ? AND s.collection_date >= DATETIME(?, '-2 hours') " +
                                         "ORDER BY t.name";
                        
                        List<Test> tests = new ArrayList<>();
                        try (PreparedStatement tPstmt = conn.prepareStatement(testSql)) {
                            tPstmt.setString(1, patientId);
                            tPstmt.setString(2, collectionTime);
                            try (ResultSet tRs = tPstmt.executeQuery()) {
                                while (tRs.next()) {
                                    tests.add(new Test(
                                        tRs.getInt("id"),
                                        tRs.getString("numeric_code"),
                                        tRs.getString("alpha_code"),
                                        tRs.getString("name"),
                                        tRs.getString("category"),
                                        tRs.getDouble("price"),
                                        tRs.getString("result_time"),
                                        tRs.getString("notes"),
                                        tRs.getInt("is_special"),
                                        tRs.getInt("is_microscopic"),
                                        tRs.getInt("is_culture"),
                                        tRs.getString("specimen"),
                                        tRs.getString("image_path"),
                                        tRs.getString("protocol_class"),
                                        tRs.getString("container"),
                                        tRs.getString("volume"),
                                        tRs.getString("fasting")
                                    ));
                                }
                            }
                        }

                        if (tests.isEmpty()) {
                             String altSql = "SELECT DISTINCT t.* FROM tests t " +
                                              "JOIN test_parameters tp ON t.id = tp.test_id " +
                                              "JOIN results r ON tp.id = r.parameter_id " +
                                              "JOIN samples s ON r.sample_id = s.sample_id " +
                                              "WHERE s.patient_id = ? " +
                                              "ORDER BY s.id DESC LIMIT 10";
                             try (PreparedStatement altPstmt = conn.prepareStatement(altSql)) {
                                 altPstmt.setString(1, patientId);
                                 try (ResultSet tRs = altPstmt.executeQuery()) {
                                     while (tRs.next()) {
                                         tests.add(new Test(tRs.getInt("id"), tRs.getString("numeric_code"), tRs.getString("alpha_code"), tRs.getString("name"), tRs.getString("category"), tRs.getDouble("price"), tRs.getString("result_time"), tRs.getString("notes"), tRs.getInt("is_special"), tRs.getInt("is_microscopic"), tRs.getInt("is_culture"), tRs.getString("specimen"), tRs.getString("image_path"), tRs.getString("protocol_class"), tRs.getString("container"), tRs.getString("volume"), tRs.getString("fasting")));
                                     }
                                 }
                             }
                        }

                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");
                        java.util.Date drawDate = new java.util.Date();
                        try { drawDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(collectionTime); } catch(Exception e){}
                        String fmtCollection = sdf.format(drawDate);
                        String reportingTime = getClinicalOffsetTime(fmtCollection, 1440);

                        String newPath = generateReceipt(patName, patientId, ageVal, genderVal, phoneVal, refVal,
                                                         fmtCollection, reportingTime, tests,
                                                         total, discount, finalAmount, paid, due);
                        
                        if (newPath != null) {
                            String updateSql = "UPDATE invoices SET receipt_path = ? WHERE id = (SELECT id FROM invoices WHERE patient_id = ? ORDER BY id DESC LIMIT 1)";
                            try (PreparedStatement uPstmt = conn.prepareStatement(updateSql)) {
                                uPstmt.setString(1, newPath);
                                uPstmt.setString(2, patientId);
                                uPstmt.executeUpdate();
                            }
                        }
                        return newPath;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

        public static String generateReceipt(String patientName, String patientId, String age, String gender, 
                        String phone, String referredBy, String collectionTime, String reportingTime, 
                        List<Test> tests, double total, double discount, double finalAmount, double paid, double due) {
                
                // Fetch branding ONCE from DB before starting generation to avoid nested locks
                com.lab.lms.models.LabBranding branding = new com.lab.lms.models.LabBranding(
                        DatabaseManager.getSetting("lab_name", "Laboratory Information System"),
                        DatabaseManager.getSetting("lab_address", ""),
                        DatabaseManager.getSetting("lab_contact", ""),
                        DatabaseManager.getSetting("lab_email", ""),
                        DatabaseManager.getSetting("lab_website", ""),
                        DatabaseManager.getSetting("lab_tagline", ""),
                        DatabaseManager.getSetting("lab_logo", ""),
                        DatabaseManager.getSetting("receipt_policies", "1. Standard Clinical Receipt\n2. Please carry this for collection.")
                );
                
                return generateReceipt(patientName, patientId, age, gender, phone, referredBy, collectionTime, reportingTime, 
                                     tests, total, discount, finalAmount, paid, due, branding);
        }

        public static String generateReceipt(String patientName, String patientId, String age, String gender, 
                        String phone, String referredBy, String collectionTime, String reportingTime, 
                        List<Test> tests, double total, double discount, double finalAmount, double paid, double due,
                        com.lab.lms.models.LabBranding branding) {
                
                String receiptDir = getReceiptDir();
                File receiptFile = new File(receiptDir, "INV_" + patientId + "_" + System.currentTimeMillis() + ".pdf");
                String dest = receiptFile.getAbsolutePath();
                String trNo = patientId + "-" + (System.currentTimeMillis() / 1000);

                System.out.println("[TRACE] ReportGenerator: Starting PDF Generation for " + patientId);

                try {
                    float dynamicSize = 560 + (tests.size() * 22);
                    if (branding.policies != null && !branding.policies.isEmpty()) {
                        dynamicSize += branding.policies.split("\n").length * 10;
                    }
                    
                    PageSize receiptSize = new PageSize(226, Math.max(dynamicSize, 520)); 
                    PdfWriter writer = new PdfWriter(dest);
                    PdfDocument pdf = new PdfDocument(writer);
                    pdf.setDefaultPageSize(receiptSize);

                    Document document = new Document(pdf);
                    document.setMargins(10, 12, 5, 12);
                    document.setFontSize(7);

                    if (branding.logoPath != null && !branding.logoPath.trim().isEmpty() && new File(branding.logoPath).exists()) {
                        try {
                            System.out.println("[TRACE] ReportGenerator: Loading Logo: " + branding.logoPath);
                            Image logo = new Image(ImageDataFactory.create(branding.logoPath)).setWidth(35);
                            document.add(logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));
                            System.out.println("[TRACE] ReportGenerator: Logo Loaded.");
                        } catch (Exception e) {
                            System.err.println("[RECEIPT-ERROR] Logo load failed: " + e.getMessage());
                        }
                    }

                    document.add(new Paragraph(branding.name).setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                    if (branding.tagline != null && !branding.tagline.trim().isEmpty()) {
                        document.add(new Paragraph(branding.tagline.toUpperCase()).setFontSize(6.5f).setItalic().setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
                    }
                    if (branding.address != null && !branding.address.trim().isEmpty()) {
                        document.add(new Paragraph(branding.address).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0).setMarginBottom(0));
                    }
                    
                    StringBuilder contactBar = new StringBuilder();
                    if (branding.contact != null && !branding.contact.isEmpty()) contactBar.append(branding.contact);
                    if (branding.email != null && !branding.email.isEmpty()) {
                        if (contactBar.length() > 0) contactBar.append(" | ");
                        contactBar.append(branding.email.toLowerCase());
                    }
                    if (branding.website != null && !branding.website.isEmpty()) {
                        if (contactBar.length() > 0) contactBar.append(" | ");
                        contactBar.append(branding.website.toLowerCase());
                    }
                    
                    if (contactBar.length() > 0) {
                        document.add(new Paragraph(contactBar.toString()).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0));
                    }

                    document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(2).setMarginBottom(4));
                    document.add(new Paragraph("CASH RECEIPT").setBold().setTextAlignment(TextAlignment.CENTER).setFontSize(8));

                    Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 35, 65 })).useAllAvailableWidth();
                    infoTable.setMarginTop(5);
                    
                    String dispG = (gender == null || gender.equalsIgnoreCase("null") || gender.isEmpty()) ? "N/A" : gender;
                    String dispR = (referredBy == null || referredBy.isEmpty() || referredBy.equalsIgnoreCase("null")) ? "Self" : referredBy;
                    SolidBorder gridBorder = new SolidBorder(ColorConstants.BLACK, 0.5f);
                    
                    addBorderedRow(infoTable, "Patient ID", patientId, gridBorder);
                    addBorderedRow(infoTable, "Name", patientName.toUpperCase(), gridBorder);
                    addBorderedRow(infoTable, "Age/Sex", age + " (Y) / " + dispG, gridBorder);
                    addBorderedRow(infoTable, "Collection", collectionTime, gridBorder);
                    addBorderedRow(infoTable, "Reporting", reportingTime, gridBorder);
                    addBorderedRow(infoTable, "Phone", phone, gridBorder);
                    addBorderedRow(infoTable, "Referred By", dispR, gridBorder);
                    document.add(infoTable.setMarginBottom(10));
                    System.out.println("[TRACE] ReportGenerator: Metadata Rendered.");

                    document.add(new Paragraph("DIAGNOSTIC SERVICES").setBold().setFontSize(7).setMarginBottom(2));
                    Table testTable = new Table(UnitValue.createPercentArray(new float[] { 8, 52, 25, 15 })).useAllAvailableWidth();
                    testTable.addHeaderCell(new Cell().add(new Paragraph("S.No").setBold()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                    testTable.addHeaderCell(new Cell().add(new Paragraph("Diagnostic Service").setBold()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                    testTable.addHeaderCell(new Cell().add(new Paragraph("Expected Time").setBold()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.5f)));
                    testTable.addHeaderCell(new Cell().add(new Paragraph("Price").setBold()).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.5f)));

                    int sno = 1;
                    for (Test t : tests) {
                            testTable.addCell(new Cell().add(new Paragraph(String.valueOf(sno++))).setBorder(Border.NO_BORDER));
                            testTable.addCell(new Cell().add(new Paragraph(t.toString().toUpperCase())).setBorder(Border.NO_BORDER));
                            testTable.addCell(new Cell().add(new Paragraph(t.getResultTime() == null ? "Standard" : t.getResultTime())).setBorder(Border.NO_BORDER));
                            testTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", t.getPrice()))).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));
                    }
                    document.add(testTable.setMarginBottom(5));
                    System.out.println("[TRACE] ReportGenerator: Tests Rendered.");
                    
                    document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(2).setMarginBottom(2));
                    
                    Table totTable = new Table(UnitValue.createPercentArray(new float[] { 65, 35 })).useAllAvailableWidth();
                    addSummaryRow(totTable, "TOTAL AMOUNT:", String.format("%.2f", total), false);
                    addSummaryRow(totTable, "DISCOUNT AMOUNT:", String.format("%.2f", discount), false);
                    addSummaryRow(totTable, "PAYABLE NET:", String.format("%.2f", finalAmount), true);
                    addSummaryRow(totTable, "PAID AMOUNT:", String.format("%.2f", paid), false);
                    addSummaryRow(totTable, "DUE BALANCE:", String.format("%.2f", due), true);
                    document.add(totTable);
                    System.out.println("[TRACE] ReportGenerator: Financials Rendered.");
                    
                        document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8).setMarginBottom(4));
                        document.add(new Paragraph("TERMS & POLICIES").setBold().setFontSize(6).setMarginBottom(2));
                        document.add(new Paragraph(branding.policies).setFontSize(5.5f).setItalic().setMarginTop(0));

                        document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(10));
                        
                        String qrData = "--- MSF CLINICAL VERIFICATION ---\n" +
                                        "T/R ID: " + trNo + "\n" +
                                        "Patient: " + patientName.toUpperCase() + " (" + patientId + ")\n" +
                                        "Age/Sex: " + age + " (Y) / " + dispG + "\n" +
                                        "Referred By: " + dispR + "\n" +
                                        "Reporting: " + reportingTime + "\n" +
                                        "Amount: PKR " + String.format("%.2f", finalAmount) + "\n" +
                                        "Status: OFFICIALLY VERIFIED";
                        
                        System.out.println("[TRACE] ReportGenerator: Constructing Verification QR...");
                        BarcodeQRCode qr = new BarcodeQRCode(qrData);
                        Image qrImage = new Image(qr.createFormXObject(pdf)).setWidth(50);
                        
                        document.add(new Paragraph("SCAN ME TO VERIFY").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(5).setMarginBottom(-2));
                        document.add(new Paragraph().add(qrImage).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));
                        System.out.println("[TRACE] ReportGenerator: Verification QR Added.");

                        document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(5));
                        document.add(new Paragraph("THANK YOU").setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(-5));
                        document.add(new Paragraph("FOR VISITING OUR LABORATORY").setFontSize(7).setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

                        document.add(new Paragraph("Proudly Developed by Team MSF PVT LTD +923165794442").setFontSize(5).setTextAlignment(TextAlignment.CENTER).setItalic());

                        document.close();
                        System.out.println("[TRACE] ReportGenerator: PDF Finalized Successfully.");
                        return dest;
                } catch (Exception e) {
                        System.err.println("[RECEIPT-FATAL] Generation Crash: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }

        public static void addBorderedRow(Table table, String label, String value, SolidBorder border) {
                table.addCell(new Cell().add(new Paragraph(label)).setBorder(border).setBold().setPaddingLeft(5));
                table.addCell(new Cell().add(new Paragraph(value == null ? "" : value)).setBorder(border).setPaddingLeft(5));
        }

        public static void addReceiptRow(Table table, String label, String value) {
                table.addCell(new Cell().add(new Paragraph(label)).setBorder(Border.NO_BORDER).setBold());
                table.addCell(new Cell().add(new Paragraph(value == null ? "" : value)).setBorder(Border.NO_BORDER));
        }

        public static void addSummaryRow(Table table, String label, String value, boolean bold) {
                Paragraph lP = new Paragraph(label);
                Paragraph vP = new Paragraph(value);
                if (bold) { lP.setBold(); vP.setBold(); }
                table.addCell(new Cell().add(lP).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(vP).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        }

        public static String generatePreview(String testName, int isSpecial, int isMicroscopic, int isCulture, String specimen, String notes, List<Map<String, String>> params) {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, "PREVIEW_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);
                        document.setMargins(20, 36, getFooterMargin(), 36);

                        // Dynamic Preview Logic: Resolve Title for Single-Parameter Protocols
                        String previewTitle = testName;
                        if (params != null && params.size() == 1) {
                            previewTitle = params.get(0).getOrDefault("name", testName);
                        }

                        TestData td = new TestData(previewTitle, 0, params, notes, isSpecial, isMicroscopic, isCulture, specimen, "", "");
                        
                        String date = new java.text.SimpleDateFormat("dd-MMM-yyyy").format(new java.util.Date());
                        ClinicalTemplateFactory.getTemplate().apply(document, pdf, "PREVIEW PATIENT", "P-0000", "30", "Male", "Self", date,
                                    previewTitle, "03XX-XXXXXXX", "Laboratory Preview", java.util.Collections.singletonList(td), false, true, true, date);

                        document.close();
                        return dest;
                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        public static String generateEmptyTemplate() {
                String reportDir = getReportDir();
                File reportFile = new File(reportDir, "EMPTY_TEMPLATE_" + System.currentTimeMillis() + ".pdf");
                String dest = reportFile.getAbsolutePath();

                try {
                        PdfWriter writer = new PdfWriter(dest);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);
                        document.setMargins(20, 36, getFooterMargin(), 36);

                        ClinicalTemplateFactory.getTemplate().apply(document, pdf, "", "", "", "", "", "", "Diagnostic", "", "", null, true, true, true, "");
                        document.close();
                        return dest;
                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        private static void addStyledText(Paragraph p, String content) {
                if (content == null || content.trim().isEmpty()) return;
                
                String processed = content;
                
                // 1. Intelligent Sanitization: Isolate clinical payload from editor boilerplates
                if (content.toLowerCase().contains("<html") || content.toLowerCase().contains("<body")) {
                    // Forcefully strip Head/Style blocks (and their code content)
                    processed = processed.replaceAll("(?is)<head[^>]*>.*?</head>", "");
                    processed = processed.replaceAll("(?is)<style[^>]*>.*?</style>", "");
                    processed = processed.replaceAll("(?is)<script[^>]*>.*?</script>", "");

                    java.util.regex.Matcher bodyMatcher = java.util.regex.Pattern.compile("(?is)<body[^>]*>(.*?)</body>").matcher(processed);
                    if (bodyMatcher.find()) {
                        processed = bodyMatcher.group(1);
                    } else {
                        // Fallback: If no body found but its HTML, strip all remaining structural tags
                        processed = processed.replaceAll("(?is)<html[^>]*>", "").replaceAll("(?is)</html>", "");
                    }
                }
                
                // 2. Block Conversion: Structural tags to line breaks
                processed = processed.replaceAll("(?i)<br\\s*/?>", "\n")
                                     .replaceAll("(?i)</li>", "\n")
                                     .replaceAll("(?i)</p>|</div>", "\n\n");
                
                // 3. Selective Tag Stripping: Remove everything EXACT for basic styling [b, i, u]
                processed = processed.replaceAll("(?i)<(?!/?([biu])\\b)[^>]*>", "");
                
                // 4. Entity Protocol: Decode common HTML characters
                processed = processed.replace("&nbsp;", " ")
                                     .replace("&amp;", "&")
                                     .replace("&lt;", "<")
                                     .replace("&gt;", ">")
                                     .replace("&quot;", "\"")
                                     .replace("&#39;", "'")
                                     .trim();

                if (processed.isEmpty()) return;

                if (processed.isEmpty()) return;
                
                // 5. High-Fidelity Styling Parser (Handles <b>, <i>, <u> nesting)
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)<([/]?[biu])>");
                java.util.regex.Matcher matcher = pattern.matcher(processed);
                
                int lastEnd = 0;
                boolean bold = false, italic = false, underline = false;
                
                while (matcher.find()) {
                    String textContent = processed.substring(lastEnd, matcher.start());
                    if (!textContent.isEmpty()) {
                        Text text = new Text(textContent).setFontColor(new DeviceRgb(26, 35, 126));
                        if (bold) text.setBold();
                        if (italic) text.setItalic();
                        if (underline) text.setUnderline();
                        p.add(text);
                    }
                    
                    String tag = matcher.group(1).toLowerCase();
                    if (tag.equals("b")) bold = true;
                    else if (tag.equals("/b")) bold = false;
                    else if (tag.equals("i")) italic = true;
                    else if (tag.equals("/i")) italic = false;
                    else if (tag.equals("u")) underline = true;
                    else if (tag.equals("/u")) underline = false;
                    
                    lastEnd = matcher.end();
                }
                
                if (lastEnd < processed.length()) {
                    Text text = new Text(processed.substring(lastEnd)).setFontColor(new DeviceRgb(26, 35, 126));
                    if (bold) text.setBold();
                    if (italic) text.setItalic();
                    if (underline) text.setUnderline();
                    p.add(text);
                }
        }

        public static String extractPlainText(String html) {
                if (html == null || html.trim().isEmpty()) return "";
                
                String processed = html;
                if (html.toLowerCase().contains("<html") || html.toLowerCase().contains("<body")) {
                    // Fully strip Head/Style blocks to prevent CSS content leakage
                    processed = processed.replaceAll("(?is)<head[^>]*>.*?</head>", "");
                    processed = processed.replaceAll("(?is)<style[^>]*>.*?</style>", "");
                    processed = processed.replaceAll("(?is)<script[^>]*>.*?</script>", "");

                    java.util.regex.Matcher bodyMatcher = java.util.regex.Pattern.compile("(?is)<body[^>]*>(.*?)</body>").matcher(processed);
                    if (bodyMatcher.find()) {
                        processed = bodyMatcher.group(1);
                    } else {
                        processed = processed.replaceAll("(?is)<html[^>]*>", "").replaceAll("(?is)</html>", "");
                    }
                }
                
                // Convert breaks to newlines before stripping all tags
                processed = processed.replaceAll("(?i)<br\\s*/?>", "\n");
                processed = processed.replaceAll("(?i)</p>|</div>|</li>", "\n");

                // Final strip of all remaining tags
                processed = processed.replaceAll("(?is)<[^>]*>", "");
                
                // Decode common entities
                processed = processed.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").trim();
                return processed;
        }

        public static String generateQRString(String lab, String name, String id, String age, String gender, String date, List<TestData> tests) {
                StringBuilder sb = new StringBuilder();
                sb.append("LAB: ").append(lab).append("\n");
                sb.append("PATIENT ID: ").append(id).append("\n");
                sb.append("NAME: ").append(name).append("\n");
                
                sb.append("TESTS: ");
                if (tests != null) {
                    for (int i = 0; i < tests.size(); i++) {
                        sb.append(tests.get(i).testName);
                        if (i < tests.size() - 1) sb.append(", ");
                    }
                }
                sb.append("\n");
                
                sb.append("REPORT DATE: ").append(new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new java.util.Date())).append("\n");
                sb.append("STATUS: COMPLETED\n");
                
                String addr = DatabaseManager.getSetting("lab_address", "");
                if (addr != null && !addr.trim().isEmpty()) {
                    sb.append("LAB ADDRESS: ").append(addr.trim()).append("\n");
                }
                
                String phone = DatabaseManager.getSetting("lab_contact", "");
                if (phone != null && !phone.trim().isEmpty()) {
                    String cleanPhone = phone.replaceAll("[^0-9]", "");
                    if (cleanPhone.startsWith("0")) {
                        cleanPhone = "92" + cleanPhone.substring(1);
                    } else if (cleanPhone.length() == 10 && (cleanPhone.startsWith("3") || cleanPhone.startsWith("4"))) {
                        cleanPhone = "92" + cleanPhone;
                    }
                    sb.append("WHATSAPP: https://wa.me/").append(cleanPhone).append("\n");
                }
                
                sb.append("--- VERIFIED BY LMS ---");
                return sb.toString();
        }



    public static void renderClinicalNotes(Document doc, String html) {
        if (html == null || html.trim().isEmpty()) return;
        
        // 1. Advanced Document Sanitization (Isolates clinical payloads from HTML boilerplate)
        String content = html;
        if (html.toLowerCase().contains("<html") || html.toLowerCase().contains("<head")) {
            // Strip Head/Style blocks first (prevents CSS leaking as text)
            content = content.replaceAll("(?is)<head[^>]*>.*?</head>", "");
            content = content.replaceAll("(?is)<style[^>]*>.*?</style>", "");
            
            // Isolate Body content
            java.util.regex.Matcher bodyMatcher = java.util.regex.Pattern.compile("(?is)<body[^>]*>(.*?)</body>").matcher(content);
            if (bodyMatcher.find()) {
                content = bodyMatcher.group(1);
            } else {
                // If no body found but its partial HTML, strip all remaining structural tags
                content = content.replaceAll("(?is)<html[^>]*>", "").replaceAll("(?is)</html>", "");
            }
        }
        
        // Final Hygiene: Normalize entities and normalize breaks
        content = content.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        content = content.replaceAll("(?i)<br\\s*/?>", "\n");
        content = content.replaceAll("(?i)</p>|</div>|</li>", "\n");

        // 2. High-Fidelity Multi-Pass Parser (Interprets local Table structures)
        java.util.regex.Pattern tablePattern = java.util.regex.Pattern.compile("(?is)<table[^>]*>(.*?)</table>");
        java.util.regex.Matcher tableFinder = tablePattern.matcher(content);
        
        int lastPos = 0;
        while (tableFinder.find()) {
            // Add preceding text as a paragraph
            String preceding = content.substring(lastPos, tableFinder.start()).trim();
            if (!preceding.isEmpty()) {
                Paragraph p = new Paragraph().setFontSize(8).setFontColor(new DeviceRgb(26, 35, 126));
                addStyledText(p, preceding);
                doc.add(p);
            }
            
            // Render the table structure directly as an iText Table
            renderHtmlTable(doc, tableFinder.group(1));
            lastPos = tableFinder.end();
        }
        
        // Add final remaining text
        String remaining = content.substring(lastPos).trim();
        if (!remaining.isEmpty()) {
            Paragraph p = new Paragraph().setFontSize(8).setFontColor(new DeviceRgb(26, 35, 126));
            addStyledText(p, remaining);
            doc.add(p);
        }
    }

    private static void renderHtmlTable(Document doc, String tableInnerHtml) {
        // Find rows within the table
        java.util.regex.Pattern trPattern = java.util.regex.Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>");
        java.util.regex.Matcher trFinder = trPattern.matcher(tableInnerHtml);
        
        java.util.List<java.util.List<String>> data = new java.util.ArrayList<>();
        int maxCols = 0;
        
        while (trFinder.find()) {
            java.util.List<String> row = new java.util.ArrayList<>();
            // Find all td or th cells in this row
            java.util.regex.Pattern tdPattern = java.util.regex.Pattern.compile("(?is)<(td|th)[^>]*>(.*?)</\\1>");
            java.util.regex.Matcher tdFinder = tdPattern.matcher(trFinder.group(1));
            while (tdFinder.find()) {
                String cellContent = tdFinder.group(2).replaceAll("<[^>]*>", "").trim();
                row.add(cellContent);
            }
            if (row.size() > maxCols) maxCols = row.size();
            if (!row.isEmpty()) data.add(row);
        }
        
        if (maxCols == 0) return;
        
        // Build iText Table with structured 'Professional Navy' clinical style
        Table t = new Table(maxCols).useAllAvailableWidth().setMarginBottom(10).setMarginTop(8);
        DeviceRgb clinicalBorderColor = new DeviceRgb(189, 189, 189); // Solid Gray Border
        DeviceRgb clinicalTextColor = new DeviceRgb(45, 62, 80);   // Primary Navy theme
        
        for (java.util.List<String> rowData : data) {
            for (int i = 0; i < maxCols; i++) {
                String val = (i < rowData.size()) ? rowData.get(i) : "";
                t.addCell(new Cell().add(new Paragraph(val).setFontSize(9).setFontColor(clinicalTextColor).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER))
                           .setBorder(new com.itextpdf.layout.borders.SolidBorder(clinicalBorderColor, 0.5f))
                           .setPadding(6)
                           .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));
            }
        }
        doc.add(t);
    }

    private static String getDynamicPathologistFooter() {
        String role = SessionContext.getUserRole();
        String staffId = SessionContext.getStaffId();
        
        // If Admin, use the globally configured report_pathologist setting
        if ("ADMIN".equalsIgnoreCase(role)) {
            return DatabaseManager.getSetting("report_pathologist", "Abid Sharif\nJr Clinical Tech (Pathology)");
        }
        
        // If not Admin (e.g. Laboratorian/Technician), try to fetch their name and qualification from staff table
        if (staffId != null && !staffId.isEmpty()) {
            try (Connection conn = DatabaseManager.getConnection()) {
                String sql = "SELECT name, qualification FROM staff WHERE staff_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, staffId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String name = rs.getString("name");
                        String qual = rs.getString("qualification");
                        if (name != null) {
                            return name + (qual != null && !qual.isEmpty() ? "\n" + qual : "");
                        }
                    }
                }
            } catch (Exception e) {}
        }
        return DatabaseManager.getSetting("report_pathologist", "Abid Sharif\nJr Clinical Tech (Pathology)");
    }

    public static String getReportPathologist() {
        return getDynamicPathologistFooter();
    }

    public static String getClinicalOffsetTime(String baseDate, int minutesToAdd) {
        if (baseDate == null || baseDate.trim().isEmpty()) {
             java.util.Calendar fallbackCal = java.util.Calendar.getInstance();
             fallbackCal.add(java.util.Calendar.MINUTE, minutesToAdd);
             return new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(fallbackCal.getTime());
        }
        try {
            java.text.SimpleDateFormat sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date drawDate;
            try {
                drawDate = sdfIn.parse(baseDate);
            } catch (Exception e) {
                drawDate = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").parse(baseDate);
            }
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(drawDate);
            cal.add(java.util.Calendar.MINUTE, minutesToAdd);
            java.util.Date offsetDate = cal.getTime();
            
            java.util.Date now = new java.util.Date();
            java.util.Date finalDate = now.after(offsetDate) ? now : offsetDate;
            
            return new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a").format(finalDate);
        } catch (Exception e) {
            return baseDate;
        }
    }

    public static String generateSummaryReport(java.util.List<com.lab.lms.models.SummaryReportRow> data, String period, String doctor) {
        String dir = getReportDir();
        File file = new File(dir, "SUMMARY_REPORT_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(20, 36, 40, 36);

            // Lab Branding
            String labName = DatabaseManager.getSetting("lab_name", "Laboratory Management System");
            String labAddress = DatabaseManager.getSetting("lab_address", "");
            String labContact = DatabaseManager.getSetting("lab_contact", "");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");
            String headerPath = DatabaseManager.getSetting("lab_header", "");

            if (headerPath != null && !headerPath.trim().isEmpty() && new File(headerPath).isFile() && !headerPath.toLowerCase().contains("no header")) {
                Image customHeader = new Image(ImageDataFactory.create(headerPath)).setWidth(UnitValue.createPercentValue(100));
                document.add(customHeader);
            } else {
                addDefaultHeader(document, pdf, labName, labAddress, labContact, logoPath, "LMS-SUMMARY");
            }

            document.add(new Paragraph("CLINICAL LAB WORK SUMMARY (" + period.toUpperCase() + ")").setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(15, 101, 192)));
            if (doctor != null && !"All Doctors".equals(doctor)) {
                document.add(new Paragraph("Referred By: " + doctor.toUpperCase()).setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            }
            document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(10));

            float[] pointColumnWidths = {12F, 10F, 15F, 23F, 10F, 10F, 10F, 10F};
            Table table = new Table(UnitValue.createPercentArray(pointColumnWidths)).useAllAvailableWidth();
            
            // Header
            String[] headers = {"DATE", "MR#", "PATIENT", "TESTS", "DR", "NET", "PAID", "DUE"};
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h)).setBold().setFontSize(8).setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE));
            }

            double totalNet = 0;
            double totalPaid = 0;
            double totalDue = 0;
            for (com.lab.lms.models.SummaryReportRow row : data) {
                table.addCell(new Cell().add(new Paragraph(row.getDate() != null ? row.getDate() : "-")).setFontSize(7).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(row.getMrNumber())).setFontSize(7).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(row.getPatientName().toUpperCase())).setFontSize(7).setPadding(2).setBold());
                table.addCell(new Cell().add(new Paragraph(row.getTests() != null ? row.getTests().toUpperCase() : "-")).setFontSize(6).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(row.getDoctor() != null ? row.getDoctor().toUpperCase() : "-")).setFontSize(7).setPadding(2));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getAmount()))).setFontSize(8).setBold().setPadding(2).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getPaid()))).setFontSize(8).setPadding(2).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getDue()))).setFontSize(8).setPadding(2).setTextAlignment(TextAlignment.RIGHT).setFontColor(row.getDue() > 0 ? ColorConstants.RED : ColorConstants.BLACK));
                
                totalNet += row.getAmount();
                totalPaid += row.getPaid();
                totalDue += row.getDue();
            }

            document.add(table);

            // Totals
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{60, 13.3f, 13.3f, 13.3f})).useAllAvailableWidth();
            totalsTable.addCell(new Cell().add(new Paragraph("TOTALS (" + data.size() + " Records)")).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(5));
            totalsTable.addCell(new Cell().add(new Paragraph(String.format("%.0f PKR", totalNet))).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(232, 245, 233)).setPadding(5));
            totalsTable.addCell(new Cell().add(new Paragraph(String.format("%.0f PKR", totalPaid))).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(224, 242, 241)).setPadding(5));
            totalsTable.addCell(new Cell().add(new Paragraph(String.format("%.0f PKR", totalDue))).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(255, 235, 238)).setPadding(5));
            document.add(totalsTable.setMarginTop(10));

            document.add(new LineSeparator(new SolidLine(1f)).setMarginTop(20));
            document.add(new Paragraph("Printed on: " + new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new java.util.Date()))
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateSummaryReportThermal(java.util.List<com.lab.lms.models.SummaryReportRow> data, String period, String doctor, double tDisc, double tExp, double tPL) {
        String dir = getReportDir();
        File file = new File(dir, "CASHBOOK_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            float width = 226f;
            float height = 550f + (data.size() * 25f); 
            com.itextpdf.kernel.geom.PageSize thermalSize = new com.itextpdf.kernel.geom.PageSize(width, height);
            
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(thermalSize);
            
            Document document = new Document(pdf);
            document.setMargins(10, 12, 10, 12);
            document.setFontSize(7);

            // Lab Branding (Strict Sync with Receipt Defaults)
            String labName = DatabaseManager.getSetting("lab_name", "MSF SOLUTION HUB").toUpperCase();
            String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
            String labContact = DatabaseManager.getSetting("lab_contact", "03165794442");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");

            if (logoPath != null && new File(logoPath).exists()) {
                try {
                    Image logo = new Image(ImageDataFactory.create(logoPath)).setWidth(35);
                    document.add(logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));
                } catch (Exception e) {}
            }

            document.add(new Paragraph(labName).setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
            document.add(new Paragraph(labAddress).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0).setMarginBottom(0));
            document.add(new Paragraph(labContact).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0));

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(2).setMarginBottom(4));
            document.add(new Paragraph("SALES PERFORMANCE LOG (TEST RECORD)").setFontSize(8).setBold().setTextAlignment(TextAlignment.CENTER));
            
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth().setMarginTop(5);
            SolidBorder gridBorder = new SolidBorder(ColorConstants.BLACK, 0.5f);
            
            addBorderedRow(infoTable, "Period", period.toUpperCase(), gridBorder);
            
            if (doctor != null && !"All Doctors".equals(doctor)) {
                addBorderedRow(infoTable, "Referred By", doctor.toUpperCase(), gridBorder);
            }
            
            String currentUser = SessionContext.getUsername();
            addBorderedRow(infoTable, "Printed By", (currentUser != null ? currentUser.toUpperCase() : "ADMIN"), gridBorder);
            
            document.add(infoTable.setMarginBottom(10));
            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(5));

            // Column Header: PATIENT | NET | PAID | DUE
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 25, 15, 15, 15, 15})).useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("MR NO")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)));
            table.addHeaderCell(new Cell().add(new Paragraph("PATIENT")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)));
            table.addHeaderCell(new Cell().add(new Paragraph("NET")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("PAID")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("DUE")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("BY")).setFontSize(5.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));

            double tNet = 0, tPaid = 0, tDue = 0, tOriginal = 0;
            for (com.lab.lms.models.SummaryReportRow row : data) {
                table.addCell(new Cell().add(new Paragraph(row.getMrNumber())).setFontSize(5.5f).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(new Paragraph(row.getPatientName())).setFontSize(5.5f).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getAmount()))).setFontSize(5.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getPaid()))).setFontSize(5.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getDue()))).setFontSize(5.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(row.getPerformedBy() == null ? "" : row.getPerformedBy())).setFontSize(5.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                
                tNet += row.getAmount();
                tPaid += row.getPaid();
                tDue += row.getDue();
                tOriginal += row.getOriginalTotal();
            }
            document.add(table);

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
            
            // Grand Recap with P&L (Financial Integrity Sync)
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth().setMarginTop(8);
            
            addSummaryRow(summaryTable, "GROSS VALUE:", String.format("%,.0f", tOriginal), false);
            addSummaryRow(summaryTable, "TOTAL DISCOUNT:", String.format("%,.0f", tDisc), false);
            addSummaryRow(summaryTable, "NET REVENUE:", String.format("%,.0f", tNet), false);
            document.add(new Paragraph("\n").setFontSize(2));
            addSummaryRow(summaryTable, "TOTAL CASH REC:", String.format("%,.0f", tPaid), false);
            addSummaryRow(summaryTable, "PENDING DUE:", String.format("%,.0f", tDue), false);
            document.add(new Paragraph("\n").setFontSize(2));
            addSummaryRow(summaryTable, "TOTAL EXPENSE:", String.format("%,.0f", tExp), false);
            addSummaryRow(summaryTable, "PROFIT & LOSS (P&L):", String.format("%,.0f", tPL), true);
            
            document.add(summaryTable);

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
            
            String summaryQrData = "--- MSF FINANCIAL AUDIT ---\n" +
                                   "System: Laboratory Management System\n" +
                                   "Audit Type: Cashbook Summary\n" +
                                   "Period: " + period.toUpperCase() + "\n" +
                                   "Revenue: PKR " + String.format("%.0f", tNet) + "\n" +
                                   "Ref. Doctor: " + (doctor != null ? doctor.toUpperCase() : "ALL") + "\n" +
                                   "Date: " + new java.text.SimpleDateFormat("dd-MMM-yyyy").format(new java.util.Date()) + "\n" +
                                   "Status: OFFICIALLY VERIFIED";
            
            BarcodeQRCode qr = new BarcodeQRCode(summaryQrData);
            Image qrImage = new Image(qr.createFormXObject(pdf)).setWidth(50);
            
            document.add(new Paragraph("SCAN ME TO VERIFY").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(5).setMarginBottom(-2));
            document.add(new Paragraph().add(qrImage).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

            document.add(new Paragraph("Printed: " + new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new java.util.Date()))
                    .setFontSize(6.5f).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(5));
            document.add(new Paragraph("THANK YOU").setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(-5));
            document.add(new Paragraph("FOR VISITING OUR LABORATORY").setFontSize(7).setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

            document.add(new Paragraph("Proudly Developed by Team MSF PVT LTD +923165794442").setFontSize(5).setTextAlignment(TextAlignment.CENTER).setItalic());

            document.close();
            return dest;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String generateDoctorsSummaryReportThermal(java.util.List<com.lab.lms.models.SummaryReportRow> aggregatedData, String period, double tDisc, double tExp, double tPL) {
        String dir = getReportDir();
        File file = new File(dir, "CASHBOOK_DR_SUM_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            float width = 226f;
            float height = 550f + (aggregatedData.size() * 25f); 
            com.itextpdf.kernel.geom.PageSize thermalSize = new com.itextpdf.kernel.geom.PageSize(width, height);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(thermalSize);
            Document document = new Document(pdf);
            document.setMargins(10, 12, 10, 12);
            document.setFontSize(7);

            String labName = DatabaseManager.getSetting("lab_name", "MSF SOLUTION HUB").toUpperCase();
            String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
            String labContact = DatabaseManager.getSetting("lab_contact", "03165794442");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");

            if (logoPath != null && new File(logoPath).exists()) {
                try {
                    Image logo = new Image(ImageDataFactory.create(logoPath)).setWidth(35);
                    document.add(logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));
                } catch (Exception e) {}
            }
            document.add(new Paragraph(labName).setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
            document.add(new Paragraph(labAddress).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0).setMarginBottom(0));
            document.add(new Paragraph(labContact).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0));

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(2).setMarginBottom(4));
            document.add(new Paragraph("DOCTOR'S REVENUE SUMMARY").setFontSize(8).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth().setMarginTop(5);
            SolidBorder gridBorder = new SolidBorder(ColorConstants.BLACK, 0.5f);
            addBorderedRow(infoTable, "Period", period.toUpperCase(), gridBorder);
            String currentUser = SessionContext.getUsername();
            addBorderedRow(infoTable, "Printed By", (currentUser != null ? currentUser.toUpperCase() : "ADMIN"), gridBorder);
            document.add(infoTable.setMarginBottom(10));
            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(5));

            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20})).useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("PROVIDER")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)));
            table.addHeaderCell(new Cell().add(new Paragraph("NET")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("PAID")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("DUE")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));

            double tNetTotal = 0, tPaidTotal = 0, tDueTotal = 0;
            for (com.lab.lms.models.SummaryReportRow row : aggregatedData) {
                String drName = row.getDoctor();
                if (drName == null || drName.isEmpty()) drName = "SELF / WALK-IN";
                if (drName.length() > 20) drName = drName.substring(0, 18) + "...";
                
                table.addCell(new Cell().add(new Paragraph(drName.toUpperCase())).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getAmount()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getPaid()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getDue()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                
                tNetTotal += row.getAmount();
                tPaidTotal += row.getPaid();
                tDueTotal += row.getDue();
            }
            document.add(table);

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth().setMarginTop(8);
            addSummaryRow(summaryTable, "TOTAL NET REVENUE:", String.format("%,.0f", tNetTotal), false);
            addSummaryRow(summaryTable, "TOTAL PAID (CASH):", String.format("%,.0f", tPaidTotal), false);
            addSummaryRow(summaryTable, "TOTAL PENDING DUE:", String.format("%,.0f", tDueTotal), false);
            document.add(new Paragraph("\n").setFontSize(1));
            addSummaryRow(summaryTable, "TOTAL EXPENSE:", String.format("%,.0f", tExp), false);
            addSummaryRow(summaryTable, "PROFIT & LOSS (P&L):", String.format("%,.0f", tPL), true);
            document.add(summaryTable);

            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
            BarcodeQRCode qr = new BarcodeQRCode("MSF DOCTORS AUDIT\nNet: " + tNetTotal + "\nPeriod: " + period.toUpperCase());
            document.add(new Paragraph().add(new Image(qr.createFormXObject(pdf)).setWidth(50)).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));
            document.add(new Paragraph("Developed by MSF PVT LTD").setFontSize(5).setTextAlignment(TextAlignment.CENTER).setItalic());
            
            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateBatchSummaryReportThermal(java.util.Map<String, java.util.List<com.lab.lms.models.SummaryReportRow>> doctorGroups, String period) {
        String dir = getReportDir();
        File file = new File(dir, "CASHBOOK_BATCH_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            float width = 226f;
            pdf.setDefaultPageSize(new com.itextpdf.kernel.geom.PageSize(width, 800f));
            Document document = new Document(pdf);
            document.setMargins(0, 12, 0, 12);
            document.setFontSize(7);

            String labName = DatabaseManager.getSetting("lab_name", "MSF SOLUTION HUB").toUpperCase();
            String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
            String labContact = DatabaseManager.getSetting("lab_contact", "03165794442");
            String logoPath = DatabaseManager.getSetting("lab_logo", "");
            String currentUser = SessionContext.getUsername();
            SolidBorder gridBorder = new SolidBorder(ColorConstants.BLACK, 0.5f);

            int count = 0;
            for (java.util.Map.Entry<String, java.util.List<com.lab.lms.models.SummaryReportRow>> entry : doctorGroups.entrySet()) {
                String doctorName = entry.getKey();
                java.util.List<com.lab.lms.models.SummaryReportRow> patients = entry.getValue();
                
                float sectionHeight = 480f + (patients.size() * 22f);
                pdf.setDefaultPageSize(new com.itextpdf.kernel.geom.PageSize(width, sectionHeight));
                
                if (count > 0) {
                    document.add(new com.itextpdf.layout.element.AreaBreak(com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));
                }
                
                document.add(new Paragraph("\n").setFontSize(5)); // Top padding
                if (logoPath != null && new File(logoPath).exists()) {
                    try {
                        Image logo = new Image(ImageDataFactory.create(logoPath)).setWidth(35);
                        document.add(logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));
                    } catch (Exception e) {}
                }
                document.add(new Paragraph(labName).setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new Paragraph(labAddress).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0).setMarginBottom(0));
                document.add(new Paragraph(labContact).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0));

                document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(2).setMarginBottom(4));
                document.add(new Paragraph("INDIVIDUAL DOCTOR SUMMARY").setFontSize(8).setBold().setTextAlignment(TextAlignment.CENTER));
                
                Table infoTable = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth().setMarginTop(5);
                addBorderedRow(infoTable, "Period", period.toUpperCase(), gridBorder);
                addBorderedRow(infoTable, "Referred By", doctorName, gridBorder);
                addBorderedRow(infoTable, "Printed By", (currentUser != null ? currentUser.toUpperCase() : "ADMIN"), gridBorder);
                document.add(infoTable.setMarginBottom(10));

                Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20})).useAllAvailableWidth();
                table.addHeaderCell(new Cell().add(new Paragraph("PATIENT")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)));
                table.addHeaderCell(new Cell().add(new Paragraph("NET")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(new Cell().add(new Paragraph("PAID")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
                table.addHeaderCell(new Cell().add(new Paragraph("DUE")).setFontSize(6.5f).setBold().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));

                double tNet = 0, tPaid = 0, tDue = 0;
                for (com.lab.lms.models.SummaryReportRow row : patients) {
                    String name = row.getPatientName();
                    if (name.length() > 20) name = name.substring(0, 18) + "...";
                    table.addCell(new Cell().add(new Paragraph(name.toUpperCase())).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getAmount()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getPaid()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.0f", row.getDue()))).setFontSize(6f).setBorder(Border.NO_BORDER).setPaddingTop(2).setTextAlignment(TextAlignment.RIGHT));
                    tNet += row.getAmount();
                    tPaid += row.getPaid();
                    tDue += row.getDue();
                }
                document.add(table);

                document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
                Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth().setMarginTop(8);
                addSummaryRow(summaryTable, "TOTAL REVENUE:", String.format("%,.0f", tNet), false);
                addSummaryRow(summaryTable, "TOTAL CASH REC:", String.format("%,.0f", tPaid), false);
                addSummaryRow(summaryTable, "TOTAL RECEIVABLE:", String.format("%,.0f", tDue), true);
                document.add(summaryTable);

                document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(8));
                BarcodeQRCode qr = new BarcodeQRCode("BATCH: " + doctorName + "\nNet: " + tNet);
                document.add(new Paragraph().add(new Image(qr.createFormXObject(pdf)).setWidth(50)).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));
                document.add(new Paragraph("Developed by MSF PVT LTD").setFontSize(5).setTextAlignment(TextAlignment.CENTER).setItalic());

                count++;
            }

            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generatePatientHistoryReport(String patientId) {
        String dir = getReportDir();
        File file = new File(dir, "HISTORY_" + patientId + "_" + System.currentTimeMillis() + ".pdf");
        String dest = file.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            float width = 226f; // 80mm Thermal Width
            
            // Preliminary count for dynamic height
            int recordCount = 10; 
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM invoices WHERE patient_id = ?");
                pstmt.setString(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) recordCount += (rs.getInt(1) * 12);
            }
            
            float height = Math.max(650f, recordCount * 18f); // Generous spacing for thermal
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(new PageSize(width, height));
            Document document = new Document(pdf);
            document.setMargins(10, 15, 10, 15);
            document.setFontSize(8);

            // Lab Branding
            String labName = DatabaseManager.getSetting("lab_name", "MSF DIGITAL SOLUTIONS").toUpperCase();
            String labAddress = DatabaseManager.getSetting("lab_address", "Asmat Abad Charsadda");
            String labContact = DatabaseManager.getSetting("lab_contact", "03165794442");

            document.add(new Paragraph(labName).setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
            document.add(new Paragraph(labAddress).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0).setMarginBottom(0));
            document.add(new Paragraph(labContact).setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginTop(0));
            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(5));

            document.add(new Paragraph("CHRONOLOGICAL SUBJECT HISTORY").setBold().setFontSize(8.5f).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(150, 17, 17)));
            document.add(new Paragraph("MR REGISTRY ID: " + patientId).setBold().setFontSize(7.5f).setTextAlignment(TextAlignment.CENTER));
            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(5).setMarginBottom(10));

            try (Connection conn = DatabaseManager.getConnection()) {
                // Get Patient Name
                String pName = "N/A";
                PreparedStatement pnPstmt = conn.prepareStatement("SELECT name FROM patients WHERE patient_id = ?");
                pnPstmt.setString(1, patientId);
                ResultSet pnRs = pnPstmt.executeQuery();
                if (pnRs.next()) pName = pnRs.getString("name");
                document.add(new Paragraph("Patient: " + pName.toUpperCase()).setBold().setFontSize(7).setMarginBottom(10));

                // Get All Invoices (Visits)
                String invSql = "SELECT * FROM invoices WHERE patient_id = ? ORDER BY date DESC";
                PreparedStatement pstmt = conn.prepareStatement(invSql);
                pstmt.setString(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                
                boolean hasAny = false;
                while (rs.next()) {
                    hasAny = true;
                    String date = rs.getString("date");
                    double total = rs.getDouble("total_amount");
                    double paid = rs.getDouble("paid_amount");
                    double due = rs.getDouble("due_amount");
                    String status = rs.getString("status");

                    document.add(new Paragraph("VISIT LOG: " + date).setBold().setFontSize(7.5f).setBackgroundColor(new DeviceRgb(240, 240, 240)).setPadding(3));
                    
                    // Financial Recap for this Visit
                    Table finTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginBottom(5);
                    finTable.addCell(new Cell().add(new Paragraph("Amount: " + total)).setFontSize(6.5f).setBorder(Border.NO_BORDER));
                    finTable.addCell(new Cell().add(new Paragraph("Paid: " + paid)).setFontSize(6.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                    finTable.addCell(new Cell().add(new Paragraph("Status: " + status)).setBold().setFontSize(6.5f).setBorder(Border.NO_BORDER).setFontColor(due > 0 ? ColorConstants.RED : ColorConstants.GREEN));
                    finTable.addCell(new Cell().add(new Paragraph("Balance: " + due)).setFontSize(6.5f).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                    document.add(finTable);

                    // List Tests/Parameters
                    // Standard clinical date search with flexible formatting for subject history synchronization
                    String resSql = "SELECT t.name as tname, tp.name as pname, r.value, r.status " +
                                    "FROM results r " +
                                    "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                                    "JOIN tests t ON tp.test_id = t.id " +
                                    "JOIN samples s ON r.sample_id = s.sample_id " +
                                    "WHERE s.patient_id = ? " +
                                    "ORDER BY t.name, tp.id";
                    
                    try (PreparedStatement rPstmt = conn.prepareStatement(resSql)) {
                        rPstmt.setString(1, patientId);
                        ResultSet rRs = rPstmt.executeQuery();
                        
                        Table resTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth().setMarginBottom(10);
                        resTable.addHeaderCell(new Cell().add(new Paragraph("DIAGNOSTIC TEST")).setBold().setFontSize(6).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.3f)));
                        resTable.addHeaderCell(new Cell().add(new Paragraph("RESULT")).setBold().setFontSize(6).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.3f)).setTextAlignment(TextAlignment.RIGHT));

                        String lastTest = "";
                        boolean hasResults = false;
                        while (rRs.next()) {
                            hasResults = true;
                            String tName = rRs.getString("tname");
                            String pNameRes = rRs.getString("pname");
                            String val = rRs.getString("value");
                            
                            if (!tName.equals(lastTest)) {
                                resTable.addCell(new Cell(1, 2).add(new Paragraph(tName.toUpperCase()).setBold().setFontSize(6.5f).setPaddingTop(4)).setBorder(Border.NO_BORDER));
                                lastTest = tName;
                            }
                            
                            resTable.addCell(new Cell().add(new Paragraph(" - " + pNameRes)).setFontSize(6).setBorder(Border.NO_BORDER).setPaddingLeft(5));
                            resTable.addCell(new Cell().add(new Paragraph(val != null ? val : "-")).setFontSize(6).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                        }
                        if (hasResults) document.add(resTable);
                        else document.add(new Paragraph("No finalized results for this clinical visit.").setFontSize(6).setItalic().setMarginBottom(10));
                    }
                    document.add(new LineSeparator(new SolidLine(0.3f)).setPaddingBottom(10));
                    break; // Only show latest visit details for performance, or remove to show all
                }
                
                if (!hasAny) {
                    document.add(new Paragraph("SYSTEM ALERT: NO HISTORICAL RECORDS FOUND IN REGISTRY.").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER));
                }
            }

            document.add(new Paragraph("\n--- END OF CLINICAL HISTORY ---").setFontSize(6).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph("Printed: " + new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new java.util.Date())).setFontSize(5).setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateEmployeeCard(Staff staff) {
        String reportDir = getReportDir();
        File reportFile = new File(reportDir, "ID_" + staff.getStaffId() + "_" + System.currentTimeMillis() + ".pdf");
        String dest = reportFile.getAbsolutePath();

        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            
            // Portrait CR80 (2.125 x 3.375 inches -> 153 x 243 points)
            PageSize cardSize = new PageSize(153, 243);
            pdf.setDefaultPageSize(cardSize);
            Document document = new Document(pdf);
            document.setMargins(0, 0, 0, 0);

            String labName = DatabaseManager.getSetting("lab_name", "Laboratory Information System");
            String labAddress = DatabaseManager.getSetting("lab_address", "");
            String labPhone = DatabaseManager.getSetting("lab_contact", "");
            
            // --- FRONT FACE ---
            Table frontTable = new Table(1).useAllAvailableWidth().setHeight(243).setBorder(Border.NO_BORDER);
            
            // 1. Header: Modern Brand Bar
            Cell headerCell = new Cell().setBackgroundColor(new DeviceRgb(150, 17, 17)).setBorder(Border.NO_BORDER).setPadding(6).setHeight(38).setVerticalAlignment(VerticalAlignment.MIDDLE);
            headerCell.add(new Paragraph(labName.toUpperCase()).setFontColor(ColorConstants.WHITE).setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMultipliedLeading(0.9f));
            frontTable.addCell(headerCell);

            // 2. Body: Profile & Data
            Cell bodyCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10).setVerticalAlignment(VerticalAlignment.TOP).setTextAlignment(TextAlignment.CENTER);
            
            // Photo with Premium Border
            if (staff.getProfilePicture() != null && !staff.getProfilePicture().isEmpty() && new File(staff.getProfilePicture()).exists()) {
                 try {
                    Image img = new Image(ImageDataFactory.create(staff.getProfilePicture())).setWidth(72).setHeight(72).setMarginBottom(10).setHorizontalAlignment(HorizontalAlignment.CENTER).setBorder(new SolidBorder(new DeviceRgb(150, 17, 17), 1.5f));
                    bodyCell.add(img);
                 } catch (Exception e) {
                    bodyCell.add(new Paragraph("[ PHOTO ]").setFontSize(6).setMarginTop(20).setMarginBottom(20));
                 }
            } else {
                 bodyCell.add(new Paragraph("AUTHORIZED\nPERSONNEL").setBold().setFontSize(7).setBorder(new SolidBorder(new DeviceRgb(150, 17, 17), 0.8f)).setWidth(68).setHeight(68).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setMarginBottom(10).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginTop(12).setFontColor(new DeviceRgb(150, 17, 17)));
            }

            // Name & Designation (High Fidelity Typography)
            bodyCell.add(new Paragraph(staff.getName().toUpperCase()).setBold().setFontSize(10.5f).setFontColor(new DeviceRgb(26, 35, 126)).setMarginTop(2).setMultipliedLeading(0.95f));
            bodyCell.add(new Paragraph(staff.getDesignation() != null ? staff.getDesignation().toUpperCase() : "STAFF MEMBER").setFontSize(7.5f).setBold().setFontColor(new DeviceRgb(100, 116, 139)).setMultipliedLeading(0.9f));
            
            // Decorative Divider
            Table divider = new Table(new float[]{1, 2, 1}).setWidth(70).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginTop(10).setMarginBottom(10);
            divider.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.3f)));
            divider.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(new Paragraph("ID INFO").setFontSize(4).setFontColor(ColorConstants.GRAY)));
            divider.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(0.3f)));
            bodyCell.add(divider);
            
            bodyCell.add(new Paragraph("EMPLOYEE CODE").setFontSize(6).setBold().setFontColor(ColorConstants.GRAY));
            bodyCell.add(new Paragraph("STF-2026-" + String.format("%04d", Integer.parseInt(staff.getStaffId()))).setBold().setFontSize(8.5f).setMarginTop(-2));
            
            frontTable.addCell(bodyCell);
            
            // 3. Footer: Security Badge
            Cell footerLine = new Cell().setBorder(Border.NO_BORDER).setHeight(18).setVerticalAlignment(VerticalAlignment.BOTTOM).setPaddingBottom(6);
            Table footerLayout = new Table(new float[]{1, 1}).useAllAvailableWidth();
            footerLayout.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("STATUS: ACTIVE").setFontSize(5).setBold().setFontColor(new DeviceRgb(21, 128, 61))));
            footerLayout.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("SECURITY: VERIFIED").setFontSize(5).setBold().setTextAlignment(TextAlignment.RIGHT).setFontColor(new DeviceRgb(150, 17, 17))));
            footerLine.add(footerLayout);
            frontTable.addCell(footerLine);
            
            document.add(frontTable);

            // --- BACK FACE ---
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            Table backTable = new Table(1).useAllAvailableWidth().setHeight(243).setBorder(Border.NO_BORDER);
            Cell backCell = new Cell().setBorder(Border.NO_BORDER).setPadding(14).setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER);
            
            backCell.add(new Paragraph("TERM & CONDITIONS").setBold().setFontSize(8.5f).setMarginBottom(12).setFontColor(new DeviceRgb(150, 17, 17)));
            
            Paragraph pInstr = new Paragraph("This identification card remains the property of the organization. It must be displayed while on duty. If lost or found, please contact:")
                    .setFontSize(6.5f).setMultipliedLeading(1.1f).setMarginBottom(10).setTextAlignment(TextAlignment.JUSTIFIED);
            backCell.add(pInstr);

            if (labAddress != null && !labAddress.isEmpty()) {
                backCell.add(new Paragraph(labAddress).setFontSize(6.5f).setBold().setMarginBottom(12).setMultipliedLeading(1.1f));
            }
            
            backCell.add(new Paragraph("HELPLINE: " + labPhone).setBold().setFontSize(8.5f).setPadding(5).setBackgroundColor(new DeviceRgb(241, 245, 249)).setFontColor(new DeviceRgb(15, 23, 42)));

            // High-Resolution QR Code
            try {
                BarcodeQRCode qrCode = new BarcodeQRCode("https://lab.lms/verify/staff/" + staff.getStaffId());
                Image qrImg = new Image(qrCode.createFormXObject(pdf)).setWidth(55).setHeight(55).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginTop(15);
                backCell.add(qrImg);
            } catch (Exception e) {}

            backCell.add(new Paragraph("SCAN TO VERIFY IDENTITY").setFontSize(5).setBold().setMarginTop(5).setFontColor(ColorConstants.GRAY));
            
            backTable.addCell(backCell);
            document.add(backTable);

            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    } // End of generateEmployeeCard

    public static String generateDoctorCommissionReportThermal(String doctorName, java.time.LocalDate startDate, java.time.LocalDate endDate, double commissionRate, String periodLabel) {
        String dest = new java.io.File(getReceiptDir(), "Dr_" + doctorName.replaceAll("\\s+", "_") + "_Commission.pdf").getAbsolutePath();
        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(dest);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            // Dynamic thermal roll height based on test count: 226 points wide
            float currentY = 0;
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, new com.itextpdf.kernel.geom.PageSize(226, 800));
            document.setMargins(10, 10, 10, 10);

            // Fetch Data
            int totalTests = 0;
            double grossRevenue = 0;
            double totalDiscount = 0;
            double netRevenue = 0;
            double receivedAmount = 0;
            
            java.util.List<String> details = new java.util.ArrayList<>();
            try (java.sql.Connection conn = DatabaseManager.getConnection()) {
                String sql = "SELECT p.name AS pat_name, i.total_amount, i.discount, i.final_amount, i.paid_amount, i.due_amount, " +
                             "(SELECT GROUP_CONCAT(t.name, ', ') FROM samples s JOIN results r ON s.sample_id = r.sample_id " +
                             "JOIN test_parameters tp ON r.parameter_id = tp.id JOIN tests t ON tp.test_id = t.id " +
                             "WHERE s.patient_id = p.patient_id AND s.collection_date LIKE SUBSTR(i.date, 1, 10) || '%') as tests_list " +
                             "FROM invoices i JOIN patients p ON i.patient_id = p.patient_id " +
                             "WHERE p.referred_doctor = ? AND date(i.date, 'localtime') BETWEEN ? AND ?";
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, doctorName);
                pstmt.setString(2, startDate.toString());
                pstmt.setString(3, endDate.toString());
                java.sql.ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    totalTests++;
                    grossRevenue += rs.getDouble("total_amount");
                    totalDiscount += rs.getDouble("discount");
                    netRevenue += rs.getDouble("final_amount");
                    receivedAmount += rs.getDouble("paid_amount");
                    
                    String pat = rs.getString("pat_name");
                    String tList = rs.getString("tests_list");
                    double amt = rs.getDouble("final_amount");
                    double pd = rs.getDouble("paid_amount");
                    
                    if (tList == null) tList = "Consult/Other";
                    if (tList.length() > 20) tList = tList.substring(0, 17) + "...";
                    details.add(pat + " | " + tList);
                    details.add(String.format("Amt: %.0f PKR (Paid: %.0f)", amt, pd));
                }
            }
            
            if (totalTests == 0) {
                pdf.close();
                return null; // Return null to indicate no records found
            }

            // --- HEADER ---
            document.add(new com.itextpdf.layout.element.Paragraph("CLINICAL COMMISSION ROLL")
                    .setFontSize(10).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setFontColor(com.itextpdf.kernel.colors.DeviceRgb.BLACK));
            document.add(new com.itextpdf.layout.element.Paragraph("Clinical Laboratory")
                    .setFontSize(12).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            
            document.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(5));
            document.add(new com.itextpdf.layout.element.Paragraph("Doctor: " + doctorName).setFontSize(9).setBold());
            document.add(new com.itextpdf.layout.element.Paragraph("Period: " + periodLabel).setFontSize(8));
            document.add(new com.itextpdf.layout.element.Paragraph("Print Date: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())).setFontSize(8));
            
            document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.DashedLine()).setMarginTop(2).setMarginBottom(2));

            // --- DETAILS ---
            for (String str : details) {
                document.add(new com.itextpdf.layout.element.Paragraph(str).setFontSize(8));
            }
            
            document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.DashedLine()).setMarginTop(5).setMarginBottom(5));

            // --- SUMMARY FINANCIALS ---
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("Total Patients: %d", totalTests)).setFontSize(9).setBold());
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("Gross Tests Amount: %.0f PKR", grossRevenue)).setFontSize(8));
            if (totalDiscount > 0) document.add(new com.itextpdf.layout.element.Paragraph(String.format("Total Discount: %.0f PKR", totalDiscount)).setFontSize(8));
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("Net Revenue: %.0f PKR", netRevenue)).setFontSize(9).setBold());
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("Total Paid Received: %.0f PKR", receivedAmount)).setFontSize(8));
            
            document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.DashedLine()).setMarginTop(5).setMarginBottom(5));
            
            // --- COMMISSION COMPUTATION ---
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("Commission Rate: %.1f%%", commissionRate)).setFontSize(9));
            double payable = netRevenue * (commissionRate / 100.0);
            
            document.add(new com.itextpdf.layout.element.Paragraph(String.format("COMMISSION PAYABLE:\n%.0f PKR", payable))
                .setFontSize(12).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(220, 220, 220))
                .setPadding(3).setMarginTop(5));

            document.add(new com.itextpdf.layout.element.Paragraph("").setMarginBottom(10));
            document.add(new com.itextpdf.layout.element.Paragraph("- SYSTEM GENERATED -")
                .setFontSize(7).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setFontColor(com.itextpdf.kernel.colors.DeviceGray.GRAY));

            document.close();
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


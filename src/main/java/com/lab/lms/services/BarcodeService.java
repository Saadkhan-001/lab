package com.lab.lms.services;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.lab.lms.dao.DatabaseManager;

import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BarcodeService {

    public static void generateAndOpenBarcode(String sampleId) {
        generateAndOpenBarcodes(Arrays.asList(sampleId));
    }

    public static void generateAndOpenBarcodes(List<String> sampleIds) {
        if (sampleIds == null || sampleIds.isEmpty()) return;

        String firstId = sampleIds.get(0);
        String fileName = sampleIds.size() == 1 ? firstId : "MULTIPLE_" + System.currentTimeMillis();
        String dest = getBarcodeFilePath(fileName);
        
        try {
            // 2. Generate PDF Label (Approx 2x1 inch or 50x25 mm)
            // PageSize is in points (1/72 inch). 2x1 inch = 144x72 points.
            PageSize labelSize = new PageSize(144, 80); 
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, labelSize);
            document.setMargins(2, 5, 2, 5);

            String labName = DatabaseManager.getSetting("lab_name", "LAB MSF");
            String date = new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date());

            for (int i = 0; i < sampleIds.size(); i++) {
                if (i > 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }

                String currentId = sampleIds.get(i);
                
                // 1. Fetch Data
                String patientName = "N/A";
                String ageGender = "N/A";
                List<String> tests = new ArrayList<>();

                try (Connection conn = DatabaseManager.getConnection()) {
                    String sql = "SELECT p.name, p.age, p.age_months, p.age_days, p.gender, t.name as test_name " +
                            "FROM samples s " +
                            "JOIN patients p ON s.patient_id = p.patient_id " +
                            "JOIN results r ON s.sample_id = r.sample_id " +
                            "JOIN test_parameters tp ON r.parameter_id = tp.id " +
                            "JOIN tests t ON tp.test_id = t.id " +
                            "WHERE s.sample_id = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, currentId);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        patientName = rs.getString("name");
                        int y = rs.getInt("age");
                        int m = rs.getInt("age_months");
                        int d = rs.getInt("age_days");
                        String ageStr = y + "y";
                        if (m > 0 || d > 0) ageStr += " " + m + "m " + d + "d";
                        
                        ageGender = ageStr + " / " + rs.getString("gender");
                        tests.add(rs.getString("test_name"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String testList = String.join(", ", tests);
                if (testList.length() > 40) testList = testList.substring(0, 37) + "...";

                document.add(new Paragraph(labName).setBold().setFontSize(6).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(0).setMarginBottom(2));
                
                Paragraph pData = new Paragraph().setFontSize(6).setMarginBottom(0).setMultipliedLeading(1.0f);
                pData.add(new com.itextpdf.layout.element.Text(patientName.toUpperCase()).setBold().setFontSize(7));
                pData.add(new com.itextpdf.layout.element.Text("  " + ageGender));
                document.add(pData);

                // Barcode 128 - High Fidelity
                Barcode128 barcode = new Barcode128(pdf);
                barcode.setCode(currentId);
                barcode.setCodeType(Barcode128.CODE128);
                Image barcodeImg = new Image(barcode.createFormXObject(pdf)).setWidth(UnitValue.createPercentValue(100)).setHeight(25);
                document.add(barcodeImg.setMarginTop(1).setMarginBottom(1));

                document.add(new Paragraph(currentId).setFontSize(8).setBold().setTextAlignment(TextAlignment.CENTER).setMarginTop(-3).setMarginBottom(0));
                
                Paragraph footer = new Paragraph().setFontSize(5).setMultipliedLeading(1.0f).setTextAlignment(TextAlignment.CENTER);
                footer.add(new com.itextpdf.layout.element.Text(testList).setBold());
                footer.add(new com.itextpdf.layout.element.Text("\n" + date).setFontSize(4));
                document.add(footer);
            }

            document.close();

            // 3. Open File
            File file = new File(dest);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getBarcodeFilePath(String sampleId) {
        String dir = System.getProperty("user.home") + File.separator + ".lablms" + File.separator + "barcodes";
        new File(dir).mkdirs();
        return dir + File.separator + "BC_" + sampleId + "_" + System.currentTimeMillis() + ".pdf";
    }
}

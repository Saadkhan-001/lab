package com.lab.lms.services;

import java.awt.Desktop;

public class WhatsAppService {

    public static void sendReport(String patientName, String phone, String pdfPath) {
        sendReportWithRecovery(null, patientName, phone, pdfPath);
    }

    public static void sendReportWithRecovery(String patientId, String patientName, String phone, String pdfPath) {
        if (pdfPath == null || pdfPath.isEmpty())
            return;

        // Fetch Title for professional salutation if available
        String title = "";
        if (patientId != null && !patientId.isEmpty()) {
            try (java.sql.Connection conn = com.lab.lms.dao.DatabaseManager.getConnection()) {
                java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT title FROM patients WHERE patient_id = ?");
                pstmt.setString(1, patientId);
                java.sql.ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    title = rs.getString("title");
                    if (title == null) title = "";
                }
            } catch (Exception e) {}
        }

        String contact = phone;
        if (contact == null || contact.trim().isEmpty()) {
            // High-Velocity Recovery Prompt
            javafx.scene.control.TextInputDialog contactDialog = new javafx.scene.control.TextInputDialog();
            contactDialog.setTitle("WhatsApp Contact Required");
            contactDialog.setHeaderText("No contact number found for " + patientName);
            contactDialog.setContentText("Please enter a WhatsApp/Phone number:");

            java.util.Optional<String> contactResult = contactDialog.showAndWait();
            if (contactResult.isPresent() && !contactResult.get().trim().isEmpty()) {
                contact = contactResult.get().trim();
                
                // Synchronize back to Patient Profile if we have ID
                if (patientId != null && !patientId.isEmpty()) {
                    try (java.sql.Connection updateConn = com.lab.lms.dao.DatabaseManager.getConnection()) {
                        String updateSql = "UPDATE patients SET whatsapp = ?, phone = ? WHERE patient_id = ?";
                        java.sql.PreparedStatement upstmt = updateConn.prepareStatement(updateSql);
                        upstmt.setString(1, contact);
                        upstmt.setString(2, contact);
                        upstmt.setString(3, patientId);
                        upstmt.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            } else {
                return; // Cancelled
            }
        }

        try {
            // 1. Format Phone Number (Support for 03xx -> 923xx)
            String cleanPhone = contact.replaceAll("[^\\d]", "");
            if (cleanPhone.startsWith("0") && cleanPhone.length() == 11) {
                cleanPhone = "92" + cleanPhone.substring(1);
            }
            if (cleanPhone.isEmpty())
                return;

            // 2. Prepare Professional Message (Skip 'Other' as a vocal salutation)
            String displayName = (title != null && !title.isEmpty() && !"Other".equalsIgnoreCase(title)) ? title + " " + patientName : patientName;
            String message = "Hello *" + displayName + "*,\n\n" +
                    "Your diagnostic laboratory report is ready. Please find the attached PDF.\n\n" +
                    "Thank you for choosing our laboratory.";

            // 3. Stage File to Clipboard immediately
            java.io.File file = new java.io.File(pdfPath).getAbsoluteFile();
            if (file.exists()) {
                FileTransferable transferable = new FileTransferable(java.util.Collections.singletonList(file));
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
            }

            // 4. Open WhatsApp
            String url = "https://web.whatsapp.com/send?phone=" + cleanPhone + "&text="
                    + java.net.URLEncoder.encode(message, "UTF-8");

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new java.net.URI(url));
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("WhatsApp Assistant");
                    alert.setHeaderText("Report Copied to Clipboard");
                    alert.setContentText("1. WhatsApp will open in your browser.\n" +
                                       "2. The message is already typed for you.\n" +
                                       "3. Simply press CTRL + V to attach the report PDF.");
                    alert.show();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class FileTransferable implements java.awt.datatransfer.Transferable {
        private final java.util.List<java.io.File> files;

        public FileTransferable(java.util.List<java.io.File> files) {
            this.files = files;
        }

        @Override
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            return new java.awt.datatransfer.DataFlavor[] { java.awt.datatransfer.DataFlavor.javaFileListFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            return java.awt.datatransfer.DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor)
                throws java.awt.datatransfer.UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor))
                throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
            return files;
        }
    }
}

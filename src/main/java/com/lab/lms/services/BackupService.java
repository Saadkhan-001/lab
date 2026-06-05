package com.lab.lms.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.prefs.Preferences;

public class BackupService {
    private static final String BACKUP_PATH_KEY = "backup_location";
    private static final Preferences prefs = Preferences.userNodeForPackage(BackupService.class);

    public static void setBackupLocation(String path) {
        prefs.put(BACKUP_PATH_KEY, path);
    }

    public static String getBackupLocation() {
        return prefs.get(BACKUP_PATH_KEY, "");
    }

    public static void performBackup() {
        String destDir = getBackupLocation();
        if (destDir == null || destDir.isEmpty())
            return;

        File source = new File(com.lab.lms.dao.DatabaseManager.getDbPath());
        if (!source.exists())
            return;

        File destination = new File(destDir, "laboratory_backup.db");
        if (destination.getParentFile() != null) {
            destination.getParentFile().mkdirs();
        }

        try {
            streamCopy(source, destination);
            
            // In WAL mode, we must also copy the -wal file if it exists
            File walSource = new File(source.getAbsolutePath() + "-wal");
            if (walSource.exists()) {
                File walDest = new File(destination.getAbsolutePath() + "-wal");
                streamCopy(walSource, walDest);
            }
            
            System.out.println("Real-time backup performed to: " + destination.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Backup failed (File Lock or IO Error): " + e.getMessage());
        }
    }

    private static void streamCopy(File source, File dest) throws IOException {
        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}

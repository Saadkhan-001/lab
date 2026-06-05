package com.lab.lms.services;
 
import com.lab.lms.Main;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;
 
/**
 * Clinical Runtime Update Service
 * Handles GitHub-synchronized versioning and automated deployment.
 */
public class UpdateService {
    
    // Placeholder repository - Replace with your actual GitHub MSF repo
    private static final String GITHUB_USER = "atif-aslam-msf";
    private static final String GITHUB_REPO = "LAB-LMS";
    
    private static final String VERSION_URL = "https://raw.githubusercontent.com/" + GITHUB_USER + "/" + GITHUB_REPO + "/main/version.txt";
    private static final String UPDATE_DOWNLOAD_URL = "https://github.com/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/latest/download/ClinicalUpdate.jar";
 
    /**
     * Diagnostic check for internet reachability.
     */
    public static boolean isConnected() {
        try {
            // Using Cloudflare DNS for ultra-fast check
            URL url = new URL("https://1.1.1.1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2500); 
            conn.connect();
            return (conn.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }
 
    /**
     * Retrieve latest version ID from GitHub repository.
     */
    public static String fetchLatestVersion() throws Exception {
        URL url = new URL(VERSION_URL);
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() != 200) {
                System.err.println("[UPDATE] Repository version.txt not reached (HTTP " + conn.getResponseCode() + "). Defaulting to current.");
                return Main.APP_VERSION;
            }
            try (Scanner s = new Scanner(conn.getInputStream())) {
                if (s.hasNext()) {
                    return s.next().trim();
                }
            }
        } catch (Exception e) {
            System.err.println("[UPDATE] Connection failed: " + e.getMessage());
            throw new Exception("Unable to access GitHub update repository.");
        }
        return Main.APP_VERSION;
    }
 
    /**
     * Launch Automated Deployment Protocol
     */
    public static void applyUpdate() throws Exception {
        // 1. Download Latest Binaries to Internal Cache
        URL url = new URL(UPDATE_DOWNLOAD_URL);
        File tempDir = new File(System.getProperty("user.home"), ".lablms/cache");
        if (!tempDir.exists()) tempDir.mkdirs();
        
        File updateFile = new File(tempDir, "LMS_Update.jar");
        try (InputStream in = url.openStream()) {
            Files.copy(in, updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
 
        // 2. Seattle Sequence Replacement Script (Windows Internal)
        String currentJarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        
        File batchFile = new File("update_lms.bat");
        try (PrintWriter out = new PrintWriter(batchFile)) {
            out.println("@echo off");
            out.println("echo MSF DIGITAL SOLUTIONS - PREPARING CLINICAL UPDATE...");
            out.println("timeout /t 3 /nobreak > nul");
            out.println("copy /Y \"" + updateFile.getAbsolutePath() + "\" \"" + currentJarPath + "\"");
            out.println("echo Starting Updated Clinical Subject Management Terminal...");
            out.println("start \"\" javaw -jar \"" + currentJarPath + "\"");
            out.println("del \"%~f0\""); // Self-destruct script
        }
 
        // 3. Trigger Sequential Restart
        Runtime.getRuntime().exec("cmd /c start update_lms.bat");
        System.exit(0);
    }
}

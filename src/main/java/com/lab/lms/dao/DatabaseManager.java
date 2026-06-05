package com.lab.lms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class DatabaseManager {
        // Pre-configure WAL and Timeout at the Driver level
        // Professional Clinical Repositories (v7.0.2)
        private static final String APP_DATA_DIR = System.getProperty("user.home") + java.io.File.separator + ".lablms";
        private static final String DB_NAME = "laboratory.db";
        private static String URL;
        private static final java.util.Map<String, String> settingsCache = new java.util.concurrent.ConcurrentHashMap<>();
        private static final Object DB_LOCK = new Object();
        private static final Properties localConfig = new Properties();
        private static final String CONFIG_FILE = APP_DATA_DIR + java.io.File.separator + "local_config.properties";

        static {
                loadLocalConfig();
                refreshUrl();
        }

        private static void loadLocalConfig() {
                java.io.File dir = new java.io.File(APP_DATA_DIR);
                if (!dir.exists()) dir.mkdirs();
                
                java.io.File file = new java.io.File(CONFIG_FILE);
                if (file.exists()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                                localConfig.load(fis);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }

        private static void saveLocalConfig() {
                try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                        localConfig.store(fos, "Clinical Environment Local Configuration");
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

    public static void refreshUrl() {
        // Standard Clinical URL Resolution
        URL = "jdbc:sqlite:" + APP_DATA_DIR + java.io.File.separator + DB_NAME;
        
        // Deep Discovery engine for legacy migrations (C:\Laboratory, etc.)
        java.io.File homeDirDb = new java.io.File(APP_DATA_DIR, DB_NAME);
        java.io.File rootDirDb = new java.io.File("C:\\Laboratory", DB_NAME);
        java.io.File winAppDataDb = (System.getenv("APPDATA") != null) ? 
            new java.io.File(System.getenv("APPDATA") + java.io.File.separator + ".lablms", DB_NAME) : null;

        if (!homeDirDb.exists()) {
             if (winAppDataDb != null && winAppDataDb.exists()) {
                  URL = "jdbc:sqlite:" + winAppDataDb.getAbsolutePath();
             } else if (rootDirDb.exists()) {
                  URL = "jdbc:sqlite:" + rootDirDb.getAbsolutePath();
             }
        }
        
        System.out.println("Laboratory Production Database: " + URL);
        saveSetting("is_demo", "false"); // Force PRODUCTION mode on discovery
    }

        public static String getDbPath() {
                return System.getProperty("user.home") + java.io.File.separator + ".lablms" + java.io.File.separator
                                + DB_NAME;
        }

    public static String getDbDirectory() {
        return APP_DATA_DIR;
    }

        public static Connection getConnection() throws SQLException {
        synchronized (DB_LOCK) {
            org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
            config.setJournalMode(org.sqlite.SQLiteConfig.JournalMode.WAL);
            config.setBusyTimeout(15000); 

            int attempts = 5;
            while (attempts > 0) {
                try {
                    return config.createConnection(URL);
                } catch (SQLException e) {
                    attempts--;
                    if (attempts == 0) throw e;
                    System.out.println("[DB-RETRY] Connection failed, retrying in 1s... (" + attempts + " left)");
                    try { Thread.sleep(1000); } catch (InterruptedException ie) {}
                }
            }
            throw new SQLException("Failed to establish database connection after multiple attempts");
        }
    }

        public static void initializeDatabase() {
            synchronized (DB_LOCK) {
                try {
                System.out.println("[DB-DEBUG] 1. Folder Verification...");
                String dbDir = getDbDirectory();
                java.io.File directory = new java.io.File(dbDir);
                if (!directory.exists()) directory.mkdirs();

                System.out.println("[DB-DEBUG] 2. Lock Cleanup...");
/* 
				// [REMOVED FOR STABILITY] Deleting locks manually can crash the JVM if threads are active
				for (String suffix : new String[] { "-wal", "-shm" }) {
					java.io.File f = new java.io.File(dbDir, DB_NAME + suffix);
					if (f.exists()) f.delete();
				}
				*/


                System.out.println("[DB-DEBUG] 4. Opening Master Connection...");
                try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                    System.out.println("[DB-DEBUG] Connection Active. Applying PRAGMAs...");
                    stmt.execute("PRAGMA journal_mode=WAL"); 
                    stmt.execute("PRAGMA synchronous=NORMAL");
                    stmt.execute("PRAGMA busy_timeout=15000");

                    System.out.println("[DB-DEBUG] 5. Deploying Module: Core Clinical...");
                    stmt.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT, staff_id TEXT, full_name TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS patients (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id TEXT UNIQUE, name TEXT NOT NULL, age INTEGER, age_months INTEGER DEFAULT 0, age_days INTEGER DEFAULT 0, gender TEXT, phone TEXT, whatsapp TEXT, address TEXT, title TEXT, referred_doctor TEXT, staff_id TEXT, registration_date DATETIME DEFAULT CURRENT_TIMESTAMP)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS tests (id INTEGER PRIMARY KEY AUTOINCREMENT, numeric_code TEXT, alpha_code TEXT, name TEXT NOT NULL, category TEXT, price REAL, result_time TEXT, notes TEXT, is_special INTEGER DEFAULT 0, is_microscopic INTEGER DEFAULT 0, is_culture INTEGER DEFAULT 0, specimen TEXT DEFAULT 'Blood', container TEXT, volume TEXT, fasting TEXT, protocol_class TEXT DEFAULT 'INACTIVE')");
                    stmt.execute("CREATE TABLE IF NOT EXISTS test_parameters (id INTEGER PRIMARY KEY AUTOINCREMENT, test_id INTEGER, name TEXT NOT NULL, unit TEXT, category TEXT, min_range TEXT, max_range TEXT, min_range_male TEXT, max_range_male TEXT, min_range_female TEXT, max_range_female TEXT, min_range_kids TEXT, max_range_kids TEXT, print_order INTEGER DEFAULT 0, is_global INTEGER DEFAULT 1, FOREIGN KEY(test_id) REFERENCES tests(id) ON DELETE CASCADE)");
                    
                    System.out.println("[DB-DEBUG] 6. Deploying Module: Transactions...");
                    stmt.execute("CREATE TABLE IF NOT EXISTS invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id TEXT, total_amount REAL, discount REAL, final_amount REAL, paid_amount REAL DEFAULT 0, due_amount REAL DEFAULT 0, status TEXT, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, staff_id TEXT, receipt_path TEXT, FOREIGN KEY(patient_id) REFERENCES patients(patient_id))");
                    stmt.execute("CREATE TABLE IF NOT EXISTS samples (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT UNIQUE NOT NULL, patient_id TEXT, status TEXT, collection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, staff_id TEXT, FOREIGN KEY(patient_id) REFERENCES patients(patient_id))");
                    stmt.execute("CREATE TABLE IF NOT EXISTS results (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, parameter_id INTEGER, test_id INTEGER, value TEXT, is_abnormal INTEGER, status TEXT, doctor_approval INTEGER DEFAULT 0, pdf_path TEXT, completed_at TIMESTAMP, comment TEXT, lab_notes TEXT, identified_organism TEXT, culture_type TEXT, growth_status TEXT, growth_findings TEXT, duration TEXT, name_override TEXT, unit_override TEXT, range_override TEXT, FOREIGN KEY(sample_id) REFERENCES samples(sample_id))");
                    stmt.execute("CREATE TABLE IF NOT EXISTS test_images (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, test_id INTEGER, image_path TEXT, FOREIGN KEY(sample_id) REFERENCES samples(sample_id), FOREIGN KEY(test_id) REFERENCES tests(id))");

                    System.out.println("[DB-DEBUG] 7. Deploying Module: HR & Inventory...");
                    stmt.execute("CREATE TABLE IF NOT EXISTS staff (id INTEGER PRIMARY KEY AUTOINCREMENT, staff_id TEXT UNIQUE, name TEXT, gender TEXT, address TEXT, qualification TEXT, phone TEXT, dob TEXT, password TEXT, permissions TEXT, designation TEXT, profile_picture TEXT)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT, amount REAL, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, staff_id TEXT)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT UNIQUE NOT NULL, quantity REAL DEFAULT 0, unit TEXT, min_stock_level REAL DEFAULT 0, last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS test_inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, test_id INTEGER, inventory_id INTEGER, usage_quantity REAL, FOREIGN KEY(test_id) REFERENCES tests(id) ON DELETE CASCADE, FOREIGN KEY(inventory_id) REFERENCES inventory(id) ON DELETE CASCADE)");
                    stmt.execute("CREATE TABLE IF NOT EXISTS doctors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, commission_percentage REAL DEFAULT 0.0)");

                    System.out.println("[DB-DEBUG] 8. Executing Integrity Migrations...");
                    // Batch migrations for existing databases
                    String[] cols = {
                        "ALTER TABLE patients ADD COLUMN staff_id TEXT",
                        "ALTER TABLE patients ADD COLUMN title TEXT",
                        "ALTER TABLE patients ADD COLUMN age_months INTEGER DEFAULT 0",
                        "ALTER TABLE tests ADD COLUMN specimen TEXT DEFAULT 'Blood'",
                        "ALTER TABLE test_parameters ADD COLUMN print_order INTEGER DEFAULT 0",
                        "ALTER TABLE results ADD COLUMN lab_notes TEXT",
                        "ALTER TABLE staff ADD COLUMN permissions TEXT"
                    };
                    for (String col : cols) {
                        try { stmt.execute(col); } catch (SQLException e) {}
                    }

                    System.out.println("[DB-DEBUG] 9. Security & Seed Protocol...");
                    // Admin user check
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users")) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            System.out.println("[DB-DEBUG] Seeding default admin account...");
                            stmt.execute("INSERT OR IGNORE INTO users (username, password, role, full_name) VALUES ('admin', 'admin123', 'ADMIN', 'System Administrator')");
                        }
                    }

                    // Seeding check: Force seed if tests are missing
                    boolean needsSeed = false;
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM tests")) {
                        if (rs.next() && rs.getInt(1) == 0) needsSeed = true;
                    }
                    if (needsSeed) {
                        System.out.println("[DB-DEBUG] Database is empty. Seeding clinical protocols...");
                        seedDatabaseFromResource(conn);
                    }
                    
                    System.out.println("[DB-DEBUG] 10. Initialization Complete.");
                }
                System.out.println("Clinical repository validated at v" + com.lab.lms.Main.APP_VERSION);
            } catch (Exception e) {
                System.err.println("[DB-DEBUG] CRITICAL: Initialization aborted: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }


        public static void importExternalDatabase(File sourceFile) throws Exception {
            if (sourceFile == null || !sourceFile.exists()) {
                throw new Exception("Source database file not found.");
            }
            
            java.nio.file.Path sourcePath = sourceFile.toPath();
            java.nio.file.Path targetPath = java.nio.file.Paths.get(getDbPath());
            
            // Ensure parent directory exists
            java.nio.file.Files.createDirectories(targetPath.getParent());
            
            // Critical: Replace the live DB file
            java.nio.file.Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Clean cache to force reload of settings
            settingsCache.clear();
            System.out.println("Database Import Successful: " + targetPath.toAbsolutePath());
        }

        public static void exportTestsToSeedFile() throws Exception {
            JSONArray testsArray = new JSONArray();
            
            try (Connection conn = getConnection()) {
                String testSql = "SELECT * FROM tests ORDER BY id ASC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(testSql)) {
                    while (rs.next()) {
                        JSONObject testObj = new JSONObject();
                        int testId = rs.getInt("id");
                        testObj.put("code", rs.getString("numeric_code"));
                        testObj.put("alpha_code", rs.getString("alpha_code"));
                        testObj.put("name", rs.getString("name"));
                        testObj.put("category", rs.getString("category"));
                        testObj.put("price", rs.getDouble("price"));
                        testObj.put("result_time", rs.getString("result_time"));
                        testObj.put("notes", rs.getString("notes"));
                        testObj.put("is_special", rs.getInt("is_special"));
                        testObj.put("is_microscopic", rs.getInt("is_microscopic"));
                        testObj.put("is_culture", rs.getInt("is_culture"));
                        testObj.put("specimen", rs.getString("specimen"));
                        testObj.put("protocol_class", rs.getString("protocol_class"));
                        
                        // Parameters for this test
                        JSONArray paramsArray = new JSONArray();
                        String paramSql = "SELECT * FROM test_parameters WHERE test_id = ? AND is_global = 1 ORDER BY print_order ASC";
                        try (PreparedStatement pstmt = conn.prepareStatement(paramSql)) {
                            pstmt.setInt(1, testId);
                            try (ResultSet rsP = pstmt.executeQuery()) {
                                while (rsP.next()) {
                                    JSONObject pObj = new JSONObject();
                                    pObj.put("name", rsP.getString("name"));
                                    pObj.put("unit", rsP.getString("unit"));
                                    pObj.put("category", rsP.getString("category"));
                                    pObj.put("range", rsP.getString("min_range")); 
                                    paramsArray.put(pObj);
                                }
                            }
                        }
                        testObj.put("parameters", paramsArray);
                        testsArray.put(testObj);
                    }
                }
            }
            
            File seedFile = new File("src/main/resources/tests_seed.json");
            try (java.io.BufferedWriter bw = java.nio.file.Files.newBufferedWriter(seedFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                bw.write(testsArray.toString(2));
            }
            System.out.println("Master Seed Export Successful (UTF-8): " + seedFile.getAbsolutePath());
        }

        public static void repairClinicalDataEncoding() {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                String[][] fixes = {
                    {"%\uFFFD", "%"},           
                    {"ï¿½", "-"},               
                    {"\uFFFD", ""},              
                    {" ?/L", " 10⁹/L"},         
                    {"?/L", " 10⁹/L"},
                    {"?/uL", "µL"},
                    {"ï¿½L", "µL"}
                };
                
                try (PreparedStatement testFix = conn.prepareStatement("UPDATE tests SET name = REPLACE(name, ?, ?), specimen = REPLACE(specimen, ?, ?), notes = REPLACE(notes, ?, ?)")) {
                    for (String[] fix : fixes) {
                        testFix.setString(1, fix[0]); testFix.setString(2, fix[1]);
                        testFix.setString(3, fix[0]); testFix.setString(4, fix[1]);
                        testFix.setString(5, fix[0]); testFix.setString(6, fix[1]);
                        testFix.executeUpdate();
                    }
                }
                
                try (PreparedStatement paramFix = conn.prepareStatement("UPDATE test_parameters SET name = REPLACE(name, ?, ?), unit = REPLACE(unit, ?, ?), min_range = REPLACE(min_range, ?, ?)")) {
                    for (String[] fix : fixes) {
                        paramFix.setString(1, fix[0]); paramFix.setString(2, fix[1]);
                        paramFix.setString(3, fix[0]); paramFix.setString(4, fix[1]);
                        paramFix.setString(5, fix[0]); paramFix.setString(6, fix[1]);
                        paramFix.executeUpdate();
                    }
                }
                
                conn.commit();
                System.out.println("Clinical Data Repair Cycle Complete.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static void forceSeedDatabase() {
                    try (Connection conn = getConnection()) {
                            forceSeedDatabase(conn);
                    } catch (SQLException e) {
                            e.printStackTrace();
                    }
            }

        public static void forceSeedDatabase(Connection conn) {
                    seedDatabaseFromResource(conn);
        }

        private static void seedDatabaseFromResource(Connection conn) {
            System.out.println("Starting Database Seeding Protocol...");
            try {
                // Ensure WAL mode is active to reduce lock contention during seeding
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL");
                    stmt.execute("PRAGMA busy_timeout=15000"); // Increase timeout for heavy seeding
                } catch (Exception e) {}

                java.io.InputStream is = DatabaseManager.class.getResourceAsStream("/tests_seed.json");
                if (is == null) {
                    System.err.println("Seeding Critical Failure: tests_seed.json NOT FOUND in resources.");
                    return;
                }

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String content = sb.toString();
                if (content.isEmpty()) {
                    System.err.println("Seeding Critical Failure: tests_seed.json IS EMPTY.");
                    return;
                }

                org.json.JSONArray tests = new org.json.JSONArray(content);
                System.out.println("Processing " + tests.length() + " master clinical protocols for update...");

                int testsProcessed = 0;
                for (int i = 0; i < tests.length(); i++) {
                    org.json.JSONObject testObj = tests.getJSONObject(i);
                    String testName = testObj.getString("name");
                    String category = testObj.optString("category", "General");

                    int testId = -1;
                    // Use a shorter scope for finding to release locks faster
                    try (PreparedStatement findPstmt = conn.prepareStatement("SELECT id FROM tests WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))")) {
                        findPstmt.setString(1, testName);
                        try (ResultSet frs = findPstmt.executeQuery()) {
                            if (frs.next()) {
                                testId = frs.getInt(1);
                            }
                        }
                    }

                    if (testId == -1) {
                        String inSql = "INSERT INTO tests (numeric_code, alpha_code, name, category, price, result_time, notes, is_special, is_microscopic, is_culture, specimen, container, fasting, protocol_class) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement inPstmt = conn.prepareStatement(inSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                            inPstmt.setString(1, testObj.optString("code", ""));
                            inPstmt.setString(2, testObj.optString("alpha_code", ""));
                            inPstmt.setString(3, testName);
                            inPstmt.setString(4, category);
                            inPstmt.setDouble(5, testObj.optDouble("price", 0.0));
                            inPstmt.setString(6, testObj.optString("result_time", "1 Day"));
                            inPstmt.setString(7, testObj.optString("notes", ""));
                            inPstmt.setInt(8, testObj.optInt("is_special", 0));
                            inPstmt.setInt(9, testObj.optInt("is_microscopic", 0));
                            inPstmt.setInt(10, testObj.optInt("is_culture", 0));
                            inPstmt.setString(11, testObj.optString("specimen", "Blood"));
                            inPstmt.setString(12, testObj.optString("container", ""));
                            inPstmt.setString(13, testObj.optString("fasting", "No"));
                            inPstmt.setString(14, testObj.optString("protocol_class", "INACTIVE"));
                            inPstmt.executeUpdate();
                            try (ResultSet grs = inPstmt.getGeneratedKeys()) {
                                if (grs.next()) testId = grs.getInt(1);
                            }
                        }
                    }

                    if (testId != -1) {
                        org.json.JSONArray params = testObj.getJSONArray("parameters");
                        String paramInsertSql = "INSERT OR IGNORE INTO test_parameters (test_id, name, unit, category, min_range, max_range, min_range_male, max_range_male, min_range_female, max_range_female, min_range_kids, max_range_kids, print_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pPstmt = conn.prepareStatement(paramInsertSql)) {
                            for (int j = 0; j < params.length(); j++) {
                                org.json.JSONObject pObj = params.getJSONObject(j);
                                pPstmt.setInt(1, testId);
                                pPstmt.setString(2, pObj.getString("name"));
                                pPstmt.setString(3, pObj.optString("unit", "-"));
                                pPstmt.setString(4, pObj.optString("category", ""));
                                
                                String rangeCombined = pObj.optString("range", "");
                                String min = pObj.optString("min_range", "");
                                String max = pObj.optString("max_range", "");
                                
                                if (min.isEmpty() && max.isEmpty() && !rangeCombined.isEmpty()) {
                                    if (rangeCombined.contains("-") || rangeCombined.contains("\u2013")) {
                                        String[] split = rangeCombined.split("[-–]", 2);
                                        min = split[0].trim();
                                        max = split[1].trim();
                                    } else {
                                        min = rangeCombined.trim();
                                    }
                                }

                                pPstmt.setString(5, min.isEmpty() ? "-" : min);
                                pPstmt.setString(6, max);
                                pPstmt.setString(7, pObj.optString("min_range_male", ""));
                                pPstmt.setString(8, pObj.optString("max_range_male", ""));
                                pPstmt.setString(9, pObj.optString("min_range_female", ""));
                                pPstmt.setString(10, pObj.optString("max_range_female", ""));
                                pPstmt.setString(11, pObj.optString("min_range_kids", ""));
                                pPstmt.setString(12, pObj.optString("max_range_kids", ""));
                                pPstmt.setInt(13, pObj.optInt("print_order", 0));
                                pPstmt.addBatch();
                            }
                            pPstmt.executeBatch();
                        }
                        testsProcessed++;
                    }
                }
                System.out.println("Database Seeding Successful: " + testsProcessed + " clinical tests populated.");
            } catch (SQLException e) {
                System.err.println("[SCHEMA ERROR] Seeding failed due to database lock or constraint: " + e.getMessage());
                if (e.getMessage().contains("locked")) {
                    System.err.println("[RECOVERY] Please ensure no other instance of the app is running.");
                }
            } catch (Exception e) {
                System.err.println("Seeding Critical Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public static String getSetting(String key, String defaultValue) {
                // Priority 1: Local configuration override (Non-shared settings)
                if ("multi_device_mode".equals(key) || "server_ip".equals(key) || "is_demo".equals(key)) {
                        return localConfig.getProperty(key, defaultValue);
                }

                if (settingsCache.containsKey(key)) {
                        return settingsCache.get(key);
                }
                try (Connection conn = getConnection()) {
                        PreparedStatement pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = ?");
                        pstmt.setString(1, key);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                                String val = rs.getString("value");
                                settingsCache.put(key, val);
                                return val;
                        }
                } catch (SQLException e) {
                        e.printStackTrace();
                }
                return defaultValue;
        }

        public static void saveSetting(String key, String value) {
                // Priority 1: Local configuration persistence
                if ("multi_device_mode".equals(key) || "server_ip".equals(key) || "is_demo".equals(key)) {
                        localConfig.setProperty(key, value);
                        saveLocalConfig();
                        return;
                }

                settingsCache.put(key, value);
                try (Connection conn = getConnection()) {
                        PreparedStatement pstmt = conn
                                        .prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)");
                        pstmt.setString(1, key);
                        pstmt.setString(2, value);
                        pstmt.executeUpdate();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }

        /**
         * Safely resets clinical transaction data while preserving infrastructure.
         * Keeps: Tests, Parameters, Staff, Licensing, and Report Customization.
         * Clears: Patients, Samples, Results, Invoices, and Branding.
         */
        public static void resetClinicalData() {
                try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                        // 1. Transactional Data Purge
                        stmt.execute("DELETE FROM results");
                        stmt.execute("DELETE FROM samples");
                        stmt.execute("DELETE FROM invoices");
                        stmt.execute("DELETE FROM patients");
                        
                        // 2. Reset Auto-increments for these tables
                        stmt.execute("DELETE FROM sqlite_sequence WHERE name IN ('results', 'samples', 'invoices', 'patients')");

                        // 3. Clear Organization Branding from Settings (Keep identity & logic)
                        String[] brandingKeys = {"lab_name", "lab_address", "lab_contact", "lab_logo"};
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM settings WHERE key = ?")) {
                                for (String key : brandingKeys) {
                                        ps.setString(1, key);
                                        settingsCache.remove(key);
                                        ps.addBatch();
                                }
                                ps.executeBatch();
                        }

                        System.out.println("Clinical Reset Successful: Transactional data wiped, infrastructure preserved.");
                } catch (SQLException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Reset Failure: " + e.getMessage(), e);
                }
        }

}

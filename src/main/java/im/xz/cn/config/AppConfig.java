package im.xz.cn.config;

import im.xz.cn.util.ConfigCipher;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class AppConfig {
    private static AppConfig instance;

    private DatabaseConfig databaseConfig;
    private MailConfig mailConfig;
    private boolean installed;

    public static final String CONFIG_FILE = "sql.yml";
    public static final String INSTALLED_FILE = ".INSTALLED";
    public static final String APP_NAME = "LingYggdrasil";
    public static final String APP_VERSION = loadVersion();
    public static final String APP_REPO = "LingYggdrasil";

    private static String loadVersion() {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("app.version", "unknown");
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    private AppConfig() {
        this.databaseConfig = new DatabaseConfig();
        this.mailConfig = new MailConfig();
        this.installed = false;
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public boolean isInstalled() {
        return Files.exists(Path.of(INSTALLED_FILE));
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {
        this.installed = isInstalled();

        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }

        boolean needsMigration = false;

        try (InputStream is = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> root = yaml.load(is);
            if (root == null) return;

            Map<String, Object> dbMap = (Map<String, Object>) root.get("database");
            if (dbMap != null) {
                if (dbMap.containsKey("type")) databaseConfig.setType(String.valueOf(dbMap.get("type")));
                if (dbMap.containsKey("sqlitePath")) databaseConfig.setSqlitePath(String.valueOf(dbMap.get("sqlitePath")));
                if (dbMap.containsKey("host")) databaseConfig.setHost(String.valueOf(dbMap.get("host")));
                if (dbMap.containsKey("port")) databaseConfig.setPort(((Number) dbMap.get("port")).intValue());
                if (dbMap.containsKey("database")) databaseConfig.setDatabase(String.valueOf(dbMap.get("database")));
                if (dbMap.containsKey("username")) databaseConfig.setUsername(String.valueOf(dbMap.get("username")));
                if (dbMap.containsKey("password")) {
                    String rawDbPassword = String.valueOf(dbMap.get("password"));
                    if (!rawDbPassword.isEmpty() && !ConfigCipher.isEncrypted(rawDbPassword)) {
                        needsMigration = true;
                    }
                    databaseConfig.setPassword(ConfigCipher.decrypt(rawDbPassword));
                }
            }

            Map<String, Object> mailMap = (Map<String, Object>) root.get("mail");
            if (mailMap != null) {
                if (mailMap.containsKey("enabled")) mailConfig.setEnabled((Boolean) mailMap.get("enabled"));
                if (mailMap.containsKey("host")) mailConfig.setHost(String.valueOf(mailMap.get("host")));
                if (mailMap.containsKey("port")) mailConfig.setPort(((Number) mailMap.get("port")).intValue());
                if (mailMap.containsKey("username")) mailConfig.setUsername(String.valueOf(mailMap.get("username")));
                if (mailMap.containsKey("password")) {
                    String rawMailPassword = String.valueOf(mailMap.get("password"));
                    if (!rawMailPassword.isEmpty() && !ConfigCipher.isEncrypted(rawMailPassword)) {
                        needsMigration = true;
                    }
                    mailConfig.setPassword(ConfigCipher.decrypt(rawMailPassword));
                }
                if (mailMap.containsKey("from")) mailConfig.setFrom(String.valueOf(mailMap.get("from")));
                if (mailMap.containsKey("tls")) mailConfig.setTls((Boolean) mailMap.get("tls"));
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }

        if (needsMigration) {
            saveConfig();
        }
    }

    public void saveConfig() {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> dbMap = new LinkedHashMap<>();
        dbMap.put("type", databaseConfig.getType());
        if ("sqlite".equalsIgnoreCase(databaseConfig.getType())) {
            dbMap.put("sqlitePath", databaseConfig.getSqlitePath());
        } else {
            dbMap.put("host", databaseConfig.getHost());
            dbMap.put("port", databaseConfig.getPort());
            dbMap.put("database", databaseConfig.getDatabase());
            dbMap.put("username", databaseConfig.getUsername());
            dbMap.put("password", ConfigCipher.encrypt(databaseConfig.getPassword()));
        }
        root.put("database", dbMap);

        Map<String, Object> mailMap = new LinkedHashMap<>();
        mailMap.put("enabled", mailConfig.isEnabled());
        if (mailConfig.isEnabled()) {
            mailMap.put("host", mailConfig.getHost());
            mailMap.put("port", mailConfig.getPort());
            mailMap.put("username", mailConfig.getUsername());
            mailMap.put("password", ConfigCipher.encrypt(mailConfig.getPassword()));
            mailMap.put("from", mailConfig.getFrom());
            mailMap.put("tls", mailConfig.isTls());
        }
        root.put("mail", mailMap);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            yaml.dump(root, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void createInstalledFile() {
        try {
            Files.createFile(Path.of(INSTALLED_FILE));
            this.installed = true;
        } catch (IOException e) {
            System.err.println("Failed to create installed file: " + e.getMessage());
        }
    }

    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public void setDatabaseConfig(DatabaseConfig databaseConfig) { this.databaseConfig = databaseConfig; }

    public MailConfig getMailConfig() { return mailConfig; }
    public void setMailConfig(MailConfig mailConfig) { this.mailConfig = mailConfig; }
}

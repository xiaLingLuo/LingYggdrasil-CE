package im.xz.cn.util;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.CacheDao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TextureService {
    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final SystemConfig systemConfig;
    private final CacheDao cacheDao;

    public TextureService(SystemConfig systemConfig, CacheDao cacheDao) {
        this.systemConfig = systemConfig;
        this.cacheDao = cacheDao;
    }

    public String computeHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private void validateHash(String hash) {
        if (hash == null || !hash.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("Invalid hash format");
        }
    }

    public static boolean isPng(byte[] data) {
        if (data == null || data.length < PNG_MAGIC.length) return false;
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (data[i] != PNG_MAGIC[i]) return false;
        }
        return true;
    }

    public static boolean hasPngExtension(String filename) {
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".png");
    }

    public File saveFile(String type, String hash, byte[] data) {
        validateHash(hash);
        String dirPath = getStorageDir(type);
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, hash);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
        return file;
    }

    public byte[] readFile(String type, String hash) {
        validateHash(hash);
        File file = new File(getStorageDir(type), hash);
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteFile(String type, String hash) {
        validateHash(hash);
        File file = new File(getStorageDir(type), hash);
        return file.delete();
    }

    public String getPublicUrl(String type, String hash) {
        String domain = systemConfig.getApiDomain();
        if (domain == null || domain.isEmpty()) {
            domain = "http://localhost:35577";
        }
        return domain + "/textures/" + type + "/" + hash;
    }

    public boolean isDownloadAllowed(String type) {
        if ("SKIN".equalsIgnoreCase(type)) {
            return systemConfig.isAllowDownloadSkin();
        } else if ("CAPE".equalsIgnoreCase(type)) {
            return systemConfig.isAllowDownloadCape();
        }
        return false;
    }

    public boolean checkRateLimit(String userId, String type) {
        int rateLimit = getRateLimit(type);
        if (rateLimit < 0) return true;
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String key = "texture_limit:" + userId + ":" + type + ":" + date;
        String current = cacheDao.get(key);
        if (current == null) {
            return true;
        }
        try {
            int count = Integer.parseInt(current);
            return count < rateLimit;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public void recordUpload(String userId, String type) {
        int rateLimit = getRateLimit(type);
        if (rateLimit < 0) return;
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String key = "texture_limit:" + userId + ":" + type + ":" + date;
        String current = cacheDao.get(key);
        int count = 1;
        if (current != null) {
            try {
                count = Integer.parseInt(current) + 1;
            } catch (NumberFormatException ignored) {}
        }
        cacheDao.put(key, String.valueOf(count), "texture_limit", 86400);
    }

    public boolean checkCountLimit(String userId, String type, int currentCount, int maxCount) {
        int limit = getMaxCount(type);
        if (limit < 0) return true;
        return currentCount < limit;
    }

    public boolean checkTotalSizeLimit(String userId, String type, long currentSize, long additionalSize, long maxSize) {
        int limit = getMaxTotalSize(type);
        if (limit < 0) return true;
        return (currentSize + additionalSize) <= (limit * 1024L);
    }

    private String getStorageDir(String type) {
        if ("CAPE".equalsIgnoreCase(type)) {
            return systemConfig.getCapeStoragePath();
        }
        return systemConfig.getSkinStoragePath();
    }

    private int getRateLimit(String type) {
        return "CAPE".equalsIgnoreCase(type) ? systemConfig.getCapeRateLimit() : systemConfig.getSkinRateLimit();
    }

    private int getMaxCount(String type) {
        return "CAPE".equalsIgnoreCase(type) ? systemConfig.getCapeMaxCount() : systemConfig.getSkinMaxCount();
    }

    private int getMaxTotalSize(String type) {
        return "CAPE".equalsIgnoreCase(type) ? systemConfig.getCapeMaxTotalSize() : systemConfig.getSkinMaxTotalSize();
    }

    public int getMaxSize(String type) {
        return "CAPE".equalsIgnoreCase(type) ? systemConfig.getCapeMaxSize() : systemConfig.getSkinMaxSize();
    }
}

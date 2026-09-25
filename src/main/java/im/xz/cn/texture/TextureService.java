/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.texture;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.logging.logApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TextureService {
    public static final long MAX_UPLOAD_BYTES = 16L * 1024L * 1024L;

    private static final logApi log = logApi.getLogger(TextureService.class);
    private static final AtomicInteger PNG_ACTIVE = new AtomicInteger();
    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final SystemConfig systemConfig;
    private final CacheDao cacheDao;

    public enum PngUploadStatus {
        VALID,
        INVALID,
        TOO_LARGE,
        BUSY
    }

    public static final class PngUploadResult implements AutoCloseable {
        private final byte[] data;
        private final String sourceHash;
        private final PngUploadStatus status;
        private boolean permitHeld;

        private PngUploadResult(byte[] data, String sourceHash, PngUploadStatus status, boolean permitHeld) {
            this.data = data;
            this.sourceHash = sourceHash;
            this.status = status;
            this.permitHeld = permitHeld;
        }

        public byte[] data() { return data; }
        public String sourceHash() { return sourceHash; }
        public PngUploadStatus status() { return status; }

        @Override
        public synchronized void close() {
            if (!permitHeld) return;
            permitHeld = false;
            PNG_ACTIVE.decrementAndGet();
        }
    }

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
            log.error("Failed to read file: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean deleteFile(String type, String hash) {
        validateHash(hash);
        File file = new File(getStorageDir(type), hash);
        return file.delete();
    }

    public List<String> listStoredHashes(String type) {
        List<String> hashes = new ArrayList<>();
        File[] files = new File(getStorageDir(type)).listFiles();
        if (files == null) return hashes;
        for (File file : files) {
            if (file.isFile() && file.getName().matches("^[a-f0-9]{64}$")) {
                hashes.add(file.getName());
            }
        }
        return hashes;
    }

    public String getPublicUrl(String type, String hash) {
        String domain = systemConfig.getApiDomain();
        if (domain == null || domain.isEmpty()) {
            domain = systemConfig.getCommonDomain();
        }
        if (domain == null || domain.isEmpty()) {
            domain = "http://localhost:35577";
        }
        while (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        String url = domain + "/textures/" + type + "/" + hash;
        log.debug("[TextureService] getPublicUrl: type={}, apiDomain={}, commonDomain={} -> {}",
                type, systemConfig.getApiDomain(), systemConfig.getCommonDomain(), url);
        return url;
    }

    public boolean isDownloadAllowed(String type) {
        if ("SKIN".equalsIgnoreCase(type)) {
            return systemConfig.isAllowDownloadSkin();
        } else if ("CAPE".equalsIgnoreCase(type)) {
            return systemConfig.isAllowDownloadCape();
        }
        return false;
    }

    public boolean tryRecordUpload(String userId, String type) {
        int rateLimit = getRateLimit(type);
        if (rateLimit < 0) return true;
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        String key = "texture_limit:" + userId + ":" + type + ":" + date;
        return cacheDao.incrementAndGet(key, "texture_limit", 86400) <= rateLimit;
    }

    public long getMaxUploadBytes(String type) {
        long configuredBytes = Math.max(0L, getMaxSize(type)) * 1024L;
        return Math.min(configuredBytes, MAX_UPLOAD_BYTES);
    }

    public PngUploadResult readAndNormalizePng(InputStream input, String type) throws IOException {
        if (!tryAcquirePngSlot()) {
            return new PngUploadResult(null, null, PngUploadStatus.BUSY, false);
        }
        boolean permitTransferred = false;
        try {
            int maxBytes = (int) getMaxUploadBytes(type);
            byte[] data = input.readNBytes(maxBytes + 1);
            if (data.length > maxBytes) {
                return new PngUploadResult(null, null, PngUploadStatus.TOO_LARGE, false);
            }
            if (maxBytes == 0) {
                return new PngUploadResult(null, null, PngUploadStatus.TOO_LARGE, false);
            }
            if (!isPng(data)) {
                return new PngUploadResult(null, null, PngUploadStatus.INVALID, false);
            }
            String sourceHash = computeHash(data);
            if (!systemConfig.isPngValidationEnabled()) {
                PngUploadResult result = new PngUploadResult(data, sourceHash, PngUploadStatus.VALID, true);
                permitTransferred = true;
                return result;
            }
            PngNormalizer.Result normalized = new PngNormalizer.Builder()
                    .maxFileSize(maxBytes)
                    .maxChunkSize((long) systemConfig.getPngMaxChunkSizeKib() * 1024L)
                    .maxWidth(systemConfig.getPngMaxWidth())
                    .maxHeight(systemConfig.getPngMaxHeight())
                    .maxPixels(systemConfig.getPngMaxPixels())
                    .strictChunkMode(systemConfig.isPngStrictChunkMode())
                    .build()
                    .normalize(data);
            if (!normalized.isSuccess()) {
                return new PngUploadResult(null, sourceHash, PngUploadStatus.INVALID, false);
            }
            PngUploadResult result = new PngUploadResult(normalized.getPngData(), sourceHash, PngUploadStatus.VALID, true);
            permitTransferred = true;
            return result;
        } finally {
            if (!permitTransferred) PNG_ACTIVE.decrementAndGet();
        }
    }

    private static boolean tryAcquirePngSlot() {
        int max = Math.max(1, SystemConfig.getInstance().getPngMaxConcurrent());
        if (PNG_ACTIVE.incrementAndGet() > max) {
            PNG_ACTIVE.decrementAndGet();
            return false;
        }
        return true;
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

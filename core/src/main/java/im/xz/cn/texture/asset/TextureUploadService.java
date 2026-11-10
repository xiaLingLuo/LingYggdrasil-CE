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
package im.xz.cn.texture.asset;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.texture.TextureService;
import im.xz.cn.texture.library.LibraryEntryRepository;
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;

import java.io.InputStream;
import java.time.Instant;
import java.util.Locale;


public final class TextureUploadService {

    public record UploadResult(long assetId, long uploadId, String hash, String url,
                               int width, int height) {
    }

    public static final class UploadException extends RuntimeException {
        private final String code;

        public UploadException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final DatabaseManager database;
    private final TextureService textureService;
    private final TextureBlobRepository blobs;
    private final TextureAssetRepository assets;
    private final TextureUploadRepository uploads;
    private final LibraryEntryRepository library;
    private final im.xz.cn.infrastructure.persistence.UnitOfWork unitOfWork;


    public TextureUploadService(DatabaseManager database, TextureService textureService) {
        this.database = database;
        this.textureService = textureService;
        this.blobs = new TextureBlobRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.library = new LibraryEntryRepository(database);
        this.unitOfWork = new im.xz.cn.infrastructure.persistence.UnitOfWork(database);
    }

    public UploadResult upload(String userId, String type, String fileName, InputStream pngStream) {
        if (userId == null || userId.isBlank()) {
            throw new UploadException("UNAUTHORIZED", "Login required");
        }
        TextureType textureType;
        try {
            textureType = TextureType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (RuntimeException error) {
            throw new UploadException("INVALID_ARGUMENT", "type must be SKIN or CAPE");
        }
        String safeName = sanitizeFileName(fileName, textureType);
        try (TextureService.PngUploadResult result =
                     textureService.readAndNormalizePng(pngStream, textureType.name())) {
            switch (result.status()) {
                case VALID -> {
                    
                }
                case TOO_LARGE -> throw new UploadException("PAYLOAD_TOO_LARGE", "Texture file too large");
                case BUSY -> throw new UploadException("BUSY", "Texture pipeline busy, retry later");
                default -> throw new UploadException("INVALID_ARGUMENT", "Invalid PNG texture");
            }
            return persist(userId, textureType, safeName, result.data());
        } catch (UploadException error) {
            throw error;
        } catch (Exception error) {
            throw new UploadException("INVALID_ARGUMENT", "Upload failed: " + error.getMessage());
        }
    }

    
    private static String sanitizeFileName(String raw, TextureType type) {
        String fallback = type.name().toLowerCase(Locale.ROOT) + ".png";
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return fallback;
        }
        if (name.length() > 200) {
            name = name.substring(0, 200);
        }
        return name;
    }

    private synchronized UploadResult persist(String userId, TextureType type, String fileName, byte[] data) {
        String hash = textureService.computeHash(data);
        if (textureService.isHashBlacklisted(hash)) {
            throw new UploadException("HASH_BLOCKED", "此纹理已被管理员列入纹理黑名单");
        }
        int[] dimensions = readPngDimensions(data, type);
        Instant now = Instant.now();

        
        var reusableBlob = blobs.findBySha256(hash);
        if (reusableBlob.isPresent()) {
            var reusableAsset = assets.findByTypeAndBlob(type, reusableBlob.get().id());
            if (reusableAsset.isPresent()) {
                var existing = uploads.findByUserAndAsset(userId, reusableAsset.get().id());
                if (existing.isPresent() && existing.get().active()
                        && reusableAsset.get().state() == TextureAsset.AssetState.READY) {
                    TextureUpload upload = existing.get();
                    return new UploadResult(reusableAsset.get().id(), upload.id(), hash,
                            textureService.getPublicUrl(type.name(), hash),
                            reusableAsset.get().width(), reusableAsset.get().height());
                }
            }
        }

        enforceQuota(userId, type, data.length);
        boolean hadBlob = reusableBlob.isPresent();
        textureService.saveFile(type.name(), hash, data);
        try {
            return unitOfWork.execute(connection -> {
                TextureBlob blob = blobs.findBySha256(hash).orElseGet(() ->
                        blobs.findById(blobs.insert(new TextureBlob(0L, hash, data.length, "image/png",
                                textureService.getStoragePath(type.name(), hash), BlobState.READY, now)))
                                .orElseThrow());
                TextureAsset asset = assets.findByTypeAndBlob(type, blob.id()).orElseGet(() ->
                        assets.findById(assets.createReady(blob.id(), type, dimensions[0], dimensions[1],
                                fileName, null, now)).orElseThrow());
                TextureUpload upload = uploads.findByUserAndAsset(userId, asset.id()).orElseGet(() ->
                        uploads.grant(userId, asset.id(), fileName, now));
                if (!upload.active()) {
                    upload = uploads.grant(userId, asset.id(), fileName, now);
                }
                library.ensure(userId, asset.id(), now);
                return new UploadResult(asset.id(), upload.id(), hash,
                        textureService.getPublicUrl(type.name(), hash), dimensions[0], dimensions[1]);
            });
        } catch (RuntimeException error) {
            
            if (!hadBlob) {
                try {
                    textureService.deleteFile(type.name(), hash);
                } catch (RuntimeException ignored) {
                    
                }
            }
            throw new UploadException("UPLOAD_FAILED", "Failed to persist texture: " + error.getMessage());
        }
    }

    private void enforceQuota(String userId, TextureType type, long additionalBytes) {
        long activeCount = im.xz.cn.infrastructure.persistence.Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_uploads u JOIN texture_assets a ON a.id = u.asset_id"
                        + " WHERE u.user_id = ? AND u.state = 'ACTIVE' AND a.type = ?",
                userId, type.name());
        if (!textureService.checkCountLimit(userId, type.name(), (int) activeCount, 0)) {
            throw new UploadException("QUOTA_EXCEEDED", "Texture count limit reached");
        }
        long currentBytes = im.xz.cn.infrastructure.persistence.Jdbc.queryCount(database,
                "SELECT COALESCE(SUM(b.size_bytes), 0) FROM texture_uploads u"
                        + " JOIN texture_assets a ON a.id = u.asset_id"
                        + " JOIN texture_blobs b ON b.id = a.blob_id"
                        + " WHERE u.user_id = ? AND u.state = 'ACTIVE' AND a.type = ?",
                userId, type.name());
        if (!textureService.checkTotalSizeLimit(userId, type.name(), currentBytes, additionalBytes, 0)) {
            throw new UploadException("QUOTA_EXCEEDED", "Texture storage limit reached");
        }
    }

    
    static int[] readPngDimensions(byte[] data, TextureType type) {
        try {
            if (data.length >= 24 && data[12] == 'I' && data[13] == 'H' && data[14] == 'D'
                    && data[15] == 'R') {
                int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16)
                        | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
                int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16)
                        | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            }
        } catch (RuntimeException ignored) {
            
        }
        return type == TextureType.CAPE ? new int[]{64, 32} : new int[]{64, 64};
    }
}

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
package im.xz.cn.server.handler.user;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import io.javalin.http.Context;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;


public class UserTextureFileHandler {

    private final TextureBlobRepository blobs;
    private final TextureAssetRepository assets;
    private final PublicationRepository publications;
    private final TextureUploadRepository uploads;
    private final im.xz.cn.texture.sharing.ShareGrantRepository grants;

    public UserTextureFileHandler(DatabaseManager database) {
        this.blobs = new TextureBlobRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.publications = new PublicationRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.grants = new im.xz.cn.texture.sharing.ShareGrantRepository(database);
    }

    public void serve(Context ctx) {
        TextureType type;
        try {
            type = TextureType.valueOf(String.valueOf(ctx.pathParam("type")).toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException error) {
            ctx.status(404);
            return;
        }
        String hash = ctx.pathParam("hash");
        if (hash == null || !hash.matches("[a-fA-F0-9]{64}")) {
            ctx.status(404);
            return;
        }
        Optional<TextureBlob> blob = blobs.findBySha256(hash.toLowerCase(java.util.Locale.ROOT));
        if (blob.isEmpty() || blob.get().state() != im.xz.cn.texture.asset.BlobState.READY) {
            ctx.status(404);
            return;
        }
        Optional<TextureAsset> asset = assets.findByTypeAndBlob(type, blob.get().id());
        if (asset.isEmpty()
                || asset.get().state() != TextureAsset.AssetState.READY
                || !isVisible(ctx, asset.get())) {
            ctx.status(404);
            return;
        }
        byte[] data = readBytes(blob.get(), type, hash);
        if (data == null) {
            ctx.status(404);
            return;
        }
        ctx.contentType("image/png");
        ctx.header("Cache-Control", "public, max-age=3600");
        ctx.result(data);
    }

    public void serveFace(Context ctx) {
        TextureType type;
        try {
            type = TextureType.valueOf(String.valueOf(ctx.pathParam("type")).toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException error) {
            ctx.status(404);
            return;
        }
        if (type != TextureType.SKIN) {
            ctx.status(404);
            return;
        }
        String hash = ctx.pathParam("hash");
        if (hash == null || !hash.matches("[a-fA-F0-9]{64}")) {
            ctx.status(404);
            return;
        }
        Optional<TextureBlob> blob = blobs.findBySha256(hash.toLowerCase(java.util.Locale.ROOT));
        if (blob.isEmpty() || blob.get().state() != im.xz.cn.texture.asset.BlobState.READY) {
            ctx.status(404);
            return;
        }
        Optional<TextureAsset> asset = assets.findByTypeAndBlob(type, blob.get().id());
        if (asset.isEmpty() || asset.get().state() != TextureAsset.AssetState.READY) {
            ctx.status(404);
            return;
        }
        byte[] data = readBytes(blob.get(), type, hash);
        if (data == null) {
            ctx.status(404);
            return;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
            if (source == null || source.getWidth() < 64) {
                ctx.status(404);
                return;
            }
            int scale = Math.max(1, source.getWidth() / 64);
            BufferedImage face = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = face.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, 0, 0, 64, 64,
                    8 * scale, 8 * scale, 16 * scale, 16 * scale, null);
            if (source.getWidth() >= 64 * scale && source.getHeight() >= 64 * scale) {
                graphics.drawImage(source, 0, 0, 64, 64,
                        40 * scale, 8 * scale, 48 * scale, 16 * scale, null);
            }
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(face, "png", output);
            ctx.contentType("image/png");
            ctx.header("Cache-Control", "public, max-age=3600");
            ctx.result(output.toByteArray());
        } catch (Exception error) {
            ctx.status(404);
        }
    }

    private boolean isVisible(Context ctx, TextureAsset asset) {
        if (publications.hasAvailableForAsset(asset.id())) {
            return true;
        }
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            return false;
        }
        return uploads.hasActiveUpload(userId, asset.id())
                || grants.hasActiveForRecipient(userId, asset.id());
    }

    private static byte[] readBytes(TextureBlob blob, TextureType type, String hash) {
        try {
            if (blob.storageKey() != null && !blob.storageKey().isBlank()) {
                File file = new File(blob.storageKey());
                if (file.isFile()) {
                    return Files.readAllBytes(file.toPath());
                }
            }
        } catch (Exception ignored) {
            
        }
        return null;
    }
}

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
package im.xz.cn.texture.storage;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.texture.TextureService;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public final class TextureStorageReconciliationService {

    public record Report(int unregisteredFiles, int unreferencedBlobs, int missingFiles,
                         List<String> sampleUnregisteredHashes) {

        public Report {
            sampleUnregisteredHashes = List.copyOf(sampleUnregisteredHashes);
        }
    }

    private final DatabaseManager database;
    private final TextureService textureService;

    public TextureStorageReconciliationService(DatabaseManager database, TextureService textureService) {
        this.database = database;
        this.textureService = textureService;
    }

    public Report scan() {
        Set<String> knownHashes = new LinkedHashSet<>();
        int unreferencedBlobs = 0;
        int missingFiles = 0;
        for (BlobRow row : Jdbc.queryMany(database, resultSet -> new BlobRow(
                        resultSet.getString("sha256"),
                        resultSet.getString("storage_key"),
                        resultSet.getLong("asset_refs")),
                "SELECT b.sha256, b.storage_key,"
                        + " (SELECT COUNT(*) FROM texture_assets a WHERE a.blob_id = b.id) AS asset_refs"
                        + " FROM texture_blobs b")) {
            knownHashes.add(row.sha256());
            if (row.assetRefs() == 0) {
                unreferencedBlobs++;
            }
            if (row.storagePath() != null && !row.storagePath().isBlank()
                    && !new File(row.storagePath()).exists()) {
                missingFiles++;
            }
        }
        addLegacyHashes(knownHashes);
        List<String> unregistered = new ArrayList<>(storedHashes());
        unregistered.removeAll(knownHashes);
        return new Report(unregistered.size(), unreferencedBlobs, missingFiles,
                unregistered.size() > 20 ? unregistered.subList(0, 20) : unregistered);
    }

    
    public int deleteUnregisteredFiles() {
        Set<String> knownHashes = new LinkedHashSet<>();
        Jdbc.queryMany(database, resultSet -> resultSet.getString(1),
                "SELECT sha256 FROM texture_blobs").forEach(knownHashes::add);
        addLegacyHashes(knownHashes);
        int deleted = 0;
        for (String hash : storedHashes()) {
            if (!knownHashes.contains(hash) && textureService.deleteFile("SKIN", hash)) {
                deleted++;
            }
        }
        return deleted;
    }

    
    private void addLegacyHashes(Set<String> knownHashes) {
        try {
            Jdbc.queryMany(database, resultSet -> resultSet.getString(1),
                    "SELECT DISTINCT hash FROM textures").forEach(knownHashes::add);
        } catch (RuntimeException ignored) {
            
        }
    }

    
    private Set<String> storedHashes() {
        Set<String> hashes = new LinkedHashSet<>();
        for (String type : List.of("SKIN", "CAPE")) {
            hashes.addAll(textureService.listStoredHashes(type));
        }
        return hashes;
    }

    private record BlobRow(String sha256, String storagePath, long assetRefs) {
    }
}

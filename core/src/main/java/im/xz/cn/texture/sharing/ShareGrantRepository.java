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
package im.xz.cn.texture.sharing;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class ShareGrantRepository {

    private static final Jdbc.RowMapper<TextureShareGrant> MAPPER = resultSet -> new TextureShareGrant(
            resultSet.getLong("id"),
            resultSet.getLong("owner_upload_id"),
            resultSet.getString("owner_id"),
            resultSet.getString("recipient_id"),
            resultSet.getLong("asset_id"),
            GrantState.parse(resultSet.getString("state")),
            Jdbc.readInstant(resultSet, "created_at"),
            Jdbc.readInstant(resultSet, "ended_at")
    );

    private final DatabaseManager database;

    public ShareGrantRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<TextureShareGrant> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_share_grants WHERE id = ?", id));
    }

    public Optional<TextureShareGrant> findActive(String ownerId, String recipientId, long assetId) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_share_grants WHERE owner_id = ? AND recipient_id = ?"
                        + " AND asset_id = ? AND state = 'ACTIVE'",
                ownerId, recipientId, assetId));
    }

    public boolean hasActiveForRecipient(String recipientId, long assetId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_share_grants WHERE recipient_id = ?"
                        + " AND asset_id = ? AND state = 'ACTIVE'", recipientId, assetId) > 0;
    }

    public List<TextureShareGrant> findActiveByRecipient(String recipientId) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM texture_share_grants WHERE recipient_id = ? AND state = 'ACTIVE'"
                        + " ORDER BY id DESC", recipientId);
    }

    public List<TextureShareGrant> findActiveByOwner(String ownerId) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM texture_share_grants WHERE owner_id = ? AND state = 'ACTIVE'"
                        + " ORDER BY id DESC", ownerId);
    }

    
    public TextureShareGrant create(long ownerUploadId, String ownerId, String recipientId, long assetId, Instant now) {
        Optional<TextureShareGrant> existing = findActive(ownerId, recipientId, assetId);
        if (existing.isPresent()) {
            return existing.get();
        }
        long id = Jdbc.insert(database,
                "INSERT INTO texture_share_grants (owner_upload_id, owner_id, recipient_id,"
                        + " asset_id, state, created_at) VALUES (?, ?, ?, ?, 'ACTIVE', ?)",
                ownerUploadId, ownerId, recipientId, assetId, Jdbc.writeInstant(now));
        return findById(id).orElseThrow();
    }

    
    public TextureShareGrant revokeByOwner(String ownerId, long grantId, Instant now) {
        TextureShareGrant grant = requireGrant(grantId);
        if (!grant.ownerId().equals(ownerId)) {
            throw new SecurityException("grant does not belong to owner: " + grantId);
        }
        if (grant.state() != GrantState.ACTIVE) {
            return grant;
        }
        Jdbc.update(database,
                "UPDATE texture_share_grants SET state = 'OWNER_REVOKED', ended_at = ? WHERE id = ?",
                Jdbc.writeInstant(now), grantId);
        return findById(grantId).orElseThrow();
    }

    
    public TextureShareGrant removeByRecipient(String recipientId, long grantId, Instant now) {
        TextureShareGrant grant = requireGrant(grantId);
        if (!grant.recipientId().equals(recipientId)) {
            throw new SecurityException("grant does not belong to recipient: " + grantId);
        }
        if (grant.state() != GrantState.ACTIVE) {
            return grant;
        }
        Jdbc.update(database,
                "UPDATE texture_share_grants SET state = 'RECEIVER_REMOVED', ended_at = ? WHERE id = ?",
                Jdbc.writeInstant(now), grantId);
        return findById(grantId).orElseThrow();
    }

    
    public int revokeAllForAsset(String ownerId, long assetId, String reason, Instant now) {
        int affected = 0;
        for (TextureShareGrant grant : findActiveByOwner(ownerId)) {
            if (grant.assetId() != assetId) {
                continue;
            }
            revokeByOwner(ownerId, grant.id(), now);
            affected++;
        }
        return affected;
    }

    
    public int revokeAllActiveForAsset(long assetId, Instant now) {
        return Jdbc.update(database,
                "UPDATE texture_share_grants SET state = 'OWNER_REVOKED', ended_at = ?"
                        + " WHERE asset_id = ? AND state = 'ACTIVE'",
                Jdbc.writeInstant(now), assetId);
    }

    public long countActiveForAsset(long assetId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_share_grants WHERE asset_id = ? AND state = 'ACTIVE'", assetId);
    }

    private TextureShareGrant requireGrant(long grantId) {
        return findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("grant not found: " + grantId));
    }
}

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
package im.xz.cn.texture.publication;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class PublicationRepository {

    private static final Jdbc.RowMapper<TexturePublication> MAPPER = resultSet -> new TexturePublication(
            resultSet.getLong("id"),
            resultSet.getString("publisher_id"),
            resultSet.getLong("asset_id"),
            PublicationState.parse(resultSet.getString("state")),
            Jdbc.readInstant(resultSet, "first_published_at"),
            Jdbc.readInstant(resultSet, "last_published_at"),
            Jdbc.readInstant(resultSet, "withdrawn_at"),
            resultSet.getLong("version")
    );

    private final DatabaseManager database;

    public PublicationRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<TexturePublication> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_publications WHERE id = ?", id));
    }

    public Optional<TexturePublication> findByPublisherAndAsset(String publisherId, long assetId) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_publications WHERE publisher_id = ? AND asset_id = ?",
                publisherId, assetId));
    }

    public List<TexturePublication> findAvailableByPublisher(String publisherId, int limit, int offset) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM texture_publications WHERE publisher_id = ? AND state = 'PUBLIC'"
                        + " ORDER BY last_published_at DESC, id DESC LIMIT ? OFFSET ?",
                publisherId, limit, offset);
    }

    public List<TexturePublication> findAvailablePage(int limit, int offset) {
        return findAvailablePage(limit, offset, null, false);
    }

    
    public List<TexturePublication> findAvailablePage(int limit, int offset, String type, boolean hot) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.* FROM texture_publications p"
                        + " JOIN texture_assets a ON a.id = p.asset_id"
                        + " WHERE p.state = 'PUBLIC' AND a.state = 'READY'");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (type != null && !type.isBlank()) {
            sql.append(" AND a.type = ?");
            params.add(type.toUpperCase(java.util.Locale.ROOT));
        }
        if (hot) {
            sql.append(" ORDER BY ((SELECT COUNT(*) FROM publication_likes l WHERE l.publication_id = p.id)"
                    + " + 5 * (SELECT COUNT(*) FROM publication_favorites f WHERE f.publication_id = p.id)) DESC,"
                    + " p.last_published_at DESC, p.id DESC");
        } else {
            sql.append(" ORDER BY p.last_published_at DESC, p.id DESC");
        }
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.queryMany(database, MAPPER, sql.toString(), params.toArray());
    }

    public long countAvailable(String type) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM texture_publications p"
                + " JOIN texture_assets a ON a.id = p.asset_id"
                + " WHERE p.state = 'PUBLIC' AND a.state = 'READY'");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (type != null && !type.isBlank()) {
            sql.append(" AND a.type = ?");
            params.add(type.toUpperCase(java.util.Locale.ROOT));
        }
        return Jdbc.queryCount(database, sql.toString(), params.toArray());
    }

    
    public TexturePublication publish(String publisherId, long assetId, Instant now) {
        Optional<TexturePublication> existing = findByPublisherAndAsset(publisherId, assetId);
        if (existing.isEmpty()) {
            long id = Jdbc.insert(database,
                    "INSERT INTO texture_publications (publisher_id, asset_id, state,"
                            + " first_published_at, last_published_at, version)"
                            + " VALUES (?, ?, 'PUBLIC', ?, ?, 1)",
                    publisherId, assetId, Jdbc.writeInstant(now), Jdbc.writeInstant(now));
            return findById(id).orElseThrow();
        }
        TexturePublication publication = existing.get();
        if (publication.state() == PublicationState.PUBLIC) {
            return publication;
        }
        Jdbc.update(database,
                "UPDATE texture_publications SET state = 'PUBLIC', last_published_at = ?,"
                        + " withdrawn_at = NULL, version = version + 1 WHERE id = ?",
                Jdbc.writeInstant(now), publication.id());
        return findById(publication.id()).orElseThrow();
    }

    
    public TexturePublication withdraw(long publicationId, Instant now) {
        TexturePublication publication = findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("publication not found: " + publicationId));
        if (publication.state() != PublicationState.PUBLIC) {
            return publication;
        }
        Jdbc.update(database,
                "UPDATE texture_publications SET state = 'PRIVATE', withdrawn_at = ?,"
                        + " version = version + 1 WHERE id = ?",
                Jdbc.writeInstant(now), publicationId);
        return findById(publicationId).orElseThrow();
    }

    public int updateState(long publicationId, PublicationState state, Instant now) {
        return Jdbc.update(database,
                "UPDATE texture_publications SET state = ?, withdrawn_at = ?, version = version + 1"
                        + " WHERE id = ?",
                state.name(), Jdbc.writeInstant(now), publicationId);
    }

    public long countAvailable() {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_publications WHERE state = 'PUBLIC'");
    }

    public long countAvailableByPublisher(String publisherId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_publications WHERE publisher_id = ? AND state = 'PUBLIC'",
                publisherId);
    }

    
    public boolean hasAvailableForAsset(long assetId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_publications p"
                        + " JOIN texture_assets a ON a.id = p.asset_id"
                        + " WHERE p.asset_id = ? AND p.state = 'PUBLIC' AND a.state = 'READY'",
                assetId) > 0;
    }

    public List<TexturePublication> findAvailableByAsset(long assetId) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM texture_publications WHERE asset_id = ? AND state = 'PUBLIC'"
                        + " ORDER BY last_published_at DESC, id DESC", assetId);
    }
}

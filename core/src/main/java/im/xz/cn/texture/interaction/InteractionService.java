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
package im.xz.cn.texture.interaction;

import im.xz.cn.infrastructure.persistence.UnitOfWork;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;

import java.time.Clock;
import java.time.Instant;


public final class InteractionService {

    private final UnitOfWork unitOfWork;
    private final PublicationRepository publications;
    private final InteractionRepository interactions;
    private final Clock clock;

    public InteractionService(UnitOfWork unitOfWork,
                              PublicationRepository publications,
                              InteractionRepository interactions) {
        this(unitOfWork, publications, interactions, Clock.systemUTC());
    }

    public InteractionService(UnitOfWork unitOfWork,
                              PublicationRepository publications,
                              InteractionRepository interactions,
                              Clock clock) {
        this.unitOfWork = unitOfWork;
        this.publications = publications;
        this.interactions = interactions;
        this.clock = clock;
    }

    public InteractionCounts like(String userId, long publicationId) {
        return unitOfWork.execute(connection -> {
            requireAvailable(publicationId);
            interactions.like(userId, publicationId, clock.instant());
            return interactions.counts(publicationId);
        });
    }

    public InteractionCounts unlike(String userId, long publicationId) {
        return unitOfWork.execute(connection -> {
            requireExisting(publicationId);
            interactions.unlike(userId, publicationId);
            return interactions.counts(publicationId);
        });
    }

    public InteractionCounts favorite(String userId, long publicationId) {
        return unitOfWork.execute(connection -> {
            requireAvailable(publicationId);
            if (!interactions.hasFavorite(userId, publicationId)) {
                int limit = im.xz.cn.config.SystemConfig.getInstance().getMaxFavorites();
                if (limit >= 0 && interactions.countFavoritesByUser(userId) >= limit) {
                    throw new IllegalStateException("favorite limit reached");
                }
            }
            interactions.favorite(userId, publicationId, clock.instant());
            return interactions.counts(publicationId);
        });
    }

    public InteractionCounts unfavorite(String userId, long publicationId) {
        return unitOfWork.execute(connection -> {
            requireExisting(publicationId);
            interactions.unfavorite(userId, publicationId);
            return interactions.counts(publicationId);
        });
    }

    public boolean likedBy(String userId, long publicationId) {
        return interactions.hasLike(userId, publicationId);
    }

    public boolean favoritedBy(String userId, long publicationId) {
        return interactions.hasFavorite(userId, publicationId);
    }

    public InteractionCounts counts(long publicationId) {
        return interactions.counts(publicationId);
    }

    private void requireAvailable(long publicationId) {
        TexturePublication publication = requireExisting(publicationId);
        if (!publication.available()) {
            throw new IllegalStateException("publication is not available for new interactions: " + publicationId);
        }
    }

    private TexturePublication requireExisting(long publicationId) {
        return publications.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("publication not found: " + publicationId));
    }

    
    public Instant now() {
        return clock.instant();
    }
}

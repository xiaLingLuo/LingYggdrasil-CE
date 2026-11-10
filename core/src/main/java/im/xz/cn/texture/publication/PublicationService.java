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

import im.xz.cn.infrastructure.persistence.UnitOfWork;
import im.xz.cn.texture.library.LibraryEntryRepository;
import im.xz.cn.texture.library.TextureUploadRepository;

import java.time.Clock;
import java.time.Instant;


public final class PublicationService {

    private final UnitOfWork unitOfWork;
    private final PublicationRepository publications;
    private final TextureUploadRepository uploads;
    private final LibraryEntryRepository library;
    private final Clock clock;

    public PublicationService(UnitOfWork unitOfWork,
                              PublicationRepository publications,
                              TextureUploadRepository uploads,
                              LibraryEntryRepository library) {
        this(unitOfWork, publications, uploads, library, Clock.systemUTC());
    }

    public PublicationService(UnitOfWork unitOfWork,
                              PublicationRepository publications,
                              TextureUploadRepository uploads,
                              LibraryEntryRepository library,
                              Clock clock) {
        this.unitOfWork = unitOfWork;
        this.publications = publications;
        this.uploads = uploads;
        this.library = library;
        this.clock = clock;
    }

    public TexturePublication publish(String publisherId, long assetId) {
        if (!uploads.hasActiveUpload(publisherId, assetId)) {
            throw new IllegalStateException("publish requires an active upload qualification");
        }
        Instant now = clock.instant();
        return unitOfWork.execute(connection -> {
            library.ensure(publisherId, assetId, now);
            return publications.publish(publisherId, assetId, now);
        });
    }

    public TexturePublication withdraw(String publisherId, long publicationId) {
        Instant now = clock.instant();
        return unitOfWork.execute(connection -> {
            TexturePublication publication = publications.findById(publicationId)
                    .orElseThrow(() -> new IllegalArgumentException("publication not found: " + publicationId));
            if (!publication.publisherId().equals(publisherId)) {
                throw new SecurityException("publication does not belong to publisher: " + publicationId);
            }
            return publications.withdraw(publicationId, now);
        });
    }
}

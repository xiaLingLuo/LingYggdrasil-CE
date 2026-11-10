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
package im.xz.cn.texture.access;

import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;
import im.xz.cn.texture.sharing.ShareGrantRepository;

import java.util.EnumSet;
import java.util.Optional;


public final class TextureAccessService {

    private final TextureUploadRepository uploads;
    private final PublicationRepository publications;
    private final ShareGrantRepository grants;

    public TextureAccessService(TextureUploadRepository uploads,
                                PublicationRepository publications,
                                ShareGrantRepository grants) {
        this.uploads = uploads;
        this.publications = publications;
        this.grants = grants;
    }

    public TextureAccess evaluate(String userId, long assetId) {
        boolean hasUpload = uploads.hasActiveUpload(userId, assetId);
        boolean hasGrant = grants.hasActiveForRecipient(userId, assetId);
        boolean publicAvailable = publications.hasAvailableForAsset(assetId);
        return build(hasUpload, hasGrant, publicAvailable);
    }

    
    public TextureAccess evaluatePublication(String userId, long publicationId) {
        Optional<TexturePublication> publication = publications.findById(publicationId);
        if (publication.isEmpty()) {
            return TextureAccess.denied(false);
        }
        long assetId = publication.get().assetId();
        boolean hasUpload = uploads.hasActiveUpload(userId, assetId);
        boolean hasGrant = grants.hasActiveForRecipient(userId, assetId);
        boolean publicationAvailable = publication.get().available();
        boolean publicAvailable = publicationAvailable || publications.hasAvailableForAsset(assetId);
        return build(hasUpload, hasGrant, publicAvailable);
    }

    private static TextureAccess build(boolean hasUpload, boolean hasGrant, boolean publicAvailable) {
        EnumSet<AccessSource> sources = EnumSet.noneOf(AccessSource.class);
        if (hasUpload) {
            sources.add(AccessSource.UPLOAD);
        }
        if (hasGrant) {
            sources.add(AccessSource.SHARE_GRANT);
        }
        if (publicAvailable) {
            sources.add(AccessSource.PUBLIC);
        }
        return new TextureAccess(!sources.isEmpty(), publicAvailable, sources);
    }
}

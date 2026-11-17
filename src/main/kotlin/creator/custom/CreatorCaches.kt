/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.creator.custom.providers.RemoteTemplateProvider
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.creator.custom.types.ArchitecturyVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.FabricGameVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.FabricVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.ForgeVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.GradlePluginSelectorCreatorProperty
import com.demonwav.mcdev.creator.custom.types.MavenArtifactVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.NeoForgeVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.PaperVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.ParchmentCreatorProperty
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.annotations.TestOnly

object CreatorCaches {

    /**
     * Forgets everything the creator downloaded during this JVM's lifetime, so the next creator downloads versions and
     * template repos again, as it would after an IDE restart. Deletes the downloaded template repos from disk too.
     */
    @TestOnly
    fun clear() {
        ArchitecturyVersionsCreatorProperty.clearCache()
        FabricGameVersionCreatorProperty.clearCache()
        FabricVersionsCreatorProperty.clearCache()
        ForgeVersionsCreatorProperty.clearCache()
        GradlePluginSelectorCreatorProperty.clearCache()
        MavenArtifactVersionCreatorProperty.clearCache()
        NeoForgeVersionsCreatorProperty.clearCache()
        PaperVersionCreatorProperty.clearCache()
        ParchmentCreatorProperty.clearCache()

        for (key in TemplateProvider.getAllKeys()) {
            (TemplateProvider.get(key) as? RemoteTemplateProvider)?.clearCache()
        }

        val templatesDir = RemoteTemplateProvider.RemoteTemplateRepo.templatesBaseDir
        NioFiles.deleteRecursively(templatesDir)
        // Drop the VFS's view of the deleted zips, so a new zip with the same name isn't read from stale data
        VfsUtil.markDirtyAndRefresh(false, true, true, templatesDir.toFile())
    }
}

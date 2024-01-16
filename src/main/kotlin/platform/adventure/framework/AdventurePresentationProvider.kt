/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.adventure.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.adventure.AdventureConstants
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE
import java.util.jar.Manifest

class AdventurePresentationProvider : LibraryPresentationProvider<DummyLibraryProperties>(ADVENTURE_LIBRARY_KIND) {
    override fun getIcon(properties: DummyLibraryProperties?) = PlatformAssets.ADVENTURE_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): DummyLibraryProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            if (findUsingManifest(manifest)) {
                return DummyLibraryProperties.INSTANCE
            }
        }

        return null
    }

    private fun findUsingManifest(manifest: Manifest): Boolean =
        manifest[SPECIFICATION_TITLE] == AdventureConstants.API_SPECIFICATION_TITLE ||
            manifest["Automatic-Module-Name"] == AdventureConstants.API_MODULE_ID
}

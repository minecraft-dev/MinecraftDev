/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.sponge.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE
import java.util.jar.Attributes.Name.SPECIFICATION_VERSION

class SpongePresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(SPONGE_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.SPONGE_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            loop@ for (title in setOf("SpongeAPI", "spongeapi")) {
                val versionAttribute = when (title) {
                    manifest[IMPLEMENTATION_TITLE] -> IMPLEMENTATION_VERSION
                    manifest[SPECIFICATION_TITLE] -> SPECIFICATION_VERSION
                    else -> continue@loop
                }

                val version = manifest[versionAttribute] ?: continue
                return LibraryVersionProperties(version)
            }
        }
        return null
    }
}

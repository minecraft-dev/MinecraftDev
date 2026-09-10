/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.demonwav.mcdev.facet.MinecraftLibraryDetector
import com.demonwav.mcdev.facet.hasLibraryManifest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.get
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE
import java.util.jar.Attributes.Name.SPECIFICATION_VERSION

class SpongeLibraryDetector : MinecraftLibraryDetector {
    override val platformType = PlatformType.SPONGE

    override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        hasLibraryManifest(scope) { manifest ->
            loop@ for (title in setOf("SpongeAPI", "spongeapi")) {
                val versionAttribute = when (title) {
                    manifest[IMPLEMENTATION_TITLE] -> IMPLEMENTATION_VERSION
                    manifest[SPECIFICATION_TITLE] -> SPECIFICATION_VERSION
                    else -> continue@loop
                }

                if (manifest[versionAttribute] != null) {
                    return@hasLibraryManifest true
                }
            }
            false
        }
}

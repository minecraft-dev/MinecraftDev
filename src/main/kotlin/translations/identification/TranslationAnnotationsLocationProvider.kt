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

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.platform.mcp.framework.MCP_LIBRARY_KIND
import com.intellij.codeInsight.externalAnnotation.location.AnnotationsLocation
import com.intellij.codeInsight.externalAnnotation.location.AnnotationsLocationProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager

class TranslationAnnotationsLocationProvider : AnnotationsLocationProvider {
    override fun getLocations(
        project: Project,
        library: Library,
        artifactId: String?,
        groupId: String?,
        version: String?
    ): Collection<AnnotationsLocation> {
        val isMinecraftLibrary = LibraryPresentationManager.getInstance().isLibraryOfKind(
            library.getFiles(OrderRootType.CLASSES).toList(),
            MCP_LIBRARY_KIND
        )
        if (isMinecraftLibrary) {
            return listOf(TranslationExternalAnnotationsArtifactsResolver.Util.fakeMavenLocation)
        }
        return emptyList()
    }
}

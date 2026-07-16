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

package com.demonwav.mcdev.platform.velocity.framework

import com.demonwav.mcdev.facet.ClassMinecraftLibraryDetector
import com.demonwav.mcdev.facet.hasLibraryFile
import com.demonwav.mcdev.facet.readLibraryText
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class VelocityLibraryDetector : ClassMinecraftLibraryDetector(
    PlatformType.VELOCITY,
    "com.velocitypowered.api.proxy.ProxyServer",
) {
    private val annotationProcessorsPath = "META-INF/services/javax.annotation.processing.Processor"

    override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        super.isLibraryPresent(project, scope) ||
            hasLibraryFile("javax.annotation.processing.Processor", annotationProcessorsPath, scope) { file ->
                file.readLibraryText()?.lineSequence()?.any {
                    it.trim() == "com.velocitypowered.api.plugin.ap.PluginAnnotationProcessor"
                } == true
            }
}

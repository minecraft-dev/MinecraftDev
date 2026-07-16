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

package com.demonwav.mcdev.platform.mixin.framework

import com.demonwav.mcdev.facet.MinecraftLibraryDetector
import com.demonwav.mcdev.facet.hasLibraryFile
import com.demonwav.mcdev.facet.hasLibraryManifest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.get
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class MixinLibraryDetector : MinecraftLibraryDetector {
    override val platformType = PlatformType.MIXIN

    private val hintFilePath = "META-INF/services/org.spongepowered.asm.service.IMixinService"

    override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        hasLibraryManifest(scope) { it["Agent-Class"] == MixinConstants.Classes.MIXIN_AGENT } ||
            hasLibraryFile("org.spongepowered.asm.service.IMixinService", hintFilePath, scope)
}

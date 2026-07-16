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

package com.demonwav.mcdev.platform.adventure.framework

import com.demonwav.mcdev.facet.MinecraftLibraryDetector
import com.demonwav.mcdev.facet.hasLibraryManifest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.adventure.AdventureConstants
import com.demonwav.mcdev.util.get
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE

class AdventureLibraryDetector : MinecraftLibraryDetector {
    override val platformType = PlatformType.ADVENTURE

    override fun isLibraryPresent(project: Project, scope: GlobalSearchScope): Boolean =
        hasLibraryManifest(scope) { manifest ->
        manifest[SPECIFICATION_TITLE] == AdventureConstants.API_SPECIFICATION_TITLE ||
            manifest["Automatic-Module-Name"] == AdventureConstants.API_MODULE_ID
        }
}

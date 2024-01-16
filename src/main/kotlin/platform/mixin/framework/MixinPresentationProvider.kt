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

package com.demonwav.mcdev.platform.mixin.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION

class MixinPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(MIXIN_LIBRARY_KIND) {

    private val hintFilePath = "META-INF/services/org.spongepowered.asm.service.IMixinService"

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.MIXIN_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest
            if (manifest?.get("Agent-Class") != MixinConstants.Classes.MIXIN_AGENT &&
                classesRoot.findFileByRelativePath(hintFilePath) == null
            ) {
                continue
            }

            return LibraryVersionProperties(manifest?.get(IMPLEMENTATION_VERSION))
        }
        return null
    }
}

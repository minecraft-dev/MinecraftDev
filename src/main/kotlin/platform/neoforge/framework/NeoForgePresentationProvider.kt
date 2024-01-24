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

package com.demonwav.mcdev.platform.neoforge.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.neoforge.util.NeoForgeConstants
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile

class NeoForgePresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(NEOFORGE_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.NEOFORGE_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            if (JarUtil.containsClass(classesRoot.localFile, NeoForgeConstants.MOD_ANNOTATION)) {
                return LibraryVersionProperties()
            }
        }
        return null
    }
}

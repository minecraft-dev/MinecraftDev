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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library

interface LibraryModIdProvider {
    fun getModId(project: Project, library: Library): String?

    companion object {
        val EP_NAME = ExtensionPointName<LibraryModIdProvider>("com.demonwav.minecraft-dev.libraryModIdProvider")

        fun getModId(project: Project, library: Library): String? {
            return EP_NAME.extensionList.mapFirstNotNull { it.getModId(project, library) }
        }
    }
}

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

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.io.FileNotFoundException

class VfsLoadedTemplate(
    val templateRoot: VirtualFile,
    override val label: String,
    override val tooltip: String? = null,
    override val descriptor: TemplateDescriptor,
    override val isValid: Boolean,
) : LoadedTemplate {

    override fun loadTemplateContents(path: String): String? {
        templateRoot.refresh(false, true)
        val virtualFile = templateRoot.findFileByRelativePath(path)
            ?: throw FileNotFoundException("Could not find file $path in template root ${templateRoot.path}")
        virtualFile.refresh(false, false)
        return virtualFile.readText()
    }
}

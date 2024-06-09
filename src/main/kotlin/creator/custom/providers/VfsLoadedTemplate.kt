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
import com.google.gson.Gson
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.io.FileNotFoundException

class VfsLoadedTemplate(
    val repoRoot: VirtualFile,
    val templateRoot: VirtualFile,
    val descriptorFile: VirtualFile,
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

    data class Serialized(
        val repoRoot: String,
        val templateRoot: String,
        val descriptorPath: String
    ) {
        fun load(modalityState: ModalityState): LoadedTemplate? {
            val fs = if (repoRoot.contains(JarFileSystem.JAR_SEPARATOR)) {
                JarFileSystem.getInstance()
            } else {
                LocalFileSystem.getInstance()
            }
            val repoRoot = fs.refreshAndFindFileByPath(repoRoot)
                ?: return null
            val templateRoot = fs.refreshAndFindFileByPath(templateRoot)
                ?: return null
            val descriptorFile = fs.refreshAndFindFileByPath(descriptorPath)
                ?: return null
            val tooltip = descriptorFile.path

            val bundle = TemplateProvider.loadMessagesBundle(modalityState, repoRoot)
            return TemplateProvider.createVfsLoadedTemplate(
                modalityState,
                repoRoot,
                templateRoot,
                descriptorFile,
                tooltip,
                bundle
            )
        }
    }

    override fun serialize(): String? {
        return Gson().toJson(Serialized(repoRoot.path, templateRoot.path, descriptorFile.path))
    }
}

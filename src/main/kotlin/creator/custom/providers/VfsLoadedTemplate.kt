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
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.readText
import com.intellij.util.xmlb.XmlSerializer
import java.io.FileNotFoundException
import org.jdom.Element

class VfsLoadedTemplate(
    val root: VirtualFile,
    val descriptorFile: VirtualFile,
    override val label: String,
    override val tooltip: String? = null,
    override val descriptor: TemplateDescriptor,
    override val isValid: Boolean,
) : LoadedTemplate {

    override fun loadTemplateContents(path: String): String? {
        root.refresh(false, true)
        val virtualFile = root.findFileByRelativePath(path)
            ?: throw FileNotFoundException("Could not find file $path in template root ${root.path}")
        virtualFile.refresh(false, false)
        return virtualFile.readText()
    }

    data class Serialized(
        val root: String,
        val descriptorPath: String
    ) {
        fun load(): LoadedTemplate? {
            val fs = if (root.contains(JarFileSystem.JAR_SEPARATOR)) {
                JarFileSystem.getInstance()
            } else {
                LocalFileSystem.getInstance()
            }
            val rootFile = fs.refreshAndFindFileByPath(root)
                ?: return null
            val descriptorFile = fs.refreshAndFindFileByPath(descriptorPath)
                ?: return null
            val tooltip = descriptorFile.path

            return TemplateProvider.createVfsLoadedTemplate(rootFile, descriptorFile, tooltip)
        }
    }

    override fun serialize(): String? {
        return Gson().toJson(Serialized(root.path, descriptorFile.path))
    }

    companion object {

        fun deserialize(element: Element): VfsLoadedTemplate? = runCatching {
            val serialized = XmlSerializer.deserialize(element, Serialized::class.java)

            val rootFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(serialized.root)
                ?: return null
            val descriptorFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(serialized.descriptorPath)
                ?: return null

            TemplateProvider.createVfsLoadedTemplate(rootFile, descriptorFile)
        }.getOrLogException(logger<VfsLoadedTemplate>())
    }
}

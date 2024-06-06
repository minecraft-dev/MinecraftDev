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
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.util.function.Consumer
import javax.swing.JComponent

/**
 * Extensions responsible for creating a [TemplateDescriptor] based on whatever data it is provided in its [UI][setupUi].
 */
interface TemplateProvider {

    fun getLabel(): String

    fun getTooltip(): String? = null

    fun init(indicator: ProgressIndicator) = Unit

    fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent?

    fun deserializeAndLoad(element: String): LoadedTemplate?

    companion object {

        private val EP_NAME = ExtensionPointName<TemplateProvider>("com.demonwav.minecraft-dev.creatorTemplateProvider")

        fun get(name: String): TemplateProvider? {
            return getAll().find { it.javaClass.name == name }
        }

        fun getAll(): Collection<TemplateProvider> = EP_NAME.extensionList

        fun findTemplates(
            directory: VirtualFile,
            templates: MutableList<VfsLoadedTemplate> = mutableListOf(),
        ): List<VfsLoadedTemplate> {
            directory.refresh(false, true)
            for (child in directory.children) {
                if (child.isDirectory) {
                    findTemplates(child, templates)
                } else if (child.name.endsWith(".mcdev.template.json")) {
                    try {
                        createVfsLoadedTemplate(directory, child)?.let(templates::add)
                    } catch (t: Throwable) {
                        if (t is ControlFlowException) {
                            throw t
                        }

                        val attachment = runCatching { Attachment(child.name, child.readText()) }.getOrNull()
                        if (attachment != null) {
                            thisLogger().error("Failed to load template ${child.path}", t, attachment)
                        } else {
                            thisLogger().error("Failed to load template ${child.path}", t)
                        }
                    }
                }
            }

            return templates
        }

        fun createVfsLoadedTemplate(
            root: VirtualFile,
            descriptorFile: VirtualFile,
            tooltip: String? = null
        ): VfsLoadedTemplate? {
            root.refresh(false, true)
            descriptorFile.refresh(false, false)

            var descriptor = Gson().fromJson<TemplateDescriptor>(descriptorFile.readText())
            if (descriptor.version != 1) {
                thisLogger().warn("Cannot handle template ${descriptorFile.path} of version ${descriptor.version}")
                return null
            }

            if (descriptor.hidden == true) {
                return null
            }

            val label = descriptor.label
                ?: descriptorFile.name.removeSuffix(".mcdev.template.json").takeIf(String::isNotBlank)
                ?: root.presentableName

            if (descriptor.inherit != null) {
                val parent = root.findFileByRelativePath(descriptor.inherit!!)
                if (parent != null) {
                    parent.refresh(false, false)
                    val parentDescriptor = Gson().fromJson<TemplateDescriptor>(parent.readText())
                    val mergedProperties = parentDescriptor.properties.orEmpty() + descriptor.properties.orEmpty()
                    val mergedFiles = parentDescriptor.files.orEmpty() + descriptor.files.orEmpty()
                    descriptor = descriptor.copy(properties = mergedProperties, files = mergedFiles)
                } else {
                    thisLogger().error(
                        "Could not find inherited template descriptor ${descriptor.inherit} from ${descriptorFile.path}"
                    )
                }
            }

            return VfsLoadedTemplate(root, descriptorFile, label, tooltip, descriptor, true)
        }

        fun deserializeAndLoadVfs(element: String): LoadedTemplate? {
            val serialized = Gson().fromJson<VfsLoadedTemplate.Serialized>(element)
            return serialized.load()
        }
    }
}

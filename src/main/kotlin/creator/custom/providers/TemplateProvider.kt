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
import com.demonwav.mcdev.creator.custom.TemplateResourceBundle
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.refreshSync
import com.google.gson.Gson
import com.intellij.DynamicBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.util.ResourceBundle
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

    fun deserializeAndLoad(element: String, modalityState: ModalityState): LoadedTemplate?

    companion object {

        private val EP_NAME = ExtensionPointName<TemplateProvider>("com.demonwav.minecraft-dev.creatorTemplateProvider")

        fun get(name: String): TemplateProvider? {
            return getAll().find { it.javaClass.name == name }
        }

        fun getAll(): Collection<TemplateProvider> = EP_NAME.extensionList

        fun findTemplates(
            modalityState: ModalityState,
            repoRoot: VirtualFile,
            directory: VirtualFile = repoRoot,
            templates: MutableList<VfsLoadedTemplate> = mutableListOf(),
            bundle: ResourceBundle? = loadMessagesBundle(modalityState, repoRoot)
        ): List<VfsLoadedTemplate> {
            directory.refreshSync(modalityState)
            for (child in directory.children) { // TODO use visitor instead of loop
                ProgressManager.checkCanceled()
                if (child.isDirectory) {
                    findTemplates(modalityState, repoRoot, child, templates, bundle)
                } else if (child.name.endsWith(".mcdev.template.json")) {
                    try {
                        createVfsLoadedTemplate(modalityState, repoRoot, directory, child, bundle = bundle)?.let(
                            templates::add
                        )
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

        fun loadMessagesBundle(modalityState: ModalityState, repoRoot: VirtualFile): ResourceBundle? = try {
            val locale = DynamicBundle.getLocale()
            // Simplified bundle resolution, but covers all the most common cases
            val baseBundle = doLoadMessageBundle(
                repoRoot.findChild("messages.properties"),
                modalityState,
                null
            )
            val languageBundle = doLoadMessageBundle(
                repoRoot.findChild("messages_${locale.language}.properties"),
                modalityState,
                baseBundle
            )
            doLoadMessageBundle(
                repoRoot.findChild("messages_${locale.language}_${locale.country}.properties"),
                modalityState,
                languageBundle
            )
        } catch (t: Throwable) {
            if (t is ControlFlowException) {
                throw t
            }

            thisLogger().error("Failed to load resource bundle of template repository ${repoRoot.path}", t)
            null
        }

        private fun doLoadMessageBundle(
            file: VirtualFile?,
            modalityState: ModalityState,
            parent: ResourceBundle?
        ): ResourceBundle? {
            if (file == null) {
                return parent
            }

            try {
                file.refreshSync(modalityState)
                return file.inputStream.reader().use { TemplateResourceBundle(it, parent) }
            } catch (t: Throwable) {
                if (t is ControlFlowException) {
                    return parent
                }

                thisLogger().error("Failed to load resource bundle ${file.path}", t)
            }

            return parent
        }

        fun createVfsLoadedTemplate(
            modalityState: ModalityState,
            repoRoot: VirtualFile,
            templateRoot: VirtualFile,
            descriptorFile: VirtualFile,
            tooltip: String? = null,
            bundle: ResourceBundle? = null
        ): VfsLoadedTemplate? {
            descriptorFile.refreshSync(modalityState)
            var descriptor = Gson().fromJson<TemplateDescriptor>(descriptorFile.readText())
            if (descriptor.version != 1) {
                thisLogger().warn("Cannot handle template ${descriptorFile.path} of version ${descriptor.version}")
                return null
            }

            if (descriptor.hidden == true) {
                return null
            }

            descriptor.bundle = bundle

            val labelKey = descriptor.label
                ?: descriptorFile.name.removeSuffix(".mcdev.template.json").takeIf(String::isNotBlank)
                ?: templateRoot.presentableName
            val label =
                descriptor.translateOrNull("platform.${labelKey.lowercase()}.label") ?: descriptor.translate(labelKey)

            if (descriptor.inherit != null) {
                val parent = templateRoot.findFileByRelativePath(descriptor.inherit!!)
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

            if (bundle != null) {
                descriptor.properties?.forEach { property ->
                    property.bundle = bundle
                }
            }

            return VfsLoadedTemplate(repoRoot, templateRoot, descriptorFile, label, tooltip, descriptor, true)
        }

        fun deserializeAndLoadVfs(element: String, modalityState: ModalityState): LoadedTemplate? {
            val serialized = Gson().fromJson<VfsLoadedTemplate.Serialized>(element)
            return serialized.load(modalityState)
        }
    }
}

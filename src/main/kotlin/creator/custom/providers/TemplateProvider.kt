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

import com.demonwav.mcdev.MinecraftSettings
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
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import java.util.ResourceBundle
import javax.swing.JComponent

/**
 * Extensions responsible for creating a [TemplateDescriptor] based on whatever data it is provided in its configuration
 * [UI][setupConfigUi].
 */
interface TemplateProvider {

    val label: String

    val hasConfig: Boolean

    fun init(indicator: ProgressIndicator, repos: List<MinecraftSettings.TemplateRepo>) = Unit

    fun loadTemplates(context: WizardContext, repo: MinecraftSettings.TemplateRepo): Collection<LoadedTemplate>

    fun setupConfigUi(data: String, dataSetter: (String) -> Unit): JComponent?

    companion object {

        private val EP_NAME =
            ExtensionPointName<TemplateProviderBean>("com.demonwav.minecraft-dev.creatorTemplateProvider")
        private val COLLECTOR = KeyedExtensionCollector<TemplateProvider, String>(EP_NAME)

        fun get(key: String): TemplateProvider? = COLLECTOR.findSingle(key)

        fun getAllKeys() = EP_NAME.extensionList.mapNotNull { it.key }

        fun findTemplates(
            modalityState: ModalityState,
            repoRoot: VirtualFile,
            templates: MutableList<VfsLoadedTemplate> = mutableListOf(),
            bundle: ResourceBundle? = loadMessagesBundle(modalityState, repoRoot)
        ): List<VfsLoadedTemplate> {
            val visitor = object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isFile || !file.name.endsWith(".mcdev.template.json")) {
                        return true
                    }

                    try {
                        createVfsLoadedTemplate(modalityState, file.parent, file, bundle = bundle)
                            ?.let(templates::add)
                    } catch (t: Throwable) {
                        if (t is ControlFlowException) {
                            throw t
                        }

                        val attachment = runCatching { Attachment(file.name, file.readText()) }.getOrNull()
                        if (attachment != null) {
                            thisLogger().error("Failed to load template ${file.path}", t, attachment)
                        } else {
                            thisLogger().error("Failed to load template ${file.path}", t)
                        }
                    }

                    return true
                }
            }
            VfsUtilCore.visitChildrenRecursively(repoRoot, visitor)
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
                return file.refreshSync(modalityState)
                    ?.inputStream?.reader()?.use { TemplateResourceBundle(it, parent) }
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
            templateRoot: VirtualFile,
            descriptorFile: VirtualFile,
            tooltip: String? = null,
            bundle: ResourceBundle? = null
        ): VfsLoadedTemplate? {
            descriptorFile.refreshSync(modalityState)
            var descriptor = Gson().fromJson<TemplateDescriptor>(descriptorFile.readText())
            if (descriptor.version != TemplateDescriptor.FORMAT_VERSION) {
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

            return VfsLoadedTemplate(templateRoot, label, tooltip, descriptor, true)
        }
    }
}

class TemplateProviderBean : BaseKeyedLazyInstance<TemplateProvider>(), KeyedLazyInstance<TemplateProvider> {

    @Attribute("key")
    @RequiredElement
    lateinit var name: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getKey(): String? = name

    override fun getImplementationClassName(): String? = implementation
}

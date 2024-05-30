package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.PropertyGraph
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
            for (child in directory.children) {
                if (child.isDirectory) {
                    findTemplates(child, templates)
                } else if (child.name.endsWith(".mcdev.template.json")) {
                    try {
                        templates.add(createVfsLoadedTemplate(directory, child))
                    } catch (e: Throwable) {
                        val attachment = runCatching { Attachment(child.name, child.readText()) }.getOrNull()
                        if (attachment != null) {
                            thisLogger().error("Failed to load template ${child.path}", e, attachment)
                        } else {
                            thisLogger().error("Failed to load template ${child.path}", e)
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
        ): VfsLoadedTemplate {
            root.refresh(false, true)
            descriptorFile.refresh(false, false)

            val descriptor = Gson().fromJson<TemplateDescriptor>(descriptorFile.readText())
            val label = descriptorFile.name.removeSuffix(".mcdev.template.json").takeIf(String::isNotBlank)
                ?: root.presentableName
            return VfsLoadedTemplate(root, descriptorFile, label, tooltip, descriptor, true)
        }

        fun deserializeAndLoadVfs(element: String): LoadedTemplate? {
            val serialized = Gson().fromJson<VfsLoadedTemplate.Serialized>(element)
            return serialized.load()
        }
    }
}

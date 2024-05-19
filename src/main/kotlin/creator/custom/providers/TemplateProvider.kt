package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.util.projectWizard.WizardContext
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

    companion object {

        private val EP_NAME = ExtensionPointName<TemplateProvider>("com.demonwav.minecraft-dev.creatorTemplateProvider")

        fun getAll(): Collection<TemplateProvider> = EP_NAME.extensionList

        fun findTemplates(
            directory: VirtualFile,
            templates: MutableList<VfsLoadedTemplate> = mutableListOf(),
        ): List<VfsLoadedTemplate> {
            for (child in directory.children) {
                if (child.isDirectory) {
                    findTemplates(child, templates)
                } else if (child.name.endsWith(".mcdev.template.json")) {
                    val descriptor = Gson().fromJson<TemplateDescriptor>(child.readText())
                    val label = child.name.removeSuffix(".mcdev.template.json").takeIf(String::isNotBlank)
                        ?: directory.presentableName
                    templates.add(VfsLoadedTemplate(directory, label, null, descriptor, true))
                }
            }

            return templates
        }
    }
}

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.PropertyGraph
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
        provideTemplate: Consumer<() -> LoadedTemplate>
    ): JComponent

    companion object {

        private val EP_NAME = ExtensionPointName<TemplateProvider>("com.demonwav.minecraft-dev.creatorTemplateProvider")

        fun getAll(): Collection<TemplateProvider> = EP_NAME.extensionList

        fun setupAllUi(
            context: WizardContext,
            propertyGraph: PropertyGraph,
            provideTemplate: Consumer<() -> LoadedTemplate>
        ) {
            EP_NAME.forEachExtensionSafe { extension ->
                extension.setupUi(context, propertyGraph, provideTemplate)
            }
        }
    }
}

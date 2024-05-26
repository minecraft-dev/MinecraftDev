package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.virtualFile
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import java.util.function.Consumer
import javax.swing.JComponent

class BuiltInTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Built In"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent? {
        provideTemplate.accept {
            val builtinTemplatesPath = PluginUtil.plugin.pluginPath.resolve("lib/resources/builtin-templates")
            builtinTemplatesPath.virtualFile?.let(TemplateProvider::findTemplates) ?: emptyList()
        }

        return null
    }

    override fun deserializeAndLoad(element: String): LoadedTemplate? = TemplateProvider.deserializeAndLoadVfs(element)
}

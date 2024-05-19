package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.update.PluginUtil
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.absolutePathString

class BuiltInTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Built In"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> LoadedTemplate>
    ): JComponent? {
        provideTemplate.accept {
            val builtinTemplatesPath = PluginUtil.plugin.pluginPath.resolve("lib/resources/templates.zip")
            ZipTemplateProvider.loadTemplateFrom(builtinTemplatesPath.absolutePathString())
        }

        return null
    }
}

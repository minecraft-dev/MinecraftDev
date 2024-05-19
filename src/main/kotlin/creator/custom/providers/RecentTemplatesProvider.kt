package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.model.RecentProjectTemplates
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import java.util.function.Consumer
import javax.swing.JComponent

class RecentTemplatesProvider : TemplateProvider {

    override fun getLabel(): String = "Recent"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent? {
        provideTemplate.accept {
            RecentProjectTemplates.instance.state.templates.mapNotNull { (provider, element) ->
                TemplateProvider.get(provider)?.deserializeAndLoad(element)
            }
        }
        return null
    }

    override fun deserializeAndLoad(element: String): LoadedTemplate =
        throw UnsupportedOperationException("The recent templates provider is not supposed to deserialize nor load")
}

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.JdkComboBoxWithPreference
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.CreatorJdk
import com.demonwav.mcdev.creator.jdkComboBoxWithPreference
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.ui.dsl.builder.Panel

class JdkCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<CreatorJdk>(graph, descriptor, properties) {

    private lateinit var jdkComboBox: JdkComboBoxWithPreference

    override fun createDefaultValue(raw: Any?): CreatorJdk = CreatorJdk(null)

    override fun serialize(value: CreatorJdk): String = value.sdk?.homePath ?: ""

    override fun deserialize(string: String): CreatorJdk =
        CreatorJdk(ProjectJdkTable.getInstance().allJdks.find { it.homePath == string })

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            val sdkProperty = graphProperty.transform(CreatorJdk::sdk, ::CreatorJdk)
            jdkComboBox = this.jdkComboBoxWithPreference(context, sdkProperty, descriptor.name).component

            val minVersionPropName = descriptor.default as? String
            if (minVersionPropName != null) {
                val minVersionProperty = properties[minVersionPropName]
                    ?: throw RuntimeException("Could not find property $minVersionPropName referenced by default value of property ${descriptor.name}")

                jdkComboBox.setPreferredJdk(JavaSdkVersion.entries[minVersionProperty.graphProperty.get() as Int])
                minVersionProperty.graphProperty.afterPropagation {
                    jdkComboBox.setPreferredJdk(JavaSdkVersion.entries[minVersionProperty.graphProperty.get() as Int])
                }
            }
        }
    }
}

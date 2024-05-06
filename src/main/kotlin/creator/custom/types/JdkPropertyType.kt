package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.JdkComboBoxWithPreference
import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.creator.custom.model.CreatorJdk
import com.demonwav.mcdev.creator.jdkComboBoxWithPreference
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.ui.dsl.builder.Panel

class JdkPropertyType : PropertyType<CreatorJdk> {

    override fun createDefaultValue(raw: Any?): CreatorJdk = CreatorJdk(null)

    override fun serialize(value: CreatorJdk): String = value.sdk?.homePath ?: ""

    override fun deserialize(string: String): CreatorJdk =
        CreatorJdk(ProjectJdkTable.getInstance().allJdks.find { it.homePath == string })

    override fun Panel.buildUi(context: WizardContext, graphProperty: GraphProperty<CreatorJdk>, property: TemplateProperty) {
        val preferredVersion = property.default as? Int ?: 17
        lateinit var jdkComboBox: JdkComboBoxWithPreference
        row(property.label) {
            val sdkProperty = graphProperty.transform(CreatorJdk::sdk, ::CreatorJdk)
            jdkComboBox = jdkComboBoxWithPreference(context, sdkProperty, property.name).component
            jdkComboBox.setPreferredJdk(JavaSdkVersion.entries[preferredVersion])
        }
    }
}

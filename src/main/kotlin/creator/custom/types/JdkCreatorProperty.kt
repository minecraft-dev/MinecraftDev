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
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<CreatorJdk>(descriptor, graph, properties, CreatorJdk::class.java) {

    private lateinit var jdkComboBox: JdkComboBoxWithPreference

    override fun createDefaultValue(raw: Any?): CreatorJdk = CreatorJdk(null)

    override fun serialize(value: CreatorJdk): String = value.sdk?.homePath ?: ""

    override fun deserialize(string: String): CreatorJdk =
        CreatorJdk(ProjectJdkTable.getInstance().allJdks.find { it.homePath == string })

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            val sdkProperty = graphProperty.transform(CreatorJdk::sdk, ::CreatorJdk)
            jdkComboBox = this.jdkComboBoxWithPreference(context, sdkProperty, descriptor.name).component

            val minVersionPropName = descriptor.default as? String
            if (minVersionPropName != null) {
                val minVersionProperty = properties[minVersionPropName]
                    ?: throw RuntimeException(
                        "Could not find property $minVersionPropName referenced" +
                            " by default value of property ${descriptor.name}"
                    )

                jdkComboBox.setPreferredJdk(JavaSdkVersion.entries[minVersionProperty.graphProperty.get() as Int])
                minVersionProperty.graphProperty.afterPropagation {
                    jdkComboBox.setPreferredJdk(JavaSdkVersion.entries[minVersionProperty.graphProperty.get() as Int])
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = JdkCreatorProperty(descriptor, graph, properties)
    }
}

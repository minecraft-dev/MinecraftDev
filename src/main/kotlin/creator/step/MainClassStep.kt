/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.updateWhenChanged
import com.demonwav.mcdev.creator.whenStepAvailable
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class MainClassStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private fun suggestMainClassName(): String {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()

        if (buildSystemProps.artifactId.contains('.')) {
            // if the artifact id is invalid, don't confuse ourselves by copying its dots
            return className
        }

        return buildSystemProps.groupId.toPackageName() +
            "." + buildSystemProps.artifactId.toPackageName() +
            "." + findStep<AbstractModNameStep>().name.toJavaClassName()
    }

    private fun suggestGroupId(): String {
        val parts = className.split('.').dropLast(2)
        return if (parts.isEmpty()) {
            findStep<BuildSystemPropertiesStep<*>>().groupId
        } else {
            parts.joinToString(".")
        }
    }

    val classNameProperty = propertyGraph.lazyProperty(::suggestMainClassName)
    var className by classNameProperty

    init {
        whenStepAvailable<BuildSystemPropertiesStep<*>> { buildSystemStep ->
            classNameProperty.updateWhenChanged(buildSystemStep.groupIdProperty, ::suggestMainClassName)
            classNameProperty.updateWhenChanged(buildSystemStep.artifactIdProperty, ::suggestMainClassName)
        }
        whenStepAvailable<AbstractModNameStep> { modNameStep ->
            classNameProperty.updateWhenChanged(modNameStep.nameProperty, ::suggestMainClassName)
        }
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(MCDevBundle("creator.ui.main_class.label")) {
                textField()
                    .columns(COLUMNS_LARGE)
                    .bindText(classNameProperty)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, className)
    }

    companion object {
        val KEY = Key.create<String>("${MainClassStep::class.java.name}.className")
    }
}

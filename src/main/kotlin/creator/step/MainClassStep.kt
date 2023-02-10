/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

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
    private fun suggestMainClassName() = findStep<BuildSystemPropertiesStep<*>>().groupId.toPackageName() +
        ".${findStep<BuildSystemPropertiesStep<*>>().artifactId.toPackageName()}" +
        ".${findStep<AbstractModNameStep>().name.toJavaClassName()}"

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

            buildSystemStep.groupIdProperty.updateWhenChanged(classNameProperty, ::suggestGroupId)
        }
        whenStepAvailable<AbstractModNameStep> { modNameStep ->
            classNameProperty.updateWhenChanged(modNameStep.nameProperty, ::suggestMainClassName)
        }
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Main Class:") {
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

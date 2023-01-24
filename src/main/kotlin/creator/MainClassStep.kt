/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText

class MainClassStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private fun suggestMainClassName() = findStep<BuildSystemPropertiesStep<*>>().groupId.toPackageName() +
        ".${findStep<BuildSystemPropertiesStep<*>>().artifactId.toPackageName()}" +
        ".${findStep<AbstractModNameStep>().name.toJavaClassName()}"

    val classNameProperty = propertyGraph.lazyProperty(::suggestMainClassName)
    var className by classNameProperty
    init {
        whenStepAvailable<BuildSystemPropertiesStep<*>> { buildSystemStep ->
            buildSystemStep.groupIdProperty.afterChange { className = suggestMainClassName() }
            buildSystemStep.artifactIdProperty.afterChange { className = suggestMainClassName() }
        }
        whenStepAvailable<AbstractModNameStep> { modNameStep ->
            modNameStep.nameProperty.afterChange { className = suggestMainClassName() }
        }
    }

    override fun setupUI(builder: Panel) {
        with (builder) {
            row("Main Class:") {
                textField()
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
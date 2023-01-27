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

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected

class UseMixinsStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    val useMixinsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.useMixins")
    var useMixins by useMixinsProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Use Mixins:") {
                checkBox("")
                    .bindSelected(useMixinsProperty)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, useMixins)
    }

    companion object {
        val KEY = Key.create<Boolean>("${UseMixinsStep::class.java.name}.useMixins")
    }
}

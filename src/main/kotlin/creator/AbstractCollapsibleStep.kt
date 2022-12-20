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
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

abstract class AbstractCollapsibleStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val child by lazy { createStep() }

    abstract val title: String

    protected abstract fun createStep(): NewProjectWizardStep

    override fun setupUI(builder: Panel) {
        with(builder) {
            collapsibleGroup(title) {
                child.setupUI(this)
            }
        }
    }

    override fun setupProject(project: Project) {
        child.setupProject(project)
    }
}
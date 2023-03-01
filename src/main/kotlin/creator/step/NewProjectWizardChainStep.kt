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

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

/**
 * Backported from 2023.1
 */
class NewProjectWizardChainStep<S : NewProjectWizardStep> : AbstractNewProjectWizardStep {

    private val step: S
    private val steps: List<NewProjectWizardStep> // including this.step

    constructor(step: S) : this(step, emptyList())

    private constructor(step: S, descendantSteps: List<NewProjectWizardStep>) : super(step) {
        this.step = step
        this.steps = descendantSteps + step
    }

    fun <NS : NewProjectWizardStep> nextStep(create: (S) -> NS): NewProjectWizardChainStep<NS> {
        return NewProjectWizardChainStep(create(step), steps)
    }

    override fun setupUI(builder: Panel) {
        for (step in steps) {
            step.setupUI(builder)
        }
    }

    override fun setupProject(project: Project) {
        for (step in steps) {
            step.setupProject(project)
        }
    }

    companion object {
        fun <S : NewProjectWizardStep, NS : NewProjectWizardStep> S.nextStep(
            create: (S) -> NS
        ): NewProjectWizardChainStep<NS> {
            return NewProjectWizardChainStep(this).nextStep(create)
        }
    }
}

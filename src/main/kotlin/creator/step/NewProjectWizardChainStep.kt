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

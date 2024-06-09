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

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.storeToData
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel

abstract class AbstractBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractNewProjectWizardMultiStep<AbstractBuildSystemStep, AbstractBuildSystemStep.Factory>(parent, EP_NAME) {
    companion object {
        private val PLATFORM_NAME_KEY = Key.create<String>("mcdev.platformName")
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.buildSystemWizard")
    }

    init {
        storeToData(javaClass)
    }

    abstract val platformName: String

    override val self get() = this
    override val label
        get() = MCDevBundle("creator.ui.build_system.label")

    override fun initSteps(): LinkedHashMap<String, NewProjectWizardStep> {
        context.putUserData(PLATFORM_NAME_KEY, platformName)
        return super.initSteps()
    }

    override fun setupSwitcherUi(builder: Panel) {
        if (steps.size > 1) {
            super.setupSwitcherUi(builder)
        }
    }

    override fun setupUI(builder: Panel) {
        val stepUninitialized = step.isEmpty()
        super.setupUI(builder)

        // if no value was previously set for the step (i.e. not saved from when the user previously used this wizard)
        // then set the build system to the preferred one for this platform, if one exists
        if (stepUninitialized) {
            for (buildSystem in steps.keys) {
                if (BuildSystemSupport.getInstance(platformName, buildSystem)?.preferred == true) {
                    step = buildSystem
                    break
                }
            }
        }
    }

    interface Factory : NewProjectWizardMultiStepFactory<AbstractBuildSystemStep> {
        override fun isEnabled(context: WizardContext): Boolean {
            val platformName = context.getUserData(PLATFORM_NAME_KEY)
                ?: throw IllegalStateException("Platform name not set")
            return BuildSystemSupport.getInstance(platformName, name) != null
        }

        override fun createStep(parent: AbstractBuildSystemStep): NewProjectWizardStep {
            val platformName = parent.context.getUserData(PLATFORM_NAME_KEY)
                ?: throw IllegalStateException("Platform name not set")
            val buildSystemSupport = BuildSystemSupport.getInstance(platformName, name)
                ?: throw IllegalStateException("Build system unsupported, this factory should have been filtered out")
            return buildSystemSupport.createStep(BuildSystemSupport.PRE_STEP, parent)
        }
    }
}

class GradleBuildSystem : AbstractBuildSystemStep.Factory {
    override val name
        get() = MCDevBundle("creator.ui.build_system.label.gradle")
}

class MavenBuildSystem : AbstractBuildSystemStep.Factory {
    override val name
        get() = MCDevBundle("creator.ui.build_system.label.maven")
}

abstract class AbstractRunBuildSystemStep(
    parent: NewProjectWizardStep,
    private val buildSystemStepClass: Class<out AbstractBuildSystemStep>,
) : AbstractNewProjectWizardStep(parent) {
    abstract val step: String

    override fun setupProject(project: Project) {
        val buildSystemStep = findStep(buildSystemStepClass)
        val buildSystemSupport = BuildSystemSupport.getInstance(buildSystemStep.platformName, buildSystemStep.step)
            ?: throw IllegalStateException("Build system unsupported, this should have been filtered out")
        buildSystemSupport.createStep(step, this).setupProject(project)
    }
}

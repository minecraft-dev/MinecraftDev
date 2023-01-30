/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

/**
 * This step shows to the user that we're waiting for smart mode as opposed to taking a while doing something else.
 * Note that dumb mode may occur immediately after this step, and subsequent steps must not assume smart mode is active.
 * Thus, this step is for UX purposes only.
 */
class WaitForSmartModeStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Indexing"

    override fun perform(project: Project) {
        DumbService.getInstance(project).waitForSmartMode()
    }
}

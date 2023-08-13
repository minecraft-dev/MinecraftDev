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
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

/**
 * This step shows to the user that we're waiting for smart mode as opposed to taking a while doing something else.
 * Note that dumb mode may occur immediately after this step, and subsequent steps must not assume smart mode is active.
 * Thus, this step is for UX purposes only.
 */
class WaitForSmartModeStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle.message("creator.step.wait_for_smart.description")

    override fun perform(project: Project) {
        DumbService.getInstance(project).waitForSmartMode()
    }
}

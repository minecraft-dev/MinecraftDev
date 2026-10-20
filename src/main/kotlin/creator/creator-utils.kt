/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.creator

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import javax.swing.JComponent

val WizardContext.contentPanel: JComponent?
    get() = this.getUserData(AbstractWizard.KEY)?.contentPanel

val WizardContext.modalityState: ModalityState
    get() {
        ProgressManager.checkCanceled()
        val contentPanel = contentPanel
        if (contentPanel == null) {
            thisLogger().error("Wizard content panel is null, using default modality state")
            return ModalityState.defaultModalityState()
        }

        return ModalityState.stateForComponent(contentPanel)
    }

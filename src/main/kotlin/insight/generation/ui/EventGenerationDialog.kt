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

package com.demonwav.mcdev.insight.generation.ui

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.insight.generation.GenerationData
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo

class EventGenerationDialog(
    editor: Editor,
    private val panel: EventGenerationPanel,
    className: String,
    defaultListenerName: String,
) : DialogWrapper(editor.project, editor.component, false, IdeModalityType.PROJECT) {

    private val wizard: EventListenerWizard = EventListenerWizard(panel.panel, className, defaultListenerName)

    var data: GenerationData? = null
        private set

    init {
        title = MCDevBundle("generate.event_listener.settings")
        isOKActionEnabled = true
        setValidationDelay(0)

        init()
    }

    override fun doOKAction() {
        data = panel.gatherData()
        super.doOKAction()
    }

    override fun createCenterPanel() = wizard.panel

    override fun doValidate(): ValidationInfo? {
        return panel.doValidate()
    }

    val chosenName: String
        get() = wizard.chosenClassName
}

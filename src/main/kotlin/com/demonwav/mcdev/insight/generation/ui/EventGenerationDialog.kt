/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation.ui

import com.demonwav.mcdev.insight.generation.GenerationData
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo

class EventGenerationDialog(
    editor: Editor,
    private val panel: EventGenerationPanel,
    className: String,
    defaultListenerName: String
) : DialogWrapper(editor.component, false) {

    private val wizard: EventListenerWizard = EventListenerWizard(panel.panel, className, defaultListenerName)

    var data: GenerationData? = null
        private set

    init {
        title = "Event Listener Settings"
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

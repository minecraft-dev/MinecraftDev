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

import com.intellij.ide.highlighter.JavaHighlightingColors
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.util.ui.UIUtil
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField

class EventListenerWizard(panel: JPanel?, className: String, defaultListenerName: String) {
    lateinit var panel: JPanel
    private lateinit var classNameTextField: JTextField
    private lateinit var listenerNameTextField: JTextField
    private lateinit var publicVoidLabel: JLabel
    private lateinit var contentPanel: JPanel
    private lateinit var separator: JSeparator

    init {
        classNameTextField.font = EditorUtil.getEditorFont()
        listenerNameTextField.font = EditorUtil.getEditorFont()
        publicVoidLabel.font = EditorUtil.getEditorFont()
        if (UIUtil.isUnderDarcula()) {
            publicVoidLabel.foreground = JavaHighlightingColors.KEYWORD.defaultAttributes.foregroundColor
        } else {
            publicVoidLabel.foreground =
                JavaHighlightingColors.KEYWORD.fallbackAttributeKey!!.defaultAttributes.foregroundColor
        }

        if (panel != null) {
            separator.isVisible = true
            contentPanel.add(panel, innerContentPanelConstraints)
        }

        classNameTextField.text = className
        listenerNameTextField.text = defaultListenerName

        IdeFocusTraversalPolicy.getPreferredFocusedComponent(listenerNameTextField).requestFocus()
        listenerNameTextField.requestFocus()
    }

    val chosenClassName: String
        get() = listenerNameTextField.text

    companion object {
        private val innerContentPanelConstraints = GridConstraints()

        init {
            innerContentPanelConstraints.row = 0
            innerContentPanelConstraints.column = 0
            innerContentPanelConstraints.rowSpan = 1
            innerContentPanelConstraints.colSpan = 1
            innerContentPanelConstraints.anchor = GridConstraints.ANCHOR_CENTER
            innerContentPanelConstraints.fill = GridConstraints.FILL_BOTH
            innerContentPanelConstraints.hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
            innerContentPanelConstraints.vSizePolicy = GridConstraints.SIZEPOLICY_FIXED
        }
    }
}

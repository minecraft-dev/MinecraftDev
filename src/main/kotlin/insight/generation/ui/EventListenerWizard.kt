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

import com.intellij.ide.highlighter.JavaHighlightingColors
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy
import com.intellij.ui.JBColor
import com.intellij.uiDesigner.core.GridConstraints
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
        if (!JBColor.isBright()) {
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

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
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.util.ui.UIUtil
import javax.swing.JPanel

class EventListenerWizard(panel: JPanel?, className: String, defaultListenerName: String) {

    private val graph = PropertyGraph("EventListenerWizard graph")

    private val listenerNameProperty = graph.property(defaultListenerName)
    val chosenClassName: String by listenerNameProperty

    val panel: JPanel by lazy {
        panel {
            row {
                textField()
                    .text(className)
                    .align(AlignX.FILL)
                    .apply {
                        component.font = EditorUtil.getEditorFont()
                        component.isEditable = false
                    }
            }

            row {
                label("public void").apply {
                    component.font = EditorUtil.getEditorFont()
                    if (UIUtil.isUnderDarcula()) {
                        component.foreground = JavaHighlightingColors.KEYWORD.defaultAttributes.foregroundColor
                    } else {
                        component.foreground =
                            JavaHighlightingColors.KEYWORD.fallbackAttributeKey!!.defaultAttributes.foregroundColor
                    }
                }

                textField()
                    .bindText(listenerNameProperty)
                    .columns(COLUMNS_LARGE)
                    .focused()
                    .apply {
                        component.font = EditorUtil.getEditorFont()
                    }
            }

            if (panel != null) {
                separator()

                row {
                    cell(panel)
                }
            }
        }
    }
}

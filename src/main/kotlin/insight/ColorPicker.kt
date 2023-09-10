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

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ColorPicker(private val colorMap: Map<String, Color>, parent: JComponent) {

    private val panel = JPanel(GridBagLayout())

    private var chosenColor: String? = null
    private val dialog: ColorPickerDialog

    init {
        dialog = ColorPickerDialog(parent, panel)
    }

    fun showDialog(): String? {
        init()

        dialog.show()

        return chosenColor
    }

    private fun init() {
        val iterator = colorMap.entries.iterator()
        addToPanel(0, panel, iterator)
        addToPanel(1, panel, iterator)
    }

    private fun addToPanel(row: Int, panel: JPanel, iterator: Iterator<Map.Entry<String, Color>>) {
        for (i in 0 until 8) {
            if (!iterator.hasNext()) {
                break
            }

            val entry = iterator.next()
            val icon = ColorIcon(28, entry.value, true)

            val label = JLabel(icon)
            label.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        chosenColor = entry.key
                        dialog.close(0)
                    }
                },
            )

            val constraints = GridBagConstraints()
            constraints.gridy = row
            constraints.fill = GridBagConstraints.NONE
            constraints.insets = JBUI.insets(10)

            panel.add(label, constraints)
        }
    }

    private class ColorPickerDialog(parent: JComponent, private val component: JComponent) :
        DialogWrapper(parent, false) {

        init {
            title = MCDevBundle("generate.color.choose_action")
            isResizable = true

            init()
        }

        override fun createCenterPanel(): JComponent {
            return component
        }
    }
}

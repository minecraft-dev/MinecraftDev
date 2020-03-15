/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.ColorIcon
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
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
        addToPanel(0, 8, panel, iterator)
        addToPanel(1, 8, panel, iterator)
    }

    private fun addToPanel(row: Int, cols: Int, panel: JPanel, iterator: Iterator<Map.Entry<String, Color>>) {
        for (i in 0 until cols) {
            if (!iterator.hasNext()) {
                break
            }

            val entry = iterator.next()
            val icon = ColorIcon(28, entry.value, true)

            val label = JLabel(icon)
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    chosenColor = entry.key
                    dialog.close(0)
                }
            })

            val constraints = GridBagConstraints()
            constraints.gridy = row
            constraints.fill = GridBagConstraints.NONE
            constraints.insets = Insets(10, 10, 10, 10)

            panel.add(label, constraints)
        }
    }

    private class ColorPickerDialog constructor(parent: JComponent, private val component: JComponent) :
        DialogWrapper(parent, false) {

        init {
            title = "Choose Color"
            setResizable(true)

            init()
        }

        override fun createCenterPanel(): JComponent? {
            return component
        }
    }
}

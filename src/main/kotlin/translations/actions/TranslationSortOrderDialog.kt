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

package com.demonwav.mcdev.translations.actions

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.translations.sorting.Ordering
import com.intellij.CommonBundle
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.KeyStroke
import javax.swing.WindowConstants

class TranslationSortOrderDialog(excludeDefaultOption: Boolean, defaultSelection: Ordering) : JDialog() {

    private val graph = PropertyGraph("TranslationSortOrderDialog graph")

    private val orderProperty = graph.property(defaultSelection)
    private val keepCommentsProperty = graph.property(0)

    private var canceled = false

    init {
        val availableOrderings = if (excludeDefaultOption) NON_DEFAULT_ORDERINGS else ALL_ORDERINGS
        val panel = panel {
            row(MCDevBundle("translation_sort.order")) {
                comboBox(availableOrderings, CellRenderer)
                    .bindItem(orderProperty)
            }

            row(MCDevBundle("translation_sort.keep_comment")) {
                spinner(0..Int.MAX_VALUE)
                    .bindIntValue(keepCommentsProperty::get, keepCommentsProperty::set)
            }

            row {
                button(CommonBundle.message("button.ok")) { onOK() }.align(AlignX.RIGHT).also {
                    getRootPane().defaultButton = it.component
                }
                button(CommonBundle.message("button.cancel")) { onCancel() }.align(AlignX.RIGHT)
            }
        }
        contentPane = panel

        isModal = true
        title = MCDevBundle("translation_sort.title")

        // call onCancel() when cross is clicked
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    onCancel()
                }
            },
        )

        // call onCancel() on ESCAPE
        panel.registerKeyboardAction(
            { onCancel() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
        )
    }

    private fun onOK() {
        dispose()
    }

    private fun onCancel() {
        canceled = true
        dispose()
    }

    object CellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val displayValue = (value as? Ordering)?.text
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus)
        }
    }

    companion object {
        private val ALL_ORDERINGS = Ordering.entries
        private val NON_DEFAULT_ORDERINGS = Ordering.entries
            .filterNot { it == Ordering.LIKE_DEFAULT }

        fun show(excludeDefaultOption: Boolean, defaultSelection: Ordering): Pair<Ordering?, Int> {
            val dialog = TranslationSortOrderDialog(excludeDefaultOption, defaultSelection)
            dialog.pack()
            dialog.setLocationRelativeTo(dialog.owner)
            dialog.isVisible = true
            val order = if (dialog.canceled) null else dialog.orderProperty.get()
            val comments = dialog.keepCommentsProperty.get()
            return (order to comments)
        }
    }
}

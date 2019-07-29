/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.actions

import com.demonwav.mcdev.i18n.sorting.Ordering
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.KeyStroke
import javax.swing.SpinnerNumberModel
import javax.swing.WindowConstants

class TranslationSortOrderDialog(excludeDefaultOption: Boolean, defaultSelection: Ordering) : JDialog() {
    private lateinit var contentPane: JPanel
    private lateinit var buttonOK: JButton
    private lateinit var buttonCancel: JButton
    private lateinit var comboSelection: JComboBox<Ordering>
    private lateinit var spinnerComments: JSpinner

    init {
        setContentPane(contentPane)
        isModal = true
        title = "Select Sort Order"
        getRootPane().defaultButton = buttonOK

        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }
        spinnerComments.model = SpinnerNumberModel(0, 0, Int.MAX_VALUE, 1)
        val availableOrderings = if (excludeDefaultOption) NON_DEFAULT_ORDERINGS else ALL_ORDERINGS
        comboSelection.model = DefaultComboBoxModel(availableOrderings)
        comboSelection.renderer = CellRenderer
        comboSelection.selectedItem = defaultSelection

        // call onCancel() when cross is clicked
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    onCancel()
                }
            }
        )

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
            { onCancel() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
    }

    private fun onOK() {
        dispose()
    }

    private fun onCancel() {
        comboSelection.selectedIndex = -1
        dispose()
    }

    object CellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val displayValue = (value as? Ordering)?.text
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus)
        }
    }

    companion object {
        private val ALL_ORDERINGS = Ordering.values()
        private val NON_DEFAULT_ORDERINGS = Ordering.values()
            .filterNot { it == Ordering.LIKE_DEFAULT }.toTypedArray()

        fun show(excludeDefaultOption: Boolean, defaultSelection: Ordering): Pair<Ordering?, Int> {
            val dialog = TranslationSortOrderDialog(excludeDefaultOption, defaultSelection)
            dialog.pack()
            dialog.setLocationRelativeTo(dialog.owner)
            dialog.isVisible = true
            val order = dialog.comboSelection.selectedItem as? Ordering
            val comments = dialog.spinnerComments.value as Int
            return (order to comments)
        }
    }
}

/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.actions

import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.KeyStroke
import javax.swing.SpinnerNumberModel
import javax.swing.WindowConstants

class TranslationSortOrderDialog : JDialog() {
    private lateinit var contentPane: JPanel
    private lateinit var buttonOK: JButton
    private lateinit var buttonCancel: JButton
    private lateinit var comboSelection: JComboBox<String>
    private lateinit var spinnerComments: JSpinner

    init {
        setContentPane(contentPane)
        isModal = true
        title = "Select Sort Order"
        getRootPane().defaultButton = buttonOK

        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }
        spinnerComments.model = SpinnerNumberModel(0, 0, Int.MAX_VALUE, 1)

        // call onCancel() when cross is clicked
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onCancel()
            }
        })

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction({ onCancel() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    }

    private fun onOK() {
        dispose()
    }

    private fun onCancel() {
        comboSelection.selectedIndex = -1
        dispose()
    }

    companion object {
        fun show(): Pair<Int, Int> {
            val dialog = TranslationSortOrderDialog()
            dialog.pack()
            dialog.setLocationRelativeTo(dialog.owner)
            dialog.isVisible = true
            return (dialog.comboSelection.selectedIndex to (dialog.spinnerComments.value as Int))
        }
    }
}

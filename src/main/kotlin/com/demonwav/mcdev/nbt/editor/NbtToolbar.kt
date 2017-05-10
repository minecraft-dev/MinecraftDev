/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.nbt.NbtVirtualFile
import com.demonwav.mcdev.util.runWriteTaskLater
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel

class NbtToolbar(nbtFile: NbtVirtualFile) {
    lateinit var panel: JPanel
    private lateinit var compressionBox: JComboBox<CompressionSelection>
    private lateinit var saveButton: JButton

    private var lastSelection: CompressionSelection

    init {
        compressionBox.addItem(CompressionSelection.GZIP)
        compressionBox.addItem(CompressionSelection.UNCOMPRESSED)
        compressionBox.selectedItem = if (nbtFile.isCompressed) CompressionSelection.GZIP else CompressionSelection.UNCOMPRESSED
        lastSelection = selection

        saveButton.isVisible = false

        if (!nbtFile.isWritable) {
            compressionBox.isEnabled = false
        } else {
            compressionBox.addActionListener {
                saveButton.isVisible = lastSelection != selection
            }
        }

        saveButton.addActionListener {
            saveButton.isVisible = false

            runWriteTaskLater {
                nbtFile.writeFile(this)
            }
        }
    }

    val selection
        get() = compressionBox.selectedItem as CompressionSelection
}

/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
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
    lateinit var saveButton: JButton

    private var lastSelection: CompressionSelection

    init {
        compressionBox.addItem(CompressionSelection.GZIP)
        compressionBox.addItem(CompressionSelection.UNCOMPRESSED)
        compressionBox.selectedItem =
            if (nbtFile.isCompressed) CompressionSelection.GZIP else CompressionSelection.UNCOMPRESSED
        lastSelection = selection

        if (!nbtFile.isWritable || !nbtFile.parseSuccessful) {
            compressionBox.isEnabled = false
        }

        if (!nbtFile.parseSuccessful) {
            panel.isVisible = false
        }

        saveButton.addActionListener {
            lastSelection = selection

            runWriteTaskLater {
                nbtFile.writeFile(this)
            }
        }
    }

    val selection
        get() = compressionBox.selectedItem as CompressionSelection
}

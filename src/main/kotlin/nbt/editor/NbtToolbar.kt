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

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.nbt.NbtVirtualFile
import com.demonwav.mcdev.util.runWriteTaskLater
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class NbtToolbar(nbtFile: NbtVirtualFile) {
    lateinit var panel: JPanel
    private lateinit var fileTypeLabel: JLabel
    private lateinit var compressionBox: JComboBox<CompressionSelection>
    lateinit var saveButton: JButton

    private var lastSelection: CompressionSelection

    init {
        fileTypeLabel.text = MCDevBundle("nbt.compression.file_type.label")
        saveButton.text = MCDevBundle("nbt.compression.save.button")

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

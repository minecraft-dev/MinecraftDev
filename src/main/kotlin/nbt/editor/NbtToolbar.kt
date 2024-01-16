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

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.nbt.NbtVirtualFile
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel

class NbtToolbar(nbtFile: NbtVirtualFile) {

    private var compressionSelection: CompressionSelection? =
        if (nbtFile.isCompressed) CompressionSelection.GZIP else CompressionSelection.UNCOMPRESSED

    val selection: CompressionSelection
        get() = compressionSelection!!

    lateinit var panel: DialogPanel

    init {
        panel = panel {
            row(MCDevBundle("nbt.compression.file_type.label")) {
                comboBox(EnumComboBoxModel(CompressionSelection::class.java))
                    .bindItem(::compressionSelection)
                    .enabled(nbtFile.isWritable && nbtFile.parseSuccessful)
                button(MCDevBundle("nbt.compression.save.button")) {
                    panel.apply()
                    runWriteTaskLater {
                        nbtFile.writeFile(this)
                    }
                }
            }
            visible(nbtFile.parseSuccessful)
        }
    }
}

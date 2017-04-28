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

import javax.swing.JComboBox
import javax.swing.JPanel

class NbtToolbar(selection: CompressionSelection) {
    lateinit var panel: JPanel
    private lateinit var compressionBox: JComboBox<CompressionSelection>

    init {
        compressionBox.addItem(CompressionSelection.GZIP)
        compressionBox.addItem(CompressionSelection.UNCOMPRESSED)
        compressionBox.selectedItem = selection
    }

    val selection
        get() = compressionBox.selectedItem as CompressionSelection
}

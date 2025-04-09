package com.demonwav.mcdev.nbt.editor

import com.intellij.ui.CollectionComboBoxModel

private fun makeItems(isInRegionFile: Boolean) = if (isInRegionFile) {
    CompressionSelection.entries.toList()
} else {
    CompressionSelection.entries.asSequence().filter { !it.regionFileOnly }.toList()
}

class CompressionComboBoxModel(isInRegionFile: Boolean) :
    CollectionComboBoxModel<CompressionSelection?>(makeItems(isInRegionFile))

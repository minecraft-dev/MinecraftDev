/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.editor.nbt

import com.intellij.openapi.fileEditor.FileEditorLocation

class NbtFileEditorLocation(private val editor: NbtFileEditor) : FileEditorLocation {

    override fun compareTo(other: FileEditorLocation): Int {
        if (other !is NbtFileEditorLocation) {
            return 0 // shouldn't happen, just doing this for the smart cast
        }

        TODO("not implemented")
    }

    override fun getEditor() = editor
}

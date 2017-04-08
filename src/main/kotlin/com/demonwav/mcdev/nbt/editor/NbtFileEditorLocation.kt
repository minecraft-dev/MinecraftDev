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

class NbtFileEditorLocation(private val editor: NbtFileEditor) : com.intellij.openapi.fileEditor.FileEditorLocation {

    override fun compareTo(other: com.intellij.openapi.fileEditor.FileEditorLocation): Int {
        if (other !is com.demonwav.mcdev.nbt.editor.NbtFileEditorLocation) {
            return 0 // shouldn't happen, just doing this for the smart cast
        }

        TODO("not implemented")
    }

    override fun getEditor() = editor
}

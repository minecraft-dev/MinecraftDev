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

import com.demonwav.mcdev.nbt.Nbt
import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.demonwav.mcdev.nbt.tags.TagCompound
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class NbtFileEditor internal constructor(private val file: VirtualFile) : FileEditor, UserDataHolderBase() {

    private val rootCompound: TagCompound = Nbt.buildTagTree(file.inputStream)
    private var modifiedCompound: TagCompound = rootCompound.copy()

    private val editor: Editor = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(modifiedCompound.toString()))

    override fun isModified(): Boolean {
        return rootCompound != modifiedCompound
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getName(): String {
        return "NBT Editor"
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return FileEditorState.INSTANCE // TODO
    }
    override fun setState(state: FileEditorState) {}

    override fun getComponent(): JComponent {
        return editor.component
    }
    override fun getPreferredFocusedComponent() = component

    override fun selectNotify() {}
    override fun deselectNotify() {}

    override fun getCurrentLocation(): NbtFileEditorLocation? {
        // TODO: is this necessary? it's optional
        return null
    }

    override fun isValid() = file.isValid && file.fileType === NbtFileType

    override fun getBackgroundHighlighter() = null
    override fun dispose() {}
}

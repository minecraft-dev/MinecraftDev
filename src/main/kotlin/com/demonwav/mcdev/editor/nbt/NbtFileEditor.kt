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

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class NbtFileEditor(private val file: VirtualFile) : FileEditor {

    override fun isModified(): Boolean {
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getName(): String {
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {}
    override fun setState(state: FileEditorState) {}

    override fun getComponent(): JComponent {
    }
    override fun getPreferredFocusedComponent() = component

    override fun <T : Any?> getUserData(key: Key<T>): T? {
    }
    override fun selectNotify() {}

    override fun deselectNotify() {}

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    }

    override fun getCurrentLocation(): NbtFileEditorLocation? {
        // TODO: is this necessary? it's optional
    }

    override fun isValid() = file.isValid && file.isNbtFile

    override fun getBackgroundHighlighter() = null
    override fun dispose() {}
}

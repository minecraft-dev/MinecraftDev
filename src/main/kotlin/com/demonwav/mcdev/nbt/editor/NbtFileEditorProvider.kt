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
import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JPanel

class NbtFileEditorProvider : PsiAwareTextEditorProvider(), DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun getPolicy() = FileEditorPolicy.NONE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val nbtFile = NbtVirtualFile(file, project)
        val fileEditor = NbtFileEditor(super.createEditor(project, nbtFile), nbtFile)

        if (NonProjectFileWritingAccessProvider.isWriteAccessAllowed(file, project)) {
            NonProjectFileWritingAccessProvider.allowWriting(nbtFile)
        }

        nbtFile.toolbar = fileEditor.toolbar
        return fileEditor
    }

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

private class NbtFileEditor(private val editor: FileEditor, nbtFile: NbtVirtualFile) : FileEditor {

    val toolbar = NbtToolbar(nbtFile)
    private val component = JPanel(BorderLayout())

    init {
        component.add(toolbar.panel, BorderLayout.NORTH)
        component.add(editor.component, BorderLayout.CENTER)
    }

    override fun isModified() = editor.isModified
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = editor.addPropertyChangeListener(listener)
    override fun getName() = editor.name
    override fun setState(state: FileEditorState) = editor.setState(state)
    override fun getState(level: FileEditorStateLevel) = editor.getState(level)
    override fun getComponent() = component
    override fun getPreferredFocusedComponent() = editor.preferredFocusedComponent
    override fun <T : Any?> getUserData(key: Key<T>) = editor.getUserData(key)
    override fun selectNotify() = editor.selectNotify()
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = editor.putUserData(key, value)
    override fun getCurrentLocation() = editor.currentLocation
    override fun deselectNotify() = editor.deselectNotify()
    override fun getBackgroundHighlighter() = editor.backgroundHighlighter
    override fun isValid() = editor.isValid
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = editor.removePropertyChangeListener(listener)
    override fun dispose() = editor.dispose()
    override fun getStructureViewBuilder() = editor.structureViewBuilder
    override fun equals(other: Any?): Boolean {
        if (other !is NbtFileEditor) {
            return false
        }

        return other.component == this.component
    }
    override fun hashCode() = editor.hashCode()
    override fun toString() = editor.toString()
}

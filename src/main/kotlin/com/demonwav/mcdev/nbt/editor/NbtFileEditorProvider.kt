/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.nbt.NbtVirtualFile
import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLoadingPanel
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JPanel

class NbtFileEditorProvider : PsiAwareTextEditorProvider(), DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun getPolicy() = FileEditorPolicy.NONE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val fileEditor = NbtFileEditor { nbtFile ->  super.createEditor(project, nbtFile) }

        ApplicationManager.getApplication().executeOnPooledThread {
            val nbtFile = NbtVirtualFile(file, project)

            if (NonProjectFileWritingAccessProvider.isWriteAccessAllowed(file, project)) {
                NonProjectFileWritingAccessProvider.allowWriting(listOf(nbtFile))
            }

            invokeLater {
                fileEditor.ready(nbtFile)
            }
        }

        return fileEditor
    }

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

private class NbtFileEditor(private val editorProvider: (NbtVirtualFile) -> FileEditor) : FileEditor {

    private var editor: FileEditor? = null
    private val component = JPanel(BorderLayout())

    init {
        val loading = JBLoadingPanel(null, this)
        loading.setLoadingText("Parsing NBT file")
        loading.startLoading()
        component.add(loading, BorderLayout.CENTER)
    }

    fun ready(nbtFile: NbtVirtualFile) {
        component.removeAll()

        val toolbar = NbtToolbar(nbtFile)
        nbtFile.toolbar = toolbar
        editor = editorProvider(nbtFile)
        editor?.let { editor ->
            Disposer.register(this, editor)
            component.add(toolbar.panel, BorderLayout.NORTH)
            component.add(editor.component, BorderLayout.CENTER)
        }
    }

    override fun isModified() = editor.exec { isModified } ?: false
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        editor.exec { addPropertyChangeListener(listener) }
    }
    override fun getName() = editor.exec { name } ?: ""
    override fun setState(state: FileEditorState) {
        editor.exec { setState(state) }
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState = editor.exec { getState(level) }
        ?: FileEditorState.INSTANCE

    override fun getComponent() = component
    override fun getPreferredFocusedComponent() = editor.exec { preferredFocusedComponent }
    override fun <T : Any?> getUserData(key: Key<T>) = editor.exec { getUserData(key) }
    override fun selectNotify() {
        editor.exec { selectNotify() }
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        editor.exec { putUserData(key, value) }
    }

    override fun getCurrentLocation() = editor.exec { currentLocation }
    override fun deselectNotify() {
        editor.exec { deselectNotify() }
    }

    override fun getBackgroundHighlighter() = editor.exec { backgroundHighlighter }
    override fun isValid() = editor.exec { isValid } ?: true
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        editor.exec { removePropertyChangeListener(listener) }
    }
    override fun dispose() {}

    override fun getStructureViewBuilder() = editor.exec { structureViewBuilder }
    override fun equals(other: Any?) = other is NbtFileEditor && other.component == this.component
    override fun hashCode() = editor.hashCode()
    override fun toString() = editor.toString()

    private inline fun <T : Any?> FileEditor?.exec(action: FileEditor.() -> T): T? {
        if (editor?.let { ed -> Disposer.isDisposed(ed) || Disposer.isDisposing(ed) } == true) {
            return null
        }
        return this?.action()
    }
}

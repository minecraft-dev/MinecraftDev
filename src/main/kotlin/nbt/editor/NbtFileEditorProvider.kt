/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.nbt.NbtVirtualFile
import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.intellij.ide.actions.SaveAllAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.IncorrectOperationException
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JPanel
import org.jetbrains.concurrency.runAsync

class NbtFileEditorProvider : PsiAwareTextEditorProvider(), DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun getPolicy() = FileEditorPolicy.NONE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val fileEditor = NbtFileEditor(file) { nbtFile ->
            invokeAndWait {
                super.createEditor(project, nbtFile)
            }
        }

        runAsync {
            val nbtFile = NbtVirtualFile(file, project)

            if (NonProjectFileWritingAccessProvider.isWriteAccessAllowed(file, project)) {
                NonProjectFileWritingAccessProvider.allowWriting(listOf(nbtFile))
            }

            fileEditor.ready(nbtFile, project)
        }

        return fileEditor
    }

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

private class NbtFileEditor(
    private val file: VirtualFile,
    private val editorProvider: (NbtVirtualFile) -> FileEditor
) : FileEditor {

    private var editor: FileEditor? = null
    private val component = JPanel(BorderLayout())

    init {
        val loading = JBLoadingPanel(null, this)
        loading.setLoadingText("Parsing NBT file")
        loading.startLoading()
        component.add(loading, BorderLayout.CENTER)
    }

    fun ready(nbtFile: NbtVirtualFile, project: Project) {
        if (project.isDisposed) {
            return
        }

        component.removeAll()

        val toolbar = NbtToolbar(nbtFile)
        nbtFile.toolbar = toolbar
        editor = invokeAndWait {
            editorProvider(nbtFile)
        }
        editor?.let { editor ->
            try {
                Disposer.register(this, editor)
            } catch (e: IncorrectOperationException) {
                // The editor can be disposed really quickly when opening a large number of NBT files
                // Since everything happens basically at the same time, calling Disposer.isDisposed right before
                // returns false but #dispose throws this IOE...
                Disposer.dispose(this)
                return@let
            }
            invokeLater {
                component.add(toolbar.panel, BorderLayout.NORTH)
                component.add(editor.component, BorderLayout.CENTER)
            }
        }

        // This can be null if the file is too big to be parsed as a psi file
        val psiFile = runReadAction {
            PsiManager.getInstance(project).findFile(nbtFile) as? NbttFile
        } ?: return

        WriteCommandAction.writeCommandAction(psiFile)
            .shouldRecordActionForActiveDocument(false)
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
            .run<Throwable> {
                CodeStyleManager.getInstance(project).reformat(psiFile, true)
            }

        project.messageBus.connect(this).subscribe(
            AnActionListener.TOPIC,
            object : AnActionListener {
                override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                    if (action !is SaveAllAction) {
                        return
                    }

                    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor ?: return

                    if (selectedEditor !is NbtFileEditor || selectedEditor.editor != editor) {
                        return
                    }

                    nbtFile.writeFile(this)
                }
            }
        )
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
        ?: TextEditorState()

    override fun getFile(): VirtualFile = editor.exec { file } ?: file

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
        if (editor?.let { ed -> Disposer.isDisposed(ed) } == true) {
            return null
        }
        return this?.action()
    }
}

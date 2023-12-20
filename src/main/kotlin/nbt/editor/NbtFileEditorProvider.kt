/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.demonwav.mcdev.nbt.lang.NbttFileType
import com.intellij.ide.actions.SaveAllAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import java.beans.PropertyChangeListener

class NbtFileEditorProvider : AsyncFileEditorProvider, DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
    override fun createEditorAsync(project: Project, file: VirtualFile): AsyncFileEditorProvider.Builder {
        val nbtFile = NbtVirtualFile(file, project)

        val allowWrite = NonProjectFileWritingAccessProvider.isWriteAccessAllowed(nbtFile, project)
        if (allowWrite) {
            NonProjectFileWritingAccessProvider.allowWriting(listOf(nbtFile))
        }

        return NbtEditorBuilder(project, nbtFile)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val nbtFile = NbtVirtualFile(file, project)
        return NbtEditorBuilder(project, nbtFile).build()
    }

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

private class NbtEditorBuilder(val project: Project, val nbtFile: NbtVirtualFile) : AsyncFileEditorProvider.Builder() {
    override fun build(): FileEditor {
        val document = FileDocumentManager.getInstance().getDocument(nbtFile)!!
        val backingEditor = EditorFactory.getInstance().createEditor(document, project, NbttFileType, false)
        val fileEditor = NbtFileEditor(nbtFile, backingEditor, project)
        return fileEditor
    }
}

private class NbtFileEditor(
    private val file: NbtVirtualFile,
    private val editor: Editor,
    project: Project,
) : FileEditor {

    init {
        val toolbar = NbtToolbar(file)
        file.toolbar = toolbar
        (editor as? EditorEx)?.permanentHeaderComponent = toolbar.panel
        editor.headerComponent = toolbar.panel
        EditorNotifications.getInstance(project).updateAllNotifications()

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

                    file.writeFile(this)
                }
            },
        )
    }

    override fun isModified() = false
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getName() = MCDevBundle("nbt.editor.name")
    override fun setState(state: FileEditorState) = Unit

    override fun getFile(): VirtualFile = file

    override fun getComponent() = editor.component
    override fun getPreferredFocusedComponent() = null
    override fun <T : Any?> getUserData(key: Key<T>): T? = editor.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = editor.putUserData(key, value)

    override fun isValid() = true
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }

    override fun equals(other: Any?) = other is NbtFileEditor && other.component == this.component
    override fun hashCode() = editor.hashCode()
    override fun toString() = editor.toString()
}

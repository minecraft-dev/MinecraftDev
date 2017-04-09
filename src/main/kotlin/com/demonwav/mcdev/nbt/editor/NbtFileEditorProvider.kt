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
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NbtFileEditorProvider : PsiAwareTextEditorProvider(), DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun getPolicy() = FileEditorPolicy.NONE
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return super.createEditor(project, NbtVirtualFile(file, project))
    }

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

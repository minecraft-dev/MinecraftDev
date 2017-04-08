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

import com.demonwav.mcdev.nbt.filetype.NbtFileType
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NbtFileEditorProvider : FileEditorProvider, DumbAware {
    override fun getEditorTypeId() = EDITOR_TYPE_ID
    override fun accept(project: Project, file: VirtualFile) = file.fileType == NbtFileType
    override fun createEditor(project: Project, file: VirtualFile) = NbtFileEditor(file)
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        private const val EDITOR_TYPE_ID = "nbt_editor"
    }
}

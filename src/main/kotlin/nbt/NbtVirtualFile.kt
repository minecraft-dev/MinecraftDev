/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.nbt.editor.CompressionSelection
import com.demonwav.mcdev.nbt.editor.NbtToolbar
import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.util.runReadActionAsync
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.lang.Language
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ThreeState
import java.io.DataOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

fun NbtVirtualFile(backingFile: VirtualFile, project: Project): NbtVirtualFile {
    var language: Language = NbttLanguage

    var text: String
    var compressed: Boolean
    var parseSuccessful: Boolean

    try {
        val (rootCompound, isCompressed) = Nbt.buildTagTree(backingFile.inputStream, TimeUnit.SECONDS.toMillis(10))
        text = rootCompound.toString()
        compressed = isCompressed
        parseSuccessful = true
    } catch (e: MalformedNbtFileException) {
        text = MCDevBundle("nbt.lang.errors.wrapped_error_message", e.message)
        compressed = false
        parseSuccessful = false
    }

    if (!parseSuccessful) {
        language = PlainTextLanguage.INSTANCE
    }

    return NbtVirtualFile(backingFile, project, language, text, compressed, parseSuccessful)
}

class NbtVirtualFile(
    private val backingFile: VirtualFile,
    private val project: Project,
    language: Language,
    text: String,
    val isCompressed: Boolean,
    val parseSuccessful: Boolean,
) : LightVirtualFile(backingFile.name + ".nbtt", language, text), IdeDocumentHistoryImpl.SkipFromDocumentHistory {

    lateinit var toolbar: NbtToolbar

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        backingFile.refresh(asynchronous, recursive, postRunnable)
    }

    override fun getParent() = backingFile
    override fun isWritable() = backingFile.isWritable
    override fun isTooLargeForIntelligence() = ThreeState.NO

    fun writeFile(requester: Any) {
        runReadActionAsync {
            val nbttFile = PsiManager.getInstance(project).findFile(this) as? NbttFile

            if (nbttFile == null) {
                Notification(
                    "NBT Save Error",
                    MCDevBundle("nbt.file.save_notify.file_type_error.title"),
                    MCDevBundle("nbt.file.save_notify.file_type_error.content"),
                    NotificationType.WARNING,
                ).notify(project)
                return@runReadActionAsync
            }

            val rootTag = nbttFile.getRootCompound()?.getRootCompoundTag()

            if (rootTag == null) {
                Notification(
                    "NBT Save Error",
                    MCDevBundle("nbt.file.save_notify.parse_error.title"),
                    MCDevBundle("nbt.file.save_notify.parse_error.content", backingFile.name),
                    NotificationType.WARNING,
                ).notify(project)
                return@runReadActionAsync
            }

            runWriteTaskLater {
                // just to be safe
                this.parent.bom = null
                val filteredStream = when (toolbar.selection) {
                    CompressionSelection.GZIP -> GZIPOutputStream(this.parent.getOutputStream(requester))
                    CompressionSelection.UNCOMPRESSED -> this.parent.getOutputStream(requester)
                }

                DataOutputStream(filteredStream).use { stream ->
                    rootTag.write(stream)
                }

                Notification(
                    "NBT Save Success",
                    MCDevBundle("nbt.file.save_notify.success.title"),
                    MCDevBundle("nbt.file.save_notify.success.content", backingFile.name),
                    NotificationType.INFORMATION,
                ).notify(project)
            }
        }
    }
}

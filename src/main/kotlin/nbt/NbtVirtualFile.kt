/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.nbt.editor.CompressionSelection
import com.demonwav.mcdev.nbt.editor.NbtToolbar
import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.nbt.lang.NbttFileType
import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.util.runReadActionAsync
import com.demonwav.mcdev.util.runWriteTaskLater
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

class NbtVirtualFile(private val backingFile: VirtualFile, private val project: Project) :
    LightVirtualFile(backingFile.name + ".nbtt", NbttFileType, ""),
    IdeDocumentHistoryImpl.SkipFromDocumentHistory {

    val isCompressed: Boolean
    lateinit var toolbar: NbtToolbar
    val parseSuccessful: Boolean

    init {
        originalFile = backingFile
        language = NbttLanguage

        var text: String
        var tempCompressed: Boolean
        var tempParseSuccessful: Boolean

        try {
            val (rootCompound, isCompressed) = Nbt.buildTagTree(backingFile.inputStream, TimeUnit.SECONDS.toMillis(10))
            text = rootCompound.toString()
            tempCompressed = isCompressed
            tempParseSuccessful = true
        } catch (e: MalformedNbtFileException) {
            text = "Malformed NBT file:\n${e.message}"
            tempCompressed = false
            tempParseSuccessful = false
        }

        this.isCompressed = tempCompressed
        this.parseSuccessful = tempParseSuccessful

        if (!this.parseSuccessful) {
            language = PlainTextLanguage.INSTANCE
        }

        setContent(this, text, false)
    }

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
                    "Error saving NBT file",
                    "The file is not recognised as a NBTT file. This might be caused by wrong file type associations," +
                        " or the file could be too large.",
                    NotificationType.WARNING
                ).notify(project)
                return@runReadActionAsync
            }

            val rootTag = nbttFile.getRootCompound()?.getRootCompoundTag()

            if (rootTag == null) {
                Notification(
                    "NBT Save Error",
                    "Error saving NBT file",
                    "Due to errors in the text representation, ${backingFile.name} could not be saved.",
                    NotificationType.WARNING
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
                    "Saved NBT file successfully",
                    "${backingFile.name} was saved successfully.",
                    NotificationType.INFORMATION
                ).notify(project)
            }
        }
    }
}

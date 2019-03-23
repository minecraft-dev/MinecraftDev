/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.nbt.editor.CompressionSelection
import com.demonwav.mcdev.nbt.editor.NbtToolbar
import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

class NbtVirtualFile(private val backingFile: VirtualFile, private val project: Project) : VirtualFile() {

    var bytes: ByteArray
    val isCompressed: Boolean
    lateinit var toolbar: NbtToolbar
    val parseSuccessful: Boolean

    init {
        this.bytes = byteArrayOf()
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

        if (this.parseSuccessful) {
            val psiFile = runReadAction {
                PsiFileFactory.getInstance(project).createFileFromText(NbttLanguage, text)
            }
            invokeAndWait {
                psiFile.runWriteAction {
                    this.bytes = PsiDocumentManager.getInstance(project).getDocument(
                        CodeStyleManager.getInstance(project).reformat(psiFile, true) as PsiFile
                    )?.immutableCharSequence?.toString()?.toByteArray() ?: byteArrayOf()
                }
            }
        } else {
            this.bytes = text.toByteArray()
        }
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        backingFile.refresh(asynchronous, recursive, postRunnable)
    }

    override fun getLength() = bytes.size.toLong()
    override fun getFileSystem() = backingFile.fileSystem
    override fun getPath() = backingFile.path
    override fun isDirectory() = false
    override fun getTimeStamp() = backingFile.timeStamp
    override fun getModificationStamp() = 0L
    override fun getName() = backingFile.name + (if (parseSuccessful) ".nbtt" else ".txt") // don't highlight syntax on bad files
    override fun contentsToByteArray() = bytes
    override fun isValid() = backingFile.isValid
    override fun getInputStream() = ByteArrayInputStream(bytes)
    override fun getParent() = backingFile
    override fun getChildren() = emptyArray<VirtualFile>()
    override fun isWritable() = backingFile.isWritable
    override fun getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long) =
        VfsUtilCore.outputStreamAddingBOM(NbtOutputStream(this, requestor), this)

    fun writeFile(requester: Any) {
        val nbttFile = PsiManager.getInstance(project).findFile(this) as NbttFile
        val rootTag = nbttFile.getRootCompound()?.getRootCompoundTag()

        if (rootTag == null) {
            Notification(
                "NBT Save Error",
                "Error Saving NBT File",
                "Due to errors in the text representation, ${backingFile.name} could not be saved.",
                NotificationType.WARNING
            ).notify(project)
            return
        }

        this.bytes = PsiDocumentManager.getInstance(project).getDocument(nbttFile)?.immutableCharSequence?.toString()?.toByteArray()
            ?: byteArrayOf()

        // just to be safe
        this.parent.bom = null
        val filteredStream = when (toolbar.selection) {
            CompressionSelection.GZIP -> GZIPOutputStream(this.parent.getOutputStream(requester))
            CompressionSelection.UNCOMPRESSED -> this.parent.getOutputStream(requester)
        }

        DataOutputStream(filteredStream).use { stream ->
            rootTag.write(stream)
        }
    }
}

private class NbtOutputStream(private val file: NbtVirtualFile, private val requestor: Any) : ByteArrayOutputStream() {
    override fun close() {
        file.bytes = toByteArray()

        file.writeFile(requestor)
    }
}

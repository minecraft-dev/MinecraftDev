/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.nbt.editor.CompressionSelection
import com.demonwav.mcdev.nbt.editor.NbtToolbar
import com.demonwav.mcdev.nbt.lang.NbttFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.GZIPOutputStream

class NbtVirtualFile(private val backingFile: VirtualFile, private val project: Project) : VirtualFile() {

    var bytes: ByteArray
    val isCompressed: Boolean
    lateinit var toolbar: NbtToolbar
    val parseSuccessful: Boolean

    init {
        var tempCompressed: Boolean
        var tempParseSuccessful: Boolean
        try {
            val (rootCompound, isCompressed) = Nbt.buildTagTree(backingFile.inputStream, 1000)
            this.bytes = rootCompound.toString().toByteArray()
            tempCompressed = isCompressed
            tempParseSuccessful = true
        } catch (e: MalformedNbtFileException) {
            this.bytes = "Malformed NBT file:\n${e.message}".toByteArray()
            tempCompressed = false
            tempParseSuccessful = false
        }
        this.isCompressed = tempCompressed
        this.parseSuccessful = tempParseSuccessful
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

    fun writeFile(requestor: Any) {
        val nbttFile = PsiManager.getInstance(project).findFile(this) as NbttFile
        val rootTag = nbttFile.getRootCompound().getRootCompoundTag()

        // just to be safe
        this.parent.bom = null
        val filteredStream = when (toolbar.selection) {
            CompressionSelection.GZIP -> GZIPOutputStream(this.parent.getOutputStream(requestor))
            CompressionSelection.UNCOMPRESSED -> this.parent.getOutputStream(requestor)
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

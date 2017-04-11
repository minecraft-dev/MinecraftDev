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

import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttRootCompound
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.GZIPOutputStream

class NbtVirtualFile(private val backingFile: VirtualFile, private val project: Project) : VirtualFile() {

    var bytes = Nbt.buildTagTree(backingFile.inputStream).toString().toByteArray()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        backingFile.refresh(asynchronous, recursive, postRunnable)
    }

    override fun getLength() = bytes.size.toLong()
    override fun getFileSystem() = backingFile.fileSystem
    override fun getPath() = backingFile.path
    override fun isDirectory() = false
    override fun getTimeStamp() = backingFile.timeStamp
    override fun getModificationStamp() = 0L
    override fun getName() = backingFile.name + ".nbtt"
    override fun contentsToByteArray() = bytes
    override fun isValid() = backingFile.isValid
    override fun getInputStream() = ByteArrayInputStream(bytes)
    override fun getParent() = backingFile
    override fun getChildren() = emptyArray<VirtualFile>()
    override fun isWritable() = backingFile.isWritable
    override fun getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long) =
        VfsUtilCore.outputStreamAddingBOM(NbtOutputStream(requestor, this, project, true), this)
}

private class NbtOutputStream(
    private val requestor: Any,
    private val file: NbtVirtualFile,
    private val project: Project,
    private val compressed: Boolean
) : ByteArrayOutputStream() {
    override fun close() {
        file.bytes = toByteArray()

        val nbttFile = PsiManager.getInstance(project).findFile(file) as NbttFile
        val rootTag = nbttFile.getRootCompound().getRootCompoundTag()

        // just to be safe
        file.parent.bom = null
        val filteredStream = if (compressed) {
            GZIPOutputStream(file.parent.getOutputStream(requestor))
        } else {
            file.parent.getOutputStream(requestor)
        }

        DataOutputStream(filteredStream).use { stream ->
            rootTag.write(stream)
        }
    }
}

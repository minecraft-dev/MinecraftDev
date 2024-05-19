package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.io.FileNotFoundException

class VfsLoadedTemplate(
    val root: VirtualFile,
    override val label: String,
    override val tooltip: String? = null,
    override val descriptor: TemplateDescriptor,
    override val isValid: Boolean,
) : LoadedTemplate {

    override fun loadTemplateContents(path: String): String? {
        val virtualFile = root.fileSystem.findFileByPath(root.path + "/" + path)
            ?: throw FileNotFoundException("Could not find file $path in template root ${root.path}")
        return virtualFile.readText()
    }
}

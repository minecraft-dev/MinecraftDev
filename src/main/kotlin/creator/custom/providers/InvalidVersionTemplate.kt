package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.intellij.openapi.vfs.VirtualFile

class InvalidVersionTemplate(
    templateRoot: VirtualFile,
    label: String,
    descriptor: TemplateDescriptor,
    tooltip: String? = null
) : VfsLoadedTemplate(
    templateRoot,
    label,
    tooltip,
    descriptor,
    false
) {
    override fun loadTemplateContents(path: String): String {
        throw NotImplementedError("You cannot load template contents of a template with an invalid version.")
    }
}

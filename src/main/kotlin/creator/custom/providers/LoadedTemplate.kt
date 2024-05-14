package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor

interface LoadedTemplate {

    val descriptor: TemplateDescriptor

    fun loadTemplateContents(path: String): String?
}

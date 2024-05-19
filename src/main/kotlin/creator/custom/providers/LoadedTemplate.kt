package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor

interface LoadedTemplate {

    val label: String
    val tooltip: String?
    val descriptor: TemplateDescriptor
    val isValid: Boolean

    fun loadTemplateContents(path: String): String?

    fun serialize(): String?
}

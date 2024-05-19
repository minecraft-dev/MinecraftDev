package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.TemplateDescriptor

/**
 * Placeholder template
 */
object EmptyLoadedTemplate : LoadedTemplate {

    override val label: String = "Empty template"
    override val tooltip: String = "Empty template tooltip"

    override val descriptor: TemplateDescriptor
        get() = throw UnsupportedOperationException("The empty template can't have a descriptor")

    override val isValid: Boolean = false

    override fun loadTemplateContents(path: String): String? =
        throw UnsupportedOperationException("The empty template can't have contents")
}

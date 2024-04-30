package com.demonwav.mcdev.creator.custom

data class TemplateDescriptor(
    val properties: List<TemplateProperty>,
    val files: List<TemplateFile>
)

data class TemplateProperty(
    val name: String,
    val type: String,
    val label: String,
    val options: List<Any>,
    val default: Any
)

data class TemplateFile(
    val template: String,
    val destination: String,
    val condition: String? = null
)

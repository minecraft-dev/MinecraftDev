package com.demonwav.mcdev.creator.custom

data class TemplateDescriptor(
    val properties: List<TemplateProperty>,
    val files: List<TemplateFile>,
)

data class TemplateProperty(
    val name: String,
    val type: String,
    val label: String,
    val options: List<Any>,
    val remember: Boolean?,
    val hidden: Boolean?,
    val editable: Boolean?,
    val default: Any,
    val derives: PropertyDerivation?,
)

data class PropertyDerivation(
    val from: String,
    val method: String,
    val default: Any,
    val whenModified: Boolean?,
)

data class TemplateFile(
    val template: String,
    val destination: String,
    val condition: String? = null,
)

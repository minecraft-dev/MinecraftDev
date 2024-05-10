package com.demonwav.mcdev.creator.custom

data class TemplateDescriptor(
    val properties: List<TemplatePropertyDescriptor>,
    val files: List<TemplateFile>,
)

data class TemplatePropertyDescriptor(
    val name: String,
    val type: String,
    val label: String,
    val options: List<Any>?,
    val remember: Boolean?,
    val hidden: Boolean?,
    val editable: Boolean?,
    val default: Any,
    val derives: PropertyDerivation?,
    val inheritFrom: String?
)

data class PropertyDerivation(
    val parents: List<String>?,
    val method: String,
    val default: Any,
    val whenModified: Boolean?,
    val parameters: Map<String, Any?>?,
)

data class TemplateFile(
    val template: String,
    val destination: String,
    val condition: String? = null,
)

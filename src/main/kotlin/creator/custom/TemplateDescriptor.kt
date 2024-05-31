package com.demonwav.mcdev.creator.custom

data class TemplateDescriptor(
    val version: Int,
    val label: String? = null,
    val inherit: String? = null,
    val hidden: Boolean? = null,
    val properties: List<TemplatePropertyDescriptor>,
    val files: List<TemplateFile>,
)

data class TemplatePropertyDescriptor(
    val name: String,
    val type: String,
    val label: String,
    val order: Int? = null,
    val options: Any? = null,
    val limit: Int? = null,
    val maxSegmentedButtonsCount: Int? = null,
    val forceDropdown: Boolean? = null,
    val groupProperties: List<TemplatePropertyDescriptor>? = null,
    val remember: Boolean? = null,
    val hidden: Boolean? = null,
    val editable: Boolean? = null,
    val collapsible: Boolean? = null,
    val default: Any,
    val nullIfDefault: Boolean? = null,
    val derives: PropertyDerivation? = null,
    val inheritFrom: String? = null,
    val parameters: Map<String, Any>? = null
)

data class PropertyDerivation(
    val parents: List<String>? = null,
    val method: String,
    val default: Any,
    val whenModified: Boolean? = null,
    val parameters: Map<String, Any?>? = null,
)

data class TemplateFile(
    val template: String,
    val destination: String,
    val condition: String? = null,
    var contents: String = "",
)

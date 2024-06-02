/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.creator.custom

data class TemplateDescriptor(
    val version: Int,
    val label: String? = null,
    val inherit: String? = null,
    val hidden: Boolean? = null,
    val properties: List<TemplatePropertyDescriptor>? = null,
    val files: List<TemplateFile>? = null,
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
    val parameters: Map<String, Any>? = null,
    val validator: Any? = null
)

data class PropertyDerivation(
    val parents: List<String>? = null,
    val method: String? = null,
    val select: List<PropertyDerivationSelect>? = null,
    val default: Any? = null,
    val whenModified: Boolean? = null,
    val parameters: Map<String, Any?>? = null,
)

data class PropertyDerivationSelect(
    val condition: String,
    val value: Any
)

data class TemplateFile(
    val template: String,
    val destination: String,
    val condition: String? = null,
    var contents: String = "",
)

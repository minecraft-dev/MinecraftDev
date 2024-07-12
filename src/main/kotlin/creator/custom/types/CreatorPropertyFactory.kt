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

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

interface CreatorPropertyFactory {

    companion object {

        private val EP_NAME = ExtensionPointName<KeyedLazyInstance<CreatorPropertyFactory>>(
            "com.demonwav.minecraft-dev.creatorPropertyType"
        )

        private val COLLECTOR = KeyedExtensionCollector<CreatorPropertyFactory, String>(EP_NAME)

        fun createFromType(
            type: String,
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*>? {
            return COLLECTOR.findSingle(type)?.create(descriptor, graph, properties)
        }
    }

    fun create(
        descriptor: TemplatePropertyDescriptor,
        graph: PropertyGraph,
        properties: Map<String, CreatorProperty<*>>
    ): CreatorProperty<*>
}

class CreatorPropertyFactoryBean :
    BaseKeyedLazyInstance<CreatorPropertyFactory>(), KeyedLazyInstance<CreatorPropertyFactory> {

    @Attribute("type")
    @RequiredElement
    lateinit var type: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getImplementationClassName(): String = implementation

    override fun getKey(): String = type
}

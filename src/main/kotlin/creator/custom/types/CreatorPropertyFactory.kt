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

        private val EP_NAME =
            ExtensionPointName<KeyedLazyInstance<CreatorPropertyFactory>>("com.demonwav.minecraft-dev.creatorPropertyType")

        private val COLLECTOR = KeyedExtensionCollector<CreatorPropertyFactory, String>(EP_NAME)

        fun createFromType(
            type: String,
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*>? {
            return COLLECTOR.findSingle(type)?.create(graph, descriptor, properties)
        }
    }

    fun create(
        graph: PropertyGraph,
        descriptor: TemplatePropertyDescriptor,
        properties: Map<String, CreatorProperty<*>>
    ): CreatorProperty<*>
}

class CreatorPropertyFactoryBean : BaseKeyedLazyInstance<CreatorPropertyFactory>(),
    KeyedLazyInstance<CreatorPropertyFactory> {

    @Attribute("type")
    @RequiredElement
    lateinit var type: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getImplementationClassName(): String = implementation

    override fun getKey(): String = type
}

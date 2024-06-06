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

package com.demonwav.mcdev.creator.custom.finalizers

import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

interface CreatorFinalizer {

    fun execute(project: Project, properties: Map<String, Any>, templateProperties: Map<String, Any?>)

    companion object {
        private val EP_NAME =
            ExtensionPointName.create<CreatorFinalizerBean>("com.demonwav.minecraft-dev.creatorFinalizer")
        private val COLLECTOR = KeyedExtensionCollector<CreatorFinalizer, String>(EP_NAME)

        fun executeAll(project: Project, finalizers: List<Map<String, Any>>, templateProperties: Map<String, Any?>) {
            for (properties in finalizers) {
                val type = properties["type"] as? String
                if (type == null) {
                    thisLogger().warn("Missing finalizer 'type' value")
                    continue
                }

                val condition = properties["condition"] as? String
                if (condition != null &&
                    !TemplateEvaluator.condition(templateProperties, condition).getOrElse { false }
                ) {
                    continue
                }

                val finalizer = COLLECTOR.findSingle(type)
                if (finalizer == null) {
                    thisLogger().warn("Unknown finalizer $type")
                    continue
                }

                try {
                    finalizer.execute(project, properties, templateProperties)
                } catch (t: Throwable) {
                    if (t is ControlFlowException) {
                        throw t
                    }
                    thisLogger().error("Unhandled exception in finalizer $type", t)
                }
            }
        }
    }
}

class CreatorFinalizerBean : BaseKeyedLazyInstance<CreatorFinalizer>(), KeyedLazyInstance<CreatorFinalizer> {

    @Attribute("type")
    @RequiredElement
    lateinit var type: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getKey(): String? = type

    override fun getImplementationClassName(): String? = implementation
}

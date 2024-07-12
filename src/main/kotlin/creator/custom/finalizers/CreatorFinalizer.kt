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
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.TemplateValidationReporterImpl
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

interface CreatorFinalizer {

    fun validate(reporter: TemplateValidationReporter, properties: Map<String, Any>) = Unit

    fun execute(context: WizardContext, properties: Map<String, Any>, templateProperties: Map<String, Any?>)

    companion object {
        private val EP_NAME =
            ExtensionPointName.create<CreatorFinalizerBean>("com.demonwav.minecraft-dev.creatorFinalizer")
        private val COLLECTOR = KeyedExtensionCollector<CreatorFinalizer, String>(EP_NAME)

        fun validateAll(
            reporter: TemplateValidationReporterImpl,
            finalizers: List<Map<String, Any>>,
        ) {
            for ((index, properties) in finalizers.withIndex()) {
                reporter.subject = "Finalizer #$index"

                val type = properties["type"] as? String
                if (type == null) {
                    reporter.error("Missing required 'type' value")
                }

                val condition = properties["condition"]
                if (condition != null && condition !is String) {
                    reporter.error("'condition' must be a string")
                }

                if (type != null) {
                    val finalizer = COLLECTOR.findSingle(type)
                    if (finalizer == null) {
                        reporter.error("Unknown finalizer of type $type")
                    } else {
                        try {
                            finalizer.validate(reporter, properties)
                        } catch (t: Throwable) {
                            reporter.error("Unexpected error during finalizer validation: ${t.message}")
                            thisLogger().error("Unexpected error during finalizer validation", t)
                        }
                    }
                }
            }
        }

        fun executeAll(
            context: WizardContext,
            finalizers: List<Map<String, Any>>,
            templateProperties: Map<String, Any?>
        ) {
            for ((index, properties) in finalizers.withIndex()) {
                val type = properties["type"] as String
                val condition = properties["condition"] as? String
                if (condition != null &&
                    !TemplateEvaluator.condition(templateProperties, condition).getOrElse { false }
                ) {
                    continue
                }

                val finalizer = COLLECTOR.findSingle(type)!!
                try {
                    finalizer.execute(context, properties, templateProperties)
                } catch (t: Throwable) {
                    if (t is ControlFlowException) {
                        throw t
                    }
                    thisLogger().error("Unhandled exception in finalizer #$index ($type)", t)
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

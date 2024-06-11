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

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel

interface TemplateValidationReporter {

    fun warn(message: String)

    fun error(message: String)

    fun fatal(message: String, cause: Throwable? = null): Nothing
}

class TemplateValidationReporterImpl : TemplateValidationReporter {

    private val validationItems: MutableMap<String, MutableList<TemplateValidationItem>> = linkedMapOf()
    var hasErrors = false
        private set
    var hasWarns = false
        private set

    var subject: String? = null

    override fun warn(message: String) {
        check(subject != null) { "No subject is being validated" }
        hasWarns = true
        validationItems.getOrPut(subject!!, ::mutableListOf).add(TemplateValidationItem.Warn(message))
    }

    override fun error(message: String) {
        check(subject != null) { "No subject is being validated" }
        hasErrors = true
        validationItems.getOrPut(subject!!, ::mutableListOf).add(TemplateValidationItem.Error(message))
    }

    override fun fatal(message: String, cause: Throwable?): Nothing {
        error("Fatal validation error: $message")
        throw TemplateValidationException(message, cause)
    }

    fun display(panel: Panel) {
        if (!hasErrors && !hasWarns) {
            return
        }

        panel.row {
            when {
                hasWarns && hasErrors -> label(MCDevBundle("creator.ui.error.template_warns_and_errors")).apply {
                    component.foreground = JBColor.RED
                }

                hasWarns -> label(MCDevBundle("creator.ui.error.template_warns")).apply {
                    component.foreground = JBColor.YELLOW
                }

                hasErrors -> label(MCDevBundle("creator.ui.error.template_errors")).apply {
                    component.foreground = JBColor.RED
                }
            }
        }

        for ((subjectName, items) in validationItems) {
            panel.row {
                label("$subjectName:")
            }

            panel.indent {
                for (item in items) {
                    row {
                        label(item.message).component.foreground = item.color
                    }
                }
            }
        }
    }
}

class TemplateValidationException(message: String?, cause: Throwable? = null) : Exception(message, cause)

private sealed class TemplateValidationItem(val message: String, val color: JBColor) {

    class Warn(message: String) : TemplateValidationItem(message, JBColor.YELLOW)
    class Error(message: String) : TemplateValidationItem(message, JBColor.RED)
}

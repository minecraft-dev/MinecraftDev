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

import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.util.runGradleTaskAndWait
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger

class RunGradleTasksFinalizer : CreatorFinalizer {

    override fun validate(
        reporter: TemplateValidationReporter,
        properties: Map<String, Any>
    ) {
        @Suppress("UNCHECKED_CAST")
        val tasks = properties["tasks"] as? List<String>
        if (tasks == null) {
            reporter.warn("Missing list of 'tasks' to execute")
        }
    }

    override fun execute(context: WizardContext, properties: Map<String, Any>, templateProperties: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val tasks = properties["tasks"] as List<String>
        val project = context.project!!
        val projectDir = context.projectDirectory

        thisLogger().info("tasks = $tasks projectDir = $projectDir")
        runGradleTaskAndWait(project, projectDir) { settings ->
            settings.taskNames = tasks
        }

        thisLogger().info("Done running tasks")
    }
}

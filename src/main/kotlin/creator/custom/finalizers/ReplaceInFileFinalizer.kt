/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Replaces text in a file of the project, for example to adjust a file created by another finalizer.
 *
 * - `file`: the path of the file, relative to the project directory, evaluated as a template
 * - `find`: the text to find, used as-is
 * - `regex`: whether `find` is a regular expression, defaults to `false`
 * - `replace`: the replacement text, evaluated as a template
 */
class ReplaceInFileFinalizer : CreatorFinalizer {

    override fun validate(reporter: TemplateValidationReporter, properties: Map<String, Any>) {
        if (properties["file"] !is String) {
            reporter.error("Missing 'file' to replace text in")
        }

        val find = properties["find"]
        if (find !is String || find.isEmpty()) {
            reporter.error("Missing 'find' text")
        } else if (properties["regex"] == true) {
            runCatching { Regex(find) }.onFailure { reporter.error("Invalid 'find' regex: ${it.message}") }
        }

        if (properties["replace"] !is String) {
            reporter.error("Missing 'replace' text")
        }

        val regex = properties["regex"]
        if (regex != null && regex !is Boolean) {
            reporter.error("'regex' must be a boolean")
        }
    }

    override suspend fun execute(
        context: WizardContext,
        project: Project,
        properties: Map<String, Any>,
        templateProperties: Map<String, Any?>
    ) {
        val projectDir = context.projectDirectory
        val relativePath = TemplateEvaluator.template(templateProperties, properties["file"] as String).getOrNull()
            ?: return
        val file = projectDir.resolve(relativePath).toAbsolutePath().normalize()
        if (!file.startsWith(projectDir)) {
            thisLogger().error("File $relativePath is outside of the project directory")
            return
        }

        val find = properties["find"] as String
        val replace = TemplateEvaluator.template(templateProperties, properties["replace"] as String).getOrNull()
            ?: return

        withContext(Dispatchers.IO) {
            if (!file.isRegularFile()) {
                thisLogger().warn("Could not replace text in $file, it does not exist")
                return@withContext
            }

            val text = file.readText()
            val newText = if (properties["regex"] == true) {
                // The replacement is used literally, so it can't accidentally reference groups
                Regex(find).replace(text, Regex.escapeReplacement(replace))
            } else {
                text.replace(find, replace)
            }

            if (newText == text) {
                thisLogger().warn("Could not find '$find' in $file")
                return@withContext
            }
            file.writeText(newText)
        }

        LocalFileSystem.getInstance().refreshNioFiles(listOf(file))
    }
}
